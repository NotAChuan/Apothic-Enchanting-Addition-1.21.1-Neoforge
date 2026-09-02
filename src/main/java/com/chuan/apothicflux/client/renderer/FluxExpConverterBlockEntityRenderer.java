package com.chuan.apothicflux.client.renderer;

import com.chuan.apothicflux.block.entity.FluxExpConverterBlockEntity;
import com.chuan.apothicflux.registry.ModRegistry;
import com.chuan.apothicflux.util.ExpConverterTopScale;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.model.data.ModelData;

public class FluxExpConverterBlockEntityRenderer implements BlockEntityRenderer<FluxExpConverterBlockEntity> {

    public FluxExpConverterBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(FluxExpConverterBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (blockEntity.getLevel() == null) {
            return;
        }

        float introProgress = Mth.clamp(blockEntity.getIntroProgress(partialTick), 0.0F, 1.0F);
        float introEase = introProgress * introProgress * (3.0F - 2.0F * introProgress);
        float spinTime = blockEntity.getLevel().getGameTime() + partialTick;

        poseStack.pushPose();
        float bob = Mth.sin(spinTime * 0.1F) * 0.02F;
        float height = Mth.lerp(introEase, 0.0F, 1.15F) + bob;
        float targetScale = ExpConverterTopScale.scaleForStoredXp(blockEntity.getStoredXp());
        float scale = Mth.lerp(introEase, ExpConverterTopScale.MIN_SCALE, targetScale);
        poseStack.translate(0.5F, height, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(spinTime * 1.5F));
        poseStack.mulPose(Axis.XP.rotationDegrees(20.0F));
        poseStack.scale(scale, scale, scale);
        poseStack.translate(-0.5, -0.5, -0.5);

        // 直接渲染块模型（不依赖任何物品）
        var blockState = ModRegistry.EXP_CONVERTER_TOP_BLOCK.get().defaultBlockState();
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                blockState, poseStack, bufferSource, LightTexture.FULL_BRIGHT, packedOverlay,
                ModelData.EMPTY, null);

        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(FluxExpConverterBlockEntity blockEntity) {
        var pos = blockEntity.getBlockPos();
        return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0D, pos.getY() + 1.5D, pos.getZ() + 1.0D);
    }
}
