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
 * 地球渲染器 — P1-1 基类重构后版本。
 *
 * <p>所有渲染逻辑已移至 {@link CelestialBodyRenderer}，
 * 此类只负责提供地球特有的 {@link CelestialBodyRenderer.Config} 参数。
 *
 * <p>Phase 2 更新：EARTH_POSITION 改为引用 SpaceConstants.EARTH_POSITION（已移至 10000, 0, 0）。
 */
@Mod.EventBusSubscriber(modid = CosmicVoyage.MOD_ID, value = Dist.CLIENT)
public class EarthRenderer {

    /** 地球在太空维度中的固定位置 — Phase 2：太阳在原点 (0,0,0)，地球在 (10000, 0, 0) */
    public static final Vec3 EARTH_POSITION = SpaceConstants.EARTH_POSITION;

    private static final CelestialBodyRenderer RENDERER = new CelestialBodyRenderer(
            new CelestialBodyRenderer.Config(
                    "Earth",                              // name
                    EARTH_POSITION,                       // position (10000, 0, 0)
                    SpaceConstants.EARTH_RENDER_RADIUS,   // renderRadius
                    30.0,                                 // minRenderDist
                    new float[]{0.12f, 0.30f, 0.72f},    // colorBody (海洋蓝)
                    new float[]{0.45f, 0.75f, 1.0f},     // colorAtmosphere
                    new float[]{0.25f, 0.50f, 1.0f},     // colorBillboard
                    1.25f,                                // atmosphereScale (有大气)
                    1.5f, 8.0f,                           // billboardSizeMin/Max
                    0.15f                                 // billboardAlphaMin
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