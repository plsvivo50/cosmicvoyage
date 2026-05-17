# CosmicVoyage 接口契约固化与重构方案

**日期**: 2026-05-18（Day 3 — API Freezing）  
**输入**: Day 1 架构测绘报告 + Day 2 代码异味扫描报告  
**目标**: 核心模块对外接口固化，圈定 Phase 2 启动前必须完成的 P0 重构项  

---

## 一、核心模块接口契约

### 1.1 ShipEntity — 飞船实体

```java
/**
 * 飞船实体 — 6DoF 物理模拟 + 碰撞 + 骑乘
 * 
 * 职责边界：
 *   - 只负责自身物理状态（位置/速度/姿态）和碰撞响应
 *   - 不处理输入、不处理网络包解析、不处理渲染
 *   - 对外暴露最小接口，所有字段私有化
 */
public class ShipEntity extends Entity {

    // ===== 构造与注册（Forge 要求） =====
    public ShipEntity(EntityType<? extends ShipEntity> type, Level level)
    
    // ===== 物理参数（从 Config 读取，非硬编码） =====
    public static double getForceMain()        // Config.FORCE_MAIN
    public static double getForceVertical()    // Config.FORCE_VERTICAL
    public static double getDamping()          // Config.DAMPING
    public static double getMaxSpeedTick()     // Config.MAX_SPEED_TICK
    public static double getYawSpeed()         // Config.YAW_SPEED
    public static double getCollisionDamping() // Config.COLLISION_DAMPING
    public static double getStrafeForceRatio() // Config.STRAFE_FORCE_RATIO
    public static double getPassengerOffsetY() // PASSENGER_OFFSET_Y = 0.8
    public static double getGimbalLockThresholdSq() // GIMBAL_LOCK_THRESHOLD_SQ = 0.0001
    
    // ===== 速度访问（替代原 public 字段） =====
    public Vec3 getShipVelocity()
    public void setShipVelocity(Vec3 velocity)
    
    // ===== 姿态访问 =====
    public float getShipPitch()       // 欧拉角 Pitch（临时，后续换四元数）
    public void setShipPitch(float pitch)
    
    // ===== 网络同步（替代 network 层直接操作字段） =====
    /**
     * 应用 ShipSyncPacket 的数据，一次性更新所有同步字段
     * 由 network 层调用，内部处理 setPos + hasImpulse + 速度赋值
     */
    public void applySyncPacket(double x, double y, double z, 
                                float yRot, float xRot,
                                double vx, double vy, double vz)
    
    // ===== 本地坐标系（6DoF） =====
    public Vec3 getForwardVector()     // 基于 yRot + pitch
    public Vec3 getRightVector()       // forward × worldUp
    public Vec3 getLocalUpVector()     // right × forward
    
    // ===== 碰撞反馈 =====
    public boolean isHorizontalCollision()
    public void applyCollisionDamping()  // 撞墙后速度 *= COLLISION_DAMPING
    
    // ===== 骑乘系统 =====
    @Override public Vec3 getDismountLocationForPassenger(LivingEntity passenger)
    @Override protected boolean canAddPassenger(Entity passenger)
    @Override public void onPassengerTurned(Entity passenger)  // 空实现 — 由 ShipInputHandler 手动同步
    
    // ===== 原版覆盖（已冻结，不动） =====
    @Override public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps)
    @Override public void absMoveTo(double x, double y, double z, float yRot, float xRot)
    @Override protected void defineSynchedData()
    @Override protected void readAdditionalSaveData(CompoundTag tag)
    @Override protected void addAdditionalSaveData(CompoundTag tag)
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket()
}
```

**偏离度评估**：当前 `shipVelocity` / `hasImpulse` 为 public 字段，network 层直接赋值。修复后所有同步逻辑集中到 `applySyncPacket()`。

---

### 1.2 SpaceData — 锚点持久化

