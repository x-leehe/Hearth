package org.awp0rtuh1ty.hearthtech;

import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Hearth Tech shared block state properties.
 * <p>
 * Dust Bag piston_state:
 * 0 = DEFAULT (piston destroys, drops with NBT, like shulker box)
 * 1 = MOVABLE_NO_DROP (piston moves, no drop, keeps contents)
 * 2 = BLOCK (piston cannot push)
 */
public final class HearthTechProperties {
    /** 集尘袋活塞状态: 0=默认 1=蜜脾可推不掉 2=草木灰不可推 */
    public static final IntegerProperty PISTON_STATE = IntegerProperty.create("piston_state", 0, 2);

    private HearthTechProperties() {
    }
}
