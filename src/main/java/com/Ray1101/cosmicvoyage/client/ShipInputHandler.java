package com.Ray1101.cosmicvoyage.client;

import com.Ray1101.cosmicvoyage.CosmicVoyage;
import com.Ray1101.cosmicvoyage.entity.ShipEntity;
import com.Ray1101.cosmicvoyage.network.CosmicVoyagePacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CosmicVoyage.MOD_ID, value = Dist.CLIENT)
public class ShipInputHandler {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !(mc.player.getVehicle() instanceof ShipEntity ship)) return;

        // 1. 运行物理
        ship.clientTickPhysics(
                mc.options.keyUp.isDown(),
                mc.options.keyDown.isDown(),
                mc.options.keyLeft.isDown(),
                mc.options.keyRight.isDown(),
                mc.options.keyJump.isDown(),
                mc.options.keySprint.isDown()
        );

        // 2. 强制同步玩家位置到飞船（防止 LocalPlayer 网络预测导致脱节）
        // 直接设置位置，不触发网络同步（LocalPlayer.setPos 客户端不发包）
        double targetX = ship.getX();
        double targetY = ship.getY() + 0.8;
        double targetZ = ship.getZ();

        // 只在有偏差时设置，避免不必要的调用
        if (mc.player.distanceToSqr(targetX, targetY, targetZ) > 0.01) {
            mc.player.setPos(targetX, targetY, targetZ);
        }

        // 3. 发自定义网络包到服务端
        CosmicVoyagePacketHandler.INSTANCE.sendToServer(new CosmicVoyagePacketHandler.ShipSyncPacket(ship));
    }
}