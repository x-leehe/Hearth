package org.awp0rtuh1ty.hearth;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

public class WoodAshItem extends Item {
    public WoodAshItem(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.sidedSuccess(true);
        }

        BlockPos pos = context.getClickedPos();
        ItemStack itemStack = context.getItemInHand();
        BlockState state = level.getBlockState(pos);

        // 对水炼药锅使用草木灰 → 转换为荡涤药水炼药锅
        if (state.is(Blocks.WATER_CAULDRON)) {
            int currentLevel = state.getValue(LayeredCauldronBlock.LEVEL);
            BlockState cleansingState = CleansingCauldron.CLEANSING_CAULDRON
                    .defaultBlockState()
                    .setValue(LayeredCauldronBlock.LEVEL, currentLevel);
            level.setBlock(pos, cleansingState, 3);
            itemStack.shrink(1);
            level.gameEvent(context.getPlayer(), GameEvent.BLOCK_CHANGE, pos);
            return InteractionResult.sidedSuccess(false);
        }

        if (!(state.getBlock() instanceof BonemealableBlock bonemealable)) {
            return InteractionResult.PASS;
        }

        if (!bonemealable.isValidBonemealTarget(level, pos, state)) {
            return InteractionResult.PASS;
        }

        if (level instanceof ServerLevel serverLevel) {
            if (state.getBlock() instanceof CropBlock crop) {
                if (!crop.isMaxAge(state)) {
                    level.setBlock(pos, state.setValue(CropBlock.AGE, crop.getMaxAge()), 2);
                }
            } else {
                bonemealable.performBonemeal(serverLevel, serverLevel.getRandom(), pos, state);
            }

            Player player = context.getPlayer();
            if (player != null) {
                player.gameEvent(GameEvent.BLOCK_CHANGE);
            }
            level.levelEvent(2005, pos, 0);
            itemStack.shrink(1);
            return InteractionResult.sidedSuccess(true);
        }

        return InteractionResult.PASS;
    }
}
