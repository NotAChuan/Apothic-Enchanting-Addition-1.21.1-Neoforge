package com.chuan.apothicenchantingaddition.block;

import com.chuan.apothicenchantingaddition.block.entity.FluxSpawnerBlockEntity;
import com.chuan.apothicenchantingaddition.config.ApothicAdditionConfig;
import com.chuan.apothicenchantingaddition.registry.ModRegistry;
import com.chuan.apothicenchantingaddition.util.MachineStateDropHelper;
import com.chuan.apothicenchantingaddition.util.SpawnerStatApplicator;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import java.util.List;

// 导入神化的 API
import dev.shadowsoffire.apothic_spawners.modifiers.SpawnerModifier;
import dev.shadowsoffire.apothic_spawners.modifiers.StatModifier;
import dev.shadowsoffire.apothic_spawners.stats.SpawnerStats;

public class FluxSpawnerBlock extends BaseEntityBlock {

    public static final MapCodec<FluxSpawnerBlock> CODEC = simpleCodec(FluxSpawnerBlock::new);

    public static final IntegerProperty ENERGY_LEVEL = IntegerProperty.create("energy_level", 1, 4);


    public FluxSpawnerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(ENERGY_LEVEL, 1));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluxSpawnerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModRegistry.FLUX_SPAWNER_BE.get(), FluxSpawnerBlockEntity::tick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof FluxSpawnerBlockEntity fluxBE)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (!level.isClientSide) {
            // 1. 优先尝试应用神化刷怪笼的升级配方
            if (tryApplyApothicModifier(level, player, hand, stack, fluxBE)) {
                return ItemInteractionResult.SUCCESS;
            }

            // 2. 如果不是升级物品，则打开通量刷怪笼的 GUI 界面
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.openMenu(fluxBE, pos);
            }
        }

        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * 核心逻辑：拦截神化配方并应用（方案一：精准过滤）
     */
    private boolean tryApplyApothicModifier(Level level, Player player, InteractionHand hand, ItemStack stack, FluxSpawnerBlockEntity fluxBE) {
        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);

        var recipeManager = level.getRecipeManager();

        // 抓取并安全匹配配方
        var match = recipeManager.getRecipes().stream()
                .filter(holder -> holder.value() instanceof SpawnerModifier)
                .map(holder -> (SpawnerModifier) holder.value())
                // 对齐神化的逻辑：优先排序带有副手物品的配方
                .sorted((r1, r2) -> r1.getOffhandInput().isEmpty() ? r2.getOffhandInput().isEmpty() ? 0 : 1 : -1)
                // 精髓：传入 null 完美绕过限制，直接调用正确的 3 参数 matches 方法！
                .filter(modifier -> modifier.matches(null, mainHand, offHand))
                .findFirst();

        if (match.isPresent()) {
            SpawnerModifier modifier = match.get();
            boolean appliedAtLeastOne = false;

            // 调用确定的 Getter 获取属性修改列表
            for (StatModifier<?> statMod : modifier.getStatModifiers()) {
                Object targetStat = statMod.stat();

                if (targetStat == SpawnerStats.MIN_DELAY) {
                    int val = ((Number) statMod.value()).intValue();
                    int current = fluxBE.getMinDelay();
                    int next = SpawnerStatApplicator.applyLowerBoundStat(
                            current,
                            val,
                            toApplicatorMode(statMod.mode()),
                            ApothicAdditionConfig.FLUX_SPAWNER_MIN_DELAY_LIMIT.get(),
                            200);
                    if (current != next) {
                        fluxBE.setMinDelay(next);
                        appliedAtLeastOne = true;
                    }
                }
                else if (targetStat == SpawnerStats.MAX_DELAY) {
                    int val = ((Number) statMod.value()).intValue();
                    int current = fluxBE.getMaxDelay();
                    int next = SpawnerStatApplicator.applyLowerBoundStat(
                            current,
                            val,
                            toApplicatorMode(statMod.mode()),
                            ApothicAdditionConfig.FLUX_SPAWNER_MAX_DELAY_LIMIT.get(),
                            800);
                    if (current != next) {
                        fluxBE.setMaxDelay(next);
                        appliedAtLeastOne = true;
                    }
                }
                else if (targetStat == SpawnerStats.SPAWN_COUNT) {
                    int val = ((Number) statMod.value()).intValue();
                    int current = fluxBE.getSpawnCount();
                    int next = SpawnerStatApplicator.applyUpperBoundStat(
                            current,
                            val,
                            toApplicatorMode(statMod.mode()),
                            1,
                            ApothicAdditionConfig.FLUX_SPAWNER_SPAWN_COUNT_LIMIT.get());
                    if (current != next) {
                        fluxBE.setSpawnCount(next);
                        appliedAtLeastOne = true;
                    }
                }
                else if (targetStat == SpawnerStats.REDSTONE_CONTROL) {
                    boolean val = (Boolean) statMod.value();
                    if (fluxBE.isRedstoneControl() != val) {
                        fluxBE.setRedstoneControl(val);
                        appliedAtLeastOne = true;
                    }
                }
                else if (targetStat == SpawnerStats.ECHOING) {
                    int val = ((Number) statMod.value()).intValue();
                    int current = fluxBE.getEchoing();
                    int next = SpawnerStatApplicator.applyUpperBoundStat(
                            current,
                            val,
                            toApplicatorMode(statMod.mode()),
                            0,
                            ApothicAdditionConfig.FLUX_SPAWNER_ECHOING_LIMIT.get());
                    if (current != next) {
                        fluxBE.setEchoing(next);
                        appliedAtLeastOne = true;
                    }
                }
            }

            // 只有触发了实质性变化，才扣除资源并播放动画！
            if (appliedAtLeastOne) {
                if (!player.isCreative()) {
                    stack.shrink(1);
                    if (modifier.consumesOffhand() && !offHand.isEmpty()) {
                        offHand.shrink(1);
                    }
                }

                BlockPos pos = fluxBE.getBlockPos();
                if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    // 播放吸收经验的清脆“叮”声，音高略高，代表升级成功
                    serverLevel.playSound(null, pos, net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
                    // 在方块的正上方 (Y + 1.2) 爆发一团村民开心的绿色魔法粒子！
                    serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                            pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                            10, 0.25, 0.25, 0.25, 0.0);
                }

                fluxBE.setChanged();
                return true;
            }
        }

        return false;
    }

    private static SpawnerStatApplicator.Mode toApplicatorMode(StatModifier.Mode mode) {
        return mode == StatModifier.Mode.SET ? SpawnerStatApplicator.Mode.SET : SpawnerStatApplicator.Mode.ADD;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof FluxSpawnerBlockEntity fluxBE) {
                // 遍历 72 个槽位 (8个输入 + 64个输出)，把里面的东西全部喷出来
                for (int i = 0; i < fluxBE.inventory.getSlots(); i++) {
                    ItemStack stack = fluxBE.inventory.getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
                    }
                }
                // 更新周围方块的状态
                level.updateNeighbourForOutputSignal(pos, this);
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }


    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (params.getOptionalParameter(LootContextParams.EXPLOSION_RADIUS) != null) {
            return super.getDrops(state, params);
        }

        BlockEntity blockEntity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockEntity instanceof FluxSpawnerBlockEntity fluxBE) {
            return List.of(MachineStateDropHelper.createFluxSpawnerDrop(params.getLevel().registryAccess(), fluxBE));
        }

        return super.getDrops(state, params);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ENERGY_LEVEL);
    }
}
