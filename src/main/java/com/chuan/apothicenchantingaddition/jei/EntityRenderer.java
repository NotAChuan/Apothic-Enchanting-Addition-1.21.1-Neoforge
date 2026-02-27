package com.chuan.apothicenchantingaddition.jei;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.joml.Quaternionf;

public class EntityRenderer {

    /**
     * 渲染实体展示：LivingEntity 渲染3D模型，其他显示图片+文字
     */
    public static void render(GuiGraphics guiGraphics, String entityId,
                              int centerX, int centerY, int boxSize,
                              ResourceLocation fallbackTexture) {

        // 解析 ResourceLocation
        ResourceLocation entityRL = ResourceLocation.tryParse(entityId);
        if (entityRL == null) {
            renderFallback(guiGraphics, entityId, centerX, centerY, boxSize, fallbackTexture);
            return;
        }

        // 检查是否在注册表中
        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(entityRL)) {
            renderFallback(guiGraphics, entityId, centerX, centerY, boxSize, fallbackTexture);
            return;
        }

        EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(entityRL);
        Level level = Minecraft.getInstance().level;
        if (level == null || entityType == null) {
            renderFallback(guiGraphics, entityId, centerX, centerY, boxSize, fallbackTexture);
            return;
        }

        // 创建实体实例，判断是否为 LivingEntity
        Entity entity;
        try {
            entity = entityType.create(level);
        } catch (Exception e) {
            renderFallback(guiGraphics, entityId, centerX, centerY, boxSize, fallbackTexture);
            return;
        }

        if (entity == null || !(entity instanceof LivingEntity)) {
            // 非生物类实体（闪电、TNT、烟花等）一律显示图片+文字
            renderFallback(guiGraphics, entityId, centerX, centerY, boxSize, fallbackTexture);
            return;
        }

        // LivingEntity：渲染3D模型
        try {
            LivingEntity livingEntity = (LivingEntity) entity;
            EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();

            // 动态缩放：根据实体包围盒适配展示框
            float entityWidth = entity.getBbWidth();
            float entityHeight = entity.getBbHeight();
            float maxDimension = Math.max(entityWidth, entityHeight);
            float scale = (boxSize * 0.6f) / Math.max(maxDimension, 0.1f);
            scale = Math.min(scale, boxSize * 0.8f);
            scale = Math.max(scale, 4.0f);

            PoseStack poseStack = guiGraphics.pose();
            poseStack.pushPose();

            // 移动到展示框中心
            poseStack.translate(centerX, centerY + entityHeight * scale * 0.3f, 100);
            poseStack.scale(scale, -scale, scale);

            // 轻微旋转，朝向玩家
            Quaternionf rotation = Axis.YP.rotationDegrees(-30);
            poseStack.mulPose(rotation);

            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            net.minecraft.client.renderer.MultiBufferSource.BufferSource bufferSource =
                    Minecraft.getInstance().renderBuffers().bufferSource();

            dispatcher.setRenderShadow(false);
            dispatcher.render(livingEntity, 0, 0, 0, 0,
                    1.0f, poseStack, bufferSource, 0xF000F0);
            bufferSource.endBatch();
            dispatcher.setRenderShadow(true);

            poseStack.popPose();

        } catch (Exception e) {
            // 渲染异常降级
            renderFallback(guiGraphics, entityId, centerX, centerY, boxSize, fallbackTexture);
        }
    }

    /**
     * 降级渲染：自定义贴图 + 实体名称文字
     */
    private static void renderFallback(GuiGraphics guiGraphics, String entityId,
                                       int centerX, int centerY, int boxSize,
                                       ResourceLocation fallbackTexture) {
        int iconSize = Math.min(boxSize, 24);
        int iconX = centerX - iconSize / 2;
        int iconY = centerY - iconSize / 2 - 4;

        // 绘制自定义图标
        guiGraphics.blit(fallbackTexture, iconX, iconY, 0, 0, iconSize, iconSize, iconSize, iconSize);

        // 获取实体本地化名称
        ResourceLocation rl = ResourceLocation.tryParse(entityId);
        String displayName;
        if (rl != null && BuiltInRegistries.ENTITY_TYPE.containsKey(rl)) {
            // 使用 EntityType 的本地化名称
            displayName = Component.translatable(
                    BuiltInRegistries.ENTITY_TYPE.get(rl).getDescriptionId()
            ).getString();
        } else {
            // 无法解析时显示原始 ID
            displayName = rl != null ? rl.getPath().replace("_", " ") : entityId;
            if (!displayName.isEmpty()) {
                displayName = Character.toUpperCase(displayName.charAt(0)) + displayName.substring(1);
            }
        }

        guiGraphics.drawCenteredString(
                Minecraft.getInstance().font,
                displayName,
                centerX,
                iconY + iconSize + 2,
                0xFFFFFF
        );
    }
}
