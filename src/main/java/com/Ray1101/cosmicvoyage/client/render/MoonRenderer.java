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
 * 月球渲染器 — P1-1 基类重构后版本。
 *
 * <p>所有渲染逻辑已移至 {@link CelestialBodyRenderer}，
 * 此类只负责提供月球特有的 {@link CelestialBodyRenderer.Config} 参数。
 */
@Mod.EventBusSubscriber(modid = CosmicVoyage.MOD_ID, value = Dist.CLIENT)
public class MoonRenderer {

    /** 月球在太空维度中的固定位置 */
    public static final Vec3 MOON_POSITION = new Vec3(SpaceConstants.MOON_DISTANCE, 0.0, 0.0);

    private static final CelestialBodyRenderer RENDERER = new CelestialBodyRenderer(
            new CelestialBodyRenderer.Config(
                    "Moon",                               // name
                    MOON_POSITION,                        // position
                    SpaceConstants.MOON_RENDER_RADIUS,    // renderRadius
                    15.0,                                 // minRenderDist
                    new float[]{0.75f, 0.75f, 0.70f},    // colorBody (月球灰)
                    new float[]{0.75f, 0.75f, 0.70f},    // colorAtmosphere (复用灰)
                    new float[]{0.70f, 0.70f, 0.68f},    // colorBillboard
                    0.0f,                                 // atmosphereScale (无大气)
                    1.0f, 6.0f,                           // billboardSizeMin/Max
                    0.12f                                 // billboardAlphaMin
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