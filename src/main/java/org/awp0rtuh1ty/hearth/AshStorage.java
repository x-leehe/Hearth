package org.awp0rtuh1ty.hearth;

import net.minecraft.world.item.ItemStack;

public interface AshStorage {
    /** 副产物槽 3 */
    ItemStack hearth$getExtraSlot3();

    /** 副产物槽 4 */
    ItemStack hearth$getExtraSlot4();

    ItemStack hearth$takeExtraSlot3();

    ItemStack hearth$takeExtraSlot4();

    void hearth$shrinkExtraSlot3(int amount);

    void hearth$shrinkExtraSlot4(int amount);

    /** 熔炉输入格 (slot 0) */
    ItemStack hearth$getInputStack();

    /** 熔炉燃料格 (slot 1) */
    ItemStack hearth$getFuelStack();

    /** 熔炉输出格 (slot 2) */
    ItemStack hearth$getOutputStack();

    /** 当前烧制进度 */
    int hearth$getCookingProgress();

    /** 总烧制时间 */
    int hearth$getCookingTotalTime();
}