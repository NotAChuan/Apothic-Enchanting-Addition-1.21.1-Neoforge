package com.chuan.apothicflux.menu;

import com.chuan.apothicflux.block.entity.FluxInfuserBlockEntity;
import com.chuan.apothicflux.network.FluxInfuserSelectPayload;
import com.chuan.apothicflux.registry.ModRegistry;
import com.chuan.apothicflux.util.FluxInfuserCandidates;
import com.chuan.apothicflux.util.FluxInfuserRecords;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 自动通量灌注器的容器菜单。
 * <p>
 * 布局（GUI 176 × 186）：左侧 3×4 候选展示区（**不是真实槽位**，由 Screen 直接绘制并处理点击）·
 * 中间 1 个输入槽 · 右侧 3×4 输出槽 · 下方玩家背包。
 * <p>
 * 候选列表由**客户端本地计算**（配方两端一致、属性经 ContainerData 同步）；
 * 点击时把「配方 ID」发给服务端，服务端完整重算校验后才生效。
 */
public class FluxInfuserMenu extends AbstractContainerMenu {

    // ===== GUI 尺寸与布局（Screen 与 Menu 共用）=====
    public static final int GUI_WIDTH = 176;
    public static final int GUI_HEIGHT = 200;
    public static final int SLOT_STEP = 18;

    /** 左侧候选展示区：3 列 × 4 行 = 12。坐标为「物品」位置，边框画在 (x-1, y-1)。 */
    public static final int CANDIDATE_COLS = 3;
    public static final int CANDIDATE_ROWS = 4;
    public static final int CANDIDATE_X0 = 8;
    public static final int CANDIDATE_Y0 = 18;

    public static final int INPUT_X = 79;
    public static final int INPUT_Y = 45;

    /** 右侧输出区：3 列 × 4 行 = 12。 */
    public static final int OUTPUT_COLS = 3;
    public static final int OUTPUT_ROWS = 4;
    public static final int OUTPUT_X0 = 106;
    public static final int OUTPUT_Y0 = 18;

    public static final int PLAYER_INV_X = 8;
    public static final int PLAYER_INV_Y = 117;
    public static final int HOTBAR_Y = 175;

    // ===== 菜单槽位下标 =====
    /** 菜单槽位 0：中间输入槽（对应 BE 物品栏 0）。 */
    public static final int INPUT_SLOT_INDEX = 0;
    /** 菜单槽位 1..12：右侧输出槽（对应 BE 物品栏 1..12）。 */
    public static final int OUTPUT_SLOT_START = INPUT_SLOT_INDEX + 1;
    public static final int OUTPUT_SLOT_END = OUTPUT_SLOT_START + FluxInfuserBlockEntity.OUTPUT_COUNT;
    /** 菜单槽位 13..48：玩家背包（27 + 9）。 */
    public static final int PLAYER_SLOT_START = OUTPUT_SLOT_END;
    public static final int PLAYER_SLOT_END = PLAYER_SLOT_START + 36;

    private final FluxInfuserBlockEntity blockEntity;
    private final ContainerData data;

    /** 全部候选（跨页）。仅客户端填充。 */
    private final List<FluxInfuserCandidates.Candidate> allCandidates = new ArrayList<>();
    private int page = 0;

    /** 候选重算的脏检查快照。 */
    private ItemStack lastInput = ItemStack.EMPTY;
    private float lastEterna = Float.NaN;
    private float lastQuanta = Float.NaN;
    private float lastArcana = Float.NaN;
    private int lastRecordCount = -1;

