package com.Ray1101.cosmicvoyage.client;

import com.Ray1101.cosmicvoyage.CosmicVoyage;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * 为 Space 和 Moon 维度注册纯虚空天空效果
 *
 * SkyType.NONE = 原版完全不渲染天空（包括太阳、蓝天渐变、云）
 * 我们的星空在 AFTER_SKY 阶段独立渲染
 */
@Mod.EventBusSubscriber(modid = CosmicVoyage.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModDimensionEffects {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            try {
                // 反射获取私有的 EFFECTS map
                Field field = DimensionSpecialEffects.class.getDeclaredField("EFFECTS");
                field.setAccessible(true);
                @SuppressWarnings("unchecked")
                Map<ResourceLocation, DimensionSpecialEffects> effects =
                        (Map<ResourceLocation, DimensionSpecialEffects>) field.get(null);

                // 太空维度
                effects.put(
                        ResourceLocation.fromNamespaceAndPath(CosmicVoyage.MOD_ID, "space"),
                        new VoidDimensionEffects()
                );

                // 月球维度
                effects.put(
                        ResourceLocation.fromNamespaceAndPath(CosmicVoyage.MOD_ID, "moon"),
                        new VoidDimensionEffects()
                );

                System.out.println("[CosmicVoyage] Dimension effects registered");
            } catch (Exception e) {
                System.err.println("[CosmicVoyage] Failed to register effects: " + e);
                e.printStackTrace();
            }
        });
    }

    /**
     * 纯虚空效果：无天空盒、无太阳、无云
     */
    public static class VoidDimensionEffects extends DimensionSpecialEffects {

        public VoidDimensionEffects() {
            super(Float.NaN, false, SkyType.NONE, false, false);
        }

        @Override
        public Vec3 getBrightnessDependentFogColor(Vec3 biomeFogColor, float daylight) {
            return Vec3.ZERO; // 纯黑雾
        }

        @Override
        public boolean isFoggyAt(int x, int y) {
            return false;
        }
    }
}