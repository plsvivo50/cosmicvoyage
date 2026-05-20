package com.Ray1101.cosmicvoyage.network.packet;

import com.Ray1101.cosmicvoyage.entity.ShipEntity;
import com.Ray1101.cosmicvoyage.network.CosmicVoyagePacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/**
 * 飞船同步数据包 — 客户端权威同步方案。
 *
 * <p>职责：将客户端计算的飞船状态（位置/姿态/速度）发送到服务端，
 * 服务端信任客户端数据并应用到飞船实体。
 *
 * <p>同步时序：
 *   - 客户端每 tick 计算物理 → 发送 ShipSyncPacket
 *   - 服务端调用 {@link ShipEntity#applySyncPacket} 一次性更新所有字段
 *   - 不直接操作 Entity 字段（P0-1 封装）
 */
public class ShipSyncPacket {

    public int entityId;
    public double x, y, z;
    public float yaw, pitch;
    public double vx, vy, vz;

    public ShipSyncPacket() {}

    public ShipSyncPacket(ShipEntity ship) {
        this.entityId = ship.getId();
        this.x = ship.getX();
        this.y = ship.getY();
        this.z = ship.getZ();
        this.yaw = ship.getYRot();
        this.pitch = ship.getXRot();
        // P0-1：使用 getShipVelocity() 替代直接访问 shipVelocity 字段
        this.vx = ship.getShipVelocity().x;
        this.vy = ship.getShipVelocity().y;
        this.vz = ship.getShipVelocity().z;
    }

    public static void encode(ShipSyncPacket pkt, FriendlyByteBuf buf) {
        buf.writeInt(pkt.entityId);
        buf.writeDouble(pkt.x);
        buf.writeDouble(pkt.y);
        buf.writeDouble(pkt.z);
        buf.writeFloat(pkt.yaw);
        buf.writeFloat(pkt.pitch);
        buf.writeDouble(pkt.vx);
        buf.writeDouble(pkt.vy);
        buf.writeDouble(pkt.vz);
    }

    public static ShipSyncPacket decode(FriendlyByteBuf buf) {
        ShipSyncPacket pkt = new ShipSyncPacket();
        pkt.entityId = buf.readInt();
        pkt.x = buf.readDouble();
        pkt.y = buf.readDouble();
        pkt.z = buf.readDouble();
        pkt.yaw = buf.readFloat();
        pkt.pitch = buf.readFloat();
        pkt.vx = buf.readDouble();
        pkt.vy = buf.readDouble();
        pkt.vz = buf.readDouble();
        return pkt;
    }

    /**
     * 服务端处理：验证偏差后调用 {@link ShipEntity#applySyncPacket}。
     *
     * <p>关键：只调用封装方法，不直接操作任何 Entity 字段。
     * <p>5月11日事故教训：修改同步时序会破坏零回跳修复，此处只移动代码位置。
     */
    public static void handle(ShipSyncPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            if (!(player.getVehicle() instanceof ShipEntity ship)) return;
            if (ship.getId() != pkt.entityId) return;

            double dx = Math.abs(ship.getX() - pkt.x);
            double dy = Math.abs(ship.getY() - pkt.y);
            double dz = Math.abs(ship.getZ() - pkt.z);
            if (dx > 10.0 || dy > 10.0 || dz > 10.0) {
                // 偏差过大：服务端回写正确状态给客户端
                CosmicVoyagePacketHandler.INSTANCE.send(
                        PacketDistributor.PLAYER.with(() -> player),
                        new ShipSyncPacket(ship)
                );
                return;
            }

            // P0-1：使用 applySyncPacket 替代直接字段操作
            ship.applySyncPacket(pkt.x, pkt.y, pkt.z, pkt.yaw, pkt.pitch, pkt.vx, pkt.vy, pkt.vz);
        });
        ctx.get().setPacketHandled(true);
    }
}