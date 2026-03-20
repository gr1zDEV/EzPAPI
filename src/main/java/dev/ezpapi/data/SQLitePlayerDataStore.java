package dev.ezpapi.data;

import dev.ezpapi.EZPAPIPlugin;
import dev.ezpapi.model.PlaceholderDefinition;
import dev.ezpapi.model.ValueParseResult;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

public final class SQLitePlayerDataStore {
    private static final String DATABASE_FILE_NAME = "ezpapi.db";
    private static final String CREATE_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS player_variables (
                player_uuid TEXT NOT NULL,
                variable_key TEXT NOT NULL,
                value TEXT NOT NULL,
                updated_at INTEGER NOT NULL,
                PRIMARY KEY (player_uuid, variable_key)
            )
            """;
    private static final String SELECT_VALUE_SQL = "SELECT value FROM player_variables WHERE player_uuid = ? AND variable_key = ?";
    private static final String UPSERT_VALUE_SQL = """
            INSERT INTO player_variables (player_uuid, variable_key, value, updated_at)
            VALUES (?, ?, ?, ?)
            ON CONFLICT(player_uuid, variable_key) DO UPDATE SET
                value = excluded.value,
                updated_at = excluded.updated_at
            """;
    private static final String DELETE_VALUE_SQL = "DELETE FROM player_variables WHERE player_uuid = ? AND variable_key = ?";

    private final EZPAPIPlugin plugin;
    private final Path databasePath;
    private final Path legacyYamlPath;
    private final Path migratedYamlPath;
    private Connection connection;

    public SQLitePlayerDataStore(EZPAPIPlugin plugin) {
        this.plugin = plugin;
        this.databasePath = plugin.getDataFolder().toPath().resolve(DATABASE_FILE_NAME);
        this.legacyYamlPath = plugin.getDataFolder().toPath().resolve("players.yml");
        this.migratedYamlPath = plugin.getDataFolder().toPath().resolve("players.yml.migrated");
    }

    public synchronized void initialize() {
        ensureDataFolder();
        openConnection();
        createTables();
        migrateLegacyYamlIfNeeded();
    }

    public synchronized void reload() {
        initialize();
    }

    public synchronized Object getValue(UUID uuid, PlaceholderDefinition definition) {
        String storedValue = getStoredValue(uuid, definition.key());
        if (storedValue == null) {
            return definition.defaultValue();
        }

        ValueParseResult parseResult = plugin.getConfigManager().parseValue(definition, storedValue);
        if (!parseResult.success()) {
            plugin.getLogger().warning("Invalid stored SQLite value for player " + uuid + " and key '" + definition.key() + "'; falling back to config default.");
            return definition.defaultValue();
        }
        return parseResult.value();
    }

    public synchronized void setValue(UUID uuid, String key, Object value) {
        String normalizedKey = plugin.getConfigManager().normalizeKey(key);
        try (PreparedStatement statement = requireConnection().prepareStatement(UPSERT_VALUE_SQL)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, normalizedKey);
            statement.setString(3, String.valueOf(value));
            statement.setLong(4, System.currentTimeMillis());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to store SQLite value for " + uuid + " / " + normalizedKey, ex);
        }
    }

    public synchronized void resetValue(UUID uuid, String key) {
        String normalizedKey = plugin.getConfigManager().normalizeKey(key);
        try (PreparedStatement statement = requireConnection().prepareStatement(DELETE_VALUE_SQL)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, normalizedKey);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to reset SQLite value for " + uuid + " / " + normalizedKey, ex);
        }
    }

    public synchronized void close() {
        if (connection == null) {
            return;
        }

        try {
            connection.close();
        } catch (SQLException ex) {
            plugin.getLogger().warning("Failed to close SQLite connection: " + ex.getMessage());
        } finally {
            connection = null;
        }
    }

    public String getDatabaseFileName() {
        return DATABASE_FILE_NAME;
    }

    private String getStoredValue(UUID uuid, String key) {
        try (PreparedStatement statement = requireConnection().prepareStatement(SELECT_VALUE_SQL)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, key);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString("value");
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to read SQLite value for " + uuid + " / " + key, ex);
        }
        return null;
    }

    private Connection requireConnection() {
        if (connection == null) {
            initialize();
        }
        return connection;
    }

    private void ensureDataFolder() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            throw new IllegalStateException("Could not create plugin data folder at " + dataFolder.getAbsolutePath());
        }
    }

    private void openConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                return;
            }

            connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath.toAbsolutePath());
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON");
                statement.execute("PRAGMA busy_timeout = 5000");
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to open SQLite database at " + databasePath.toAbsolutePath(), ex);
        }
    }

    private void createTables() {
        try (Statement statement = requireConnection().createStatement()) {
            statement.executeUpdate(CREATE_TABLE_SQL);
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to create SQLite tables for EZPAPI", ex);
        }
    }

    private void migrateLegacyYamlIfNeeded() {
        if (!Files.exists(legacyYamlPath) || Files.exists(migratedYamlPath)) {
            return;
        }

        YamlConfiguration legacyConfig = YamlConfiguration.loadConfiguration(legacyYamlPath.toFile());
        ConfigurationSection playersSection = legacyConfig.getConfigurationSection("players");
        if (playersSection == null) {
            renameLegacyYaml(0, 0);
            return;
        }

        int migratedValues = 0;
        int skippedValues = 0;

        try {
            Connection activeConnection = requireConnection();
            boolean previousAutoCommit = activeConnection.getAutoCommit();
            activeConnection.setAutoCommit(false);

            try {
                for (String uuidKey : playersSection.getKeys(false)) {
                    UUID uuid;
                    try {
                        uuid = UUID.fromString(uuidKey);
                    } catch (IllegalArgumentException ex) {
                        skippedValues++;
                        plugin.getLogger().warning("Skipping legacy YAML data for invalid UUID '" + uuidKey + "'.");
                        continue;
                    }

                    ConfigurationSection playerSection = playersSection.getConfigurationSection(uuidKey);
                    if (playerSection == null) {
                        continue;
                    }

                    for (String rawKey : playerSection.getKeys(false)) {
                        String normalizedKey = plugin.getConfigManager().normalizeKey(rawKey);
                        PlaceholderDefinition definition = plugin.getConfigManager().getDefinition(normalizedKey);
                        if (definition == null) {
                            skippedValues++;
                            plugin.getLogger().warning("Skipping legacy YAML value for unknown key '" + rawKey + "' while migrating player " + uuid + ".");
                            continue;
                        }

                        Object rawValue = playerSection.get(rawKey);
                        ValueParseResult parseResult = plugin.getConfigManager().parseValue(definition, rawValue == null ? "" : String.valueOf(rawValue));
                        if (!parseResult.success()) {
                            skippedValues++;
                            plugin.getLogger().warning("Skipping legacy YAML value for player " + uuid + " and key '" + normalizedKey + "' because it is invalid for type " + definition.type().name().toLowerCase(java.util.Locale.ROOT) + ".");
                            continue;
                        }

                        setValue(uuid, normalizedKey, parseResult.value());
                        migratedValues++;
                    }
                }

                activeConnection.commit();
            } catch (Exception ex) {
                activeConnection.rollback();
                throw ex;
            } finally {
                activeConnection.setAutoCommit(previousAutoCommit);
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to migrate legacy YAML player data into SQLite", ex);
        }

        renameLegacyYaml(migratedValues, skippedValues);
    }

    private void renameLegacyYaml(int migratedValues, int skippedValues) {
        try {
            Files.move(legacyYamlPath, migratedYamlPath, StandardCopyOption.REPLACE_EXISTING);
            plugin.getLogger().info("Migrated legacy player data from players.yml to " + DATABASE_FILE_NAME + " (migrated " + migratedValues + " values, skipped " + skippedValues + "). Renamed old file to players.yml.migrated.");
        } catch (IOException ex) {
            throw new IllegalStateException("Migrated legacy YAML values into SQLite but failed to rename players.yml to players.yml.migrated", ex);
        }
    }
}
