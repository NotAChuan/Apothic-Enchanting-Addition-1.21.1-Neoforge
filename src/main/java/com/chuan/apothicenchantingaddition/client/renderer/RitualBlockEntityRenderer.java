package com.chuan.apothicenchantingaddition.client.renderer;

import com.chuan.apothicenchantingaddition.block.entity.RitualBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class RitualBlockEntityRenderer implements BlockEntityRenderer<RitualBlockEntity> {

    public RitualBlockEntityRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(RitualBlockEntity be, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        float time = be.renderTick + partialTick;

        // 预先统计外圈实际放入的物品总数，用于平分圆
        int outerCount = 0;
        for (int k = 1; k < 17; k++) {
            if (!be.inventory.getStackInSlot(k).isEmpty()) outerCount++;
        }

        int outerIndex = 0;
        for (int i = 0; i < 17; i++) {
            ItemStack stack = be.inventory.getStackInSlot(i);
            if (stack.isEmpty()) continue;

            poseStack.pushPose();

            if (i == 0) {
                // 中心物品：上下浮动 + 自转
                poseStack.translate(0.5, 0.5 + Math.sin(time * 0.1) * 0.1, 0.5);
                poseStack.mulPose(Axis.YP.rotationDegrees(time * 2));
            } else {
                // 外圈物品：平分圆，从0度（3点钟方向）出发
                // outerCount == 0 理论上不会发生，但防止除零
                float angleStep = outerCount > 0 ? (360.0f / outerCount) : 0;
                float angle = (outerIndex * angleStep) + time;
                double rad = Math.toRadians(angle);
                double radius = 1.0;

                poseStack.translate(
                        0.5 + Math.cos(rad) * radius,
                        0.5 + Math.sin((time + outerIndex * 10) * 0.1) * 0.1,
                        0.5 + Math.sin(rad) * radius
                );
                poseStack.mulPose(Axis.YP.rotationDegrees(time * 3));
                outerIndex++;
            }

            poseStack.scale(0.5f, 0.5f, 0.5f);
            Minecraft.getInstance().getItemRenderer().renderStatic(
                    stack, ItemDisplayContext.GROUND, packedLight, packedOverlay, poseStack, buffer, be.getLevel(), 0
            );

            poseStack.popPose();
        }
    }
}
