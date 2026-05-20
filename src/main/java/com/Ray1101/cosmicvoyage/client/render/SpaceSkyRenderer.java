package com.Ray1101.cosmicvoyage.client.render;

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

/**
 * 太空天空渲染器 — P2-2: float[] 数组替换为结构化 Star 记录。
 *
 * <p>星星参数（大小、亮度、位置）从匿名 float[5] 数组改为命名的 {@link Star} 记录，
 * 消除魔法数字索引（star[0]=x, star[3]=size 等），提升可读性和类型安全。
 *
 * <p>BugFix: 添加缺失的 @Mod.EventBusSubscriber 注册注解和维度检查。
 * 之前因缺少注册导致 onRenderLevelStage 永远不会被调用，太空维度星空不显示。
 *
 * <p>Phase 2: 维度检查加入 MARS，火星维度也显示星空。
 */
@Mod.EventBusSubscriber(modid = CosmicVoyage.MOD_ID, value = Dist.CLIENT)
public class SpaceSkyRenderer {

    // ===== P2-2: 星星配置常量 =====
    /** 星星数量 */
    private static final int STAR_COUNT = 2000;
    /** 星星最小大小 */
    private static final float STAR_SIZE_MIN = 0.3f;
    /** 星星大小随机范围 */
    private static final float STAR_SIZE_RANGE = 0.7f;
    /** 星星最小亮度 */
    private static final float STAR_BRIGHTNESS_MIN = 0.5f;
    /** 星星亮度随机范围 */
    private static final float STAR_BRIGHTNESS_RANGE = 0.5f;
    /** 星星渲染全局缩放 */
    private static final float STAR_RENDER_SCALE = 0.008f;
    /** 星星旋转速度 */
    private static final float STAR_ROTATION_SPEED = 0.0001f;
    /** 星星颜色轻微蓝化系数 */
    private static final float STAR_BLUE_TINT = 0.95f;

    /** P2-2: 结构化星星记录，替代 float[5] 数组 */
    private record Star(float x, float y, float z, float size, float brightness) {}

    private static final List<Star> STARS = new ArrayList<>();
    private static boolean initialized = false;

    private static void initStars() {
        if (initialized) return;
        initialized = true;
        Random random = new Random(42);
        for (int i = 0; i < STAR_COUNT; i++) {
            double theta = random.nextDouble() * Math.PI * 2;
            double phi = random.nextDouble() * Math.PI;
            double x = Math.sin(phi) * Math.cos(theta);
            double y = Math.cos(phi);
            double z = Math.sin(phi) * Math.sin(theta);
            float size = STAR_SIZE_MIN + random.nextFloat() * STAR_SIZE_RANGE;
            float brightness = STAR_BRIGHTNESS_MIN + random.nextFloat() * STAR_BRIGHTNESS_RANGE;
            // P2-2: 使用结构化 Star 记录替代 new float[]{x, y, z, size, brightness}
            STARS.add(new Star((float) x, (float) y, (float) z, size, brightness));
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        // Phase 2: 维度检查 — 太空/月球/火星维度都渲染星空
        if (!mc.level.dimension().equals(ModDimensions.SPACE)
                && !mc.level.dimension().equals(ModDimensions.MOON)
                && !mc.level.dimension().equals(ModDimensions.MARS)) {
            return;
        }

        initStars();

        PoseStack poseStack = event.getPoseStack();
        float time = mc.level.getGameTime() + event.getPartialTick();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);

        // 缓慢旋转星空
        poseStack.pushPose();
        poseStack.mulPoseMatrix(new org.joml.Matrix4f().rotationY(time * STAR_ROTATION_SPEED));

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Matrix4f matrix = poseStack.last().pose();
        buffer.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

        // P2-2: 使用结构化访问替代魔法数字索引
        for (Star star : STARS) {
            float size = star.size() * STAR_RENDER_SCALE;
            float b = star.brightness();
            float x = star.x();
            float y = star.y();
            float z = star.z();

            // 使用命名属性替代 star[0], star[1], star[2], star[3], star[4]
            buffer.vertex(matrix, x - size, y, z).color(b, b, b * STAR_BLUE_TINT, 1.0f).endVertex();
            buffer.vertex(matrix, x + size, y, z).color(b, b, b * STAR_BLUE_TINT, 1.0f).endVertex();
            buffer.vertex(matrix, x, y - size, z).color(b, b, b * STAR_BLUE_TINT, 1.0f).endVertex();
            buffer.vertex(matrix, x, y + size, z).color(b, b, b * STAR_BLUE_TINT, 1.0f).endVertex();
            buffer.vertex(matrix, x, y, z - size).color(b, b, b * STAR_BLUE_TINT, 1.0f).endVertex();
            buffer.vertex(matrix, x, y, z + size).color(b, b, b * STAR_BLUE_TINT, 1.0f).endVertex();
        }

        tesselator.end();
        poseStack.popPose();

        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }
}