```java
/**
 * 太空数据持久化 — 双锚点系统（地球 + 月球）
 * 
 * 职责边界：
 *   - 只负责锚点数据的序列化/反序列化
 *   - 不处理维度切换逻辑、不处理网络通信
 *   - NBT 结构版本化管理，支持未来扩展
 */
public class SpaceData extends SavedData {

    private static final int DATA_VERSION = 2;  // 从 v1(单锚点) 升级到 v2(双锚点)
    
    // ===== 地球锚点 =====
    public boolean hasEarthAnchor()
    public BlockPos getEarthAnchor()
    public void setEarthAnchor(BlockPos pos)
    public void clearEarthAnchor()
    
    // ===== 月球锚点 =====
    public boolean hasMoonAnchor()
    public BlockPos getMoonAnchor()
    public void setMoonAnchor(BlockPos pos)
    public void clearMoonAnchor()
    
    // ===== 通用查询 =====
    /**
     * 根据维度返回对应锚点
     * @param dimension 玩家当前所在维度
     * @return 若在主世界返回月球锚点（目标），若在月球返回地球锚点（目标）
     */
    public BlockPos getAnchorFor(ResourceKey<Level> dimension)
    
    // ===== SavedData 生命周期 =====
    @Override public CompoundTag save(CompoundTag tag)
    public static SpaceData load(CompoundTag tag)
    public static SpaceData get(ServerLevel level)
}
```

**偏离度评估**：当前只有 `anchorX/Y/Z` + `hasAnchor`（地球锚点）。需扩展为双锚点，NBT 加载时兼容 v1 旧数据。

---

### 1.3 Packet 系统 — 拆分后结构

```
network/
├── CosmicVoyagePacketHandler.java   [注册 + 分发，只负责 SimpleChannel 管理]
├── packet/
│   ├── ShipSyncPacket.java           [encode/decode/handle 自包含]
│   ├── MoonTransitionPacket.java     [encode/decode/handle 自包含]
│   ├── LaunchToSpacePacket.java      [encode/decode/handle 自包含]
│   └── LandOnEarthPacket.java        [encode/decode/handle 自包含（预留）]
```

```java
// PacketHandler.java — 只负责注册，不处理业务逻辑
public class CosmicVoyagePacketHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(...);
    
    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, ShipSyncPacket.class, 
            ShipSyncPacket::encode, ShipSyncPacket::decode, ShipSyncPacket::handle);
        CHANNEL.registerMessage(id++, MoonTransitionPacket.class,
            MoonTransitionPacket::encode, MoonTransitionPacket::decode, MoonTransitionPacket::handle);
        CHANNEL.registerMessage(id++, LaunchToSpacePacket.class,
            LaunchToSpacePacket::encode, LaunchToSpacePacket::decode, LaunchToSpacePacket::handle);
        // LandOnEarthPacket 预留
    }
}

// ShipSyncPacket.java — 自包含
public class ShipSyncPacket {
    private final double x, y, z, vx, vy, vz;
    private final float yRot, xRot;
    
    public static void encode(ShipSyncPacket pkt, FriendlyByteBuf buf) { ... }
    public static ShipSyncPacket decode(FriendlyByteBuf buf) { ... }
    public static void handle(ShipSyncPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // 只调用 ShipEntity.applySyncPacket，不直接操作字段
            ship.applySyncPacket(pkt.x, pkt.y, pkt.z, pkt.yRot, pkt.xRot, pkt.vx, pkt.vy, pkt.vz);
        });
        ctx.get().setPacketHandled(true);
    }
}
```

**偏离度评估**：当前 3 个 Packet + 3 个 Handler 全在 291 行的 CosmicVoyagePacketHandler.java 中。拆分后每个 Packet 独立文件，Handler 只负责注册。

---

### 1.4 ShipInputHandler — 输入捕获

```java
/**
 * 飞船输入处理器 — 键鼠捕获 + 物理驱动
 * 
 * 职责边界：
 *   - 只负责读取输入状态并驱动 ShipEntity 的物理
 *   - 不处理网络同步（发送 ShipSyncPacket 是职责延伸，允许）
 *   - 不处理渲染、不处理维度切换
 *   - GUI 打开时暂停所有物理更新
 */
@Mod.EventBusSubscriber(modid = CosmicVoyage.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ShipInputHandler {

    // ===== 高度常量（非硬编码） =====
    public static final double LAUNCH_ALTITUDE = 300.0;
    
    // ===== 输入状态查询 =====
    public static boolean isPilotingShip()  // 当前玩家是否骑乘飞船
    
    // ===== 物理驱动（每 tick 调用） =====
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent event)
    
    // ===== GUI 检查（Issue #12） =====
    // if (minecraft.screen != null) return; // 暂停物理
    
    // ===== 自动发射检测 =====
    // OVERWORLD y >= LAUNCH_ALTITUDE → 发送 LaunchToSpacePacket
    // MOON y >= LAUNCH_ALTITUDE → 发送 LaunchToSpacePacket（服务端区分处理）
    
    // ===== FOV 锁定（根治方案） =====
    @SubscribeEvent
    public static void onFovModifier(ComputeFovModifierEvent event)
    // 只保留事件方案，删除 setSprinting(false) 防御层
}
```

