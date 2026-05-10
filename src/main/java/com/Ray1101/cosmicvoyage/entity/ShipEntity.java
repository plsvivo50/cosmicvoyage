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
import net.minecraft.util.Mth;

public class ShipEntity extends Entity {

    public static final float FORCE_MAIN = 0.08f;
    public static final float FORCE_VERTICAL = 0.05f;
    public static final float DAMPING = 0.015f;
    public static final float MAX_SPEED_TICK = 25.0f;
    public static final float YAW_SPEED = 2.5f;
    public static final float PITCH_SPEED = 2.0f;

    /** 目标俯仰角（用于着陆姿态平滑过渡） */
    public float targetPitch = 0.0f;

    public Vec3 shipVelocity = Vec3.ZERO;

    public ShipEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = false;
        this.setNoGravity(true);
    }

    @Override
    public boolean isControlledByLocalInstance() {
        // 关键：当本地玩家骑乘时，客户端忽略服务端发来的位置同步
        Entity passenger = this.getFirstPassenger();
        return this.level().isClientSide && passenger instanceof Player;
    }

    @Override
    public net.minecraft.world.entity.LivingEntity getControllingPassenger() {
        net.minecraft.world.entity.Entity passenger = this.getFirstPassenger();
        return passenger instanceof net.minecraft.world.entity.LivingEntity living ? living : null;
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    public void tick() {
        super.tick();
        // 服务端：什么都不做，等网络包
        // 客户端：什么都不做，等 ShipInputHandler 驱动
    }

    // ===== 客户端物理 =====

    public void clientTickPhysics(boolean fwd, boolean back, boolean left, boolean right,
                                  boolean up, boolean down) {
        if (!this.level().isClientSide) return;

        // 偏航
        float yaw = this.getYRot();
        if (left)  yaw -= YAW_SPEED;
        if (right) yaw += YAW_SPEED;
        this.setYRot(yaw);
        this.setYHeadRot(yaw);

        // 水平前进方向
        double yr = Math.toRadians(yaw);
        Vec3 forward = new Vec3(-Math.sin(yr), 0, Math.cos(yr)).normalize();

        // 推力
        Vec3 thrust = Vec3.ZERO;
        if (fwd)  thrust = thrust.add(forward.scale(FORCE_MAIN));
        if (back) thrust = thrust.add(forward.scale(-FORCE_MAIN * 0.5));
        if (up)   thrust = thrust.add(0, FORCE_VERTICAL, 0);
        if (down) thrust = thrust.add(0, -FORCE_VERTICAL, 0);

        // 积分 + 阻尼
        shipVelocity = shipVelocity.add(thrust);
        shipVelocity = shipVelocity.scale(1.0f - DAMPING);

        // 软上限
        double speed = shipVelocity.length();
        if (speed > MAX_SPEED_TICK) {
            shipVelocity = shipVelocity.scale(MAX_SPEED_TICK / speed);
        }

        // 更新位置
        this.move(net.minecraft.world.entity.MoverType.SELF, shipVelocity);
        this.hasImpulse = true;

        // ===== 姿态自动对齐 =====
        // 根据速度向量自动调整俯仰（pitch）
        // 水平飞行时 pitch ≈ 0，垂直向下时 pitch ≈ 90，垂直向上时 pitch ≈ -90
        alignPitchToVelocity();
    }

    /**
     * 根据速度向量自动调整飞船俯仰角度。
     * 水平速度为主时 pitch → 0，垂直下落时 pitch → 90（机头朝下）。
     * 使用平滑插值避免突兀变化。
     */
    private void alignPitchToVelocity() {
        double speed = shipVelocity.length();
        if (speed < 0.01) return; // 静止时不调整

        // 计算速度向量与水平面的夹角
        Vec3 horizontal = new Vec3(shipVelocity.x, 0, shipVelocity.z);
        double horizSpeed = horizontal.length();
        
        // 根据垂直/水平速度比计算目标 pitch
        float desiredPitch;
        if (horizSpeed < 0.01) {
            // 纯垂直运动
            desiredPitch = (float)(shipVelocity.y < 0 ? 90.0 : -90.0);
        } else {
            double angle = Math.atan2(-shipVelocity.y, horizSpeed);
            desiredPitch = (float)Math.toDegrees(angle);
        }

        // 使用 targetPitch 实现平滑过渡
        // 正常飞行中为0，着陆时由外部设为 -90
        float pitchDiff = targetPitch - desiredPitch;
        if (Math.abs(pitchDiff) < 1.0f) {
            desiredPitch = targetPitch;
        } else {
            // 朝 targetPitch 方向靠近，但不越过
            desiredPitch += Math.signum(pitchDiff) * Math.min(Math.abs(pitchDiff), 0.5f);
        }

        // 平滑插值当前 pitch 到 desired pitch
        float currentPitch = this.getXRot();
        float newPitch = currentPitch + (desiredPitch - currentPitch) * 0.15f;
        this.setXRot(Mth.clamp(newPitch, -90.0f, 90.0f));
    }

    // ===== 禁用原版位置插值 =====

    @Override
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int steps, boolean teleport) {
        // 100格 = 拒绝所有延迟同步（25格/tick × 4tick延迟 = 100格）
        // 只有真正的传送（维度切换等）才接受
        if (teleport || this.distanceToSqr(x, y, z) > 10000.0) {
            this.setPos(x, y, z);
            this.setYRot(yaw);
            this.setXRot(pitch);
        }
        // 小幅位置更新：完全忽略，客户端本地物理为准
    }

    @Override
    public void absMoveTo(double x, double y, double z, float yaw, float pitch) {
        // 同上，大幅偏离才接受，堵住 TeleportEntityPacket 路径
        if (this.distanceToSqr(x, y, z) > 10000.0) {
            super.absMoveTo(x, y, z, yaw, pitch);
        }
    }

    // ===== 骑乘系统 =====

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
        // 完全禁用原版视角同步
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction moveFunction) {
        // 无条件同步所有乘客，原版 MoveFunction 内部就是 setPos
        double offset = passenger instanceof Player ? 0.8 : 0.0;
        moveFunction.accept(passenger, this.getX(), this.getY() + this.getPassengersRidingOffset() + offset, this.getZ());
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


