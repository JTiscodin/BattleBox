package plugins.battlebox.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import plugins.battlebox.core.GameService;
import plugins.battlebox.core.MusicService;
import plugins.battlebox.managers.ArenaManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Main command handler for BattleBox plugin.
 * Handles /battlebox commands with clean subcommand structure.
 */
public class BattleBoxCommand implements CommandExecutor, TabCompleter {

    private final GameService gameService;
    private final ArenaManager arenaManager;
    private final MusicService musicService;

    public BattleBoxCommand(GameService gameService, ArenaManager arenaManager, MusicService musicService) {
        this.gameService = gameService;
        this.arenaManager = arenaManager;
        this.musicService = musicService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use BattleBox commands!");
            return true;
        }

        if (!player.hasPermission("battlebox.use")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use BattleBox!");
            return true;
        }

        if (args.length == 0) {
            showHelp(player);
            return true;
        }
        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "create" -> handleCreate(player, args);
            case "join" -> handleJoin(player, args);
            case "leave" -> handleLeave(player);
            case "list" -> handleList(player);
            case "info" -> handleInfo(player, args);
            case "music" -> handleMusic(player, args);
            default -> showHelp(player);
        }

        return true;
    }

    private void handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /battlebox create <arena-name>");
            return;
        }

        String arenaName = args[1];
        if (gameService.createGame(player, arenaName)) {
            // Success message is handled in GameService
        }
    }

    private void handleJoin(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /battlebox join <arena-name>");
            return;
        }

        String arenaName = args[1];
        // Try to join existing game first, create new one if none exists
        if (!gameService.joinOrCreateGame(player, arenaName)) {
            player.sendMessage(ChatColor.RED + "Failed to join/create game for arena: " + arenaName);
        }
    }

    private void handleLeave(Player player) {
        if (gameService.leaveGame(player)) {
            player.sendMessage(ChatColor.YELLOW + "You left the game.");
        } else {
            player.sendMessage(ChatColor.RED + "You are not in a game!");
        }
    }

    private void handleList(Player player) {
        List<String> arenas = arenaManager.getArenas().stream()
                .map(arena -> arena.id)
                .toList();

        if (arenas.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "No arenas available. Create one with /arena create <name>");
            return;
        }

        player.sendMessage(ChatColor.GOLD + "Available Arenas:");
        for (String arena : arenas) {
            player.sendMessage(ChatColor.AQUA + "- " + arena);
        }
    }

    private void handleInfo(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /battlebox info <arena-name>");
            return;
        }

        String arenaName = args[1];
        var arena = arenaManager.getArena(arenaName);

        if (arena == null) {
            player.sendMessage(ChatColor.RED + "Arena '" + arenaName + "' not found!");
            return;
        }

        player.sendMessage(ChatColor.GOLD + "=== Arena Info: " + arenaName + " ===");
        player.sendMessage(ChatColor.AQUA + "World: " + ChatColor.WHITE + arena.world);

        // Show completion status
        boolean hasSpawns = arena.teamSpawns != null &&
                arena.teamSpawns.redSpawn != null &&
                arena.teamSpawns.blueSpawn != null;
        boolean hasTeleports = arena.teamSpawns != null &&
                arena.teamSpawns.redTeleport != null &&
                arena.teamSpawns.blueTeleport != null;
        boolean hasCenter = arena.centerBox != null;

        player.sendMessage((hasSpawns ? ChatColor.GREEN + "✓" : ChatColor.RED + "✗") +
                ChatColor.WHITE + " Team spawns");
        player.sendMessage((hasTeleports ? ChatColor.GREEN + "✓" : ChatColor.RED + "✗") +
                ChatColor.WHITE + " Teleport positions");
        player.sendMessage((hasCenter ? ChatColor.GREEN + "✓" : ChatColor.RED + "✗") +
                ChatColor.WHITE + " Center area");

        boolean isComplete = hasSpawns && hasTeleports && hasCenter;
        player.sendMessage(ChatColor.YELLOW + "Status: " +
                (isComplete ? ChatColor.GREEN + "Complete" : ChatColor.RED + "Incomplete"));
    }

    private void showHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== BattleBox Commands ===");
        player.sendMessage(ChatColor.AQUA + "/battlebox create <arena>" + ChatColor.WHITE + " - Start a new game");
        player.sendMessage(ChatColor.AQUA + "/battlebox join <arena>" + ChatColor.WHITE + " - Join a game");
        player.sendMessage(ChatColor.AQUA + "/battlebox leave" + ChatColor.WHITE + " - Leave current game");
        player.sendMessage(ChatColor.AQUA + "/battlebox list" + ChatColor.WHITE + " - List available arenas");
        player.sendMessage(ChatColor.AQUA + "/battlebox info <arena>" + ChatColor.WHITE + " - Show arena details");
        player.sendMessage(ChatColor.AQUA + "/battlebox music" + ChatColor.WHITE + " - Music and sound settings");
        player.sendMessage(ChatColor.GRAY + "Use /arena commands to create/manage arenas");
    }

    private void handleMusic(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.YELLOW + "=== Music Commands ===");
            player.sendMessage(ChatColor.AQUA + "/battlebox music toggle" + ChatColor.WHITE + " - Toggle music on/off");
            player.sendMessage(
                    ChatColor.AQUA + "/battlebox music sounds" + ChatColor.WHITE + " - Toggle sound effects on/off");
            player.sendMessage(
                    ChatColor.AQUA + "/battlebox music volume <0.0-1.0>" + ChatColor.WHITE + " - Set music volume");
            player.sendMessage(ChatColor.AQUA + "/battlebox music info" + ChatColor.WHITE + " - Show current settings");
            return;
        }

        switch (args[1].toLowerCase()) {
            case "toggle" -> {
                boolean enabled = musicService.togglePlayerMusic(player);
                player.sendMessage(ChatColor.YELLOW + "Music " +
                        (enabled ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled"));
            }
            case "sounds" -> {
                boolean enabled = musicService.togglePlayerSoundEffects(player);
                player.sendMessage(ChatColor.YELLOW + "Sound effects " +
                        (enabled ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled"));
            }
            case "volume" -> {
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Usage: /battlebox music volume <0.0-1.0>");
                    return;
                }
                try {
                    float volume = Float.parseFloat(args[2]);
                    MusicService.MusicPreference pref = musicService.getPlayerPreference(player);
                    pref.setMusicVolume(volume);
                    musicService.updatePlayerPreference(player, pref);
                    player.sendMessage(ChatColor.YELLOW + "Music volume set to " + ChatColor.AQUA + volume);
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Invalid volume. Use a number between 0.0 and 1.0");
                }
            }
            case "info" -> showMusicInfo(player);
            default -> player.sendMessage(ChatColor.RED + "Unknown music command. Use '/battlebox music' for help.");
        }
    }

    private void showMusicInfo(Player player) {
        MusicService.MusicPreference pref = musicService.getPlayerPreference(player);
        player.sendMessage(ChatColor.GOLD + "=== Music Settings ===");
        player.sendMessage(ChatColor.WHITE + "Music: "
                + (pref.isMusicEnabled() ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled"));
        player.sendMessage(ChatColor.WHITE + "Sound Effects: "
                + (pref.isSoundEffectsEnabled() ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled"));
        player.sendMessage(
                ChatColor.WHITE + "Music Volume: " + ChatColor.AQUA + String.format("%.1f", pref.getMusicVolume()));
        player.sendMessage(
                ChatColor.WHITE + "Sound Volume: " + ChatColor.AQUA + String.format("%.1f", pref.getSoundVolume()));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filterStartingWith(Arrays.asList("create", "join", "leave", "list", "info", "music"), args[0]);
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("create") ||
                    args[0].equalsIgnoreCase("join") ||
                    args[0].equalsIgnoreCase("info")) {
                return filterStartingWith(arenaManager.getArenas().stream()
                        .map(arena -> arena.id).toList(), args[1]);
            }

            if (args[0].equalsIgnoreCase("music")) {
                return filterStartingWith(Arrays.asList("toggle", "sounds", "volume", "info"), args[1]);
            }
        }

        return new ArrayList<>();
    }

    private List<String> filterStartingWith(List<String> list, String prefix) {
        return list.stream()
                .filter(s -> s.toLowerCase().startsWith(prefix.toLowerCase()))
                .toList();
    }
}
