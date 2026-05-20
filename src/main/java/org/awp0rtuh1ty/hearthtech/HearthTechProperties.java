package org.awp0rtuh1ty.hearthtech;

import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Shared block state properties for Hearth Tech.
 * WAXED: false=default (piston destroys, drops NBT)  true=waxed (piston blocked, player can still break)
 */
public final class HearthTechProperties {
    public static final BooleanProperty WAXED = BooleanProperty.create("waxed");

    private HearthTechProperties() {
    }
}
