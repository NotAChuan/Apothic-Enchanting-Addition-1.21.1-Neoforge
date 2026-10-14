package com.chuan.apothicflux.block.entity;

import com.chuan.apothicflux.block.FluxInfuserBlock;
import com.chuan.apothicflux.config.ApothicAdditionConfig;
import com.chuan.apothicflux.menu.FluxInfuserMenu;
import com.chuan.apothicflux.registry.ModRegistry;
import com.chuan.apothicflux.util.FluxInfuserCandidates;
import com.chuan.apothicflux.util.FluxInfuserRecords;
import dev.shadowsoffire.apothic_enchanting.table.EnchantmentTableStats;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.RangedWrapper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 自动通量灌注器。
 * <p>
 * 读取当前游戏内所有 {@code apothic_enchanting:infusion} 配方；中间输入槽放入可灌注物品后，
 * 由 GUI 左侧列出「属性满足上下限」的全部候选结果，玩家点选其一后机器循环灌注，
 * 每次消耗 1 个输入物并产出该配方的产物，产物进入右侧 12 个输出槽。
 * <p>
 * 记录表（{@link FluxInfuserRecords}）按输入物分别记住选中的配方，
 * 因此一台机器可以同时记住多条不同输入物的配方。
 */
public class FluxInfuserBlockEntity extends BlockEntity implements MenuProvider {

    /** GUI 左侧候选展示槽数量（3 列 × 4 行）。 */
    public static final int CANDIDATE_COUNT = 12;
    /** 中间输入槽在物品栏中的下标。 */
    public static final int INPUT_SLOT = 0;
    /** 右侧输出槽起始下标。 */
    public static final int OUTPUT_START = 1;
    /** 右侧输出槽数量。 */
    public static final int OUTPUT_COUNT = 12;
    /** 右侧输出槽结束下标（不含）。 */
    public static final int OUTPUT_END = OUTPUT_START + OUTPUT_COUNT;
    /** 方块实体内部物品栏总槽数。 */
    public static final int CONTAINER_SIZE = OUTPUT_END;

    // ===== 同步给 GUI 的 ContainerData 索引 =====
    public static final int DATA_PROGRESS = 0;
    public static final int DATA_ENERGY_HIGH = 1;
    public static final int DATA_ENERGY_LOW = 2;
    public static final int DATA_ETERNA = 3;
    public static final int DATA_QUANTA = 4;
    public static final int DATA_ARCANA = 5;
    public static final int DATA_RECORD_COUNT = 6;
    public static final int DATA_COUNT = 7;

    private static final int STAT_REFRESH_INTERVAL = 20;
    private static final int BELOW_HANDLER_CACHE_TICKS = 5;

    private static final String KEY_ENERGY = "Energy";
    private static final String KEY_INVENTORY = "Inventory";
    private static final String KEY_PROGRESS = "Progress";

    /** 玩家选择候选的结果。 */
    public enum SelectResult {
        /** 选择成功（新增记录或改选已有记录）。 */
        OK,
        /** 该配方当前不是合法候选（输入不匹配 / 属性不满足 / 配方不存在）。 */
        INVALID,
        /** 记录表已满且当前输入物没有记录，无法新增。 */
        LIMIT_REACHED
    }

    private final EnergyStorage energyStorage;
    private final List<FluxInfuserRecords.Record> records = new ArrayList<>();

    /** 当前单次灌注进度（tick）。 */
    private int progress = 0;

    /** 让首 tick 立即采集一次属性，避免放置后空转 1 秒。 */
    private int statRefreshTimer = STAT_REFRESH_INTERVAL;
    private int lastEnergy = -1;

    /** 服务端采集到的附魔属性（位阶 / 量子化 / 阿卡那）。 */
    private float eterna = 0.0F;
    private float quanta = 0.0F;
    private float arcana = 0.0F;

    private long belowHandlerCacheExpiryTick = Long.MIN_VALUE;
    private @Nullable IItemHandler cachedBelowHandler = null;

