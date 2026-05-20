package com.Ray1101.cosmicvoyage.dimension;

import com.Ray1101.cosmicvoyage.CosmicVoyage;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

/**
 * 自定义维度 ResourceKey 注册。
 *
 * <p>Phase 2：新增 MARS 维度，沿用与 SPACE/MOON 相同的注册模式。
 */
public class ModDimensions {

    // ===== 太空维度 =====
    public static final ResourceKey<Level> SPACE = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(CosmicVoyage.MOD_ID, "space")
    );

    public static final ResourceKey<DimensionType> SPACE_TYPE = ResourceKey.create(
            Registries.DIMENSION_TYPE,
            ResourceLocation.fromNamespaceAndPath(CosmicVoyage.MOD_ID, "space")
    );

    // ===== 月球维度 =====
    public static final ResourceKey<Level> MOON = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(CosmicVoyage.MOD_ID, "moon")
    );

    public static final ResourceKey<DimensionType> MOON_TYPE = ResourceKey.create(
            Registries.DIMENSION_TYPE,
            ResourceLocation.fromNamespaceAndPath(CosmicVoyage.MOD_ID, "moon")
    );

    // ===== 火星维度（Phase 2） =====
    public static final ResourceKey<Level> MARS = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(CosmicVoyage.MOD_ID, "mars")
    );

    public static final ResourceKey<DimensionType> MARS_TYPE = ResourceKey.create(
            Registries.DIMENSION_TYPE,
            ResourceLocation.fromNamespaceAndPath(CosmicVoyage.MOD_ID, "mars")
    );
}