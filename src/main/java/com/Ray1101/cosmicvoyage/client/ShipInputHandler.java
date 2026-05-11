package com.Ray1101.cosmicvoyage.client;

import com.Ray1101.cosmicvoyage.CosmicVoyage;
import com.Ray1101.cosmicvoyage.entity.ShipEntity;
import com.Ray1101.cosmicvoyage.network.CosmicVoyagePacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CosmicVoyage.MOD_ID, value = Dist.CLIENT)
public class ShipInputHandler {

    private static int syncTick = 0;
    private static final int SYNC_INTERVAL = 3;
    private static int launchCooldown = 0;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !(mc.player.getVehicle() instanceof ShipEntity ship)) return;

        // === 主世界高度检测：y>=300 自动发射到太空 ===
        if (mc.level.dimension().equals(Level.OVERWORLD) && mc.player.getY() >= 300.0) {
            if (launchCooldown <= 0) {
                launchCooldown = 100; // 5秒冷却（20 ticks/秒）
                CosmicVoyagePacketHandler.INSTANCE.sendToServer(
                        new CosmicVoyagePacketHandler.LaunchToSpacePacket()
                );
            }
        }
        if (launchCooldown > 0) launchCooldown--;

        if (ship.isAutoLanding) return;

        // === 下降键：Left Ctrl（keySprint）===
        boolean up = mc.options.keyJump.isDown();
        boolean down = mc.options.keySprint.isDown();

        if (up) {
            mc.player.input.jumping = false;
            mc.player.setJumping(false);
        }

        // === 运行飞船物理 ===
        ship.clientTickPhysics(
                mc.options.keyUp.isDown(),
                mc.options.keyDown.isDown(),
                mc.options.keyLeft.isDown(),
                mc.options.keyRight.isDown(),
                up,
                down
        );

        // === 同步玩家位置到飞船（冻结，不动）===
        double targetX = ship.getX();
        double targetY = ship.getY() + ship.getPassengersRidingOffset() + 0.8;
        double targetZ = ship.getZ();

        if (mc.player.distanceToSqr(targetX, targetY, targetZ) > 0.25) {
            mc.player.setPos(targetX, targetY, targetZ);
            mc.player.xOld = targetX;
            mc.player.yOld = targetY;
            mc.player.zOld = targetZ;
            mc.player.setDeltaMovement(Vec3.ZERO);
        }

        // === 网络同步（节流）===
        syncTick++;
        if (syncTick >= SYNC_INTERVAL) {
            syncTick = 0;
            CosmicVoyagePacketHandler.INSTANCE.sendToServer(
                    new CosmicVoyagePacketHandler.ShipSyncPacket(ship)
            );
        }
    }
}