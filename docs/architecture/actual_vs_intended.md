# CosmicVoyage 架构测绘报告

**日期**: 2026-05-15  
**阶段**: Day 1 — 架构测绘（3天整固计划）  
**输入**: `src/main/java` 完整包结构（22 Java + 4 JSON 文件）  
**产出**: 模块划分图 / 依赖分析 / Forge耦合点 / 高风险区域嗅探

---

## 一、模块划分图

### 1.1 包结构与职责

```
com.Ray1101.cosmicvoyage/
│
├── CosmicVoyage.java              [主入口 + 生命周期管理]
├── Config.java                     [ForgeConfigSpec 配置定义（当前未生效）]
│
├── entity/                         [实体层 — 物理 + 注册]
│   ├── ModEntities.java            [DeferredRegister 实体注册]
│   └── ShipEntity.java             [飞船实体：6DoF物理、碰撞、网络同步防护、骑乘]
│
├── client/                         [客户端渲染层 — 视觉 + 输入 + 过渡（最大包，9个文件）]
│   ├── EarthRenderer.java          [地球缩略球：camera-relative + LOD三层]
│   ├── MoonRenderer.java           [月球缩略球：复用地球渲染逻辑]
│   ├── NavigationHUDRenderer.java  [远距离天体2D导航指示器]
│   ├── SpaceSkyRenderer.java       [1500颗预生成星星，AFTER_SKY渲染]
│   ├── ShipRenderer.java           [飞船线框模型渲染（青色箭头）]
│   ├── ShipInputHandler.java       [WASD+鼠标输入捕获 + 网络同步节流]
│   ├── TransitionHandler.java      [距离触发月球着陆（客户端判定）]
│   ├── ModDimensionEffects.java    [反射注册VoidDimensionEffects（旧方案）]
│   ├── SpaceDimensionEffects.java  [DimensionSpecialEffects子类（新方案）]
│   └── VT1Renderer.java            [VT1百万格精度验证渲染（调试遗留）]
│
├── network/                        [网络层 — 包定义 + 编解码 + 服务端处理]
│   └── CosmicVoyagePacketHandler.java  [3种包(ShipSync/MoonTransition/LaunchToSpace)全在一个文件]
│
├── dimension/                      [维度层 — 纯ResourceKey注册]
│   └── ModDimensions.java          [SPACE + MOON 两个ResourceKey]
│
├── data/                           [数据持久化层 — SavedData]
│   └── SpaceData.java              [单锚点存储（地球返回点）]
│
├── command/                        [命令层 — 调试+管理]
│   ├── CvSpaceCommand.java         [/cvspace enter/return/moon/setanchor/warp]
│   └── VT1TestCommand.java         [/cvtest vt1/vt3/ship — 测试命令]
│
├── space/                          [状态管理层 — 运行时状态]
│   ├── SpaceState.java             [内存Set<UUID>记录"在太空中的玩家"]
│   └── SpaceTickHandler.java       [无重力设置 + 摔落重置 + LivingAttackEvent拦截]
│
└── item/                           [物品层 — 当前为骨架]
    └── ModItems.java               [SHIP_CORE + SPACE_ANCHOR（未实现功能）]
```

### 1.2 各模块职责总结

| 包名 | 文件数 | 核心职责 | 稳定性评级 |
|------|--------|----------|------------|
| `entity` | 2 | 飞船物理模拟 + 实体注册 | A (已冻结) |
| `client` | 10 | 全部渲染 + 输入 + 过渡触发 | B (最大包，职责混杂) |
| `network` | 1 | 网络通信枢纽 | B (三合一，单一职责违规) |
| `dimension` | 1 | 维度标识符注册 | A (纯数据) |
| `data` | 1 | 锚点持久化 | B (单锚点设计，未覆盖月球) |
| `command` | 2 | 调试命令 | A (稳定) |
| `space` | 2 | 运行时状态 + Tick事件 | B (跨职责：重力+伤害拦截) |
| `item` | 1 | 占位注册 | C (骨架，未实现) |
| 根包 | 2 | 入口 + 配置 | C (配置未生效) |

