package com.Ray1101.cosmicvoyage.client;

import com.Ray1101.cosmicvoyage.CosmicVoyage;
import com.Ray1101.cosmicvoyage.dimension.ModDimensions;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 月球缩略球渲染器
 *
 * 渲染逻辑与 EarthRenderer 相同，差异点：
 * - 位置：(12000, 0, 0) — 从地球出发约 24 秒飞行距离（500格/秒）
 * - 颜色：灰色坑洼表面
 * - 半径：30（约为地球的 1/4，真实比例）
 * - 无大气光晕（月球无大气）
 * - LOD 阈值与地球相同
 */
@Mod.EventBusSubscriber(modid = CosmicVoyage.MOD_ID, value = Dist.CLIENT)
public class MoonRenderer {

    private static final Logger LOGGER = LoggerFactory.getLogger(MoonRenderer.class);

    /** 月球在太空维度中的固定位置 */
    public static final Vec3 MOON_POSITION = new Vec3(12000, 0, 0);

    // ===== 视觉参数 =====
    public static final float MOON_RENDER_RADIUS = 30.0f;

    // ===== LOD 距离阈值（与地球相同） =====
    public static final double LOD_FAR = 8000.0;
    public static final double LOD_MID = 500.0;
    public static final double LOD_NEAR = 200.0;
    public static final double MIN_RENDER_DIST = 15.0;

    // ===== 颜色：灰色坑洼月球表面 =====
    private static final float[] COLOR_MOON = {0.55f, 0.55f, 0.58f};
    private static final float[] COLOR_MOON_DARK = {0.35f, 0.35f, 0.38f};
    private static final float[] COLOR_BILLBOARD = {0.70f, 0.70f, 0.75f};

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (!mc.level.dimension().equals(ModDimensions.SPACE)) return;

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 camPos = camera.getPosition();
        double dist = camPos.distanceTo(MOON_POSITION);

        // camera-relative 偏移
        float relX = (float)(MOON_POSITION.x - camPos.x);
        float relY = (float)(MOON_POSITION.y - camPos.y);
        float relZ = (float)(MOON_POSITION.z - camPos.z);

        // 进入轨道日志
        if (mc.level.getGameTime() % 20 == 0 && dist < LOD_NEAR) {
            LOGGER.info("{}[Moon] Approaching Moon! Distance: {}", CosmicVoyage.MOD_PREFIX, String.format("%.1f", dist));
        }

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(relX, relY, relZ);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();

        if (dist > LOD_FAR) {
            renderLODFar(poseStack, dist);
        } else if (dist > LOD_MID) {
            renderLODMid(poseStack, dist);
        } else if (dist > MIN_RENDER_DIST) {
            renderLODNear(poseStack, dist);
        } else {
            renderInsideIndicator(poseStack);
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();

        poseStack.popPose();
    }

    // ===== LOD 远：Billboard 白色光点 =====
    private static void renderLODFar(PoseStack poseStack, double distance) {
        float size = Mth.clamp((float)(MOON_RENDER_RADIUS * 200.0 / distance), 1.0f, 6.0f);
        float alpha = Mth.clamp((float)(1.0 - (distance - LOD_FAR) / LOD_FAR), 0.12f, 1.0f);

        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Matrix4f mat = poseStack.last().pose();

        // 十字光芒
        buf.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        float r = COLOR_BILLBOARD[0], g = COLOR_BILLBOARD[1], b = COLOR_BILLBOARD[2];

        buf.vertex(mat, -size, 0, 0).color(r, g, b, alpha).endVertex();
        buf.vertex(mat,  size, 0, 0).color(r, g, b, alpha).endVertex();
        buf.vertex(mat, 0, -size, 0).color(r, g, b, alpha).endVertex();
        buf.vertex(mat, 0,  size, 0).color(r, g, b, alpha).endVertex();
        buf.vertex(mat, 0, 0, -size).color(r, g, b, alpha).endVertex();
        buf.vertex(mat, 0, 0,  size).color(r, g, b, alpha).endVertex();
        tess.end();

        // 中心光点
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        float s = size * 0.5f;
        buf.vertex(mat, -s, -s, 0).color(r, g, b, alpha * 0.7f).endVertex();
        buf.vertex(mat,  s, -s, 0).color(r, g, b, alpha * 0.7f).endVertex();
        buf.vertex(mat,  s,  s, 0).color(r, g, b, alpha * 0.7f).endVertex();
        buf.vertex(mat, -s,  s, 0).color(r, g, b, alpha * 0.7f).endVertex();
        tess.end();
    }

    // ===== LOD 中：灰色线框球 =====
    private static void renderLODMid(PoseStack poseStack, double distance) {
        float alpha = Mth.clamp((float)((LOD_FAR - distance) / (LOD_FAR - LOD_MID)), 0.4f, 1.0f);
        renderWireSphere(poseStack, MOON_RENDER_RADIUS, 8, 6, COLOR_MOON, alpha);
    }

