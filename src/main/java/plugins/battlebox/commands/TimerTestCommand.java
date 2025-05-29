package plugins.battlebox.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import plugins.battlebox.BattleBox;
import plugins.battlebox.game.Game;
import plugins.battlebox.game.GameManager;
import plugins.battlebox.managers.TimerManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Command to test timer functionality
 * Usage: /timertest <subcommand> [args...]
 */
public class TimerTestCommand implements CommandExecutor, TabCompleter {
    
    private final BattleBox plugin;
    private final TimerManager timerManager;
    private final GameManager gameManager;
    
    public TimerTestCommand(BattleBox plugin) {
        this.plugin = plugin;
        this.timerManager = plugin.getTimerManager();
        this.gameManager = plugin.getGameManager();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        plugin.getLogger().info("TimerTestCommand executed with args: " + Arrays.toString(args));
        
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("battlebox.timertest.admin")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use timer test commands!");
            return true;
        }
        
        if (args.length == 0) {
            sendHelpMessage(player);
            return true;
        }
        
        String subcommand = args[0].toLowerCase();
        
        switch (subcommand) {
            case "start":
                handleStartCommand(player, args);
                break;
            case "global":
                handleGlobalCommand(player, args);
                break;
            case "game":
                handleGameCommand(player, args);
                break;
            case "stop":
                handleStopCommand(player, args);
                break;
            case "stopall":
                handleStopAllCommand(player);
                break;
            case "status":
                handleStatusCommand(player);
                break;
            case "remaining":
                handleRemainingCommand(player, args);
                break;
            case "help":
                sendHelpMessage(player);
                break;
            default:
                player.sendMessage(ChatColor.RED + "Unknown subcommand: " + subcommand);
                sendHelpMessage(player);
                break;
        }
        