---

## 二、包间依赖分析

### 2.1 依赖方向图

```
                    ┌─────────────┐
                    │   根包(main) │ ←── 启动时注册所有模块
                    └──────┬──────┘
                           │
       ┌───────────────────┼───────────────────┐
       ▼                   ▼                   ▼
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   entity    │◄───│   client    │    │   command   │
│  ShipEntity │    │(渲染+输入)  │    │             │
└──────┬──────┘    └──────┬──────┘    └──────┬──────┘
       │                  │                  │
       │            ┌─────┘                  │
       │            ▼                        │
       │    ┌─────────────┐                  │
       └───►│   network   │◄─────────────────┘
            │(包处理中枢)  │◄────────────────┐
            └──────┬──────┘                 │
                   │                         │
       ┌───────────┼───────────┐             │
       ▼           ▼           ▼             │
┌─────────────┐ ┌────────┐ ┌─────────┐      │
│  dimension  │ │  data  │ │  space  │◄─────┘
│ModDimensions│ │SpaceData│ │SpaceState│
└─────────────┘ └────────┘ └─────────┘
       ▲
       │
┌──────┴──────┐
│    space    │  ← SpaceTickHandler 也依赖 ModDimensions
└─────────────┘
```

### 2.2 依赖矩阵

| 依赖方 \\ 被依赖方 | entity | client | network | dimension | data | command | space | item |
|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| **entity** | — | — | — | ✅ | — | — | — | — |
| **client** | ✅ | — | ✅ | ✅ | — | — | — | — |
| **network** | ✅ | — | — | ✅ | ✅ | — | ✅ | — |
| **dimension** | — | — | — | — | — | — | — | — |
| **data** | — | — | — | — | — | — | — | — |
| **command** | — | — | — | ✅ | ✅ | — | ✅ | — |
| **space** | — | — | — | ✅ | — | — | — | — |
| **item** | — | — | — | — | — | — | — | — |
| **main** | ✅ | ✅ | ✅ | — | — | — | — | — |

### 2.3 关键依赖问题

#### 🔴 问题 D1: network 包 → entity 包（上层依赖下层违规）

- **位置**: `CosmicVoyagePacketHandler.java` 第6行 `import ShipEntity`
- **说明**: 网络层（通信枢纽）直接引用了实体层的具体类 `ShipEntity`，访问其字段 `shipVelocity`、调用 `setPos`/`setYRot` 等。
- **风险**: 网络层与实体实现强耦合，ShipEntity 的任何修改都可能破坏网络同步。
- **理想架构**: 网络层只传输原始数据（DTO），由实体层或专门的同步管理器处理 ShipEntity 的赋值。

#### 🟡 问题 D2: network 包 → data 包（跨层访问）

- **位置**: `CosmicVoyagePacketHandler.java` 第4行 `import SpaceData`
- **说明**: 网络层直接读写 `SpaceData`（SavedData持久化层）。
- **风险**: 数据持久化逻辑泄漏到网络处理中。
- **理想架构**: 由命令层或服务层处理锚点保存，网络层只负责传递请求。

#### 🟡 问题 D3: client 包过大（9个文件，职责混杂）

- **说明**: `client` 包同时包含：
  - 渲染器（Earth/Moon/Sky/Ship/VT1 ×5）
  - 输入处理（ShipInputHandler）
  - 游戏逻辑（TransitionHandler — 距离判定+过渡触发）
  - 维度效果（ModDimensionEffects/SpaceDimensionEffects ×2）
- **风险**: 渲染、输入、游戏逻辑三类职责耦合在同一包内。
- **理想架构**: 拆分为 `client/render`、`client/input`、`client/effects`、`game/transition`。

#### 🟡 问题 D4: space 包跨职责

- **说明**: `SpaceTickHandler` 同时处理：
  1. 无重力设置（物理环境）
  2. 摔落距离重置（伤害系统）
  3. 虚空伤害拦截（伤害系统 — LivingAttackEvent）
- **风险**: 一个类横跨"环境控制"和"伤害系统"两个域。

