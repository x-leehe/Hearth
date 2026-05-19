package org.awp0rtuh1ty.hearthtech.mixin;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import org.awp0rtuh1ty.hearth.block.DustBagBlock;
import org.awp0rtuh1ty.hearthtech.HearthTechProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Override piston push reaction for Dust Bags based on piston_state.
 * 0 DEFAULT  -> DESTROY  (push breaks, drops with NBT)
 * 1 WAXED    -> NORMAL   (push moves, preserves contents)
 * 2 STICKY   -> BLOCK    (push blocked)
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseMixin {

    @Inject(method = "getPistonPushReaction", at = @At("HEAD"), cancellable = true)
    private void hearthtech$modifyDustBagPushReaction(CallbackInfoReturnable<PushReaction> cir) {
        BlockState state = (BlockState) (Object) this;
        Block block = state.getBlock();
        if (!(block instanceof DustBagBlock)) {
            return;
        }
        if (!state.hasProperty(HearthTechProperties.PISTON_STATE)) {
            return;
        }
        int mode = state.getValue(HearthTechProperties.PISTON_STATE);
        PushReaction reaction = switch (mode) {
            case 1 -> PushReaction.NORMAL;
            case 2 -> PushReaction.BLOCK;
            default -> PushReaction.DESTROY;
        };
        cir.setReturnValue(reaction);
    }
}
