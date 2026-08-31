package com.chuan.apothicflux.block.entity;

import com.chuan.apothicflux.item.CompressedSolidifiedFluxExperienceItem;
import com.chuan.apothicflux.item.SolidifiedFluxExperienceItem;
import com.chuan.apothicflux.menu.FluxExpConverterMenu;
import com.chuan.apothicflux.network.FluxExpConverterActionPayload;
import com.chuan.apothicflux.registry.ModRegistry;
import com.chuan.apothicflux.util.ExperienceMath;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FluxExpConverterBlockEntity extends BlockEntity implements MenuProvider {
    public static final long MAX_STORED_XP = Long.MAX_VALUE;
    public static final int MB_PER_XP = 20;
    public static final int INPUT_SLOT = 0;

    private long storedXp = 0L;

    public final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot == INPUT_SLOT && isSupportedXpItem(stack);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) (storedXp >>> 32);
                case 1 -> (int) storedXp;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public FluxExpConverterBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModRegistry.FLUX_EXP_CONVERTER_BE.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FluxExpConverterBlockEntity entity) {
        if (level.isClientSide) {
            return;
        }
        entity.absorbInputItems();
    }

    private void absorbInputItems() {
        ItemStack stack = this.inventory.getStackInSlot(INPUT_SLOT);
        if (stack.isEmpty()) {
            return;
        }

        long totalGain = 0L;
        ItemStack remainder = stack;

        if (stack.getItem() instanceof SolidifiedFluxExperienceItem) {
            totalGain = stack.getCount() * 5L;
            remainder = ItemStack.EMPTY;
        } else if (stack.getItem() instanceof CompressedSolidifiedFluxExperienceItem) {
            totalGain = stack.getCount() * 45L;
            remainder = ItemStack.EMPTY;
        }

        if (totalGain > 0L) {
            this.storedXp = Math.min(MAX_STORED_XP, this.storedXp + totalGain);
            this.inventory.setStackInSlot(INPUT_SLOT, remainder);
            setChanged();
        }
    }

    public boolean insertItem(ItemStack stack) {
        if (!isSupportedXpItem(stack) || stack.isEmpty()) {
            return false;
        }
        ItemStack existing = this.inventory.getStackInSlot(INPUT_SLOT);
        if (!existing.isEmpty()) {
            return false;
        }
        this.inventory.setStackInSlot(INPUT_SLOT, stack.copy());
        return true;
    }

    public long getStoredXp() {
        return storedXp;
    }

    public IFluidHandler getFluidHandler() {
        return new ConverterFluidHandler();
    }

    public void setStoredXp(long storedXp) {
        this.storedXp = Math.max(0L, Math.min(MAX_STORED_XP, storedXp));
        setChanged();
    }

    public long takeXp(long amount) {
        long taken = Math.min(Math.max(0L, amount), this.storedXp);
        this.storedXp -= taken;
        if (taken > 0L) {
            setChanged();
        }
        return taken;
    }

    public void giveXp(long amount) {
        if (amount <= 0L) {
            return;
        }
        this.storedXp = Math.min(MAX_STORED_XP, this.storedXp + amount);
        setChanged();
    }

    public void handleAction(Player player, FluxExpConverterActionPayload.Action action) {
        if (this.level == null || this.level.isClientSide) {
            return;
        }

        boolean success = false;
        switch (action) {
            case STORE_1 -> success = storePlayerXp(player, 1);
            case STORE_10 -> success = storePlayerXp(player, 10);
            case STORE_ALL -> success = storePlayerXp(player, Integer.MAX_VALUE);
            case TAKE_1 -> success = takeToPlayer(player, 1);
            case TAKE_10 -> success = takeToPlayer(player, 10);
            case TAKE_ALL -> success = takeToPlayer(player, Integer.MAX_VALUE);
        }

        if (success) {
            playActionSound(action, player);
        }
    }

    private boolean storePlayerXp(Player player, int levels) {
        if (player.isCreative()) {
            return false;
        }

        // 1. 获取玩家真实经验
        long realXp = ExperienceMath.getRealExperience(player);
        if (realXp <= 0) {
            return false;
        }

        // 2. 计算请求存入的经验量（存入 N 级 = 把玩家降到 N 级前，进度归零，对齐 EnderIO removeLevelsFromPlayer）
        long requestedXp;
        if (levels == Integer.MAX_VALUE) {
            requestedXp = realXp; // 存入全部
        } else {
            requestedXp = ExperienceMath.xpNeededToLoseLevels(player, levels);
            if (requestedXp <= 0) {
                return false;
            }
        }

        // 3. 实际扣除量 = min(请求, 玩家当前经验)
        long actual = Math.min(requestedXp, realXp);
        if (actual <= 0) {
            return false;
        }

        // 4. 从玩家扣除（long 全链路，支持超过 int 上限的经验）
        long newXp = realXp - actual;
        ExperienceMath.setPlayerXp(player, newXp);

        // 5. 给机器增加经验
        this.giveXp(actual);
        return true;
    }

    private boolean takeToPlayer(Player player, int levels) {
        // 1. 获取玩家真实经验
        long realXp = ExperienceMath.getRealExperience(player);

        // 2. 计算请求取出的经验量
        long requestedXp;
        if (levels == Integer.MAX_VALUE) {
            requestedXp = this.storedXp;
        } else {
            requestedXp = ExperienceMath.xpNeededToGainLevels(player, levels);
            if (requestedXp <= 0) {
                return false;
            }
        }

        // 3. 实际取出量 = min(请求, 机器存量)
        long actual = Math.min(requestedXp, this.storedXp);
        if (actual <= 0) {
            return false;
        }

        // 4. 给玩家增加经验（long 全链路，支持超过 int 上限的经验）
        long newXp = (actual > Long.MAX_VALUE - realXp) ? Long.MAX_VALUE : realXp + actual;
        ExperienceMath.setPlayerXp(player, newXp);

        // 5. 从机器扣除
        this.storedXp -= actual;
        setChanged();
        return true;
    }

    private void playActionSound(FluxExpConverterActionPayload.Action action, Player player) {
        if (this.level == null) {
            return;
        }

        float volume;
        float pitch;
        switch (action) {
            case STORE_1 -> {
                volume = 0.6F;
                pitch = 1.8F;
            }
            case STORE_10 -> {
                volume = 1.0F;
                pitch = 1.5F;
            }
            case STORE_ALL -> {
                volume = 1.4F;
                pitch = 1.2F;
            }
            case TAKE_1 -> {
                volume = 0.6F;
                pitch = 0.6F;
            }
            case TAKE_10 -> {
                volume = 1.0F;
                pitch = 0.8F;
            }
            case TAKE_ALL -> {
                volume = 1.4F;
                pitch = 1.0F;
            }
            default -> {
                volume = 1.0F;
                pitch = 1.0F;
            }
        }

        this.level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, volume, pitch);
    }

    private long xpToMb(long xp) {
        if (xp <= 0L) {
            return 0L;
        }
        long maxXpWithoutOverflow = Integer.MAX_VALUE / MB_PER_XP;
        if (xp >= maxXpWithoutOverflow) {
            return Integer.MAX_VALUE;
        }
        return xp * MB_PER_XP;
    }

    public static boolean isSupportedXpItem(ItemStack stack) {
        return stack.getItem() instanceof SolidifiedFluxExperienceItem
                || stack.getItem() instanceof CompressedSolidifiedFluxExperienceItem;
    }

    public static long xpValueOf(ItemStack stack) {
        if (stack.getItem() instanceof SolidifiedFluxExperienceItem) {
            return 5L * stack.getCount();
        }
        if (stack.getItem() instanceof CompressedSolidifiedFluxExperienceItem) {
            return 45L * stack.getCount();
        }
        return 0L;
    }

    public int getDisplayLevel() {
        return ExperienceMath.levelForXp(this.storedXp);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("gui.apothic_flux.exp_converter");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new FluxExpConverterMenu(containerId, inventory, this, this.dataAccess);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("StoredXp", this.storedXp);
        tag.put("Inventory", this.inventory.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.storedXp = Math.max(0L, tag.contains("StoredXp") ? tag.getLong("StoredXp") : 0L);
        if (tag.contains("Inventory")) {
            this.inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level != null && this.level.isClientSide) {
            return;
        }
        setChanged();
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

    @Override
    public void setRemoved() {
        super.setRemoved();
        setChanged();
    }

    private final class ConverterFluidHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public @NotNull FluidStack getFluidInTank(int tank) {
            if (tank != 0) {
                return FluidStack.EMPTY;
            }
            int amount = (int) xpToMb(FluxExpConverterBlockEntity.this.storedXp);
            return amount <= 0 ? FluidStack.EMPTY : new FluidStack(ModRegistry.EXPERIENCE_FLUID.get(), amount);
        }

        @Override
        public int getTankCapacity(int tank) {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
            return tank == 0 && !stack.isEmpty() && stack.getFluid() == ModRegistry.EXPERIENCE_FLUID.get();
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || resource.getFluid() != ModRegistry.EXPERIENCE_FLUID.get()) {
                return 0;
            }

            int accepted = resource.getAmount() - resource.getAmount() % MB_PER_XP;
            if (accepted <= 0) {
                return 0;
            }

            if (action.execute()) {
                FluxExpConverterBlockEntity.this.giveXp(accepted / MB_PER_XP);
            }

            return accepted;
        }

        @Override
        public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || resource.getFluid() != ModRegistry.EXPERIENCE_FLUID.get()) {
                return FluidStack.EMPTY;
            }
            return drain(resource.getAmount(), action);
        }

        @Override
        public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
            int availableMb = (int) xpToMb(FluxExpConverterBlockEntity.this.storedXp);
            int drained = Math.min(maxDrain, availableMb);
            drained -= drained % MB_PER_XP;
            if (drained <= 0) {
                return FluidStack.EMPTY;
            }

            if (action.execute()) {
                FluxExpConverterBlockEntity.this.takeXp(drained / MB_PER_XP);
            }

            return new FluidStack(ModRegistry.EXPERIENCE_FLUID.get(), drained);
        }
    }
}
