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

        // 太空维度和月球维度
        if (level.dimension().equals(ModDimensions.SPACE)
                || level.dimension().equals(ModDimensions.MOON)) {

            // 无重力
            if (!player.isNoGravity()) {
                player.setNoGravity(true);
            }

            // 重置摔落距离（防摔落伤害）
            player.fallDistance = 0;

            // ===== 虚空防护：掉出世界边界时传送回安全高度（Issue #7）=====
            if (!level.isClientSide() && player.getY() < level.getMinBuildHeight() + 10) {
                player.moveTo(player.getX(), 200.0, player.getZ(),
                        player.getYRot(), player.getXRot());
            }

        } else {
            // 主世界等其他维度：恢复重力
            if (player.isNoGravity()) {
                player.setNoGravity(false);
            }
        }
    }
}