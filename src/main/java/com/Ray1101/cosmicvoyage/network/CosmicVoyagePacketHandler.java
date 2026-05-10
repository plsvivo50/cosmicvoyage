package com.Ray1101.cosmicvoyage.network;

import com.Ray1101.cosmicvoyage.CosmicVoyage;
import com.Ray1101.cosmicvoyage.dimension.ModDimensions;
import com.Ray1101.cosmicvoyage.entity.ShipEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.network.PacketDistributor;
import java.util.EnumSet;
import java.util.function.Function;
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

    // ===== ShipSync 服务端处理（已修复安全验证）=====

    private static void handleShipSync(ShipSyncPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            // ✅ 安全：必须从玩家当前骑乘的实体获取，禁止用 level.getEntity(entityId)
            if (!(player.getVehicle() instanceof ShipEntity ship)) return;
            if (ship.getId() != pkt.entityId) return; // 二次校验，防止伪造

            // 距离校验：客户端位置偏离不能太大（防作弊）
            double dx = Math.abs(ship.getX() - pkt.x);
            double dy = Math.abs(ship.getY() - pkt.y);
            double dz = Math.abs(ship.getZ() - pkt.z);
            if (dx > 10.0 || dy > 10.0 || dz > 10.0) {
                // 差距太大，拒绝更新，把服务端正确位置发回客户端校正
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

    // ===== MoonTransition 服务端处理（已修复跨维度传送）=====

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

                // 1. 保存旧飞船速度用于月球初速度
                Vec3 entryVelocity = new Vec3(pkt.vx, pkt.vy, pkt.vz);

                // 2. 先让玩家下船（防止骑乘关系在跨维度时断裂）
                player.stopRiding();

                // 3. 传送玩家到月球高空
                player.teleportTo(moonLevel, 0.0, 200.0, 0.0,
                        EnumSet.noneOf(net.minecraft.world.entity.RelativeMovement.class),
                        yaw, 0.0f);

                // 4. 在月球创建新飞船
                ShipEntity newShip = (ShipEntity) shipType.create(moonLevel);
                if (newShip == null) return;

                newShip.moveTo(0.0, 200.0, 0.0, yaw, 0.0f);
                newShip.shipVelocity = entryVelocity.scale(0.5); // 保留部分水平速度
                newShip.setDeltaMovement(newShip.shipVelocity);
                newShip.hasImpulse = true;

                // 5. 必须先加到世界，再让玩家骑乘
                moonLevel.addFreshEntity(newShip);
                player.startRiding(newShip, true);

                // 6. 销毁旧维度的飞船
                oldShip.discard();

            } else {
                // 玩家没骑飞船，只传送玩家
                player.teleportTo(moonLevel, 0.0, 70.0, 0.0,
                        EnumSet.noneOf(net.minecraft.world.entity.RelativeMovement.class),
                        pkt.yaw, 0.0f);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}