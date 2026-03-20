package dev.ezpapi.command;

import dev.ezpapi.EZPAPIPlugin;
import dev.ezpapi.model.PlaceholderDefinition;
import dev.ezpapi.model.PlaceholderType;
import dev.ezpapi.model.ValueParseResult;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class EZPAPICommand implements CommandExecutor, TabCompleter {
    private static final List<String> SUBCOMMANDS = List.of("set", "get", "toggle", "reset", "reload");

    private final EZPAPIPlugin plugin;

    public EZPAPICommand(EZPAPIPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            plugin.getMessagesManager().send(sender, "usage");
            return true;
        }

        String subcommand = args[0].toLowerCase(Locale.ROOT);
        return switch (subcommand) {
            case "set" -> handleSet(sender, args);
            case "get" -> handleGet(sender, args);
            case "toggle" -> handleToggle(sender, args);
            case "reset" -> handleReset(sender, args);
            case "reload" -> handleReload(sender);
            default -> {
                plugin.getMessagesManager().send(sender, "usage");
                yield true;
            }
        };
    }

    private boolean handleSet(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "ezpapi.set")) {
            return true;
        }
        if (args.length < 4) {
            plugin.getMessagesManager().send(sender, "usage-set");
            return true;
        }

        OfflinePlayer target = resolveTarget(sender, args[1]);
        if (target == null) {
            return true;
        }

        PlaceholderDefinition definition = validateKey(sender, args[2]);
        if (definition == null) {
            return true;
        }

        String valueInput = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
        ValueParseResult parseResult = plugin.getConfigManager().parseValue(definition, valueInput);
        if (!parseResult.success()) {
            plugin.getMessagesManager().send(sender, "invalid-value", Map.of(
                    "value", valueInput,
                    "key", definition.key(),
                    "type", definition.type().name().toLowerCase(Locale.ROOT)
            ));
            return true;
        }

        plugin.getPlayerDataManager().setValue(target.getUniqueId(), definition.key(), parseResult.value());
        plugin.getPlayerDataManager().save();
        plugin.getMessagesManager().send(sender, "set-success", successPlaceholders(target, definition, parseResult.value()));
        return true;
    }

    private boolean handleGet(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "ezpapi.get")) {
            return true;
        }
        if (args.length != 3) {
            plugin.getMessagesManager().send(sender, "usage-get");
            return true;
        }

        OfflinePlayer target = resolveTarget(sender, args[1]);
        if (target == null) {
            return true;
        }

        PlaceholderDefinition definition = validateKey(sender, args[2]);
        if (definition == null) {
            return true;
        }

        Object value = plugin.getPlayerDataManager().getValue(target.getUniqueId(), definition);
        Map<String, String> replacements = successPlaceholders(target, definition, value);
        replacements.put("formatted", plugin.getMessagesManager().colorize(definition.formatValue(value)));
        plugin.getMessagesManager().send(sender, "get-success", replacements);
        return true;
    }

    private boolean handleToggle(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "ezpapi.toggle")) {
            return true;
        }
        if (args.length != 3) {
            plugin.getMessagesManager().send(sender, "usage-toggle");
            return true;
        }

        OfflinePlayer target = resolveTarget(sender, args[1]);
        if (target == null) {
            return true;
        }

        PlaceholderDefinition definition = validateKey(sender, args[2]);
        if (definition == null) {
            return true;
        }
        if (definition.type() != PlaceholderType.BOOLEAN) {
            plugin.getMessagesManager().send(sender, "boolean-only-toggle");
            return true;
        }

        boolean current = (boolean) plugin.getPlayerDataManager().getValue(target.getUniqueId(), definition);
        boolean updated = !current;
        plugin.getPlayerDataManager().setValue(target.getUniqueId(), definition.key(), updated);
        plugin.getPlayerDataManager().save();
        plugin.getMessagesManager().send(sender, "toggle-success", successPlaceholders(target, definition, updated));
        return true;
    }

    private boolean handleReset(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "ezpapi.reset")) {
            return true;
        }
        if (args.length != 3) {
            plugin.getMessagesManager().send(sender, "usage-reset");
            return true;
        }

        OfflinePlayer target = resolveTarget(sender, args[1]);
        if (target == null) {
            return true;
        }

        PlaceholderDefinition definition = validateKey(sender, args[2]);
        if (definition == null) {
            return true;
        }

        plugin.getPlayerDataManager().resetValue(target.getUniqueId(), definition.key());
        plugin.getPlayerDataManager().save();
        plugin.getMessagesManager().send(sender, "reset-success", successPlaceholders(target, definition, definition.defaultValue()));
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!hasPermission(sender, "ezpapi.reload")) {
            return true;
        }

        plugin.reloadPlugin();
        plugin.getMessagesManager().send(sender, "reload-success");
        return true;
    }

    private boolean hasPermission(CommandSender sender, String permission) {
        if (!(sender instanceof org.bukkit.command.ConsoleCommandSender) && !sender.hasPermission(permission) && !sender.hasPermission("ezpapi.admin")) {
            plugin.getMessagesManager().send(sender, "no-permission");
            return false;
        }
        return true;
    }

    private OfflinePlayer resolveTarget(CommandSender sender, String input) {
        OfflinePlayer target = PlayerResolver.resolve(input);
        if (target == null) {
            plugin.getMessagesManager().send(sender, "player-not-found", Map.of("player", input));
        }
        return target;
    }

    private PlaceholderDefinition validateKey(CommandSender sender, String input) {
        PlaceholderDefinition definition = plugin.getConfigManager().getDefinition(input);
        if (definition == null) {
            plugin.getMessagesManager().send(sender, "invalid-key", Map.of("key", input));
        }
        return definition;
    }

    private Map<String, String> successPlaceholders(OfflinePlayer target, PlaceholderDefinition definition, Object value) {
        Map<String, String> replacements = new LinkedHashMap<>();
        replacements.put("player", target.getName() != null ? target.getName() : target.getUniqueId().toString());
        replacements.put("key", definition.key());
        replacements.put("value", String.valueOf(value));
        replacements.put("type", definition.type().name().toLowerCase(Locale.ROOT));
        return replacements;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return partial(args[0], SUBCOMMANDS);
        }

        String subcommand = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2 && !subcommand.equals("reload")) {
            List<String> players = new ArrayList<>();
            Bukkit.getOnlinePlayers().forEach(player -> players.add(player.getName()));
            return partial(args[1], players);
        }

        if (args.length == 3 && List.of("set", "get", "toggle", "reset").contains(subcommand)) {
            return partial(args[2], new ArrayList<>(plugin.getConfigManager().getKeys()));
        }

        if (args.length == 4 && subcommand.equals("set")) {
            PlaceholderDefinition definition = plugin.getConfigManager().getDefinition(args[2]);
            if (definition != null && definition.type() == PlaceholderType.BOOLEAN) {
                return partial(args[3], List.of("true", "false"));
            }
        }

        return Collections.emptyList();
    }

    private List<String> partial(String token, List<String> options) {
        List<String> matches = new ArrayList<>();
        StringUtil.copyPartialMatches(token, options, matches);
        Collections.sort(matches);
        return matches;
    }
}
