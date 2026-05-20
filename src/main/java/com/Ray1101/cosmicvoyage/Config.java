package com.Ray1101.cosmicvoyage;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * CosmicVoyage 配置系统 — 所有可调参数集中管理。
 *
 * <p>P1-2：飞船物理参数从硬编码迁移到 Config，
 * 支持不同维度/行星的不同物理特性（未来扩展）。
 *
 * <p>Config-Constant 连接：所有默认值从 {@link SpaceConstants} 读取，
 * 调整 SpaceConstants 时 Config 默认值自动同步。
 */
public class Config {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // =========================
    // Flight System
    // =========================

    public static final ForgeConfigSpec.DoubleValue MAX_FLIGHT_SPEED = BUILDER
            .comment("Maximum flight speed in blocks per second")
            .defineInRange("maxFlightSpeed", 500.0, 50.0, 5000.0);

    public static final ForgeConfigSpec.DoubleValue FLIGHT_ACCELERATION = BUILDER
            .comment("Ship acceleration multiplier")
            .defineInRange("flightAcceleration", SpaceConstants.FORCE_MAIN, 0.01, 5.0);

    // =========================
    // Gravity System
    // =========================

    public static final ForgeConfigSpec.DoubleValue GRAVITY_SCALE = BUILDER
            .comment("Global gravity scale for space dimension")
            .defineInRange("gravityScale", 0.02, 0.0, 1.0);

    // =========================
    // Transition System
    // =========================

    public static final ForgeConfigSpec.IntValue TRANSITION_TIME = BUILDER
            .comment("Dimension transition duration (ticks)")
            .defineInRange("transitionTime", 100, 20, 600);

    // =========================
    // Ship Physics (P1-2)
    // 默认值全部从 SpaceConstants 读取，确保单一源头
    // =========================

    public static final ForgeConfigSpec.DoubleValue FORCE_MAIN = BUILDER
            .comment("Main engine thrust force")
            .defineInRange("forceMain", SpaceConstants.FORCE_MAIN, 0.1, 5.0);

    public static final ForgeConfigSpec.DoubleValue FORCE_VERTICAL = BUILDER
            .comment("Vertical thrust force (up/down)")
            .defineInRange("forceVertical", SpaceConstants.FORCE_VERTICAL, 0.1, 2.0);

    public static final ForgeConfigSpec.DoubleValue DAMPING = BUILDER
            .comment("Velocity damping factor per tick")
            .defineInRange("damping", SpaceConstants.DAMPING, 0.001, 0.1);

    public static final ForgeConfigSpec.DoubleValue MAX_SPEED_TICK = BUILDER
            .comment("Maximum speed per tick (blocks/tick)")
            .defineInRange("maxSpeedTick", SpaceConstants.MAX_SPEED_TICK, 5.0, 100.0);

    public static final ForgeConfigSpec.DoubleValue YAW_SPEED = BUILDER
            .comment("Yaw rotation speed")
            .defineInRange("yawSpeed", SpaceConstants.YAW_SPEED, 0.5, 10.0);

    public static final ForgeConfigSpec.DoubleValue COLLISION_DAMPING = BUILDER
            .comment("Velocity retention after collision (0.3 = 30%)")
            .defineInRange("collisionDamping", SpaceConstants.COLLISION_DAMPING, 0.1, 0.9);

    public static final ForgeConfigSpec.DoubleValue SIDE_THRUST_RATIO = BUILDER
            .comment("Side/back thrust ratio relative to main (0.5 = 50%)")
            .defineInRange("sideThrustRatio", SpaceConstants.SIDE_THRUST_RATIO, 0.1, 1.0);

    // =========================
    // FINAL SPEC
    // =========================

    public static final ForgeConfigSpec SPEC = BUILDER.build();
}