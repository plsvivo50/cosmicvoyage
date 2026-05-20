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
 * 着陆地球数据包 — 从太空维度飞行返回地球。
 *
 * <p>触发条件：玩家在太空维度骑乘飞船，距离地球中心 < TRIGGER_LAND_EARTH。
 * <p>职责：读取地球锚点，将玩家+飞船从太空传送到主世界锚点位置。
 */
public class LandOnEarthPacket {

    public LandOnEarthPacket() {}

    public static void encode(LandOnEarthPacket pkt, FriendlyByteBuf buf) {
        // 空包，无数据
    }

    public static LandOnEarthPacket decode(FriendlyByteBuf buf) {
        return new LandOnEarthPacket();
    }

    /**
     * 服务端处理：将玩家和飞船从太空传送到地球锚点位置。
     *
     * <p>流程：
     *   1. 读取 SpaceData 的地球锚点
     *   2. 传送玩家+飞船到主世界锚点位置
     *   3. 清理太空状态标记
     */
    public static void handle(LandOnEarthPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            // 验证当前在太空维度
            if (!player.level().dimension().equals(ModDimensions.SPACE)) return;

            ServerLevel overworld = player.getServer().overworld();
            if (overworld == null) return;

            // 读取地球锚点
            SpaceData data = SpaceData.get(overworld);
            double targetX, targetY, targetZ;

            if (data.hasEarthAnchor()) {
                targetX = data.getEarthX();
                targetY = data.getEarthY();
                targetZ = data.getEarthZ();
            } else {
                // 无锚点：传送到主世界出生点（安全 fallback）
                targetX = overworld.getSharedSpawnPos().getX();
                targetY = overworld.getSharedSpawnPos().getY();
                targetZ = overworld.getSharedSpawnPos().getZ();
            }

            // 返回地球时强制 Y < LAUNCH_HEIGHT，防止锚点 Y≥300 时死循环
            double safeY = Math.min(targetY + 2.0, SpaceConstants.LAUNCH_HEIGHT - 10.0);

            if (player.getVehicle() instanceof ShipEntity oldShip) {
                EntityType<?> shipType = oldShip.getType();
                float yaw = oldShip.getYRot();

                Vec3 entryVelocity = oldShip.getShipVelocity();

                player.stopRiding();

                // 传送到地球锚点（safeY 确保不会立刻触发自动发射）
                player.teleportTo(overworld, targetX, safeY, targetZ,
                        EnumSet.noneOf(net.minecraft.world.entity.RelativeMovement.class),
                        yaw, 0.0f);

                ShipEntity newShip = (ShipEntity) shipType.create(overworld);
                if (newShip != null) {
                    newShip.moveTo(targetX, safeY, targetZ, yaw, 0.0f);
                    newShip.setShipVelocity(entryVelocity.scale(SpaceConstants.ENTRY_VELOCITY_SCALE));
                    newShip.setDeltaMovement(newShip.getShipVelocity());
                    newShip.hasImpulse = true;

                    overworld.addFreshEntity(newShip);
                    player.startRiding(newShip, true);
                }

                oldShip.discard();

            } else {
                player.teleportTo(overworld, targetX, safeY, targetZ,
                        EnumSet.noneOf(net.minecraft.world.entity.RelativeMovement.class),
                        player.getYRot(), 0.0f);
            }

            SpaceState.exitSpace(player.getUUID());
        });
        ctx.get().setPacketHandled(true);
    }
}