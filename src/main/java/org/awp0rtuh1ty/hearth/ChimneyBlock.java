package org.awp0rtuh1ty.hearth;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;

public class ChimneyBlock extends Block {
    public ChimneyBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        BlockPos abovePos = pos.above();
        BlockState aboveState = level.getBlockState(abovePos);

        // 如果上方是墙类方块，粒子在墙方块处生成；否则在烟囱本身生成
        BlockPos particlePos;
        if (aboveState.getBlock() instanceof WallBlock) {
            particlePos = abovePos;
        } else {
            particlePos = pos;
        }

        // 篝火噼啪声
        if (random.nextInt(10) == 0) {
            level.playLocalSound(
                    (double) particlePos.getX() + 0.5,
                    (double) particlePos.getY() + 0.5,
                    (double) particlePos.getZ() + 0.5,
                    SoundEvents.FURNACE_FIRE_CRACKLE,
                    SoundSource.BLOCKS,
                    0.5F + random.nextFloat(),
                    random.nextFloat() * 0.7F + 0.6F,
                    false);
        }

        // 烟雾粒子
        for (int i = 0; i < 3; i++) {
            level.addParticle(
                    ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    (double) particlePos.getX() + 0.5 + random.nextDouble() / 3.0 * (random.nextBoolean() ? 1 : -1),
                    (double) particlePos.getY() + 1.0 + random.nextDouble() + random.nextDouble(),
                    (double) particlePos.getZ() + 0.5 + random.nextDouble() / 3.0 * (random.nextBoolean() ? 1 : -1),
                    0.0,
                    0.07,
                    0.0);
        }
    }
}
