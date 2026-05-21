package org.awp0rtuh1ty.hearth;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;

public class Chimney {
    public static final Block CHIMNEY_BLOCK = Registry.register(
            BuiltInRegistries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(Hearth.MOD_ID, "chimney"),
            new ChimneyBlock(Block.Properties.of().strength(2.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE))
    );

    public static final Item CHIMNEY_ITEM = Hearth.register("chimney", new BlockItem(CHIMNEY_BLOCK, new Item.Properties()));

    public static void initialize() {
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(content -> {
            content.accept(CHIMNEY_ITEM);
        });
    }
}
