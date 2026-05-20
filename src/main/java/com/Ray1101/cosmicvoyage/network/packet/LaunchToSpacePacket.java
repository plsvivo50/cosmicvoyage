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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.EnumSet;
import java.util.function.Supplier;

/**
 * 发射到太空数据包 — 从主世界 y≥300 触发传送到太空维度。
 *
 * <p>职责：处理主世界→太空的维度切换，包括：
 *   - 保存地球锚点（P0-2：使用 setEarthAnchor）
 *   - 传送玩家和飞船到太空安全坐标（P0-6：使用 SpaceConstants.SPACE_ENTRY_POS）
 *   - 注册玩家在太空的状态
 */
public class LaunchToSpacePacket {

    public LaunchToSpacePacket() {}

    public static void encode(LaunchToSpacePacket pkt, FriendlyByteBuf buf) {
        // 空包，无数据
    }

    public static LaunchToSpacePacket decode(FriendlyByteBuf buf) {
        return new LaunchToSpacePacket();
    }

    /**
     * 服务端处理：将玩家和飞船从主世界传送到太空。
     *
     * <p>P0-6 修改：传送坐标从硬编码 (0, 200, -600) 改为 SpaceConstants.SPACE_ENTRY_POS，
     * 数学上保证距离地球 > TRIGGER_LAND_EARTH，避免死循环。
     */
    public static void handle(LaunchToSpacePacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            // 只在主世界触发
            if (!player.level().dimension().equals(Level.OVERWORLD)) return;

            ServerLevel spaceLevel = player.getServer().getLevel(ModDimensions.SPACE);
            if (spaceLevel == null) return;

            // P0-2：保存地球锚点（API 已从 setAnchor 改为 setEarthAnchor）
            SpaceData.get(player.serverLevel()).setEarthAnchor(
                    player.getX(), player.getY(), player.getZ()
            );

            // P0-6：使用 SpaceConstants 推导的太空进入坐标
            double entryX = SpaceConstants.SPACE_ENTRY_POS.x;
            double entryY = SpaceConstants.SPACE_ENTRY_POS.y;
            double entryZ = SpaceConstants.SPACE_ENTRY_POS.z;

            if (player.getVehicle() instanceof ShipEntity oldShip) {
                EntityType<?> shipType = oldShip.getType();
                float yaw = oldShip.getYRot();

                // 1. 玩家下船
                player.stopRiding();

                // 2. 传送玩家到太空（安全坐标，距离地球 > 触发阈值）
                player.teleportTo(spaceLevel, entryX, entryY, entryZ,
                        EnumSet.noneOf(net.minecraft.world.entity.RelativeMovement.class),
                        yaw, 0.0f);

                // 3. 在太空创建新飞船
                ShipEntity newShip = (ShipEntity) shipType.create(spaceLevel);
                if (newShip != null) {
                    newShip.moveTo(entryX, entryY, entryZ, yaw, 0.0f);
                    // P0-1：使用 setter 替代直接字段访问
                    newShip.setShipVelocity(Vec3.ZERO);
                    newShip.setDeltaMovement(Vec3.ZERO);
                    newShip.hasImpulse = true;

                    spaceLevel.addFreshEntity(newShip);
                    player.startRiding(newShip, true);
                }

                // 4. 销毁旧维度飞船
                oldShip.discard();

            } else {
                // 没骑飞船，只传送玩家
                player.teleportTo(spaceLevel, entryX, entryY, entryZ,
                        EnumSet.noneOf(net.minecraft.world.entity.RelativeMovement.class),
                        player.getYRot(), 0.0f);
            }

            SpaceState.enterSpace(player.getUUID());
        });
        ctx.get().setPacketHandled(true);
    }
}