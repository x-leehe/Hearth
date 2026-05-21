package org.awp0rtuh1ty.hearth.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.awp0rtuh1ty.hearth.Hearth;
import org.awp0rtuh1ty.hearth.effect.HearthEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Set;

@Mixin(AreaEffectCloud.class)
public abstract class AreaEffectCloudMixin {

    private static final Set<ResourceLocation> CLEANSING_POTIONS = Set.of(
            ResourceLocation.fromNamespaceAndPath(Hearth.MOD_ID, "cleansing"),
            ResourceLocation.fromNamespaceAndPath(Hearth.MOD_ID, "long_cleansing"),
            ResourceLocation.fromNamespaceAndPath(Hearth.MOD_ID, "strong_cleansing")
    );

    private static final int BLOCK_CLEAN_INTERVAL = 40; // 每40 tick清理一次方块

    @Shadow
    private PotionContents potionContents;

    @Shadow
    private int duration;

    @Shadow
    public abstract float getRadius();

    @Inject(method = "tick", at = @At("TAIL"))
    private void hearth$tickCleansingCloud(CallbackInfo ci) {
        AreaEffectCloud self = (AreaEffectCloud) (Object) this;

        // 判断是否为荡涤药水云
        boolean isCleansing = potionContents != null && potionContents.potion()
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

        // B.1: 对范围内怪物施加伤害 + 远离玩家
        float radius = getRadius();
        List<LivingEntity> entities = level.getEntitiesOfClass(
                LivingEntity.class,
                self.getBoundingBox().inflate(radius, radius / 2.0, radius)
        );

        for (LivingEntity entity : entities) {
            if (entity.isAlive()) {
                // 给非玩家的生物施加荡涤效果（如果还没有）
                if (!(entity instanceof Player) && !entity.hasEffect(HearthEffects.CLEANSING)) {
                    entity.addEffect(new MobEffectInstance(HearthEffects.CLEANSING, 100, 0));
                }

                // 对怪物持续扣血（每20 tick = 1秒扣1HP）
                if (!(entity instanceof Player) && self.tickCount % 20 == 0) {
                    entity.hurt(entity.damageSources().magic(), 1.0F);
                }
            }
        }

        // B.2: 清理染色方块
        if (self.tickCount % BLOCK_CLEAN_INTERVAL == 0) {
            BlockPos center = self.blockPosition();
            int r = (int) Math.ceil(radius);
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -r; dz <= r; dz++) {
                        BlockPos pos = center.offset(dx, dy, dz);
                        BlockState state = level.getBlockState(pos);
                        Block replacement = getUndyedVersion(state.getBlock());

                        if (replacement != null && replacement != state.getBlock()) {
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
                                    3, 0.3, 0.3, 0.3, 0.05);
                        }
                    }
                }
            }
        }
    }

    private static Block getUndyedVersion(Block block) {
        if (block instanceof StainedGlassBlock) return Blocks.GLASS;
        if (block instanceof StainedGlassPaneBlock) return Blocks.GLASS_PANE;
        if (block instanceof WoolCarpetBlock) return Blocks.WHITE_CARPET;
        if (block instanceof ShulkerBoxBlock) return Blocks.SHULKER_BOX;
        if (block instanceof GlazedTerracottaBlock) return Blocks.WHITE_GLAZED_TERRACOTTA;
        if (block instanceof ConcretePowderBlock) return Blocks.WHITE_CONCRETE_POWDER;
        if (block instanceof CandleBlock && !(block instanceof CandleCakeBlock)) return Blocks.CANDLE;
        if (block instanceof BannerBlock) return Blocks.WHITE_BANNER;

        String name = BuiltInRegistries.BLOCK.getKey(block).getPath();
        if (name.endsWith("_wool") && !name.equals("white_wool")) return Blocks.WHITE_WOOL;
        if (name.endsWith("_concrete") && !name.equals("white_concrete")) return Blocks.WHITE_CONCRETE;
        if (name.endsWith("_terracotta") && !name.equals("terracotta") && !name.contains("glazed")) return Blocks.TERRACOTTA;
        return null;
    }
}
