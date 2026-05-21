package org.awp0rtuh1ty.hearth.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.Iterator;

public class CleansingEffect extends MobEffect {
    public CleansingEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xE0F8FF);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        // 每 5 tick 执行一次，平衡性能与效果
        return duration % 5 == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        // 仅对非玩家生物生效：远离最近玩家
        if (!(entity instanceof Player)) {
            Level level = entity.level();
            if (!level.isClientSide) {
                Player nearestPlayer = level.getNearestPlayer(entity, 16.0);
                if (nearestPlayer != null) {
                    double dx = entity.getX() - nearestPlayer.getX();
                    double dz = entity.getZ() - nearestPlayer.getZ();
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    if (dist > 0.01) {
                        entity.push(dx / dist * 0.35, 0.1, dz / dist * 0.35);
                    }
                }
            }
        }
        return true;
    }

    @Override
    public void onEffectStarted(LivingEntity entity, int amplifier) {
        // 立即清除所有负面效果
        Iterator<MobEffectInstance> it = entity.getActiveEffects().iterator();
        while (it.hasNext()) {
            MobEffectInstance instance = it.next();
            if (instance.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                entity.removeEffect(instance.getEffect());
            }
        }

        // 饮用时 -2HP（仅对玩家生效，避免喷溅/滞留药水误伤）
        if (entity instanceof Player) {
            entity.hurt(entity.damageSources().magic(), 2.0F);
        }
    }
}
