package dev.ezpapi.config;

import dev.ezpapi.EZPAPIPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public final class MessagesManager {
    private final EZPAPIPlugin plugin;
    private File messagesFile;
    private FileConfiguration messagesConfig;
    private String prefix = "";

    public MessagesManager(EZPAPIPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Could not create plugin data folder for messages.yml");
        }

        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        prefix = colorize(messagesConfig.getString("prefix", ""));
    }

    public void reload() {
        load();
    }

    public void save() {
        if (messagesConfig == null || messagesFile == null) {
            return;
        }

        try {
            messagesConfig.save(messagesFile);
        } catch (IOException ex) {
            plugin.getLogger().warning("Failed to save messages.yml: " + ex.getMessage());
        }
    }

    public String get(String key) {
        return colorize(messagesConfig.getString(key, key));
    }

    public String format(String key, Map<String, String> replacements) {
        String message = get(key);
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            message = message.replace('%' + entry.getKey() + '%', entry.getValue());
        }
        return prefix + message;
    }

    public void send(CommandSender sender, String key) {
        sender.sendMessage(prefix + get(key));
    }

    public void send(CommandSender sender, String key, Map<String, String> replacements) {
        sender.sendMessage(format(key, replacements));
    }

    public String colorize(String input) {
        return ChatColor.translateAlternateColorCodes('&', input == null ? "" : input);
    }
}
