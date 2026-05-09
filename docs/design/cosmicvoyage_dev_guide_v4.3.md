# CosmicVoyage 工程开发指南 v4.3（工程冻结版）

**版本**：v4.3-FROZEN  
**状态**：已冻结，开发中  
**最后修订**：2026-05-09  
**适用范围**：Minecraft Java 1.20.1 / Forge 47.4.0  
**开发模式**：单人开发 | 单一核心Mod

---

## 核心执行令

> 先完成第一艘飞船。先飞向月球。先让测试者说出"哇"。
> 在此之前，禁止讨论殖民帝国。

本指南为 CosmicVoyage Mod 的唯一技术蓝图。任何偏离本指南的设计决策必须经过 Kill Criteria 评审。

---

## 一、项目概述

### 1.1 愿景

CosmicVoyage 是一个 Minecraft 太空探索 Mod，核心体验为：从地球发射飞船、穿越太空、抵达月球并着陆。核心情感目标是让玩家在首次看到月球表面时产生"哇"的震撼感。

### 1.2 技术定位

- **单一核心Mod**：不依赖其他Mod，不开发兼容层
- **单人开发**：所有代码、美术、设计由一人完成
- **Forge API 原生**：基于 1.20.1 Forge，不使用 Fabric/NeoForge/混合加载器
- **工程冻结**：v4.3 版本已冻结，开发过程中不允许新增功能范围

### 1.3 交付定义

| 层级 | 定义 |
|:---|:---|
| **硬性交付（Hard Deliverable）** | 发射 -> 太空 -> 飞向月球 -> 着陆 |
| **期望交付（Expected Deliverable）** | + 返回地球闭环 |
| **降级（Fallback）** | 返航推迟至 v1.1 版本 |

---

## 二、开发路线图

### 2.1 总体时间线
第 0 阶段：技术预研        [2周]
第一阶段：地月首航闭环      [3-4个月]
第二阶段：核心太阳系        [4-6个月]
第三阶段：网络与 Polish     [3-5个月]
────────────────────────────────────
总计：9-15个月（Hard Deliverable 在第1阶段末完成）
plain
复制

