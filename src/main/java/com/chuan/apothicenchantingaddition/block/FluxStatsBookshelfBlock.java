package com.chuan.apothicenchantingaddition.block;

import com.chuan.apothicenchantingaddition.block.entity.FluxStatsBookshelfBlockEntity;
import com.chuan.apothicenchantingaddition.menu.FluxStatsBookshelfMenu;
import dev.shadowsoffire.apothic_enchanting.api.EnchantmentStatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public class FluxStatsBookshelfBlock extends Block implements EntityBlock, EnchantmentStatBlock {

    // 添加能量等级属性（1=0%, 2=33%, 3=66%, 4=100%）
    public static final IntegerProperty ENERGY_LEVEL = IntegerProperty.create("energy_level", 1, 4);

    private final Tier tier;

    public FluxStatsBookshelfBlock(Properties properties, Tier tier) {
        super(properties);
        this.tier = tier;
        this.registerDefaultState(this.stateDefinition.any().setValue(ENERGY_LEVEL, 1));
    }

    public Tier getTier() {
        return tier;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluxStatsBookshelfBlockEntity(pos, state);
    }

    // ========== 神化 API 对接与断电判断 ==========

    @Override
    public float getQuantaBonus(BlockState state, LevelReader level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof FluxStatsBookshelfBlockEntity be) {
            return be.getQuanta();
        }
        return 0;
    }

    @Override
    public float getArcanaBonus(BlockState state, LevelReader level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof FluxStatsBookshelfBlockEntity be) {
            return be.getArcana();
        }
        return 0;
    }

    @Override
    public int getBonusClues(BlockState state, LevelReader level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof FluxStatsBookshelfBlockEntity be) {
            return be.getClues();
        }
        return 0;
    }

    @Override
    public boolean allowsTreasure(BlockState state, LevelReader level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof FluxStatsBookshelfBlockEntity be) {
            return be.allowsTreasure();
        }
        return false;
    }

    @Override
    public boolean providesStability(BlockState state, LevelReader level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof FluxStatsBookshelfBlockEntity be) {
            return be.isStable(); // 校准
        }
        return false;
    }

    @Override
    public float getMaxEnchantingPower(BlockState state, LevelReader level, BlockPos pos) {
        return tier.getMaxEterna(); // 返回该层级的最大位阶上限
    }

    // NeoForge 方法，用于提供位阶 (Eterna)
    @Override
    public float getEnchantPowerBonus(BlockState state, LevelReader level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof FluxStatsBookshelfBlockEntity be) {
            // 注意：根据源码，神化会将这个值乘2，所以我们需要将存储的 eterna 值除以 2 返回
            return be.getEterna() / 2.0F;
        }
        return 0;
    }

    // ========== BlockEntity 生命周期 ==========

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return level.isClientSide ? null : (lvl, pos, st, be) -> {
            if (be instanceof FluxStatsBookshelfBlockEntity statsBE) {
                statsBE.tick(lvl, pos, st);
            }
        };
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            if (level.getBlockEntity(pos) instanceof FluxStatsBookshelfBlockEntity entity) {
                ServerPlayer serverPlayer = (ServerPlayer) player;
                serverPlayer.openMenu(new net.minecraft.world.MenuProvider() {
                    @Override
                    public net.minecraft.network.chat.Component getDisplayName() {
                        return net.minecraft.network.chat.Component.translatable("gui.apothicenchantingaddition.title.stats_bookshelf");
                    }

                    @Override
                    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int containerId, net.minecraft.world.entity.player.Inventory inventory, net.minecraft.world.entity.player.Player player) {
                        return new FluxStatsBookshelfMenu(containerId, inventory, entity);
                    }
                }, pos); // 传入 pos 以便 Menu 构造函数能读取
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ENERGY_LEVEL);
    }
}
