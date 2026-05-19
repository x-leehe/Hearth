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
 * 拦截 BlockStateBase.getPistonPushReaction()，
 * 为集尘袋根据 piston_state 返回对应的活塞行为
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
        int pistonState = state.getValue(HearthTechProperties.PISTON_STATE);
        PushReaction reaction = switch (pistonState) {
            case 1 -> PushReaction.NORMAL;
            case 2 -> PushReaction.DESTROY;
            default -> PushReaction.BLOCK;
        };
        cir.setReturnValue(reaction);
    }
}
