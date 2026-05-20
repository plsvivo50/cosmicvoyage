package com.Ray1101.cosmicvoyage.network.packet;

import com.Ray1101.cosmicvoyage.SpaceConstants;
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
 * 月球过渡数据包 — 从太空维度传送到月球维度。
 *
 * <p>职责：处理太空→月球的维度切换，包括：
 *   - 在月球维度创建新飞船
 *   - 保持飞船速度和姿态
 *   - 销毁旧维度飞船
 */
public class MoonTransitionPacket {

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

    /**
     * 服务端处理：将玩家和飞船从太空传送到月球。
     *
     * <p>冻结逻辑（不动）：维度切换时序、飞船创建流程、位置计算。
     */
    public static void handle(MoonTransitionPacket pkt, Supplier<NetworkEvent.Context> ctx) {
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

                player.teleportTo(moonLevel,
                        SpaceConstants.MOON_ENTRY_POS.x, SpaceConstants.MOON_ENTRY_POS.y, SpaceConstants.MOON_ENTRY_POS.z,
                        EnumSet.noneOf(net.minecraft.world.entity.RelativeMovement.class),
                        yaw, 0.0f);

                ShipEntity newShip = (ShipEntity) shipType.create(moonLevel);
                if (newShip == null) return;

                newShip.moveTo(SpaceConstants.MOON_ENTRY_POS.x, SpaceConstants.MOON_ENTRY_POS.y, SpaceConstants.MOON_ENTRY_POS.z, yaw, 0.0f);
                // P0-1：使用 setter 替代直接字段访问
                newShip.setShipVelocity(entryVelocity.scale(0.5));
                newShip.setDeltaMovement(newShip.getShipVelocity());
                newShip.hasImpulse = true;

                moonLevel.addFreshEntity(newShip);
                player.startRiding(newShip, true);

                oldShip.discard();

            } else {
                player.teleportTo(moonLevel,
                        SpaceConstants.MOON_ENTRY_POS.x, SpaceConstants.MOON_ENTRY_POS.y, SpaceConstants.MOON_ENTRY_POS.z,
                        EnumSet.noneOf(net.minecraft.world.entity.RelativeMovement.class),
                        pkt.yaw, 0.0f);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}