---

## 三、Forge / Minecraft 原生类耦合点

### 3.1 反射耦合（高风险）

| 文件 | 耦合点 | 风险 |
|------|--------|------|
| `ModDimensionEffects.java` | 反射获取 `DimensionSpecialEffects.EFFECTS` 私有Map | 🔴 高 — Mojang可能随时改字段名/类型，导致注册失败 |
| `ModDimensionEffects.java` | `field.setAccessible(true)` | 🔴 高 — Java模块系统可能阻止 |

**状态**: `ModDimensionEffects` 是旧方案，`CosmicVoyage.ClientSetup` 已用 `RegisterDimensionSpecialEffectsEvent` 替代。但旧代码**未被删除**，存在双注册风险。

### 3.2 原生类直接继承/实现

| 文件 | 继承/实现 | 说明 | 风险 |
|------|-----------|------|------|
| `ShipEntity.java` | `extends Entity` | 直接继承原版实体 | 🟢 正常 — 必要耦合 |
| `ShipRenderer.java` | `extends EntityRenderer<ShipEntity>` | 实体渲染器 | 🟢 正常 |
| `SpaceData.java` | `extends SavedData` | 世界保存数据 | 🟢 正常 |
| `SpaceDimensionEffects.java` | `extends DimensionSpecialEffects` | 维度视觉效果 | 🟢 正常 |
| `Config.java` | `ForgeConfigSpec` | Forge配置系统 | 🟢 正常 |

### 3.3 EventBus 订阅耦合

| 文件 | 订阅事件 | 触发频率 | 风险 |
|------|----------|----------|------|
| `ShipInputHandler.java` | `ClientTickEvent.END` + `PlayerTickEvent` + `RenderTickEvent.START` + `ComputeFovModifierEvent` | 每tick 3次 | 🟡 中 — 高频回调，需确保 early-return |
| `TransitionHandler.java` | `ClientTickEvent.END` | 每tick 1次 | 🟢 正常 — 有 early-return |
| `SpaceTickHandler.java` | `PlayerTickEvent.END` + `LivingAttackEvent` | 每tick 1次 + 事件 | 🟢 正常 |
| `EarthRenderer.java` | `RenderLevelStageEvent.AFTER_SKY` | 每帧 1次 | 🟢 正常 |
| `MoonRenderer.java` | `RenderLevelStageEvent.AFTER_SKY` | 每帧 1次 | 🟢 正常 |
| `SpaceSkyRenderer.java` | `RenderLevelStageEvent.AFTER_SKY` | 每帧 1次 | 🟢 正常 |
| `NavigationHUDRenderer.java` | `RenderGuiOverlayEvent.Post` | 每帧 1次 | 🟢 正常 |
| `ShipRenderer.java` | `EntityRenderersEvent.RegisterRenderers` | 一次性 | 🟢 正常 |
| `VT1Renderer.java` | `RenderLevelStageEvent.AFTER_SOLID_BLOCKS` | 每帧（条件触发） | 🟡 中 — 调试遗留代码 |

### 3.4 关键原生API使用

| 文件 | 原生API | 用途 | 替代难度 |
|------|---------|------|----------|
| `ShipEntity.java` | `move(MoverType.SELF, motion)` | AABB碰撞检测 | 极高（核心机制） |
| `ShipEntity.java` | `lerpTo()` / `absMoveTo()` 覆盖 | 阻断原版同步 | 高（已冻结） |
| `ModDimensionEffects.java` | `Field.setAccessible()` + `field.get()` | 反射注入 | 已替代 |
| `ShipInputHandler.java` | `MouseHandler.xpos()` / `ypos()` | 原始鼠标输入 | 中 |
| `TransitionHandler.java` | `player.sendSystemMessage()` | 客户端发消息 | 低 |

---

## 四、高风险区域嗅探（初步异味）

### 4.1 🔴 P0 级风险（可能引发崩溃/数据丢失）

