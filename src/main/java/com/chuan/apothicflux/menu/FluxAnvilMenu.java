package com.chuan.apothicflux.menu;

import com.chuan.apothicflux.block.entity.FluxAnvilBlockEntity;
import com.chuan.apothicflux.config.ApothicAdditionConfig;
import com.chuan.apothicflux.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class FluxAnvilMenu extends AnvilMenu {

    private final FluxAnvilBlockEntity blockEntity;

    // 高低位拆分同步大数值电量
    private final DataSlot energyHigh = DataSlot.standalone();
    private final DataSlot energyLow = DataSlot.standalone();

    // 客户端构造函数
    public FluxAnvilMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInventory, extraData.readBlockPos());
    }

    // 服务端与通用构造函数
    public FluxAnvilMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        super(containerId, playerInventory, ContainerLevelAccess.create(playerInventory.player.level(), pos));

        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        if (be instanceof FluxAnvilBlockEntity fluxBe) {
            this.blockEntity = fluxBe;
        } else {
            this.blockEntity = null;
        }

        // 注册数据槽
        this.addDataSlot(energyHigh);
        this.addDataSlot(energyLow);
    }

    @Override
    public MenuType<?> getType() {
        return ModRegistry.FLUX_ANVIL_MENU.get();
    }

    // 修复 Bug 3 & 2：加上 !isClientSide() 锁，严禁客户端用 0 电量覆盖数据槽！
    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (this.blockEntity != null && !this.blockEntity.getLevel().isClientSide()) {
            int energy = this.blockEntity.getEnergyStorage().getEnergyStored();
            this.energyHigh.set(energy >> 16);
            this.energyLow.set(energy & 0xFFFF);
        }
    }

    public int getEnergy() {
        return (this.energyHigh.get() << 16) | (this.energyLow.get() & 0xFFFF);
    }

    public int getMaxEnergy() {
        return FluxAnvilBlockEntity.MAX_ENERGY;
    }

    // 修复 Bug 4：丢弃反射，直接使用原版的 public getCost() 方法
    public int getFeCost() {
        return this.getCost() * ApothicAdditionConfig.FLUX_ANVIL_BASE_COST.get();
    }

    @Override
    protected boolean isValidBlock(BlockState state) {
        return state.is(ModRegistry.FLUX_ANVIL.get());
    }

    // 覆盖输出槽的拿取条件
    @Override
    protected boolean mayPickup(Player player, boolean hasItem) {
        return (player.getAbilities().instabuild || getEnergy() >= getFeCost()) && this.getCost() > 0;
    }

    // 修复 Bug 5 (刷物品) & 拦截神化经验扣除：使用“快照还原法”
    @Override
    protected void onTake(Player player, ItemStack stack) {
        // 0. 在物品被消耗前，提前记录下当前的真实耗电量！
        int actualEnergyCost = getFeCost();

        // 1. 记录玩家当前的经验值快照 (等级、进度、总经验)
        int oldLevel = player.experienceLevel;
        float oldProgress = player.experienceProgress;
        int oldTotal = player.totalExperience;

        // 2. 调用原版逻辑，让它去完美处理物品堆叠扣除和耐久消耗
        // （注意：这一步执行完后，原版的 cost 会瞬间变成 0！）
        super.onTake(player, stack);

        // 3. 瞬间还原玩家的经验值！原版和神化刚才扣掉的经验，全部像时光倒流一样还回来
        player.experienceLevel = oldLevel;
        player.experienceProgress = oldProgress;
        player.totalExperience = oldTotal;

        // 4. 扣除机器真正的 FE 电量（使用我们在第 0 步缓存的 actualEnergyCost！）
        if (!player.getAbilities().instabuild && this.blockEntity != null && !player.level().isClientSide()) {
            this.blockEntity.getEnergyStorage().extractEnergy(actualEnergyCost, false);
            // 标记方块实体数据已改变，确保电量能正确保存和同步
            this.blockEntity.setChanged();
        }
    }
}
