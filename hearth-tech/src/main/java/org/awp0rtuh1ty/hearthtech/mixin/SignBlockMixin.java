package org.awp0rtuh1ty.hearthtech.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import org.awp0rtuh1ty.hearth.potion.CleansingPotions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 荡涤药水右键告示牌 → 洗去上蜡状态和荧光效果
 * 仅普通荡涤药水，喷溅/滞留型不实现
 */
@Mixin(SignBlock.class)
public abstract class SignBlockMixin {

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void hearthtech$cleanseSign(
            ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit,
            CallbackInfoReturnable<ItemInteractionResult> cir) {
        if (level.isClientSide) {
            return;
        }

        // 仅处理荡涤药水
        if (!stack.is(Items.POTION) || !isCleansingPotion(stack)) {
            return;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof SignBlockEntity signBe)) {
            return;
        }

        boolean changed = false;

        // 去除上蜡状态
        if (signBe.isWaxed()) {
            signBe.setWaxed(false);
            changed = true;
        }

        // 去除荧光效果
        SignText front = signBe.getFrontText();
        SignText back = signBe.getBackText();
        if (front.hasGlowingText()) {
            signBe.setText(front.setHasGlowingText(false), true);
            changed = true;
        }
        if (back.hasGlowingText()) {
            signBe.setText(back.setHasGlowingText(false), false);
            changed = true;
        }

        if (!changed) {
            return;
        }

        // 消耗药水，返还玻璃瓶
        stack.shrink(1);
        ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
        if (stack.isEmpty()) {
            player.setItemInHand(hand, bottle);
        } else if (!player.getInventory().add(bottle)) {
            player.drop(bottle, false);
        }

        level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY,
                SoundSource.PLAYERS, 1.0F, 1.0F);
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
