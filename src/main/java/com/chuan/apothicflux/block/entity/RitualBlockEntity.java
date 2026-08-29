package com.chuan.apothicflux.block.entity;

import com.chuan.apothicflux.recipe.RitualCraftingRecipe;
import com.chuan.apothicflux.registry.ModRegistry;
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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.ItemStackHandler;

public class RitualBlockEntity extends BlockEntity {

    public enum RitualState {
        IDLE,
        ACTIVATING,
        CRAFTING,
        FINISHING
    }

    public RitualState ritualState = RitualState.IDLE;
    public int stateTimer = 0;

    public float craftingStartRenderTick = 0;
    private boolean isFinishing = false;

    public final ItemStackHandler inventory = new ItemStackHandler(17) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide) {
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

    // [新增] 辅助方法：构建标准的 RecipeInput，将法阵的物品栏包装成配方系统能识别的输入源
    private RecipeInput createRecipeInput() {
        return new RecipeInput() {
            @Override
            public ItemStack getItem(int index) {
                return inventory.getStackInSlot(index);
            }

            @Override
            public int size() {
                return inventory.getSlots();
            }
        };
    }

    private void tryStartRitual() {
        if (level == null || level.isClientSide || isFinishing) return;
        if (ritualState != RitualState.IDLE) return;

        if (isObstructed()) {
            warnObstruction();
            return;
        }

        currentRecipe = findMatchingRecipe(level);
        if (currentRecipe != null) {
            progress = 0;
            stateTimer = 20;
            craftingStartRenderTick = 0;
            ritualState = RitualState.ACTIVATING;
            level.getEntitiesOfClass(Player.class, new AABB(worldPosition).inflate(5)).forEach(p ->
                    p.displayClientMessage(
                            Component.translatable("ritual.apothic_flux.crafting")
                                    .withStyle(ChatFormatting.GOLD), true));
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
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
                    tryStartRitual();
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

    // [新增] 检测上方是否受阻
    private boolean isObstructed() {
        if (level == null) return false;
        // 检测上方一格是否为空气
        return !level.getBlockState(worldPosition.above()).isAir();
    }

    // [新增] 发送受阻提示给附近玩家
    private void warnObstruction() {
        if (level == null) return;
        level.getEntitiesOfClass(Player.class, new AABB(worldPosition).inflate(5)).forEach(p ->
                p.displayClientMessage(
                        Component.translatable("ritual.apothic_flux.obstructed")
                                .withStyle(ChatFormatting.RED), true));
    }

    public void tick() {
        if (level == null) return;

        if (level.isClientSide) {
            renderTick++;
            return;
        }

        // 读档恢复逻辑：退出重进后，内存中的 currentRecipe 会变成 null
        if (currentRecipe == null && (ritualState == RitualState.CRAFTING || ritualState == RitualState.ACTIVATING)) {
            currentRecipe = findMatchingRecipe(level);
            // 如果玩家在存档期间删除了模组或者配方无效了，才重置状态
            if (currentRecipe == null) {
                ritualState = RitualState.IDLE;
                progress = 0;
                stateTimer = 0;
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                return;
            }
        }

        if (ritualState == RitualState.ACTIVATING || ritualState == RitualState.FINISHING) {
            stateTimer--;
            if (stateTimer <= 0) {
                if (ritualState == RitualState.ACTIVATING) {
                    ritualState = RitualState.CRAFTING;
                    craftingStartRenderTick = 0;
                    level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                } else {
                    level.removeBlock(worldPosition, false);
                    return;
                }
            }
            if (ritualState == RitualState.ACTIVATING) return;
        }

        // 1. IDLE 状态不再周期性扫描配方；
        //    改为在放入物品时立即检测并启动，减少空闲时的重复遍历。

        // 2. CRAFTING 状态运行仪式
        if (currentRecipe != null && ritualState == RitualState.CRAFTING) {
            // [新增] 过程中检测：如果中途被放置了方块，中断仪式
            if (isObstructed()) {
                warnObstruction();
                // 中断重置
                currentRecipe = null;
                progress = 0;
                ritualState = RitualState.IDLE;
                craftingStartRenderTick = 0;
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                return;
            }

            // 配方有效性再次校验
            if (!currentRecipe.matches(createRecipeInput(), level)) {
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

    // [修改] 调用配方自身的 matches 方法进行严格校验
    private RitualCraftingRecipe findMatchingRecipe(Level level) {
        RecipeInput input = createRecipeInput();
        return level.getRecipeManager().getAllRecipesFor(ModRegistry.RITUAL_TYPE.get()).stream()
                .map(RecipeHolder::value)
                .filter(recipe -> recipe.matches(input, level))
                .findFirst()
                .orElse(null);
    }

    private void finishRitual(Level level, BlockPos pos) {
        if (currentRecipe == null) return;
        RitualCraftingRecipe recipe = currentRecipe;
        currentRecipe = null;
        progress = 0;

        isFinishing = true;
        for (int i = 0; i < inventory.getSlots(); i++) {
            inventory.setStackInSlot(i, ItemStack.EMPTY);
        }
        isFinishing = false;
        setChanged();

        // [修改] 将物品生成的 Y 轴抬高到 pos.getY() + 1.0 (原为 0.5)
        // 原因：法阵在下一 tick 才自毁，如果生成在 0.5 可能会被卡没或弹飞
        if (!recipe.outputItem().isEmpty()) {
            ItemStack outputStack = recipe.outputItem().copy();
            ItemEntity itemEntity = new ItemEntity(level,
                    pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, outputStack);
            itemEntity.setPickUpDelay(10);
            itemEntity.setInvulnerable(true);
            level.addFreshEntity(itemEntity);
        }

        // [修改] 将流体生成的坐标改为了 pos.above() (原为 pos)
        // 致命原因：下一 tick 执行 level.removeBlock(pos) 时，会把 pos 的方块变成空气。
        // 如果把水放在 pos，水一出现就会立刻被跟着法阵一起“删掉”。
        recipe.outputFluid().ifPresent(fluidId -> {
            try {
                Fluid fluid = BuiltInRegistries.FLUID.get(ResourceLocation.parse(fluidId));
                if (fluid != Fluids.EMPTY) {
                    BlockPos fluidPos = pos.above();
                    if (level.getBlockState(fluidPos).canBeReplaced()) {
                        level.setBlock(fluidPos, fluid.defaultFluidState().createLegacyBlock(), 3);
                    }
                }
            } catch (Exception ignored) {
            }
        });

        // [修改] 同样将实体的生成 Y 轴抬高到 pos.getY() + 1.0 (原为 pos.getY())
        recipe.outputEntity().ifPresent(entityId -> {
            EntityType.byString(entityId).ifPresent(type -> {
                Entity entity = type.create(level);
                if (entity == null) return;

                // 最小改动：仅对 Mob 改走标准生成流程，保留其他实体原本逻辑。
                if (entity instanceof Mob) {
                    entity.discard();
                    if (level instanceof ServerLevel serverLevel) {
                        Entity spawned = type.spawn(serverLevel, pos.above(), MobSpawnType.EVENT);
                        if (spawned != null) {
                            spawned.moveTo(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 0, 0);
                        }
                    }
                    return;
                }

                entity.moveTo(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 0, 0);
                level.addFreshEntity(entity);
            });
        });

        spawnFinishParticles(level, pos);
    }

    private void spawnCraftingParticles(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + 0.5;
        double cz = pos.getZ() + 0.5;

        // 无论配方有几个物品，代表阵法中心粒子都必须无条件渲染
        serverLevel.sendParticles(ParticleTypes.WITCH,
                cx, cy, cz, 2, 0.3, 0.1, 0.3, 0.01);

        int count = 0;
        for (int i = 1; i < inventory.getSlots(); i++) {
            if (!inventory.getStackInSlot(i).isEmpty()) count++;
        }

        // 只有当外圈有物品时，才去计算和渲染外圈连接到中心的传送门粒子 (PORTAL)
        if (count > 0) {
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
        }
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
