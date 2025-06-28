package plugins.battlebox.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import plugins.battlebox.game.Game;
import plugins.battlebox.game.GameManager;
import plugins.battlebox.managers.ArenaManager;
import config.ArenaConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GameTestCommand implements CommandExecutor, TabCompleter {

    private final GameManager gameManager;
    private final ArenaManager arenaManager;

    public GameTestCommand(GameManager gameManager, ArenaManager arenaManager) {
        this.gameManager = gameManager;
        this.arenaManager = arenaManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use game test commands!");
            return true;
        }

        if (!player.hasPermission("battlebox.gametest.admin")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use game test commands!");
            return true;
        }

        if (args.length == 0) {
            sendHelpMessage(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "create" -> handleCreateGame(player, args);
            case "join" -> handleJoinGame(player, args);
            case "leave" -> handleLeaveGame(player);
            case "teams" -> handleShowTeams(player, args);
            case "winner" -> handleCheckWinner(player, args);
            case "woolcount" -> handleWoolCount(player, args);
            case "list" -> handleListGames(player);
            case "info" -> handleGameInfo(player, args);
            case "delete" -> handleDeleteGame(player, args);
            default -> sendHelpMessage(player);
        }

        return true;
    }

    private void handleCreateGame(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: /gametest create <game-name> <arena-name>");
            return;
        }

        String gameName = args[1];
        String arenaName = args[2];

        if (gameManager.getGame(gameName) != null) {
            player.sendMessage(ChatColor.RED + "Game '" + gameName + "' already exists!");
            return;
        }

        // Check if arena exists
        ArenaConfig arena = null;
        for (ArenaConfig a : arenaManager.getArenas()) {
            if (a.id.equals(arenaName)) {
                arena = a;
                break;
            }
        }

        if (arena == null) {
            player.sendMessage(ChatColor.RED + "Arena '" + arenaName + "' not found!");
            return;
        }

        Game game = new Game(gameName, arenaName);
        gameManager.createGame(gameName, game);

        player.sendMessage(ChatColor.GREEN + "Created game '" + gameName + "' in arena '" + arenaName + "'!");
    }

    private void handleJoinGame(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /gametest join <game-name>");
            return;
        }

        String gameName = args[1];
        Game game = gameManager.getGame(gameName);

        if (game == null) {
            player.sendMessage(ChatColor.RED + "Game '" + gameName + "' not found!");
            return;
        }

        if (game.addPlayer(player)) {
            player.sendMessage(ChatColor.GREEN + "Joined game '" + gameName + "'!");

            // Show team assignment
            Game.TeamColor team = game.getPlayerTeam(player);
            if (team != null) {
                player.sendMessage(ChatColor.AQUA + "You are on team: " + team.chatColor + team.displayName);
            }
        }
    }

    private void handleLeaveGame(Player player) {
        Game game = gameManager.getPlayerGame(player);

        if (game == null) {
            player.sendMessage(ChatColor.RED + "You're not in any game!");
            return;
        }

        game.removePlayer(player);
        gameManager.removePlayerFromGame(player);
        player.sendMessage(ChatColor.YELLOW + "Left the game.");
    }

    private void handleShowTeams(Player player, String[] args) {
        String gameName = args.length > 1 ? args[1] : null;
        Game game;

        if (gameName != null) {
            game = gameManager.getGame(gameName);
            if (game == null) {
                player.sendMessage(ChatColor.RED + "Game '" + gameName + "' not found!");
                return;
            }
        } else {
            game = gameManager.getPlayerGame(player);
            if (game == null) {
                player.sendMessage(
                        ChatColor.RED + "You're not in any game! Specify game name: /gametest teams <game-name>");
                return;
            }
        }

        player.sendMessage(ChatColor.GOLD + "=== Team Information ===");
        for (Game.TeamColor color : Game.TeamColor.values()) {
            var teamPlayers = game.getTeamPlayers(color);
            player.sendMessage(color.chatColor + color.displayName + " Team (" + teamPlayers.size() + " players):");

            for (var playerId : teamPlayers) {
                Player teamPlayer = player.getServer().getPlayer(playerId);
                String playerName = teamPlayer != null ? teamPlayer.getName() : "Offline";
                player.sendMessage(ChatColor.WHITE + "  - " + playerName);
            }
        }
    }

    private void handleCheckWinner(Player player, String[] args) {
        String gameName = args.length > 1 ? args[1] : null;
        Game game;

        if (gameName != null) {
            game = gameManager.getGame(gameName);
            if (game == null) {
                player.sendMessage(ChatColor.RED + "Game '" + gameName + "' not found!");
                return;
            }
        } else {
            game = gameManager.getPlayerGame(player);
            if (game == null) {
                player.sendMessage(
                        ChatColor.RED + "You're not in any game! Specify game name: /gametest winner <game-name>");
                return;
            }
        }

        // Get arena config
        ArenaConfig arena = null;
        for (ArenaConfig a : arenaManager.getArenas()) {
            if (a.id.equals(game.getArenaId())) {
                arena = a;
                break;
            }
        }

        if (arena == null) {
            player.sendMessage(ChatColor.RED + "Arena configuration not found!");
            return;
        }

        // Calculate winner
        game.calculateWinner(arena);

        player.sendMessage(ChatColor.GOLD + "=== Winner Check ===");
        if (game.hasWinner()) {
            player.sendMessage(ChatColor.GREEN + game.getWinReason());
        } else if (game.isDraw()) {
            player.sendMessage(ChatColor.YELLOW + game.getWinReason());
        } else {
            player.sendMessage(ChatColor.RED + "Unable to determine winner: " + game.getWinReason());
        }
    }

    private void handleWoolCount(Player player, String[] args) {
        String gameName = args.length > 1 ? args[1] : null;
        Game game;

        if (gameName != null) {
            game = gameManager.getGame(gameName);
            if (game == null) {
                player.sendMessage(ChatColor.RED + "Game '" + gameName + "' not found!");
                return;
            }
        } else {
            game = gameManager.getPlayerGame(player);
            if (game == null) {
                player.sendMessage(
                        ChatColor.RED + "You're not in any game! Specify game name: /gametest woolcount <game-name>");
                return;
            }
        }

        // Get arena config
        ArenaConfig arena = null;
        for (ArenaConfig a : arenaManager.getArenas()) {
            if (a.id.equals(game.getArenaId())) {
                arena = a;
                break;
            }
        }

        if (arena == null) {
            player.sendMessage(ChatColor.RED + "Arena configuration not found!");
            return;
        }

        String woolInfo = game.getWoolCountInfo(arena);
        player.sendMessage(woolInfo);
    }

    private void handleListGames(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Active Games ===");

        if (gameManager.getActiveGameCount() == 0) {
            player.sendMessage(ChatColor.YELLOW + "No active games.");
            return;
        }

        // Note: We'd need to add a method to get all games to GameManager
        player.sendMessage(ChatColor.AQUA + "Total active games: " + gameManager.getActiveGameCount());
    }

    private void handleGameInfo(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /gametest info <game-name>");
            return;
        }

        String gameName = args[1];
        Game game = gameManager.getGame(gameName);

        if (game == null) {
            player.sendMessage(ChatColor.RED + "Game '" + gameName + "' not found!");
            return;
        }

        player.sendMessage(ChatColor.GOLD + "=== Game Info: " + gameName + " ===");
        player.sendMessage(ChatColor.AQUA + "Arena: " + ChatColor.WHITE + game.getArenaId());
        player.sendMessage(ChatColor.AQUA + "State: " + ChatColor.WHITE + game.getState());
        player.sendMessage(ChatColor.AQUA + "Players: " + ChatColor.WHITE + game.getPlayerCount() + "/8");

        var teamCounts = game.getTeamCounts();
        player.sendMessage(ChatColor.AQUA + "Teams:");
        for (Game.TeamColor color : Game.TeamColor.values()) {
            player.sendMessage(ChatColor.WHITE + "  " + color.chatColor + color.displayName + ": "
                    + teamCounts.get(color) + " players");
        }
    }

    private void handleDeleteGame(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /gametest delete <game-name>");
            return;
        }

        String gameName = args[1];
        Game game = gameManager.getGame(gameName);

        if (game == null) {
            player.sendMessage(ChatColor.RED + "Game '" + gameName + "' not found!");
            return;
        }

        gameManager.removeGame(gameName);
        player.sendMessage(ChatColor.GREEN + "Deleted game '" + gameName + "'!");
    }

    private void sendHelpMessage(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== BattleBox Game Test Commands ===");
        player.sendMessage(
                ChatColor.AQUA + "/gametest create <game> <arena>" + ChatColor.WHITE + " - Create a test game");
        player.sendMessage(ChatColor.AQUA + "/gametest join <game>" + ChatColor.WHITE + " - Join a game");
        player.sendMessage(ChatColor.AQUA + "/gametest leave" + ChatColor.WHITE + " - Leave current game");
        player.sendMessage(ChatColor.AQUA + "/gametest teams [game]" + ChatColor.WHITE + " - Show team information");
        player.sendMessage(
                ChatColor.AQUA + "/gametest winner [game]" + ChatColor.WHITE + " - Check who wins based on wool");
        player.sendMessage(
                ChatColor.AQUA + "/gametest woolcount [game]" + ChatColor.WHITE + " - Show wool count in arena box");
        player.sendMessage(ChatColor.AQUA + "/gametest list" + ChatColor.WHITE + " - List active games");
        player.sendMessage(ChatColor.AQUA + "/gametest info <game>" + ChatColor.WHITE + " - Show game information");
        player.sendMessage(ChatColor.AQUA + "/gametest delete <game>" + ChatColor.WHITE + " - Delete a game");

        player.sendMessage(ChatColor.YELLOW + "\nTo test winner logic:");
        player.sendMessage(ChatColor.WHITE + "1. Create a game in an arena");
        player.sendMessage(ChatColor.WHITE + "2. Join with multiple players");
        player.sendMessage(ChatColor.WHITE + "3. Place colored wool in the arena box");
        player.sendMessage(ChatColor.WHITE + "4. Use /gametest winner to see results");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("create", "join", "leave", "teams", "winner", "woolcount", "list",
                    "info", "delete");
            String partial = args[0].toLowerCase();

            for (String subCommand : subCommands) {
                if (subCommand.startsWith(partial)) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();

            if (subCommand.equals("join") || subCommand.equals("teams") || subCommand.equals("winner") ||
                    subCommand.equals("woolcount") || subCommand.equals("info") || subCommand.equals("delete")) {
                // Tab complete with game names (we'd need to add a method to get all game
                // names)
                // For now, just return empty
            }
        }

        return completions;
    }
}
