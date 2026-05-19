package org.awp0rtuh1ty.hearth;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.fabricmc.loader.api.FabricLoader;

public final class HearthLogConfig {
    // 配置文件名称，放在 Fabric 的 config 目录下
    private static final String CONFIG_FILE_NAME = "hearth.json";
    // JSON 里控制日志开关的字段名
    private static final String LOG_ENABLED_KEY = "logEnabled";
    // 用于从 JSON 文本中提取 true/false 值的正则表达式
    private static final Pattern LOG_ENABLED_PATTERN = Pattern.compile(
            "\"" + LOG_ENABLED_KEY + "\"\\s*:\\s*(true|false)", Pattern.CASE_INSENSITIVE);
    // 运行时缓存当前是否允许日志输出
    private static volatile boolean logEnabled = false;

    private HearthLogConfig() {
    }

    public static void initialize() {
        logEnabled = false;
        // 获取 config 目录下的 hearth.json 文件路径
        Path configFile = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE_NAME);
        // 读取文件并设置 logEnabled，如果没有文件则使用默认 false
        logEnabled = readLogEnabled(configFile, false);
    }

    public static boolean isLoggingEnabled() {
        return logEnabled;
    }

    private static boolean readLogEnabled(Path configFile, boolean defaultValue) {
        if (!Files.exists(configFile)) {
            // 没有配置文件则返回默认值
            return defaultValue;
        }

        try {
            // 读取 hearth.json 全文
            String content = Files.readString(configFile, StandardCharsets.UTF_8);
            // 匹配 "logEnabled": true 或 false
            Matcher matcher = LOG_ENABLED_PATTERN.matcher(content);
            if (matcher.find()) {
                return Boolean.parseBoolean(matcher.group(1).toLowerCase());
            }
        } catch (IOException ignored) {
            // 读取失败时继续使用默认值
        }
        return defaultValue;
    }
}
