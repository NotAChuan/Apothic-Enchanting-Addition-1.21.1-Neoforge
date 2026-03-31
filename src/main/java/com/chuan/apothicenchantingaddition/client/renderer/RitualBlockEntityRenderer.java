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
    public void render(RitualBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {

        float time = be.renderTick + partialTick;
        RitualBlockEntity.RitualState state = be.ritualState;

        // 基础旋转速度（IDLE 和 ACTIVATING 状态）
        float baseRotationSpeed = 2.0f;
        float itemScale = 1.0f;

        // 旋转角度要和“速度”分开计算。
        // 之前虽然把速度改成只看合成进度，但角度仍然是 time * rotationSpeed，
        // 这会把法阵存在总时长也乘进去，导致放在地上越久看起来越快。
        float orbitAngle = time * baseRotationSpeed;
        float selfSpinAngle = time * baseRotationSpeed;
        double radius = 1.0;

        if (state == RitualBlockEntity.RitualState.CRAFTING) {
            float elapsed = be.progress + partialTick;

            // 角速度：baseRotationSpeed + elapsed * 0.05f
            // 积分后角度：base * time + 0.5 * 0.05 * elapsed^2
            // 这样既能保持进入 CRAFTING 时连续，又不会受“放置多久”影响加速度。
            float acceleratedAngle = 0.025f * elapsed * elapsed;
            orbitAngle += acceleratedAngle;
            selfSpinAngle += acceleratedAngle * 1.5f;

        } else if (state == RitualBlockEntity.RitualState.FINISHING) {
            float finishProgress = Math.min((time % 40) / 20.0f, 1.0f);
            itemScale = 1.0f - finishProgress;
            radius = 1.0 - finishProgress;

            orbitAngle = time * 12.0f;
            selfSpinAngle = time * 18.0f;
        } else {
            // IDLE / ACTIVATING
            selfSpinAngle = orbitAngle * 1.5f;
        }

        // 统计外圈物品数量
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
                double floatY = 0.5 + Math.sin(time * 0.1) * 0.1;
                poseStack.translate(0.5, floatY, 0.5);
                poseStack.mulPose(Axis.YP.rotationDegrees(selfSpinAngle));
                poseStack.scale(0.5f * itemScale, 0.5f * itemScale, 0.5f * itemScale);
            } else {
                float angleStep = outerCount > 0 ? (360.0f / outerCount) : 0;
                float angle = (outerIndex * angleStep) + orbitAngle;
                double rad = Math.toRadians(angle);

                double floatY = 0.5 + Math.sin((time + outerIndex * 10) * 0.1) * 0.1;
                poseStack.translate(
                        0.5 + Math.cos(rad) * radius,
                        floatY,
                        0.5 + Math.sin(rad) * radius
                );
                poseStack.mulPose(Axis.YP.rotationDegrees(selfSpinAngle));
                poseStack.scale(0.5f * itemScale, 0.5f * itemScale, 0.5f * itemScale);
                outerIndex++;
            }

            Minecraft.getInstance().getItemRenderer().renderStatic(
                    stack, ItemDisplayContext.GROUND,
                    packedLight, packedOverlay,
                    poseStack, buffer, be.getLevel(), 0
            );

            poseStack.popPose();
        }
    }
}
