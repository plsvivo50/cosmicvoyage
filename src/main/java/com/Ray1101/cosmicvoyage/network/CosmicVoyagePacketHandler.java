package com.Ray1101.cosmicvoyage.network;

import com.Ray1101.cosmicvoyage.CosmicVoyage;
import com.Ray1101.cosmicvoyage.data.SpaceData;
import com.Ray1101.cosmicvoyage.dimension.ModDimensions;
import com.Ray1101.cosmicvoyage.entity.ShipEntity;
import com.Ray1101.cosmicvoyage.space.SpaceState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.EnumSet;
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

        INSTANCE.registerMessage(
                packetId++,
                MoonTransitionPacket.class,
                MoonTransitionPacket::encode,
                MoonTransitionPacket::decode,
                CosmicVoyagePacketHandler::handleMoonTransition
        );

        INSTANCE.registerMessage(
                packetId++,
                LaunchToSpacePacket.class,
                LaunchToSpacePacket::encode,
                LaunchToSpacePacket::decode,
                CosmicVoyagePacketHandler::handleLaunchToSpace
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

    // ===== MoonTransitionPacket =====

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

    // ===== LaunchToSpacePacket =====

    public static class LaunchToSpacePacket {
        public LaunchToSpacePacket() {}

        public static void encode(LaunchToSpacePacket pkt, FriendlyByteBuf buf) {
            // 空包，无数据
        }

        public static LaunchToSpacePacket decode(FriendlyByteBuf buf) {
            return new LaunchToSpacePacket();
        }
    }

    // ===== ShipSync 服务端处理（冻结，不动）=====

    private static void handleShipSync(ShipSyncPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            if (!(player.getVehicle() instanceof ShipEntity ship)) return;
            if (ship.getId() != pkt.entityId) return;

            double dx = Math.abs(ship.getX() - pkt.x);
            double dy = Math.abs(ship.getY() - pkt.y);
            double dz = Math.abs(ship.getZ() - pkt.z);
            if (dx > 10.0 || dy > 10.0 || dz > 10.0) {
                INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new ShipSyncPacket(ship));
                return;
            }

            ship.setPos(pkt.x, pkt.y, pkt.z);
            ship.setYRot(pkt.yaw);
            ship.setXRot(pkt.pitch);
            ship.setYHeadRot(pkt.yaw);
            ship.shipVelocity = new Vec3(pkt.vx, pkt.vy, pkt.vz);
            ship.setDeltaMovement(ship.shipVelocity);
            ship.hasImpulse = true;
        });
        ctx.get().setPacketHandled(true);
    }

    // ===== MoonTransition 服务端处理（冻结，不动）=====

    private static void handleMoonTransition(MoonTransitionPacket pkt,
                                             Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            ServerLevel moonLevel = player.getServer().getLevel(ModDimensions.MOON);
            if (moonLevel == null) return;

            if (player.getVehicle() instanceof ShipEntity oldShip) {
                EntityType<?> shipType = oldShip.getType();
                float yaw = pkt.yaw;

                Vec3 entryVelocity = new Vec3(pkt.vx, pkt.vy, pkt.vz);

                player.stopRiding();

                player.teleportTo(moonLevel, 0.0, 200.0, 0.0,
                        EnumSet.noneOf(net.minecraft.world.entity.RelativeMovement.class),
                        yaw, 0.0f);

                ShipEntity newShip = (ShipEntity) shipType.create(moonLevel);
                if (newShip == null) return;

                newShip.moveTo(0.0, 200.0, 0.0, yaw, 0.0f);
                newShip.shipVelocity = entryVelocity.scale(0.5);
                newShip.setDeltaMovement(newShip.shipVelocity);
                newShip.hasImpulse = true;

                moonLevel.addFreshEntity(newShip);
                player.startRiding(newShip, true);

                oldShip.discard();

            } else {
                player.teleportTo(moonLevel, 0.0, 70.0, 0.0,
                        EnumSet.noneOf(net.minecraft.world.entity.RelativeMovement.class),
                        pkt.yaw, 0.0f);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    // ===== LaunchToSpace 服务端处理（新增）=====

    private static void handleLaunchToSpace(LaunchToSpacePacket pkt,
                                            Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            // 只在主世界触发
            if (!player.level().dimension().equals(Level.OVERWORLD)) return;

            ServerLevel spaceLevel = player.getServer().getLevel(ModDimensions.SPACE);
            if (spaceLevel == null) return;

            // 保存地球锚点
            SpaceData.get(player.serverLevel()).setAnchor(
                    player.getX(), player.getY(), player.getZ()
            );

            if (player.getVehicle() instanceof ShipEntity oldShip) {
                EntityType<?> shipType = oldShip.getType();
                float yaw = oldShip.getYRot();

                // 1. 玩家下船
                player.stopRiding();

                // 2. 传送玩家到太空（-600z 确保距离地球>500，不死循环）
                player.teleportTo(spaceLevel, 0.0, 200.0, -600.0,
                        EnumSet.noneOf(net.minecraft.world.entity.RelativeMovement.class),
                        yaw, 0.0f);

                // 3. 在太空创建新飞船
                ShipEntity newShip = (ShipEntity) shipType.create(spaceLevel);
                if (newShip != null) {
                    newShip.moveTo(0.0, 200.0, -600.0, yaw, 0.0f);
                    newShip.shipVelocity = Vec3.ZERO;
                    newShip.setDeltaMovement(Vec3.ZERO);
                    newShip.hasImpulse = true;

                    spaceLevel.addFreshEntity(newShip);
                    player.startRiding(newShip, true);
                }

                // 4. 销毁旧维度飞船
                oldShip.discard();

            } else {
                // 没骑飞船，只传送玩家
                player.teleportTo(spaceLevel, 0.0, 200.0, -600.0,
                        EnumSet.noneOf(net.minecraft.world.entity.RelativeMovement.class),
                        player.getYRot(), 0.0f);
            }

            SpaceState.enterSpace(player.getUUID());
        });
        ctx.get().setPacketHandled(true);
    }
}