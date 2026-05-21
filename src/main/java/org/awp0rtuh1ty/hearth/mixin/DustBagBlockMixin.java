package org.awp0rtuh1ty.hearth.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import org.awp0rtuh1ty.hearth.HearthTechProperties;
import org.awp0rtuh1ty.hearth.WoodAsh;
import org.awp0rtuh1ty.hearth.block.DustBagBlock;
import org.awp0rtuh1ty.hearth.potion.CleansingPotions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Dust Bag right-click state switching (player-only).
 * - Honeycomb   -> waxed=true   piston blocked
 * - Wood ash    -> waxed=true   piston blocked (alternative)
 * - Cleansing potion -> waxed=false  restore default (returns glass bottle)
 */
@Mixin(DustBagBlock.class)
public abstract class DustBagBlockMixin extends Block {

    public DustBagBlockMixin(Properties properties) {
        super(properties);
    }

    @Inject(method = "createBlockStateDefinition", at = @At("TAIL"))
    private void hearth$addWaxedProperty(StateDefinition.Builder<Block, BlockState> builder, CallbackInfo ci) {
        builder.add(HearthTechProperties.WAXED);
    }

    @Inject(method = "getStateForPlacement", at = @At("RETURN"), cancellable = true)
    private void hearth$setDefaultWaxed(
            net.minecraft.world.item.context.BlockPlaceContext ctx,
            CallbackInfoReturnable<BlockState> cir) {
        cir.setReturnValue(cir.getReturnValue().setValue(HearthTechProperties.WAXED, false));
    }

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void hearth$switchWaxedState(
            ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit,
            CallbackInfoReturnable<ItemInteractionResult> cir) {
        if (level.isClientSide) {
            return;
        }

        boolean wax;
        SoundEvent sound;
        float pitch;

        if (stack.is(Items.HONEYCOMB)) {
            wax = true;
            sound = SoundEvents.HONEYCOMB_WAX_ON;
            pitch = 1.0F;
        } else if (stack.is(WoodAsh.WOOD_ASH)) {
            wax = true;
            sound = SoundEvents.SAND_PLACE;
            pitch = 0.8F;
        } else if (stack.is(Items.POTION) && isCleansingPotion(stack)) {
            wax = false;
            sound = SoundEvents.BOTTLE_EMPTY;
            pitch = 1.0F;
        } else {
            return;
        }

        level.setBlock(pos, state.setValue(HearthTechProperties.WAXED, wax), 3);

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

        level.playSound(null, pos, sound, SoundSource.PLAYERS, 1.0F, pitch);
        level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);

        cir.setReturnValue(ItemInteractionResult.sidedSuccess(false));
    }

    private static boolean isCleansingPotion(ItemStack stack) {
        PotionContents contents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        return contents.is(CleansingPotions.CLEANSING)
                || contents.is(CleansingPotions.LONG_CLEANSING)
                || contents.is(CleansingPotions.STRONG_CLEANSING);
    }
}
