package org.awp0rtuh1ty.hearth;

public final class HearthLogConfig {

    private HearthLogConfig() {
    }

    public static void initialize() {
        HearthConfig.initialize();
    }

    public static boolean isLoggingEnabled() {
        return HearthConfig.isLoggingEnabled();
    }
}
