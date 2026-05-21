package org.awp0rtuh1ty.hearth;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class HearthConfig {
    private static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("hearth.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type LIST_STRING_TYPE = new TypeToken<List<String>>(){}.getType();

    private static volatile ConfigData data = new ConfigData();

    private HearthConfig() {}

    public static void initialize() {
        if (Files.exists(CONFIG_FILE)) {
            try {
                String content = Files.readString(CONFIG_FILE, StandardCharsets.UTF_8);
                ConfigData loaded = GSON.fromJson(content, ConfigData.class);
                if (loaded != null) {
                    data = loaded;
                    ensureDefaults(data);
                }
            } catch (IOException e) {
                Hearth.LOGGER.warn("Failed to read config, using defaults", e);
            }
        } else {
            save();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_FILE.getParent());
            Files.writeString(CONFIG_FILE, GSON.toJson(data), StandardCharsets.UTF_8);
        } catch (IOException e) {
            Hearth.LOGGER.error("Failed to save config", e);
        }
    }

    // --- Accessors ---

    public static boolean isLoggingEnabled() {
        return data.logEnabled;
    }

    public static boolean isDestroyEmptyBottles() {
        return data.destroyEmptyBottles;
    }

    public static void setDestroyEmptyBottles(boolean value) {
        data.destroyEmptyBottles = value;
        save();
    }

    public static int getPotionStackSize() {
        return data.potionStackSize;
    }

    public static void setPotionStackSize(int value) {
        data.potionStackSize = Math.max(1, Math.min(value, 64));
        save();
    }

    public static ResourceLocation getByproductItem() {
        return ResourceLocation.tryParse(data.byproductItem);
    }

    public static int getByproductCount() {
        return data.byproductCount;
    }

    public static void setByproduct(String item, int count) {
        data.byproductItem = item;
        data.byproductCount = Math.max(1, Math.min(count, 64));
        save();
    }

    public static boolean isEntityAffected(EntityType<?> type) {
        ResourceLocation id = EntityType.getKey(type);
        String idStr = id.toString();
        if (data.potionAffectEntities.exclude.contains(idStr)) {
            return false;
        }
        if (data.potionAffectEntities.include.contains(idStr)) {
            return true;
        }
        // Not in either list: only affect if it's naturally a Monster
        return type.getCategory() == MobCategory.MONSTER;
    }

    public static void addAffectedEntity(ResourceLocation id) {
        String idStr = id.toString();
        data.potionAffectEntities.exclude.remove(idStr);
        if (!data.potionAffectEntities.include.contains(idStr)) {
            data.potionAffectEntities.include.add(idStr);
        }
        save();
    }

    public static void removeAffectedEntity(ResourceLocation id) {
        String idStr = id.toString();
        data.potionAffectEntities.include.remove(idStr);
        if (!data.potionAffectEntities.exclude.contains(idStr)) {
            data.potionAffectEntities.exclude.add(idStr);
        }
        save();
    }

    public static List<String> getIncludedEntities() {
        return Collections.unmodifiableList(data.potionAffectEntities.include);
    }

    public static List<String> getExcludedEntities() {
        return Collections.unmodifiableList(data.potionAffectEntities.exclude);
    }

    // --- Defaults ---

    private static void ensureDefaults(ConfigData d) {
        if (d.potionAffectEntities == null) {
            d.potionAffectEntities = new AffectEntities();
        }
        if (d.potionAffectEntities.include == null || d.potionAffectEntities.include.isEmpty()) {
            d.potionAffectEntities.include = defaultMonsters();
        }
        if (d.potionAffectEntities.exclude == null) {
            d.potionAffectEntities.exclude = new ArrayList<>();
        }
        if (d.byproductItem == null) d.byproductItem = "hearth:wood_ash";
        if (d.byproductCount <= 0) d.byproductCount = 2;
        if (d.potionStackSize <= 0) d.potionStackSize = 1;
    }

    private static List<String> defaultMonsters() {
        return Arrays.asList(
                "minecraft:blaze",
                "minecraft:bogged",
                "minecraft:breeze",
                "minecraft:cave_spider",
                "minecraft:creeper",
                "minecraft:drowned",
                "minecraft:elder_guardian",
                "minecraft:enderman",
                "minecraft:endermite",
                "minecraft:evoker",
                "minecraft:ghast",
                "minecraft:guardian",
                "minecraft:hoglin",
                "minecraft:husk",
                "minecraft:magma_cube",
                "minecraft:phantom",
                "minecraft:piglin",
                "minecraft:piglin_brute",
                "minecraft:pillager",
                "minecraft:ravager",
                "minecraft:shulker",
                "minecraft:silverfish",
                "minecraft:skeleton",
                "minecraft:slime",
                "minecraft:spider",
                "minecraft:stray",
                "minecraft:vex",
                "minecraft:vindicator",
                "minecraft:warden",
                "minecraft:witch",
                "minecraft:wither_skeleton",
                "minecraft:zoglin",
                "minecraft:zombie",
                "minecraft:zombie_villager",
                "minecraft:zombified_piglin"
        );
    }

    // --- POJO ---

    @SuppressWarnings("unused")
    static class ConfigData {
        boolean logEnabled;
        boolean destroyEmptyBottles = true;
        int potionStackSize = 1;
        String byproductItem = "hearth:wood_ash";
        int byproductCount = 2;
        AffectEntities potionAffectEntities;

        ConfigData() {
            this.potionAffectEntities = new AffectEntities();
            this.potionAffectEntities.include = defaultMonsters();
            this.potionAffectEntities.exclude = new ArrayList<>();
        }
    }

    @SuppressWarnings("unused")
    static class AffectEntities {
        List<String> include;
        List<String> exclude;
    }
}
