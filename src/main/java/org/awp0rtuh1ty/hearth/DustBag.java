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
import org.awp0rtuh1ty.hearth.block.DustBagBlock;
import org.awp0rtuh1ty.hearth.block.DustBagBlockEntity;
import org.awp0rtuh1ty.hearth.screen.DustBagScreenHandler;

public class DustBag {
    public static final Block DUST_BAG_BLOCK = Registry.register(
            BuiltInRegistries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(Hearth.MOD_ID, "dust_bag"),
            new DustBagBlock(Block.Properties.of().strength(2.0F).sound(SoundType.WOOL).noOcclusion())
    );

    public static final BlockEntityType<DustBagBlockEntity> DUST_BAG_BLOCK_ENTITY = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(Hearth.MOD_ID, "dust_bag"),
            BlockEntityType.Builder.of(DustBagBlockEntity::new, DUST_BAG_BLOCK).build(null)
    );

    public static final MenuType<DustBagScreenHandler> DUST_BAG_SCREEN_HANDLER = Registry.register(
            BuiltInRegistries.MENU,
            ResourceLocation.fromNamespaceAndPath(Hearth.MOD_ID, "dust_bag"),
            new MenuType<>(DustBagScreenHandler::new, FeatureFlags.DEFAULT_FLAGS)
    );

    public static final Item DUST_BAG_ITEM = Hearth.register("dust_bag", new BlockItem(DUST_BAG_BLOCK, new Item.Properties()));

    public static void initialize() {
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(content -> {
            content.accept(DUST_BAG_ITEM);
        });
    }
}
