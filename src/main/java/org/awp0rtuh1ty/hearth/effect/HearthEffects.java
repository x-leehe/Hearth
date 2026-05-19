package org.awp0rtuh1ty.hearth.effect;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import org.awp0rtuh1ty.hearth.Hearth;

public class HearthEffects {
    public static final Holder<MobEffect> CLEANSING = Registry.registerForHolder(
            BuiltInRegistries.MOB_EFFECT,
            ResourceLocation.fromNamespaceAndPath(Hearth.MOD_ID, "cleansing"),
            new CleansingEffect()
    );

    public static void initialize() {
        // class loaded to ensure static fields are initialized
    }
}