    public FluxInfuserMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory,
                playerInventory.player.level().getBlockEntity(extraData.readBlockPos()),
                new SimpleContainerData(FluxInfuserBlockEntity.DATA_COUNT));
    }

    public FluxInfuserMenu(int containerId, Inventory playerInventory, BlockEntity entity, ContainerData data) {
        super(ModRegistry.FLUX_INFUSER_MENU.get(), containerId);
        this.blockEntity = (FluxInfuserBlockEntity) entity;
        this.data = data;
        this.addDataSlots(data);

        // 中间输入槽（能否放入由 FluxInfuserBlockEntity.inventory.isItemValid 决定）
        this.addSlot(new SlotItemHandler(this.blockEntity.inventory,
                FluxInfuserBlockEntity.INPUT_SLOT, INPUT_X, INPUT_Y));

        // 右侧 3×4 输出槽
        for (int row = 0; row < OUTPUT_ROWS; row++) {
            for (int col = 0; col < OUTPUT_COLS; col++) {
                int index = row * OUTPUT_COLS + col;
                this.addSlot(new SlotItemHandler(this.blockEntity.inventory,
                        FluxInfuserBlockEntity.OUTPUT_START + index,
                        OUTPUT_X0 + col * SLOT_STEP, OUTPUT_Y0 + row * SLOT_STEP));
            }
        }

        // 玩家背包
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        PLAYER_INV_X + col * SLOT_STEP, PLAYER_INV_Y + row * SLOT_STEP));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, PLAYER_INV_X + col * SLOT_STEP, HOTBAR_Y));
        }
    }

    // ===================== 同步数据读取 =====================

    public int getEnergy() {
        return (this.data.get(FluxInfuserBlockEntity.DATA_ENERGY_HIGH) << 16)
                | (this.data.get(FluxInfuserBlockEntity.DATA_ENERGY_LOW) & 0xFFFF);
    }

    public float getEterna() {
        return Float.intBitsToFloat(this.data.get(FluxInfuserBlockEntity.DATA_ETERNA));
    }

    public float getQuanta() {
        return Float.intBitsToFloat(this.data.get(FluxInfuserBlockEntity.DATA_QUANTA));
    }

    public float getArcana() {
        return Float.intBitsToFloat(this.data.get(FluxInfuserBlockEntity.DATA_ARCANA));
    }

    public int getProgress() {
        return this.data.get(FluxInfuserBlockEntity.DATA_PROGRESS);
    }

    public int getRecordCount() {
        return this.data.get(FluxInfuserBlockEntity.DATA_RECORD_COUNT);
    }

    public int getRecordLimit() {
        return FluxInfuserRecords.MAX_RECORDS;
    }

    public FluxInfuserBlockEntity getBlockEntity() {
        return blockEntity;
    }

    // ===================== 候选列表（客户端本地计算）=====================

    /** 由 Screen 每帧调用；只有脏检查命中时才真正重算。 */
    public void refreshCandidatesIfNeeded() {
        if (this.blockEntity.getLevel() == null || !this.blockEntity.getLevel().isClientSide) {
            return;
        }

        ItemStack input = inputStack();
        float eterna = getEterna();
        float quanta = getQuanta();
        float arcana = getArcana();
        int recordCount = this.blockEntity.records().size();

        if (ItemStack.matches(this.lastInput, input)
                && this.lastEterna == eterna && this.lastQuanta == quanta && this.lastArcana == arcana
                && this.lastRecordCount == recordCount) {
            return;
        }

        this.lastInput = input.copy();
        this.lastEterna = eterna;
        this.lastQuanta = quanta;
        this.lastArcana = arcana;
        this.lastRecordCount = recordCount;

        this.allCandidates.clear();
        if (!input.isEmpty()) {
            this.allCandidates.addAll(
                    FluxInfuserCandidates.resolve(this.blockEntity.getLevel(), input, eterna, quanta, arcana));
        }
        this.page = 0;
    }

    public int getPage() {
        return this.page;
    }

    public int getPageCount() {
        return Math.max(1, (this.allCandidates.size() + FluxInfuserBlockEntity.CANDIDATE_COUNT - 1)
                / FluxInfuserBlockEntity.CANDIDATE_COUNT);
    }

    public boolean hasMultiplePages() {
        return this.allCandidates.size() > FluxInfuserBlockEntity.CANDIDATE_COUNT;
    }

    public void changePage(int delta) {
        this.page = Mth.clamp(this.page + delta, 0, getPageCount() - 1);
    }

    public int getCandidateTotal() {
        return this.allCandidates.size();
    }

    /** 当前页第 slotIndex 个（0..11）候选；没有则返回 null。 */
    @Nullable
    public FluxInfuserCandidates.Candidate candidateAt(int slotIndex) {
        int index = this.page * FluxInfuserBlockEntity.CANDIDATE_COUNT + slotIndex;
        return index >= 0 && index < this.allCandidates.size() ? this.allCandidates.get(index) : null;
    }

    public ItemStack inputStack() {
        return this.blockEntity.inventory.getStackInSlot(FluxInfuserBlockEntity.INPUT_SLOT);
    }

    /** 当前输入物对应的记录；没有则为 null。 */
    @Nullable
    public FluxInfuserRecords.Record currentRecord() {
        return FluxInfuserRecords.find(this.blockEntity.records(), inputStack());
    }

    /** 某个候选槽是否应该画发光边框（= 它是当前选中项，且本次选择后还没产出过）。 */
    public boolean isGlowing(int slotIndex) {
        FluxInfuserCandidates.Candidate candidate = candidateAt(slotIndex);
        FluxInfuserRecords.Record record = currentRecord();
        return candidate != null && record != null
                && record.recipeId().equals(candidate.id()) && record.glow();
    }

    /**
     * 记录表已满、且当前输入物还没有记录时，候选槽置灰提示。
     * 注意：置灰**不影响点击**——点击仍会发到服务端，由服务端弹出上限提示。
     */
    public boolean isLockedByRecordLimit() {
        return getRecordCount() >= FluxInfuserRecords.MAX_RECORDS && currentRecord() == null;
    }

    // ===================== 选择下发（客户端 → 服务端）=====================

    /** 客户端点击候选槽时调用，把配方 ID 发给服务端。 */
    public void requestSelect(ResourceLocation recipeId) {
        PacketDistributor.sendToServer(
                new FluxInfuserSelectPayload(this.blockEntity.getBlockPos(), recipeId));
    }

    /** 服务端处理选择请求：完整重算校验，必要时给玩家发提示。 */
    public void handleSelect(Player player, ResourceLocation recipeId) {
        if (this.blockEntity.getLevel() == null || this.blockEntity.getLevel().isClientSide) {
            return;
        }
        FluxInfuserBlockEntity.SelectResult result = this.blockEntity.selectCandidate(recipeId);
        if (result == FluxInfuserBlockEntity.SelectResult.LIMIT_REACHED) {
            player.displayClientMessage(
                    Component.translatable("message.apothic_flux.flux_infuser.record_limit")
                            .withStyle(ChatFormatting.RED),
                    true);
        }
    }

    // ===================== AbstractContainerMenu =====================

    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(
                ContainerLevelAccess.create(this.blockEntity.getLevel(), this.blockEntity.getBlockPos()),
                player, ModRegistry.FLUX_INFUSER.get());
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stackInSlot = slot.getItem();
        ItemStack original = stackInSlot.copy();

        if (index < PLAYER_SLOT_START) {
            // 机器内部（输入槽 / 输出槽）→ 玩家背包
            if (!this.moveItemStackTo(stackInSlot, PLAYER_SLOT_START, PLAYER_SLOT_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (this.blockEntity.isInfusableInput(stackInSlot)) {
            // 可灌注物品 → 输入槽
            if (!this.moveItemStackTo(stackInSlot, INPUT_SLOT_INDEX, INPUT_SLOT_INDEX + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < PLAYER_SLOT_START + 27) {
            if (!this.moveItemStackTo(stackInSlot, PLAYER_SLOT_START + 27, PLAYER_SLOT_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(stackInSlot, PLAYER_SLOT_START, PLAYER_SLOT_START + 27, false)) {
            return ItemStack.EMPTY;
        }

        if (stackInSlot.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        if (stackInSlot.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }

        slot.onTake(player, stackInSlot);
        return original;
    }
}
