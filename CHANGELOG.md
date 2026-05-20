# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

---

## [v0.4.2-design] - 2026-05-21

### Phase 2 System Design — 内容扩展系统设计完成

**目标**: 完成 Phase 2 及后续阶段的所有内容扩展方向设计，涵盖飞船建造、资源体系、引力危险、太阳系规划、前哨站与遗迹系统。

### Added — 新增设计文档

#### 飞船系统 / Ship System
- **Ship_Design.md** — 可建造飞船完整设计
  - 核心哲学：飞船不是载具，是家
  - 可建造飞船实体（类似 Create 列车/机械，有内部空间）
  - 起飞安全检测（驾驶舱+引擎+氧气+气密外壳）
  - 核心系统方块：标准/轻量/耐热/耐压/抗冻船壳、驾驶舱、引擎、氧气生成器、气闸门、太阳能板、核聚变电池
  - 四大扩展模块类别：环境耐受（隔热层/辐射盾/压力壳/抗冻层）、航程扩展（跃迁引擎/太阳能帆/自动导航）、探索能力（扫描仪/钻探/采样臂）、空间扩展（货仓/生活舱/实验室/观测穹顶）
  - 蓝图系统：前人飞船设计记录，叙事参考而非解锁机制
  - 情感设计：家的感觉、成长感、孤独与温暖

#### 资源与合成系统 / Resource & Crafting
- **Resource_Crafting_Entity_Design.md** — 完整资源体系框架
  - 资源四级分级：基础→普通太空→高级太空→极限
  - 8 行星特产矩阵（硫晶/钨矿/云母/钛矿/氦-3/赤铁矿/深冰核/生物荧光素/甲烷冰/托林/固态氮/未知晶体）
  - 五种采集方式：地表拾取、常规采矿、深层钻探、大气提取、生物采集
  - 四级合成复杂度：原版级→航天级→特产级→稀有级
  - 四种合成台：原版工作台→航天装配台→精密加工台→化学合成台
  - 工具/消耗品/收集品完整清单
  - 实体设计：生物极少（木卫二深海生物、土卫六微生物），无战斗向实体，环境实体（小行星/土星环碎片/太阳风/火山喷发物）

#### 引力危险系统 / Gravity Hazard
- **Gravity_Hazard_System.md** — 物理环境危险设计
  - 太阳引力：距离分层拉扯（10000/5000/3000/2000/1000）
  - 洛希极限：气态巨行星附近的引力撕裂
  - 土星环碎片：高速运动冰块/岩石，高风险高回报采矿
  - 飞船损坏五级系统（绿/黄/红/黑 + 濒毁）
  - 五种损坏类型：船体裂纹、引擎过热、电子设备故障、燃料泄漏、结构损伤（不可逆）
  - 引力弹弓机制：高级飞行技巧
  - 情感弧线：敬畏→紧张→恐惧→解脱→掌控

#### 太阳系规划 / Solar System
- **SolarSystem_Vision.md** — 8 可降落天体 + 维度绑定
  - 8 可降落天体：水星、金星(下界)、地球、月球、火星、木卫二、土卫六、冥王星(末地)
  - 太空坐标系布局（太阳在原点，相邻行星间距 3000-5000）
  - 原版维度绑定：金星↔下界（炎热）、冥王星↔末地（荒凉）
  - 气态巨行星纯渲染背景（不可降落，巨物感来源）
  - 距离设计：地球→月球 4秒，地球→火星 10秒，地球→冥王星 80秒
  - 巡航模式 + 跃迁引擎双航行系统

#### 前哨站与遗迹系统 / Outpost & Ruins
- **Design_Decisions.md** — 关键设计决策确认
  - 星文系统 (Astral Script)：Outer Wilds 式几何图案，逐步学习解读
  - 前哨站复杂度三层：极简(默认，5分钟建站)→标准→硬核(Create联动)
  - 废弃站修复：资源即时修复，联动接口
  - 氧气系统：非主世界维度必需，获取容易，溺水式惩罚
  - 已确认 10 项关键决策汇总

#### 最终愿景 / Final Vision
- **FINAL_VISION.md** — 完整游戏愿景
  - 核心循环：探索→发现→理解→建造
  - 跃迁引擎：100-1000倍超光速，消耗氦-3，燃料=距离×飞船质量
  - 上古虫洞设定：原版传送门合理化（前人微型虫洞稳定器）
  - 虫洞科技线：Phase 3+ 自建虫洞网络
  - 多人游戏架构预留
  - 设计哲学检查清单

### Updated
- **README.md** — 全面重写，以系统设计文档为核心内容
- **CHANGELOG.md** — 新增本条目，记录所有设计文档内容摘要

---

## [v0.4.2] - 2026-05-20

### Phase 1 Consolidation — 代码整固

**目标**: 消除代码异味，建立可维护的架构基础，为 Phase 2 内容扩展做准备。

**Git 统计**: 19 files changed, 574 insertions(+), 708 deletions(-)

### Architecture
- **P1-1** `CelestialBodyRenderer` 基类抽取 — EarthRenderer(315→33 行) + MoonRenderer(261→31 行)，Config 模式支持第二行星扩展
- **P0-5** PacketHandler 拆分 — 291 行三合一拆为 5 个独立 Packet 文件 + 60 行注册中心 (ShipSync/MoonTransition/LaunchToSpace/LandOnEarth/ReturnToSpace)
- **Client 分包** — 9 文件拆为 `render/input/game` 三子包

