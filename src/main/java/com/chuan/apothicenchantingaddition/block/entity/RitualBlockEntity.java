package com.chuan.apothicenchantingaddition.block.entity;

import com.chuan.apothicenchantingaddition.recipe.RitualCraftingRecipe;
import com.chuan.apothicenchantingaddition.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Unit;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;

public class RitualBlockEntity extends BlockEntity {

    public final ItemStackHandler inventory = new ItemStackHandler(17) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide) {
                // 内容改变时，重置配方状态
                currentRecipe = null;
                progress = 0;
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    };

    // 核心字段
    public RitualCraftingRecipe currentRecipe;
    public int progress = 0;

    // 客户端渲染用的旋转变量
    public float renderTick = 0;

    public RitualBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.RITUAL_BE.get(), pos, state);
    }

    public boolean interact(Player player, InteractionHand hand) {
        if (level == null || level.isClientSide) return true;

        ItemStack handStack = player.getItemInHand(hand);

        if (!handStack.isEmpty()) {
            for (int i = 0; i < inventory.getSlots(); i++) {
                if (inventory.getStackInSlot(i).isEmpty()) {
                    ItemStack toInsert = handStack.copy();
                    toInsert.setCount(1);
                    inventory.setStackInSlot(i, toInsert);
                    if (!player.isCreative()) handStack.shrink(1);
                    return true;
                }
            }
        } else {
            for (int i = inventory.getSlots() - 1; i >= 0; i--) {
                ItemStack stack = inventory.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    player.setItemInHand(hand, stack.copy());
                    inventory.setStackInSlot(i, ItemStack.EMPTY);
                    return true;
                }
            }
        }
        return false;
    }

    public void tick() {
        if (level == null) return;

        if (level.isClientSide) {
            renderTick++;
            return;
        }

        // 1. 查找配方 (每秒检查一次以节省性能，或者在 currentRecipe 为空时检查)
        if (currentRecipe == null && (level.getGameTime() % 20 == 0)) {
            currentRecipe = findMatchingRecipe(level, inventory);
            if (currentRecipe != null) {
                progress = 0;
                // 发送提示
                level.getEntitiesOfClass(Player.class, new AABB(worldPosition).inflate(5)).forEach(p ->
                        p.displayClientMessage(Component.translatable("ritual.apothicenchantingaddition.crafting").withStyle(ChatFormatting.GOLD), true));
                // 同步开始状态
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }

        // 2. 进度处理
        if (currentRecipe != null) {
            // 再次校验配方是否有效 (防止中途取走物品)
            if (!matches(currentRecipe, inventory)) {
                currentRecipe = null;
                progress = 0;
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                return;
            }

            progress++;

            // 每 20 tick 同步一次进度，或者你可以根据需要更高频同步
            if (progress % 2 == 0) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }

            if (progress >= currentRecipe.craftTime()) {
                finishRitual(level, worldPosition);
            }
        }
    }

    private RitualCraftingRecipe findMatchingRecipe(Level level, ItemStackHandler inventory) {
        return level.getRecipeManager().getAllRecipesFor(ModRegistry.RITUAL_TYPE.get()).stream()
                .map(RecipeHolder::value)
                .filter(recipe -> matches(recipe, inventory))
                .findFirst()
                .orElse(null);
    }

    // 匹配逻辑：检查背包里的物品是否包含配方所需的所有原料
    private boolean matches(RitualCraftingRecipe recipe, ItemStackHandler inventory) {
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < inventory.getSlots(); i++) {
            if (!inventory.getStackInSlot(i).isEmpty()) items.add(inventory.getStackInSlot(i));
        }

        List<Ingredient> ingredients = recipe.inputs();
        if (items.size() != ingredients.size()) return false;

        List<ItemStack> checkItems = new ArrayList<>(items);

        for (Ingredient ing : ingredients) {
            boolean found = false;
            for (int i = 0; i < checkItems.size(); i++) {
                if (ing.test(checkItems.get(i))) {
                    checkItems.remove(i);
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return true;
    }

    private void finishRitual(Level level, BlockPos pos) {
        if (currentRecipe == null) return;
        // 保存配方引用，因为清空物品会触发 onContentsChanged 将其置为 null
        RitualCraftingRecipe recipe = currentRecipe;

        // 1. 消耗输入物品（清空所有槽位）
        for (int i = 0; i < inventory.getSlots(); i++) {
            inventory.setStackInSlot(i, ItemStack.EMPTY);
        }
        setChanged(); // 标记数据变化（可选）

        // 2. 生成物品
        if (!recipe.outputItem().isEmpty()) {
            ItemStack outputStack = recipe.outputItem().copy();
            ItemEntity itemEntity = new ItemEntity(
                    level,
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5,
                    outputStack
            );
            // 设置拾取延迟（10 tick = 0.5秒，防止立刻被玩家误拾）
            itemEntity.setPickUpDelay(10);
            // 设置无敌
            itemEntity.setInvulnerable(true);
            level.addFreshEntity(itemEntity);
        }

        // 3. 生成流体
        recipe.outputFluid().ifPresent(fluidId -> {
            try {
                Fluid fluid = BuiltInRegistries.FLUID.get(ResourceLocation.parse(fluidId));
                if (fluid != Fluids.EMPTY) {
                    level.setBlock(pos, fluid.defaultFluidState().createLegacyBlock(), 3);
                }
            } catch (Exception e) {
                // 忽略无效ID
            }
        });

        // 4. 生成实体
        recipe.outputEntity().ifPresent(entityId -> {
            EntityType.byString(entityId).ifPresent(type -> {
                Entity entity = type.create(level);
                if (entity != null) {
                    entity.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
                    level.addFreshEntity(entity);
                }
            });
        });

        // 5. 最后移除仪式核心方块
        level.removeBlock(pos, false);
    }

    // ================== 数据同步 ==================

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", inventory.serializeNBT(registries));
        tag.putInt("Progress", progress); // 保存进度
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        }
        progress = tag.getInt("Progress"); // 读取进度
    }

    // 也就是 level.sendBlockUpdated 触发时发送的数据包
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries); // 把进度也发给客户端
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
