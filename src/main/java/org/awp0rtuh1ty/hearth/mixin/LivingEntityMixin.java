package org.awp0rtuh1ty.hearth.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.awp0rtuh1ty.hearth.effect.HearthEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @ModifyVariable(
            method = "actuallyHurt",
            at = @At("HEAD"),
            argsOnly = true
    )
    private float hearth$halveDamageWithCleansing(float damage) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.hasEffect(HearthEffects.CLEANSING)) {
            return damage * 0.5F;
        }
        return damage;
    }
}
