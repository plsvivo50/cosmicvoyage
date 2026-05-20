package com.Ray1101.cosmicvoyage.client.render;

import com.Ray1101.cosmicvoyage.CosmicVoyage;
import com.Ray1101.cosmicvoyage.SpaceConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 天体渲染器基类 — EarthRenderer 与 MoonRenderer 的 74-77% 重复代码提取。
 *
 * <p>设计模式：Config 对象。Earth/Moon 只提供不同的 {@link Config} 参数，
 * 所有渲染逻辑（LOD、线框球体、赤道线）在基类中统一实现。
 *
 * <p>P1-1：第二行星扩展时只需 new Config() + 一个 @SubscribeEvent 包装。
 */
public class CelestialBodyRenderer {

    private static final Logger LOGGER = LoggerFactory.getLogger(CelestialBodyRenderer.class);

    // ===== LOD 距离阈值（从 SpaceConstants 读取） =====
    // 设计原则：TRIGGER_LAND_* < RENDER_LOD_NEAR，确保完整球体在维度切换前显示
    public static final double LOD_FAR = SpaceConstants.RENDER_LOD_FAR;   // 8000.0
    public static final double LOD_MID = SpaceConstants.RENDER_LOD_MID;   // 2000.0
    public static final double LOD_NEAR = SpaceConstants.RENDER_LOD_NEAR; // 1000.0

    /** 天体配置参数 */
    public static class Config {
        public final String name;
        public final Vec3 position;
        public final float renderRadius;
        public final double minRenderDist;
        public final float[] colorBody;
        public final float[] colorAtmosphere;
        public final float[] colorBillboard;
        public final float atmosphereScale; // 0 = 无大气
        public final float billboardSizeMin, billboardSizeMax;
        public final float billboardAlphaMin;

        public Config(String name, Vec3 position, float renderRadius, double minRenderDist,
                      float[] colorBody, float[] colorAtmosphere, float[] colorBillboard,
                      float atmosphereScale,
                      float billboardSizeMin, float billboardSizeMax, float billboardAlphaMin) {
            this.name = name;
            this.position = position;
            this.renderRadius = renderRadius;
            this.minRenderDist = minRenderDist;
            this.colorBody = colorBody;
            this.colorAtmosphere = colorAtmosphere;
            this.colorBillboard = colorBillboard;
            this.atmosphereScale = atmosphereScale;
            this.billboardSizeMin = billboardSizeMin;
            this.billboardSizeMax = billboardSizeMax;
            this.billboardAlphaMin = billboardAlphaMin;
        }
    }

    private final Config cfg;

    public CelestialBodyRenderer(Config config) {
        this.cfg = config;
    }

    // ============================
    // 主渲染入口
    // ============================

    public void render(RenderLevelStageEvent event, Minecraft mc) {
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 camPos = camera.getPosition();
        double dist = camPos.distanceTo(cfg.position);

        // camera-relative 偏移
        float relX = (float)(cfg.position.x - camPos.x);
        float relY = (float)(cfg.position.y - camPos.y);
        float relZ = (float)(cfg.position.z - camPos.z);

        // 日志提示
        if (mc.level != null && mc.level.getGameTime() % 20 == 0 && dist < LOD_NEAR) {
            LOGGER.info("{}[{}] Approaching {}! Distance: {}",
                    CosmicVoyage.MOD_PREFIX, cfg.name, cfg.name,
                    String.format("%.1f", dist));
        }

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
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
        } else if (dist > cfg.minRenderDist) {
            renderLODNear(poseStack, dist);
        } else {
            renderInsideIndicator(poseStack);
        }

        // 恢复渲染状态
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();

