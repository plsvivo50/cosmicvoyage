package com.Ray1101.cosmicvoyage.client;

import com.Ray1101.cosmicvoyage.CosmicVoyage;
import com.Ray1101.cosmicvoyage.SpaceConstants;
import com.Ray1101.cosmicvoyage.dimension.ModDimensions;
import com.Ray1101.cosmicvoyage.entity.ShipEntity;
import com.Ray1101.cosmicvoyage.network.CosmicVoyagePacketHandler;
import com.Ray1101.cosmicvoyage.network.packet.LandOnEarthPacket;
import com.Ray1101.cosmicvoyage.network.packet.MoonTransitionPacket;
import com.Ray1101.cosmicvoyage.network.packet.ReturnToSpacePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 过渡触发器 — 客户端距离/高度检测，自动触发维度切换。
 *
 * <p>职责：
 *   - 在太空维度：检测与月球的距离（月球着陆）和与地球的距离（返回地球）
 *   - 在月球维度：检测 y 坐标（返回太空）
 *   - 冷却时间防止重复触发
 *
 * <p>设计决策：不强制要求骑乘飞船。
 *   所有在太空/月球维度的玩家都进行检测，
 *   骑飞船时传送飞船，不骑飞船时只传送玩家。
 */
@Mod.EventBusSubscriber(modid = CosmicVoyage.MOD_ID, value = Dist.CLIENT)
public class TransitionHandler {

    // ===== 太空维度检测 =====

    // 月球位置和触发距离
    public static final Vec3 MOON_POSITION = new Vec3(SpaceConstants.MOON_DISTANCE, 0.0, 0.0);
    public static final double TRIGGER_LAND_MOON = SpaceConstants.TRIGGER_LAND_MOON;

    // 地球位置和触发距离
    public static final Vec3 EARTH_POSITION = new Vec3(0.0, 0.0, 0.0);
    public static final double TRIGGER_LAND_EARTH = SpaceConstants.TRIGGER_LAND_EARTH;

    // ===== 月球维度检测 =====

    // 月球返回太空的 y 阈值
    public static final double MOON_ESCAPE_HEIGHT = ReturnToSpacePacket.MOON_ESCAPE_HEIGHT;

    private static long lastTransitionTime = 0;
    private static final long COOLDOWN = 100;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        long tick = mc.level.getGameTime();
        if (tick - lastTransitionTime < COOLDOWN) return;

        // ===== 太空维度：月球着陆 + 地球返回 =====
        if (mc.level.dimension().equals(ModDimensions.SPACE)) {
            handleSpaceDimension(mc, tick);
            return;
        }

        // ===== 月球维度：返回太空 =====
        if (mc.level.dimension().equals(ModDimensions.MOON)) {
            handleMoonDimension(mc, tick);
        }
    }

    /**
     * 太空维度处理：检测月球和地球的距离。
     */
    private static void handleSpaceDimension(Minecraft mc, long tick) {
        // 检测月球着陆
        double moonDist = Math.sqrt(mc.player.distanceToSqr(
                MOON_POSITION.x, MOON_POSITION.y, MOON_POSITION.z));

        if (moonDist < TRIGGER_LAND_MOON) {
            lastTransitionTime = tick;
            mc.player.sendSystemMessage(
                    net.minecraft.network.chat.Component.literal(
                            "\u00a7b[CosmicVoyage] Initiating lunar landing...")
            );

            if (mc.player.getVehicle() instanceof ShipEntity ship) {
                CosmicVoyagePacketHandler.INSTANCE.sendToServer(
                        new MoonTransitionPacket(
                                ship.getX(), ship.getY(), ship.getZ(),
                                ship.getYRot(), ship.getXRot(),
                                0, -0.1, 0
                        )
                );
            } else {
                CosmicVoyagePacketHandler.INSTANCE.sendToServer(
                        new MoonTransitionPacket(
                                mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                                mc.player.getYRot(), mc.player.getXRot(),
                                0, -0.1, 0
                        )
                );
            }
            return;
        }

        // 检测地球着陆（返回地球）
        double earthDist = Math.sqrt(mc.player.distanceToSqr(
                EARTH_POSITION.x, EARTH_POSITION.y, EARTH_POSITION.z));

        if (earthDist < TRIGGER_LAND_EARTH) {
            lastTransitionTime = tick;
            mc.player.sendSystemMessage(
                    net.minecraft.network.chat.Component.literal(
                            "\u00a7b[CosmicVoyage] Approaching Earth. Initiating atmospheric entry...")
            );

            CosmicVoyagePacketHandler.INSTANCE.sendToServer(new LandOnEarthPacket());
        }
    }

    /**
     * 月球维度处理：检测 y 坐标，达到阈值时返回太空。
     */
    private static void handleMoonDimension(Minecraft mc, long tick) {
        if (mc.player.getY() < MOON_ESCAPE_HEIGHT) return;

        lastTransitionTime = tick;
        mc.player.sendSystemMessage(
                net.minecraft.network.chat.Component.literal(
                        "\u00a7b[CosmicVoyage] Leaving lunar atmosphere. Returning to space...")
        );

        CosmicVoyagePacketHandler.INSTANCE.sendToServer(new ReturnToSpacePacket());
    }
}