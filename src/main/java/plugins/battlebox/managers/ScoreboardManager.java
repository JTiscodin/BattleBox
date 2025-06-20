package plugins.battlebox.managers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.*;
import plugins.battlebox.game.Game;
import plugins.battlebox.game.GameManager;
import plugins.battlebox.game.GameState;

import java.util.HashMap;
import java.util.Map;

public class ScoreboardManager {
    private final GameManager gameManager;
    private final Map<Player, Scoreboard> playerScoreboards;

    public ScoreboardManager(JavaPlugin plugin, GameManager gameManager) {
        this.gameManager = gameManager;
        this.playerScoreboards = new HashMap<>();
    }public void createScoreboard(Player player) {
        org.bukkit.scoreboard.ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard scoreboard = manager.getNewScoreboard();        Objective objective = scoreboard.registerNewObjective("battlebox", Criteria.DUMMY, 
            ChatColor.GOLD + "" + ChatColor.BOLD + "BattleBox");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        playerScoreboards.put(player, scoreboard);
        player.setScoreboard(scoreboard);
        updateScoreboard(player);
    }

    public void updateScoreboard(Player player) {
        Scoreboard scoreboard = playerScoreboards.get(player);
        if (scoreboard == null) return;

        Objective objective = scoreboard.getObjective("battlebox");
        if (objective == null) return;

        // Clear existing scores
        for (String entry : scoreboard.getEntries()) {
            scoreboard.resetScores(entry);
        }

        // Add scoreboard content
        int score = 15;

        // Empty line
        objective.getScore(ChatColor.WHITE + "").setScore(score--);

        // Player info
        objective.getScore(ChatColor.AQUA + "Player: " + ChatColor.WHITE + player.getName()).setScore(score--);
        
        // Empty line
        objective.getScore(ChatColor.GRAY + "").setScore(score--);

        // Game status
        Game currentGame = gameManager.getPlayerGame(player);
        if (currentGame != null) {
            objective.getScore(ChatColor.GREEN + "Game: " + ChatColor.WHITE + currentGame.getId()).setScore(score--);
            objective.getScore(ChatColor.GREEN + "Arena: " + ChatColor.WHITE + currentGame.getArenaId()).setScore(score--);
            objective.getScore(ChatColor.GREEN + "Status: " + ChatColor.WHITE + getGameStateDisplay(currentGame.getState())).setScore(score--);
            objective.getScore(ChatColor.GREEN + "Players: " + ChatColor.WHITE + currentGame.getPlayerCount() + "/8").setScore(score--);
        } else {
            objective.getScore(ChatColor.RED + "Status: " + ChatColor.WHITE + "Not in game").setScore(score--);
            objective.getScore(ChatColor.YELLOW + "Use /join to find a game!").setScore(score--);
        }

        // Empty line
        objective.getScore(ChatColor.DARK_GRAY + "").setScore(score--);

        // Server info
        objective.getScore(ChatColor.YELLOW + "Online: " + ChatColor.WHITE + Bukkit.getOnlinePlayers().size()).setScore(score--);
        objective.getScore(ChatColor.YELLOW + "Games: " + ChatColor.WHITE + gameManager.getActiveGameCount()).setScore(score--);
        
        // Empty line
        objective.getScore(ChatColor.BLACK + "").setScore(score--);

        // Footer
        objective.getScore(ChatColor.GOLD + "play.battlebox.com").setScore(score--);
    }

    /**
     * Update scoreboard with timer information
     */
    public void updateScoreboardWithTimer(Player player, String timerTitle, String timeDisplay, ChatColor color) {
        Scoreboard scoreboard = playerScoreboards.get(player);
        if (scoreboard == null) return;

        Objective objective = scoreboard.getObjective("battlebox");
        if (objective == null) return;

        // Clear existing scores
        for (String entry : scoreboard.getEntries()) {
            scoreboard.resetScores(entry);
        }

        // Add scoreboard content with timer
        int score = 15;

        // Timer section at top
        objective.getScore(color + "" + ChatColor.BOLD + timerTitle).setScore(score--);
        objective.getScore(color + "" + ChatColor.BOLD + timeDisplay).setScore(score--);
        
        // Empty line
        objective.getScore(ChatColor.WHITE + "").setScore(score--);

        // Player info
        objective.getScore(ChatColor.AQUA + "Player: " + ChatColor.WHITE + player.getName()).setScore(score--);
        
        // Empty line
        objective.getScore(ChatColor.GRAY + "").setScore(score--);

        // Game status
        Game currentGame = gameManager.getPlayerGame(player);
        if (currentGame != null) {
            objective.getScore(ChatColor.GREEN + "Game: " + ChatColor.WHITE + currentGame.getId()).setScore(score--);
            objective.getScore(ChatColor.GREEN + "Arena: " + ChatColor.WHITE + currentGame.getArenaId()).setScore(score--);
            objective.getScore(ChatColor.GREEN + "Status: " + ChatColor.WHITE + getGameStateDisplay(currentGame.getState())).setScore(score--);
            
            // Show team info
            Game.TeamColor playerTeam = currentGame.getPlayerTeam(player);
            if (playerTeam != null) {
                objective.getScore(ChatColor.GREEN + "Team: " + playerTeam.chatColor + playerTeam.displayName).setScore(score--);
            }
        } else {
            objective.getScore(ChatColor.RED + "Status: " + ChatColor.WHITE + "Not in game").setScore(score--);
        }

        // Empty line
        objective.getScore(ChatColor.DARK_GRAY + "").setScore(score--);

        // Footer
        objective.getScore(ChatColor.GOLD + "play.battlebox.com").setScore(score--);
    }
    
    private String getGameStateDisplay(GameState state) {
        return switch (state) {
            case WAITING -> "Waiting";
            case STARTING -> "Starting";
            case IN_PROGRESS -> "In Progress";
            case ENDING -> "Ending";
        };
    }

    public void removeScoreboard(Player player) {
        playerScoreboards.remove(player);
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    public void updateAllScoreboards() {
        for (Player player : playerScoreboards.keySet()) {
            if (player.isOnline()) {
                updateScoreboard(player);
            }
        }
    }
}
