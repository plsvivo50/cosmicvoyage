package com.Ray1101.cosmicvoyage.client;

import com.Ray1101.cosmicvoyage.CosmicVoyage;
import com.Ray1101.cosmicvoyage.entity.ShipEntity;
import com.Ray1101.cosmicvoyage.network.CosmicVoyagePacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CosmicVoyage.MOD_ID, value = Dist.CLIENT)
public class ShipInputHandler {

    private static int syncTick = 0;
    private static final int SYNC_INTERVAL = 3;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !(mc.player.getVehicle() instanceof ShipEntity ship)) return;

        if (ship.isAutoLanding) return;

        // === 下降键：Left Ctrl（keySprint）===
        // Space 被 Forge 视为跳跃键，骑乘时可能触发原版跳跃逻辑
        // 解决方案：每 tick 强制重置跳跃状态，阻止原版逻辑
        boolean up = mc.options.keyJump.isDown();
        boolean down = mc.options.keySprint.isDown();

        if (up) {
            // 阻止原版 Space 跳跃行为
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

        // === 同步玩家位置到飞船（已有，不动）===
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