**偏离度评估**：当前有三层 FOV 防御（onPlayerTick + onRenderTick + onFovModifier），需清理为单层。缺少月球维度发射检测。缺少 `mc.screen != null` 检查。

---

### 1.5 CelestialBodyRenderer — 天体渲染基类（新增）

```java
/**
 * 天体渲染器抽象基类 — 统一 Earth/Moon 渲染管线
 * 
 * 职责边界：
 *   - 定义 LOD 分层渲染骨架
 *   - 子类只提供位置/半径/颜色参数
 */
public abstract class CelestialBodyRenderer {

    // ===== 子类必须实现的参数 =====
    protected abstract Vec3 getPosition();           // 天体中心世界坐标
    protected abstract float getRadius();             // 渲染半径
    protected abstract int getColorPrimary();         // 主色调 ARGB
    protected abstract int getColorSecondary();       // 次色调 ARGB
    protected abstract boolean hasAtmosphere();       // 是否有大气光晕
    protected abstract String getBodyName();          // 用于日志和调试
    
    // ===== LOD 阈值（可被子类覆盖） =====
    protected float getLODFarDistance()   { return 5000.0f; }
    protected float getLODMidDistance()   { return 1000.0f; }
    protected float getLODNearDistance()  { return 200.0f; }
    protected float getLODInsideDistance(){ return 30.0f; }
    
    // ===== 共用渲染方法 =====
    protected void renderLODFar(PoseStack poseStack, Vec3 relativePos, float distance)
    protected void renderLODMid(PoseStack poseStack, Vec3 relativePos, float distance)
    protected void renderLODNear(PoseStack poseStack, Vec3 relativePos, float distance)
    protected void renderInside(PoseStack poseStack, Vec3 relativePos)
    protected void renderWireSphere(BufferBuilder builder, float radius, int segments, int color)
    protected void renderEquatorLine(BufferBuilder builder, float radius, int color)
    protected void renderAtmosphere(BufferBuilder builder, float radius, int color)
    protected void renderInsideIndicator(PoseStack poseStack, Vec3 relativePos)
    
    // ===== 入口 =====
    @SubscribeEvent
    public void onRenderLevelStage(RenderLevelStageEvent event)
}

// EarthRenderer.java — 缩减到 ~40 行
public class EarthRenderer extends CelestialBodyRenderer {
    @Override protected Vec3 getPosition() { return new Vec3(0, 0, 0); }
    @Override protected float getRadius() { return SpaceConstants.EARTH_RENDER_RADIUS; } // 200.0f
    @Override protected int getColorPrimary() { return 0xFF44AA44; }   // 蓝绿色
    @Override protected int getColorSecondary() { return 0xFF2266CC; } // 海洋蓝
    @Override protected boolean hasAtmosphere() { return true; }
    @Override protected String getBodyName() { return "Earth"; }
}

// MoonRenderer.java — 缩减到 ~35 行
public class MoonRenderer extends CelestialBodyRenderer {
    @Override protected Vec3 getPosition() { return new Vec3(12000, 0, 0); }
    @Override protected float getRadius() { return SpaceConstants.MOON_RENDER_RADIUS; } // ~50.0f
    @Override protected int getColorPrimary() { return 0xFFAAAAAA; }   // 灰白色
    @Override protected int getColorSecondary() { return 0xFF888888; } // 陨石坑灰
    @Override protected boolean hasAtmosphere() { return false; }
    @Override protected String getBodyName() { return "Moon"; }
}
```

**偏离度评估**：EarthRenderer(311行) 和 MoonRenderer(258行) 74-77% 重复。抽取基类后各自压缩到 35-40 行。

---

## 二、SpaceConstants — 常量推导系统

