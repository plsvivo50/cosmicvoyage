package com.Ray1101.cosmicvoyage.network.packet;

import com.Ray1101.cosmicvoyage.SpaceConstants;
import com.Ray1101.cosmicvoyage.data.SpaceData;
import com.Ray1101.cosmicvoyage.dimension.ModDimensions;
import com.Ray1101.cosmicvoyage.entity.ShipEntity;
import com.Ray1101.cosmicvoyage.space.SpaceState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.EnumSet;
import java.util.function.Supplier;

/**
 * 返回太空数据包 — 从月球维度 y≥阈值 自动触发传送到太空。
 *
 * <p>触发条件：玩家在月球维度骑乘飞船，y 坐标 ≥ MOON_ESCAPE_HEIGHT。
 * <p>职责：将玩家+飞船从月球传送到太空安全坐标。
 */
public class ReturnToSpacePacket {

    public static final double MOON_ESCAPE_HEIGHT = 200.0;

    public ReturnToSpacePacket() {}

    public static void encode(ReturnToSpacePacket pkt, FriendlyByteBuf buf) {
        // 空包，无数据
    }

    public static ReturnToSpacePacket decode(FriendlyByteBuf buf) {
        return new ReturnToSpacePacket();
    }

    /**
     * 服务端处理：将玩家和飞船从月球传送到太空。
     *
     * <p>流程：
     *   1. 保存月球锚点（P0-2：使用 setMoonAnchor）
     *   2. 传送玩家+飞船到太空安全坐标（P0-6：使用 SpaceConstants.SPACE_ENTRY_POS）
     *   3. 注册玩家在太空的状态
     */
    public static void handle(ReturnToSpacePacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            // 验证当前在月球维度
            if (!player.level().dimension().equals(ModDimensions.MOON)) return;

            ServerLevel spaceLevel = player.getServer().getLevel(ModDimensions.SPACE);
            if (spaceLevel == null) return;

            // P0-2：保存月球锚点
            SpaceData.get(player.serverLevel()).setMoonAnchor(
                    player.getX(), player.getY(), player.getZ()
            );

            // P0-6：使用 SpaceConstants 推导的太空进入坐标
            double entryX = SpaceConstants.SPACE_ENTRY_POS.x;
            double entryY = SpaceConstants.SPACE_ENTRY_POS.y;
            double entryZ = SpaceConstants.SPACE_ENTRY_POS.z;

            if (player.getVehicle() instanceof ShipEntity oldShip) {
                EntityType<?> shipType = oldShip.getType();
                float yaw = oldShip.getYRot();

                player.stopRiding();

                player.teleportTo(spaceLevel, entryX, entryY, entryZ,
                        EnumSet.noneOf(net.minecraft.world.entity.RelativeMovement.class),
                        yaw, 0.0f);

                ShipEntity newShip = (ShipEntity) shipType.create(spaceLevel);
                if (newShip != null) {
                    newShip.moveTo(entryX, entryY, entryZ, yaw, 0.0f);
                    newShip.setShipVelocity(Vec3.ZERO);
                    newShip.setDeltaMovement(Vec3.ZERO);
                    newShip.hasImpulse = true;

                    spaceLevel.addFreshEntity(newShip);
                    player.startRiding(newShip, true);
                }

                oldShip.discard();

            } else {
                player.teleportTo(spaceLevel, entryX, entryY, entryZ,
                        EnumSet.noneOf(net.minecraft.world.entity.RelativeMovement.class),
                        player.getYRot(), 0.0f);
            }

            SpaceState.enterSpace(player.getUUID());
        });
        ctx.get().setPacketHandled(true);
    }
}