package com.chuan.apothicenchantingaddition.menu;

import com.chuan.apothicenchantingaddition.block.entity.FluxEnchantingTableBlockEntity;
import com.chuan.apothicenchantingaddition.config.ApothicAdditionConfig;
import com.chuan.apothicenchantingaddition.registry.ModRegistry;
import dev.shadowsoffire.apothic_enchanting.table.ApothEnchantmentHelper;
import dev.shadowsoffire.apothic_enchanting.table.EnchantmentTableStats;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.List;

public class FluxEnchantingMenu extends AbstractContainerMenu {

    private final FluxEnchantingTableBlockEntity blockEntity;
    private final ContainerLevelAccess levelAccess;
    private final RandomSource random = RandomSource.create();
    private final DataSlot enchantmentSeed = DataSlot.standalone();

    public final int[] costs = new int[3];
    public final int[] enchantClue = new int[]{-1, -1, -1};
    public final int[] levelClue = new int[]{-1, -1, -1};

    private final DataSlot energyUpper = DataSlot.standalone();
    private final DataSlot energyLower = DataSlot.standalone();

    // 【新增】：同步神化三大属性
    private final DataSlot eternaSlot = DataSlot.standalone();
    private final DataSlot quantaSlot = DataSlot.standalone();
    private final DataSlot arcanaSlot = DataSlot.standalone();

    public FluxEnchantingMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, (FluxEnchantingTableBlockEntity) playerInventory.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public FluxEnchantingMenu(int containerId, Inventory playerInventory, FluxEnchantingTableBlockEntity entity) {
        super(ModRegistry.FLUX_ENCHANTING_MENU.get(), containerId);
        this.blockEntity = entity;
        this.levelAccess = ContainerLevelAccess.create(entity.getLevel(), entity.getBlockPos());

        this.addSlot(new SlotItemHandler(entity.inventory, 0, 15, 47) {
            @Override
            public void setChanged() {
                super.setChanged();
                slotsChanged(new SimpleContainer(0));
            }
        });
        this.addSlot(new SlotItemHandler(entity.inventory, 1, 35, 47));

        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
        for (int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(playerInventory, k, 8 + k * 18, 142));
        }

        this.addDataSlot(enchantmentSeed).set(playerInventory.player.getEnchantmentSeed());
        this.addDataSlot(DataSlot.shared(costs, 0));
        this.addDataSlot(DataSlot.shared(costs, 1));
        this.addDataSlot(DataSlot.shared(costs, 2));
        this.addDataSlot(DataSlot.shared(enchantClue, 0));
        this.addDataSlot(DataSlot.shared(enchantClue, 1));
        this.addDataSlot(DataSlot.shared(enchantClue, 2));
        this.addDataSlot(DataSlot.shared(levelClue, 0));
        this.addDataSlot(DataSlot.shared(levelClue, 1));
        this.addDataSlot(DataSlot.shared(levelClue, 2));
        this.addDataSlot(energyUpper);
        this.addDataSlot(energyLower);