        poseStack.popPose();
    }

    // ============================
    // LOD 远：Billboard 光点
    // ============================

    private void renderLODFar(PoseStack poseStack, double distance) {
        float size = Mth.clamp((float)(cfg.renderRadius * 200.0 / distance),
                cfg.billboardSizeMin, cfg.billboardSizeMax);
        float alpha = Mth.clamp((float)(1.0 - (distance - LOD_FAR) / LOD_FAR),
                cfg.billboardAlphaMin, 1.0f);

        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Matrix4f mat = poseStack.last().pose();

        float r = cfg.colorBillboard[0], g = cfg.colorBillboard[1], b = cfg.colorBillboard[2];

        // 十字光芒
        buf.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
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

    // ============================
    // LOD 中：低模球体
    // ============================

    private void renderLODMid(PoseStack poseStack, double distance) {
        float alpha = Mth.clamp((float)((LOD_FAR - distance) / (LOD_FAR - LOD_MID)), 0.4f, 1.0f);
        renderWireSphere(poseStack, cfg.renderRadius, 8, 6, cfg.colorBody, alpha);
    }

    // ============================
    // LOD 近：完整球体 + 可选大气
    // ============================

    private void renderLODNear(PoseStack poseStack, double distance) {
        float alpha = Mth.clamp((float)((LOD_MID - distance) / (LOD_MID - cfg.minRenderDist)), 0.6f, 1.0f);

        // 大气光晕（如果有）
        if (cfg.atmosphereScale > 0) {
            renderWireSphere(poseStack, cfg.renderRadius * cfg.atmosphereScale, 8, 6,
                    cfg.colorAtmosphere, alpha * 0.25f);
        }

        // 天体本体
        renderWireSphere(poseStack, cfg.renderRadius, 12, 8, cfg.colorBody, alpha);

        // 赤道线
        renderEquatorLine(poseStack, cfg.renderRadius * 1.005f, alpha * 0.3f,
                cfg.colorAtmosphere);
    }

    // ============================
    // 球内指示器
    // ============================

    private void renderInsideIndicator(PoseStack poseStack) {
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Matrix4f mat = poseStack.last().pose();

        float r = cfg.colorBody[0], g = cfg.colorBody[1], b = cfg.colorBody[2];
        float a = 0.12f;
        float s = 4.0f;

        buf.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i < 6; i++) {
            float angle = (float)(Math.PI * 2.0 * i / 6.0);
            buf.vertex(mat, 0, 0, 0).color(r, g, b, a).endVertex();
            buf.vertex(mat, Mth.cos(angle) * s, Mth.sin(angle) * s, 0)
                    .color(r, g, b, 0.0f).endVertex();
        }
        tess.end();
    }

    // ============================
    // 通用线框球体（静态工具方法）
    // ============================

    public static void renderWireSphere(PoseStack poseStack, float radius, int slices, int stacks,
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

                buf.vertex(mat, Mth.cos(theta0) * ringRadius, y, Mth.sin(theta0) * ringRadius)
                        .color(r * brightness, g * brightness, b * brightness, alpha).endVertex();
                buf.vertex(mat, Mth.cos(theta1) * ringRadius, y, Mth.sin(theta1) * ringRadius)
                        .color(r * brightness, g * brightness, b * brightness, alpha).endVertex();
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

                float b0 = 0.5f + 0.5f * Mth.sin(phi0);
                float b1 = 0.5f + 0.5f * Mth.sin(phi1);

                buf.vertex(mat, x0, y0, z0).color(r * b0, g * b0, b * b0, alpha).endVertex();
                buf.vertex(mat, x1, y1, z1).color(r * b1, g * b1, b * b1, alpha).endVertex();
            }
        }
        tess.end();
    }

    // ============================
    // 通用赤道线（静态工具方法）
    // ============================

    public static void renderEquatorLine(PoseStack poseStack, float radius, float alpha,
                                         float[] color) {
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Matrix4f mat = poseStack.last().pose();
        float r = color[0], g = color[1], b = color[2];

        buf.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        int segments = 64;
        for (int i = 0; i < segments; i++) {
            float theta0 = 2.0f * (float)Math.PI * i / segments;
            float theta1 = 2.0f * (float)Math.PI * (i + 1) / segments;
            buf.vertex(mat, Mth.cos(theta0) * radius, 0, Mth.sin(theta0) * radius)
                    .color(r, g, b, alpha).endVertex();
            buf.vertex(mat, Mth.cos(theta1) * radius, 0, Mth.sin(theta1) * radius)
                    .color(r, g, b, alpha).endVertex();
        }
        tess.end();
    }
}