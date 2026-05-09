package com.Ray1101.cosmicvoyage.client;

import com.Ray1101.cosmicvoyage.CosmicVoyage;
import com.Ray1101.cosmicvoyage.entity.ShipEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(modid = CosmicVoyage.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ShipRenderer extends EntityRenderer<ShipEntity> {

    public ShipRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(ShipEntity entity) {
        return null;
    }

    @Override
    public void render(ShipEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {

        poseStack.pushPose();

        // ===== 翻转后的线框：+Z 方向为机头 =====
        poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(entity.getXRot()));
        poseStack.scale(1.5f, 1.5f, 1.5f);

        VertexConsumer buffer = bufferSource.getBuffer(RenderType.lines());
        Matrix4f matrix = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();

        int r = 0, g = 200, b = 255, a = 255;

// 机头（+Z 方向）
        vertex(buffer, matrix, normal, 0, 0, 1.5f, r, g, b, a);
        vertex(buffer, matrix, normal, 0.5f, 0, -0.5f, r, g, b, a);

        vertex(buffer, matrix, normal, 0, 0, 1.5f, r, g, b, a);
        vertex(buffer, matrix, normal, -0.5f, 0, -0.5f, r, g, b, a);

        vertex(buffer, matrix, normal, 0, 0, 1.5f, r, g, b, a);
        vertex(buffer, matrix, normal, 0, 0.5f, -0.5f, r, g, b, a);

        vertex(buffer, matrix, normal, 0, 0, 1.5f, r, g, b, a);
        vertex(buffer, matrix, normal, 0, -0.3f, -0.5f, r, g, b, a);

// 机翼
        vertex(buffer, matrix, normal, 0.5f, 0, -0.5f, r, g, b, a);
        vertex(buffer, matrix, normal, -0.5f, 0, -0.5f, r, g, b, a);

        vertex(buffer, matrix, normal, 0, 0.5f, -0.5f, r, g, b, a);
        vertex(buffer, matrix, normal, 0, -0.3f, -0.5f, r, g, b, a);

// 尾翼
        vertex(buffer, matrix, normal, 0, 0, -0.8f, r, g, b, a);
        vertex(buffer, matrix, normal, 0.3f, 0, -0.5f, r, g, b, a);

        vertex(buffer, matrix, normal, 0, 0, -0.8f, r, g, b, a);
        vertex(buffer, matrix, normal, -0.3f, 0, -0.5f, r, g, b, a);

        poseStack.popPose();

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    private static void vertex(VertexConsumer buffer, Matrix4f matrix, Matrix3f normal,
                               float x, float y, float z,
                               int r, int g, int b, int a) {
        buffer.vertex(matrix, x, y, z).color(r, g, b, a).normal(normal, 0, 1, 0).endVertex();
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(com.Ray1101.cosmicvoyage.entity.ModEntities.SHIP.get(), ShipRenderer::new);
    }
}