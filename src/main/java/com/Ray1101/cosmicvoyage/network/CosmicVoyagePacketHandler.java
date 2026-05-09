package com.Ray1101.cosmicvoyage.network;

import com.Ray1101.cosmicvoyage.CosmicVoyage;
import com.Ray1101.cosmicvoyage.dimension.ModDimensions;
import com.Ray1101.cosmicvoyage.entity.ShipEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
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

        // 新增：月球过渡包注册
        INSTANCE.registerMessage(
                packetId++,
                MoonTransitionPacket.class,
                MoonTransitionPacket::encode,
                MoonTransitionPacket::decode,
                CosmicVoyagePacketHandler::handleMoonTransition
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

    // ===== MoonTransitionPacket（新增）=====

    public static class MoonTransitionPacket {
        public double x, y, z;
        public float yaw, pitch;
        public double vx, vy, vz;

        public MoonTransitionPacket() {}

        public MoonTransitionPacket(double x, double y, double z, float yaw, float pitch,
                                    double vx, double vy, double vz) {
            this.x = x; this.y = y; this.z = z;
            this.yaw = yaw; this.pitch = pitch;
            this.vx = vx; this.vy = vy; this.vz = vz;
        }

        public static void encode(MoonTransitionPacket pkt, FriendlyByteBuf buf) {
            buf.writeDouble(pkt.x);
            buf.writeDouble(pkt.y);
            buf.writeDouble(pkt.z);
            buf.writeFloat(pkt.yaw);
            buf.writeFloat(pkt.pitch);
            buf.writeDouble(pkt.vx);
            buf.writeDouble(pkt.vy);
            buf.writeDouble(pkt.vz);
        }

        public static MoonTransitionPacket decode(FriendlyByteBuf buf) {
            return new MoonTransitionPacket(
                    buf.readDouble(), buf.readDouble(), buf.readDouble(),
                    buf.readFloat(), buf.readFloat(),
                    buf.readDouble(), buf.readDouble(), buf.readDouble()
            );
        }
    }

    // ===== ShipSync 服务端处理 =====

    private static void handleShipSync(ShipSyncPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            Level level = player.level();
            Entity entity = level.getEntity(pkt.entityId);
            if (!(entity instanceof ShipEntity ship)) return;

            double dx = Math.abs(ship.getX() - pkt.x);
            double dy = Math.abs(ship.getY() - pkt.y);
            double dz = Math.abs(ship.getZ() - pkt.z);
            if (dx > 1.0 || dy > 1.0 || dz > 1.0) {
                ship.setPos(pkt.x, pkt.y, pkt.z);
            }
            ship.setYRot(pkt.yaw);
            ship.setXRot(pkt.pitch);
            ship.setYHeadRot(pkt.yaw);
            ship.shipVelocity = new net.minecraft.world.phys.Vec3(pkt.vx, pkt.vy, pkt.vz);
            ship.setDeltaMovement(ship.shipVelocity);

            if (!ship.getPassengers().isEmpty()) {
                for (Entity passenger : ship.getPassengers()) {
                    passenger.setPos(pkt.x, pkt.y + 0.8, pkt.z);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    // ===== MoonTransition 服务端处理（新增）=====

    private static void handleMoonTransition(MoonTransitionPacket pkt,
                                             Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            ServerLevel moonLevel = player.getServer().getLevel(ModDimensions.MOON);
            if (moonLevel == null) return;

            if (player.getVehicle() instanceof ShipEntity ship) {
                // 传送点改为月球高空 (0, 200, 0)，面朝下
                ship.teleportTo(moonLevel, 0.0, 200.0, 0.0,
                        java.util.EnumSet.noneOf(net.minecraft.world.entity.RelativeMovement.class),
                        pkt.yaw, -90.0f); // pitch = -90 面朝下

                player.teleportTo(moonLevel, 0.0, 200.0, 0.0,
                        java.util.EnumSet.noneOf(net.minecraft.world.entity.RelativeMovement.class),
                        pkt.yaw, -90.0f);

                // 保留向下的微小速度
                ship.shipVelocity = new Vec3(0, -0.1, 0);
                ship.setDeltaMovement(ship.shipVelocity);
            } else {
                player.teleportTo(moonLevel, 0.0, 70.0, 0.0,
                        java.util.EnumSet.noneOf(net.minecraft.world.entity.RelativeMovement.class),
                        pkt.yaw, 0.0f);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}