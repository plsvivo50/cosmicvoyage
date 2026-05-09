package com.Ray1101.cosmicvoyage.client;

import com.Ray1101.cosmicvoyage.CosmicVoyage;
import com.Ray1101.cosmicvoyage.dimension.ModDimensions;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Mod.EventBusSubscriber(modid = CosmicVoyage.MOD_ID, value = Dist.CLIENT)
public class SpaceSkyRenderer {

    // 预生成的星星数据（坐标 + 大小）
    private static final List<float[]> STARS = new ArrayList<>();
    private static final int STAR_COUNT = 1500;
    private static boolean initialized = false;

    private static void initStars() {
        if (initialized) return;
        initialized = true;

        Random random = new Random(10842L);
        for (int i = 0; i < STAR_COUNT; i++) {
            // 单位球面上的随机点
            double theta = random.nextDouble() * Math.PI * 2; // 水平角
            double phi = Math.acos(2.0 * random.nextDouble() - 1.0); // 垂直角，均匀分布

            double x = Math.sin(phi) * Math.cos(theta);
            double y = Math.sin(phi) * Math.sin(theta);
            double z = Math.cos(phi);

            // 随机大小：0.3 ~ 1.0
            float size = 0.3f + random.nextFloat() * 0.7f;

            // 随机亮度：0.5 ~ 1.0
            float brightness = 0.5f + random.nextFloat() * 0.5f;

            STARS.add(new float[]{(float) x, (float) y, (float) z, size, brightness});
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (!mc.level.dimension().equals(ModDimensions.SPACE)
                && !mc.level.dimension().equals(ModDimensions.MOON)) return;
        initStars();

        PoseStack poseStack = event.getPoseStack();
        float time = mc.level.getGameTime() + event.getPartialTick();

        poseStack.pushPose();

        // 极缓慢旋转（一整圈约17分钟）
        poseStack.mulPoseMatrix(new org.joml.Matrix4f().rotationY(time * 0.0001f));

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();

        buffer.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix = poseStack.last().pose();

        for (float[] star : STARS) {
            float x = star[0], y = star[1], z = star[2];
            float size = star[3] * 0.008f; // 整体缩小
            float b = star[4];

            // 3D 星号：三个轴线各一条线，从任何角度看都立体
            // X轴
            buffer.vertex(matrix, x - size, y, z).color(b, b, b * 0.95f, 1.0f).endVertex();
            buffer.vertex(matrix, x + size, y, z).color(b, b, b * 0.95f, 1.0f).endVertex();
            // Y轴
            buffer.vertex(matrix, x, y - size, z).color(b, b, b * 0.95f, 1.0f).endVertex();
            buffer.vertex(matrix, x, y + size, z).color(b, b, b * 0.95f, 1.0f).endVertex();
            // Z轴
            buffer.vertex(matrix, x, y, z - size).color(b, b, b * 0.95f, 1.0f).endVertex();
            buffer.vertex(matrix, x, y, z + size).color(b, b, b * 0.95f, 1.0f).endVertex();
        }

        tesselator.end();

        poseStack.popPose();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
    }
}