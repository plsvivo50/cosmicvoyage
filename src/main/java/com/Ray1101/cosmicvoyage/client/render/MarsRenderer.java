package com.Ray1101.cosmicvoyage.client.render;

import com.Ray1101.cosmicvoyage.CosmicVoyage;
import com.Ray1101.cosmicvoyage.SpaceConstants;
import com.Ray1101.cosmicvoyage.dimension.ModDimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 火星渲染器 — Phase 2 第二行星。
 *
 * <p>基于 P1-1 重构的 {@link CelestialBodyRenderer} 基类，
 * 仅通过 Config 参数定义火星特有的视觉属性。
 *
 * <p>火星特征：
 *   - 半径 = 1/2 地球（100），比月球（50）大一倍
 *   - 铁锈红色表面，稀薄大气
 *   - 位置：(15000, 0, 0)，距地球 5000 格
 */
@Mod.EventBusSubscriber(modid = CosmicVoyage.MOD_ID, value = Dist.CLIENT)
public class MarsRenderer {

    /** 火星在太空维度中的固定位置 */
    public static final Vec3 MARS_POSITION = SpaceConstants.MARS_POSITION;

    private static final CelestialBodyRenderer RENDERER = new CelestialBodyRenderer(
            new CelestialBodyRenderer.Config(
                    "Mars",                               // name
                    MARS_POSITION,                        // position (15000, 0, 0)
                    SpaceConstants.MARS_RENDER_RADIUS,    // renderRadius (100)
                    50.0f,                                // minRenderDist — 比月球大（行星尺度）
                    new float[]{0.65f, 0.25f, 0.12f},    // colorBody — 铁锈红 (rusty red)
                    new float[]{0.80f, 0.45f, 0.30f},    // colorAtmosphere — 淡橙红（稀薄大气）
                    new float[]{0.90f, 0.35f, 0.15f},    // colorBillboard — 亮橙红（远处光点）
                    1.05f,                                // atmosphereScale — 稀薄大气，几乎不可见
                    2.0f, 10.0f,                          // billboardSizeMin/Max — 比月球大
                    0.20f                                 // billboardAlphaMin
            )
    );

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (!mc.level.dimension().equals(ModDimensions.SPACE)) return;

        RENDERER.render(event, mc);
    }
}