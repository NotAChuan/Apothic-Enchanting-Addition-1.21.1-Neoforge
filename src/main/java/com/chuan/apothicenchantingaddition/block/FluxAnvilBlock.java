package com.chuan.apothicenchantingaddition.block;

import com.chuan.apothicenchantingaddition.block.entity.FluxAnvilBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import com.chuan.apothicenchantingaddition.registry.ModRegistry;
import com.chuan.apothicenchantingaddition.util.MachineStateDropHelper;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import java.util.List;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jetbrains.annotations.Nullable;

public class FluxAnvilBlock extends AnvilBlock implements EntityBlock {
    // 1.21.1 标准 Codec 注册
    public static final MapCodec<FluxAnvilBlock> CODEC = simpleCodec(FluxAnvilBlock::new);

    public static final IntegerProperty ENERGY_LEVEL = IntegerProperty.create("energy_level", 1, 4);

    public FluxAnvilBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(ENERGY_LEVEL, 1));
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


    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (params.getOptionalParameter(LootContextParams.EXPLOSION_RADIUS) != null) {
            return super.getDrops(state, params);
        }

        BlockEntity blockEntity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockEntity instanceof FluxAnvilBlockEntity fluxAnvilBlockEntity) {
            return List.of(MachineStateDropHelper.createFluxAnvilDrop(params.getLevel().registryAccess(), fluxAnvilBlockEntity));
        }

        return super.getDrops(state, params);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder); // 保留父类 AnvilBlock 的朝向属性
        builder.add(ENERGY_LEVEL);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return (lvl, pos, st, be) -> {
            if (be instanceof FluxAnvilBlockEntity fluxAnvil) {
                FluxAnvilBlockEntity.tick(lvl, pos, st, fluxAnvil);
            }
        };
    }
}
