package org.awp0rtuh1ty.hearth.potion;

import net.fabricmc.fabric.api.registry.FabricBrewingRecipeRegistryBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.gameevent.GameEvent;
import org.awp0rtuh1ty.hearth.CleansingCauldron;
import org.awp0rtuh1ty.hearth.Hearth;
import org.awp0rtuh1ty.hearth.PotionCauldron;
import org.awp0rtuh1ty.hearth.WoodAsh;
import org.awp0rtuh1ty.hearth.block.PotionCauldronBlock;
import org.awp0rtuh1ty.hearth.block.PotionCauldronBlockEntity;
import org.awp0rtuh1ty.hearth.effect.HearthEffects;

import java.util.Set;

public class CleansingPotions {
    public static final Holder<Potion> CLEANSING = Registry.registerForHolder(
            BuiltInRegistries.POTION,
            ResourceLocation.fromNamespaceAndPath(Hearth.MOD_ID, "cleansing"),
            new Potion(new MobEffectInstance(HearthEffects.CLEANSING, 3600))
    );

    public static final Holder<Potion> LONG_CLEANSING = Registry.registerForHolder(
            BuiltInRegistries.POTION,
            ResourceLocation.fromNamespaceAndPath(Hearth.MOD_ID, "long_cleansing"),
            new Potion(new MobEffectInstance(HearthEffects.CLEANSING, 9600))
    );

    public static final Holder<Potion> STRONG_CLEANSING = Registry.registerForHolder(
            BuiltInRegistries.POTION,
            ResourceLocation.fromNamespaceAndPath(Hearth.MOD_ID, "strong_cleansing"),
            new Potion(new MobEffectInstance(HearthEffects.CLEANSING, 1800, 1))
    );

    public static void initialize() {
        FabricBrewingRecipeRegistryBuilder.BUILD.register(builder -> {
            builder.registerPotionRecipe(Potions.WATER, Ingredient.of(WoodAsh.WOOD_ASH), CLEANSING);
            builder.registerPotionRecipe(CLEANSING, Ingredient.of(Items.REDSTONE), LONG_CLEANSING);
            builder.registerPotionRecipe(CLEANSING, Ingredient.of(Items.GLOWSTONE_DUST), STRONG_CLEANSING);
        });

        // 空炼药锅 + 药水 → 填药炼药锅 / 荡涤炼药锅
        CauldronInteraction fillCauldronFromPotion = (state, level, pos, player, hand, stack) -> {
            PotionContents contents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
            if (contents.equals(PotionContents.EMPTY)) {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }

            boolean isCleansing = contents.potion()
                    .map(h -> CLEANSING_POTION_IDS.contains(
                            h.unwrapKey().map(k -> k.location()).orElse(null)))
                    .orElse(false);

            if (!level.isClientSide) {
                if (isCleansing) {
                    level.setBlock(pos, CleansingCauldron.CLEANSING_CAULDRON.defaultBlockState()
                            .setValue(LayeredCauldronBlock.LEVEL, 1), 3);
                } else {
                    level.setBlock(pos, PotionCauldron.POTION_CAULDRON_BLOCK.defaultBlockState()
                            .setValue(PotionCauldronBlock.LEVEL, 1), 3);
                    BlockEntity be = level.getBlockEntity(pos);
                    if (be instanceof PotionCauldronBlockEntity potionBe) {
                        potionBe.setPotionContents(contents);
                        potionBe.setFillLevel(1);
                    }
                }
                stack.shrink(1);
                ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
                if (stack.isEmpty()) {
                    player.setItemInHand(hand, bottle);
                } else if (!player.getInventory().add(bottle)) {
                    player.drop(bottle, false);
                }
                level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
                level.gameEvent(null, GameEvent.FLUID_PLACE, pos);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        };

        CauldronInteraction.EMPTY.map().put(Items.POTION, fillCauldronFromPotion);
        CauldronInteraction.EMPTY.map().put(Items.SPLASH_POTION, fillCauldronFromPotion);
        CauldronInteraction.EMPTY.map().put(Items.LINGERING_POTION, fillCauldronFromPotion);
    }

    private static final Set<ResourceLocation> CLEANSING_POTION_IDS = Set.of(
            ResourceLocation.fromNamespaceAndPath(Hearth.MOD_ID, "cleansing"),
            ResourceLocation.fromNamespaceAndPath(Hearth.MOD_ID, "long_cleansing"),
            ResourceLocation.fromNamespaceAndPath(Hearth.MOD_ID, "strong_cleansing")
    );
}