        return true;
    }
    
    private void handleStartCommand(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: /timertest start <seconds> [title]");
            return;
        }
        
        try {
            int seconds = Integer.parseInt(args[1]);
            if (seconds <= 0) {
                player.sendMessage(ChatColor.RED + "Duration must be positive!");
                return;
            }
            
            String title = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : null;
            String timerId = "test_" + player.getName();
            
            Runnable onComplete = () -> {
                player.sendMessage(ChatColor.GREEN + "Timer '" + timerId + "' completed!");
                plugin.getLogger().info("Timer " + timerId + " completed for player " + player.getName());
            };
            
            timerManager.startTimer(timerId, Set.of(player), seconds, title, onComplete);
            player.sendMessage(ChatColor.GREEN + "Started timer '" + timerId + "' for " + seconds + " seconds" + 
                              (title != null ? " with title: " + title : ""));
                              
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid number: " + args[1]);
        }
    }
    
    private void handleGlobalCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /timertest global <seconds> [title]");
            return;
        }
        
        try {
            int seconds = Integer.parseInt(args[1]);
            if (seconds <= 0) {
                player.sendMessage(ChatColor.RED + "Duration must be positive!");
                return;
            }
            
            String title = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : null;
            String timerId = "global_" + System.currentTimeMillis();
            
            Runnable onComplete = () -> {
                Bukkit.broadcastMessage(ChatColor.GREEN + "Global timer completed!");
                plugin.getLogger().info("Global timer " + timerId + " completed");
            };
            
            timerManager.startGlobalTimer(timerId, seconds, title, onComplete);
            player.sendMessage(ChatColor.GREEN + "Started global timer for " + seconds + " seconds for " + 
                              Bukkit.getOnlinePlayers().size() + " players");
                              
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid number: " + args[1]);
        }
    }
    
    private void handleGameCommand(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: /timertest game <gameId> <seconds> [title]");
            return;
        }
        
        String gameId = args[1];
        Game game = gameManager.getGame(gameId);
        
        if (game == null) {
            player.sendMessage(ChatColor.RED + "Game not found: " + gameId);
            return;
        }
        
        try {
            int seconds = Integer.parseInt(args[2]);
            if (seconds <= 0) {
                player.sendMessage(ChatColor.RED + "Duration must be positive!");
                return;
            }
            
            String title = args.length > 3 ? String.join(" ", Arrays.copyOfRange(args, 3, args.length)) : null;
            
            Runnable onComplete = () -> {
                for (Player gamePlayer : game.getPlayers()) {
                    gamePlayer.sendMessage(ChatColor.GREEN + "Game timer completed!");
                }
                plugin.getLogger().info("Game timer completed for game " + gameId);
            };
            
            timerManager.startGameTimer(game, seconds, title, onComplete);
            player.sendMessage(ChatColor.GREEN + "Started game timer for game '" + gameId + "' (" + 
                              game.getPlayers().size() + " players) for " + seconds + " seconds");
                              
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid number: " + args[2]);
        }
    }
    
    private void handleStopCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /timertest stop <timerId>");
            return;
        }
        
        String timerId = args[1];
        
        if (!timerManager.isTimerRunning(timerId)) {
            player.sendMessage(ChatColor.RED + "Timer '" + timerId + "' is not running!");
            return;
        }
        
        timerManager.stopTimer(timerId);
        player.sendMessage(ChatColor.GREEN + "Stopped timer: " + timerId);
    }
    
    private void handleStopAllCommand(Player player) {
        int count = timerManager.getActiveTimerCount();
        timerManager.stopAllTimers();
        player.sendMessage(ChatColor.GREEN + "Stopped " + count + " active timers");
    }
    
    private void handleStatusCommand(Player player) {
        int activeCount = timerManager.getActiveTimerCount();
        player.sendMessage(ChatColor.GOLD + "=== Timer Status ===");
        player.sendMessage(ChatColor.YELLOW + "Active timers: " + ChatColor.WHITE + activeCount);
        player.sendMessage(ChatColor.YELLOW + "Online players: " + ChatColor.WHITE + Bukkit.getOnlinePlayers().size());
    }
    
    private void handleRemainingCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /timertest remaining <timerId>");
            return;
        }
        
        String timerId = args[1];
        int remaining = timerManager.getRemainingTime(timerId);
        
        if (remaining == -1) {
            player.sendMessage(ChatColor.RED + "Timer '" + timerId + "' is not running!");
        } else {
            player.sendMessage(ChatColor.GREEN + "Timer '" + timerId + "' has " + remaining + " seconds remaining");
        }
    }
    
    private void sendHelpMessage(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Timer Test Commands ===");
        player.sendMessage(ChatColor.YELLOW + "/timertest start <seconds> [title]" + ChatColor.WHITE + " - Start personal timer");
        player.sendMessage(ChatColor.YELLOW + "/timertest global <seconds> [title]" + ChatColor.WHITE + " - Start global timer");
        player.sendMessage(ChatColor.YELLOW + "/timertest game <gameId> <seconds> [title]" + ChatColor.WHITE + " - Start game timer");
        player.sendMessage(ChatColor.YELLOW + "/timertest stop <timerId>" + ChatColor.WHITE + " - Stop specific timer");
        player.sendMessage(ChatColor.YELLOW + "/timertest stopall" + ChatColor.WHITE + " - Stop all timers");
        player.sendMessage(ChatColor.YELLOW + "/timertest status" + ChatColor.WHITE + " - Show timer status");
        player.sendMessage(ChatColor.YELLOW + "/timertest remaining <timerId>" + ChatColor.WHITE + " - Check remaining time");
        player.sendMessage(ChatColor.YELLOW + "/timertest help" + ChatColor.WHITE + " - Show this help");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        plugin.getLogger().info("TimerTestCommand tab completion with args: " + Arrays.toString(args));
        
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // Subcommands
            List<String> subcommands = Arrays.asList("start", "global", "game", "stop", "stopall", "status", "remaining", "help");
            String input = args[0].toLowerCase();
            
            for (String subcommand : subcommands) {
                if (subcommand.startsWith(input)) {
                    completions.add(subcommand);
                }
            }
        } else if (args.length == 2) {
            String subcommand = args[0].toLowerCase();
            
            if ("start".equals(subcommand) || "global".equals(subcommand)) {
                // Suggest common durations
                completions.addAll(Arrays.asList("5", "10", "30", "60"));            } else if ("game".equals(subcommand)) {
                // Suggest game IDs
                for (String gameId : gameManager.getActiveGameIds()) {
                    completions.add(gameId);
                }
            } else if ("stop".equals(subcommand) || "remaining".equals(subcommand)) {
                // Suggest timer IDs (basic ones)
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    completions.add("test_" + player.getName());
                    completions.add("global_timer");
                }
            }
        } else if (args.length == 3) {
            String subcommand = args[0].toLowerCase();
            
            if ("game".equals(subcommand)) {
                // Suggest durations for game timers
                completions.addAll(Arrays.asList("5", "10", "30", "60"));
            } else if ("start".equals(subcommand) || "global".equals(subcommand)) {
                // Suggest title prefixes
                completions.addAll(Arrays.asList("GAME", "STARTING", "COUNTDOWN", "PREPARE"));
            }
        } else if (args.length == 4 && "game".equals(args[0].toLowerCase())) {
            // Suggest title prefixes for game timers
            completions.addAll(Arrays.asList("GAME", "STARTING", "COUNTDOWN", "PREPARE"));
        }
        
        return completions;
    }
}
