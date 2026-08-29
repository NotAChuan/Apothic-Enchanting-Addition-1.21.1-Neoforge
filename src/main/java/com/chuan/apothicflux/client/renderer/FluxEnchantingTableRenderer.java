package com.chuan.apothicflux.client.renderer;

import com.chuan.apothicflux.block.entity.FluxEnchantingTableBlockEntity;
import com.chuan.apothicflux.util.BookAnimationHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.BookModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

public class FluxEnchantingTableRenderer implements BlockEntityRenderer<FluxEnchantingTableBlockEntity> {

    public static final Material BOOK_LOCATION = new Material(
            ResourceLocation.withDefaultNamespace("textures/atlas/blocks.png"),
            ResourceLocation.withDefaultNamespace("entity/enchanting_table_book")
    );

    private final BookModel bookModel;

    public FluxEnchantingTableRenderer(BlockEntityRendererProvider.Context context) {
        this.bookModel = new BookModel(context.bakeLayer(ModelLayers.BOOK));
    }

    @Override
    public void render(FluxEnchantingTableBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.75F, 0.5F);

        float animationTime = blockEntity.time + partialTick;
        poseStack.translate(0.0F, 0.1F + Mth.sin(animationTime * 0.1F) * 0.01F, 0.0F);

        float rotation = blockEntity.oRot + BookAnimationHelper.rotationDelta(blockEntity.oRot, blockEntity.rot) * partialTick;
        poseStack.mulPose(Axis.YP.rotation(-rotation));
        poseStack.mulPose(Axis.ZP.rotationDegrees(80.0F));

        float flip = Mth.lerp(partialTick, blockEntity.oFlip, blockEntity.flip);
        float pageFlipLeft = Mth.frac(flip + 0.25F) * 1.6F - 0.3F;
        float pageFlipRight = Mth.frac(flip + 0.75F) * 1.6F - 0.3F;
        float open = blockEntity.getOpenAngle(partialTick);
        this.bookModel.setupAnim(
                animationTime,
                Mth.clamp(pageFlipLeft, 0.0F, 1.0F),
                Mth.clamp(pageFlipRight, 0.0F, 1.0F),
                open);

        VertexConsumer vertexConsumer = BOOK_LOCATION.buffer(bufferSource, RenderType::entitySolid);
        this.bookModel.render(poseStack, vertexConsumer, packedLight, packedOverlay, 0xFFFFFFFF);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(FluxEnchantingTableBlockEntity blockEntity) {
        var pos = blockEntity.getBlockPos();
        return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0D, pos.getY() + 1.5D, pos.getZ() + 1.0D);
    }
}
