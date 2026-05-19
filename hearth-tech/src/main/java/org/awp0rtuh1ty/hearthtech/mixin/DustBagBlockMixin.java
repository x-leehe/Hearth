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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import org.awp0rtuh1ty.hearth.block.DustBagBlock;
import org.awp0rtuh1ty.hearth.potion.CleansingPotions;
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

    /** 注册 waxed 属性 */
    @Inject(method = "createBlockStateDefinition", at = @At("TAIL"))
    private void hearthtech$addWaxedProperty(StateDefinition.Builder<Block, BlockState> builder, CallbackInfo ci) {
        builder.add(HearthTechProperties.WAXED);
    }

    /** 放置时默认 waxed=false */
    @Inject(method = "getStateForPlacement", at = @At("RETURN"), cancellable = true)
    private void hearthtech$setDefaultWaxed(
            net.minecraft.world.item.context.BlockPlaceContext ctx,
            CallbackInfoReturnable<BlockState> cir) {
        cir.setReturnValue(cir.getReturnValue().setValue(HearthTechProperties.WAXED, false));
    }

    /**
     * 玩家右键集尘袋修改上蜡状态（仅玩家有效）：
     * - 蜜脾 → 上蜡（活塞不可推）
     * - 荡涤药水 → 恢复默认（活塞可推破坏）
     */
    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void hearthtech$modifyWaxedState(
            ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit,
            CallbackInfoReturnable<ItemInteractionResult> cir) {
        if (level.isClientSide) {
            return;
        }

        boolean wax;
        if (stack.is(Items.HONEYCOMB)) {
            wax = true;
        } else if (stack.is(Items.POTION) && isCleansingPotion(stack)) {
            wax = false;
        } else {
            return;
        }

        level.setBlock(pos, state.setValue(HearthTechProperties.WAXED, wax), 3);

        // 消耗物品
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

        if (wax) {
            level.playSound(null, pos, SoundEvents.HONEYCOMB_WAX_ON,
                    SoundSource.PLAYERS, 1.0F, 1.0F);
        } else {
            level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY,
                    SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);

        cir.setReturnValue(ItemInteractionResult.sidedSuccess(false));
    }

    /** 判断手持药水是否为荡涤药水 */
    private static boolean isCleansingPotion(ItemStack stack) {
        PotionContents contents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        return contents.is(CleansingPotions.CLEANSING)
                || contents.is(CleansingPotions.LONG_CLEANSING)
                || contents.is(CleansingPotions.STRONG_CLEANSING);
    }
}
