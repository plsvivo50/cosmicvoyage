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
import org.joml.Vector3f;

/**
 * 地球缩略球渲染器
 *
 * 在太空维度中渲染一颗可见的地球，支持LOD分层。
 * 核心设计：camera-relative位置 + PoseStack正确变换。
 *
 * 坐标系统说明：
 * - 地球绝对位置：(0, 0, 0)
 * - camera-relative = 地球位置 - 相机位置（float精度完全够，因为距离只有~2000格）
 * - 通过 poseStack.translate(relX, relY, relZ) 将模型坐标系原点移到地球中心
 * - 顶点使用本地坐标（相对地球中心），经过完整 model-view-projection 变换
 */
@Mod.EventBusSubscriber(modid = CosmicVoyage.MOD_ID, value = Dist.CLIENT)
public class EarthRenderer {

    /** 地球在太空维度中的固定位置 */
    public static final Vec3 EARTH_POSITION = new Vec3(0, 0, 0);

    // ===== 视觉参数 =====

    /** 地球渲染半径（视觉大小，非物理）。50格 = 适中的视觉大小 */
    public static final float EARTH_RENDER_RADIUS = 50.0f;
    /** 大气光晕半径比例 */
    public static final float ATMOSPHERE_SCALE = 1.25f;

    // ===== LOD 距离阈值 =====

    /** 远距离起点：>8000格 = billboard光点 */
    public static final double LOD_FAR = 8000.0;
    /** 中近距离切换：8000~500格 = 低模球体 */
    public static final double LOD_MID = 500.0;
    /** 近距离终点：<200格 = 完整球体+大气。不能太小，避免相机进球 */
    public static final double LOD_NEAR = 200.0;
    /** 最小渲染距离。低于此值不渲染球体（相机在球内），只显示方向指示 */
    public static final double MIN_RENDER_DIST = 30.0;

    // ===== 颜色 =====

    private static final float[] COLOR_OCEAN = {0.12f, 0.30f, 0.72f};
    private static final float[] COLOR_ATMOSPHERE = {0.45f, 0.75f, 1.0f};
    private static final float[] COLOR_BILLBOARD = {0.25f, 0.50f, 1.0f};

    // ===== 事件入口 =====

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (!mc.level.dimension().equals(ModDimensions.SPACE)) return;

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 camPos = camera.getPosition();
        double dist = camPos.distanceTo(EARTH_POSITION);

        // 计算 camera-relative 偏移（double减法保精度，结果转float）
        float relX = (float)(EARTH_POSITION.x - camPos.x);
        float relY = (float)(EARTH_POSITION.y - camPos.y);
        float relZ = (float)(EARTH_POSITION.z - camPos.z);

        // 进入近距离的日志提示
        if (mc.level.getGameTime() % 20 == 0 && dist < LOD_NEAR) {
            System.out.println("[CosmicVoyage][Earth] Approaching Earth! Distance: "
                    + String.format("%.1f", dist));
        }

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();

        // 核心：将模型坐标系原点平移到地球中心
        // 这样所有顶点是相对地球中心的本地坐标，经过完整 mvp 变换
        poseStack.translate(relX, relY, relZ);

        // 渲染状态
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();

        // LOD 选择
        if (dist > LOD_FAR) {
            renderLODFar(poseStack, dist);
        } else if (dist > LOD_MID) {
            renderLODMid(poseStack, dist);
        } else if (dist > MIN_RENDER_DIST) {
            renderLODNear(poseStack, dist);
        } else {
            // 相机在地球内部：只显示一个小方向指示器
            renderInsideIndicator(poseStack);
        }

        // 恢复渲染状态
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();