### Systems
- **P0-2** SpaceData 双锚点 — 支持地球+月球锚点，v1→v2 NBT 自动迁移
- **P0-6** `SpaceConstants` 常量系统 — 唯一源头 `EARTH_RADIUS`，全部比例推导
- **P1-2** Config 系统 — 7 项物理参数可配置，默认值从 SpaceConstants 读取
- **P0-7** SpaceState 内存泄漏修复 — PlayerLoggedOutEvent 自动清理

### Bug Fixes
- **维度切换闭环** — 地球→太空(0,400,-1200)→月球(12000,150,0)→太空→地球锚点
- **月球返回太空** — 新建 ReturnToSpacePacket + TransitionHandler 双维度检测，y≥200 自动触发
- **LandOnEarth safeY** — `Math.min(targetY+2, LAUNCH_HEIGHT-10)` 防止返回死循环
- **Moon Y 轴限制** — `min_y` -2032 → -64（与主世界一致）

### Code Quality
- **P1-8** System.out.println → LOGGER（EarthRenderer, MoonRenderer）
- **P1-9/10** 命名常量 — COLLISION_DAMPING, GIMBAL_LOCK_THRESHOLD_SQR, SIDE_THRUST_RATIO
- **P1-6** yRot 归一化 — `Mth.wrapDegrees` 防止太阳 360° 跳变
- **P1-4** 下降键独立 KeyMapping — 替代 `keySprint`/`LeftCtrl`
- **P2-1** MOD_PREFIX 常量化 — 所有 `[CosmicVoyage]` 前缀统一
- **P2-2** SpaceSkyRenderer `float[]` → `Star` 记录类

### License
- 切换至 MIT License（原 Forge MDK 默认 LGPL 2.1 仅覆盖工具链）

---

## [v0.3.x] - 2026-05-09 ~ 2026-05-13

### Phase 1: Core Mechanisms — 核心机制验证

### 2026-05-09 (v0.1.0-dev)
- VT-1/2/3/4 全部通过 — 百万格精度、碰撞检测、长距离飞行验证
- 太空维度创建与注册 — ResourceKey + dimension_type/dimension JSON
- 飞船实体 — 惯性物理、六自由度、`move()` 碰撞、骑乘系统
- 星空渲染 — 1500 颗预生成星星，SpaceSkyRenderer
- 网络同步 — ShipSyncPacket，客户端权威 + 服务端信任模式
- `/cvspace` 命令系统 — enter/return/setanchor/warp

### 2026-05-10 (v0.2.0-dev)
- 地球缩略球渲染 — EarthRenderer，camera-relative + LOD 三层
- 月球渲染 + 维度注册 — MoonRenderer，ModDimensions.MOON
- 飞船零回跳同步 — lerpTo 阈值 + absMoveTo 覆盖 + positionRider 同步
- 逼近过渡系统原型 — TransitionHandler + MoonTransitionPacket
- 月球环境 — the_end 效果 + 无重力 + 星空双维度覆盖

### 2026-05-11 (v0.2.1-dev)
- 碰撞反馈 — 撞墙速度衰减 scale(0.3)
- 地球尺寸放大 — EARTH_RENDER_RADIUS 50→200
- NavigationHUDRenderer — 远距离天体 2D 屏幕空间指示器，三段式 LOD
- 输入映射调整 — Space 上升/Ctrl 下降，清除原版骑乘跳跃冲突

### 2026-05-12 (v0.3.0-dev)
- **6DoF 飞行物理重构** — 本地坐标系正交基 (forward/right/localUp)，纯净 DeltaMovement
- **虚空伤害修复** — 三重方案：自定义 DimensionSpecialEffects + LivingAttackEvent 拦截 + dimension_type 高度扩大
- **FOV 缩放修复** — ComputeFovModifierEvent 根治空格键疾跑判定
- **万向节锁临时防御** — pitch≈±90° 退化时继承 lastRight
- 鼠标-飞船完全同步 — 视角 = 飞船姿态，Outer Wilds 式沉浸感

### 2026-05-13
- **许可证切换 MIT** — 原创代码 MIT，Forge/MCP/Mojang 映射仍受原许可约束
- **3 天整固计划启动** — 架构测绘 → 异味大扫除 → 接口契约固化
- 工程隐患识别 — 上下文污染、耦合度隐性上升、重复造轮子、边界侵蚀

---

## [v0.2.x] - 2026-04

### Prototype — 原型验证

- 飞船实体原型
- 基础渲染管线
- 维度系统原型

---

## [v0.1.x] - 2026-03

### Initial Setup — 项目初始化

- Forge 开发环境搭建
- 项目骨架创建（从 tutorialmod 迁移到 cosmicvoyage）
- 基础资源文件

---

## 版本号规则

| 版本段 | 含义 |
|:---|:---|
| Major (X.0.0) | 大版本更新，可能破坏存档兼容性 |
| Minor (0.X.0) | Phase 级别更新，新内容阶段 |
| Patch (0.0.X) | 同 Phase 内的重构/BugFix |
