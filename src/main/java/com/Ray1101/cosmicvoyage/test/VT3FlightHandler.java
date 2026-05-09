package com.Ray1101.cosmicvoyage.test;

import com.Ray1101.cosmicvoyage.CosmicVoyage;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = CosmicVoyage.MOD_ID)
public class VT3FlightHandler {

    private static final Set<UUID> ACTIVE_PLAYERS = new HashSet<>();
    private static int tickCounter = 0;

    public static void start(ServerPlayer player) {
        ACTIVE_PLAYERS.add(player.getUUID());
        tickCounter = 0;
        player.setDeltaMovement(25.0, 0.0, 0.0);
    }

    public static void stop(ServerPlayer player) {
        ACTIVE_PLAYERS.remove(player.getUUID());
        player.setDeltaMovement(0.0, 0.0, 0.0);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        if (ACTIVE_PLAYERS.isEmpty()) return;

        tickCounter++;

        for (UUID id : new HashSet<>(ACTIVE_PLAYERS)) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p == null) {
                ACTIVE_PLAYERS.remove(id);
                continue;
            }

            // 500格/秒 = 25格/tick
            // 直接增加位置，绕过创造模式飞行逻辑
            p.setPos(p.getX() + 25.0, p.getY(), p.getZ());

            // 每秒输出一次坐标
            if (tickCounter % 20 == 0) {
                p.sendSystemMessage(Component.literal(
                        String.format("[VT-3] T+%.1fs | Pos: %.1f, %.1f, %.1f",
                                tickCounter / 20.0, p.getX(), p.getY(), p.getZ())
                ));
            }
        }

        // 10秒自动停止（200 ticks）
        if (tickCounter >= 200) {
            for (UUID id : new HashSet<>(ACTIVE_PLAYERS)) {
                ServerPlayer p = server.getPlayerList().getPlayer(id);
                if (p != null) {
                    p.setDeltaMovement(0, 0, 0);
                    p.sendSystemMessage(Component.literal("[VT-3] 10秒测试结束，自动停止"));
                }
            }
            ACTIVE_PLAYERS.clear();
        }
    }
}