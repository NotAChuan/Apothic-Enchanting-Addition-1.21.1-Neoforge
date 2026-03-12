package com.chuan.apothicenchantingaddition.block.entity;

import com.chuan.apothicenchantingaddition.block.FluxSpawnerBlock;
import com.chuan.apothicenchantingaddition.config.ApothicAdditionConfig;
import com.chuan.apothicenchantingaddition.menu.FluxSpawnerMenu;
import com.chuan.apothicenchantingaddition.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
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
import net.minecraft.core.registries.Registries;
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

    private static final TagKey<EntityType<?>> APOTHIC_SPAWNER_BLACKLIST = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("apothic_spawners", "blacklisted_from_spawners"));

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
            if (slot < 8) {
                if (!(stack.getItem() instanceof SpawnEggItem egg)) return false;
                return canUseSpawnEgg(egg);
            }
            return true;
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot < 8 ? 1 : 64;
        }

        @Override
        protected void onContentsChanged(int slot) {
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

        // 【修复】：能量等级检测独立出来，不受其他条件影响
        if (level.getGameTime() % 20 == 0) {
            int currentEnergy = entity.energyStorage.getEnergyStored();
            if (currentEnergy != entity.lastEnergy) {
                entity.lastEnergy = currentEnergy;
                entity.setChanged();

                int newEnergyLevel = calculateEnergyLevel(currentEnergy, entity.energyStorage.getMaxEnergyStored());
                BlockState currentState = level.getBlockState(pos);
                if (currentState.hasProperty(FluxSpawnerBlock.ENERGY_LEVEL) &&
                        currentState.getValue(FluxSpawnerBlock.ENERGY_LEVEL) != newEnergyLevel) {
                    level.setBlock(pos, currentState.setValue(FluxSpawnerBlock.ENERGY_LEVEL, newEnergyLevel), 3);
                }
            }
        }

        // 【极致优化 1】降低自动输出频率
        if (level.getGameTime() % 5 == 0) {
            entity.autoOutputToBelow();
        }

        // 统计刷怪蛋
        int eggCount = 0;
        for (int i = 0; i < 8; i++) {
            if (!entity.inventory.getStackInSlot(i).isEmpty()) {
                eggCount++;
            }
        }
        if (eggCount == 0) return;

        if (entity.redstoneControl && !level.hasNeighborSignal(pos)) return;
        if (entity.isOutputFull()) return;

        // 计算耗电
        int baseCost = ApothicAdditionConfig.FLUX_SPAWNER_ENERGY_PER_EGG.get();
        int energyCost = (baseCost * eggCount)
                + (baseCost * (800 / Math.max(1, entity.maxDelay)))
                + (baseCost * entity.spawnCount)
                + (baseCost * entity.echoing * 2);

        if (entity.energyStorage.getEnergyStored() < energyCost) return;

        entity.energyStorage.extractEnergy(energyCost, false);

        entity.delay--;
        if (entity.delay <= 0) {
            entity.generateLoot((ServerLevel) level);
            int range = entity.maxDelay - entity.minDelay;
            entity.delay = entity.minDelay + (range > 0 ? level.random.nextInt(range) : 0);
            entity.setChanged();
        }
    }


    private void generateLoot(ServerLevel serverLevel) {
        FakePlayer fakePlayer = FakePlayerFactory.getMinecraft(serverLevel);
        DamageSource damageSource = serverLevel.damageSources().playerAttack(fakePlayer);
        IItemHandler belowHandler = getBelowHandler();

        int eggCount = 0; // 用于计算经验

        // 遍历前 8 个输入槽
        for (int i = 0; i < 8; i++) {
            ItemStack stack = this.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof SpawnEggItem egg) {
                if (!canUseSpawnEgg(egg)) {
                    continue;
                }

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
                        insertLootToOutputs(lootTable.getRandomItems(params), belowHandler);
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
            insertLootToOutputs(expDrops, belowHandler);
        }
    }

    private void insertLootToOutputs(List<ItemStack> drops, @Nullable IItemHandler belowHandler) {
        for (ItemStack drop : drops) {
            ItemStack remainder = drop.copy();

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
        if (this.level == null) return null;
        return this.level.getCapability(Capabilities.ItemHandler.BLOCK, this.worldPosition.below(), Direction.UP);
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

//        EntityType<?> entityType = egg.getType(ItemStack.EMPTY);
//        Holder.Reference<EntityType<?>> holder = entityType.builtInRegistryHolder();
//        Holder.Reference<EntityType<?>> holder = entityType.;
//        return !holder.is(APOTHIC_SPAWNER_BLACKLIST);

        EntityType<?> entityType = egg.getType(ItemStack.EMPTY);
        Holder<EntityType<?>> holder = BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(entityType);
        return !holder.is(APOTHIC_SPAWNER_BLACKLIST);
    }

    private void autoOutputToBelow() {
        IItemHandler belowHandler = getBelowHandler();
        if (belowHandler == null) return;

        for (int i = 8; i < 72; i++) {
            ItemStack stackInSlot = this.inventory.getStackInSlot(i);
            if (!stackInSlot.isEmpty()) {
                ItemStack remainder = insertIntoHandler(belowHandler, stackInSlot.copy());
                this.inventory.setStackInSlot(i, remainder);
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
    }

    private static int calculateEnergyLevel(int energy, int maxEnergy) {
        if (energy == 0) return 1;
        if (energy >= maxEnergy * 0.66) return 4;
        if (energy >= maxEnergy * 0.33) return 3;
        return 2;
    }
}
