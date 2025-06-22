package plugins.battlebox.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import fr.mrmicky.fastboard.FastBoard;
import plugins.battlebox.game.Game;
import plugins.battlebox.game.GameManager;
import plugins.battlebox.game.GameState;

public class ScoreboardManager {
    private final GameManager gameManager;
    private final Map<Player, FastBoard> playerScoreboards;
    private final Map<Player, TimerInfo> playerTimers;

    public ScoreboardManager(JavaPlugin plugin, GameManager gameManager) {
        this.gameManager = gameManager;
        this.playerScoreboards = new HashMap<>();
        this.playerTimers = new HashMap<>();
    }

    // Timer information storage class
    private static class TimerInfo {
        public final String title;
        public final String timeDisplay;
        public final ChatColor color;

        public TimerInfo(String title, String timeDisplay, ChatColor color) {
            this.title = title;
            this.timeDisplay = timeDisplay;
            this.color = color;
        }
    }

    public void createScoreboard(Player player) {
        FastBoard board = new FastBoard(player);
        board.updateTitle(ChatColor.GOLD + "" + ChatColor.BOLD + "BattleBox");

        playerScoreboards.put(player, board);
        updateScoreboard(player);
    }

    public void updateScoreboard(Player player) {
        FastBoard board = playerScoreboards.get(player);
        if (board == null)
            return;

        List<String> lines = new ArrayList<>();

        // Check if player has active timer
        TimerInfo timerInfo = playerTimers.get(player);
        if (timerInfo != null) {
            // Timer section at top
            lines.add(timerInfo.color + "" + ChatColor.BOLD + timerInfo.title);
            lines.add(timerInfo.color + "" + ChatColor.BOLD + timerInfo.timeDisplay);
            lines.add("");
        }

        // Empty line (if no timer)
        if (timerInfo == null) {
            lines.add("");
        }

        // Player info
        lines.add(ChatColor.AQUA + "Player: " + ChatColor.WHITE + player.getName());

        // Empty line
        lines.add("");

        // Game status
        Game currentGame = gameManager.getPlayerGame(player);
        if (currentGame != null) {
            lines.add(ChatColor.GREEN + "Game: " + ChatColor.WHITE + currentGame.getId());
            lines.add(ChatColor.GREEN + "Arena: " + ChatColor.WHITE + currentGame.getArenaId());
            lines.add(ChatColor.GREEN + "Status: " + ChatColor.WHITE + getGameStateDisplay(currentGame.getState()));

            // Show team info if in timer mode, otherwise show player count
            Game.TeamColor playerTeam = currentGame.getPlayerTeam(player);
            if (timerInfo != null && playerTeam != null) {
                lines.add(ChatColor.GREEN + "Team: " + playerTeam.chatColor + playerTeam.displayName);
            } else {
                lines.add(ChatColor.GREEN + "Players: " + ChatColor.WHITE + currentGame.getPlayerCount() + "/8");
            }
        } else {
            lines.add(ChatColor.RED + "Status: " + ChatColor.WHITE + "Not in game");
            if (timerInfo == null) {
                lines.add(ChatColor.YELLOW + "Use /join to find a game!");
            }
        }

        // Empty line
        lines.add("");

        // Server info (only show if no timer)
        if (timerInfo == null) {
            lines.add(ChatColor.YELLOW + "Online: " + ChatColor.WHITE + Bukkit.getOnlinePlayers().size());
            lines.add(ChatColor.YELLOW + "Games: " + ChatColor.WHITE + gameManager.getActiveGameCount());
            lines.add("");
        }

        // Footer
        lines.add(ChatColor.GOLD + "play.battlebox.com");

        board.updateLines(lines);
    }

    /**
     * Update scoreboard with timer information
     */
    public void updateScoreboardWithTimer(Player player, String timerTitle, String timeDisplay, ChatColor color) {
        FastBoard board = playerScoreboards.get(player);
        if (board == null)
            return;

        List<String> lines = new ArrayList<>();

        // Timer section at top
        lines.add(color + "" + ChatColor.BOLD + timerTitle);
        lines.add(color + "" + ChatColor.BOLD + timeDisplay);

        // Empty line
        lines.add("");

        // Player info
        lines.add(ChatColor.AQUA + "Player: " + ChatColor.WHITE + player.getName());

        // Empty line
        lines.add("");

        // Game status
        Game currentGame = gameManager.getPlayerGame(player);
        if (currentGame != null) {
            lines.add(ChatColor.GREEN + "Game: " + ChatColor.WHITE + currentGame.getId());
            lines.add(ChatColor.GREEN + "Arena: " + ChatColor.WHITE + currentGame.getArenaId());
            lines.add(ChatColor.GREEN + "Status: " + ChatColor.WHITE + getGameStateDisplay(currentGame.getState()));

            // Show team info
            Game.TeamColor playerTeam = currentGame.getPlayerTeam(player);
            if (playerTeam != null) {
                lines.add(ChatColor.GREEN + "Team: " + playerTeam.chatColor + playerTeam.displayName);
            }
        } else {
            lines.add(ChatColor.RED + "Status: " + ChatColor.WHITE + "Not in game");
        }

        // Empty line
        lines.add("");

        // Footer
        lines.add(ChatColor.GOLD + "play.battlebox.com");

        board.updateLines(lines);
    }

    private String getGameStateDisplay(GameState state) {
        return switch (state) {
            case WAITING -> "Waiting";
            case KIT_SELECTION -> "Kit Selection";
            case IN_PROGRESS -> "In Progress";
            case ENDING -> "Ending";
        };
    }

    public void removeScoreboard(Player player) {
        FastBoard board = playerScoreboards.remove(player);
        if (board != null) {
            board.delete();
        }
        // Also clean up timer info
        playerTimers.remove(player);
    }

    /**
     * Set timer information for a player
     */
    public void setTimerInfo(Player player, String timerTitle, String timeDisplay, ChatColor color) {
        playerTimers.put(player, new TimerInfo(timerTitle, timeDisplay, color));
    }

    /**
     * Clear timer information for a player
     */
    public void clearTimerInfo(Player player) {
        playerTimers.remove(player);
    }

    /**
     * Clear timer information for multiple players
     */
    public void clearTimerInfo(Iterable<Player> players) {
        for (Player player : players) {
            playerTimers.remove(player);
        }
    }

    public void updateAllScoreboards() {
        for (Player player : playerScoreboards.keySet()) {
            if (player.isOnline()) {
                updateScoreboard(player);
            }
        }
    }
}
