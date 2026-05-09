package com.Ray1101.cosmicvoyage.client;

import com.Ray1101.cosmicvoyage.CosmicVoyage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod.EventBusSubscriber(modid = CosmicVoyage.MOD_ID, value = Dist.CLIENT)
public class VT1Renderer {

    private static final Logger LOGGER = LoggerFactory.getLogger(VT1Renderer.class);
    private static final BlockPos TEST_POS = new BlockPos(1_000_000, 200, 0);

    @SubscribeEvent
    public static void onRenderWorld(RenderLevelStageEvent event) {
        // 1.20.1 Forge 标准阶段
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // 只在 90万~110万格范围内渲染，避免平时干扰
        double px = mc.player.getX();
        if (px < 900_000 || px > 1_100_000) {
            return;
        }

        LOGGER.info("[VT1] Rendering! Player at: {}, {}, {}", px, mc.player.getY(), mc.player.getZ());

        PoseStack poseStack = event.getPoseStack();
        Vec3 camPos = event.getCamera().getPosition();

        // ===== Camera-relative 坐标转换（核心）=====
        double relX = TEST_POS.getX() + 0.5 - camPos.x;
        double relY = TEST_POS.getY() + 0.5 - camPos.y;
        double relZ = TEST_POS.getZ() + 0.5 - camPos.z;

        poseStack.pushPose();
        poseStack.translate(relX, relY, relZ);

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();

        buffer.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix = poseStack.last().pose();

        int r = 255, g = 0, b = 0, a = 255;
        float s = 4.0f; // 8x8x8 线框，足够大，容易看到

        // 12条边
        // 底面
        line(buffer, matrix, -s, -s, -s,  s, -s, -s, r, g, b, a);
        line(buffer, matrix,  s, -s, -s,  s, -s,  s, r, g, b, a);
        line(buffer, matrix,  s, -s,  s, -s, -s,  s, r, g, b, a);
        line(buffer, matrix, -s, -s,  s, -s, -s, -s, r, g, b, a);
        // 顶面
        line(buffer, matrix, -s,  s, -s,  s,  s, -s, r, g, b, a);
        line(buffer, matrix,  s,  s, -s,  s,  s,  s, r, g, b, a);
        line(buffer, matrix,  s,  s,  s, -s,  s,  s, r, g, b, a);
        line(buffer, matrix, -s,  s,  s, -s,  s, -s, r, g, b, a);
        // 竖边
        line(buffer, matrix, -s, -s, -s, -s,  s, -s, r, g, b, a);
        line(buffer, matrix,  s, -s, -s,  s,  s, -s, r, g, b, a);
        line(buffer, matrix,  s, -s,  s,  s,  s,  s, r, g, b, a);
        line(buffer, matrix, -s, -s,  s, -s,  s,  s, r, g, b, a);

        tesselator.end();

        poseStack.popPose();
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
    }

    private static void line(BufferBuilder buffer, Matrix4f matrix,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             int r, int g, int b, int a) {
        buffer.vertex(matrix, x1, y1, z1).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a).endVertex();
    }
}