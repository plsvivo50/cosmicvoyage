package com.Ray1101.cosmicvoyage;

import net.minecraft.world.phys.Vec3;

/**
 * 太空维度常量 — 唯一硬编码 EARTH_RADIUS，其余通过比例推导。
 *
 * <p>设计原则：
 *   - 只有一个源头常量 {@link #EARTH_RADIUS}
 *   - 所有其他常量基于它比例推导
 *   - 修改 EARTH_RADIUS 即可全局缩放，不会出现不一致
 *   - 所有涉及玩家/飞船的物理参数统一在此管理
 *
 * <p>5月11日教训：基于虚拟机文件做常量重构导致引用混乱。
 * 本次常量系统基于实际代码推导，所有值可数学验证。
 */
public final class SpaceConstants {

    private SpaceConstants() {} // 禁止实例化

    // ===== 唯一硬编码源头 =====
    public static final float EARTH_RADIUS = 200.0f;

    // ===== 地球渲染参数 =====
    /** 地球渲染半径，用于 EarthRenderer 和 HUD 距离计算。 */
    public static final float EARTH_RENDER_RADIUS = EARTH_RADIUS;           // 200.0f
    /** 地球大气层渲染半径，用于大气效果渲染。 */
    public static final float EARTH_ATMOSPHERE_RADIUS = EARTH_RADIUS * 1.2f; // 240.0f

    // ===== 月球参数（比例：1/4 地球） =====
    /** 月球物理半径，用于 MoonRenderer 和着陆触发检测。 */
    public static final float MOON_RADIUS = EARTH_RADIUS * 0.25f;           // 50.0f
    /** 月球渲染半径，用于 MoonRenderer 可视化。 */
    public static final float MOON_RENDER_RADIUS = MOON_RADIUS;             // 50.0f
    /** 月球在太空坐标系中的水平距离。 */
    public static final float MOON_DISTANCE = 12000.0f;

    // ===== 飞船物理参数 =====
    /** 飞船乘客 Y 轴偏移量，用于骑乘时摄像机位置计算。 */
    public static final double SHIP_PASSENGER_OFFSET_Y = 0.8;

    // ===== 发射参数 =====
    /** 主世界自动发射到太空的高度阈值 */
    public static final double LAUNCH_HEIGHT = 300.0;

    // ===== 月球维度参数 =====
    /** 月球维度 y 坐标达到此值时自动返回太空 */
    public static final double MOON_ESCAPE_HEIGHT = 200.0;

    // ===== 触发距离 =====
    /** 地球着陆触发距离（3D），大于此距离不会触发返回地球。 */
    public static final float TRIGGER_LAND_EARTH = EARTH_RADIUS * 4.0f;     // 800.0f
    /** 月球着陆触发距离（3D），大于此距离不会触发月球着陆。 */
    public static final float TRIGGER_LAND_MOON = MOON_RADIUS * 4.0f;       // 200.0f

    // ===== 太空进入坐标（数学上不可能触发着陆） =====
    // 缓冲设计：3D 距离 = sqrt(y² + z²) 必须 > TRIGGER_LAND_EARTH + 安全余量
    // (0, 400, -1200) → 3D 距离 ≈ 1265 > 800，水平缓冲 507 格
    public static final Vec3 SPACE_ENTRY_POS = new Vec3(
            0.0,
            EARTH_RADIUS + 200.0f,                          // 400.0f — 高于地球表面
            -(EARTH_RADIUS + TRIGGER_LAND_EARTH + 200.0f)   // -1200.0f — 距离地球 > 触发阈值+余量
    );

    // ===== 月球进入坐标 =====
    // (12000, 150, 0) — 月球表面上方 100 格（月球半径 50）
    public static final Vec3 MOON_ENTRY_POS = new Vec3(
            MOON_DISTANCE,                                  // 12000.0f
            MOON_RADIUS + 100.0f,                           // 150.0f — 高于月球表面
            0.0
    );

    // ===== HUD LOD 阈值 =====
    /** 距离小于此值时隐藏 HUD 导航箭头，让 3D 球体接管视觉引导。 */
    public static final float HUD_HIDE_DISTANCE = EARTH_RADIUS * 3.0f;      // 600.0f
}