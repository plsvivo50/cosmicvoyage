package com.Ray1101.cosmicvoyage.network.packet;

import com.Ray1101.cosmicvoyage.SpaceConstants;
import com.Ray1101.cosmicvoyage.data.SpaceData;
import com.Ray1101.cosmicvoyage.dimension.ModDimensions;
import com.Ray1101.cosmicvoyage.entity.ShipEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.EnumSet;
import java.util.function.Supplier;

/**
 * 火星着陆数据包 — 从太空接近火星时触发维度切换。
 *
 * <p>职责：将玩家+飞船从太空维度传送到火星维度。
 * <p>使用 SpaceConstants.MARS_ENTRY_POS 作为进入坐标。
 */
public class MarsLandingPacket {

    private final double prevX, prevY, prevZ;
    private final float yRot, xRot;
    private final double vx, vy, vz;

    public MarsLandingPacket(double prevX, double prevY, double prevZ, float yRot, float xRot, double vx, double vy, double vz) {
        this.prevX = prevX;
        this.prevY = prevY;
        this.prevZ = prevZ;
        this.yRot = yRot;
        this.xRot = xRot;
        this.vx = vx;
        this.vy = vy;
        this.vz = vz;
    }

    public static void encode(MarsLandingPacket pkt, FriendlyByteBuf buf) {
        buf.writeDouble(pkt.prevX);
        buf.writeDouble(pkt.prevY);
        buf.writeDouble(pkt.prevZ);
        buf.writeFloat(pkt.yRot);
        buf.writeFloat(pkt.xRot);
        buf.writeDouble(pkt.vx);
        buf.writeDouble(pkt.vy);
        buf.writeDouble(pkt.vz);
    }

    public static MarsLandingPacket decode(FriendlyByteBuf buf) {
        return new MarsLandingPacket(
                buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readFloat(), buf.readFloat(),
                buf.readDouble(), buf.readDouble(), buf.readDouble()
        );
    }

    public static void handle(MarsLandingPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            ServerLevel marsLevel = player.getServer().getLevel(ModDimensions.MARS);
            if (marsLevel == null) return;

            // 保存火星锚点
            SpaceData.get(player.serverLevel()).setMarsAnchor(
                    pkt.prevX, pkt.prevY, pkt.prevZ
            );

            double entryX = SpaceConstants.MARS_ENTRY_POS.x;
            double entryY = SpaceConstants.MARS_ENTRY_POS.y;
            double entryZ = SpaceConstants.MARS_ENTRY_POS.z;

            if (player.getVehicle() instanceof ShipEntity oldShip) {
                EntityType<?> shipType = oldShip.getType();
                float yaw = oldShip.getYRot();

                player.stopRiding();

                player.teleportTo(marsLevel, entryX, entryY, entryZ,
                        EnumSet.noneOf(net.minecraft.world.entity.RelativeMovement.class),
                        yaw, 0.0f);

                ShipEntity newShip = (ShipEntity) shipType.create(marsLevel);
                if (newShip != null) {
                    newShip.moveTo(entryX, entryY, entryZ, yaw, 0.0f);
                    newShip.setShipVelocity(Vec3.ZERO);
                    newShip.setDeltaMovement(Vec3.ZERO);
                    newShip.hasImpulse = true;

                    marsLevel.addFreshEntity(newShip);
                    player.startRiding(newShip, true);
                }

                oldShip.discard();

            } else {
                player.teleportTo(marsLevel, entryX, entryY, entryZ,
                        EnumSet.noneOf(net.minecraft.world.entity.RelativeMovement.class),
                        player.getYRot(), 0.0f);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}