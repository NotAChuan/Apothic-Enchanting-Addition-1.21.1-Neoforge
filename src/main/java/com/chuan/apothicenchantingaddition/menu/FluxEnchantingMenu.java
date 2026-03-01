package com.chuan.apothicenchantingaddition.menu;

import com.chuan.apothicenchantingaddition.block.entity.FluxEnchantingTableBlockEntity;
import com.chuan.apothicenchantingaddition.config.ApothicAdditionConfig;
import com.chuan.apothicenchantingaddition.network.FluxCluePayload;
import com.chuan.apothicenchantingaddition.registry.ModRegistry;
import dev.shadowsoffire.apothic_enchanting.table.ApothEnchantmentMenu;
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
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class FluxEnchantingMenu extends AbstractContainerMenu {

    private final FluxEnchantingTableBlockEntity blockEntity;
    private final ContainerLevelAccess levelAccess;
    private final RandomSource random = RandomSource.create();
    private final DataSlot enchantmentSeed = DataSlot.standalone();
    private final Player player;

    public final int[] costs = new int[3];
    public final int[] enchantClue = new int[]{-1, -1, -1};
    public final int[] levelClue = new int[]{-1, -1, -1};

    private final DataSlot energyUpper = DataSlot.standalone();
    private final DataSlot energyLower = DataSlot.standalone();

    private final DataSlot eternaSlot = DataSlot.standalone();
    private final DataSlot quantaSlot = DataSlot.standalone();
    private final DataSlot arcanaSlot = DataSlot.standalone();

    public final List<EnchantmentInstance>[] clientClues = new List[]{List.of(), List.of(), List.of()};
    public final boolean[] clientAllRevealed = new boolean[3];

    // ==========================================
    // ✨ [调整] 严格的附魔资格计算（不拦截放入，只拦截附魔生成）
    // ==========================================
    public static boolean canEnchantItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.is(Items.ENCHANTED_BOOK)) return false; // 附魔书拒绝二次附魔

        // 如果物品的附魔价值 <= 0 且不是普通书，说明它是石头、泥土等绝对不可附魔的杂物
        if (stack.getEnchantmentValue() <= 0 && !stack.is(Items.BOOK)) {
            return false;
        }

        return stack.isEnchantable() || ApothEnchantmentMenu.isEnchantableEnough(stack);
    }

    public void setClues(int slot, List<EnchantmentInstance> clues, boolean allRevealed) {
        if (slot >= 0 && slot < 3) {
            this.clientClues[slot] = clues;
            this.clientAllRevealed[slot] = allRevealed;
        }
    }

    public FluxEnchantingMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, (FluxEnchantingTableBlockEntity) playerInventory.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public FluxEnchantingMenu(int containerId, Inventory playerInventory, FluxEnchantingTableBlockEntity entity) {
        super(ModRegistry.FLUX_ENCHANTING_MENU.get(), containerId);
        this.blockEntity = entity;
        this.player = playerInventory.player;
        this.levelAccess = ContainerLevelAccess.create(entity.getLevel(), entity.getBlockPos());

        // ✨ [调整] 机器槽位 0：不再限制放入，像原版一样允许塞入任何东西
        this.addSlot(new SlotItemHandler(entity.inventory, 0, 15, 47) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return true;
            }

            @Override
            public void setChanged() {
                super.setChanged();
                slotsChanged(new SimpleContainer(0));
            }
        });

        // 机器槽位 1：刷新材料槽
        this.addSlot(new SlotItemHandler(entity.inventory, 1, 35, 47) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                String configItemStr = ApothicAdditionConfig.FLUX_ENCHANTER_REFRESH_ITEM.get();
                Item requiredItem = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.parse(configItemStr));
                return stack.is(requiredItem);
            }
        });

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
        this.addDataSlot(eternaSlot);
        this.addDataSlot(quantaSlot);
        this.addDataSlot(arcanaSlot);
    }

    @Override
    public void slotsChanged(Container inventory) {
        if (blockEntity.getLevel().isClientSide) return;
        ItemStack stack = blockEntity.inventory.getStackInSlot(0);

        EnchantmentTableStats stats = EnchantmentTableStats.gatherStats(blockEntity.getLevel(), blockEntity.getBlockPos(), stack.isEmpty() ? 0 : stack.getEnchantmentValue());
        eternaSlot.set(Float.floatToIntBits(stats.eterna()));
        quantaSlot.set(Float.floatToIntBits(stats.quanta()));
        arcanaSlot.set(Float.floatToIntBits(stats.arcana()));

        boolean isEnchantable = canEnchantItem(stack);

        if (isEnchantable) {
            this.random.setSeed(this.enchantmentSeed.get());
            for (int i = 0; i < 3; ++i) {
                this.costs[i] = ApothEnchantmentHelper.getEnchantmentCost(random, i, stats.eterna(), stack);
                this.enchantClue[i] = -1;
                this.levelClue[i] = -1;
            }

            for (int i = 0; i < 3; ++i) {
                if (this.costs[i] > 0) {
                    net.minecraft.core.Registry<Enchantment> enchRegistry = blockEntity.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT);

                    this.random.setSeed((long) (this.enchantmentSeed.get() + i));

                    List<EnchantmentInstance> list = ApothEnchantmentHelper.selectEnchantment(random, stack, this.costs[i], stats, enchRegistry.asLookup());

                    if (list != null && !list.isEmpty()) {
                        EnchantmentInstance firstInstance = list.get(0);
                        this.enchantClue[i] = enchRegistry.getId(firstInstance.enchantment.value());
                        this.levelClue[i] = firstInstance.level;

                        int maxClues = stats.clues();
                        List<EnchantmentInstance> displayClues = new ArrayList<>();
                        boolean allRevealed = false;

                        if (maxClues > 0) {
                            List<EnchantmentInstance> copyList = new ArrayList<>(list);
                            while (displayClues.size() < maxClues && !copyList.isEmpty()) {
                                displayClues.add(copyList.remove(this.blockEntity.getLevel().random.nextInt(copyList.size())));
                            }
                            allRevealed = copyList.isEmpty();
                        }

                        if (this.player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                            PacketDistributor.sendToPlayer(serverPlayer, new FluxCluePayload(i, displayClues, allRevealed));
                        }
                    } else {
                        if (this.player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                            PacketDistributor.sendToPlayer(serverPlayer, new FluxCluePayload(i, List.of(), true));
                        }
                    }
                } else {
                    if (this.player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                        PacketDistributor.sendToPlayer(serverPlayer, new FluxCluePayload(i, List.of(), true));
                    }
                }
            }
        } else {
            // ✨ [调整] 放入杂物或槽位为空时，强制将 costs 设为 0，这会让右侧 UI 瞬间全空
            for (int i = 0; i < 3; ++i) {
                this.costs[i] = 0;
                this.enchantClue[i] = -1;
                this.levelClue[i] = -1;
                if (this.player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                    PacketDistributor.sendToPlayer(serverPlayer, new FluxCluePayload(i, List.of(), true));
                }
            }
        }
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
                this.random.setSeed((long) (this.enchantmentSeed.get() + slot));
                net.minecraft.core.Registry<Enchantment> enchRegistry = blockEntity.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
                List<EnchantmentInstance> list = ApothEnchantmentHelper.selectEnchantment(random, stack, costLevel, stats, enchRegistry.asLookup());

                if (list != null && !list.isEmpty()) {
                    blockEntity.energyStorage.extractEnergy(feCost, false);

                    if (stack.is(Items.BOOK)) {
                        stack = new ItemStack(Items.ENCHANTED_BOOK);
                    }

                    for (EnchantmentInstance instance : list) {
                        stack.enchant(instance.enchantment, instance.level);
                    }

                    blockEntity.inventory.setStackInSlot(0, stack);

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

        if (!blockEntity.getLevel().isClientSide && blockEntity.getLevel().getGameTime() % 20 == 0) {
            ItemStack stack = blockEntity.inventory.getStackInSlot(0);
            EnchantmentTableStats currentStats = EnchantmentTableStats.gatherStats(blockEntity.getLevel(), blockEntity.getBlockPos(), stack.isEmpty() ? 0 : stack.getEnchantmentValue());
            if (Float.floatToIntBits(currentStats.eterna()) != eternaSlot.get() ||
                    Float.floatToIntBits(currentStats.quanta()) != quantaSlot.get() ||
                    Float.floatToIntBits(currentStats.arcana()) != arcanaSlot.get()) {
                this.slotsChanged(new SimpleContainer(0));
            }
        }
    }

    public int getEnergy() {
        return (energyUpper.get() << 16) | (energyLower.get() & 0xFFFF);
    }

    public float getEterna() {
        return Float.intBitsToFloat(eternaSlot.get());
    }

    public float getQuanta() {
        return Float.intBitsToFloat(quantaSlot.get());
    }

    public float getArcana() {
        return Float.intBitsToFloat(arcanaSlot.get());
    }

    public FluxEnchantingTableBlockEntity getBlockEntity() {
        return this.blockEntity;
    }

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

            if (index == 0 || index == 1) {
                if (!this.moveItemStackTo(itemstack1, 2, 38, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                String configItemStr = ApothicAdditionConfig.FLUX_ENCHANTER_REFRESH_ITEM.get();
                Item requiredItem = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.parse(configItemStr));

                boolean movedToMachine = false;

                if (itemstack1.is(requiredItem)) {
                    movedToMachine = this.moveItemStackTo(itemstack1, 1, 2, false);
                }

                // ✨ [调整] 像原版附魔台一样，只要附魔槽是空的，任何东西都可以被 Shift 塞进去
                if (!movedToMachine) {
                    movedToMachine = this.moveItemStackTo(itemstack1, 0, 1, false);
                }

                if (!movedToMachine) {
                    if (index >= 2 && index < 29) {
                        if (!this.moveItemStackTo(itemstack1, 29, 38, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (index >= 29 && index < 38) {
                        if (!this.moveItemStackTo(itemstack1, 2, 29, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                }
            }

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
