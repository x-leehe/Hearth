package org.awp0rtuh1ty.hearth;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.*;

public final class DamageTracker {
    private static final Map<UUID, Map<UUID, Long>> damageRecords = new HashMap<>();

    private DamageTracker() {}

    public static void recordDamage(LivingEntity attacker, LivingEntity victim) {
        if (victim instanceof Player) {
            damageRecords
                    .computeIfAbsent(attacker.getUUID(), k -> new HashMap<>())
                    .put(victim.getUUID(), victim.level().getGameTime());
        }
    }

    public static boolean wasRecentlyDamagedBy(Player player, LivingEntity attacker, int memoryTicks) {
        Map<UUID, Long> victims = damageRecords.get(attacker.getUUID());
        if (victims == null) return false;
        Long time = victims.get(player.getUUID());
        if (time == null) return false;
        return player.level().getGameTime() - time < memoryTicks;
    }

    public static void cleanup(long maxAge) {
        damageRecords.values().removeIf(victims -> {
            victims.values().removeIf(t -> t < maxAge);
            return victims.isEmpty();
        });
    }
}
