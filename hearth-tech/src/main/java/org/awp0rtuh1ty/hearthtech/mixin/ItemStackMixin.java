package org.awp0rtuh1ty.hearthtech.mixin;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import org.awp0rtuh1ty.hearth.DustBag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 集尘袋堆叠逻辑：
 * - 含有物品时（有 BlockEntityData）→ 最大堆叠 1（类似潜影盒）
 * - 无物品时 → 可堆叠至 64
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @Inject(method = "getMaxStackSize", at = @At("HEAD"), cancellable = true)
    private void hearthtech$dustBagMaxStackSize(CallbackInfoReturnable<Integer> cir) {
        ItemStack self = (ItemStack) (Object) this;
        if (self.is(DustBag.DUST_BAG_ITEM) && self.has(DataComponents.BLOCK_ENTITY_DATA)) {
            cir.setReturnValue(1);
        }
    }
}