#### R1: 双 DimensionEffects 注册冲突
- **位置**: `ModDimensionEffects.java`（反射注册）+ `CosmicVoyage.ClientSetup`（事件注册）
- **问题**: 两个类都尝试为 `cosmicvoyage:space` 注册 `DimensionSpecialEffects`。反射方案先执行（FMLClientSetupEvent），事件方案后执行（RegisterDimensionSpecialEffectsEvent）。**后执行的会覆盖先执行的**。如果事件注册时Map已被反射修改，行为不确定。
- **建议**: **删除 `ModDimensionEffects.java`**，只保留事件注册方案。

#### R2: SpaceData 单锚点 vs 双锚点需求
- **位置**: `SpaceData.java`
- **问题**: 当前只有 `anchorX/Y/Z` + `hasAnchor`（地球锚点）。但 `CosmicVoyagePacketHandler.handleLaunchToSpace()` 和 `CvSpaceCommand` 都需要保存/读取锚点。5月11日日志明确提到需要"地球+月球双锚点"。
- **风险**: 月球起飞后无法正确返回太空对应坐标。
- **建议**: 扩展为 `earthAnchor` + `moonAnchor` 双锚点。

#### R3: network 层直接操作 Entity 字段
- **位置**: `CosmicVoyagePacketHandler.handleShipSync()` 第178-184行
- **问题**: 直接设置 `ship.shipVelocity = new Vec3(...)`（访问 public 字段）、调用 `ship.hasImpulse = true`（访问 public 字段）、调用 `ship.setPos()`。
- **风险**: 破坏了 ShipEntity 的封装。5月11日开发日志中" ShipSync 改坏冻结功能"事件与此直接相关。
- **建议**: 将同步逻辑移到 `ShipEntity` 内的方法（如 `applySyncPacket()`），network 层只调用方法。

#### R4: LaunchToSpace 传送坐标硬编码
- **位置**: `CosmicVoyagePacketHandler.handleLaunchToSpace()` 第261行 `(0, 200, -600)`
- **问题**: 5月11日日志中，`(0, 200, 0)` 导致循环传送Bug（距离地球200 < 500触发着陆）。`-600` 是修复后的魔数。
- **风险**: 如果 `TRIGGER_LAND` 或地球位置改变，可能再次出现死循环。
- **建议**: 使用 `SpaceConstants` 推导，确保传送点距离 > 着陆触发距离。

### 4.2 🟡 P1 级风险（影响可维护性）

#### R5: Config 配置未生效
- **位置**: `Config.java` 定义了 `MAX_FLIGHT_SPEED`、`FLIGHT_ACCELERATION` 等
- **问题**: `ShipEntity.java` 使用的是硬编码常量 `FORCE_MAIN = 0.5f`、`MAX_SPEED_TICK = 25.0f` 等，**完全没有读取 Config**。
- **风险**: 配置系统成为摆设，玩家无法调整参数。

#### R6: 三个 Packet 类内聚在 Handler 文件中
- **位置**: `CosmicVoyagePacketHandler.java` 第63-158行
- **问题**: `ShipSyncPacket`、`MoonTransitionPacket`、`LaunchToSpacePacket` 都是 `public static class`，与 Handler 逻辑耦合。
- **风险**: 文件过长（290+行），单一文件职责过重。
- **建议**: 每个 Packet 独立为单独文件（或内部类但至少分离）。

#### R7: EarthRenderer 与 MoonRenderer 大量重复代码
- **位置**: `EarthRenderer.java` (311行) + `MoonRenderer.java` (258行)
- **问题**: `renderWireSphere()`、`renderEquatorLine()`、`renderLODFar()`、`renderInsideIndicator()` 几乎完全相同（仅颜色/半径参数不同）。LOD阈值常量也完全相同。
- **风险**: 修改通用渲染逻辑需要改两个文件，容易遗漏。
- **建议**: 抽取 `CelestialBodyRenderer` 基类或工具类。

#### R8: client 包包含游戏逻辑（TransitionHandler）
- **位置**: `TransitionHandler.java`
- **问题**: 这是"客户端渲染/输入包"，但 `TransitionHandler` 处理的是**游戏核心逻辑**（距离判定→维度切换）。
- **风险**: 渲染包与游戏逻辑耦合，后续拆分困难。
- **建议**: 移到 `game` 或 `transition` 包。

