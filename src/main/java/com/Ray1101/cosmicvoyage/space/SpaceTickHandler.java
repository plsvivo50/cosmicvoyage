package com.Ray1101.cosmicvoyage.space;

import com.Ray1101.cosmicvoyage.CosmicVoyage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


@Mod.EventBusSubscriber(modid = CosmicVoyage.MOD_ID)
public class SpaceTickHandler {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {

        if (event.phase != TickEvent.Phase.END) return;

        if (!(event.player instanceof ServerPlayer player)) return;

        if (SpaceState.isInSpace(player.getUUID())) {
            System.out.println("[CosmicVoyage] SPACE TICK ACTIVE: " + player.getName().getString());
        }
    }
}