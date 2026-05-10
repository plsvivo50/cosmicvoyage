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

/**
 * 逼近过渡系统 - 自动着陆序列
 */
@Mod.EventBusSubscriber(modid = CosmicVoyage.MOD_ID, value = Dist.CLIENT)
public class TransitionHandler {

    public static final Vec3 MOON_POSITION = MoonRenderer.MOON_POSITION;
    public static final double TRIGGER_ORBIT = 500.0;   // 轨道进入提示
    public static final double TRIGGER_LAND = 200.0;    // 着陆触发

    private static long lastTransitionTime = 0;
    private static final long COOLDOWN = 100;

    // 着陆状态机
    private static enum LandingState { IDLE, ORBITING, LANDING }
    private static LandingState state = LandingState.IDLE;
    private static int landingTick = 0;
    private static final int LANDING_DURATION = 60; // 3秒 (20tick/秒)

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (!mc.level.dimension().equals(ModDimensions.SPACE)) {
            state = LandingState.IDLE;
            return;
        }

        if (!(mc.player.getVehicle() instanceof ShipEntity ship)) return;

        long tick = mc.level.getGameTime();
        if (tick - lastTransitionTime < COOLDOWN) return;

        double dist = ship.distanceToSqr(MOON_POSITION.x, MOON_POSITION.y, MOON_POSITION.z);
        double distHoriz = Math.sqrt(dist);

        switch (state) {
            case IDLE:
                if (distHoriz < TRIGGER_ORBIT) {
                    state = LandingState.ORBITING;
                    mc.player.sendSystemMessage(
                            net.minecraft.network.chat.Component.literal("§b[CosmicVoyage] Entered lunar orbit. Approaching surface...")
                    );
                }
                break;

            case ORBITING:
                if (distHoriz < TRIGGER_LAND) {
                    state = LandingState.LANDING;
                    landingTick = 0;
                    mc.player.sendSystemMessage(
                            net.minecraft.network.chat.Component.literal("§b[CosmicVoyage] Landing sequence initiated. Auto-pilot engaged.")
                    );
                }
                break;

            // LANDING 状态：指向月球中心的垂直下降
            case LANDING:
                landingTick++;

                // 计算指向月球中心的方向向量
                Vec3 moonCenter = MOON_POSITION;
                Vec3 toMoon = new Vec3(
                    moonCenter.x - ship.getX(),
                    moonCenter.y - ship.getY(),
                    moonCenter.z - ship.getZ()
                ).normalize();

                // 速度向量指向月球中心（头朝下垂直下降）
                double landSpeed = 0.5;
                ship.shipVelocity = toMoon.scale(landSpeed);

                // 设置 targetPitch 让 ShipEntity 自动平滑过渡姿态
                ship.targetPitch = -90.0f;

                // 每 tick 移动
                ship.move(net.minecraft.world.entity.MoverType.SELF, ship.shipVelocity);
                
                if (landingTick >= LANDING_DURATION) {
                    lastTransitionTime = tick;
                    state = LandingState.IDLE;
                    ship.targetPitch = 0.0f; // 恢复

                    // 发送过渡包到服务端
                    CosmicVoyagePacketHandler.INSTANCE.sendToServer(
                            new CosmicVoyagePacketHandler.MoonTransitionPacket(
                                    ship.getX(), ship.getY(), ship.getZ(),
                                    ship.getYRot(), ship.getXRot(),
                                    toMoon.x * 0.1, toMoon.y * 0.1, toMoon.z * 0.1
                            )
                    );

                    mc.player.sendSystemMessage(
                            net.minecraft.network.chat.Component.literal("§b[CosmicVoyage] Touchdown.")
                    );
                }
                break;
        }
    }
}