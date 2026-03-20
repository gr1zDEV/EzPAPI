package dev.ezpapi.command;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

public final class PlayerResolver {
    private PlayerResolver() {
    }

    public static OfflinePlayer resolve(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }

        try {
            UUID uuid = UUID.fromString(input);
            OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
            return isResolvable(player) ? player : null;
        } catch (IllegalArgumentException ignored) {
            OfflinePlayer onlineFirst = Bukkit.getPlayerExact(input);
            if (onlineFirst != null) {
                return onlineFirst;
            }

            OfflinePlayer cached = Bukkit.getOfflinePlayerIfCached(input);
            if (cached != null) {
                return cached;
            }

            OfflinePlayer player = Bukkit.getOfflinePlayer(input);
            return player.hasPlayedBefore() ? player : null;
        }
    }

    public static boolean isResolvable(OfflinePlayer player) {
        return player != null && (player.hasPlayedBefore() || player.isOnline() || player.getName() != null);
    }
}
