package com.Ray1101.cosmicvoyage;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // =========================
    // 🚀 Flight System
    // =========================

    public static final ForgeConfigSpec.DoubleValue MAX_FLIGHT_SPEED = BUILDER
            .comment("Maximum flight speed in blocks per second")
            .defineInRange("maxFlightSpeed", 500.0, 50.0, 5000.0);

    public static final ForgeConfigSpec.DoubleValue FLIGHT_ACCELERATION = BUILDER
            .comment("Ship acceleration multiplier")
            .defineInRange("flightAcceleration", 0.5, 0.01, 5.0);

    // =========================
    // 🌌 Gravity System
    // =========================

    public static final ForgeConfigSpec.DoubleValue GRAVITY_SCALE = BUILDER
            .comment("Global gravity scale for space dimension")
            .defineInRange("gravityScale", 0.02, 0.0, 1.0);

    // =========================
    // 🌠 Transition System
    // =========================

    public static final ForgeConfigSpec.IntValue TRANSITION_TIME = BUILDER
            .comment("Dimension transition duration (ticks)")
            .defineInRange("transitionTime", 100, 20, 600);

    // =========================
    // FINAL SPEC
    // =========================

    public static final ForgeConfigSpec SPEC = BUILDER.build();
}