package org.awp0rtuh1ty.hearth.mixin;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import org.awp0rtuh1ty.hearth.HearthTechProperties;
import org.awp0rtuh1ty.hearth.block.DustBagBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Dust Bag piston push reaction:
 * - default (waxed=false) -> DESTROY  (piston destroys, drops with NBT)
 * - waxed (waxed=true)   -> BLOCK    (piston cannot push, player can still break)
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseMixin {

    @Inject(method = "getPistonPushReaction", at = @At("HEAD"), cancellable = true)
    private void hearth$modifyDustBagPushReaction(CallbackInfoReturnable<PushReaction> cir) {
        BlockState state = (BlockState) (Object) this;
        Block block = state.getBlock();
        if (!(block instanceof DustBagBlock)) {
            return;
        }
        if (!state.hasProperty(HearthTechProperties.WAXED)) {
            return;
        }
        boolean waxed = state.getValue(HearthTechProperties.WAXED);
        cir.setReturnValue(waxed ? PushReaction.BLOCK : PushReaction.DESTROY);
    }
}
