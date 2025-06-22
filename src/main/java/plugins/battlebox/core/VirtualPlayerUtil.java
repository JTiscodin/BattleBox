package plugins.battlebox.core;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Utility class for detecting and handling virtual players to prevent
 * network-related crashes
 */
public class VirtualPlayerUtil {

    private static JavaPlugin plugin;

    public static void initialize(JavaPlugin pluginInstance) {
        plugin = pluginInstance;
    }

    /**
     * Check if a player is a virtual/fake player
     * Virtual players often have different class implementations that don't support
     * network operations
     */
    public static boolean isVirtualPlayer(Player player) {
        if (player == null) {
            return false;
        }

        String className = player.getClass().getSimpleName();
        String packageName = player.getClass().getPackage().getName();

        // Common virtual player indicators
        return className.contains("Virtual") ||
                className.contains("Fake") ||
                className.contains("NPC") ||
                packageName.contains("virtualplayers") ||
                packageName.contains("citizens") ||
                packageName.contains("znpcs");
    }

    /**
     * Safely send a message to a player, handling virtual players
     */
    public static void safeSendMessage(Player player, String message) {
        if (player == null || isVirtualPlayer(player)) {
            if (plugin != null) {
                plugin.getLogger().info("Skipping message to virtual player: " +
                        (player != null ? player.getName() : "null"));
            }
            return;
        }

        try {
            player.sendMessage(message);
        } catch (Exception e) {
            if (plugin != null) {
                plugin.getLogger()
                        .warning("Failed to send message to player " + player.getName() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Safely check if a player is online, handling virtual players
     */
    public static boolean isSafelyOnline(Player player) {
        if (player == null || isVirtualPlayer(player)) {
            return false;
        }

        try {
            return player.isOnline();
        } catch (Exception e) {
            if (plugin != null) {
                plugin.getLogger().warning(
                        "Failed to check online status for player " + player.getName() + ": " + e.getMessage());
            }
            return false;
        }
    }

    /**
     * Safely perform network operations on a player
     */
    public static boolean canPerformNetworkOperations(Player player) {
        return player != null && !isVirtualPlayer(player) && isSafelyOnline(player);
    }
}
