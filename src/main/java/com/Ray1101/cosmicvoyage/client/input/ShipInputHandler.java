package com.Ray1101.cosmicvoyage.client.input;

import com.Ray1101.cosmicvoyage.CosmicVoyage;
import com.Ray1101.cosmicvoyage.dimension.ModDimensions;
import com.Ray1101.cosmicvoyage.network.packet.ReturnToSpacePacket;
import com.Ray1101.cosmicvoyage.SpaceConstants;
import com.Ray1101.cosmicvoyage.entity.ShipEntity;
import com.Ray1101.cosmicvoyage.network.CosmicVoyagePacketHandler;
import com.Ray1101.cosmicvoyage.network.packet.LaunchToSpacePacket;
import com.Ray1101.cosmicvoyage.network.packet.ShipSyncPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.fml.common.Mod;
import com.mojang.blaze3d.platform.InputConstants;

@Mod.EventBusSubscriber(modid = CosmicVoyage.MOD_ID, value = Dist.CLIENT)
public class ShipInputHandler {

    private static int syncTick = 0;
    private static final int SYNC_INTERVAL = 3;
    private static int launchCooldown = 0;

    // P1-4：下降键独立 KeyMapping（原用 LeftCtrl / keySprint，现独立注册避免冲突）
    public static final net.minecraft.client.KeyMapping KEY_DESCEND = new net.minecraft.client.KeyMapping(
            "key.cosmicvoyage.descend",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_LCONTROL,
            "key.category.cosmicvoyage"
    );

    // === 6DoF 重构：鼠标跟踪 ===
    private static double lastMouseX = 0;
    private static double lastMouseY = 0;
    private static boolean mouseInitialized = false;
    private static final float MOUSE_SENSITIVITY = 0.15f;
    /** 位置同步距离阈值：偏差超过此值才发送同步包 */
    private static final double SYNC_DISTANCE_THRESHOLD = 0.25;

    // P1-4：注册下降键 KeyMapping
    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(KEY_DESCEND);
    }

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

        // P2-3：GUI 打开时暂停飞船物理（防止背包/菜单打开时飞船继续飞行）
        if (Minecraft.getInstance().screen != null) return;

        Minecraft mc = Minecraft.getInstance();

        // 未骑乘时重置鼠标状态
        if (mc.player == null || !(mc.player.getVehicle() instanceof ShipEntity ship)) {
            mouseInitialized = false;
            return;
        }

        // === 主世界高度检测：y>=LAUNCH_HEIGHT 自动发射到太空（冻结）===
        if (mc.level.dimension().equals(Level.OVERWORLD) && mc.player.getY() >= SpaceConstants.LAUNCH_HEIGHT) {
            if (launchCooldown <= 0) {
                launchCooldown = 100;
                CosmicVoyagePacketHandler.INSTANCE.sendToServer(
                        new LaunchToSpacePacket()
                );
            }
        }
        // === P1-5：月球维度 y>=MOON_ESCAPE_HEIGHT 自动返回太空 ===
        if (mc.level.dimension().equals(ModDimensions.MOON) && mc.player.getY() >= SpaceConstants.MOON_ESCAPE_HEIGHT) {
            if (launchCooldown <= 0) {
                launchCooldown = 100;
                CosmicVoyagePacketHandler.INSTANCE.sendToServer(
                        new ReturnToSpacePacket()
                );
            }
        }
        if (launchCooldown > 0) launchCooldown--;

        if (ship.isAutoLanding) return;

        // P1-4：下降键使用独立 KeyMapping（原 keySprint / LeftCtrl）
        boolean up = mc.options.keyJump.isDown();
        boolean down = KEY_DESCEND.isDown();

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
        mc.player.setYRot(net.minecraft.util.Mth.wrapDegrees(ship.getYRot()));
        mc.player.yRotO = ship.yRotO;
        mc.player.setXRot(ship.getXRot());
        mc.player.xRotO = ship.xRotO;

        // === 同步玩家位置到飞船（冻结，不动）===
        double targetX = ship.getX();
        double targetY = ship.getY() + ship.getPassengersRidingOffset() + SpaceConstants.SHIP_PASSENGER_OFFSET_Y;
        double targetZ = ship.getZ();

        if (mc.player.distanceToSqr(targetX, targetY, targetZ) > SYNC_DISTANCE_THRESHOLD) {
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
                    new ShipSyncPacket(ship)
            );
        }
    }
}