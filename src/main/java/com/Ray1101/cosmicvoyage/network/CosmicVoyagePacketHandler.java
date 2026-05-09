package com.Ray1101.cosmicvoyage.network;

import com.Ray1101.cosmicvoyage.CosmicVoyage;
import com.Ray1101.cosmicvoyage.entity.ShipEntity;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

public class CosmicVoyagePacketHandler {

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(CosmicVoyage.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    public static void register() {
        INSTANCE.registerMessage(
                packetId++,
                ShipSyncPacket.class,
                ShipSyncPacket::encode,
                ShipSyncPacket::decode,
                CosmicVoyagePacketHandler::handleShipSync
        );
    }

    // ===== ShipSyncPacket =====

    public static class ShipSyncPacket {
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
            this.vx = ship.shipVelocity.x;
            this.vy = ship.shipVelocity.y;
            this.vz = ship.shipVelocity.z;
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
    }

    // ===== 服务端处理 =====

    private static void handleShipSync(ShipSyncPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            Level level = player.level();
            Entity entity = level.getEntity(pkt.entityId);
            if (!(entity instanceof ShipEntity ship)) return;

            // 服务端：直接信任客户端，不做任何校验
            ship.setPos(pkt.x, pkt.y, pkt.z);
            ship.setYRot(pkt.yaw);
            ship.setXRot(pkt.pitch);
            ship.setYHeadRot(pkt.yaw);
            ship.shipVelocity = new net.minecraft.world.phys.Vec3(pkt.vx, pkt.vy, pkt.vz);
            ship.setDeltaMovement(ship.shipVelocity);

            // 同步乘客位置（服务端侧）
            if (!ship.getPassengers().isEmpty()) {
                for (Entity passenger : ship.getPassengers()) {
                    passenger.setPos(pkt.x, pkt.y + 0.8, pkt.z);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}