package com.Ray1101.cosmicvoyage.client.render;

import com.Ray1101.cosmicvoyage.CosmicVoyage;
import com.Ray1101.cosmicvoyage.dimension.ModDimensions;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * 导航 HUD 渲染器 — 在太空维度显示远处天体的方向指示器。
 *
 * <p>Phase 2：新增火星导航目标。
 * 同时，引用 MarsRenderer.MARS_POSITION 确保 MarsRenderer 类被 JVM 加载，
 * 其 @Mod.EventBusSubscriber 事件注册生效。
 */
@Mod.EventBusSubscriber(modid = CosmicVoyage.MOD_ID, value = Dist.CLIENT)
public class NavigationHUDRenderer {

    public static final float MARGIN = 30.0f;
    public static final float ARROW_SIZE = 14.0f;

    // Phase 2：新增火星导航目标
    // 引用 MarsRenderer.MARS_POSITION 确保 MarsRenderer 类被加载
    private static final NavTarget[] TARGETS = {
            new NavTarget("Earth", EarthRenderer.EARTH_POSITION,
                    600.0, 2000.0,
                    0.25f, 0.50f, 1.0f),
            new NavTarget("Moon", MoonRenderer.MOON_POSITION,
                    600.0, 800.0,
                    0.70f, 0.70f, 0.75f),
            new NavTarget("Mars", MarsRenderer.MARS_POSITION,          // Phase 2：火星
                    800.0, 2500.0,                                    // closeDist/farDist 比地球大（行星尺度）
                    0.90f, 0.35f, 0.15f)                               // 橙红色
    };

    @SubscribeEvent
    public static void onRenderGui(RenderGuiOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        if (!mc.level.dimension().equals(ModDimensions.SPACE)) return;

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 camPos = camera.getPosition();
        float playerYaw = camera.getYRot();
        float playerPitch = camera.getXRot();
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();
        float fovDeg = mc.options.fov().get().floatValue();

        for (NavTarget target : TARGETS) {
            double dist = camPos.distanceTo(target.pos);
            if (dist < target.closeDist) continue;

            AngleDiff diff = calcAngleDiff(camPos, target.pos, playerYaw, playerPitch,
                    fovDeg, screenW, screenH);

            if (diff.onScreen) {
                renderOnScreen(event.getGuiGraphics(), diff, target, dist);
            } else {
                renderEdgeArrow(event.getGuiGraphics(), diff, target, dist, screenW, screenH);
            }
        }
    }

    private static AngleDiff calcAngleDiff(Vec3 camPos, Vec3 targetPos,
                                           float playerYaw, float playerPitch,
                                           float fovDeg, int screenW, int screenH) {
        double dx = targetPos.x - camPos.x;
        double dy = targetPos.y - camPos.y;
        double dz = targetPos.z - camPos.z;

        // atan2(-dx, dz) = Minecraft yaw 约定
        double targetYaw = Math.atan2(-dx, dz);
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        double targetPitch = Math.atan2(-dy, horizontalDist);

        double yawDiff = targetYaw - Math.toRadians(playerYaw);
        double pitchDiff = targetPitch - Math.toRadians(playerPitch);

        while (yawDiff > Math.PI) yawDiff -= 2 * Math.PI;
        while (yawDiff < -Math.PI) yawDiff += 2 * Math.PI;

        double halfFovY = Math.toRadians(fovDeg);
        double aspect = (double) screenW / screenH;
        double halfFovX = halfFovY * aspect;

        // normX 不加负号：yawDiff 正 = 目标在左边 = screenX 左（< cx）
        double normX = yawDiff / halfFovX;
        double normY = pitchDiff / halfFovY;

        double screenX = (normX + 1.0) * 0.5 * screenW;
        double screenY = (normY + 1.0) * 0.5 * screenH;

        boolean onScreen = Math.abs(normX) <= 1.1 && Math.abs(normY) <= 1.1;

        return new AngleDiff(screenX, screenY, normX, normY, onScreen);
    }

    private static void renderOnScreen(GuiGraphics gui, AngleDiff diff,
                                       NavTarget target, double dist) {
        float px = (float) diff.screenX;
        float py = (float) diff.screenY;

        float dotSize = Mth.clamp((float)(target.closeDist * 0.4 / dist), 2.0f, 8.0f);
        float alpha = dist > target.farDist ? 0.6f : 0.85f;

        int coreColor = packColor(target.r, target.g, target.b, alpha);
        int glowColor = packColor(target.r, target.g, target.b, alpha * 0.25f);

        gui.fill((int)(px - dotSize - 2), (int)(py - dotSize - 2),
                (int)(px + dotSize + 3), (int)(py + dotSize + 3), glowColor);
        gui.fill((int)(px - dotSize), (int)(py - dotSize),
                (int)(px + dotSize + 1), (int)(py + dotSize + 1), coreColor);

        String label = target.name + " " + formatDistance(dist);
        gui.drawString(Minecraft.getInstance().font, label,
                (int)(px + dotSize + 5), (int)(py - 4), 0xFFFFFF);
    }

