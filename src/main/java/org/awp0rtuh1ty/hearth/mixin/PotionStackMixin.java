package org.awp0rtuh1ty.hearth.mixin;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.awp0rtuh1ty.hearth.HearthConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class PotionStackMixin {

    @Inject(method = "getMaxStackSize", at = @At("HEAD"), cancellable = true)
    private void hearth$potionStackSize(CallbackInfoReturnable<Integer> cir) {
        ItemStack self = (ItemStack) (Object) this;
        if (self.is(Items.POTION) || self.is(Items.SPLASH_POTION) || self.is(Items.LINGERING_POTION)) {
            cir.setReturnValue(HearthConfig.getPotionStackSize());
        }
    }
}
