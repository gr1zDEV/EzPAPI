package dev.ezpapi.data;

import dev.ezpapi.EZPAPIPlugin;
import dev.ezpapi.model.PlaceholderDefinition;

import java.util.UUID;

public final class PlayerDataManager {
    private final SQLitePlayerDataStore dataStore;

    public PlayerDataManager(EZPAPIPlugin plugin) {
        this.dataStore = new SQLitePlayerDataStore(plugin);
    }

    public void load() {
        dataStore.initialize();
    }

    public void reload() {
        dataStore.reload();
    }

    public Object getValue(UUID uuid, PlaceholderDefinition definition) {
        return dataStore.getValue(uuid, definition);
    }

    public void setValue(UUID uuid, String key, Object value) {
        dataStore.setValue(uuid, key, value);
    }

    public void resetValue(UUID uuid, String key) {
        dataStore.resetValue(uuid, key);
    }

    public void close() {
        dataStore.close();
    }

    public String getDatabaseFileName() {
        return dataStore.getDatabaseFileName();
    }
}
