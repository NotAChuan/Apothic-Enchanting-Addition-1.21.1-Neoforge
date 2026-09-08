package com.chuan.apothicflux.block;

import com.chuan.apothicflux.block.entity.FluxSpawnerBlockEntity;
import com.chuan.apothicflux.config.ApothicAdditionConfig;
import com.chuan.apothicflux.integration.productivebees.ProductiveBeesIntegration;
import com.chuan.apothicflux.registry.ModRegistry;
import com.chuan.apothicflux.util.MachineStateDropHelper;
import com.chuan.apothicflux.util.SpawnerStatApplicator;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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

    private static final ResourceLocation UPGRADE_BLOCK_ID =
            ResourceLocation.fromNamespaceAndPath("productivelib", "upgrade_block");
    private static final ResourceLocation UPGRADE_PRODUCTIVITY_ID =
            ResourceLocation.fromNamespaceAndPath("productivelib", "upgrade_productivity");
    private static final ResourceLocation UPGRADE_PRODUCTIVITY_2_ID =
            ResourceLocation.fromNamespaceAndPath("productivelib", "upgrade_productivity_2");
    private static final ResourceLocation UPGRADE_PRODUCTIVITY_3_ID =
            ResourceLocation.fromNamespaceAndPath("productivelib", "upgrade_productivity_3");
    private static final ResourceLocation UPGRADE_PRODUCTIVITY_4_ID =
            ResourceLocation.fromNamespaceAndPath("productivelib", "upgrade_productivity_4");

    private static final List<ProductivityUpgrade> PRODUCTIVITY_UPGRADES = List.of(
            new ProductivityUpgrade(UPGRADE_PRODUCTIVITY_ID, 20, 40),
            new ProductivityUpgrade(UPGRADE_PRODUCTIVITY_2_ID, 50, 100),
            new ProductivityUpgrade(UPGRADE_PRODUCTIVITY_3_ID, 100, 200),
            new ProductivityUpgrade(UPGRADE_PRODUCTIVITY_4_ID, 160, 320)
    );

    private record ProductivityUpgrade(ResourceLocation itemId, int stepPercent, int capPercent) {
    }


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
            // 1. 优先尝试应用本模组的蜂箱模拟升级
            if (tryApplyBeehiveSimulationUpgrade(level, player, stack, fluxBE)) {
                return ItemInteractionResult.SUCCESS;
            }

            // 2. 尝试应用资源蜜蜂的蜜脾块 / 增产升级
            if (tryApplyProductiveBeesSubUpgrade(level, player, stack, fluxBE)) {
                return ItemInteractionResult.SUCCESS;
            }

            // 3. 其次尝试应用神化刷怪笼的升级配方
            if (tryApplyApothicModifier(level, player, hand, stack, fluxBE)) {
                return ItemInteractionResult.SUCCESS;
            }

            // 4. 如果不是升级物品，则打开通量刷怪笼的 GUI 界面
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.openMenu(fluxBE, pos);
            }
        }

        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    private boolean tryApplyBeehiveSimulationUpgrade(Level level, Player player, ItemStack stack, FluxSpawnerBlockEntity fluxBE) {
        if (ModRegistry.BEEHIVE_SIMULATION_UPGRADE == null || !stack.is(ModRegistry.BEEHIVE_SIMULATION_UPGRADE.get())) {
            return false;
        }

        boolean uninstall = hasQuartzOffhand(player);
        if (uninstall) {
            if (!fluxBE.hasBeehiveSimulationUpgrade()) {
                player.displayClientMessage(Component.translatable(
                        "message.apothic_flux.beehive_simulation_upgrade.not_installed"), true);
                return true;
            }

            fluxBE.setBeehiveSimulationUpgrade(false);
            consumeMainHand(player, stack);
            playUpgradeFeedback(level, fluxBE, false);
            return true;
        }

        if (fluxBE.hasBeehiveSimulationUpgrade()) {
            player.displayClientMessage(Component.translatable(
                    "message.apothic_flux.beehive_simulation_upgrade.already_installed"), true);
            return true;
        }

        fluxBE.setBeehiveSimulationUpgrade(true);
        consumeMainHand(player, stack);
        playUpgradeFeedback(level, fluxBE, true);
        return true;
    }

    private boolean tryApplyProductiveBeesSubUpgrade(Level level, Player player, ItemStack stack, FluxSpawnerBlockEntity fluxBE) {
        if (!ProductiveBeesIntegration.isLoaded() || !isProductiveBeesSubUpgrade(stack)) {
            return false;
        }

        if (!fluxBE.hasBeehiveSimulationUpgrade()) {
            player.displayClientMessage(Component.translatable(
                    "message.apothic_flux.beehive_simulation_upgrade.requires_beehive"), true);
            return true;
        }

        boolean uninstall = hasQuartzOffhand(player);

        if (isItem(stack, UPGRADE_BLOCK_ID)) {
            boolean targetMode = !uninstall;
            if (fluxBE.isHoneycombBlockMode() == targetMode) {
                player.displayClientMessage(Component.translatable(
                        "message.apothic_flux.beehive_simulation_upgrade.no_change"), true);
                return true;
            }

            fluxBE.setHoneycombBlockMode(targetMode);
            consumeMainHand(player, stack);
            playUpgradeFeedback(level, fluxBE, targetMode);
            return true;
        }

        for (ProductivityUpgrade upgrade : PRODUCTIVITY_UPGRADES) {
            if (!isItem(stack, upgrade.itemId())) {
                continue;
            }

            int current = fluxBE.getHoneycombProductivityBonusPercent();
            int next;
            if (uninstall) {
                next = Math.max(0, current - upgrade.stepPercent());
            } else {
                if (current >= upgrade.capPercent()) {
                    player.displayClientMessage(Component.translatable(
                            "message.apothic_flux.beehive_simulation_upgrade.no_change"), true);
                    return true;
                }
                next = Math.min(upgrade.capPercent(), current + upgrade.stepPercent());
            }

            if (next == current) {
                player.displayClientMessage(Component.translatable(
                        "message.apothic_flux.beehive_simulation_upgrade.no_change"), true);
                return true;
            }

            fluxBE.setHoneycombProductivityBonusPercent(next);
            consumeMainHand(player, stack);
            playUpgradeFeedback(level, fluxBE, next > current);
            return true;
        }

        return false;
    }

    private boolean hasQuartzOffhand(Player player) {
        return player.getItemInHand(InteractionHand.OFF_HAND).is(Items.QUARTZ);
    }

    private boolean isProductiveBeesSubUpgrade(ItemStack stack) {
        if (isItem(stack, UPGRADE_BLOCK_ID)) {
            return true;
        }
        for (ProductivityUpgrade upgrade : PRODUCTIVITY_UPGRADES) {
            if (isItem(stack, upgrade.itemId())) {
                return true;
            }
        }
        return false;
    }

    private boolean isItem(ItemStack stack, ResourceLocation itemId) {
        Item item = BuiltInRegistries.ITEM.get(itemId);
        return item != null && stack.is(item);
    }

    private void consumeMainHand(Player player, ItemStack stack) {
        if (!player.isCreative()) {
            stack.shrink(1);
        }
    }

    private void playUpgradeFeedback(Level level, FluxSpawnerBlockEntity fluxBE, boolean installed) {
        BlockPos pos = fluxBE.getBlockPos();
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            serverLevel.playSound(null, pos, net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP,
                    net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, installed ? 1.2F : 0.8F);
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                    pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                    installed ? 12 : 8, 0.25, 0.25, 0.25, 0.0);
        }
        fluxBE.setChanged();
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
