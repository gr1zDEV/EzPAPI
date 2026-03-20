package dev.ezpapi.integration;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class FloodgateHook {
    private final Object api;
    private final java.lang.reflect.Method isFloodgatePlayerMethod;
    private final boolean available;

    public FloodgateHook() {
        Object resolvedApi = null;
        java.lang.reflect.Method resolvedMethod = null;
        boolean resolvedAvailable = false;

        try {
            Class<?> apiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            java.lang.reflect.Method getInstanceMethod = apiClass.getMethod("getInstance");
            resolvedApi = getInstanceMethod.invoke(null);
            resolvedMethod = apiClass.getMethod("isFloodgatePlayer", java.util.UUID.class);
            resolvedAvailable = Bukkit.getPluginManager().getPlugin("floodgate") != null;
        } catch (ReflectiveOperationException ignored) {
            resolvedAvailable = false;
        }

        this.api = resolvedApi;
        this.isFloodgatePlayerMethod = resolvedMethod;
        this.available = resolvedAvailable;
    }

    public boolean isAvailable() {
        return available && api != null && isFloodgatePlayerMethod != null;
    }

    public boolean isBedrockPlayer(Player player) {
        if (player == null || !isAvailable()) {
            return false;
        }

        try {
            return (boolean) isFloodgatePlayerMethod.invoke(api, player.getUniqueId());
        } catch (ReflectiveOperationException ex) {
            return false;
        }
    }

    public String getPlatform(Player player) {
        return isBedrockPlayer(player) ? "bedrock" : "java";
    }
}
