package com.Ray1101.cosmicvoyage.client;

import com.Ray1101.cosmicvoyage.CosmicVoyage;
import com.Ray1101.cosmicvoyage.dimension.ModDimensions;
import com.Ray1101.cosmicvoyage.entity.ShipEntity;
import com.Ray1101.cosmicvoyage.network.CosmicVoyagePacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CosmicVoyage.MOD_ID, value = Dist.CLIENT)
public class TransitionHandler {

    public static final Vec3 MOON_POSITION = MoonRenderer.MOON_POSITION;
    public static final double TRIGGER_LAND = 200.0;
    private static long lastTransitionTime = 0;
    private static final long COOLDOWN = 100;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (!mc.level.dimension().equals(ModDimensions.SPACE)) return;
        if (!(mc.player.getVehicle() instanceof ShipEntity ship)) return;

        long tick = mc.level.getGameTime();
        if (tick - lastTransitionTime < COOLDOWN) return;

        double dist = Math.sqrt(ship.distanceToSqr(
                MOON_POSITION.x, MOON_POSITION.y, MOON_POSITION.z));

        if (dist < TRIGGER_LAND) {
            lastTransitionTime = tick;
            mc.player.sendSystemMessage(
                    net.minecraft.network.chat.Component.literal(
                            "\u00a7b[CosmicVoyage] Initiating lunar landing...")
            );

            // 发送维度切换请求到服务端
            CosmicVoyagePacketHandler.INSTANCE.sendToServer(
                    new CosmicVoyagePacketHandler.MoonTransitionPacket(
                            ship.getX(), ship.getY(), ship.getZ(),
                            ship.getYRot(), ship.getXRot(),
                            0, -0.1, 0
                    )
            );
        }
    }
}