    /** 槽位 0 = 输入；槽位 1..12 = 输出。 */
    public final ItemStackHandler inventory = new ItemStackHandler(CONTAINER_SIZE) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == INPUT_SLOT) {
                return isInfusableInput(stack);
            }
            // 输出槽只出不进：
            // GUI 的 SlotItemHandler.mayPlace、以及任何走 insertItem 的路径都会被这里挡掉。
            // 机器自己放产物走 insertIntoOutputs（用 setStackInSlot，绕过这个检查）。
            return false;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }
    };

    /** {@link #automationItemHandler} 里的相对下标：视图从输入槽开始，所以输入槽是 0。 */
    private static final int VIEW_INPUT_SLOT = 0;

    /**
     * 暴露给外部自动化（漏斗/管道）的物品能力。
     * <p>
     * 注册时不分面，所以**六个面行为完全一致**：
     * <ul>
     *   <li>输入槽（视图下标 0）：<b>只进不出</b>；能放入的条件由 {@code inventory.isItemValid} 决定，
     *       也就是「该物品至少能匹配一条灌注配方」。</li>
     *   <li>输出槽（视图下标 1..12）：<b>只出不进</b>。</li>
     * </ul>
     * 视图覆盖输入槽只是为了允许插入；抽取输入槽被下面重写的 {@code extractItem} 挡掉。
     */
    public final IItemHandler automationItemHandler = new RangedWrapper(inventory, INPUT_SLOT, OUTPUT_END) {
        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot != VIEW_INPUT_SLOT) {
                return stack; // 输出槽不收
            }
            return super.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot == VIEW_INPUT_SLOT) {
                return ItemStack.EMPTY; // 输入槽不给抽
            }
            return super.extractItem(slot, amount, simulate);
        }
    };

    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_PROGRESS -> progress;
                case DATA_ENERGY_HIGH -> energyStorage.getEnergyStored() >> 16;
                case DATA_ENERGY_LOW -> energyStorage.getEnergyStored() & 0xFFFF;
                case DATA_ETERNA -> Float.floatToIntBits(eterna);
                case DATA_QUANTA -> Float.floatToIntBits(quanta);
                case DATA_ARCANA -> Float.floatToIntBits(arcana);
                case DATA_RECORD_COUNT -> records.size();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // 全部为只读展示数据
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public FluxInfuserBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.FLUX_INFUSER_BE.get(), pos, state);
        // 储电上限在构造时读取配置：运行中修改配置需要重载世界（或重新放置机器）才会生效。
        this.energyStorage = new EnergyStorage(Math.max(0, ApothicAdditionConfig.FLUX_INFUSER_CAPACITY.get()),
                Integer.MAX_VALUE, Integer.MAX_VALUE) {
            @Override
            public int receiveEnergy(int maxReceive, boolean simulate) {
                int received = super.receiveEnergy(maxReceive, simulate);
                if (received > 0 && !simulate) {
                    setChanged();
                }
                return received;
            }

            @Override
            public int extractEnergy(int maxExtract, boolean simulate) {
                int extracted = super.extractEnergy(maxExtract, simulate);
                if (extracted > 0 && !simulate) {
                    setChanged();
                }
                return extracted;
            }
        };
    }

    public EnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    public List<FluxInfuserRecords.Record> records() {
        return records;
    }

    public int getProgress() {
        return progress;
    }

    public int getCraftTime() {
        return Math.max(1, ApothicAdditionConfig.FLUX_INFUSER_CRAFT_TIME.get());
    }

    public float getEterna() {
        return eterna;
    }

    public float getQuanta() {
        return quanta;
    }

    public float getArcana() {
        return arcana;
    }

    /** 输入槽过滤：只要该物品至少能匹配一条灌注配方即可放入（与记录表是否已满无关）。 */
    public boolean isInfusableInput(ItemStack stack) {
        if (stack.isEmpty() || this.level == null) {
            return false;
        }
        return FluxInfuserCandidates.hasAnyRecipe(this.level, stack);
    }

    /**
     * 玩家选择某个候选配方。服务端会完整重算候选做校验，不信任客户端传来的 ID。
     */
    public SelectResult selectCandidate(ResourceLocation recipeId) {
        if (this.level == null) {
            return SelectResult.INVALID;
        }
        ItemStack input = inventory.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty()) {
            return SelectResult.INVALID;
        }

        // 用最新属性做校验，避免使用最多 1 秒前的快照
        refreshStats(this.level);

        if (FluxInfuserCandidates.resolveById(this.level, input, eterna, quanta, arcana, recipeId) == null) {
            return SelectResult.INVALID;
        }

        FluxInfuserRecords.Record existing = FluxInfuserRecords.find(records, input);
        if (existing != null) {
            // 已有记录：改选只是覆盖，不占用新名额
            if (existing.select(recipeId)) {
                progress = 0;
                sync();
            }
            return SelectResult.OK;
        }

        if (records.size() >= FluxInfuserRecords.MAX_RECORDS) {
            return SelectResult.LIMIT_REACHED;
        }

        records.add(new FluxInfuserRecords.Record(input, recipeId, false));
        progress = 0;
        sync();
        return SelectResult.OK;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FluxInfuserBlockEntity entity) {
        if (level.isClientSide) {
            return;
        }
        entity.serverTick(level, pos, state);
    }

    private void serverTick(Level level, BlockPos pos, BlockState state) {
        clampEnergy();

        if (++statRefreshTimer >= STAT_REFRESH_INTERVAL) {
            statRefreshTimer = 0;
            refreshStats(level);
            updateEnergyLevel(level, pos, state);
        }

        if (ApothicAdditionConfig.FLUX_INFUSER_AUTO_OUTPUT.get()
                && level.getGameTime() % BELOW_HANDLER_CACHE_TICKS == 0) {
            autoOutputToBelow();
        }

        ItemStack input = inventory.getStackInSlot(INPUT_SLOT);
        FluxInfuserRecords.Record record = FluxInfuserRecords.find(records, input);

        FluxInfuserCandidates.Candidate active = record == null ? null
                : FluxInfuserCandidates.resolveById(level, input, eterna, quanta, arcana, record.recipeId());

        if (input.isEmpty() || record == null || active == null) {
            // 中止：输入为空 / 尚未选择 / 已选配方不再满足属性上下限 → 直接清除进度
            if (progress != 0) {
                progress = 0;
                sync();
            }
            return;
        }

        int craftTime = getCraftTime();
        int cost = Math.max(0, ApothicAdditionConfig.FLUX_INFUSER_ENERGY_PER_TICK.get());
        boolean energyEnabled = ApothicAdditionConfig.FLUX_INFUSER_ENERGY_ENABLED.get();

        // 电量不足 → 暂停并保留进度（电量属于外部供给问题，与属性不足的中止语义不同）
        if (energyEnabled && cost > 0 && energyStorage.getEnergyStored() < cost) {
            return;
        }

        // 完成前一 tick 预检输出空间；放不下 → 暂停并保留进度（不扣电）
        if (progress + 1 >= craftTime && !canFitOutput(active)) {
            return;
        }

        if (energyEnabled && cost > 0) {
            energyStorage.extractEnergy(cost, false);
        }

        progress++;
        if (progress >= craftTime) {
            produce(level, pos, input, active, record);
            progress = 0;
        }
        setChanged();
    }

    private void produce(Level level, BlockPos pos, ItemStack input, FluxInfuserCandidates.Candidate active,
                         FluxInfuserRecords.Record record) {
        ItemStack remainder = insertIntoOutputs(active.output().copy());
        if (!remainder.isEmpty()) {
            // 已在完成前预检过，这里只是兜底，避免产物凭空消失
            Block.popResource(level, pos.above(), remainder);
        }

        // 每次灌注恒定消耗 1 个输入物（神化灌注配方只有一个输入）
        inventory.extractItem(INPUT_SLOT, 1, false);

        // 取消发光，且此后保持取消；只有玩家改选其它配方才会重新发光
        record.markProduced();
        sync();
    }

    public void refreshStats(Level level) {
        EnchantmentTableStats stats = EnchantmentTableStats.gatherStats(level, this.worldPosition);
        this.eterna = stats.tableEterna();
        this.quanta = stats.quanta();
        this.arcana = stats.arcana();
    }

    private void updateEnergyLevel(Level level, BlockPos pos, BlockState state) {
        int current = energyStorage.getEnergyStored();
        if (current == lastEnergy) {
            return;
        }
        lastEnergy = current;
        setChanged();

        if (!state.hasProperty(FluxInfuserBlock.ENERGY_LEVEL)) {
            return;
        }
        int newLevel = calculateEnergyLevel(current, energyStorage.getMaxEnergyStored());
        if (state.getValue(FluxInfuserBlock.ENERGY_LEVEL) != newLevel) {
            level.setBlock(pos, state.setValue(FluxInfuserBlock.ENERGY_LEVEL, newLevel), 3);
        }
    }

    private static int calculateEnergyLevel(int energy, int maxEnergy) {
        if (maxEnergy <= 0 || energy <= 0) {
            return 0;
        }
        long level = ((long) energy * 4 + maxEnergy - 1) / maxEnergy;
        return (int) Math.min(4, Math.max(0, level));
    }

    /** 配置里把储电上限调小后，存档中的电量可能超过上限，这里兜底钳制。 */
    private void clampEnergy() {
        int capacity = energyStorage.getMaxEnergyStored();
        int stored = energyStorage.getEnergyStored();
        if (stored > capacity) {
            energyStorage.extractEnergy(stored - capacity, false);
        }
    }

    private boolean canFitOutput(FluxInfuserCandidates.Candidate active) {
        return insertIntoOutputs(active.output().copy(), true).isEmpty();
    }

    private ItemStack insertIntoOutputs(ItemStack stack) {
        return insertIntoOutputs(stack, false);
    }

    /**
     * 往输出槽放产物。
     * <p>
     * 这里**不能**用 {@code inventory.insertItem}：输出槽的 {@code isItemValid} 是 {@code false}
     * （用来禁止玩家和管道放东西进去），而产物是机器自己放的，所以这里自己合并堆叠。
     */
    private ItemStack insertIntoOutputs(ItemStack stack, boolean simulate) {
        ItemStack remainder = stack;
        for (int slot = OUTPUT_START; slot < OUTPUT_END && !remainder.isEmpty(); slot++) {
            ItemStack existing = inventory.getStackInSlot(slot);
            int limit = Math.min(inventory.getSlotLimit(slot), remainder.getMaxStackSize());

            if (existing.isEmpty()) {
                int move = Math.min(remainder.getCount(), limit);
                if (!simulate) {
                    inventory.setStackInSlot(slot, remainder.copyWithCount(move));
                }
                remainder = remainder.copyWithCount(remainder.getCount() - move);
            } else if (ItemStack.isSameItemSameComponents(existing, remainder)) {
                int room = limit - existing.getCount();
                if (room <= 0) {
                    continue;
                }
                int move = Math.min(room, remainder.getCount());
                if (!simulate) {
                    inventory.setStackInSlot(slot, existing.copyWithCount(existing.getCount() + move));
                }
                remainder = remainder.copyWithCount(remainder.getCount() - move);
            }
        }
        return remainder;
    }

    private void autoOutputToBelow() {
        IItemHandler belowHandler = getBelowHandler();
        if (belowHandler == null) {
            return;
        }

        for (int slot = OUTPUT_START; slot < OUTPUT_END; slot++) {
            ItemStack stackInSlot = inventory.getStackInSlot(slot);
            if (stackInSlot.isEmpty()) {
                continue;
            }

            ItemStack original = stackInSlot.copy();
            ItemStack remainder = insertIntoHandler(belowHandler, original.copy());
            if (!ItemStack.matches(original, remainder)) {
                inventory.setStackInSlot(slot, remainder);
            }
        }
    }

    private @Nullable IItemHandler getBelowHandler() {
        if (this.level == null) {
            return null;
        }
        long gameTime = this.level.getGameTime();
        if (gameTime >= this.belowHandlerCacheExpiryTick) {
            this.cachedBelowHandler = this.level.getCapability(Capabilities.ItemHandler.BLOCK,
                    this.worldPosition.below(), Direction.UP);
            this.belowHandlerCacheExpiryTick = gameTime + BELOW_HANDLER_CACHE_TICKS;
        }
        return this.cachedBelowHandler;
    }

    private ItemStack insertIntoHandler(IItemHandler handler, ItemStack stack) {
        ItemStack remainder = stack;
        for (int slot = 0; slot < handler.getSlots() && !remainder.isEmpty(); slot++) {
            remainder = handler.insertItem(slot, remainder, false);
        }
        return remainder;
    }

    private void sync() {
        setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.apothic_flux.flux_infuser");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new FluxInfuserMenu(containerId, playerInventory, this, this.dataAccess);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put(KEY_ENERGY, energyStorage.serializeNBT(registries));
        tag.put(KEY_INVENTORY, inventory.serializeNBT(registries));
        tag.putInt(KEY_PROGRESS, progress);
        tag.put(FluxInfuserRecords.listKey(), FluxInfuserRecords.save(records, registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(KEY_ENERGY)) {
            energyStorage.deserializeNBT(registries, tag.get(KEY_ENERGY));
        }
        if (tag.contains(KEY_INVENTORY)) {
            inventory.deserializeNBT(registries, tag.getCompound(KEY_INVENTORY));
        }
        progress = tag.getInt(KEY_PROGRESS);
        records.clear();
        records.addAll(FluxInfuserRecords.load(
                tag.getList(FluxInfuserRecords.listKey(), Tag.TAG_COMPOUND), registries));
        lastEnergy = energyStorage.getEnergyStored();
        clampEnergy();
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
