package org.awp0rtuh1ty.hearth.mixin;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.awp0rtuh1ty.hearth.DustBag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Dust Bag stacking: empty bags stack to 64; bags with any items inside max 1.
 */
@Mixin(ItemStack.class)
public abstract class DustBagStackMixin {

    @Inject(method = "getMaxStackSize", at = @At("HEAD"), cancellable = true)
    private void hearth$dustBagMaxStackSize(CallbackInfoReturnable<Integer> cir) {
        ItemStack self = (ItemStack) (Object) this;
        if (!self.is(DustBag.DUST_BAG_ITEM)) {
            return;
        }
        if (!self.has(DataComponents.BLOCK_ENTITY_DATA)) {
            return;
        }
        CompoundTag beTag = self.get(DataComponents.BLOCK_ENTITY_DATA).copyTag();
        if (!beTag.contains("Items", Tag.TAG_LIST)) {
            return;
        }
        if (beTag.getList("Items", Tag.TAG_COMPOUND).isEmpty()) {
            return;
        }
        cir.setReturnValue(1);
    }
}
