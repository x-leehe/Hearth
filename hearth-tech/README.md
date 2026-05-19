# Hearth Tech (炉 生电扩展)

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen)](https://www.minecraft.net/)
[![Fabric](https://img.shields.io/badge/Fabric-0.19.2+-blue)](https://fabricmc.net/)

> 依赖模组: [Hearth (草木灰)](https://github.com/x-leehe/Hearth)

Hearth Tech 是 Hearth (草木灰) 模组的生电扩展, 为原模组的草木灰/炉渣/集尘袋等添加自动化/发射器/活塞兼容功能.

---

## 功能

### 1. 发射器兼容

#### 发射器使用草木灰 -> 催熟作物

遵循原版骨粉发射器行为. 将草木灰放入发射器, 对前方可催熟方块执行 `performBonemeal` 原版骨粉逻辑. 不可催熟则作为物品射出.

#### 发射器使用炉渣 -> 水炼药锅淘金

将炉渣放入发射器:
- 前方为水炼药锅 -> 消耗炉渣, 产物 (1~9 个随机粒) 沿发射器朝向射出, 不消耗炼药锅水位
- 其他方块 -> 作为物品射出

### 2. 集尘袋活塞机制

> 仅玩家右键有效, 发射器/投掷器无法触发. 玩家始终可以敲掉集尘袋.

| 手持物品 | 状态 | 活塞行为 |
|----------|------|----------|
| 无 (默认) | 未涂蜡 | 活塞可推破坏, 掉落保留 NBT |
| 蜜脾 | 涂蜡 | 活塞不可推 |
| 草木灰 | 涂蜡 | 活塞不可推 |
| 荡涤药水 | 恢复默认 | 活塞可推破坏, 返还玻璃瓶 |

### 3. 集尘袋堆叠

- 没有任何内部物品的集尘袋: 最大堆叠 64
- 含有任意内部物品的集尘袋: 最大堆叠 1 (不可堆叠)

### 4. 告示牌清洗

> 仅玩家右键有效, 仅普通荡涤药水 (喷溅/滞留型不实现).

手持荡涤药水右键告示牌, 同时清除:
- 上蜡状态 (蜂蜜涂蜡)
- 发光文字状态 (荧光墨囊)

消耗药水, 返还空玻璃瓶.

---

## 安装

- Minecraft 1.21.1
- Fabric Loader >= 0.19.2
- Fabric API
- Hearth (草木灰) 模组 >= 1.0.0

### 构建

```bash
# 先构建主模组
./gradlew :build -x test

# 再构建生电扩展
./gradlew :hearth-tech:build -x test
```

构建产物位于 `hearth-tech/build/libs/hearth-tech-1.0.0.jar`.

将 `build/libs/hearth-1.0.0.jar` 和 `hearth-tech/build/libs/hearth-tech-1.0.0.jar` 放入 mods 文件夹.

---

## 技术说明

### 项目结构

```
hearth-tech/
  build.gradle.kts
  src/main/
    java/.../hearthtech/
      HearthTech.java              主入口 (发射器行为注册)
      HearthTechProperties.java    共享方块属性 (PISTON_STATE)
      mixin/
        BlockStateBaseMixin.java   拦截活塞推动判定
        DustBagBlockMixin.java     集尘袋右键状态切换
        ItemStackMixin.java        集尘袋堆叠限制
        SignBlockMixin.java        告示牌清洗
    resources/
      fabric.mod.json
      hearthtech.mixins.json
      assets/hearthtech/lang/
```

### 核心机制

- 发射器: 原版 `DispenserBlock.registerBehavior()` API; 草木灰走 `performBonemeal` 骨粉逻辑; 炉渣淘金不耗水, 产物沿发射器朝向射出
- 活塞: Mixin `BlockStateBase.getPistonPushReaction()`, 涂蜡 -> BLOCK, 默认 -> DESTROY
- 堆叠: Mixin `ItemStack.getMaxStackSize()`, 检查 BlockEntityData.Items 列表是否实际含物品
- 告示牌: Mixin `SignBlock.useItemOn()`, 通过 SignBlockEntity 清除 waxed/glowing

---

## 许可证

CC0-1.0
