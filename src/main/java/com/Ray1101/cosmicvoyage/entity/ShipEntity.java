package com.Ray1101.cosmicvoyage.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class ShipEntity extends Entity {

    // === 对齐 v4.3 日志参数 ===
    public static final float FORCE_MAIN = 0.5f;
    public static final float FORCE_VERTICAL = 0.3f;
    public static final float DAMPING = 0.015f;
    public static final float MAX_SPEED_TICK = 25.0f;
    public static final float YAW_SPEED = 2.5f;

    public boolean isAutoLanding = false;
    public Vec3 shipVelocity = Vec3.ZERO;

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

    @Override
    public void tick() {
        super.tick();
    }

    // ===== 六自由度物理（仅客户端）=====
    public void clientTickPhysics(boolean fwd, boolean back, boolean left, boolean right,
                                  boolean up, boolean down) {
        if (!this.level().isClientSide) return;

        // A/D 控制偏航（左右转弯）
        float yaw = this.getYRot();
        if (left)  yaw -= YAW_SPEED;
        if (right) yaw += YAW_SPEED;
        this.setYRot(yaw);
        this.setYHeadRot(yaw);

        // W/S 沿机头方向前后，Space/Ctrl 垂直上下
        double yr = Math.toRadians(yaw);
        Vec3 forward = new Vec3(-Math.sin(yr), 0, Math.cos(yr)).normalize();

        Vec3 thrust = Vec3.ZERO;
        if (fwd)  thrust = thrust.add(forward.scale(FORCE_MAIN));
        if (back) thrust = thrust.add(forward.scale(-FORCE_MAIN * 0.5));
        if (up)   thrust = thrust.add(0, FORCE_VERTICAL, 0);
        if (down) thrust = thrust.add(0, -FORCE_VERTICAL, 0);

        shipVelocity = shipVelocity.add(thrust);
        shipVelocity = shipVelocity.scale(1.0f - DAMPING);

        double speed = shipVelocity.length();
        if (speed > MAX_SPEED_TICK) {
            shipVelocity = shipVelocity.scale(MAX_SPEED_TICK / speed);
        }

        this.move(net.minecraft.world.entity.MoverType.SELF, shipVelocity);
        this.hasImpulse = true;

        // 碰撞反馈：撞墙后速度快速衰减（Issue #2）
        if (this.horizontalCollision || this.verticalCollision) {
            shipVelocity = shipVelocity.scale(0.3);
        }
    }

    // ===== 网络同步防护（已有，不动）=====

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

    // ===== 骑乘系统（已有，不动）=====

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
        // 禁用原版视角同步（关键！）
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