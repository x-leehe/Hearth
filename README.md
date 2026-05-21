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
| **Dispenser + Wood Ash** | Dispensers can use Wood Ash to fertilize crops (vanilla `performBonemeal` logic). |
| **Dispenser + Slag** | Dispensers can use Slag on Water Cauldrons to extract nuggets (1-9). Items eject in dispenser facing direction. |
| **Dust Bag Piston State** | Default: piston destroys, drops with NBT. **Honeycomb** or **Wood Ash** → waxed, piston blocked. **Cleansing Potion** → restores default. *Player only.* |
| **Dust Bag Stacking** | Empty bags stack to 64. Bags with items inside max stack 1. |
| **Sign Cleansing** | Cleansing Potion on sign removes waxed + glowing text states. *Player only.* |

### Installation

1. Install **Fabric Loader** and **Fabric API** for Minecraft 1.21.1
2. Download `hearth-1.0.0.jar`
3. Place it in your `mods/` folder

### Build from Source

```bash
./gradlew build -x test
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

### Acknowledgments

Special thanks to:

- **[bentianjia](https://github.com/bentianjia)** — provided foundational technical support in the early stages of development.
- **[Awp0rtuh1ty](https://github.com/Awp0rtuh1ty)** — provided essential conceptual design for the mod.

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

### 安装

1. 安装 Fabric Loader 和 Fabric API（Minecraft 1.21.1）
2. 下载 `hearth-1.0.0.jar`
3. 放入 `mods/` 文件夹

### 构建

```bash
./gradlew build -x test
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

### 致谢

特别感谢：

- **[bentianjia](https://github.com/bentianjia)** — 在开发初期提供了宝贵的技术支持。
- **[Awp0rtuh1ty](https://github.com/Awp0rtuh1ty)** — 为模组提供了重要的概念设计。

---

## License

CC0-1.0 — Feel free to learn from it and incorporate it in your own projects.
