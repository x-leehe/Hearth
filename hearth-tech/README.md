# Hearth Tech（炉·生电扩展）

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen)](https://www.minecraft.net/)
[![Fabric](https://img.shields.io/badge/Fabric-0.19.2+-blue)](https://fabricmc.net/)

> 依赖模组：[Hearth（草木灰）](https://github.com/x-leehe/Hearth)

Hearth Tech 是 **Hearth（草木灰）模组**的生电扩展，为原模组的草木灰、炉渣、集尘袋等物品方块添加自动化/发射器/活塞兼容功能。

---

## 功能

### 1. 发射器兼容

#### 发射器使用草木灰 → 催熟作物 / 水炼药锅淘金

将草木灰放入发射器：
- **前方为可催熟作物** → 直接催熟至成熟（遵循原版骨粉行为）
- **前方为水炼药锅** → 消耗草木灰，降低水位，掉落 1~9 个随机粒
- **其他方块** → 作为物品射出

#### 发射器使用炉渣 → 水炼药锅淘金

将炉渣放入发射器：
- **前方为水炼药锅** → 消耗炉渣，降低水位，掉落 1~9 个随机粒（铁粒、金粒等）
- **其他方块** → 作为物品射出

### 2. 集尘袋活塞/堆叠机制

> **仅玩家右键有效**，发射器/投掷器无法触发。

| 状态 | 活塞行为 | 物品堆叠 |
|------|----------|----------|
| 默认（放置后） | **可推动破坏**（类似潜影盒） | 含物品时最大堆叠 **1**，空袋可堆叠 **64** |
| 蜜脾上蜡 | **不可推动** | 不变 |
| 荡涤药水清洗 | **恢复默认**（可推破坏） | 不变 |

- **蜜脾右键** → 上蜡，播放上蜡音效（参考告示牌上蜡）
- **荡涤药水右键** → 清洗恢复默认，消耗药水返还空玻璃瓶

### 3. 告示牌清洗

> **仅玩家右键有效**，仅普通荡涤药水，**不实现**喷溅/滞留型。

手持荡涤药水右键告示牌，同时清除：
- **上蜡状态**（蜂蜜涂蜡）
- **荧光效果**（荧光墨囊涂色）

消耗药水，返还空玻璃瓶。

---

## 安装

### 前置要求

- Minecraft **1.21.1**
- Fabric Loader **>= 0.19.2**
- Fabric API
- **Hearth（草木灰）模组 >= 1.0.0**

### 构建

```bash
./gradlew :build -x test           # 先构建主模组
./gradlew :hearth-tech:build -x test  # 构建生电扩展
```

构建产物位于 `hearth-tech/build/libs/hearth-tech-1.0.0.jar`。

---

## 技术说明

### 项目结构

```
hearth-tech/
├── build.gradle.kts
└── src/main/
    ├── java/.../hearthtech/
    │   ├── HearthTech.java              # 主入口（发射器行为注册）
    │   ├── HearthTechProperties.java    # 共享方块属性（WAXED）
    │   └── mixin/
    │       ├── BlockStateBaseMixin.java  # 拦截活塞推动判定
    │       ├── DustBagBlockMixin.java    # 集尘袋上蜡/清洗交互
    │       ├── ItemStackMixin.java       # 集尘袋堆叠限制
    │       └── SignBlockMixin.java       # 告示牌清洗
    └── resources/
        ├── fabric.mod.json
        ├── hearthtech.mixins.json
        └── assets/hearthtech/lang/
```

### 核心机制

- **发射器**：原版 `DispenserBlock.registerBehavior()` API，草木灰优先催熟、其次淘金
- **活塞**：Mixin `BlockStateBase.getPistonPushReaction()`，集尘袋默认 `DESTROY`，上蜡后 `BLOCK`
- **堆叠**：Mixin `ItemStack.getMaxStackSize()`，有 `BlockEntityData` 时限制为 1
- **告示牌**：Mixin `SignBlock.useItemOn()`，通过 `SignBlockEntity` 清除 waxed/glowing

---

## 许可证

CC0-1.0 — 同 Hearth 主模组。
