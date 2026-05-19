# Hearth · 炉

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen)](https://www.minecraft.net/)
[![Fabric](https://img.shields.io/badge/Fabric-0.19.2+-blue)](https://fabricmc.net/)
[![License](https://img.shields.io/badge/License-CC0--1.0-lightgrey)](LICENSE)

> Where there's smoke, there's a hearth; Where there's a hearth, there's a heart.
>
> 有烟必有炉，有炉必有情。

---

[English](#english) | [中文](#中文)

---

## English

Hearth is a Fabric mod for Minecraft 1.21.1 that adds furnace byproducts and cleansing potions. When you smelt items, furnaces produce **Wood Ash** or **Slag** as byproducts. These can be used for farming, alchemy, and more.

### Features

#### Base Mod (`hearth`)

| Feature | Description |
|---------|-------------|
| **Wood Ash** (草木灰) | Produced by Furnace (×2) and Smoker (×5). Can be used as bonemeal on crops, or to create Cleansing Cauldrons. |
| **Slag** (炉渣) | Produced by Blast Furnace (×2). Use on Water Cauldrons to extract 1~9 random nuggets (iron, gold, etc.). |
| **Dust Bag** (集尘袋) | A storage block that auto-pulls ash/slag from adjacent furnaces. Has 27 slots, hopper-compatible. |
| **Cleansing Potion** (荡涤药水) | Brewed from Water Bottle + Wood Ash. Removes negative effects, halves damage taken, pushes monsters away. |
| **Cleansing Cauldron** | Right-click with dyed items to restore them to their natural color. Convert Rotten Flesh → Leather. |
| **Potion-Filled Cauldron** | Store any potion in a cauldron for later use. |
| **Furnace Ash Extraction** | Right-click a furnace with a shovel to collect accumulated ash/slag. |

#### Tech Extension (`hearthtech`)

| Feature | Description |
|---------|-------------|
| **Dispenser + Wood Ash** | Dispensers can use Wood Ash to fertilize crops. If facing a Water Cauldron, triggers gold panning (1-9 random nuggets). |
| **Dispenser + Slag** | Dispensers can use Slag on Water Cauldrons to automate nugget extraction. |
| **Dust Bag Piston State** | Default: piston-breakable with item drop (like shulker). Right-click with **Honeycomb** → waxed (piston-immune). Right-click with **Cleansing Potion** → restores default. *Player-only.* |
| **Dust Bag Stacking** | Dust Bags with items inside cannot stack (max 1). Empty Dust Bags stack up to 64. |
| **Sign Cleansing** | Right-click a sign with Cleansing Potion to remove waxed and glowing states. *Player-only, splash/lingering not implemented.* |

### Installation

1. Install **Fabric Loader** and **Fabric API** for Minecraft 1.21.1
2. Download `hearth-1.0.0.jar` and `hearth-tech-1.0.0.jar`
3. Place both in your `mods/` folder

### Build from Source

```bash
# Build main mod
./gradlew :build -x test

# Build tech extension (after main mod is built)
./gradlew :hearth-tech:build -x test
```

Outputs:
- `build/libs/hearth-1.0.0.jar` — Main mod
- `hearth-tech/build/libs/hearth-tech-1.0.0.jar` — Tech extension

### Project Structure

```
Hearth/
├── src/main/           # Main mod source
├── hearth-tech/        # Tech extension subproject
│   ├── README.md        # Tech extension docs (Chinese)
│   └── src/main/        # Extension source
├── settings.gradle.kts
└── build.gradle.kts
```

---

## 中文

Hearth（炉）是一个 Fabric 1.21.1 模组，为原版熔炉添加副产物系统与荡涤药水。烧炼物品时，熔炉会额外产出**草木灰**或**炉渣**，可用于农耕、炼金等。

### 功能

#### 主模组 (`hearth`)

| 功能 | 说明 |
|------|------|
| **草木灰** | 熔炉产出 ×2，烟熏炉产出 ×5。可作骨粉催熟作物，也可将水炼药锅转为荡涤炼药锅。 |
| **炉渣** | 高炉产出 ×2。手持右键水炼药锅可淘出 1~9 个随机粒（铁粒、金粒等）。 |
| **集尘袋** | 存储容器，自动吸取相邻熔炉的草木灰/炉渣。27 格空间，支持漏斗交互。 |
| **荡涤药水** | 水瓶 + 草木灰酿造。清除负面效果、减半所有伤害、驱散怪物远离玩家。 |
| **荡涤炼药锅** | 右键染色物品还原无色/白色；腐肉→皮革。 |
| **填药炼药锅** | 将任意药水存入炼药锅，支持装瓶复用。 |
| **铲取灰烬** | 手持铲子右键熔炉，取出积累的草木灰/炉渣。 |

#### 生电扩展 (`hearthtech`)

| 功能 | 说明 |
|------|------|
| **发射器 + 草木灰** | 发射器可自动使用草木灰催熟作物；面对水炼药锅时触发淘金（1~9 随机粒）。 |
| **发射器 + 炉渣** | 发射器可用炉渣对水炼药锅自动淘金。 |
| **集尘袋活塞状态** | 默认：活塞可推破坏（类似潜影盒）。**蜜脾**右键 → 上蜡（活塞不可推）。**荡涤药水**右键 → 恢复默认。*仅玩家可触发。* |
| **集尘袋堆叠** | 含物品的集尘袋不可堆叠（最大1）；空袋可堆叠至 64。 |
| **告示牌清洗** | 荡涤药水右键告示牌，洗去上蜡状态和荧光效果。*仅玩家可触发，不实现喷溅/滞留型。* |

### 安装

1. 安装 Fabric Loader 和 Fabric API（Minecraft 1.21.1）
2. 下载 `hearth-1.0.0.jar` 和 `hearth-tech-1.0.0.jar`
3. 放入 `mods/` 文件夹

### 构建

```bash
# 先构建主模组
./gradlew :build -x test

# 再构建生电扩展
./gradlew :hearth-tech:build -x test
```

构建产物：
- `build/libs/hearth-1.0.0.jar` — 主模组
- `hearth-tech/build/libs/hearth-tech-1.0.0.jar` — 生电扩展

### 项目结构

```
Hearth/
├── src/main/           # 主模组源码
├── hearth-tech/        # 生电扩展子项目
│   ├── README.md        # 扩展模组详细说明
│   └── src/main/        # 扩展源码
├── settings.gradle.kts
└── build.gradle.kts
```

---

## License

CC0-1.0 — Feel free to learn from it and incorporate it in your own projects.