```java
/**
 * 太空维度常量 — 唯一硬编码 EARTH_RADIUS，其余通过比例推导
 * 
 * 设计原则：
 *   - 只有一个源头常量 EARTH_RADIUS = 200.0f
 *   - 所有其他常量基于它比例推导
 *   - 修改 EARTH_RADIUS 即可全局缩放，不会出现不一致
 */
public final class SpaceConstants {
    private SpaceConstants() {} // 禁止实例化
    
    // ===== 唯一硬编码源头 =====
    public static final float EARTH_RADIUS = 200.0f;
    
    // ===== 地球渲染参数 =====
    public static final float EARTH_RENDER_RADIUS = EARTH_RADIUS;           // 200.0f
    public static final float EARTH_ATMOSPHERE_RADIUS = EARTH_RADIUS * 1.2f; // 240.0f
    
    // ===== 月球参数（比例：1/4 地球） =====
    public static final float MOON_RADIUS = EARTH_RADIUS * 0.25f;           // 50.0f
    public static final float MOON_RENDER_RADIUS = MOON_RADIUS;             // 50.0f
    public static final float MOON_DISTANCE = 12000.0f;                     // 太空坐标系中的位置
    
    // ===== 触发距离 =====
    public static final float TRIGGER_LAND_EARTH = EARTH_RADIUS * 2.5f;     // 500.0f — 地球着陆触发
    public static final float TRIGGER_LAND_MOON = MOON_RADIUS * 4.0f;       // 200.0f — 月球着陆触发
    
    // ===== 太空进入坐标（数学上不可能触发着陆） =====
    public static final Vec3 SPACE_ENTRY_POS = new Vec3(
        0.0, 
        EARTH_RADIUS + 100.0f,  // 300.0f — 高于地球表面
        -(EARTH_RADIUS + TRIGGER_LAND_EARTH + 100.0f)  // -800.0f — 距离地球 > 触发阈值
    );
    
    // ===== 月球进入坐标 =====
    public static final Vec3 MOON_ENTRY_POS = new Vec3(
        MOON_DISTANCE,  // 12000.0f
        MOON_RADIUS + 100.0f,  // 150.0f
        0.0
    );
    
    // ===== HUD LOD 阈值 =====
    public static final float HUD_HIDE_DISTANCE = EARTH_RADIUS * 3.0f;      // < 此距离隐藏 HUD（让 3D 球体接管）
}
```

**偏离度评估**：5月11日尝试基于虚拟机文件做常量重构导致引用混乱。本次常量系统基于**实际代码**推导，所有值可由 EARTH_RADIUS 单一源头计算。

---

## 三、P0 修复项与重构方案

### P0-1: ShipEntity 字段封装
- **范围**: `entity/ShipEntity.java` + `network/CosmicVoyagePacketHandler.java`
- **改动**: 
  1. `shipVelocity` → `private Vec3 shipVelocity`
  2. `hasImpulse` → 由 `applySyncPacket()` 内部设置，不暴露
  3. 新增 `applySyncPacket(x,y,z,yRot,xRot,vx,vy,vz)` 方法
  4. PacketHandler 所有 `ship.shipVelocity = ...` / `ship.hasImpulse = ...` 改为 `ship.applySyncPacket(...)`
- **风险**: 5月11日修改此逻辑曾破坏零回跳修复。本次只移动代码，不改动同步时序。
- **验证**: 编译通过后实机测试飞船驾驶，确认零回跳。
- **耗时**: ~2h

### P0-2: SpaceData 双锚点扩展
- **范围**: `data/SpaceData.java` + `network/CosmicVoyagePacketHandler.java` + `command/CvSpaceCommand.java`
- **改动**:
  1. NBT 结构改为 `earthAnchor` + `moonAnchor` 两组字段
  2. 保存时写入 `DATA_VERSION = 2`
  3. 加载时检测旧版本 `anchorX/Y/Z`（v1），自动迁移到 `earthAnchor`
  4. `setAnchor()` → `setEarthAnchor()` / `setMoonAnchor()`
  5. `CvSpaceCommand` 和 PacketHandler 更新方法名
- **耗时**: ~1h

### P0-3: 删除 ModDimensionEffects.java（反射注册）
- **范围**: `client/ModDimensionEffects.java` 整文件删除
- **确认**: `CosmicVoyage.ClientSetup` 已通过 `RegisterDimensionSpecialEffectsEvent` 注册，功能无损失
- **耗时**: ~0.5h

