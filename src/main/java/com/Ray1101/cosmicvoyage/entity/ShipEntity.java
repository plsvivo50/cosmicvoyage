package com.Ray1101.cosmicvoyage.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import com.Ray1101.cosmicvoyage.Config;
import com.Ray1101.cosmicvoyage.SpaceConstants;
import com.Ray1101.cosmicvoyage.dimension.ModDimensions;

/**
 * 飞船实体 — 6DoF 物理模拟 + 碰撞 + 骑乘
 *
 * <p>职责边界：
 *   - 只负责自身物理状态（位置/速度/姿态）和碰撞响应
 *   - 不处理输入、不处理网络包解析、不处理渲染
 *   - 对外暴露最小接口，所有字段私有化
 */
public class ShipEntity extends Entity {

    // === 物理参数（对齐 v4.3 日志，冻结，不动）===
    /** 碰撞后速度衰减系数（撞墙后保留速度比例），从 Config 读取 */
    public static final float COLLISION_DAMPING = Config.COLLISION_DAMPING.get().floatValue();
    /** @deprecated Use {@link Config#FORCE_MAIN} instead */
    @Deprecated
    public static final float FORCE_MAIN = Config.FORCE_MAIN.get().floatValue();
    /** 侧向/后退推力相对于主推力的比例，从 Config 读取 */
    public static final float SIDE_THRUST_RATIO = Config.SIDE_THRUST_RATIO.get().floatValue();
    /** @deprecated Use {@link Config#FORCE_VERTICAL} instead */
    @Deprecated
    public static final float FORCE_VERTICAL = Config.FORCE_VERTICAL.get().floatValue();
    /** @deprecated Use {@link Config#DAMPING} instead */
    @Deprecated
    public static final float DAMPING = Config.DAMPING.get().floatValue();
    /** @deprecated Use {@link Config#MAX_SPEED_TICK} instead */
    @Deprecated
    public static final float MAX_SPEED_TICK = Config.MAX_SPEED_TICK.get().floatValue();
    /** @deprecated Use {@link Config#YAW_SPEED} instead */
    @Deprecated
    public static final float YAW_SPEED = Config.YAW_SPEED.get().floatValue();

    // P1-7：乘客 Y 轴偏移量已移至 SpaceConstants.SHIP_PASSENGER_OFFSET_Y

    public boolean isAutoLanding = false;

    // ===== 私有字段（P0-1 封装） =====
    private Vec3 shipVelocity = Vec3.ZERO;

    // === 万向节锁防御：退化时继承上一帧 right 向量（Issue #11 临时方案 B）===
    /** 万向节锁退化阈值平方值，向量长度²小于此值视为退化 */
    public static final double GIMBAL_LOCK_THRESHOLD_SQR = 0.0001;
    private Vec3 lastRight = new Vec3(1, 0, 0);

