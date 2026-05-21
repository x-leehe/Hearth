package org.awp0rtuh1ty.hearth.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.awp0rtuh1ty.hearth.Hearth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(ThrownPotion.class)
public abstract class ThrownPotionMixin {

    private static final Set<ResourceLocation> CLEANSING_POTIONS = Set.of(
            ResourceLocation.fromNamespaceAndPath(Hearth.MOD_ID, "cleansing"),
            ResourceLocation.fromNamespaceAndPath(Hearth.MOD_ID, "long_cleansing"),
            ResourceLocation.fromNamespaceAndPath(Hearth.MOD_ID, "strong_cleansing")
    );

    @Inject(method = "onHit", at = @At("TAIL"))
    private void hearth$cleanDyedBlocks(HitResult hitResult, CallbackInfo ci) {
        ThrownPotion self = (ThrownPotion) (Object) this;

        // 判断是否为荡涤药水
        PotionContents contents = self.getItem().getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        boolean isCleansing = contents.potion()
                .map(holder -> CLEANSING_POTIONS.contains(holder.unwrapKey()
                        .map(key -> key.location())
                        .orElse(null)))
                .orElse(false);

        if (!isCleansing) {
            return;
        }

        Level level = self.level();
        if (level.isClientSide) {
            return;
        }

        BlockPos center;
        if (hitResult instanceof BlockHitResult blockHit) {
            center = blockHit.getBlockPos();
        } else {
            center = self.blockPosition();
        }

        // 遍历 3×3×3 区域，还原染色方块
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(pos);
                    Block block = state.getBlock();
                    Block replacement = getUndyedVersion(block);

                    if (replacement != null && replacement != block) {
                        // 保存方块实体数据（潜影盒内容、旗帜图案等）
                        BlockEntity oldBe = level.getBlockEntity(pos);
                        CompoundTag beTag = null;
                        if (oldBe != null) {
                            beTag = oldBe.saveWithFullMetadata(level.registryAccess());
                        }
                        level.setBlock(pos, replacement.withPropertiesOf(state), 3);
                        if (beTag != null) {
                            BlockEntity newBe = level.getBlockEntity(pos);
                            if (newBe != null) {
                                newBe.loadWithComponents(beTag, level.registryAccess());
                            }
                        }
                        // 净化粒子效果
                        ((ServerLevel) level).sendParticles(
                                ParticleTypes.WAX_ON,
                                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                                5, 0.4, 0.4, 0.4, 0.1);
                    }
                }
            }
        }
    }

    private static Block getUndyedVersion(Block block) {
        if (block instanceof StainedGlassBlock) {
            return Blocks.GLASS;
        }
        if (block instanceof StainedGlassPaneBlock) {
            return Blocks.GLASS_PANE;
        }
        if (block instanceof WoolCarpetBlock) {
            return Blocks.WHITE_CARPET;
        }
        if (block instanceof ShulkerBoxBlock) {
            return Blocks.SHULKER_BOX;
        }
        if (block instanceof GlazedTerracottaBlock) {
            return Blocks.WHITE_GLAZED_TERRACOTTA;
        }
        if (block instanceof ConcretePowderBlock) {
            return Blocks.WHITE_CONCRETE_POWDER;
        }
        if (block instanceof CandleBlock && !(block instanceof CandleCakeBlock)) {
            return Blocks.CANDLE;
        }
        if (block instanceof BannerBlock) {
            return Blocks.WHITE_BANNER;
        }

        // 通过方块 ID 名称匹配染色方块（Terracotta、Wool、Concrete 等在 1.21 中没有单独的类）
        String name = BuiltInRegistries.BLOCK.getKey(block).getPath();
        if (name.endsWith("_wool") && !name.equals("white_wool")) {
            return Blocks.WHITE_WOOL;
        }
        if (name.endsWith("_concrete") && !name.equals("white_concrete")) {
            return Blocks.WHITE_CONCRETE;
        }
        if (name.endsWith("_terracotta") && !name.equals("terracotta") && !name.contains("glazed")) {
            return Blocks.TERRACOTTA;
        }
        return null;
    }
}
