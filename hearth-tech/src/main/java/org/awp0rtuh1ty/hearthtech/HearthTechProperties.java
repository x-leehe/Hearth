package org.awp0rtuh1ty.hearthtech;

import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Hearth Tech 扩展模组共享属性
 */
public class HearthTechProperties {
    /** 集尘袋活塞状态: 0=默认 1=蜜蜡 2=水瓶 3=草木灰 */
    public static final IntegerProperty PISTON_STATE = IntegerProperty.create("piston_state", 0, 3);
}
