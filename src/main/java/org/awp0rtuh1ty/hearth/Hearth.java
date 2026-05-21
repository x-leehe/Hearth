package org.awp0rtuh1ty.hearth;

import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.resources.ResourceLocation;

import org.awp0rtuh1ty.hearth.effect.HearthEffects;
import org.awp0rtuh1ty.hearth.potion.CleansingPotions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Hearth implements ModInitializer {
    public static final String MOD_ID = "hearth";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public Hearth() {}

    public static <T extends Item> T register(String path, T item) {
        return Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MOD_ID, path), item);
    }

    public static void initialize() {
    }

    @Override
    public void onInitialize() {
        HearthLogConfig.initialize();
        LOGGER.info("Mod {} has been initialized.", MOD_ID);

        WoodAsh.initialize();
        Slag.initialize();
        DustBag.initialize();
        HearthSounds.initialize();
        HearthEffects.initialize();
        CleansingPotions.initialize();
        CleansingCauldron.initialize();
        PotionCauldron.initialize();
        Chimney.initialize();
        Repellent.initialize();
        HearthTech.initialize();
        HearthCommand.initialize();
    }
}