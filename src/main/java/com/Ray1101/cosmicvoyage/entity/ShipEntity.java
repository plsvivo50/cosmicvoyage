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

import com.Ray1101.cosmicvoyage.dimension.ModDimensions;

public class ShipEntity extends Entity {

    // === 对齐 v4.3 日志参数（冻结，不动）===
    public static final float FORCE_MAIN = 0.5f;
    public static final float FORCE_VERTICAL = 0.3f;
    public static final float DAMPING = 0.015f;
    public static final float MAX_SPEED_TICK = 25.0f;
    public static final float YAW_SPEED = 2.5f;

    public boolean isAutoLanding = false;
    public Vec3 shipVelocity = Vec3.ZERO;

    // === 万向节锁防御：退化时继承上一帧 right 向量（Issue #11 临时方案 B）===
    private Vec3 lastRight = new Vec3(1, 0, 0);

    public ShipEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = false;
        this.setNoGravity(true);
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
            if (motion.lengthSqr() > 0.0001) {
                this.move(MoverType.SELF, motion);

                // 碰撞反馈：撞墙后速度快速衰减（Issue #2，冻结逻辑）
                if (this.horizontalCollision || this.verticalCollision) {
                    shipVelocity = shipVelocity.scale(0.3);
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
        newPitch = Mth.clamp(newPitch, -89.9f, 89.9f);
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
        if (right.lengthSqr() < 0.0001) {
            right = lastRight; // 退化时继承上一帧 right（万向节锁防御 B）
        } else {
            right = right.normalize();
            lastRight = right; // 正常时更新缓存
        }

        // --- 本地 up 向量（right × forward = up，退化时 fallback 世界 up）---
        Vec3 localUp = right.cross(forward);
        if (localUp.lengthSqr() < 0.0001) {
            localUp = new Vec3(0, 1, 0);
        } else {
            localUp = localUp.normalize();
        }

        // --- 3. 推力计算（纯本地坐标系）---
        Vec3 thrust = Vec3.ZERO;
        if (fwd)         thrust = thrust.add(forward.scale(FORCE_MAIN));
        if (back)        thrust = thrust.add(forward.scale(-FORCE_MAIN * 0.5));
        if (strafeLeft)  thrust = thrust.add(right.scale(-FORCE_MAIN * 0.5));
        if (strafeRight) thrust = thrust.add(right.scale(FORCE_MAIN * 0.5));
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
        if (teleport || this.distanceToSqr(x, y, z) > 10000.0) {
            this.setPos(x, y, z);
            this.setYRot(yaw);
            this.setXRot(pitch);
        }
    }

    @Override
    public void absMoveTo(double x, double y, double z, float yaw, float pitch) {
        if (this.distanceToSqr(x, y, z) > 10000.0) {
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

    @Override
    public void onPassengerTurned(Entity passenger) {
        // 禁用原版视角同步（关键！冻结，不动）
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction moveFunction) {
        double offset = passenger instanceof Player ? 0.8 : 0.0;
        moveFunction.accept(passenger, this.getX(),
                this.getY() + this.getPassengersRidingOffset() + offset, this.getZ());
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity livingEntity) {
        return new Vec3(this.getX(), this.getY() + 1.5, this.getZ());
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