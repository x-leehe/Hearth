package org.awp0rtuh1ty.hearthtech.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import org.awp0rtuh1ty.hearth.WoodAsh;
import org.awp0rtuh1ty.hearth.block.DustBagBlock;
import org.awp0rtuh1ty.hearthtech.HearthTechProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DustBagBlock.class)
public abstract class DustBagBlockMixin extends Block {

    public DustBagBlockMixin(Properties properties) {
        super(properties);
    }

    /** 向方块状态定义注册 piston_state 属性 */
    @Inject(method = "createBlockStateDefinition", at = @At("TAIL"))
    private void hearthtech$addPistonStateProperty(StateDefinition.Builder<Block, BlockState> builder, CallbackInfo ci) {
        builder.add(HearthTechProperties.PISTON_STATE);
    }

    /** 放置时设置默认 piston_state = 0 */
    @Inject(method = "getStateForPlacement", at = @At("RETURN"), cancellable = true)
    private void hearthtech$setDefaultPistonState(
            net.minecraft.world.item.context.BlockPlaceContext ctx,
            CallbackInfoReturnable<BlockState> cir) {
        cir.setReturnValue(cir.getReturnValue().setValue(HearthTechProperties.PISTON_STATE, 0));
    }

    /**
     * 玩家右键集尘袋修改活塞状态（仅玩家有效）：
     * - 蜜蜡 → 可推不掉落 (state=1)
     * - 水瓶 → 可推且掉落 (state=2)
     * - 草木灰 → 不可推 (state=3)
     */
    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void hearthtech$modifyPistonState(
            ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit,
            CallbackInfoReturnable<ItemInteractionResult> cir) {
        if (level.isClientSide) {
            return;
        }

        int newState;
        if (stack.is(Items.HONEYCOMB)) {
            newState = 1;
        } else if (stack.is(Items.POTION)) {
            newState = 2;
        } else if (stack.is(WoodAsh.WOOD_ASH)) {
            newState = 3;
        } else {
            return;
        }

        level.setBlock(pos, state.setValue(HearthTechProperties.PISTON_STATE, newState), 3);

        if (stack.is(Items.POTION)) {
            stack.shrink(1);
            ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
            if (stack.isEmpty()) {
                player.setItemInHand(hand, bottle);
            } else if (!player.getInventory().add(bottle)) {
                player.drop(bottle, false);
            }
        } else {
            stack.shrink(1);
            if (stack.isEmpty()) {
                player.setItemInHand(hand, ItemStack.EMPTY);
            }
        }

        if (newState == 1) {
            level.playSound(null, pos, SoundEvents.HONEYCOMB_WAX_ON,
                    SoundSource.PLAYERS, 1.0F, 1.0F);
        } else if (newState == 2) {
            level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY,
                    SoundSource.PLAYERS, 1.0F, 1.0F);
        } else {
            level.playSound(null, pos, SoundEvents.SAND_PLACE,
                    SoundSource.PLAYERS, 1.0F, 0.8F);
        }
        level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);

        cir.setReturnValue(ItemInteractionResult.sidedSuccess(false));
    }
}
