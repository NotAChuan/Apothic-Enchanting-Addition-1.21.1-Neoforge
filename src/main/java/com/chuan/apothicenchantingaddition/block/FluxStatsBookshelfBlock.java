package com.chuan.apothicenchantingaddition.block;

import com.chuan.apothicenchantingaddition.block.entity.StatsBookshelfBlockEntity;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public class FluxStatsBookshelfBlock extends Block implements EntityBlock, EnchantmentStatBlock {

    private final Tier tier;

    public FluxStatsBookshelfBlock(Properties properties, Tier tier) {
        super(properties);
        this.tier = tier;
    }

    public Tier getTier() {
        return tier;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StatsBookshelfBlockEntity(pos, state);
    }

    // ========== 神化 API 对接与断电判断 ==========

    @Override
    public float getQuantaBonus(BlockState state, LevelReader level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof StatsBookshelfBlockEntity be) {
            return be.getQuanta();
        }
        return 0;
    }

    @Override
    public float getArcanaBonus(BlockState state, LevelReader level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof StatsBookshelfBlockEntity be) {
            return be.getArcana();
        }
        return 0;
    }

    @Override
    public int getBonusClues(BlockState state, LevelReader level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof StatsBookshelfBlockEntity be) {
            return be.getClues();
        }
        return 0;
    }

    @Override
    public boolean allowsTreasure(BlockState state, LevelReader level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof StatsBookshelfBlockEntity be) {
            return be.allowsTreasure();
        }
        return false;
    }

    @Override
    public boolean providesStability(BlockState state, LevelReader level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof StatsBookshelfBlockEntity be) {
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
        if (level.getBlockEntity(pos) instanceof StatsBookshelfBlockEntity be) {
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
            if (be instanceof StatsBookshelfBlockEntity statsBE) {
                statsBE.tick(lvl, pos, st);
            }
        };
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            if (level.getBlockEntity(pos) instanceof StatsBookshelfBlockEntity entity) {
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
}