        poseStack.popPose();
    }

    // ===== LOD 远：Billboard 蓝色光点 =====

    private static void renderLODFar(PoseStack poseStack, double distance) {
        // Billboard 大小：随距离衰减，但保持最小可见大小
        float size = Mth.clamp((float)(EARTH_RENDER_RADIUS * 200.0 / distance), 1.5f, 8.0f);

        // 淡出：超过LOD_FAR 2倍距离开始淡出
        float alpha = Mth.clamp((float)(1.0 - (distance - LOD_FAR) / LOD_FAR), 0.15f, 1.0f);

        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        // 使用 TRIANGLE_FAN 绘制菱形 billboards
        Matrix4f mat = poseStack.last().pose();

        buf.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

        float r = COLOR_BILLBOARD[0], g = COLOR_BILLBOARD[1], b = COLOR_BILLBOARD[2];

        // 十字形光芒（4条线）
        buf.vertex(mat, -size, 0, 0).color(r, g, b, alpha).endVertex();
        buf.vertex(mat,  size, 0, 0).color(r, g, b, alpha).endVertex();

        buf.vertex(mat, 0, -size, 0).color(r, g, b, alpha).endVertex();
        buf.vertex(mat, 0,  size, 0).color(r, g, b, alpha).endVertex();

        buf.vertex(mat, 0, 0, -size).color(r, g, b, alpha).endVertex();
        buf.vertex(mat, 0, 0,  size).color(r, g, b, alpha).endVertex();

        tess.end();

        // 中心光点（四边形，始终面向相机用billboard trick）
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        // Billboard trick：用 camera 的旋转逆来构造四边形
        // 由于 PoseStack 已经包含了 view rotation，我们在 model space 中绘制一个面向 -Z 的quad
        // view rotation 会自动把它转向相机
        float s = size * 0.6f;

        buf.vertex(mat, -s, -s, 0).color(r, g, b, alpha * 0.8f).endVertex();
        buf.vertex(mat,  s, -s, 0).color(r, g, b, alpha * 0.8f).endVertex();
        buf.vertex(mat,  s,  s, 0).color(r, g, b, alpha * 0.8f).endVertex();
        buf.vertex(mat, -s,  s, 0).color(r, g, b, alpha * 0.8f).endVertex();

        tess.end();
    }

    // ===== LOD 中：低细分球体 =====

    private static void renderLODMid(PoseStack poseStack, double distance) {
        // 中距离过渡：距离越近越清晰
        float alpha = Mth.clamp((float)((LOD_FAR - distance) / (LOD_FAR - LOD_MID)), 0.4f, 1.0f);

        // 线框球体（4x2 经纬线，够用）
        renderWireSphere(poseStack, EARTH_RENDER_RADIUS, 8, 6, COLOR_OCEAN, alpha);
    }

    // ===== LOD 近：完整球体 + 大气光晕 =====

    private static void renderLODNear(PoseStack poseStack, double distance) {
        float alpha = Mth.clamp((float)((LOD_MID - distance) / (LOD_MID - MIN_RENDER_DIST)), 0.6f, 1.0f);

        // 大气光晕（半透明稍大球体，先渲染）
        renderWireSphere(poseStack, EARTH_RENDER_RADIUS * ATMOSPHERE_SCALE, 8, 6, COLOR_ATMOSPHERE, alpha * 0.25f);

        // 地球本体
        renderWireSphere(poseStack, EARTH_RENDER_RADIUS, 12, 8, COLOR_OCEAN, alpha);

        // 赤道线（更亮）
        renderEquatorLine(poseStack, EARTH_RENDER_RADIUS * 1.01f, alpha);
    }

    // ===== 相机在球体内部：方向指示器 =====

    private static void renderInsideIndicator(PoseStack poseStack) {
        // 在中心显示一个微弱的蓝色光晕，表示"你在大气层内"
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Matrix4f mat = poseStack.last().pose();

        buf.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

        float r = COLOR_ATMOSPHERE[0], g = COLOR_ATMOSPHERE[1], b = COLOR_ATMOSPHERE[2];
        float a = 0.15f;
        float s = 5.0f;

        // 六条放射线
        for (int i = 0; i < 6; i++) {
            float angle = (float)(Math.PI * 2.0 * i / 6.0);
            float x = Mth.cos(angle) * s;
            float y = Mth.sin(angle) * s;
            buf.vertex(mat, 0, 0, 0).color(r, g, b, a).endVertex();
            buf.vertex(mat, x, y, 0).color(r, g, b, 0.0f).endVertex();
        }

        tess.end();
    }

    // ===== 通用线框球体 =====

    private static void renderWireSphere(PoseStack poseStack, float radius, int slices, int stacks,
                                         float[] color, float alpha) {
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Matrix4f mat = poseStack.last().pose();
        float r = color[0], g = color[1], b = color[2];

        buf.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

        // 纬线（水平圈）
        for (int i = 1; i < stacks; i++) {
            float phi = (float)Math.PI * i / stacks;
            float y = Mth.cos(phi) * radius;
            float ringRadius = Mth.sin(phi) * radius;
            float brightness = 0.5f + 0.5f * Mth.sin(phi); // 简单光照

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

        // 经线（垂直弧）
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
        float r = COLOR_ATMOSPHERE[0], g = COLOR_ATMOSPHERE[1], b = COLOR_ATMOSPHERE[2];

        buf.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

        int segments = 64;
        for (int i = 0; i < segments; i++) {
            float theta0 = 2.0f * (float)Math.PI * i / segments;
            float theta1 = 2.0f * (float)Math.PI * (i + 1) / segments;

            float x0 = Mth.cos(theta0) * radius;
            float z0 = Mth.sin(theta0) * radius;
            float x1 = Mth.cos(theta1) * radius;
            float z1 = Mth.sin(theta1) * radius;

            buf.vertex(mat, x0, 0, z0).color(r, g, b, alpha * 0.6f).endVertex();
            buf.vertex(mat, x1, 0, z1).color(r, g, b, alpha * 0.6f).endVertex();
        }

        tess.end();
    }
}