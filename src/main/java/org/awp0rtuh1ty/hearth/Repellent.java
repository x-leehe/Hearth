package org.awp0rtuh1ty.hearth;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.awp0rtuh1ty.hearth.screen.RepellentScreenHandler;

public class Repellent {
    public static final Block REPELLENT_BLOCK = Registry.register(
            BuiltInRegistries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(Hearth.MOD_ID, "repellent"),
            new RepellentBlock(Block.Properties.of().strength(2.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE))
    );

    public static final BlockEntityType<RepellentBlockEntity> REPELLENT_BLOCK_ENTITY = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(Hearth.MOD_ID, "repellent"),
            BlockEntityType.Builder.of(RepellentBlockEntity::new, REPELLENT_BLOCK).build(null)
    );

    public static final MenuType<RepellentScreenHandler> REPELLENT_SCREEN_HANDLER = Registry.register(
            BuiltInRegistries.MENU,
            ResourceLocation.fromNamespaceAndPath(Hearth.MOD_ID, "repellent"),
            new MenuType<>(RepellentScreenHandler::new, FeatureFlags.DEFAULT_FLAGS)
    );

    public static final Item REPELLENT_ITEM = Hearth.register("repellent",
            new BlockItem(REPELLENT_BLOCK, new Item.Properties()));

    public static void initialize() {
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(content -> {
            content.accept(REPELLENT_ITEM);
        });
    }
}
