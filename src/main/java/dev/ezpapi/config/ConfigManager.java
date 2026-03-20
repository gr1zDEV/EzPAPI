package dev.ezpapi.config;

import dev.ezpapi.EZPAPIPlugin;
import dev.ezpapi.model.PlaceholderDefinition;
import dev.ezpapi.model.PlaceholderType;
import dev.ezpapi.model.ValueParseResult;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class ConfigManager {
    private final EZPAPIPlugin plugin;
    private Map<String, PlaceholderDefinition> placeholders = Collections.emptyMap();

    public ConfigManager(EZPAPIPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection section = config.getConfigurationSection("placeholders");
        Map<String, PlaceholderDefinition> loaded = new LinkedHashMap<>();

        if (section != null) {
            for (String rawKey : section.getKeys(false)) {
                String normalizedKey = normalizeKey(rawKey);
                ConfigurationSection placeholderSection = section.getConfigurationSection(rawKey);
                if (placeholderSection == null) {
                    plugin.getLogger().warning("Skipping placeholder '" + rawKey + "' because it is not a section.");
                    continue;
                }

                PlaceholderType type = PlaceholderType.fromConfig(placeholderSection.getString("type"));
                if (type == null) {
                    plugin.getLogger().warning("Skipping placeholder '" + rawKey + "' because its type is invalid.");
                    continue;
                }

                Object defaultValue = parseDefault(normalizedKey, type, placeholderSection.get("default"));
                if (defaultValue == null) {
                    plugin.getLogger().warning("Skipping placeholder '" + rawKey + "' because its default value is invalid for type " + type + ".");
                    continue;
                }

                loaded.put(normalizedKey, new PlaceholderDefinition(
                        normalizedKey,
                        type,
                        defaultValue,
                        placeholderSection.getString("true-format"),
                        placeholderSection.getString("false-format"),
                        placeholderSection.getString("format")
                ));
            }
        }

        this.placeholders = Collections.unmodifiableMap(loaded);
    }

    public Collection<String> getKeys() {
        return placeholders.keySet();
    }

    public PlaceholderDefinition getDefinition(String key) {
        if (key == null) {
            return null;
        }
        return placeholders.get(normalizeKey(key));
    }

    public Map<String, PlaceholderDefinition> getPlaceholders() {
        return placeholders;
    }

    public String normalizeKey(String key) {
        return key.toLowerCase(Locale.ROOT);
    }

    public ValueParseResult parseValue(PlaceholderDefinition definition, String rawInput) {
        Objects.requireNonNull(definition, "definition");
        String sanitized = sanitizeStringInput(rawInput);

        try {
            return switch (definition.type()) {
                case BOOLEAN -> {
                    if (!"true".equalsIgnoreCase(sanitized) && !"false".equalsIgnoreCase(sanitized)) {
                        yield ValueParseResult.failure("boolean");
                    }
                    yield ValueParseResult.success(Boolean.parseBoolean(sanitized));
                }
                case INTEGER -> ValueParseResult.success(Integer.parseInt(sanitized));
                case DOUBLE -> ValueParseResult.success(Double.parseDouble(sanitized));
                case STRING -> ValueParseResult.success(sanitized);
            };
        } catch (NumberFormatException ex) {
            return ValueParseResult.failure(definition.type().name().toLowerCase(Locale.ROOT));
        }
    }

    private Object parseDefault(String key, PlaceholderType type, Object rawValue) {
        if (rawValue == null) {
            return null;
        }

        ValueParseResult result = parseValue(new PlaceholderDefinition(key, type, null, null, null, null), String.valueOf(rawValue));
        return result.success() ? result.value() : null;
    }

    private String sanitizeStringInput(String input) {
        if (input == null) {
            return "";
        }

        String trimmed = input.trim();
        if (trimmed.length() >= 2) {
            boolean doubleQuoted = trimmed.startsWith("\"") && trimmed.endsWith("\"");
            boolean singleQuoted = trimmed.startsWith("'") && trimmed.endsWith("'");
            if (doubleQuoted || singleQuoted) {
                return trimmed.substring(1, trimmed.length() - 1);
            }
        }
        return trimmed;
    }
}
