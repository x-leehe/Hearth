package org.awp0rtuh1ty.hearth;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public class HearthSounds {
    public static final SoundEvent WOOD_ASH_SHOVEL = SoundEvent.createFixedRangeEvent(
            ResourceLocation.fromNamespaceAndPath(Hearth.MOD_ID, "wood_ash_shovel"),
            16.0f
    );

    public static void initialize() {
        Registry.register(BuiltInRegistries.SOUND_EVENT, WOOD_ASH_SHOVEL.getLocation(), WOOD_ASH_SHOVEL);
    }
}
