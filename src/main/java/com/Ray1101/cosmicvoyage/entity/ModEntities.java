package com.Ray1101.cosmicvoyage.entity;

import com.Ray1101.cosmicvoyage.CosmicVoyage;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, CosmicVoyage.MOD_ID);

    public static final RegistryObject<EntityType<ShipEntity>> SHIP = ENTITY_TYPES.register("ship",
            () -> EntityType.Builder.<ShipEntity>of(ShipEntity::new, MobCategory.MISC)
                    .sized(2.0f, 1.0f)
                    .build("ship"));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}