    private static void renderEdgeArrow(GuiGraphics gui, AngleDiff diff,
                                        NavTarget target, double dist,
                                        int screenW, int screenH) {
        double cx = screenW / 2.0;
        double cy = screenH / 2.0;

        // 射线从中心穿过投影点，钳制到屏幕边缘
        double scale = 1.0;
        if (Math.abs(diff.normX) > 0.001) {
            scale = Math.min(scale, 1.0 / Math.abs(diff.normX));
        }
        if (Math.abs(diff.normY) > 0.001) {
            scale = Math.min(scale, 1.0 / Math.abs(diff.normY));
        }

        double screenX = (diff.normX + 1.0) * 0.5 * screenW;
        double screenY = (diff.normY + 1.0) * 0.5 * screenH;
        double dx = screenX - cx;
        double dy = screenY - cy;

        float px = (float)(cx + dx * scale);
        float py = (float)(cy + dy * scale);

        px = Mth.clamp(px, MARGIN, screenW - MARGIN);
        py = Mth.clamp(py, MARGIN, screenH - MARGIN);

        double angle = Math.atan2(diff.normY, diff.normX);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        PoseStack poseStack = gui.pose();
        poseStack.pushPose();
        poseStack.translate(px, py, 0);

        float arrowDeg = (float)(angle * Mth.RAD_TO_DEG);
        poseStack.mulPose(new Quaternionf().rotateZ(arrowDeg * Mth.DEG_TO_RAD));

        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        Matrix4f mat = poseStack.last().pose();

        buf.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        float r = target.r, g = target.g, b = target.b;
        float s = ARROW_SIZE;

        buf.vertex(mat, s, 0, 0).color(r, g, b, 0.9f).endVertex();
        buf.vertex(mat, -s * 0.5f, s * 0.5f, 0).color(r, g, b, 0.9f).endVertex();
        buf.vertex(mat, -s * 0.5f, -s * 0.5f, 0).color(r, g, b, 0.9f).endVertex();
        tess.end();

        poseStack.popPose();

        // 文字：朝屏幕中心偏移
        String label = target.name + " " + formatDistance(dist);
        int fontW = Minecraft.getInstance().font.width(label);

        double toCenterX = cx - px;
        double toCenterY = cy - py;
        double toCenterLen = Math.sqrt(toCenterX * toCenterX + toCenterY * toCenterY);
        if (toCenterLen > 1.0) {
            double ix = toCenterX / toCenterLen;
            double iy = toCenterY / toCenterLen;

            int textX = (int)(px + ix * (ARROW_SIZE + 10));
            int textY = (int)(py + iy * 6 - 4);

            textX = Mth.clamp(textX, 5, screenW - fontW - 5);
            textY = Mth.clamp(textY, 5, screenH - 15);

            gui.drawString(Minecraft.getInstance().font, label, textX, textY, 0xFFFFFF);
        }

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private static int packColor(float r, float g, float b, float a) {
        int ri = ((int)(r * 255)) & 0xFF;
        int gi = ((int)(g * 255)) & 0xFF;
        int bi = ((int)(b * 255)) & 0xFF;
        int ai = ((int)(a * 255)) & 0xFF;
        return (ai << 24) | (ri << 16) | (gi << 8) | bi;
    }

    private static String formatDistance(double dist) {
        if (dist < 1000) return String.format("%.0fm", dist);
        return String.format("%.1fkm", dist / 1000);
    }

    private static class NavTarget {
        final String name;
        final Vec3 pos;
        final double closeDist, farDist;
        final float r, g, b;
        NavTarget(String n, Vec3 p, double c, double f, float r, float g, float b) {
            this.name = n; this.pos = p; this.closeDist = c; this.farDist = f;
            this.r = r; this.g = g; this.b = b;
        }
    }

    private static class AngleDiff {
        final double screenX, screenY;
        final double normX, normY;
        final boolean onScreen;
        AngleDiff(double sx, double sy, double nx, double ny, boolean os) {
            this.screenX = sx; this.screenY = sy;
            this.normX = nx; this.normY = ny; this.onScreen = os;
        }
    }
}