    // ===== LOD 近：完整球体 + 表面坑洼 =====
    private static void renderLODNear(PoseStack poseStack, double distance) {
        float alpha = Mth.clamp((float)((LOD_MID - distance) / (LOD_MID - MIN_RENDER_DIST)), 0.6f, 1.0f);

        // 月球本体（较亮灰色）
        renderWireSphere(poseStack, MOON_RENDER_RADIUS, 12, 8, COLOR_MOON, alpha);

        // 表面坑洼特征（较暗灰色，稍微偏移角度模拟陨石坑）
        renderWireSphere(poseStack, MOON_RENDER_RADIUS * 1.01f, 6, 4, COLOR_MOON_DARK, alpha * 0.4f);

        // 月球环形山赤道线
        renderEquatorLine(poseStack, MOON_RENDER_RADIUS * 1.005f, alpha * 0.3f);
    }

    // ===== 球内指示器 =====
    private static void renderInsideIndicator(PoseStack poseStack) {
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Matrix4f mat = poseStack.last().pose();

        buf.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        float r = COLOR_MOON[0], g = COLOR_MOON[1], b = COLOR_MOON[2];
        float a = 0.12f;
        float s = 4.0f;

        for (int i = 0; i < 6; i++) {
            float angle = (float)(Math.PI * 2.0 * i / 6.0);
            float x = Mth.cos(angle) * s;
            float y = Mth.sin(angle) * s;
            buf.vertex(mat, 0, 0, 0).color(r, g, b, a).endVertex();
            buf.vertex(mat, x, y, 0).color(r, g, b, 0.0f).endVertex();
        }
        tess.end();
    }

    // ===== 通用线框球体（复用 EarthRenderer 逻辑）=====
    private static void renderWireSphere(PoseStack poseStack, float radius, int slices, int stacks,
                                         float[] color, float alpha) {
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Matrix4f mat = poseStack.last().pose();
        float r = color[0], g = color[1], b = color[2];

        buf.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

        // 纬线
        for (int i = 1; i < stacks; i++) {
            float phi = (float)Math.PI * i / stacks;
            float y = Mth.cos(phi) * radius;
            float ringRadius = Mth.sin(phi) * radius;
            float brightness = 0.5f + 0.5f * Mth.sin(phi);

            for (int j = 0; j < slices; j++) {
                float theta0 = 2.0f * (float)Math.PI * j / slices;
                float theta1 = 2.0f * (float)Math.PI * (j + 1) / slices;

                float x0 = Mth.cos(theta0) * ringRadius;
                float z0 = Mth.sin(theta0) * ringRadius;
                float x1 = Mth.cos(theta1) * ringRadius;
                float z1 = Mth.sin(theta1) * ringRadius;

                buf.vertex(mat, x0, y, z0).color(r * brightness, g * brightness, b * brightness, alpha).endVertex();
                buf.vertex(mat, x1, y, z1).color(r * brightness, g * brightness, b * brightness, alpha).endVertex();
            }
        }

        // 经线
        for (int j = 0; j < slices; j++) {
            float theta = 2.0f * (float)Math.PI * j / slices;

            for (int i = 0; i < stacks; i++) {
                float phi0 = (float)Math.PI * i / stacks;
                float phi1 = (float)Math.PI * (i + 1) / stacks;

                float x0 = Mth.sin(phi0) * Mth.cos(theta) * radius;
                float y0 = Mth.cos(phi0) * radius;
                float z0 = Mth.sin(phi0) * Mth.sin(theta) * radius;

                float x1 = Mth.sin(phi1) * Mth.cos(theta) * radius;
                float y1 = Mth.cos(phi1) * radius;
                float z1 = Mth.sin(phi1) * Mth.sin(theta) * radius;

                float brightness0 = 0.5f + 0.5f * Mth.sin(phi0);
                float brightness1 = 0.5f + 0.5f * Mth.sin(phi1);

                buf.vertex(mat, x0, y0, z0).color(r * brightness0, g * brightness0, b * brightness0, alpha).endVertex();
                buf.vertex(mat, x1, y1, z1).color(r * brightness1, g * brightness1, b * brightness1, alpha).endVertex();
            }
        }

        tess.end();
    }

    // ===== 赤道线 =====
    private static void renderEquatorLine(PoseStack poseStack, float radius, float alpha) {
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Matrix4f mat = poseStack.last().pose();
        float r = COLOR_MOON_DARK[0], g = COLOR_MOON_DARK[1], b = COLOR_MOON_DARK[2];

        buf.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

        int segments = 64;
        for (int i = 0; i < segments; i++) {
            float theta0 = 2.0f * (float)Math.PI * i / segments;
            float theta1 = 2.0f * (float)Math.PI * (i + 1) / segments;

            float x0 = Mth.cos(theta0) * radius;
            float z0 = Mth.sin(theta0) * radius;
            float x1 = Mth.cos(theta1) * radius;
            float z1 = Mth.sin(theta1) * radius;

            buf.vertex(mat, x0, 0, z0).color(r, g, b, alpha).endVertex();
            buf.vertex(mat, x1, 0, z1).color(r, g, b, alpha).endVertex();
        }

        tess.end();
    }
}