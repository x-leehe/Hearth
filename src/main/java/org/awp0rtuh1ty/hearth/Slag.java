package org.awp0rtuh1ty.hearth;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import org.awp0rtuh1ty.hearth.item.SlagItem;

public class Slag {
    public static final Item SLAG = Hearth.register("slag", new SlagItem(new Item.Properties()));

    public static void initialize() {
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS).register(content -> {
            content.accept(SLAG);
        });
    }
}
