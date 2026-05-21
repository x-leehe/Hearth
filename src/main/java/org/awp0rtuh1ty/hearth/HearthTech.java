package org.awp0rtuh1ty.hearth;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.ArrayList;
import java.util.List;

public final class HearthTech {
    private static final TagKey<Item> NUGGETS_TAG = TagKey.create(
            BuiltInRegistries.ITEM.key(),
            ResourceLocation.fromNamespaceAndPath("c", "nuggets")
    );

    private HearthTech() {}

    public static void initialize() {
        Hearth.LOGGER.info("Hearth Tech 生电扩展已加载");
        registerWoodAshDispenserBehavior();
        registerSlagDispenserBehavior();
    }

    /**
     * 发射器使用草木灰 → 遵循原版骨粉行为催熟作物
     */
    private static void registerWoodAshDispenserBehavior() {
        DispenserBlock.registerBehavior(WoodAsh.WOOD_ASH, (source, stack) -> {
            Level level = source.level();
            BlockPos targetPos = source.pos().relative(source.state().getValue(DispenserBlock.FACING));
            BlockState targetState = level.getBlockState(targetPos);

            if (targetState.getBlock() instanceof BonemealableBlock bonemealable
                    && bonemealable.isValidBonemealTarget(level, targetPos, targetState)) {

                if (level instanceof ServerLevel serverLevel) {
                    bonemealable.performBonemeal(serverLevel,
                            serverLevel.getRandom(), targetPos, targetState);
                }

                level.playSound(null, targetPos, SoundEvents.BONE_MEAL_USE,
                        SoundSource.BLOCKS, 1.0F, 1.0F);
                stack.shrink(1);
                return stack;
            }

            return defaultDispense(source, stack);
        });
    }

    /**
     * 发射器使用炉渣 → 对水炼药锅淘金（消耗炉渣，掉落1~9个随机粒）
     */
    private static void registerSlagDispenserBehavior() {
        DispenserBlock.registerBehavior(Slag.SLAG, (source, stack) -> {
            Level level = source.level();
            BlockPos targetPos = source.pos().relative(source.state().getValue(DispenserBlock.FACING));
            BlockState targetState = level.getBlockState(targetPos);

            if (!tryGoldPanning(source, targetState)) {
                return defaultDispense(source, stack);
            }
            stack.shrink(1);
            return stack;
        });
    }

    /**
     * 发射器对水炼药锅淘金（不消耗水位，产物沿发射器朝向射出）
     */
    private static boolean tryGoldPanning(net.minecraft.core.dispenser.BlockSource source, BlockState targetState) {
        if (!targetState.is(Blocks.WATER_CAULDRON)) {
            return false;
        }
        if (targetState.getValue(LayeredCauldronBlock.LEVEL) <= 0) {
            return false;
        }

        Level level = source.level();
        if (level instanceof ServerLevel serverLevel) {
            List<Item> nuggetItems = new ArrayList<>();
            BuiltInRegistries.ITEM.getTagOrEmpty(NUGGETS_TAG).forEach(holder -> {
                nuggetItems.add(holder.value());
            });

            if (!nuggetItems.isEmpty()) {
                int count = serverLevel.getRandom().nextInt(1, 10);
                Item randomNugget = nuggetItems.get(serverLevel.getRandom().nextInt(nuggetItems.size()));
                ItemStack result = new ItemStack(randomNugget, count);
                Direction facing = source.state().getValue(DispenserBlock.FACING);
                BlockPos targetPos = source.pos().relative(facing);
                spawnItem(level, result, 6, facing, targetPos);
            }

            BlockPos targetPos = source.pos().relative(source.state().getValue(DispenserBlock.FACING));
            level.playSound(null, targetPos, SoundEvents.GENERIC_SPLASH,
                    SoundSource.BLOCKS, 1.0F, 1.0F);
            level.gameEvent(null, GameEvent.FLUID_PICKUP, targetPos);
        }
        return true;
    }

    /**
     * 默认射出行为
     */
    private static ItemStack defaultDispense(net.minecraft.core.dispenser.BlockSource source, ItemStack stack) {
        Level level = source.level();
        Direction facing = source.state().getValue(DispenserBlock.FACING);
        BlockPos targetPos = source.pos().relative(facing);
        ItemStack ejected = stack.split(1);
        spawnItem(level, ejected, 6, facing, targetPos);
        return stack;
    }

    private static void spawnItem(Level level, ItemStack stack, int speed, Direction side, BlockPos pos) {
        double d = pos.getX() + 0.5;
        double e = pos.getY() + 0.5;
        double f = pos.getZ() + 0.5;
        if (side.getAxis() == Direction.Axis.Y) {
            e -= 0.125;
        } else {
            e -= 0.15625;
        }

        ItemEntity itemEntity = new ItemEntity(level, d, e, f, stack);
        double g = level.random.nextDouble() * 0.1 + 0.2;
        itemEntity.setDeltaMovement(
                level.random.triangle(side.getStepX() * g, 0.0172275 * speed),
                level.random.triangle(0.2, 0.0172275 * speed),
                level.random.triangle(side.getStepZ() * g, 0.0172275 * speed));
        level.addFreshEntity(itemEntity);
    }
}
