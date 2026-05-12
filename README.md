# Cosmic Voyage / 星际航行

[![Minecraft Version](https://img.shields.io/badge/Minecraft-1.20.1-green.svg)](https://www.minecraft.net/)
[![Forge Version](https://img.shields.io/badge/Forge-47.4.0-orange.svg)](https://files.minecraftforge.net/)
[![Java Version](https://img.shields.io/badge/Java-17-blue.svg)](https://adoptium.net/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE.txt)

> **A compressed Outer Wilds-style space exploration mod.**  
> Fly through a scaled solar system, witness giant planets looming on the horizon, and discover hidden secrets in the Nether and the End.
>
> **一个受 Outer Wilds 启发的压缩式太空探索模组。**  
> 驾驶飞船穿越缩比太阳系，在视野边缘凝视巨大的行星缓缓升起，在下界与末地中发掘被隐藏的秘密。

---

## 项目状态 / Project Status

当前版本：**v0.0.1 早期开发阶段** (Pre-Alpha)

该项目正处于核心机制验证期，已实现 6DoF 飞船飞行、太空维度、天体渲染与维度切换等核心功能。  
详细开发进展请查阅 [`docs/devlog/`](docs/devlog/) 目录下的每日开发日志。

---

## 已实现功能 / Implemented Features

### 核心系统 / Core Systems

| 功能 | 描述 | 状态 |
|------|------|------|
| **六自由度飞行** (6DoF Flight) | 基于物理的飞船操控，支持前后左右上下 + 俯仰偏航翻滚 | VT-1~4 已通过 |
| **飞船实体** (Ship Entity) | 可驾驶的载具实体，支持登船/下船交互 | 可用 |
| **太空维度** (Space Dimension) | 独立的太空维度，自定义天空盒与视觉效果 | 可用 |
| **自定义维度效果** (Dimension Effects) | 太空维度的定制化视觉渲染（天空、 fog、光照） | 已实现 |
| **维度切换** (Dimension Travel) | 从地球表面发射进入太空，从太空降落至星球 | 可用 |
| **虚空伤害修复** (Void Damage Fix) | 太空维度中虚空伤害的特殊处理 | 已修复 |

### 渲染与视觉 / Rendering & Visuals

| 功能 | 描述 | 状态 |
|------|------|------|
| **地球 + 月球渲染** | 基于逼近过渡算法的天体渲染，随距离变化呈现不同细节层级 | 已实现 |
| **逼近过渡系统** (Approach Transition) | 从远景光点平滑过渡到近景实体表面的渲染管线 | 已实现 |
| **HUD 导航** (HUD Navigation) | 飞船驾驶时的平视显示导航信息 | 已实现 |

### 物理与交互 / Physics & Interaction

| 功能 | 描述 | 状态 |
|------|------|------|
| **飞船控制** (Ship Controls) | 键盘 + 鼠标的飞船操控方案，含惯性模拟 | 已修复 |
| **碰撞检测** (Collision Detection) | 飞船与地形/实体的碰撞反馈 | 已实现 |
| **自动着陆姿态** (Auto-Landing Alignment) | 接近星球表面时速度向量自动对齐 + 姿态调整 | 已修复 |
| **飞船零回跳** (Zero Bounce Landing) | 着陆时防止飞船异常弹跳 | 已修复 |

### 网络与调试 / Network & Debug

| 功能 | 描述 | 状态 |
|------|------|------|
| **网络包处理** (Packet Handler) | 服务端-客户端同步的自定义网络协议 | 已实现 |
| **调试命令** (`/cvspace`) | 太空维度快速传送与状态查询 | 可用 |
| **VT 测试命令** (`/vt1test`) | 验证测试专用指令集 | 可用 |

---

## 技术栈 / Tech Stack

- **Java 17**
- **Minecraft Forge 47.4.0** for Minecraft 1.20.1
- **Parchment Mappings** `2023.09.03-1.20.1`（提供参数名与 JavaDoc）
- **Gradle** 构建系统

---

## 环境要求 / Requirements

- Minecraft **1.20.1**
- Minecraft Forge **47.4.0** 或更高版本
- Java **17** 或更高版本

---

## 安装指南 / Installation

### 对于玩家 / For Players

> 注意：当前版本为早期开发阶段，尚未发布正式构建文件。如需体验，请参考下方开发环境搭建自行编译。

1. 确保已安装 Minecraft 1.20.1 与 Forge 47.4.0+
2. 下载本模组的构建产物 `.jar` 文件
3. 将 `.jar` 放入游戏目录的 `mods/` 文件夹中
4. 启动游戏

### 对于开发者 / For Developers

```bash
# 克隆仓库
git clone https://github.com/plsvivo50/cosmicvoyage.git
cd cosmicvoyage

# 使用 Gradle Wrapper 构建
./gradlew build

# 构建产物位于 build/libs/ 目录
```

#### IDE 配置 / IDE Setup

**IntelliJ IDEA (推荐)**:
```bash
./gradlew genIntellijRuns
```
然后在 IDEA 中导入项目并刷新 Gradle。

**Eclipse**:
```bash
./gradlew genEclipseRuns
```

如遇依赖问题，可尝试：
```bash
./gradlew --refresh-dependencies
./gradlew clean
```

---

## 项目结构 / Project Structure

```
cosmicvoyage/
├── docs/
│   ├── design/                     # 开发蓝图 (v4.3/v4.4)
│   │   ├── cosmicvoyage_dev_guide_v4.3.md
│   │   └── cosmicvoyage_dev_guide_v4.4.md
│   └── devlog/                     # 每日开发日志
│       ├── devlog_2026-05-09.md
│       ├── devlog_2026-05-10.md
│       ├── devlog_2026-05-11.md
│       ├── devlog_2026-05-11-a.md
│       ├── devlog_2026-05-12.md
│       └── devlog_2026-05-12-a.md
├── src/main/java/com/Ray1101/cosmicvoyage/
│   ├── client/                     # 客户端渲染逻辑
│   ├── command/                    # 游戏内命令 (/cvspace, /vt1test)
│   ├── data/                       # 数据生成 (DataGen)
│   ├── dimension/                  # 自定义维度注册与管理
│   ├── entity/                     # 实体定义 (飞船等)
│   ├── item/                       # 物品注册
│   ├── network/                    # 网络通信包
│   ├── space/                      # 太空状态机与 Tick 处理器
│   ├── test/                       # 验证测试 (VT)
│   ├── Config.java                 # 模组配置文件
│   └── CosmicVoyage.java           # 主入口类
├── src/main/resources/
│   └── assets/cosmicvoyage/        # 模型、纹理、音效等资源
├── build.gradle                    # Gradle 构建配置
├── gradle.properties               # 模组元数据与版本
├── changelog.txt                   # Forge 更新日志
└── fix_bom.py                      # BOM 编码修复脚本
```

---

## 设计蓝图 / Design Blueprint

项目遵循精心制定的开发蓝图进行迭代：

- **[`docs/design/cosmicvoyage_dev_guide_v4.3.md`](docs/design/cosmicvoyage_dev_guide_v4.3.md)** — 核心机制冻结版，定义第一至第三阶段的开发范围、时间表与终止条件 (Kill Criteria)。
- **[`docs/design/cosmicvoyage_dev_guide_v4.4.md`](docs/design/cosmicvoyage_dev_guide_v4.4.md)** — 联动扩展草案，规划与 Create / MineColonies 等模组的异步殖民地联动，需在"社区需求 + 核心稳定"满足后启动评审。

核心设计哲学：

> **殖民是游牧的。飞船是帐篷。星球是牧场。**  
> 一切殖民系统的存在目的只有一个：让玩家更想飞。  
> 不是管理，是牵挂。不是定居，是连接。不是扎根，是再启航。

---

## 开发日志 / Development Logs

每日开发记录保存在 [`docs/devlog/`](docs/devlog/) 目录，按日期命名。日志包含：

- 当日完成的功能点
- 遇到的工程问题与解决方案
- 架构决策记录 (ADR)
- 回退与重构复盘

---

## 路线图 / Roadmap

### 第一阶段：核心飞行验证 (Core Flight) ✅
- [x] 6DoF 飞船物理重构
- [x] 飞船实体与登船交互
- [x] VT-1/2/3/4 测试通过
- [x] 碰撞检测与着陆姿态

### 第二阶段：宇宙构建 (Universe Construction) ✅
- [x] 太空维度创建
- [x] 自定义维度视觉效果
- [x] 地球 + 月球渲染系统
- [x] 逼近过渡渲染管线

### 第三阶段：航行闭环 (Travel Loop) ✅
- [x] 地球↔太空维度切换
- [x] HUD 导航系统
- [x] 虚空伤害修复

### 第四阶段：待规划 (v0.1.0 Alpha 目标)
- [ ] 更多天体（火星、金星等）
- [ ] 飞船自定义系统
- [ ] 星图与导航计算机
- [ ] 生存模式资源采集与飞船建造
- [ ] 太空站与殖民地初版

---

## 贡献指南 / Contributing

这是一个单人 passion project，目前不主动寻求代码贡献。但如果你：

- 发现了 Bug 或有功能建议，欢迎提交 [Issue](../../issues)
- 想基于本模组进行二次开发，请遵循 [MIT 许可证](LICENSE.txt) 条款

---

## 许可证 / License

本项目采用 [MIT 许可证](LICENSE.txt) 开源。

---

## 致谢 / Credits

- [Minecraft Forge](https://minecraftforge.net/) 团队提供模组加载框架
- [ParchmentMC](https://parchmentmc.org/) 社区提供开源映射数据
- 灵感来源于 [Outer Wilds](https://www.mobiusdigitalgames.com/outer-wilds.html) 的太空探索体验设计

---

*Made with stardust by Ray1101.*
