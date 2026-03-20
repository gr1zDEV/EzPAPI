package dev.ezpapi.integration;

import dev.ezpapi.EZPAPIPlugin;
import dev.ezpapi.model.PlaceholderDefinition;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class EZPAPIPlaceholderExpansion extends PlaceholderExpansion {
    private static final String FORMATTED_SUFFIX = "_formatted";

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
        List<String> authors = plugin.getDescription().getAuthors();
        return authors.isEmpty() ? "OpenAI" : String.join(", ", authors);
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
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String identifier) {
        return resolvePlaceholder(player, identifier);
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String identifier) {
        return resolvePlaceholder(player, identifier);
    }

    public @Nullable String resolvePlaceholder(@Nullable OfflinePlayer player, @NotNull String identifier) {
        if (identifier.equalsIgnoreCase("is_bedrock")) {
            Player onlinePlayer = player != null ? player.getPlayer() : null;
            return String.valueOf(plugin.getFloodgateHook().isBedrockPlayer(onlinePlayer));
        }

        if (identifier.equalsIgnoreCase("platform")) {
            Player onlinePlayer = player != null ? player.getPlayer() : null;
            return plugin.getFloodgateHook().getPlatform(onlinePlayer);
        }

        boolean formatted = identifier.toLowerCase(Locale.ROOT).endsWith(FORMATTED_SUFFIX);
        String key = formatted ? identifier.substring(0, identifier.length() - FORMATTED_SUFFIX.length()) : identifier;
        PlaceholderDefinition definition = plugin.getConfigManager().getDefinition(key);
        if (definition == null) {
            return null;
        }

        Object value = getResolvedValue(player, definition);
        return formatted ? plugin.getMessagesManager().colorize(definition.formatValue(value)) : String.valueOf(value);
    }

    private Object getResolvedValue(@Nullable OfflinePlayer player, PlaceholderDefinition definition) {
        UUID uniqueId = player != null ? player.getUniqueId() : null;
        if (uniqueId == null) {
            return definition.defaultValue();
        }
        return plugin.getPlayerDataManager().getValue(uniqueId, definition);
    }
}
