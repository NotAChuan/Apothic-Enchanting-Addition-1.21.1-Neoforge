package com.chuan.apothicflux.block;

import com.chuan.apothicflux.block.entity.AdvancedFluxInfuserBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import com.mojang.serialization.MapCodec;

public class AdvancedFluxInfuserBlock extends FluxInfuserBlock {
    public static final MapCodec<AdvancedFluxInfuserBlock> CODEC = simpleCodec(AdvancedFluxInfuserBlock::new);
    public AdvancedFluxInfuserBlock(Properties properties) { super(properties); }
    @Override protected MapCodec<? extends net.minecraft.world.level.block.Block> codec() { return CODEC; }
    @Nullable @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new AdvancedFluxInfuserBlockEntity(pos, state); }
}
