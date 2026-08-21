package com.chuan.apothicenchantingaddition.block.entity;

import com.chuan.apothicenchantingaddition.block.FluxSpawnerBlock;
import com.chuan.apothicenchantingaddition.config.ApothicAdditionConfig;
import com.chuan.apothicenchantingaddition.menu.FluxSpawnerMenu;
import com.chuan.apothicenchantingaddition.registry.ModRegistry;
import com.chuan.apothicenchantingaddition.util.FluxSpawnerRecipeResolver;
import com.chuan.apothicenchantingaddition.util.FluxSpawnerTaskQueue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.RangedWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FluxSpawnerBlockEntity extends BlockEntity implements MenuProvider {

    private static final TagKey<EntityType<?>> APOTHIC_SPAWNER_BLACKLIST = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("apothic_spawners", "blacklisted_from_spawners"));
    private static final int BELOW_HANDLER_CACHE_TICKS = 5;

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
    private int cachedEggCount = 0;
    private boolean outputCacheDirty = true;
    private boolean cachedOutputHasItems = false;
    private boolean cachedOutputHasRoom = true;
    private long belowHandlerCacheExpiryTick = Long.MIN_VALUE;
    private @Nullable IItemHandler cachedBelowHandler = null;

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
            if (slot < 8) {
                return canInsertSpawnEgg(stack);
            }
            return true;
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot < 8 ? 1 : 64;
        }

        @Override
        protected void onContentsChanged(int slot) {
            if (slot < 8) {
                recountEggInputs();
            } else {
                outputCacheDirty = true;
            }
            setChanged();
        }
    };

    public final IItemHandler outputItemHandler = new RangedWrapper(inventory, 8, 72) {
        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return stack; // 拒绝插入
        }
    };

    public FluxSpawnerBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModRegistry.FLUX_SPAWNER_BE.get(), pos, blockState);
        recountEggInputs();
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

    // ================== 核心运行逻辑 ==================

    public static void tick(Level level, BlockPos pos, BlockState state, FluxSpawnerBlockEntity entity) {
        if (level.isClientSide) return;

        if (level.getGameTime() % 20 == 0) {
            int currentEnergy = entity.energyStorage.getEnergyStored();
            if (currentEnergy != entity.lastEnergy) {
                entity.lastEnergy = currentEnergy;
                entity.setChanged();

                int newEnergyLevel = calculateEnergyLevel(currentEnergy, entity.energyStorage.getMaxEnergyStored());
                BlockState currentState = level.getBlockState(pos);
                if (currentState.hasProperty(FluxSpawnerBlock.ENERGY_LEVEL)
                        && currentState.getValue(FluxSpawnerBlock.ENERGY_LEVEL) != newEnergyLevel) {
                    level.setBlock(pos, currentState.setValue(FluxSpawnerBlock.ENERGY_LEVEL, newEnergyLevel), 3);
                }
            }
        }

        if (level.getGameTime() % 5 == 0) {
            entity.autoOutputToBelow();
        }

        int eggCount = entity.cachedEggCount;
        if (eggCount == 0) return;

        if (entity.redstoneControl && !level.hasNeighborSignal(pos)) return;

        int baseCost = ApothicAdditionConfig.FLUX_SPAWNER_ENERGY_PER_EGG.get();
        int energyCost = (baseCost * eggCount)
                + (baseCost * (800 / Math.max(1, entity.maxDelay)))
                + (baseCost * entity.spawnCount)
                + (baseCost * entity.echoing * 2);

        if (entity.energyStorage.getEnergyStored() < energyCost) return;

        if (entity.delay > 1) {
            entity.energyStorage.extractEnergy(energyCost, false);
            entity.delay--;
            return;
        }

        FluxSpawnerRecipeResolver.SpawnPlan plan = entity.buildSpawnPlan((ServerLevel) level);
        if (!plan.hasValidEggs()) {
            return;
        }

        if (plan.producesItems() && !entity.hasOutputRoom()) {
            return;
        }

        entity.energyStorage.extractEnergy(energyCost, false);
        entity.enqueueLootGeneration((ServerLevel) level, plan);

        int range = entity.maxDelay - entity.minDelay;
        entity.delay = entity.minDelay + (range > 0 ? level.random.nextInt(range) : 0);
        entity.setChanged();
    }

    private FluxSpawnerRecipeResolver.SpawnPlan buildSpawnPlan(ServerLevel serverLevel) {
        Map<EntityType<?>, Integer> eggTypeCounts = collectProcessableEggTypeCounts();
        int totalRollsPerEgg = this.spawnCount * (1 + this.echoing);
        return FluxSpawnerRecipeResolver.buildPlan(serverLevel, eggTypeCounts, totalRollsPerEgg, this.echoing);
    }

    private void enqueueLootGeneration(ServerLevel serverLevel, FluxSpawnerRecipeResolver.SpawnPlan plan) {
        if (!plan.profiles().isEmpty() || plan.expCount() > 0) {
            FluxSpawnerTaskQueue.enqueue(serverLevel, this.worldPosition, plan.profiles(), plan.expCount());
        }
    }

    public void acceptGeneratedDrops(List<ItemStack> drops) {
        if (drops.isEmpty()) {
            return;
        }

        insertLootToOutputs(drops, getBelowHandler());
        setChanged();
    }

    private void insertLootToOutputs(List<ItemStack> drops, @Nullable IItemHandler belowHandler) {
        for (ItemStack drop : drops) {
            if (drop.isEmpty()) {
                continue;
            }

            ItemStack remainder = drop;

            if (belowHandler != null) {
                remainder = insertIntoHandler(belowHandler, remainder);
            }

            if (!remainder.isEmpty()) {
                for (int i = 8; i < 72; i++) {
                    if (remainder.isEmpty()) break;
                    remainder = this.inventory.insertItem(i, remainder, false);
                }
            }
        }
    }

    private @Nullable IItemHandler getBelowHandler() {
        if (this.level == null) {
            return null;
        }

        long gameTime = this.level.getGameTime();
        if (gameTime >= this.belowHandlerCacheExpiryTick) {
            this.cachedBelowHandler = this.level.getCapability(Capabilities.ItemHandler.BLOCK, this.worldPosition.below(), Direction.UP);
            this.belowHandlerCacheExpiryTick = gameTime + BELOW_HANDLER_CACHE_TICKS;
        }
        return this.cachedBelowHandler;
    }

    private ItemStack insertIntoHandler(IItemHandler handler, ItemStack stack) {
        ItemStack remainder = stack;
        for (int i = 0; i < handler.getSlots(); i++) {
            if (remainder.isEmpty()) break;
            remainder = handler.insertItem(i, remainder, false);
        }
        return remainder;
    }

    public static boolean canUseSpawnEgg(SpawnEggItem egg) {
        if (ApothicAdditionConfig.FLUX_SPAWNER_ENTITY_BLACKLIST_OPEN.get()) {
            return true;
        }

        EntityType<?> entityType = egg.getType(ItemStack.EMPTY);
        Holder<EntityType<?>> holder = BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(entityType);
        return !holder.is(APOTHIC_SPAWNER_BLACKLIST);
    }

    public boolean canInsertSpawnEgg(ItemStack stack) {
        if (!(stack.getItem() instanceof SpawnEggItem egg)) {
            return false;
        }
        if (!canUseSpawnEgg(egg)) {
            return false;
        }
        return this.level == null || FluxSpawnerRecipeResolver.canInsertEgg(this.level, egg);
    }

    private void autoOutputToBelow() {
        if (!hasOutputItems()) {
            return;
        }

        IItemHandler belowHandler = getBelowHandler();
        if (belowHandler == null) return;

        for (int i = 8; i < 72; i++) {
            ItemStack stackInSlot = this.inventory.getStackInSlot(i);
            if (stackInSlot.isEmpty()) {
                continue;
            }

            ItemStack original = stackInSlot.copy();
            ItemStack remainder = insertIntoHandler(belowHandler, original.copy());
            if (!ItemStack.matches(original, remainder)) {
                this.inventory.setStackInSlot(i, remainder);
            }
        }
    }

    private Map<EntityType<?>, Integer> collectProcessableEggTypeCounts() {
        Map<EntityType<?>, Integer> eggTypeCounts = new LinkedHashMap<>();
        for (int i = 0; i < 8; i++) {
            ItemStack stack = this.inventory.getStackInSlot(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof SpawnEggItem egg) || !canUseSpawnEgg(egg)) {
                continue;
            }

            EntityType<?> entityType = egg.getType(ItemStack.EMPTY);
            eggTypeCounts.merge(entityType, 1, Integer::sum);
        }
        return eggTypeCounts;
    }

    private boolean hasOutputItems() {
        refreshOutputCacheIfNeeded();
        return this.cachedOutputHasItems;
    }

    private boolean hasOutputRoom() {
        refreshOutputCacheIfNeeded();
        return this.cachedOutputHasRoom;
    }

    private void recountEggInputs() {
        int eggCount = 0;
        for (int i = 0; i < 8; i++) {
            ItemStack stack = this.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof SpawnEggItem egg && canUseSpawnEgg(egg)) {
                eggCount++;
            }
        }
        this.cachedEggCount = eggCount;
    }

    private void refreshOutputCacheIfNeeded() {
        if (!this.outputCacheDirty) {
            return;
        }

        boolean hasItems = false;
        boolean hasRoom = false;
        for (int i = 8; i < 72; i++) {
            ItemStack stack = this.inventory.getStackInSlot(i);
            if (stack.isEmpty()) {
                hasRoom = true;
            } else {
                hasItems = true;
                if (stack.getCount() < stack.getMaxStackSize()) {
                    hasRoom = true;
                }
            }

            if (hasItems && hasRoom) {
                break;
            }
        }

        this.cachedOutputHasItems = hasItems;
        this.cachedOutputHasRoom = hasRoom;
        this.outputCacheDirty = false;
    }

    // ================== Getter / Setter ==================
    public int getMinDelay() {
        return minDelay;
    }

    public void setMinDelay(int minDelay) {
        this.minDelay = minDelay;
        setChanged();
    }

    public int getMaxDelay() {
        return maxDelay;
    }

    public void setMaxDelay(int maxDelay) {
        this.maxDelay = maxDelay;
        setChanged();
    }

    public int getSpawnCount() {
        return spawnCount;
    }

    public void setSpawnCount(int spawnCount) {
        this.spawnCount = spawnCount;
        setChanged();
    }

    public boolean isRedstoneControl() {
        return redstoneControl;
    }

    public void setRedstoneControl(boolean redstoneControl) {
        this.redstoneControl = redstoneControl;
        setChanged();
    }

    public int getEchoing() {
        return echoing;
    }

    public void setEchoing(int echoing) {
        this.echoing = echoing;
        setChanged();
    }

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
        recountEggInputs();
        outputCacheDirty = true;
        refreshOutputCacheIfNeeded();
        belowHandlerCacheExpiryTick = Long.MIN_VALUE;
        cachedBelowHandler = null;
    }

    private static int calculateEnergyLevel(int energy, int maxEnergy) {
        if (energy == 0) return 1;
        if (energy >= maxEnergy * 0.66) return 4;
        if (energy >= maxEnergy * 0.33) return 3;
        return 2;
    }
}