    public ShipEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = false;
        this.setNoGravity(true);
    }

    // ========== 速度访问器（P0-1 新增）==========

    public Vec3 getShipVelocity() {
        return this.shipVelocity;
    }

    public void setShipVelocity(Vec3 velocity) {
        this.shipVelocity = velocity;
    }

    // ========== 网络同步（P0-1 新增，替代 PacketHandler 直接操作字段）==========

    /**
     * 应用 ShipSyncPacket 的数据，一次性更新所有同步字段。
     *
     * <p>由 network 层调用，内部处理 setPos + hasImpulse + 速度赋值。
     * <p>关键：此方法是 ShipEntity 封装边界，network 层不再直接操作任何字段。
     *
     * @param x     同步位置 X
     * @param y     同步位置 Y
     * @param z     同步位置 Z
     * @param yRot  同步偏航角
     * @param xRot  同步俯仰角
     * @param vx    同步速度 X
     * @param vy    同步速度 Y
     * @param vz    同步速度 Z
     */
    public void applySyncPacket(double x, double y, double z,
                                float yRot, float xRot,
                                double vx, double vy, double vz) {
        this.setPos(x, y, z);
        this.setYRot(yRot);
        this.setXRot(xRot);
        this.setYHeadRot(yRot);
        this.shipVelocity = new Vec3(vx, vy, vz);
        this.setDeltaMovement(this.shipVelocity);
        this.hasImpulse = true;
    }

    @Override
    public boolean isControlledByLocalInstance() {
        Entity passenger = this.getFirstPassenger();
        return this.level().isClientSide && passenger instanceof Player;
    }

    @Override
    public LivingEntity getControllingPassenger() {
        Entity passenger = this.getFirstPassenger();
        return passenger instanceof LivingEntity living ? living : null;
    }

    @Override
    protected void defineSynchedData() {}

    // ========== 6DoF 重构：tick 统一时序 ==========
    @Override
    public void tick() {
        super.tick();

        // 客户端权威：纯净 DeltaMovement 驱动移动
        if (this.level().isClientSide && this.isControlledByLocalInstance()) {
            Vec3 motion = this.getDeltaMovement();
            if (motion.lengthSqr() > GIMBAL_LOCK_THRESHOLD_SQR) {
                this.move(MoverType.SELF, motion);

                // 碰撞反馈：撞墙后速度快速衰减（Issue #2，冻结逻辑）
                if (this.horizontalCollision || this.verticalCollision) {
                    shipVelocity = shipVelocity.scale(COLLISION_DAMPING);
                }
            }
        }
    }

    // ========== 6DoF 重构：本地坐标系 + 鼠标 Pitch/Yaw ==========
    public void clientTickPhysics(boolean fwd, boolean back,
                                  boolean strafeLeft, boolean strafeRight,
                                  boolean up, boolean down,
                                  float pitchDelta, float yawDelta) {
        if (!this.level().isClientSide) return;

        // --- 1. 鼠标控制飞船姿态（Outer Wilds 式）---
        // xRotO/yRotO 由原版 Entity.tick() 自动管理，
        // 这里显式覆盖会破坏渲染插值，导致画面跳变。
        // 禁止在此处赋值 xRotO/yRotO。

        float newPitch = this.getXRot() + pitchDelta;
        newPitch = Mth.clamp(newPitch, -SpaceConstants.PITCH_CLAMP, SpaceConstants.PITCH_CLAMP);
        this.setXRot(newPitch);

        float newYaw = this.getYRot() + yawDelta;
        this.setYRot(newYaw);
        this.setYHeadRot(newYaw);

        // --- 2. 构建本地坐标系（forward / right / up）---
        double yawRad = Math.toRadians(this.getYRot());
        double pitchRad = Math.toRadians(this.getXRot());

        Vec3 forward = new Vec3(
                -Math.sin(yawRad) * Math.cos(pitchRad),
                -Math.sin(pitchRad),
                Math.cos(yawRad) * Math.cos(pitchRad)
        ).normalize();

        Vec3 worldUp = new Vec3(0, 1, 0);
        Vec3 right = forward.cross(worldUp);
        if (right.lengthSqr() < GIMBAL_LOCK_THRESHOLD_SQR) {
            right = lastRight; // 退化时继承上一帧 right（万向节锁防御 B）
        } else {
            right = right.normalize();
            lastRight = right; // 正常时更新缓存
        }

        // --- 本地 up 向量（right × forward = up，退化时 fallback 世界 up）---
        Vec3 localUp = right.cross(forward);
        if (localUp.lengthSqr() < GIMBAL_LOCK_THRESHOLD_SQR) {
            localUp = new Vec3(0, 1, 0);
        } else {
            localUp = localUp.normalize();
        }

        // --- 3. 推力计算（纯本地坐标系）---
        Vec3 thrust = Vec3.ZERO;
        if (fwd)         thrust = thrust.add(forward.scale(FORCE_MAIN));
        if (back)        thrust = thrust.add(forward.scale(-FORCE_MAIN * SIDE_THRUST_RATIO));
        if (strafeLeft)  thrust = thrust.add(right.scale(-FORCE_MAIN * SIDE_THRUST_RATIO));
        if (strafeRight) thrust = thrust.add(right.scale(FORCE_MAIN * SIDE_THRUST_RATIO));
        if (up)          thrust = thrust.add(localUp.scale(FORCE_VERTICAL));
        if (down)        thrust = thrust.add(localUp.scale(-FORCE_VERTICAL));

        // --- 4. 速度更新（冻结参数）---
        shipVelocity = shipVelocity.add(thrust);
        shipVelocity = shipVelocity.scale(1.0f - DAMPING);

        double speed = shipVelocity.length();
        if (speed > MAX_SPEED_TICK) {
            shipVelocity = shipVelocity.scale(MAX_SPEED_TICK / speed);
        }

        // --- 5. 纯净 DeltaMovement：只设速度向量，原版冲量清零 ---
        this.setDeltaMovement(shipVelocity);
    }

    // ========== 网络同步防护（冻结，完全不动）==========

    @Override
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int steps, boolean teleport) {
        if (teleport || this.distanceToSqr(x, y, z) > SpaceConstants.TELEPORT_THRESHOLD) {
            this.setPos(x, y, z);
            this.setYRot(yaw);
            this.setXRot(pitch);
        }
    }

    @Override
    public void absMoveTo(double x, double y, double z, float yaw, float pitch) {
        if (this.distanceToSqr(x, y, z) > SpaceConstants.TELEPORT_THRESHOLD) {
            super.absMoveTo(x, y, z, yaw, pitch);
        }
    }

    // ========== 骑乘系统（冻结，完全不动）==========

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!this.level().isClientSide && this.getPassengers().isEmpty()) {
            player.startRiding(this);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengers().isEmpty();
    }

    /**
     * 禁用原版视角同步 — ShipInputHandler 每 tick 手动同步 mc.player 视角到飞船。
     *
     * <p>空实现是设计决策而非遗漏：原版此方法同步乘客视角到载具，
     * 但 ShipInputHandler 已手动完成同步，因此空实现安全。
     *
     * <p>P2-5：此空实现已记录为架构决策。若未来移除 ShipInputHandler 的手动同步，
     * 需在此处恢复原版实现。
     */
    @Override
    public void onPassengerTurned(Entity passenger) {
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction moveFunction) {
        double offset = passenger instanceof Player ? SpaceConstants.SHIP_PASSENGER_OFFSET_Y : 0.0;
        moveFunction.accept(passenger, this.getX(),
                this.getY() + this.getPassengersRidingOffset() + offset, this.getZ());
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity livingEntity) {
        return new Vec3(this.getX(), this.getY() + SpaceConstants.DISMOUNT_OFFSET_Y, this.getZ());
    }

    @Override public boolean isPickable() { return !this.isRemoved(); }
    @Override public boolean canBeCollidedWith() { return true; }

    @Override protected void readAdditionalSaveData(CompoundTag t) {
        shipVelocity = new Vec3(t.getDouble("vx"), t.getDouble("vy"), t.getDouble("vz"));
    }
    @Override protected void addAdditionalSaveData(CompoundTag t) {
        t.putDouble("vx", shipVelocity.x);
        t.putDouble("vy", shipVelocity.y);
        t.putDouble("vz", shipVelocity.z);
    }
}