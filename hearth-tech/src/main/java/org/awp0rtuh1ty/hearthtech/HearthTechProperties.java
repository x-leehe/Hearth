package org.awp0rtuh1ty.hearthtech;

import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Hearth Tech 扩展模组共享属性
 */
public class HearthTechProperties {
    /** 集尘袋上蜡状态: false=默认(可推掉落) true=上蜡(不可推) */
    public static final BooleanProperty WAXED = BooleanProperty.create("waxed");
}
