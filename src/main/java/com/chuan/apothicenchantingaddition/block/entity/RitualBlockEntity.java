package com.chuan.apothicenchantingaddition.block.entity;

import com.chuan.apothicenchantingaddition.recipe.RitualCraftingRecipe;
import com.chuan.apothicenchantingaddition.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
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

    public enum RitualState {
        IDLE,
        ACTIVATING,
        CRAFTING,
        FINISHING
    }

    public RitualState ritualState = RitualState.IDLE;
    public int stateTimer = 0;

    // 记录进入 CRAFTING 时的 renderTick，用于计算加速角度差值
    public float craftingStartRenderTick = 0;

    // 标志位：防止 finishRitual 清空物品时触发 onContentsChanged 重置状态
    private boolean isFinishing = false;

    public final ItemStackHandler inventory = new ItemStackHandler(17) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide) {
                // isFinishing 为 true 时是合成完成清空物品，不重置状态
                if (!isFinishing) {
                    currentRecipe = null;
                    progress = 0;
                    ritualState = RitualState.IDLE;
                    stateTimer = 0;
                    craftingStartRenderTick = 0;
                }
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    };

    public RitualCraftingRecipe currentRecipe;
    public int progress = 0;
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

        // ===== 状态计时器处理 =====
        if (ritualState == RitualState.ACTIVATING || ritualState == RitualState.FINISHING) {
            stateTimer--;
            if (stateTimer <= 0) {
                if (ritualState == RitualState.ACTIVATING) {
                    ritualState = RitualState.CRAFTING;
                    level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                } else {
                    // FINISHING 结束：移除方块
                    level.removeBlock(worldPosition, false);
                    return;
                }
            }
            if (ritualState == RitualState.ACTIVATING) return;
        }

        // ===== 1. 查找配方（仅 IDLE 状态）=====
        if (currentRecipe == null && ritualState == RitualState.IDLE && (level.getGameTime() % 20 == 0)) {
            currentRecipe = findMatchingRecipe(level, inventory);
            if (currentRecipe != null) {
                progress = 0;
                ritualState = RitualState.ACTIVATING;
                stateTimer = 20;
                level.getEntitiesOfClass(Player.class, new AABB(worldPosition).inflate(5)).forEach(p ->
                        p.displayClientMessage(
                                Component.translatable("ritual.apothicenchantingaddition.crafting")
                                        .withStyle(ChatFormatting.GOLD), true));
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }

        // ===== 2. 合成进度（仅 CRAFTING 状态）=====
        if (currentRecipe != null && ritualState == RitualState.CRAFTING) {
            if (!matches(currentRecipe, inventory)) {
                currentRecipe = null;
                progress = 0;
                ritualState = RitualState.IDLE;
                craftingStartRenderTick = 0;
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                return;
            }

            progress++;

            if (currentRecipe.craftTime() >= 60 && progress % 4 == 0) {
                spawnCraftingParticles(level, worldPosition);
            }

            if (progress % 2 == 0) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }

            if (progress >= currentRecipe.craftTime()) {
                ritualState = RitualState.FINISHING;
                stateTimer = 1;
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
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
        RitualCraftingRecipe recipe = currentRecipe;
        currentRecipe = null;
        progress = 0;

        // 设置标志位，防止清空物品时触发状态重置
        isFinishing = true;
        for (int i = 0; i < inventory.getSlots(); i++) {
            inventory.setStackInSlot(i, ItemStack.EMPTY);
        }
        isFinishing = false;
        setChanged();

        // 生成物品
        if (!recipe.outputItem().isEmpty()) {
            ItemStack outputStack = recipe.outputItem().copy();
            ItemEntity itemEntity = new ItemEntity(level,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, outputStack);
            itemEntity.setPickUpDelay(10);
            itemEntity.setInvulnerable(true);
            level.addFreshEntity(itemEntity);
        }

        // 生成流体
        recipe.outputFluid().ifPresent(fluidId -> {
            try {
                Fluid fluid = BuiltInRegistries.FLUID.get(ResourceLocation.parse(fluidId));
                if (fluid != Fluids.EMPTY) {
                    level.setBlock(pos, fluid.defaultFluidState().createLegacyBlock(), 3);
                }
            } catch (Exception ignored) {}
        });

        // 生成实体
        recipe.outputEntity().ifPresent(entityId -> {
            EntityType.byString(entityId).ifPresent(type -> {
                Entity entity = type.create(level);
                if (entity != null) {
                    entity.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
                    level.addFreshEntity(entity);
                }
            });
        });

        spawnFinishParticles(level, pos);
    }

    private void spawnCraftingParticles(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + 0.5;
        double cz = pos.getZ() + 0.5;

        int count = 0;
        for (int i = 1; i < inventory.getSlots(); i++) {
            if (!inventory.getStackInSlot(i).isEmpty()) count++;
        }
        if (count == 0) return;

        int idx = 0;
        for (int i = 1; i < inventory.getSlots(); i++) {
            if (inventory.getStackInSlot(i).isEmpty()) continue;
            float angle = (float) (idx * 2 * Math.PI / count);
            double ox = cx + Math.cos(angle) * 1.0;
            double oz = cz + Math.sin(angle) * 1.0;
            double dx = (cx - ox) * 0.1;
            double dz = (cz - oz) * 0.1;
            serverLevel.sendParticles(ParticleTypes.PORTAL,
                    ox, cy + 0.5, oz, 1, dx, 0.05, dz, 0.01);
            idx++;
        }

        serverLevel.sendParticles(ParticleTypes.WITCH,
                cx, cy, cz, 2, 0.3, 0.1, 0.3, 0.01);
    }

    private void spawnFinishParticles(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + 0.5;
        double cz = pos.getZ() + 0.5;

        serverLevel.sendParticles(ParticleTypes.END_ROD,
                cx, cy, cz, 30, 0.3, 0.5, 0.3, 0.1);
        serverLevel.sendParticles(ParticleTypes.FLASH,
                cx, cy, cz, 1, 0, 0, 0, 0);
        serverLevel.sendParticles(ParticleTypes.ENCHANT,
                cx, cy, cz, 50, 1.0, 0.5, 1.0, 0.2);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", inventory.serializeNBT(registries));
        tag.putInt("Progress", progress);
        tag.putInt("RitualState", ritualState.ordinal());
        tag.putInt("StateTimer", stateTimer);
        tag.putFloat("CraftingStartRenderTick", craftingStartRenderTick);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        }
        progress = tag.getInt("Progress");
        int stateOrdinal = tag.getInt("RitualState");
        ritualState = RitualState.values()[Math.min(stateOrdinal, RitualState.values().length - 1)];
        stateTimer = tag.getInt("StateTimer");
        craftingStartRenderTick = tag.getFloat("CraftingStartRenderTick");

        if (level != null && !level.isClientSide && ritualState != RitualState.IDLE) {
            ritualState = RitualState.IDLE;
            progress = 0;
            stateTimer = 0;
        }
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
