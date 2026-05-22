package org.awp0rtuh1ty.hearth;

public final class HearthSettings {

    private HearthSettings() {}

    // ========== [Vulkan] ==========

    @HearthRule(categories = {"Vulkan"}, options = {"1", "64"}, strict = false)
    public static int potionStackSize = 1;

    @HearthRule(categories = {"Vulkan"})
    public static String byproductItem = "hearth:wood_ash";

    @HearthRule(categories = {"Vulkan"}, strict = false)
    public static int byproductCount = 2;

    @HearthRule(categories = {"Vulkan"}, options = {"true", "false"})
    public static boolean logEnabled;

    @HearthRule(categories = {"Vulkan"}, options = {"", "en_us", "zh_cn"}, strict = false)
    public static String language = "";

    // ========== [Tech] ==========

    @HearthRule(categories = {"Tech"}, options = {"true", "false"})
    public static boolean destroyEmptyBottles = true;
}
