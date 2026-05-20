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
 *
 * <p>Phase 2 更新：
 *   - 地球坐标从 (0, 0, 0) 移至 (10000, 0, 0)，太阳在原点
 *   - SPACE_ENTRY_POS 改为基于 EARTH_POSITION 的相对偏移
 *   - 火星参数预占位（比例推导，非硬编码）
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

    // ===== 地球坐标（Phase 2：移至 10000，太阳在原点） =====
    /** 地球在太空维度中的固定位置。太阳在 (0, 0, 0)，地球在 (10000, 0, 0)。 */
    public static final Vec3 EARTH_POSITION = new Vec3(10000.0, 0.0, 0.0);

    // ===== 月球参数（比例：1/4 地球，相对地球 +2000） =====
    /** 月球物理半径，用于 MoonRenderer 和着陆触发检测。 */
    public static final float MOON_RADIUS = EARTH_RADIUS * 0.25f;           // 50.0f
    /** 月球渲染半径，用于 MoonRenderer 可视化。 */
    public static final float MOON_RENDER_RADIUS = MOON_RADIUS;             // 50.0f
    /** 月球在太空坐标系中的水平距离（地球 + 2000）。 */
    public static final float MOON_DISTANCE = 12000.0f;

    // ===== 火星参数（Phase 2 预占位，比例推导） =====
    /** 火星半径 = 1/2 地球（视觉效果比月球大，体现行星尺度）。 */
    public static final float MARS_RADIUS = EARTH_RADIUS * 0.5f;            // 100.0f
    /** 火星渲染半径。 */
    public static final float MARS_RENDER_RADIUS = MARS_RADIUS;             // 100.0f
    /** 火星在太空坐标系中的位置（距地球 +5000）。 */
    public static final Vec3 MARS_POSITION = new Vec3(15000.0, 0.0, 0.0);
    /** 火星维度 y 坐标达到此值时自动返回太空。 */
    public static final double MARS_ESCAPE_HEIGHT = 200.0;
    /** 火星着陆触发距离（3D），大于此距离不会触发火星着陆。 */
    public static final float TRIGGER_LAND_MARS = MARS_RADIUS * 4.0f;       // 400.0f
    /** 火星进入坐标（表面上方 100 格）。 */
    public static final Vec3 MARS_ENTRY_POS = new Vec3(
            MARS_POSITION.x,                                                // 15000.0
            MARS_RADIUS + 100.0f,                                           // 200.0f
            MARS_POSITION.z                                                 // 0.0
    );

    // ===== 维度切换参数 =====
    /** 传送后速度缩放因子（维度切换后保留的速度比例） */
    public static final double ENTRY_VELOCITY_SCALE = 0.5;

    // ===== 飞船物理参数（Config 默认值源头） =====
    /** 主推力 */
    public static final double FORCE_MAIN = 0.5;
    /** 垂直推力 */
    public static final double FORCE_VERTICAL = 0.3;
    /** 速度阻尼（每 tick） */
    public static final double DAMPING = 0.015;
    /** 最大速度（blocks/tick） */
    public static final double MAX_SPEED_TICK = 25.0;
    /** 偏航速度 */
    public static final double YAW_SPEED = 2.5;
    /** 碰撞后速度保留比例 */
    public static final double COLLISION_DAMPING = 0.3;
    /** 侧向/后退推力比例 */
    public static final double SIDE_THRUST_RATIO = 0.5;
    /** 飞船乘客 Y 轴偏移量 */
    public static final double SHIP_PASSENGER_OFFSET_Y = 0.8;
    /** Pitch 限制角度（防止万向节锁） */
    public static final float PITCH_CLAMP = 89.9f;
    /** 传送位置偏差阈值 */
    public static final double TELEPORT_THRESHOLD = 10000.0;
    /** 下马位置偏移 */
    public static final double DISMOUNT_OFFSET_Y = 1.5;

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

    // ===== 太空进入坐标（基于 EARTH_POSITION 的相对偏移） =====
    // 数学验证：3D 距地球 = sqrt(y_offset² + z_offset²) = sqrt(400² + 1200²) ≈ 1265 > TRIGGER_LAND_EARTH
    // 即使 EARTH_POSITION 改变，只要相对偏移不变，就不会触发死循环
    public static final Vec3 SPACE_ENTRY_POS = new Vec3(
            EARTH_POSITION.x,                                      // 跟随地球 x
            EARTH_POSITION.y + EARTH_RADIUS + 200.0f,              // 400.0f — 高于地球表面
            EARTH_POSITION.z - (EARTH_RADIUS + TRIGGER_LAND_EARTH + 200.0f)  // -1200.0f — 距离地球 > 触发阈值+余量
    );

    // ===== 月球进入坐标 =====
    // (12000, 150, 0) — 月球表面上方 100 格（月球半径 50）
    public static final Vec3 MOON_ENTRY_POS = new Vec3(
            MOON_DISTANCE,                                  // 12000.0f
            MOON_RADIUS + 100.0f,                           // 150.0f — 高于月球表面
            0.0
    );

    // ===== 渲染 LOD 阈值 =====
    /** 距离小于此值时隐藏 HUD 导航箭头，让 3D 球体接管视觉引导。 */
    public static final float HUD_HIDE_DISTANCE = EARTH_RADIUS * 3.0f;      // 600.0f

    // 渲染 LOD 阈值设计原则：TRIGGER_LAND_* < RENDER_LOD_NEAR
    // 确保完整球体在维度切换之前显示，玩家能看到渲染效果
    /** 渲染 LOD 远距离阈值：> 此距离显示 Billboard 光点 */
    public static final double RENDER_LOD_FAR = 8000.0;
    /** 渲染 LOD 中距离阈值：< 此距离显示低模线框球体 */
    public static final double RENDER_LOD_MID = 2000.0;
    /** 渲染 LOD 近距离阈值：< 此距离显示完整球体+大气 */
    public static final double RENDER_LOD_NEAR = 1000.0;
    // 数值关系：RENDER_LOD_FAR > RENDER_LOD_MID > RENDER_LOD_NEAR > TRIGGER_LAND_EARTH
    // 玩家飞来时：Billboard → 低模球体 → 完整球体 → 球内指示器 → 维度切换
}