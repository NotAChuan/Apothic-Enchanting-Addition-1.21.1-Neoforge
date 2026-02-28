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
    private float localCraftingStartTick = -1f;
    private RitualBlockEntity.RitualState lastState = null;

    public RitualBlockEntityRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(RitualBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {

        float time = be.renderTick + partialTick;
        RitualBlockEntity.RitualState state = be.ritualState;

        // 检测刚进入 CRAFTING 状态，记录本地起始时刻
        if (state != lastState) {
            if (state == RitualBlockEntity.RitualState.CRAFTING) {
                localCraftingStartTick = time;
            }
            lastState = state;
        }

        // 基础旋转速度（IDLE 和 ACTIVATING 状态）
        float baseRotationSpeed = 2.0f;
        float rotationSpeed = baseRotationSpeed;
        float itemScale = 1.0f;

        if (state == RitualBlockEntity.RitualState.CRAFTING) {
            // 合成中：从基础速度开始，用与合成开始时的时间差计算加速
            // craftingStartRenderTick 是进入 CRAFTING 时记录的 renderTick
            // 这里由服务端同步过来，但客户端 renderTick 是独立自增的
            // 为了避免闪回，我们用 time 持续累加，乘以动态系数
            // 加速系数 0.05（比之前 0.02 更高）
            float elapsed = time - localCraftingStartTick;
            // elapsed 可能为负（服务端同步延迟），保底为0
            if (elapsed < 0) elapsed = 0;
            rotationSpeed = baseRotationSpeed + elapsed * 0.05f;

        } else if (state == RitualBlockEntity.RitualState.FINISHING) {
            // 完成动画：快速旋转 + 物品缩小消失
            rotationSpeed = 12.0f;
            // stateTimer 从 20 倒数，用 time % 40 / 20 近似进度
            float finishProgress = Math.min((time % 40) / 20.0f, 1.0f);
            itemScale = 1.0f - finishProgress;
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
                poseStack.mulPose(Axis.YP.rotationDegrees(time * rotationSpeed));
                poseStack.scale(0.5f * itemScale, 0.5f * itemScale, 0.5f * itemScale);
            } else {
                float angleStep = outerCount > 0 ? (360.0f / outerCount) : 0;
                // 公转角度：始终从 time * rotationSpeed 计算，避免状态切换时闪回
                float angle = (outerIndex * angleStep) + time * rotationSpeed;
                double rad = Math.toRadians(angle);

                double radius = 1.0;
                if (state == RitualBlockEntity.RitualState.FINISHING) {
                    float finishProgress = Math.min((time % 40) / 20.0f, 1.0f);
                    radius = 1.0 - finishProgress;
                }

                double floatY = 0.5 + Math.sin((time + outerIndex * 10) * 0.1) * 0.1;
                poseStack.translate(
                        0.5 + Math.cos(rad) * radius,
                        floatY,
                        0.5 + Math.sin(rad) * radius
                );
                poseStack.mulPose(Axis.YP.rotationDegrees(time * rotationSpeed * 1.5f));
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
