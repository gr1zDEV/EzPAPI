package dev.ezpapi.integration;

import dev.ezpapi.EZPAPIPlugin;
import dev.ezpapi.model.PlaceholderDefinition;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class EZPAPIPlaceholderExpansion extends PlaceholderExpansion {
    private final EZPAPIPlugin plugin;

    public EZPAPIPlaceholderExpansion(EZPAPIPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "ezpapi";
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors().isEmpty() ? java.util.List.of("OpenAI") : plugin.getDescription().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
        if (params.equalsIgnoreCase("is_bedrock")) {
            Player player = offlinePlayer != null ? offlinePlayer.getPlayer() : null;
            return String.valueOf(plugin.getFloodgateHook().isBedrockPlayer(player));
        }

        if (params.equalsIgnoreCase("platform")) {
            Player player = offlinePlayer != null ? offlinePlayer.getPlayer() : null;
            return plugin.getFloodgateHook().getPlatform(player);
        }

        boolean formatted = params.toLowerCase(java.util.Locale.ROOT).endsWith("_formatted");
        String key = formatted ? params.substring(0, params.length() - "_formatted".length()) : params;
        PlaceholderDefinition definition = plugin.getConfigManager().getDefinition(key);
        if (definition == null) {
            return null;
        }

        Object value = definition.defaultValue();
        if (offlinePlayer != null && offlinePlayer.getUniqueId() != null) {
            value = plugin.getPlayerDataManager().getValue(offlinePlayer.getUniqueId(), definition);
        }

        return formatted ? plugin.getMessagesManager().colorize(definition.formatValue(value)) : String.valueOf(value);
    }
}
