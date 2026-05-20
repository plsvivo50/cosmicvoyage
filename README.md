# Cosmic Voyage / 星际航行

[![Minecraft Version](https://img.shields.io/badge/Minecraft-1.20.1-green.svg)](https://www.minecraft.net/)
[![Forge Version](https://img.shields.io/badge/Forge-47.4.0-orange.svg)](https://files.minecraftforge.net/)
[![Java Version](https://img.shields.io/badge/Java-17-blue.svg)](https://adoptium.net/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE.txt)

> **A compressed Outer Wilds-style space exploration mod.**  
> Fly through a scaled solar system, witness giant planets looming on the horizon, build your own ship as a mobile home, and discover the secrets left by those who came before.
>
> **一个受 Outer Wilds 启发的压缩式太空探索模组。**  
> 驾驶飞船穿越缩比太阳系，在视野边缘凝视巨大的行星缓缓升起，亲手建造自己的飞船家园，发掘前人留下的秘密。

---

## 项目状态 / Project Status

当前版本：**v0.4.2** (Phase 1 整固完成 / Phase 1 Consolidation Complete)

**核心代码状态**：6DoF 飞船飞行、太空/月球维度、天体渲染、维度切换闭环已完成验证，代码整固完毕。
**系统设计状态**：Phase 2 内容扩展的全部系统设计已完成，涵盖飞船建造、资源体系、引力危险、太阳系规划、前哨站与文明遗迹。

---

## 设计哲学 / Design Philosophy

> **真实才能打动人。**
>
> 没有太空海盗，没有外星军队。危险来自引力、辐射、极端温度——物理本身就是敌人。知识即进度，不是等级。飞船不是载具，是家。殖民是游牧的，飞船是帐篷，星球是牧场。

### 核心原则

| 原则 | 说明 |
|:---|:---|
| **探索驱动** | 知识是唯一的进度系统——发现遗迹获得信息，信息指引你去新的地方 |
| **添加式升级** | 每个升级都是新能力（隔热层→可以靠近太阳），不是数值+10 |
| **完全自由** | 所有科技从一开始解锁，无科技树、无 grinding |
| **地球全覆盖** | 地球上所有资源都存在（极稀少），外星球是高效富集区——去外星球是为了效率，不是因为地球没有 |
| **分层复杂度** | 本体轻量完整，联动 Create/MineColonies 后深度可选 |
| **孤独与温暖** | 一个人的宇宙，前人的遗迹，荒凉中的安全感 |

---

## 系统设计文档 / System Design Documents

以下设计文档定义了 Cosmic Voyage 从 Phase 2 开始的所有内容扩展方向。所有文档均已完成框架级设计，进入评审与细化阶段。

### 飞船系统 / Ship System

**飞船不是载具，是家。** 玩家从零建造自己的飞船，带着它探索宇宙。

**核心文档**: [`docs/design/Ship_Design.md`](docs/design/Ship_Design.md)

| 子系统 | 说明 |
|:---|:---|
| **可建造飞船实体** | 类似 Create 的列车/机械，有内部空间，玩家可在内部走动、放置方块 |
| **起飞安全检测** | 必须满足：驾驶舱 + 引擎 + 氧气生成器 + 气密外壳 |
| **核心系统方块** | 标准/轻量/耐热/耐压/抗冻船壳、驾驶舱、引擎、氧气生成器、气闸门、太阳能板、核聚变电池等 |
| **扩展模块** | 环境耐受（隔热层/辐射盾/压力壳/抗冻层）、航程扩展（跃迁引擎/太阳能帆/自动导航）、探索能力（地形扫描仪/钻探设备/采样臂）、空间扩展（货仓/生活舱/实验室/观测穹顶） |
| **蓝图系统** | 遗迹中的前人飞船设计记录——叙事参考，不是解锁机制。玩家可完全自由设计 |

```
升级示例：
  安装隔热层 → 可以靠近太阳/水星（新区域）
  安装跃迁引擎 → 可以自动超光速航行（新功能）
  安装实验室 → 可以分析外星样本（新能力）
```

### 资源与合成系统 / Resource & Crafting

**原版资源为主，行星特产为引导。效率是门槛，不是存在性。**

**核心文档**: [`docs/design/Resource_Crafting_Entity_Design.md`](docs/design/Resource_Crafting_Entity_Design.md)

| 子系统 | 说明 |
|:---|:---|
| **资源四级分级** | 基础资源(地球丰富) → 普通太空资源(地球稀少) → 高级太空资源(地球极稀有) → 极限资源(特定星球富集) |
| **通用资源** | 陨铁(小行星)、冰(月球阴影陨石坑)、硅砂(地球沙漠/火星) |
| **行星特产** | 水星硫晶/钨矿、金星云母、月球钛矿/氦-3、火星赤铁矿/古遗物、木卫二深冰核/生物荧光素、土卫六甲烷冰/托林、冥王星固态氮/未知晶体 |
| **获取方式** | 地表拾取、常规采矿、深层钻探(飞船模块)、大气提取、生物采集 |
| **合成台** | 原版工作台 → 航天装配台 → 精密加工台 → 化学合成台 |
| **合成四级复杂度** | L1原版级 → L2航天级 → L3特产级 → L4稀有级 |

**工具/消耗品/收集品**: 宇航服、采样器、手持扫描仪、焊接工具、锚定枪、氧气瓶、燃料棒、修理包、地质/生物样本、前人日志、蓝图残页、星文拓片。

### 引力危险系统 / Gravity Hazard

**宇宙本身就是敌人。** 没有怪物，只有物理定律。

**核心文档**: [`docs/design/Gravity_Hazard_System.md`](docs/design/Gravity_Hazard_System.md)

| 危险类型 | 说明 |
|:---|:---|
| **太阳引力** | 距离<10000感受到拉扯，<5000需全功率对抗，<3000过热，<2000无法逃脱，<1000烧毁 |
| **洛希极限** | 气态巨行星(木星/土星)附近，距离<3000被引力撕裂 |
| **土星环碎片** | 高速冰块/岩石，高风险高回报采矿 |
| **飞船损坏分级** | 绿区(100-70%)→黄区(70-30%)→红区(30-10%)→黑区(<10%) |
| **损坏类型** | 船体裂纹、引擎过热、电子设备故障、燃料泄漏、结构损伤(不可逆) |
| **引力弹弓** | 高级技巧：以特定角度接近大质量天体，利用引力加速节省燃料 |

### 太阳系规划 / Solar System

**8 个可降落天体 + 原版维度绑定。**

**核心文档**: [`docs/design/SolarSystem_Vision.md`](docs/design/SolarSystem_Vision.md)

| 天体 | 绑定维度 | 类型 | 特征 |
|:---|:---|:---|:---|
| **太阳** | 不可降落 | 恒星 | 发光球体，强引力源 |
| **水星** | 新维度 | 岩石行星 | 灰白，陨石坑，极端温差 |
| **金星** | **下界** | 岩石行星 | 橙黄，硫酸云，462°C |
| **地球** | Overworld | 岩石行星 | 蓝绿，白云，起点 |
| **月球** | 新维度 | 卫星 | 已实现 ✅ |
| **火星** | 新维度 | 岩石行星 | 红色，沙尘，古代遗迹 |
| **木卫二** | 新维度 | 卫星 | 冰白，冰下深海生物 |
| **土卫六** | 新维度 | 卫星 | 橙黄，甲烷湖，微生物 |
| **冥王星** | **末地** | 矮行星 | 紫灰，荒凉，神秘 |

**气态巨行星**（木星/土星）：纯渲染背景，不可降落，巨物感核心来源。

**距离设计**：地球→月球 4秒，地球→火星 10秒，地球→冥王星 80秒（需跃迁）。

### 前哨站与遗迹系统 / Outpost & Ruins

**飞行是核心，建造是辅助。5分钟建好前哨站，然后回到飞船继续飞行。**

**核心文档**: [`docs/design/Design_Decisions.md`](docs/design/Design_Decisions.md)

| 子系统 | 说明 |
|:---|:---|
| **遗迹 (Ruins)** | 散落在星球表面的建筑残骸，提供背景故事碎片，不可修复为功能性建筑 |
| **星文系统 (Astral Script)** | Outer Wilds 式几何图案文字，逐步学习解读，视觉语言不依赖文字 |
| **前哨站 (Outpost)** | 极简模式：放置基础框架→自动生成墙壁，5分钟建站。功能方块自动连接（无线逻辑） |
| **废弃站 (Abandoned Station)** | 世界自动生成，半损坏。提供资源即时修复→功能启用，比从零建造省50%资源 |
| **功能方块** | 气闸门、氧气生成器、太阳能板、飞船对接端口、储物舱、观测窗、通讯天线 |

**复杂度分层**：极简模式(默认) → 标准模式(手动建造) → 硬核模式(Create/MineColonies 联动)。

### 航行系统 / Navigation

| 模式 | 说明 |
|:---|:---|
| **手动模式** | WASD+鼠标精细操控，用于着陆/近距离/看风景 |
| **巡航模式** | Shift+W 激活，速度 10-50 倍，转向大幅减弱，HUD显示航线预测 |
| **跃迁引擎** | 自动超光速航行 100-1000 倍，消耗氦-3（月球特产），燃料=距离×飞船质量 |

### 上古虫洞设定 / Ancient Wormhole Lore

原版传送门在科幻设定中的合理化：

> **传送门不是魔法，是前人遗留的微型虫洞稳定器。** 金星（下界）和冥王星（末地）各有一个虫洞终端。下界 = 前人改造的高温工业世界，末地 = 被引力撕裂的破碎世界。未来可拓展为自建虫洞网络科技线。

---

## 设计蓝图文档索引 / Document Index

### 系统级设计

| 文档 | 内容 | 状态 |
|:---|:---|:---|
| [`Ship_Design.md`](docs/design/Ship_Design.md) | 可建造飞船：核心系统、扩展模块、蓝图、情感设计 | 框架完成 |
| [`Resource_Crafting_Entity_Design.md`](docs/design/Resource_Crafting_Entity_Design.md) | 资源四级分级、合成系统、船体/功能方块、实体设计 | 框架完成 |
| [`Gravity_Hazard_System.md`](docs/design/Gravity_Hazard_System.md) | 引力危险、洛希极限、飞船损坏、生物系统 | 框架完成 |
| [`SolarSystem_Vision.md`](docs/design/SolarSystem_Vision.md) | 8可降落天体、坐标系布局、渲染LOD、巡航/跃迁 | 框架完成 |
| [`Design_Decisions.md`](docs/design/Design_Decisions.md) | 关键决策记录：星文、前哨站复杂度、废弃站、氧气 | 已确认 |

### 愿景与架构

| 文档 | 内容 |
|:---|:---|
| [`FINAL_VISION.md`](docs/design/FINAL_VISION.md) | 最终理想状态：核心循环、飞船、危险、前哨站、氧气、跃迁、虫洞设定 |
| [`cosmicvoyage_dev_guide_v4.3.md`](docs/design/cosmicvoyage_dev_guide_v4.3.md) | 核心机制冻结版：Phase 0-3 开发范围、时间表、Kill Criteria |
| [`cosmicvoyage_dev_guide_v4.4.md`](docs/design/cosmicvoyage_dev_guide_v4.4.md) | 联动扩展草案：Create/MineColonies 异步殖民地（条件触发） |
| [`Dimension Transition Flow.md`](docs/design/Dimension%20Transition%20Flow.md) | 维度切换流程图：地球↔太空↔月球 |
| [`Space-Rendering Architecture.md`](docs/design/Space-Rendering%20Architecture.md) | 太空渲染管线架构 |

---

## 已实现功能 / Implemented Features

### 核心系统（代码已完成）

| 功能 | 描述 | 状态 |
|------|------|------|
| **六自由度飞行** (6DoF Flight) | 基于物理的飞船操控，本地坐标系正交基 | VT-1~4 已通过 |
| **飞船实体** (Ship Entity) | 可驾驶载具，登船/下船，零回跳同步 | 可用 |
| **太空/月球维度** | 独立维度，自定义天空盒与视觉效果 | 可用 |
| **维度切换闭环** | 地球→太空→月球→太空→地球 | 已验证 |
| **CelestialBodyRenderer** | 基类 + Config 模式，支持多行星扩展 | P1-1 完成 |
| **逼近过渡系统** | LOD 三级切换 (Billboard→低模→完整球体+大气) | 已实现 |
| **HUD 导航** | 屏幕空间指示器，三段式 LOD | 已实现 |
| **Config + SpaceConstants** | 物理参数可配置，比例推导 | 已实现 |

### 已设计待实现（Phase 2+）

| 功能 | 状态 |
|:---|:---|
| 可建造飞船实体 | 📝 设计完成 |
| 资源四级分级 + 行星特产 | 📝 设计完成 |
| 引力危险（太阳/洛希极限） | 📝 设计完成 |
| 飞船损坏系统 | 📝 设计完成 |
| 火星/金星等第二行星 | 📝 设计完成 |
| 前哨站 + 星文系统 | 📝 设计完成 |
| 跃迁引擎 + 巡航模式 | 📝 设计完成 |
| 氧气系统 | 📝 设计完成 |

---

## 技术栈 / Tech Stack

- **Java 17**
- **Minecraft Forge 47.4.0** for Minecraft 1.20.1
- **Parchment Mappings** `2023.09.03-1.20.1`
- **Gradle** 构建系统

---

## 环境要求 / Requirements

- Minecraft **1.20.1**
- Minecraft Forge **47.4.0** 或更高版本
- Java **17** 或更高版本

---

## 安装指南 / Installation

### 对于玩家 / For Players

> 当前版本为早期开发阶段，尚未发布正式构建文件。

### 对于开发者 / For Developers

```bash
git clone https://github.com/plsvivo50/cosmicvoyage.git
cd cosmicvoyage
./gradlew build
```

IDE 配置:
```bash
./gradlew genIntellijRuns   # IntelliJ IDEA
./gradlew genEclipseRuns    # Eclipse
```

---

## 项目结构 / Project Structure

```
cosmicvoyage/
├── docs/
│   ├── design/                     # 系统设计文档
│   │   ├── Ship_Design.md
│   │   ├── Resource_Crafting_Entity_Design.md
│   │   ├── Gravity_Hazard_System.md
│   │   ├── SolarSystem_Vision.md
│   │   ├── Design_Decisions.md
│   │   ├── FINAL_VISION.md
│   │   ├── cosmicvoyage_dev_guide_v4.3.md
│   │   ├── cosmicvoyage_dev_guide_v4.4.md
│   │   └── ...                     # 其他架构文档
│   ├── devlog/                     # 每日开发日志
│   └── phase2_prep/               # Phase 1 整固产出
├── src/main/java/...              # Java 源代码
└── ...
```

---

## 路线图 / Roadmap

| 阶段 | 内容 | 状态 |
|:---|:---|:---|
| **Phase 0** | 技术预研：VT-1/2/3/4 验证 | ✅ 已完成 |
| **Phase 1** | 核心机制：6DoF 飞行 + 维度切换 + 天体渲染 | ✅ 已完成 |
| **Phase 1 Consolidation** | 代码整固：架构测绘 + P0-P2 重构 | ✅ 已完成 |
| **Phase 2** | 内容扩展：第二行星 + 太空站 + 新资源 + 飞船建造 | 📝 系统设计完成 |
| **Phase 3** | 生存与危险：氧气、引力、温度、飞船损坏 | 📝 设计完成 |
| **Phase 4** | 多人游戏：合作探索、共享宇宙 | 📝 规划完成 |

---

## 开发日志 / Development Logs

每日开发记录保存在 [`docs/devlog/`](docs/devlog/) 目录。

---

## 贡献指南 / Contributing

这是一个单人 passion project。发现 Bug 或有功能建议，欢迎提交 [Issue](../../issues)。

---

## 许可证 / License

本项目采用 [MIT 许可证](LICENSE.txt) 开源。版权所有 (c) 2026 Ray1101, plsvivo50 and contributors。

---

*Made with stardust by Ray1101.*
