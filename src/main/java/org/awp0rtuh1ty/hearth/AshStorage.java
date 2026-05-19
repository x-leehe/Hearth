package org.awp0rtuh1ty.hearth;

import net.minecraft.world.item.ItemStack;

public interface AshStorage {
    ItemStack hearth$getExtraSlot3();

    ItemStack hearth$getExtraSlot4();

    ItemStack hearth$takeExtraSlot3();

    ItemStack hearth$takeExtraSlot4();

    void hearth$shrinkExtraSlot3(int amount);

    void hearth$shrinkExtraSlot4(int amount);
}