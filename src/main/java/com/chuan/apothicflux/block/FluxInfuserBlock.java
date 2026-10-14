package com.chuan.apothicflux.block;

import com.chuan.apothicflux.block.entity.FluxInfuserBlockEntity;
import com.chuan.apothicflux.util.MachineStateDropHelper;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 自动通量灌注器方块。
 * <p>
 * 破坏时：掉落物只携带「配方记录表 + 电量」，机器内的输入物/输出物直接掉出。
 */
public class FluxInfuserBlock extends Block implements EntityBlock {

    public static final MapCodec<FluxInfuserBlock> CODEC = simpleCodec(FluxInfuserBlock::new);

    /** 0 = 无电，1..4 = 四分之一充能档位（与通量附魔台一致）。 */
    public static final IntegerProperty ENERGY_LEVEL = IntegerProperty.create("energy_level", 0, 4);

    /**
     * 模型最高点是 y=12（0.75 格，和附魔台同高），所以形状必须覆写掉默认的整格立方体，
     * 否则指向机器时黑色的选择框会是一个完整的方块。五个档位几何一致，共用一个形状。
     */
    protected static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 12.0D, 16.0D);

    public FluxInfuserBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(ENERGY_LEVEL, 0));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ENERGY_LEVEL);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluxInfuserBlockEntity(pos, state);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (level.getBlockEntity(pos) instanceof FluxInfuserBlockEntity infuser) {
                serverPlayer.openMenu(infuser, pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        // 只在方块本身被替换掉时掉落内容物；纯属性变化（energy_level）不触发
        if (!state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof FluxInfuserBlockEntity infuser) {
            for (int slot = 0; slot < FluxInfuserBlockEntity.CONTAINER_SIZE; slot++) {
                ItemStack stack = infuser.inventory.getStackInSlot(slot);
                if (!stack.isEmpty()) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (params.getOptionalParameter(LootContextParams.EXPLOSION_RADIUS) != null) {
            return super.getDrops(state, params);
        }

        BlockEntity blockEntity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockEntity instanceof FluxInfuserBlockEntity infuser) {
            return List.of(MachineStateDropHelper.createFluxInfuserDrop(params.getLevel().registryAccess(), infuser));
        }

        return super.getDrops(state, params);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return (lvl, pos, st, be) -> {
            if (be instanceof FluxInfuserBlockEntity infuser) {
                FluxInfuserBlockEntity.tick(lvl, pos, st, infuser);
            }
        };
    }
}
