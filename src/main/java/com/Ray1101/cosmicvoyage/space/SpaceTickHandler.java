package com.Ray1101.cosmicvoyage.space;

import com.Ray1101.cosmicvoyage.CosmicVoyage;
import com.Ray1101.cosmicvoyage.dimension.ModDimensions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


@Mod.EventBusSubscriber(modid = CosmicVoyage.MOD_ID)
public class SpaceTickHandler {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;
        Level level = player.level();

        // 太空维度和月球维度：无重力
        if (level.dimension().equals(ModDimensions.SPACE)
                || level.dimension().equals(ModDimensions.MOON)) {
            if (!player.isNoGravity()) {
                player.setNoGravity(true);
            }
        } else {
            // 主世界等其他维度：恢复重力
            if (player.isNoGravity()) {
                player.setNoGravity(false);
            }
        }
    }
}