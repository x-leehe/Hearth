package org.awp0rtuh1ty.hearth;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import org.awp0rtuh1ty.hearth.item.WoodAshItem;

public class WoodAsh {
    public static final Item WOOD_ASH = Hearth.register("wood_ash", new WoodAshItem(new Item.Properties()));

    public static void initialize() {
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS).register(content -> {
            content.accept(WOOD_ASH);
        });
        // class loaded to ensure static fields are initialized
    }
}
