# Hearth · 炉

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen)](https://www.minecraft.net/)
[![Fabric](https://img.shields.io/badge/Fabric-0.19.2+-blue)](https://fabricmc.net/)
[![License](https://img.shields.io/badge/License-CC0--1.0-lightgrey)](LICENSE)

> 山上层层桃李花，云间烟火是人家。
> 银钏金钗来负水，长刀短笠去烧畲。
> <div align="right">——刘禹锡《竹枝词》</div>

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
| **Dispenser + Wood Ash** | Dispensers can use Wood Ash to fertilize crops (vanilla `performBonemeal` logic). Does NOT do gold panning. |
| **Dispenser + Slag** | Dispensers can use Slag on Water Cauldrons to extract nuggets (1-9). Items eject in dispenser facing direction; water level NOT consumed. |
| **Dust Bag Piston State** | Default: piston destroys, drops with NBT. **Honeycomb** → waxed, piston blocked, player can still break. **Cleansing Potion** → restores default. *Player-only.* |
| **Dust Bag Stacking** | Empty bags stack to 64. Bags with any items inside max stack 1. |
| **Sign Cleansing** | Cleansing Potion on sign removes waxed + glowing text states. *Player-only, splash/lingering not implemented.* |

### Installation

1. Install **Fabric Loader** and **Fabric API** for Minecraft 1.21.1
2. Download `hearth-1.0.0.jar`
3. Place it in your `mods/` folder

### Build from Source

```bash
./gradlew build
```

Outputs:
- `build/libs/hearth-1.0.0.jar` — Main mod

### Project Structure

```
Hearth/
├── src/main/           # Main mod source
├── settings.gradle.kts
└── build.gradle.kts
```

---

## 中文

Hearth（炉）是一个 Fabric 1.21.1 模组，为原版熔炉添加副产物系统与荡涤药水。烧炼物品时，熔炉会额外产出**草木灰**或**炉渣**，可用于农耕、炼金等。

### 功能

#### 主模组 (`hearth`)

| 功能 | 说明 |
|:------:|:------:|
| **草木灰** | 熔炉产出 ×2，烟熏炉产出 ×5。<br>可作骨粉催熟作物，也可将水炼药锅转为荡涤炼药锅。 |
| **炉渣** | 高炉产出 ×2。<br>手持右键水炼药锅可淘出 1~9 个随机粒<br>（铁粒、金粒等）。 |
| **集尘袋** | 存储容器，自动吸取相邻熔炉的草木灰/炉渣。<br>27 格空间，支持漏斗交互。<br>空袋可堆叠至 64；<br>含任意物品最大堆叠 1。 |
| **荡涤药水** | 水瓶 + 草木灰酿造。<br>清除负面效果、减半所有伤害、驱散怪物远离玩家。 |
| **荡涤炼药锅** | 右键染色物品还原无色/白色；<br>腐肉换皮革。 |
| **填药炼药锅** | 将任意药水存入炼药锅，支持装瓶复用。 |
| **铲取灰烬** | 手持铲子右键熔炉，取出积累的草木灰/炉渣。 |
| **发射器 + 草木灰** | 发射器可自动使用草木灰催熟作物<br>（原版骨粉逻辑）。<br>不可淘金。 |
| **发射器 + 炉渣** | 发射器可用炉渣对水炼药锅自动淘金（1~9 随机粒）。<br>产物沿发射器朝向射出。 |
| **集尘袋活塞状态** | 默认：活塞可推破坏，掉落保留 NBT。<br>**蜜脾**右键可涂蜡，活塞不可推，玩家仍可破坏。<br>**荡涤药水**右键可恢复默认。<br>*仅玩家可触发。* |
| **告示牌清洗** | 荡涤药水右键告示牌，洗去上蜡状态和荧光效果。<br>*仅玩家可触发。* |

### 安装

1. 安装 Fabric Loader 和 Fabric API（Minecraft 1.21.1）
2. 下载 `hearth-1.0.0.jar`
3. 放入 `mods/` 文件夹

### 构建

```bash
./gradlew build
```

构建产物：
- `build/libs/hearth-1.0.0.jar` — 主模组

### 项目结构

```
Hearth/
├── src/main/           # 主模组源码
├── settings.gradle.kts
└── build.gradle.kts
```

---

## License

CC0-1.0 — Feel free to learn from it and incorporate it in your own projects.
