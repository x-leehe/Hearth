package org.awp0rtuh1ty.hearth;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.awp0rtuh1ty.hearth.block.CleansingCauldronBlock;

public class CleansingCauldron {
    public static final Block CLEANSING_CAULDRON = Registry.register(
            BuiltInRegistries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(Hearth.MOD_ID, "cleansing_cauldron"),
            new CleansingCauldronBlock(Block.Properties.ofFullCopy(Blocks.CAULDRON))
    );

    public static void initialize() {
    }
}