#### R9: SpaceState 内存泄漏风险
- **位置**: `SpaceState.java` 第10行 `Set<UUID> IN_SPACE`
- **问题**: `enterSpace()` 添加 UUID，但 `exitSpace()` 只在 `/cvspace return` 和正常流程中调用。如果玩家崩溃/断线，UUID 永远留在 Set 中。
- **风险**: 长期运行后 Set 无限增长。
- **建议**: 添加定时清理或 PlayerLoggedOutEvent 监听。

#### R10: VT1Renderer 调试遗留代码
- **位置**: `VT1Renderer.java`
- **问题**: 只在 90万~110万格范围渲染红色线框，且使用 `LOGGER.info()` 每帧输出日志。
- **风险**: 生产环境产生大量日志；不必要的渲染开销。
- **建议**: 添加配置开关或标记为 `@Deprecated` 待删除。

### 4.3 🟢 P2 级风险（风格/优化）

#### R11: NavigationHUDRenderer 未使用导入
- **位置**: 第18行 `import org.joml.Quaternionf;` — 已使用  
  但 `Quaternionf` 实际上用于箭头旋转，可以简化。

#### R12: ModItems 骨架占位
- **位置**: `ModItems.java`
- **问题**: `SHIP_CORE` 和 `SPACE_ANCHOR` 已注册但无实际功能代码。
- **风险**: 无直接风险，但属于未完成工作。

#### R13: SpaceSkyRenderer 星星存储方式
- **位置**: `SpaceSkyRenderer.java` 第23行 `List<float[]> STARS`
- **问题**: 使用 `float[]` 而非定义数据结构类，可读性差。
- **建议**: 定义 `record Star(float x, float y, float z, float size, float brightness)`。

#### R14: 硬编码字符串前缀
- **位置**: 多处 `"[CosmicVoyage]"` 前缀
- **建议**: 抽取为常量 `MOD_PREFIX = "[CosmicVoyage]"`。

---

## 五、实际架构 vs 理想架构对比

### 5.1 理想架构（第一阶段目标）

```
com.Ray1101.cosmicvoyage/
│
├── CosmicVoyage.java              [入口]
├── Config.java                     [配置]
│
├── entity/                         [实体层]
│   ├── ShipEntity.java             [6DoF物理 + 碰撞 + 姿态]
│   └── ModEntities.java            [注册]
│
├── physics/                        [物理引擎层 — 未来抽取]
│   └── (ShipPhysics.java)          [推力/阻尼/速度计算]
│
├── render/                         [渲染层 — 纯视觉]
│   ├── celestial/                  [天体渲染]
│   │   ├── EarthRenderer.java
│   │   ├── MoonRenderer.java
│   │   └── CelestialBodyRenderer.java [通用基类]
│   ├── ship/                       [飞船渲染]
│   │   └── ShipRenderer.java
│   ├── sky/                        [天空渲染]
│   │   └── SpaceSkyRenderer.java
│   └── hud/                        [HUD渲染]
│       └── NavigationHUDRenderer.java
│
├── input/                          [输入层]
│   └── ShipInputHandler.java       [键鼠捕获 + 物理驱动]
│
├── network/                        [网络层 — 只传数据]
│   ├── packet/                     [包定义]
│   │   ├── ShipSyncPacket.java
│   │   ├── MoonTransitionPacket.java
│   │   └── LaunchToSpacePacket.java
│   └── PacketHandler.java          [注册 + 分发]
│
├── game/                           [游戏逻辑层]
│   ├── transition/
│   │   └── TransitionHandler.java  [距离判定 + 过渡触发]
│   └── state/
│       ├── SpaceState.java         [运行时状态]
│       └── SpaceTickHandler.java   [环境效果]
│
├── data/                           [数据层]
│   └── SpaceData.java              [双锚点持久化]
│
├── dimension/                      [维度层]
│   └── ModDimensions.java
│
├── command/                        [命令层]
│   ├── CvSpaceCommand.java
│   └── VT1TestCommand.java
│
└── item/                           [物品层]
    └── ModItems.java
```

