package com.Ray1101.cosmicvoyage.dimension;

import com.Ray1101.cosmicvoyage.CosmicVoyage;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

public class ModDimensions {
    public static final ResourceKey<Level> SPACE = ResourceKey.create(
            Registries.DIMENSION,
            new ResourceLocation(CosmicVoyage.MOD_ID, "space")
    );

    public static final ResourceKey<DimensionType> SPACE_TYPE = ResourceKey.create(
            Registries.DIMENSION_TYPE,
            new ResourceLocation(CosmicVoyage.MOD_ID, "space")
    );
}