### P0-4: catch(Exception) 全吞 — 随 P0-3 一并消除

### P0-5: PacketHandler 拆分为独立文件
- **范围**: `network/` 目录结构变更
- **改动**:
  1. 新建 `network/packet/` 包
  2. 创建 `ShipSyncPacket.java`、`MoonTransitionPacket.java`、`LaunchToSpacePacket.java`
  3. `CosmicVoyagePacketHandler.java` 缩减到 ~60 行（只保留 SimpleChannel + register 方法）
  4. 每个 Packet 的 encode/decode/handle 自包含
- **耗时**: ~2h

### P0-6: LaunchToSpace 硬编码坐标 → SpaceConstants
- **范围**: `network/CosmicVoyagePacketHandler.java` L261
- **改动**: `(0, 200, -600)` → `SpaceConstants.SPACE_ENTRY_POS`
- **耗时**: ~0.5h

### P0-7: SpaceState 内存泄漏修复
- **范围**: `space/SpaceState.java`
- **改动**: 订阅 `PlayerLoggedOutEvent`，下线时清理 UUID
- **耗时**: ~0.5h

### P0 总计: ~6.5h

---

## 四、已冻结决策清单（Phase 2 不动）

| 决策 | 冻结理由 | 接触风险 |
|------|----------|----------|
| camera-relative 渲染 | VT-1 验证通过，百万格稳定 | 修改会破坏大坐标渲染 |
| 客户端权威 + ShipSyncPacket | 零回跳方案已验证 | 5月11日事故证明风险极高 |
| `move()` AABB 碰撞 | VT-3B 验证通过 | 替换碰撞系统需重新验证 |
| `lerpTo`/`absMoveTo` 阈值覆盖 | 网络同步防护 | 修改会导致回跳/抽搐 |
| 欧拉角 Pitch/Yaw（临时） | Issue #11 四元数后续迭代 | 当前 lastRight 兜底足够 |
| 飞船线框模型 | 占位模型，Phase 3 换 Blockbench | 不影响功能 |
| NavigationHUD 角度差投影 | 已验证稳定 | 换成矩阵投影可能引入坐标系错误 |

---

## 五、Phase 2 启动检查清单

```
□ P0-1  ShipEntity 字段封装 + applySyncPacket()      [预计 2h]
□ P0-2  SpaceData 双锚点扩展                         [预计 1h]
□ P0-3  删除 ModDimensionEffects.java                 [预计 0.5h]
□ P0-5  PacketHandler 拆分为独立文件                  [预计 2h]
□ P0-6  LaunchToSpace 坐标 SpaceConstants 化           [预计 0.5h]
□ P0-7  SpaceState PlayerLoggedOutEvent 清理           [预计 0.5h]

□ 编译通过
□ 实机测试：飞船零回跳                              
□ 实机测试：地球→太空→月球→返回地球 闭环
□ Git 提交并打 tag: v0.4.0-phase2-ready

以上全部完成 → 正式启动 Phase 2（核心太阳系）
```

---

## 六、接口偏离度总结

| 模块 | 当前偏离项 | 修复后偏离度 | 风险等级 |
|------|-----------|------------|---------|
| ShipEntity | public 字段 2 处 | 0% — 全封装 | 🔴 P0 → 🟢 清零 |
| SpaceData | 单锚点 | 0% — 双锚点 | 🔴 P0 → 🟢 清零 |
| Packet 系统 | 三合一 291 行文件 | 0% — 独立文件 | 🔴 P0 → 🟢 清零 |
| ShipInputHandler | 三层 FOV 防御 + 缺 GUI 检查 | ~10% — 常量未全部 Config 化 | 🟡 P1 |
| CelestialBodyRenderer | 无基类，Earth/Moon 各自 250+ 行 | 0% — 基类抽取后 35-40 行 | 🟡 P1 → 🟢 清零 |
| SpaceConstants | 多处硬编码 | 0% — 唯一源头 EARTH_RADIUS | 🟡 P1 → 🟢 清零 |

**整体评估**: 6 个 P0 项修复后，核心模块接口偏离度从 **~35%** 降至 **~5%**（剩余为 P1 级 Config 接入，不影响 Phase 2 启动）。

---

*报告生成: 2026-05-18*  
*Day 3 接口契约固化完成*  
*3 天整固计划全部完成 — 明日可启动 P0 重构执行*
