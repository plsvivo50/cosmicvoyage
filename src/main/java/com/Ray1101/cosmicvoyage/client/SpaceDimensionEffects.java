package com.Ray1101.cosmicvoyage.client;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 太空/月球维度自定义视觉效果。
 * 替换 minecraft:the_end，彻底消除 the_end 的硬编码副作用（Issue #9）。
 *
 * 参数选择：
 * - cloudLevel = Float.MAX_VALUE : 不渲染云层
 * - hasGround = false            : 无地面（防地平线渲染）
 * - skyType = NONE               : 不渲染原版天空盒（防蓝天白云）
 * - forceBrightLightAmbient = false
 * - constantAmbientLight = false
 *
 * 星空渲染由 SpaceSkyRenderer 通过 RenderLevelStageEvent 独立处理，
 * 不依赖 DimensionSpecialEffects 的 sky render handler。
 */
@OnlyIn(Dist.CLIENT)
public class SpaceDimensionEffects extends DimensionSpecialEffects {

    public SpaceDimensionEffects() {
        super(Float.MAX_VALUE, false, SkyType.NONE, false, false);
    }

    @Override
    public boolean isFoggyAt(int x, int y) {
        return false; // 太空/月球无雾
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        return Vec3.ZERO; // 太空无雾，返回纯黑
    }
}