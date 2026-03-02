package com.chuan.apothicenchantingaddition.block.entity;

import com.chuan.apothicenchantingaddition.config.ApothicAdditionConfig;
import com.chuan.apothicenchantingaddition.menu.FluxSpawnerMenu;
import com.chuan.apothicenchantingaddition.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.RangedWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class FluxSpawnerBlockEntity extends BlockEntity implements MenuProvider {

    // 1. 能量缓存：10亿 FE
    public final EnergyStorage energyStorage = new EnergyStorage(1_000_000_000, Integer.MAX_VALUE, Integer.MAX_VALUE);

    // 2. 核心属性
    private int minDelay = 200;
    private int maxDelay = 800;
    private int spawnCount = 4;
    private boolean redstoneControl = false;
    private int echoing = 0;
    private int delay = 200;
    private int lastEnergy = 0;

    // 3. 与 Menu 通信的数据槽
    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (energyStorage.getEnergyStored() >> 16) & 0xFFFF; // 高16位
                case 1 -> energyStorage.getEnergyStored() & 0xFFFF;         // 低16位
                case 2 -> minDelay;
                case 3 -> maxDelay;
                case 4 -> spawnCount;
                case 5 -> redstoneControl ? 1 : 0;
                case 6 -> echoing;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // 客户端读取，这里服务端无需反向赋值属性，但为了接口完整性保留
        }

        @Override
        public int getCount() {
            return 7;
        }
    };

    // 4. 物品系统 (72 槽位 = 8 输入 + 64 输出)
    public final ItemStackHandler inventory = new ItemStackHandler(72) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot < 8) return stack.getItem() instanceof SpawnEggItem;
            return true;
        }
        @Override
        public int getSlotLimit(int slot) { return slot < 8 ? 1 : 64; }
        @Override
        protected void onContentsChanged(int slot) { setChanged(); }
    };

    public final IItemHandler outputItemHandler = new RangedWrapper(inventory, 8, 72) {
        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return stack; // 拒绝插入
        }
    };

    public FluxSpawnerBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModRegistry.FLUX_SPAWNER_BE.get(), pos, blockState);
    }

    // ================== GUI 绑定接口 (MenuProvider) ==================

    @Override
    public Component getDisplayName() {
        return Component.translatable("gui.apothicenchantingaddition.flux_spawner");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new FluxSpawnerMenu(containerId, playerInventory, this, this.dataAccess);
    }

    // ================== 核心运行逻辑 (同上) ==================

    public static void tick(Level level, BlockPos pos, BlockState state, FluxSpawnerBlockEntity entity) {
        if (level.isClientSide) return;

        // 【极致优化 1】降低自动输出频率：每 10 tick (0.5秒) 执行一次即可，拯救服务器 TPS
        if (level.getGameTime() % 10 == 0) {
            entity.autoOutputToBelow();
        }

        // 【极致优化 2】零内存分配统计刷怪蛋：避免每 tick 创建 List 导致垃圾回收 (GC) 顿卡
        int eggCount = 0;
        for (int i = 0; i < 8; i++) {
            if (!entity.inventory.getStackInSlot(i).isEmpty()) {
                eggCount++;
            }
        }
        if (eggCount == 0) return; // 没蛋不工作

        if (entity.redstoneControl && !level.hasNeighborSignal(pos)) return;
        if (entity.isOutputFull()) return;

        // 计算耗电
        int baseCost = ApothicAdditionConfig.FLUX_SPAWNER_ENERGY_PER_EGG.get();
        int energyCost = (baseCost * eggCount)
                + (baseCost * (800 / Math.max(1, entity.maxDelay)))
                + (baseCost * entity.spawnCount)
                + (baseCost * entity.echoing * 2);

        if (entity.energyStorage.getEnergyStored() < energyCost) return;

        // 扣除能量
        entity.energyStorage.extractEnergy(energyCost, false);

        // 【极致优化 3】能量快照：每 20 tick (1秒) 检查一次能量变化并存盘，避免硬盘狂写或进度丢失
        if (level.getGameTime() % 20 == 0) {
            int currentEnergy = entity.energyStorage.getEnergyStored();
            if (currentEnergy != entity.lastEnergy) {
                entity.lastEnergy = currentEnergy;
                entity.setChanged();
            }
        }

        entity.delay--;
        if (entity.delay <= 0) {
            // 将遍历逻辑移入内部，不再传 List
            entity.generateLoot((ServerLevel) level);
            int range = entity.maxDelay - entity.minDelay;
            entity.delay = entity.minDelay + (range > 0 ? level.random.nextInt(range) : 0);
            entity.setChanged();
        }
    }

    private void generateLoot(ServerLevel serverLevel) {
        FakePlayer fakePlayer = FakePlayerFactory.getMinecraft(serverLevel);
        DamageSource damageSource = serverLevel.damageSources().playerAttack(fakePlayer);

        int eggCount = 0; // 用于计算经验

        // 遍历前 8 个输入槽
        for (int i = 0; i < 8; i++) {
            ItemStack stack = this.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof SpawnEggItem egg) {
                eggCount++;

                EntityType<?> entityType = egg.getType(ItemStack.EMPTY);
                ResourceKey<LootTable> lootTableKey = entityType.getDefaultLootTable();
                LootTable lootTable = serverLevel.getServer().reloadableRegistries().getLootTable(lootTableKey);

                Entity dummyEntity = entityType.create(serverLevel);
                if (dummyEntity != null) {
                    LootParams params = new LootParams.Builder(serverLevel)
                            .withParameter(LootContextParams.THIS_ENTITY, dummyEntity)
                            .withParameter(LootContextParams.DAMAGE_SOURCE, damageSource)
                            .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(this.worldPosition))
                            .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, fakePlayer)
                            .create(LootContextParamSets.ENTITY);

                    int totalRolls = this.spawnCount * (1 + this.echoing);
                    for (int j = 0; j < totalRolls; j++) {
                        insertLootToOutputs(lootTable.getRandomItems(params));
                    }
                    dummyEntity.discard(); // 必须保留，防止内存泄漏
                }
            }
        }

        // 生成固化通量经验掉落
        int expBase = ApothicAdditionConfig.FLUX_SPAWNER_EXP_BASE_COUNT.get();
        int expCount = expBase * eggCount * (1 + this.echoing);

        if (expCount > 0) {
            List<ItemStack> expDrops = new java.util.ArrayList<>();
            while (expCount > 0) {
                int size = Math.min(expCount, 64);
                expDrops.add(new ItemStack(ModRegistry.SOLIDIFIED_FLUX_EXPERIENCE.get(), size));
                expCount -= size;
            }
            insertLootToOutputs(expDrops);
        }
    }

    private void insertLootToOutputs(List<ItemStack> drops) {
        for (ItemStack drop : drops) {
            ItemStack remainder = drop.copy();
            for (int i = 8; i < 72; i++) {
                if (remainder.isEmpty()) break;
                remainder = this.inventory.insertItem(i, remainder, false);
            }
        }
    }

    private void autoOutputToBelow() {
        if (this.level == null) return;
        IItemHandler belowHandler = this.level.getCapability(Capabilities.ItemHandler.BLOCK, this.worldPosition.below(), Direction.UP);
        if (belowHandler == null) return;

        for (int i = 8; i < 72; i++) {
            ItemStack stackInSlot = this.inventory.getStackInSlot(i);
            if (!stackInSlot.isEmpty()) {
                ItemStack copy = stackInSlot.copy();
                for (int j = 0; j < belowHandler.getSlots(); j++) {
                    if (copy.isEmpty()) break;
                    copy = belowHandler.insertItem(j, copy, false);
                }
                this.inventory.setStackInSlot(i, copy);
            }
        }
    }

    private List<SpawnEggItem> getValidEggs() {
        List<SpawnEggItem> eggs = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof SpawnEggItem egg) {
                eggs.add(egg);
            }
        }
        return eggs;
    }

    private boolean isOutputFull() {
        for (int i = 8; i < 72; i++) {
            if (this.inventory.getStackInSlot(i).isEmpty()) return false;
        }
        return true;
    }

    // ================== Getter / Setter ==================
    public int getMinDelay() { return minDelay; }
    public void setMinDelay(int minDelay) { this.minDelay = minDelay; setChanged(); }

    public int getMaxDelay() { return maxDelay; }
    public void setMaxDelay(int maxDelay) { this.maxDelay = maxDelay; setChanged(); }

    public int getSpawnCount() { return spawnCount; }
    public void setSpawnCount(int spawnCount) { this.spawnCount = spawnCount; setChanged(); }

    public boolean isRedstoneControl() { return redstoneControl; }
    public void setRedstoneControl(boolean redstoneControl) { this.redstoneControl = redstoneControl; setChanged(); }

    public int getEchoing() { return echoing; }
    public void setEchoing(int echoing) { this.echoing = echoing; setChanged(); }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Energy", energyStorage.getEnergyStored());
        tag.put("Inventory", inventory.serializeNBT(registries));
        tag.putInt("MinDelay", minDelay);
        tag.putInt("MaxDelay", maxDelay);
        tag.putInt("SpawnCount", spawnCount);
        tag.putBoolean("RedstoneControl", redstoneControl);
        tag.putInt("Echoing", echoing);
        tag.putInt("CurrentDelay", delay);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Energy")) energyStorage.receiveEnergy(tag.getInt("Energy"), false);
        if (tag.contains("Inventory")) inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        if (tag.contains("MinDelay")) minDelay = tag.getInt("MinDelay");
        if (tag.contains("MaxDelay")) maxDelay = tag.getInt("MaxDelay");
        if (tag.contains("SpawnCount")) spawnCount = tag.getInt("SpawnCount");
        if (tag.contains("RedstoneControl")) redstoneControl = tag.getBoolean("RedstoneControl");
        if (tag.contains("Echoing")) echoing = tag.getInt("Echoing");
        if (tag.contains("CurrentDelay")) delay = tag.getInt("CurrentDelay");
    }
}
