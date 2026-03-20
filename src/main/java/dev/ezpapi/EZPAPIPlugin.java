package dev.ezpapi;

import dev.ezpapi.command.EZPAPICommand;
import dev.ezpapi.config.ConfigManager;
import dev.ezpapi.config.MessagesManager;
import dev.ezpapi.data.PlayerDataManager;
import dev.ezpapi.integration.EZPAPIPlaceholderExpansion;
import dev.ezpapi.integration.FloodgateHook;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class EZPAPIPlugin extends JavaPlugin {
    private ConfigManager configManager;
    private MessagesManager messagesManager;
    private PlayerDataManager playerDataManager;
    private FloodgateHook floodgateHook;
    private EZPAPIPlaceholderExpansion placeholderExpansion;

    @Override
    public void onEnable() {
        this.configManager = new ConfigManager(this);
        this.messagesManager = new MessagesManager(this);
        this.playerDataManager = new PlayerDataManager(this);
        this.floodgateHook = new FloodgateHook();

        loadPluginState();
        registerPlaceholderExpansion();
        registerCommand();

        if (floodgateHook.isAvailable()) {
            getLogger().info("Floodgate detected; Bedrock-aware placeholder support enabled.");
        }
    }

    @Override
    public void onDisable() {
        if (playerDataManager != null) {
            playerDataManager.close();
        }

        if (placeholderExpansion != null) {
            placeholderExpansion.unregister();
            placeholderExpansion = null;
        }
    }

    public void reloadPlugin() {
        loadPluginState();
        registerPlaceholderExpansion();
    }

    private void loadPluginState() {
        configManager.load();
        messagesManager.reload();
        playerDataManager.reload();
    }

    private void registerCommand() {
        PluginCommand command = getCommand("ezpapi");
        if (command == null) {
            throw new IllegalStateException("ezpapi command is missing from plugin.yml");
        }

        EZPAPICommand executor = new EZPAPICommand(this);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    private void registerPlaceholderExpansion() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            getLogger().warning("PlaceholderAPI not found; EZPAPI placeholders will remain unavailable until it is installed.");
            return;
        }

        if (placeholderExpansion == null) {
            placeholderExpansion = new EZPAPIPlaceholderExpansion(this);
        }

        if (placeholderExpansion.isRegistered()) {
            return;
        }

        if (placeholderExpansion.register()) {
            getLogger().info("Registered EZPAPI PlaceholderAPI expansion.");
        } else {
            getLogger().warning("Failed to register EZPAPI PlaceholderAPI expansion.");
        }
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessagesManager getMessagesManager() {
        return messagesManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public FloodgateHook getFloodgateHook() {
        return floodgateHook;
    }
}
