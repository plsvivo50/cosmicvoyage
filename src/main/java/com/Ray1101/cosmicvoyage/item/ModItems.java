package com.Ray1101.cosmicvoyage.item;

import com.Ray1101.cosmicvoyage.CosmicVoyage;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, CosmicVoyage.MOD_ID);

    // =========================
    // 🌌 Core Future Items
    // =========================

    // 🚀 飞船控制核心（未来用于驾驶系统）
    public static final RegistryObject<Item> SHIP_CORE =
            ITEMS.register("ship_core",
                    () -> new Item(new Item.Properties()));

    // 🌠 太空维度入口物品（未来可替代 /cvspace 指令）
    public static final RegistryObject<Item> SPACE_ANCHOR =
            ITEMS.register("space_anchor",
                    () -> new Item(new Item.Properties()));
}