### 2.2 阶段状态总览
[第0阶段] VT-1/2/3/4  技术预研      [############] 100% 已完成
[第一阶段] 地月首航    核心闭环      [##..........] 20%  进行中
[第二阶段] 核心太阳系   扩展内容      [............] 0%   未开始
[第三阶段] 网络与Polish 长期打磨     [............] 0%   未开始
plain
复制

---

## 三、Kill Criteria（项目终止红线）

以下任一条件触发，项目立即进入评估/终止流程：

| 编号 | Kill Criteria | 说明 |
|:---:|:---|:---|
| KC-1 | VT 验证失败 | 任一 VT（VT-1/2/3/4）未通过，核心技术路径不可行 |
| KC-2 | 架构死胡同 | 关键架构决策（如网络同步方案）被证明无法收敛 |
| KC-3 | 性能天花板 | 目标硬件上帧率无法维持在 30fps 以上 |
| KC-4 | 范围蔓延 | 新增功能超出本指南定义的范围 |
| KC-5 | 开发停滞 | 连续 2 周无有效代码产出 |

**Kill Criteria 触发后流程**：停止开发 -> 24 小时内书面分析 -> 决定继续/降级/终止。

---

## 四、第 0 阶段：技术预研（2周）

### 4.1 阶段目标

验证核心技术假设。全部 VT 通过后才能进入第一阶段。本阶段**不写生产代码**，只写验证代码（测试命令、渲染器原型、虚拟实体）。

### 4.2 验证任务（VT）

| 编号 | 验证目标 | 通过标准 | 测试方法 |
|:---:|:---|:---|:---|
| **VT-1** | 1,000,000 格距离渲染无浮点抖动 | 误差 < 0.1 格，抖动 < 1px | 传送至百万格外，观察固定参照物是否亚像素稳定 |
| **VT-2** | 异步维度预加载耗时可控 | 原版 4s 预加载，90 秒巨物窗口内不可感知 | 计时维度切换 + 玩家感知测试 |
| **VT-3A** | 500 格/秒直线飞行 | 速度达标，零抖动 | `/cvtest vt3` 高速飞行测试 |
| **VT-3B** | CCD 碰撞检测有效 | 25 格/tick 速度撞 1 格厚墙不穿模 | 建造 1 格厚墙，全速撞击验证 |
| **VT-3C** | 跨 Chunk 飞行稳定 | 无穿模/崩溃，帧率 > 30 | 长距离直线飞行跨多 Chunk |
| **VT-4** | 太空维度长距离飞行性能 | 内存 < 500MB 增量，帧率波动 < 10 | 5 分钟持续飞行，监控 GC 和帧率 |

### 4.3 VT 测试产出物

- 测试命令文件：`VT1TestCommand.java`
- 测试渲染器：`VT1Renderer.java`
- 测试报告：记录在开发日志中

### 4.4 VT 验证结果记录

| VT | 日期 | 结果 | 备注 |
|:---:|:---:|:---:|:---|
| VT-1 | 2026-05-09 | 通过 | camera-relative 渲染，百万格亚像素稳定 |
| VT-2 | 2026-05-09 | 通过 | 原版 4s 预加载，90 秒窗口充裕 |
| VT-3A | 2026-05-09 | 通过 | 500 格/秒，自定义 ShipEntity 实现 |
| VT-3B | 2026-05-09 | 通过 | `move()` AABB 碰撞，25 格/tick 不穿模 |
| VT-3C | 2026-05-09 | 通过 | 虚空维度无 Chunk 压力 |
| VT-4 | 2026-05-09 | 通过 | 5 分钟飞行，120fps 稳定，GC 正常 |

---

## 五、第一阶段：地月首航闭环（3-4个月）

### 5.1 阶段目标

完成从地球发射、进入太空、飞向月球、着陆月球的完整闭环。这是唯一的 Hard Deliverable。

### 5.2 任务分解

#### 周次 1-2：太空维度基础环境

| 任务 | 产出 | 验收标准 |
|:---|:---|:---|
| 太空维度注册 | `ModDimensions.java` + JSON 配置 | `/cvspace enter` 成功进入 |
| 维度类型配置 | 无昼夜、无天空光照、无重力 | 进入后环境为纯黑虚空 |
| 星空背景 | `SpaceSkyRenderer.java` | 1500+ 颗稳定星星，缓慢旋转 |
| 维度切换命令 | `/cvspace enter` / `return` | 主世界 <-> 太空双向切换 |
| 基岩层处理 | 移除太空维度底部基岩生成 | 无基岩层，纯虚空 |

**技术要点**：
- DimensionType JSON 必须包含 `monster_spawn_light_level: 0`（1.20.1 必需字段）
- 使用 `the_void` biome
- 星空使用预生成数据，避免每帧随机导致闪烁

#### 周次 3-4：飞船实体系统

| 任务 | 产出 | 验收标准 |
|:---|:---|:---|
| 实体注册 | `ModEntities.java` | DeferredRegister 正确注册 |
| 飞船物理 | `ShipEntity.java` | 六自由度惯性飞行 |
| 碰撞检测 | `move()` AABB 集成 | 25 格/tick 不穿模 |
| 输入处理 | `ShipInputHandler.java` | WASD + Space/Ctrl 控制 |
| 网络同步 | `CosmicVoyagePacketHandler.java` | 零回跳同步 |
| 渲染器 | `ShipRenderer.java` | 线框模型跟随朝向 |
| 召唤命令 | `/cvtest ship` | 生成飞船 + 测试平台 |

**技术要点**：
- 客户端权威物理（client-authoritative）
- `isControlledByLocalInstance()` 阻止服务端覆盖
- 自定义 `ShipSyncPacket` 绕过原版载具同步
- `noPhysics = false`，使用 `move()` 而非 `setPos()` 保证碰撞
- 物理参数：`FORCE_MAIN = 0.5f`, `FORCE_VERTICAL = 0.3f`, `DAMPING = 0.015f`, `MAX_SPEED_TICK = 25.0f`, `YAW_SPEED = 2.5f`

#### 周次 5-6：地球缩略球 + 过渡系统A

| 任务 | 产出 | 验收标准 |
|:---|:---|:---|
| 地球缩略球渲染 | `EarthRenderer.java` | 太空中可见蓝色球体 |
| Camera-relative 位置 | 相对相机坐标系 | 百万格距离无抖动 |
| LOD 分层 | 远:光点 -> 中:低模球 -> 近:完整球 | 距离自适应切换 |
| 大气光晕 | 近距离球体边缘发光效果 | 视觉逼近感 |
| 逼近判定 | 进入轨道距离触发过渡 | 距离阈值检测 |

**技术要点**：
- 复用 VT-1 camera-relative 渲染方案
- LOD 切换距离阈值需调参
- 地球位置固定在 `(0, 1000000, 0)` 附近

#### 周次 7-8：月球球体 + 月球维度

| 任务 | 产出 | 验收标准 |
|:---|:---|:---|
| 月球维度注册 | 月球 DimensionType + dimension JSON | 可进入月球维度 |
| 月球表面地形 | 自定义地形生成器 | 灰色坑洼表面，低重力 |
| 月球缩略球 | `MoonRenderer.java` | 太空中可见灰色球体 |
| 着陆判定 | 进入月球表面距离触发着陆 | 自动/手动着陆 |
| 月球环境 | 低重力、无大气、特殊光照 | 物理感与地球不同 |

#### 周次 9-12：返航过渡 + 视听打磨 + "哇"验收

| 任务 | 产出 | 验收标准 |
|:---|:---|:---|
| 返航过渡系统 | 月球起飞 -> 太空 -> 地球 | 完整返回闭环 |
| 发射特效 | 粒子效果 + 屏幕震动 | 发射有冲击感 |
| 再入特效 | 大气层再入粒子/着色器 | 视觉反馈 |
| 音效打磨 | 引擎声、碰撞声、环境音 | 沉浸感 |
| "哇"验收 | 首次月球着陆体验 | 测试者主观评价 |

---

## 六、第二阶段：核心太阳系（4-6个月）

> **前提条件**：第一阶段 Hard Deliverable 已通过验收。

### 6.1 火星

- 低重力环境（约为地球 0.38g）
- 沙尘暴天气效果
- 红色地表色调

### 6.2 木星轨道 + 木卫二

- 木星作为巨大球体渲染（轨道震撼感）
- 木卫二冰面地表
- 木星大红斑等视觉特征

### 6.3 金星

- 地狱增强：橙黄色调
- 窒息效果（无氧环境）
- 极端高温伤害

---

## 七、第三阶段：网络与 Polish（3-5个月）

### 7.1 Phase 2.5：基础设施

- **Orbital Beacon**：轨道信标导航系统
- **Docking Array**：空间站对接机制
- **Relay Network**：星际通讯网络

### 7.2 Phase 3：远天体

- 土星/土卫六
- 冥王星 = 末地增强
- 地平线曲率着色器

### 7.3 通用优化

- 维度销毁机制（内存管理）
- 性能优化
- 多语言支持

---

## 八、技术架构规范

### 8.1 已冻结的架构决策

以下决策已通过 VT 验证，开发过程中**不得更改**：

| 决策 | 选择 | 验证依据 |
|:---|:---|:---|
| 坐标精度方案 | camera-relative 渲染 | VT-1 |
| 飞船同步方案 | 客户端权威 + 自定义网络包 | VT-3A/3B |
| 碰撞检测 | 原版 `move()` AABB | VT-3B |
| 网络通道 | 独立 `SimpleChannel` | VT-3A/3B/4 |
| 星空渲染 | 预生成 + DEBUG_LINES | VT-4 |

### 8.2 代码规范

- 包结构：`com.cosmicvoyage.{subsystem}`
- 命名：`PascalCase` 类名，`camelCase` 方法/变量，`SCREAMING_SNAKE_CASE` 常量
- 注册：使用 Forge `DeferredRegister` 机制
- 网络：独立 `SimpleChannel`，不使用原版实体同步
- 资源位置：`ResourceLocation.fromNamespaceAndPath()`（非过时构造函数）

### 8.3 性能基线

| 指标 | 目标 |
|:---|:---|
| 最低帧率 | 30 fps |
| 目标帧率 | 120 fps |
| 内存增量 | < 500MB（长距离飞行） |
| GC 频率 | 无频繁 Full GC |

---

## 九、文件组织规范

### 9.1 代码文件结构
src/main/java/com/cosmicvoyage/
├── CosmicVoyage.java              # 主类/Mod入口
├── dimension/
│   └── ModDimensions.java         # 维度注册
├── entity/
│   ├── ModEntities.java           # 实体注册
│   ├── ShipEntity.java            # 飞船实体+物理
│   └── ShipInputHandler.java      # 输入处理
├── client/
│   ├── SpaceSkyRenderer.java      # 星空渲染
│   └── ShipRenderer.java          # 飞船渲染器
├── network/
│   └── CosmicVoyagePacketHandler.java  # 网络同步
├── command/
│   ├── CvSpaceCommand.java        # 维度命令
│   └── VT1TestCommand.java        # 测试命令
└── data/                          # 数据层（前期）
├── SpaceState.java
└── SpaceData.java
plain
复制

### 9.2 资源文件结构
src/main/resources/
├── data/cosmicvoyage/
│   ├── dimension_type/
│   │   └── space.json             # 太空维度类型
│   └── dimension/
│       └── space.json             # 太空维度定义
└── assets/cosmicvoyage/
└── ...                        # 模型、纹理、音效
plain
复制

### 9.3 文档文件结构
docs/
├── devlog/                        # 开发日志（按日期归档）
│   └── devlog_2026-05-09.md
├── design/                        # 设计文档
│   └── cosmicvoyage_dev_guide_v4.3.md  # 本文件
├── architecture/                  # 架构决策记录（ADR）
├── vt-reports/                    # VT测试报告
└── api/                           # 对外接口文档（如有）
plain
复制

---

## 十、变更控制

### 10.1 冻结范围

v4.3 冻结以下内容：

- 阶段划分和时间线
- Kill Criteria 列表
- VT 验证标准
- 已冻结的架构决策（8.1节）
- Hard Deliverable 定义

### 10.2 允许变更

以下可在开发过程中调整：

- 周次内的任务顺序
- 具体实现方案（不影响架构决策的前提下）
- 视觉参数（LOD阈值、颜色、大小等）
- 物理参数微调（在VT验证范围内）

### 10.3 变更流程

任何涉及冻结范围的变更必须经过：
1. 书面变更申请（变更内容 + 理由 + 影响分析）
2. Kill Criteria 兼容性检查
3. 批准后方可执行

---

## 附录A：VT 测试命令速查

| 命令 | 用途 |
|:---|:---|
| `/cvtest vt1` | 传送至百万格外，验证浮点精度 |
| `/cvtest vt3` | 高速飞行测试 |
| `/cvtest ship` | 召唤飞船 + 生成玻璃测试平台 |
| `/cvspace enter` | 进入太空维度 |
| `/cvspace return` | 返回主世界 |

## 附录B：关键参数速查

### B.1 飞船物理参数

| 参数 | 值 | 说明 |
|:---|:---|:---|
| `FORCE_MAIN` | 0.5f | 主推力（W/S） |
| `FORCE_VERTICAL` | 0.3f | 垂直推力（Space/Ctrl） |
| `DAMPING` | 0.015f | 速度阻尼系数 |
| `MAX_SPEED_TICK` | 25.0f | 最大速度（格/tick） |
| `YAW_SPEED` | 2.5f | 偏航速度（度/tick） |

### B.2 换算

- 25 格/tick = 500 格/秒（20 tps）
- 100万格 = 1000公里（游戏尺度）

## 附录C：资源位置速查

```java
// Mod ID
public static final String MOD_ID = "cosmicvoyage";

// 维度
ModDimensions.SPACE  // cosmicvoyage:space

// 实体
ModEntities.SHIP     // cosmicvoyage:ship

// 网络通道
CosmicVoyagePacketHandler.INSTANCE  // "cosmicvoyage:main"
文档结束
先完成第一艘飞船。先飞向月球。先让测试者说出"哇"。