package org.awp0rtuh1ty.hearth;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.awp0rtuh1ty.hearth.block.PotionCauldronBlock;
import org.awp0rtuh1ty.hearth.block.PotionCauldronBlockEntity;

public class PotionCauldron {
    public static final Block POTION_CAULDRON_BLOCK = Registry.register(
            BuiltInRegistries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(Hearth.MOD_ID, "potion_cauldron"),
            new PotionCauldronBlock(Block.Properties.ofFullCopy(Blocks.CAULDRON)
                    .sound(SoundType.STONE).noOcclusion())
    );

    public static final BlockEntityType<PotionCauldronBlockEntity> POTION_CAULDRON_BLOCK_ENTITY =
            Registry.register(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    ResourceLocation.fromNamespaceAndPath(Hearth.MOD_ID, "potion_cauldron"),
                    BlockEntityType.Builder.of(
                            PotionCauldronBlockEntity::new, POTION_CAULDRON_BLOCK).build(null)
            );

    public static void initialize() {
        // class loaded to ensure static fields are initialized
    }
}