        // 添加属性同步槽
        this.addDataSlot(eternaSlot);
        this.addDataSlot(quantaSlot);
        this.addDataSlot(arcanaSlot);
    }

    @Override
    public void slotsChanged(Container inventory) {
        if (blockEntity.getLevel().isClientSide) return;
        ItemStack stack = blockEntity.inventory.getStackInSlot(0);

        EnchantmentTableStats stats = EnchantmentTableStats.gatherStats(blockEntity.getLevel(), blockEntity.getBlockPos(), stack.isEmpty() ? 0 : stack.getEnchantmentValue());
        // 保存属性以便推送到客户端 GUI
        eternaSlot.set(Float.floatToIntBits(stats.eterna()));
        quantaSlot.set(Float.floatToIntBits(stats.quanta()));
        arcanaSlot.set(Float.floatToIntBits(stats.arcana()));

        if (!stack.isEmpty() && stack.isEnchantable()) {
            this.random.setSeed(this.enchantmentSeed.get());
            for (int i = 0; i < 3; ++i) {
                this.costs[i] = ApothEnchantmentHelper.getEnchantmentCost(random, i, stats.eterna(), stack);
                this.enchantClue[i] = -1;
                this.levelClue[i] = -1;

                if (this.costs[i] > 0) {
                    net.minecraft.core.Registry<Enchantment> enchRegistry = blockEntity.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
                    List<EnchantmentInstance> list = ApothEnchantmentHelper.selectEnchantment(random, stack, this.costs[i], stats, enchRegistry.asLookup());
                    if (list != null && !list.isEmpty()) {
                        EnchantmentInstance instance = list.get(0);
                        this.enchantClue[i] = enchRegistry.getId(instance.enchantment.value());
                        this.levelClue[i] = instance.level;
                    }
                }
            }
        } else {
            for (int i = 0; i < 3; ++i) {
                this.costs[i] = 0;
                this.enchantClue[i] = -1;
                this.levelClue[i] = -1;
            }
        }
        this.broadcastChanges();
    }

    public void handleAction(Player player, int actionId) {
        if (blockEntity.getLevel().isClientSide) return;

        if (actionId == 0) {
            ItemStack refreshItem = blockEntity.inventory.getStackInSlot(1);
            String configItemStr = ApothicAdditionConfig.FLUX_ENCHANTER_REFRESH_ITEM.get();
            int requiredCount = ApothicAdditionConfig.FLUX_ENCHANTER_REFRESH_COUNT.get();
            Item requiredItem = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.parse(configItemStr));

            if (!refreshItem.isEmpty() && refreshItem.is(requiredItem) && refreshItem.getCount() >= requiredCount) {
                refreshItem.shrink(requiredCount);
                player.onEnchantmentPerformed(ItemStack.EMPTY, 0);
                this.enchantmentSeed.set(player.getEnchantmentSeed());
                this.slotsChanged(new SimpleContainer(0));
                blockEntity.getLevel().playSound(null, blockEntity.getBlockPos(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 1.0F, 1.0F);
            }
        } else if (actionId >= 1 && actionId <= 3) {
            int slot = actionId - 1;
            ItemStack stack = blockEntity.inventory.getStackInSlot(0);
            int costLevel = this.costs[slot];
            int feCost = costLevel * ApothicAdditionConfig.FLUX_ENCHANTER_BASE_COST.get();

            if (costLevel > 0 && !stack.isEmpty() && blockEntity.energyStorage.getEnergyStored() >= feCost) {
                EnchantmentTableStats stats = EnchantmentTableStats.gatherStats(blockEntity.getLevel(), blockEntity.getBlockPos(), stack.getEnchantmentValue());
                this.random.setSeed(this.enchantmentSeed.get());
                net.minecraft.core.Registry<Enchantment> enchRegistry = blockEntity.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
                List<EnchantmentInstance> list = ApothEnchantmentHelper.selectEnchantment(random, stack, costLevel, stats, enchRegistry.asLookup());

                if (list != null && !list.isEmpty()) {
                    blockEntity.energyStorage.extractEnergy(feCost, false);
                    if (stack.is(net.minecraft.world.item.Items.BOOK)) {
                        stack = new ItemStack(net.minecraft.world.item.Items.ENCHANTED_BOOK);
                        blockEntity.inventory.setStackInSlot(0, stack);
                    }
                    for (EnchantmentInstance instance : list) {
                        stack.enchant(instance.enchantment, instance.level);
                    }
                    player.onEnchantmentPerformed(stack, costLevel);
                    this.enchantmentSeed.set(player.getEnchantmentSeed());
                    this.slotsChanged(new SimpleContainer(0));
                    blockEntity.getLevel().playSound(null, blockEntity.getBlockPos(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, player.getRandom().nextFloat() * 0.1F + 0.9F);
                }
            }
        }
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        int energy = blockEntity.energyStorage.getEnergyStored();
        energyUpper.set(energy >> 16);
        energyLower.set(energy & 0xFFFF);

        // 【智能刷新】：每秒(20t)自动检测一次周围书架是否变动
        if (!blockEntity.getLevel().isClientSide && blockEntity.getLevel().getGameTime() % 20 == 0) {
            ItemStack stack = blockEntity.inventory.getStackInSlot(0);
            EnchantmentTableStats currentStats = EnchantmentTableStats.gatherStats(blockEntity.getLevel(), blockEntity.getBlockPos(), stack.isEmpty() ? 0 : stack.getEnchantmentValue());
            // 检查属性是否发生改变，变了就自动刷新选项
            if (Float.floatToIntBits(currentStats.eterna()) != eternaSlot.get() ||
                    Float.floatToIntBits(currentStats.quanta()) != quantaSlot.get() ||
                    Float.floatToIntBits(currentStats.arcana()) != arcanaSlot.get()) {
                this.slotsChanged(new SimpleContainer(0));
            }
        }
    }

    public int getEnergy() { return (energyUpper.get() << 16) | (energyLower.get() & 0xFFFF); }
    public float getEterna() { return Float.intBitsToFloat(eternaSlot.get()); }
    public float getQuanta() { return Float.intBitsToFloat(quantaSlot.get()); }
    public float getArcana() { return Float.intBitsToFloat(arcanaSlot.get()); }
    public FluxEnchantingTableBlockEntity getBlockEntity() { return this.blockEntity; }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(levelAccess, player, ModRegistry.FLUX_ENCHANTING_TABLE.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();

            // 如果点击的是机器槽位 (0:附魔槽, 1:刷新材料槽) -> 将物品丢回玩家背包 (槽位 2 到 37)
            if (index == 0 || index == 1) {
                if (!this.moveItemStackTo(itemstack1, 2, 38, true)) {
                    return ItemStack.EMPTY;
                }
            }
            // 如果点击的是玩家背包或快捷栏 -> 将物品放入机器
            else {
                // 动态获取配置文件中定义的刷新物品（默认应该是青金石）
                String configItemStr = ApothicAdditionConfig.FLUX_ENCHANTER_REFRESH_ITEM.get();
                Item requiredItem = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.parse(configItemStr));

                // 规则 1：如果是刷新物品，优先扔进槽位 1 (索引范围 1~2)
                if (itemstack1.is(requiredItem)) {
                    if (!this.moveItemStackTo(itemstack1, 1, 2, false)) {
                        return ItemStack.EMPTY;
                    }
                }
                // 规则 2：其他所有装备、武器、书本，都扔进槽位 0 (索引范围 0~1)
                else {
                    if (!this.moveItemStackTo(itemstack1, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                }

                // 兜底规则：如果机器槽位满了，或者放不进去，就在玩家的主背包和快捷栏之间互相转移
                if (itemstack1.getCount() == itemstack.getCount()) {
                    if (index >= 2 && index < 29) { // 玩家主背包 -> 快捷栏
                        if (!this.moveItemStackTo(itemstack1, 29, 38, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (index >= 29 && index < 38) { // 快捷栏 -> 玩家主背包
                        if (!this.moveItemStackTo(itemstack1, 2, 29, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                }
            }

            // 更新槽位状态
            if (itemstack1.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, itemstack1);
        }

        return itemstack;
    }
}
