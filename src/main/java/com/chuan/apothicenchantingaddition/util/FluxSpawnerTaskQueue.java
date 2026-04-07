package com.chuan.apothicenchantingaddition.util;

import com.chuan.apothicenchantingaddition.ApothicEnchantingAddition;
import com.chuan.apothicenchantingaddition.block.entity.FluxSpawnerBlockEntity;
import com.chuan.apothicenchantingaddition.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 将 Flux Spawner 的掉落 roll 改成分帧结算，避免在单个 tick 内一次性跑完全部 loot table。
 */
@EventBusSubscriber(modid = ApothicEnchantingAddition.MOD_ID)
public final class FluxSpawnerTaskQueue {

    private static final int BASE_ROLL_BUDGET_PER_TICK = 48;
    private static final int BURST_ROLL_BUDGET_PER_TICK = 128;
    private static final int DROP_FLUSH_STACK_THRESHOLD = 96;

    private static final Map<ServerLevel, LinkedHashMap<BlockPos, QueuedSpawnerTask>> TASKS = new WeakHashMap<>();

    private FluxSpawnerTaskQueue() {
    }

    public static void enqueue(ServerLevel level, BlockPos pos, Map<EntityType<?>, Integer> eggTypeCounts, int totalRollsPerEgg, int expCount) {
        if (eggTypeCounts.isEmpty() && expCount <= 0) {
            return;
        }

        LinkedHashMap<BlockPos, QueuedSpawnerTask> levelTasks = TASKS.computeIfAbsent(level, ignored -> new LinkedHashMap<>());
        BlockPos immutablePos = pos.immutable();
        QueuedSpawnerTask task = levelTasks.computeIfAbsent(immutablePos, QueuedSpawnerTask::new);
        task.enqueueJob(eggTypeCounts, totalRollsPerEgg, expCount);
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        LinkedHashMap<BlockPos, QueuedSpawnerTask> levelTasks = TASKS.get(serverLevel);
        if (levelTasks == null || levelTasks.isEmpty()) {
            return;
        }

        int remainingBudget = event.hasTime() ? BURST_ROLL_BUDGET_PER_TICK : BASE_ROLL_BUDGET_PER_TICK;
        int guard = Math.max(8, levelTasks.size() * 3);

        while (remainingBudget > 0 && !levelTasks.isEmpty() && guard-- > 0) {
            Map.Entry<BlockPos, QueuedSpawnerTask> firstEntry = levelTasks.entrySet().iterator().next();
            BlockPos key = firstEntry.getKey();
            QueuedSpawnerTask task = firstEntry.getValue();
            levelTasks.remove(key);

            int taskBudget = Math.min(remainingBudget, task.recommendedBudget(event.hasTime()));
            int consumed = task.process(serverLevel, Math.max(1, taskBudget));
            remainingBudget -= Math.max(1, consumed);

            if (!task.isFinished()) {
                levelTasks.put(key, task);
            }
        }

        if (levelTasks.isEmpty()) {
            TASKS.remove(serverLevel);
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        TASKS.remove(serverLevel);
        FluxSpawnerLootProbabilityModelCache.clear(serverLevel);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        TASKS.clear();
        FluxSpawnerLootProbabilityModelCache.clearAll();
    }

    private static final class QueuedSpawnerTask {
        private final BlockPos spawnerPos;
        private final ArrayDeque<LootJob> pendingJobs = new ArrayDeque<>();

        private QueuedSpawnerTask(BlockPos spawnerPos) {
            this.spawnerPos = spawnerPos;
        }

        private void enqueueJob(Map<EntityType<?>, Integer> eggTypeCounts, int totalRollsPerEgg, int expCount) {
            this.pendingJobs.addLast(new LootJob(this.spawnerPos, eggTypeCounts, totalRollsPerEgg, expCount));
        }

        private boolean isFinished() {
            return this.pendingJobs.isEmpty();
        }

        private int recommendedBudget(boolean hasTime) {
            LootJob currentJob = this.pendingJobs.peekFirst();
            return currentJob == null ? 1 : currentJob.recommendedBudget(hasTime);
        }

        private int process(ServerLevel level, int budget) {
            int consumed = 0;

            while (consumed < budget && !this.pendingJobs.isEmpty()) {
                LootJob currentJob = this.pendingJobs.peekFirst();
                consumed += currentJob.process(level, budget - consumed);

                if (currentJob.isFinished()) {
                    this.pendingJobs.removeFirst();
                } else {
                    break;
                }
            }

            return consumed;
        }
    }

    private static final class LootJob {
        private final BlockPos spawnerPos;
        private final ArrayDeque<EntityWorkUnit> workUnits = new ArrayDeque<>();
        private final DropAccumulator accumulator = new DropAccumulator();
        private int expRemaining;
        private boolean canceled;

        private LootJob(BlockPos spawnerPos, Map<EntityType<?>, Integer> eggTypeCounts, int totalRollsPerEgg, int expCount) {
            this.spawnerPos = spawnerPos;
            for (Map.Entry<EntityType<?>, Integer> entry : eggTypeCounts.entrySet()) {
                int totalRolls = Math.max(0, totalRollsPerEgg * entry.getValue());
                if (totalRolls > 0) {
                    this.workUnits.addLast(new EntityWorkUnit(entry.getKey(), totalRolls));
                }
            }
            this.expRemaining = Math.max(0, expCount);
        }

        private boolean isFinished() {
            return this.canceled || (this.workUnits.isEmpty() && this.expRemaining <= 0 && this.accumulator.isEmpty());
        }

        private int recommendedBudget(boolean hasTime) {
            EntityWorkUnit currentUnit = this.workUnits.peekFirst();
            if (currentUnit == null) {
                return hasTime ? 8 : 4;
            }

            FluxSpawnerLootProbabilityModelCache.LootProbabilityModel model = currentUnit.getCachedModel();
            return model == null ? (hasTime ? 6 : 3) : model.recommendedRollBudget(hasTime);
        }

        private int process(ServerLevel level, int budget) {
            FluxSpawnerBlockEntity owner = resolveOwner(level);
            if (owner == null) {
                this.canceled = true;
                this.workUnits.clear();
                this.expRemaining = 0;
                this.accumulator.clear();
                return 0;
            }

            FakePlayer fakePlayer = FakePlayerFactory.getMinecraft(level);
            DamageSource damageSource = level.damageSources().playerAttack(fakePlayer);
            int consumed = 0;

            while (consumed < budget) {
                if (!this.workUnits.isEmpty()) {
                    EntityWorkUnit workUnit = this.workUnits.peekFirst();
                    FluxSpawnerLootProbabilityModelCache.LootProbabilityModel model = workUnit.getOrCreateModel(level);
                    this.accumulator.mergeDrops(model.sample(level, this.spawnerPos, damageSource, fakePlayer));
                    workUnit.remainingRolls--;
                    consumed++;

                    if (workUnit.remainingRolls <= 0) {
                        this.workUnits.removeFirst();
                    }
                } else if (this.expRemaining > 0) {
                    int size = Math.min(this.expRemaining, 64);
                    this.accumulator.mergeSingleDrop(new ItemStack(ModRegistry.SOLIDIFIED_FLUX_EXPERIENCE.get(), size));
                    this.expRemaining -= size;
                    consumed++;
                } else {
                    break;
                }

                if (this.accumulator.getStackCount() >= DROP_FLUSH_STACK_THRESHOLD) {
                    this.accumulator.flushToOwner(owner);
                }
            }

            if (this.workUnits.isEmpty() && this.expRemaining <= 0) {
                this.accumulator.flushToOwner(owner);
            }

            return consumed;
        }

        private FluxSpawnerBlockEntity resolveOwner(ServerLevel level) {
            BlockEntity blockEntity = level.getBlockEntity(this.spawnerPos);
            if (blockEntity instanceof FluxSpawnerBlockEntity fluxSpawner && !fluxSpawner.isRemoved()) {
                return fluxSpawner;
            }
            return null;
        }
    }

    private static final class EntityWorkUnit {
        private final EntityType<?> entityType;
        private int remainingRolls;
        private FluxSpawnerLootProbabilityModelCache.LootProbabilityModel cachedModel;

        private EntityWorkUnit(EntityType<?> entityType, int remainingRolls) {
            this.entityType = entityType;
            this.remainingRolls = remainingRolls;
        }

        private FluxSpawnerLootProbabilityModelCache.LootProbabilityModel getOrCreateModel(ServerLevel level) {
            if (this.cachedModel == null) {
                this.cachedModel = FluxSpawnerLootProbabilityModelCache.get(level, this.entityType);
            }
            return this.cachedModel;
        }

        private FluxSpawnerLootProbabilityModelCache.LootProbabilityModel getCachedModel() {
            return this.cachedModel;
        }
    }

    private static final class DropAccumulator {
        private final Map<Item, List<ItemStack>> buckets = new LinkedHashMap<>();
        private int stackCount;

        private int getStackCount() {
            return this.stackCount;
        }

        private boolean isEmpty() {
            return this.stackCount == 0;
        }

        private void clear() {
            this.buckets.clear();
            this.stackCount = 0;
        }

        private void mergeDrops(List<ItemStack> newDrops) {
            for (ItemStack drop : newDrops) {
                mergeSingleDrop(drop);
            }
        }

        private void mergeSingleDrop(ItemStack drop) {
            if (drop.isEmpty()) {
                return;
            }

            ItemStack remaining = drop.copy();
            int maxStackSize = remaining.getMaxStackSize();
            List<ItemStack> bucket = this.buckets.computeIfAbsent(remaining.getItem(), ignored -> new ArrayList<>());

            for (ItemStack existing : bucket) {
                if (!ItemStack.isSameItemSameComponents(existing, remaining)) {
                    continue;
                }

                int transferable = Math.min(maxStackSize - existing.getCount(), remaining.getCount());
                if (transferable <= 0) {
                    continue;
                }

                existing.grow(transferable);
                remaining.shrink(transferable);
                if (remaining.isEmpty()) {
                    return;
                }
            }

            while (!remaining.isEmpty()) {
                int splitSize = Math.min(maxStackSize, remaining.getCount());
                ItemStack splitStack = remaining.copy();
                splitStack.setCount(splitSize);
                bucket.add(splitStack);
                this.stackCount++;
                remaining.shrink(splitSize);
            }
        }

        private void flushToOwner(FluxSpawnerBlockEntity owner) {
            if (isEmpty()) {
                return;
            }

            for (List<ItemStack> bucket : this.buckets.values()) {
                owner.acceptGeneratedDrops(bucket);
            }
            clear();
        }
    }
}
