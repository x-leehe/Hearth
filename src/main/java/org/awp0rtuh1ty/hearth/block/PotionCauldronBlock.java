package org.awp0rtuh1ty.hearth.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.awp0rtuh1ty.hearth.Hearth;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class PotionCauldronBlock extends BaseEntityBlock {
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 1, 3);
    public static final MapCodec<PotionCauldronBlock> CODEC = simpleCodec(PotionCauldronBlock::new);

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0, 0, 0, 2, 16, 16),
            Block.box(14, 0, 0, 16, 16, 16),
            Block.box(2, 0, 0, 14, 16, 2),
            Block.box(2, 0, 14, 14, 16, 16),
            Block.box(2, 0, 2, 14, 3, 14)
    );

    private static final Set<ResourceLocation> CLEANSING_POTION_IDS = Set.of(
            ResourceLocation.fromNamespaceAndPath(Hearth.MOD_ID, "cleansing"),
            ResourceLocation.fromNamespaceAndPath(Hearth.MOD_ID, "long_cleansing"),
            ResourceLocation.fromNamespaceAndPath(Hearth.MOD_ID, "strong_cleansing")
    );

    public PotionCauldronBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(LEVEL, 1));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PotionCauldronBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return ItemInteractionResult.sidedSuccess(true);
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof PotionCauldronBlockEntity cauldronBe)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        PotionContents stored = cauldronBe.getPotionContents();
        int curLevel = cauldronBe.getFillLevel();

        // 玻璃瓶 → 装取药水
        if (stack.is(Items.GLASS_BOTTLE)) {
            ItemStack potionStack = new ItemStack(Items.POTION);
            potionStack.set(DataComponents.POTION_CONTENTS, stored);

            stack.shrink(1);
            if (stack.isEmpty()) {
                player.setItemInHand(hand, potionStack);
            } else if (!player.getInventory().add(potionStack)) {
                player.drop(potionStack, false);
            }

            lowerLevel(level, pos, state, cauldronBe, curLevel);
            level.playSound(null, pos, SoundEvents.BOTTLE_FILL,
                    SoundSource.BLOCKS, 1.0F, 1.0F);
            level.gameEvent(null, GameEvent.FLUID_PICKUP, pos);
            return ItemInteractionResult.sidedSuccess(false);
        }

        // 相同药水 → 填充炼药锅
        PotionContents held = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        if (held.equals(stored) && curLevel < 3) {
            curLevel++;
            state = state.setValue(LEVEL, curLevel);
            level.setBlock(pos, state, 3);
            cauldronBe.setFillLevel(curLevel);

            stack.shrink(1);
            ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
            if (stack.isEmpty()) {
                player.setItemInHand(hand, bottle);
            } else if (!player.getInventory().add(bottle)) {
                player.drop(bottle, false);
            }

            level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY,
                    SoundSource.BLOCKS, 1.0F, 1.0F);
            level.gameEvent(null, GameEvent.FLUID_PLACE, pos);
            return ItemInteractionResult.sidedSuccess(false);
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    private void lowerLevel(Level level, BlockPos pos, BlockState state,
                            PotionCauldronBlockEntity be, int curLevel) {
        curLevel--;
        if (curLevel <= 0) {
            level.setBlock(pos, Blocks.CAULDRON.defaultBlockState(), 3);
            level.playSound(null, pos, SoundEvents.BUCKET_EMPTY,
                    SoundSource.BLOCKS, 1.0F, 1.0F);
        } else {
            state = state.setValue(LEVEL, curLevel);
            level.setBlock(pos, state, 3);
            be.setFillLevel(curLevel);
        }
    }
}
