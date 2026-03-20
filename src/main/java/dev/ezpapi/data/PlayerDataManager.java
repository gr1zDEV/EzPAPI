package dev.ezpapi.data;

import dev.ezpapi.EZPAPIPlugin;
import dev.ezpapi.model.PlaceholderDefinition;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerDataManager {
    private final EZPAPIPlugin plugin;
    private File dataFile;
    private FileConfiguration dataConfig;
    private final Map<UUID, Map<String, Object>> cache = new HashMap<>();

    public PlayerDataManager(EZPAPIPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Could not create plugin data folder for players.yml");
        }

        dataFile = new File(plugin.getDataFolder(), "players.yml");
        if (!dataFile.exists()) {
            try {
                if (!dataFile.createNewFile()) {
                    plugin.getLogger().warning("Failed to create players.yml");
                }
            } catch (IOException ex) {
                plugin.getLogger().warning("Failed to create players.yml: " + ex.getMessage());
            }
        }

        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        cache.clear();

        ConfigurationSection playersSection = dataConfig.getConfigurationSection("players");
        if (playersSection == null) {
            return;
        }

        for (String uuidKey : playersSection.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidKey);
                ConfigurationSection playerSection = playersSection.getConfigurationSection(uuidKey);
                if (playerSection == null) {
                    continue;
                }

                Map<String, Object> values = new LinkedHashMap<>();
                for (String key : playerSection.getKeys(false)) {
                    values.put(plugin.getConfigManager().normalizeKey(key), playerSection.get(key));
                }
                cache.put(uuid, values);
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("Ignoring invalid UUID entry in players.yml: " + uuidKey);
            }
        }
    }

    public void reload() {
        load();
    }

    public Object getValue(UUID uuid, PlaceholderDefinition definition) {
        Map<String, Object> values = cache.get(uuid);
        if (values == null) {
            return definition.defaultValue();
        }
        return values.getOrDefault(definition.key(), definition.defaultValue());
    }

    public void setValue(UUID uuid, String key, Object value) {
        Map<String, Object> values = cache.computeIfAbsent(uuid, ignored -> new LinkedHashMap<>());
        values.put(plugin.getConfigManager().normalizeKey(key), value);
    }

    public void resetValue(UUID uuid, String key) {
        Map<String, Object> values = cache.get(uuid);
        if (values == null) {
            return;
        }

        values.remove(plugin.getConfigManager().normalizeKey(key));
        if (values.isEmpty()) {
            cache.remove(uuid);
        }
    }

    public void save() {
        dataConfig.set("players", null);
        for (Map.Entry<UUID, Map<String, Object>> entry : cache.entrySet()) {
            String basePath = "players." + entry.getKey();
            for (Map.Entry<String, Object> valueEntry : entry.getValue().entrySet()) {
                dataConfig.set(basePath + "." + valueEntry.getKey(), valueEntry.getValue());
            }
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException ex) {
            plugin.getLogger().warning("Failed to save players.yml: " + ex.getMessage());
        }
    }
}
