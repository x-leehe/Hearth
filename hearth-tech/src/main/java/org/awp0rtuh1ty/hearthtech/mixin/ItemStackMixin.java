package org.awp0rtuh1ty.hearthtech.mixin;

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
 * Checks actual Items list inside BlockEntityData rather than just tag presence.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @Inject(method = "getMaxStackSize", at = @At("HEAD"), cancellable = true)
    private void hearthtech$dustBagMaxStackSize(CallbackInfoReturnable<Integer> cir) {
        ItemStack self = (ItemStack) (Object) this;
        if (!self.is(DustBag.DUST_BAG_ITEM)) {
            return;
        }
        // Check if the block entity data contains actual items
        if (!self.has(DataComponents.BLOCK_ENTITY_DATA)) {
            return; // no data at all -> stackable to 64
        }
        CompoundTag beTag = self.get(DataComponents.BLOCK_ENTITY_DATA).copyTag();
        if (!beTag.contains("Items", Tag.TAG_LIST)) {
            return; // no Items key -> stackable to 64
        }
        if (beTag.getList("Items", Tag.TAG_COMPOUND).isEmpty()) {
            return; // Items list is empty -> stackable to 64
        }
        // Has at least one stored item -> max stack 1
        cir.setReturnValue(1);
    }
}
