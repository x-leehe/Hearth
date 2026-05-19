# Hearth Tech（炉·生电扩展）

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen)](https://www.minecraft.net/)
[![Fabric](https://img.shields.io/badge/Fabric-0.19.2+-blue)](https://fabricmc.net/)

> 依赖模组：[Hearth（草木灰）](https://github.com/x-leehe/Hearth)

Hearth Tech 是 **Hearth（草木灰）模组**的生电扩展，为原模组的草木灰、炉渣、集尘袋等物品方块添加自动化/发射器/活塞兼容功能，使其更好地融入生电（技术生存）玩法。

---

## 功能

### 1. 发射器兼容

#### 发射器使用草木灰 → 催熟作物

遵循**原版骨粉**的发射器行为。将草木灰放入发射器，发射器会对前方可催熟的作物（小麦、胡萝卜等）直接催熟至成熟。

- 不可催熟的方块 → 草木灰作为物品射出（无浪费）
- 音效：骨粉使用音效

#### 发射器使用炉渣 → 水炼药锅淘金

遵循**原版水瓶**的发射器行为。将炉渣放入发射器，发射器会对前方水炼药锅消耗炉渣并产出 1~9 个随机粒（铁粒、金粒等），产物直接掉落在世界中。

- 非水炼药锅 → 炉渣作为物品射出
- 空炼药锅 → 炉渣作为物品射出

### 2. 集尘袋活塞状态修改

> **仅玩家右键有效**，发射器/投掷器无法触发。

集尘袋新增 `piston_state` 属性，玩家可通过手持特定物品右键集尘袋来改变其活塞行为：

| 手持物品 | 状态 | 活塞行为 | 说明 |
|----------|------|----------|------|
| 无（默认） | `0` | **不可推动** | 默认状态，活塞无法推动 |
| 蜜蜡 | `1` | **可推动，不掉落** | 上蜡保护，数据保留（参考告示牌上蜡） |
| 水瓶 | `2` | **可推动且掉落** | 推动时掉落为物品（参考原版潜影盒） |
| 草木灰 | `3` | **不可推动** | 加固状态 |

- **蜜蜡上蜡**：消耗1个蜜蜡，播放上蜡音效
- **水瓶浇淋**：消耗1个水瓶，返还空玻璃瓶
- **草木灰加固**：消耗1个草木灰

---

## 安装

### 前置要求

- Minecraft **1.21.1**
- Fabric Loader **>= 0.19.2**
- Fabric API
- **Hearth（草木灰）模组 >= 1.0.0**

### 构建

```bash
# 1. 先构建主模组 Hearth
./gradlew :build -x test

# 2. 构建生电扩展 Hearth Tech
./gradlew :hearth-tech:build -x test
```

构建产物位于 `hearth-tech/build/libs/hearthtech-1.0.0.jar`。

---

## 技术说明

### 项目结构

```
hearth-tech/                    # 生电扩展子项目
├── build.gradle.kts            # Gradle 构建配置
└── src/main/
    ├── java/.../hearthtech/
    │   ├── HearthTech.java             # 主入口（注册发射器行为）
    │   ├── HearthTechProperties.java   # 共享方块属性常量
    │   └── mixin/
    │       ├── BlockStateBaseMixin.java    # 拦截活塞推动判定
    │       └── DustBagBlockMixin.java      # 集尘袋右键交互 + 属性注册
    └── resources/
        ├── fabric.mod.json
        ├── hearthtech.mixins.json
        └── assets/hearthtech/lang/
            ├── zh_cn.json
            └── en_us.json
```

### 核心机制

#### 发射器行为

通过原版 `DispenserBlock.registerBehavior()` API 注册。发射器逻辑会先检查目标方块是否符合条件，符合则消耗物品并触发效果，不符合则走默认物品射出逻辑。

#### 集尘袋活塞状态

- **方块状态属性**：通过 Mixin 向 `DustBagBlock` 的 `StateDefinition` 注入 `piston_state` 属性（0-3），状态随方块存储，无需 NBT。
- **活塞推动拦截**：通过 Mixin `BlockStateBase.getPistonPushReaction()` 方法，对 `DustBagBlock` 实例根据 `piston_state` 返回对应的 `PushReaction`。
- **右键交互**：通过 Mixin 注入 `DustBagBlock.useItemOn()` 头部，检查手持物品并优先处理状态修改（蜜蜡/水瓶/草木灰），其他物品走原逻辑（打开 GUI）。

---

## 许可证

CC0-1.0 — 同 Hearth 主模组。
