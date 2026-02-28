package com.chuan.apothicenchantingaddition.block;

import com.chuan.apothicenchantingaddition.block.entity.FluxAnvilBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class FluxAnvilBlock extends AnvilBlock implements EntityBlock {
    // 1.21.1 标准 Codec 注册
    public static final MapCodec<FluxAnvilBlock> CODEC = simpleCodec(FluxAnvilBlock::new);

    public FluxAnvilBlock(Properties properties) {
        super(properties);
    }

    @SuppressWarnings("unchecked")
    @Override
    public MapCodec<AnvilBlock> codec() {
        return (MapCodec<AnvilBlock>) (Object) CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluxAnvilBlockEntity(pos, state);
    }

    // 拦截右键事件，打开我们的自定义 GUI
    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof FluxAnvilBlockEntity fluxAnvilBlockEntity) {
                // 调用 NeoForge 的菜单打开方法，将方块位置同步过去
                serverPlayer.openMenu(fluxAnvilBlockEntity, pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
