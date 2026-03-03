package com.chuan.apothicenchantingaddition.client.renderer;

import com.chuan.apothicenchantingaddition.block.entity.FluxEnchantingTableBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.BookModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

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

        // 移动到方块中心上方
        poseStack.translate(0.5D, 0.90D, 0.5D);

        // 计算书本朝向玩家的角度
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            BlockPos pos = blockEntity.getBlockPos();
            Vec3 playerPos = mc.player.getEyePosition(partialTick);
            Vec3 blockCenter = new Vec3(pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5);

            // 计算玩家相对于方块的角度
            double dx = playerPos.x - blockCenter.x;
            double dz = playerPos.z - blockCenter.z;
            float yaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI));

            // 让书本面向玩家
            poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        }

        // 书本倾斜
        poseStack.mulPose(Axis.ZP.rotationDegrees(80.0F));

        // 书本保持打开状态，不翻页
        this.bookModel.setupAnim(0.0F, 1.0F, 1.0F, 1.0F);

        VertexConsumer vertexConsumer = BOOK_LOCATION.buffer(bufferSource, RenderType::entitySolid);
        this.bookModel.renderToBuffer(poseStack, vertexConsumer, packedLight, packedOverlay, 0xFFFFFFFF);

        poseStack.popPose();
    }
}
