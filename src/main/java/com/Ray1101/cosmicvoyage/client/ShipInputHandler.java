package com.Ray1101.cosmicvoyage.client;

import com.Ray1101.cosmicvoyage.CosmicVoyage;
import com.Ray1101.cosmicvoyage.entity.ShipEntity;
import com.Ray1101.cosmicvoyage.network.CosmicVoyagePacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CosmicVoyage.MOD_ID, value = Dist.CLIENT)
public class ShipInputHandler {

    private static int syncTick = 0;
    private static final int SYNC_INTERVAL = 3;
    private static int launchCooldown = 0;

    // === 6DoF 重构：鼠标跟踪 ===
    private static double lastMouseX = 0;
    private static double lastMouseY = 0;
    private static boolean mouseInitialized = false;
    private static final float MOUSE_SENSITIVITY = 0.15f;

    // === 问题3修复：三层防御 ===
    // 根因不明：按住空格时原版某处周期性设 sprinting=true。
    // PlayerTickEvent.START 可能因 Forge 时序问题未能拦截。
    // 策略：tick 中 + ClientTick END + RenderTick START 三重设 false。

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // 两 phase 都处理，确保不遗漏
        if (!(event.player instanceof LocalPlayer player)) return;
        if (!(player.getVehicle() instanceof ShipEntity)) return;

        player.input.jumping = false;
        player.setJumping(false);
        player.setSprinting(false);
    }

    @SubscribeEvent
    public static void onRenderTickStart(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !(mc.player.getVehicle() instanceof ShipEntity)) return;
        // FOV 计算在渲染阶段，确保此时 sprinting 必为 false
        mc.player.setSprinting(false);
    }

    // === 问题3根治：FOVModifierEvent 直接锁定 ===
    // 根因不明（可能 movement speed 属性波动 / sprinting / 其他），
    // 不再在原版状态上打补丁。直接在 FOV 计算结果上覆盖，零副作用。
    @SubscribeEvent
    public static void onFovModifier(ComputeFovModifierEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !(mc.player.getVehicle() instanceof ShipEntity)) return;
        event.setNewFovModifier(1.0f);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();

        // 未骑乘时重置鼠标状态
        if (mc.player == null || !(mc.player.getVehicle() instanceof ShipEntity ship)) {
            mouseInitialized = false;
            return;
        }

        // === 主世界高度检测：y>=300 自动发射到太空（冻结）===
        if (mc.level.dimension().equals(Level.OVERWORLD) && mc.player.getY() >= 300.0) {
            if (launchCooldown <= 0) {
                launchCooldown = 100;
                CosmicVoyagePacketHandler.INSTANCE.sendToServer(
                        new CosmicVoyagePacketHandler.LaunchToSpacePacket()
                );
            }
        }
        if (launchCooldown > 0) launchCooldown--;

        if (ship.isAutoLanding) return;

        // === 下降键：Left Ctrl（冻结）===
        boolean up = mc.options.keyJump.isDown();
        boolean down = mc.options.keySprint.isDown();

        if (up) {
            mc.player.input.jumping = false;
            mc.player.setJumping(false);
            mc.player.setSprinting(false); // 阻止原版空格触发疾跑FOV缩放
        }

        // === 6DoF 重构：鼠标输入捕获 ===
        MouseHandler mouse = mc.mouseHandler;
        double mouseX = mouse.xpos();
        double mouseY = mouse.ypos();

        if (!mouseInitialized) {
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            mouseInitialized = true;
        }

        float yawDelta = (float) (mouseX - lastMouseX) * MOUSE_SENSITIVITY;
        // 屏幕坐标 Y 轴向下为正。鼠标向上移动（Y减小）应抬头（pitch 减小）。
        // mouseY - lastMouseY：向上为负，pitchDelta 为负 = 抬头，方向正确。
        float pitchDelta = (float) (mouseY - lastMouseY) * MOUSE_SENSITIVITY;

        lastMouseX = mouseX;
        lastMouseY = mouseY;

        // === 6DoF 重构：运行飞船物理 ===
        // A/D 改为左右平移（strafe），鼠标控制偏航+俯仰
        ship.clientTickPhysics(
                mc.options.keyUp.isDown(),       // W - 沿机头向前
                mc.options.keyDown.isDown(),     // S - 沿机头向后
                mc.options.keyLeft.isDown(),     // A - 左平移（strafe）
                mc.options.keyRight.isDown(),    // D - 右平移（strafe）
                up, down,
                pitchDelta, yawDelta
        );

        // === 6DoF 重构：同步玩家摄像机到飞船姿态 ===
        mc.player.setYRot(ship.getYRot());
        mc.player.yRotO = ship.yRotO;
        mc.player.setXRot(ship.getXRot());
        mc.player.xRotO = ship.xRotO;

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

        // === 网络同步（节流，冻结）===
        syncTick++;
        if (syncTick >= SYNC_INTERVAL) {
            syncTick = 0;
            CosmicVoyagePacketHandler.INSTANCE.sendToServer(
                    new CosmicVoyagePacketHandler.ShipSyncPacket(ship)
            );
        }
    }
}