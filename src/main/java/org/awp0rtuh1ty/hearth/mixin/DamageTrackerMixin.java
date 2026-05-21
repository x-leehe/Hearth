package org.awp0rtuh1ty.hearth.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.awp0rtuh1ty.hearth.DamageTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class DamageTrackerMixin {

    @Inject(method = "hurt", at = @At("HEAD"))
    private void onHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (source.getEntity() instanceof LivingEntity attacker) {
            DamageTracker.recordDamage(attacker, (LivingEntity) (Object) this);
        }
    }
}
