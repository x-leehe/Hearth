# TODO — 荡涤药水 (Cleansing Potion)

!!!注意!!!
以下内容由AI智能体生成，仅作参考！！！

> 项目：Hearth (Fabric 1.21.1)

---

## 目录

- [1. 荡涤状态效果 (CleansingEffect)](#1-荡涤状态效果-cleansingeffect)
- [2. 荡涤药水 (CleansingPotions)](#2-荡涤药水-cleansingpotions)
- [3. 酿造台酿造 (A.a)](#3-酿造台酿造-aa)
- [4. 炼药锅炼药 (A.b)](#4-炼药锅炼药-ab)
- [5. 饮用后行为 (A.1 / A.2 / A.3)](#5-饮用后行为-a1--a2--a3)
- [6. 喷溅 / 滞留药水行为 (B)](#6-喷溅--滞留药水行为-b)
- [7. 荡涤药水炼药锅 (C)](#7-荡涤药水炼药锅-c)
- [8. 文件清单 & 新建 / 修改一览](#8-文件清单--新建--修改一览)

---

## 1. 荡涤状态效果 (CleansingEffect)

### 新建文件

```
src/main/java/org/awp0rtuh1ty/hearth/effect/CleansingEffect.java
```

### 实现要点

```java
// 继承 StatusEffect，构造中设 category=StatusEffectCategory.BENEFICIAL, color=0xXXXXXX
// 
// 核心逻辑在 applyEffectTick(LivingEntity, int amplifier)：
//   - 每 tick 不做持续伤害（饮用时的 -2HP 在药水物品里处理，见 §5）
//
// 需要覆写 onEffectStarted(LivingEntity, int amplifier)：
//   - 立即清除所有负面效果（遍历 entity.getActiveEffects()，移除 Harmful 类别）
//   - entity.hurt(entity.damageSources().magic(), 2.0F)
//
// shouldApplyEffectTickThisTick 返回 false（不做持续 tick）
//
// 减伤逻辑不在此类中，通过 §5 的 Mixin 实现
```

### 注册

在 `CleansingEffect` 同级（或新建 `HearthEffects.java`）：

```java
public static final StatusEffect CLEANSING = Registry.register(
    BuiltInRegistries.MOB_EFFECT,
    ResourceLocation.fromNamespaceAndPath(Hearth.MOD_ID, "cleansing"),
    new CleansingEffect()
);
```

在 `Hearth.onInitialize()` 中调用 `HearthEffects.initialize()`。

> 注：在 1.21 Mojang mappings 中类名是 `StatusEffect`，注册表是 `BuiltInRegistries.MOB_EFFECT`。

---

## 2. 荡涤药水 (CleansingPotions)

### 新建文件

```
src/main/java/org/awp0rtuh1ty/hearth/potion/CleansingPotions.java
```

### 注册药水类型

需要注册 **4 种**药水变体（使用 `Potion` 类）：

| 内部名 | 中文名 | 时长 | 倍率 |
|--------|--------|------|------|
| `cleansing` | 荡涤药水 | 3:00 | I |
| `long_cleansing` | 荡涤药水（延长版） | 8:00 | I |
| `strong_cleansing` | 荡涤药水 II | 1:30 | II |

```java
public static final Potion CLEANSING = Registry.register(
    BuiltInRegistries.POTION,
    ResourceLocation.fromNamespaceAndPath(Hearth.MOD_ID, "cleansing"),
    new Potion(new MobEffectInstance(HearthEffects.CLEANSING, 3600))
);
// long: 9600 ticks, strong: 1800 ticks + amplifier=1
```

### 药水物品

Minecraft 1.21 中，药水物品（`PotionItem`、`SplashPotionItem`、`LingeringPotionItem`）通过 `PotionContentsComponent` 数据组件区分药水类型，不再需要为每种药水注册单独的 Item。

**不需要注册新的 Item** — 直接复用原版 `minecraft:potion` / `minecraft:splash_potion` / `minecraft:lingering_potion`，通过配方 / 酿造系统产出带有 `potion_contents: "hearth:cleansing"` 组件的物品。

---

## 3. 酿造台酿造 (A.a)

### 方法

使用 `PotionBrewing.addMix()` 注册酿造配方（在 `CleansingPotions.initialize()` 中）：

```java
// 水瓶 + 草木灰 → 初级荡涤药水
PotionBrewing.addMix(
    net.minecraft.world.item.alchemy.Potions.WATER,  // 输入药水
    WoodAsh.WOOD_ASH,                                 // 材料
    CleansingPotions.CLEANSING                        // 输出药水
);

// 初级荡涤药水 + 红石 → 延长版
PotionBrewing.addMix(CleansingPotions.CLEANSING, Items.REDSTONE, CleansingPotions.LONG_CLEANSING);

// 初级荡涤药水 + 荧石粉 → II级
PotionBrewing.addMix(CleansingPotions.CLEANSING, Items.GLOWSTONE_DUST, CleansingPotions.STRONG_CLEANSING);

// → 喷溅 / 滞留：使用火药 / 龙息，Minecraft 自动处理
```

> 注意：`PotionBrewing.addMix` 的调用时机必须在 `Hearth.onInitialize()` 中（注册后、服务器启动前）。

---

## 4. 炼药锅炼药 (A.b)

### 方法

修改 `WoodAshItem.java`，在 `useOn` 中增加逻辑：

当玩家对 **水炼药锅** 使用草木灰时：

```java
// 在 WoodAshItem.useOn() 中，BoneMeal 逻辑之后增加：
if (state.getBlock() instanceof LayeredCauldronBlock && state.is(Blocks.WATER_CAULDRON)) {
    // → 消耗 1 草木灰
    // → 将炼药锅替换为 §7 定义的荡涤药水炼药锅方块
    // → 返回 SUCCESS
}
```

涉及新建文件见 §7。

---

## 5. 饮用后行为 (A.1 / A.2 / A.3)

### A.1 — 饮用后 -2HP + 获得荡涤效果

`PotionItem` / `SplashPotionItem` 在饮用 / 投掷后通过 `PotionContentsComponent.getAllEffects()` 获取效果列表并逐一应用。

**方案**：在 `CleansingEffect.onEffectStarted()` 中直接扣血 + 清除负面效果（§1 已覆盖）。不需要额外 Hook 药水饮用事件。

### A.2 — 立即清除所有负面效果

```java
// CleansingEffect.onEffectStarted():
Iterator<MobEffectInstance> it = entity.getActiveEffects().iterator();
while (it.hasNext()) {
    MobEffectInstance instance = it.next();
    if (instance.getEffect().value().getCategory() == StatusEffectCategory.HARMFUL) {
        entity.removeEffect(instance.getEffect());
    }
}
```

### A.3 — 减半所有来源的伤害

需要 **Mixin** 到 `LivingEntity`：

#### 新建文件

```
src/main/java/org/awp0rtuh1ty/hearth/mixin/LivingEntityMixin.java
```

```java
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @ModifyVariable(
        method = "actuallyHurt",
        at = @At("HEAD"),
        argsOnly = true
    )
    private float hearth$halveDamageWithCleansing(float damage) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.hasEffect(HearthEffects.CLEANSING)) {
            return damage * 0.5F;
        }
        return damage;
    }
}
```

> `actuallyHurt` 是 1.21 Mojang 中实际处理伤害的方法（替代旧版的 `damage` / `applyDamage`）。

---

## 6. 喷溅 / 滞留药水行为 (B)

### B.1 — 怪物走入药水云获得荡涤效果 + 持续扣血 + 远离玩家

**方案**：Mixin `AreaEffectCloud`（滞留型）和 `Potion`（喷溅型碰撞逻辑）。

但实际上，**原版喷溅药水碰撞已会将药水效果施加给实体**。只需要确保：
1. `CleansingEffect` 已正确注册
2. 喷溅型荡涤药水使用与普通药水相同的 `PotionContentsComponent`

这样怪物碰到喷溅粒子就会自动获得荡涤效果。

> **"不断扣除少量血量" + "试图缓慢远离玩家"** 可以在 `CleansingEffect.applyEffectTick` 中实现：
> ```java
> // applyEffectTick: 每 20 tick 扣 1 HP
> entity.hurt(entity.damageSources().magic(), 1.0F);
> // 远离最近玩家：用 Goal / AI 或直接在 tick 中推离
> ```

替代方案：用 `MobEffectInstance` 的 `isVisible` + 在 `CleansingEffect` 中覆写 `applyEffectTick` 实现周期性扣血；远离行为可考虑 Mixin 到 `Mob.aiStep` 或简单地在 effect tick 中对怪物施加一个向远离玩家方向的速度 `entity.push(dx, 0, dz)`。

### B.2 — 还原染色物体

喷溅药水命中时，检查药水云（`AreaEffectCloud`）或溅射点周围方块：

#### 新建文件

```
src/main/java/org/awp0rtuh1ty/hearth/mixin/PotionEntityMixin.java
```

```java
@Mixin(ThrownPotion.class)  // 1.21 Mojang: ThrownPotion
public abstract class ThrownPotionMixin {

    @Inject(
        method = "applySplash",
        at = @At("TAIL")
    )
    private void hearth$cleanDyedBlocks(/* ... */) {
        // 1. 判断药水是否为荡涤药水（读取 potion_contents 组件）
        // 2. 获取碰撞点周围 3×3×3 的方块
        // 3. 遍历：
        //    - 如果方块 instanceof StainedGlassBlock → 替换为 Blocks.GLASS
        //    - 如果方块 instanceof StainedGlassPaneBlock → 替换为 Blocks.GLASS_PANE
        //    - 如果方块 instanceof WoolCarpetBlock → 替换为白色地毯
        //    - 如果方块 instanceof BedBlock / ShulkerBoxBlock / TerracottaBlock
        //      → 替换为对应无色 / 白色变体
        //    - 如果 state.is(BlockTags.WOOL) → 替换为 Blocks.WHITE_WOOL
    }
}
```

**染色方块 → 无色 / 白色映射表**（可在代码中硬编码或通过 Block 属性判断）：

| 染色方块 | → 还原为 |
|----------|----------|
| `*_wool` | `white_wool` |
| `*_stained_glass` | `glass` |
| `*_stained_glass_pane` | `glass_pane` |
| `*_carpet` | `white_carpet` |
| `*_terracotta` | `terracotta` |
| `*_glazed_terracotta` | `white_glazed_terracotta` |
| `*_concrete` | `white_concrete` |
| `*_concrete_powder` | `white_concrete_powder` |
| `*_bed` | `white_bed` |
| `*_candle` | `candle`（无色版） |
| `*_banner` | `white_banner` |
| `*_shulker_box` | `shulker_box`（紫色原版） |

**对滞留型药水**：同样需要 Mixin `AreaEffectCloud`，在每个 tick 中检查云内方块并还原。

---

## 7. 荡涤药水炼药锅 (C)

### 新建文件

```
src/main/java/org/awp0rtuh1ty/hearth/block/CleansingCauldronBlock.java
```

### 方块定义

```java
// 继承 LayeredCauldronBlock
// 与 WATER_CAULDRON 共用相同的 LEVEL 属性（1~3 层）
//
// 交互逻辑覆盖：
//   1. emptyBucket / fillBucket → 不做水桶交互（此锅装的是药水，不是水）
//   2. useItemOn 中：
//      a) 玩家手持玻璃瓶 → 装取荡涤药水（产出 potion item with potion_contents=hearth:cleansing）
//         消耗 1 层，类似从水炼药锅装水
//      b) 玩家手持荡涤药水 → 填充炼药锅（增加 1 层）
//      c) 玩家手持腐肉 → 消耗 1 层，产出皮革（C.2）
//      d) 玩家手持染色物品 → 消耗 1 层，产出无色 / 白色版本（C.1）
```

### C.1 — 染色物品还原

逻辑与 §6.B.2 相同，但操作对象是**玩家手持物品**而非世界方块：

```java
// 映射表（同 §6.B.2 表格）
// 例如：玩家手持品红色羊毛 → 消耗 1 层药水 → 返回白色羊毛
```

### C.2 — 腐肉→皮革

```java
if (heldItem.is(Items.ROTTEN_FLESH)) {
    heldItem.shrink(1);
    // 给玩家 1 个皮革
    player.getInventory().add(new ItemStack(Items.LEATHER));
    // 降低炼药锅水位
    LayeredCauldronBlock.lowerFillLevel(state, level, pos);
}
```

### 注册

参考 `DustBag` 的模式：

```java
public static final Block CLEANSING_CAULDRON = Registry.register(
    BuiltInRegistries.BLOCK,
    ResourceLocation.fromNamespaceAndPath(Hearth.MOD_ID, "cleansing_cauldron"),
    new CleansingCauldronBlock(Block.Properties.ofFullCopy(Blocks.CAULDRON))
);
```

> 注意：无需注册 `BlockItem`（玩家不会持有此方块物品），炼药锅通过交互动态创建。

### 炼药锅获得方式

1. **从水炼药锅 + 草木灰**（§4 A.b）
2. **玩家手持荡涤药水右键空炼药锅** → 填充 1 层荡涤药水
3. **玩家手持荡涤药水右键荡涤药水炼药锅** → 增加 1 层（最多 3 层）

---

## 8. 文件清单 & 新建 / 修改一览

### 新建文件

| 文件 | 用途 |
|------|------|
| `src/main/java/.../effect/CleansingEffect.java` | 荡涤状态效果 |
| `src/main/java/.../effect/HearthEffects.java` | 效果注册 |
| `src/main/java/.../potion/CleansingPotions.java` | 药水 + 酿造配方注册 |
| `src/main/java/.../block/CleansingCauldronBlock.java` | 荡涤药水炼药锅 |
| `src/main/java/.../mixin/LivingEntityMixin.java` | 减伤（§5.A.3） |
| `src/main/java/.../mixin/ThrownPotionMixin.java` | 喷溅药水方块脱色（§6.B.2） |
| `src/main/java/.../mixin/AreaEffectCloudMixin.java` | 滞留药水脱色 + 怪物远离（§6.B） |
| `src/main/resources/assets/hearth/models/item/cleansing_potion.json` | 药水物品模型（可选，复用原版 potion 模型即可） |
| `src/main/resources/assets/hearth/textures/...` | 药水颜色由 `StatusEffect.color` 控制，不需要额外纹理 |

### 修改文件

| 文件 | 改动 |
|------|------|
| `Hearth.java` | `onInitialize` 中调用 `HearthEffects.initialize()` + `CleansingPotions.initialize()` |
| `WoodAshItem.java` | `useOn` 中新增水炼药锅 → 荡涤药水炼药锅逻辑（§4） |
| `hearth.mixins.json` | 新增 3 个 Mixin 类名（§5 + §6） |
| `assets/hearth/lang/zh_cn.json` | 添加翻译 key（见下） |
| `assets/hearth/lang/en_us.json` | 添加翻译 key（见下） |

### 翻译 key

```json
// zh_cn
{
    "effect.hearth.cleansing": "荡涤",
    "item.minecraft.potion.effect.cleansing": "荡涤药水",
    "item.minecraft.splash_potion.effect.cleansing": "喷溅型荡涤药水",
    "item.minecraft.lingering_potion.effect.cleansing": "滞留型荡涤药水",
    "item.minecraft.potion.effect.long_cleansing": "荡涤药水（延长版）",
    "item.minecraft.potion.effect.strong_cleansing": "荡涤药水 II",
    "block.hearth.cleansing_cauldron": "荡涤药水炼药锅"
}
```

### 实现顺序建议

1. **CleansingEffect** — 纯效果类，不依赖其他模块
2. **HearthEffects** — 注册效果
3. **LivingEntityMixin** — 减伤逻辑（验证效果生效）
4. **CleansingPotions** — 注册药水 + 酿造配方
5. **ThrownPotionMixin + AreaEffectCloudMixin** — 喷溅 / 滞留行为
6. **CleansingCauldronBlock** — 炼药锅相关（最复杂，依赖药水系统就跑通）
7. **WoodAshItem 改动** — 连接炼药锅炼药
8. **语言文件** — 收尾