### 5.2 偏离点总结

| 偏离项 | 当前状态 | 理想状态 | 修复优先级 |
|--------|----------|----------|------------|
| client 包过大 | 9个文件，渲染+输入+逻辑混杂 | 拆为 render/input/game 三个子包 | P1 |
| network 包臃肿 | 3个Packet+3个Handler全在一个文件 | Packet独立文件，Handler只负责分发 | P1 |
| Earth/Moon 重复代码 | 两个200+行文件，渲染逻辑90%相同 | 抽取 `CelestialBodyRenderer` 基类 | P1 |
| SpaceData 单锚点 | 只存地球锚点 | 地球+月球双锚点 | **P0** |
| DimensionEffects 双注册 | 反射+事件两种方案并存 | 只保留事件注册 | **P0** |
| Config 未生效 | 配置定义了但代码不用 | ShipEntity读取Config参数 | P1 |
| SpaceState 内存泄漏 | 无退出清理 | PlayerLoggedOutEvent清理 | P1 |
| network 直接操作Entity | 访问public字段 | 封装为ShipEntity方法 | **P0** |
| VT1Renderer 遗留 | 调试代码在生产环境 | 删除或加开关 | P1 |
| TransitionHandler 包位置 | 在 client 包 | 移到 game/transition 包 | P2 |

---

## 六、核心架构决策复核

### 6.1 已冻结决策（不动）

| 决策 | 状态 | 备注 |
|------|------|------|
| camera-relative 渲染 | ✅ 冻结 | VT-1验证通过 |
| 客户端权威 + ShipSyncPacket | ✅ 冻结 | 零回跳方案 |
| `move()` AABB 碰撞 | ✅ 冻结 | VT-3B验证通过 |
| `lerpTo`/`absMoveTo` 阈值覆盖 | ✅ 冻结 | 网络同步防护 |
| 欧拉角 Pitch/Yaw（临时） | ✅ 冻结 | Issue #11 后续用四元数 |

### 6.2 需重新评估决策

| 决策 | 当前问题 | 建议 |
|------|----------|------|
| `onPassengerTurned` 空实现 | 冻结标记但未解释为何空实现足够 | 添加注释说明原版行为及为何不需要 |
| `SpaceDimensionEffects` + `ModDimensionEffects` 并存 | 双注册可能冲突 | **删除 ModDimensionEffects** |
| `ShipEntity.shipVelocity` public 字段 | network层直接访问 | 改为 private + getter/setter 或包可见 |

---

## 七、第二阶段阻塞项

以下问题如果不修复，将直接影响第二阶段开发：

1. **SpaceData 双锚点**（P0）— 月球返回需要月球锚点
2. **network 层封装**（P0）— 第二阶段新增行星需要新增Packet，当前Handler会成为瓶颈
3. **Config 生效**（P1）— 第二阶段不同行星的重力/速度参数需要配置化
4. **client 包拆分**（P1）— 第二阶段新增渲染器会进一步膨胀 client 包

---

## 八、下一步行动

### Day 2（代码异味大扫除）重点扫描区域

基于本报告，Day 2 应优先扫描以下文件：

1. **`CosmicVoyagePacketHandler.java`** — 魔法数字、封装破坏、过长方法
2. **`EarthRenderer.java` + `MoonRenderer.java`** — 重复代码、魔法数字
3. **`ShipInputHandler.java`** — 过长方法、多层防御代码异味
4. **`SpaceData.java`** — 扩展性、数据完整性
5. **`ModDimensionEffects.java`** — 反射安全、删除评估

### 产出确认

- [x] 模块划分图
- [x] 包间依赖方向分析
- [x] Forge/Minecraft 耦合点标记
- [x] 高风险区域嗅探（14项）
- [x] 实际 vs 理想架构对比
- [x] 第二阶段阻塞项识别

---

*报告生成时间: 2026-05-15*  
*Day 1 架构测绘完成*
