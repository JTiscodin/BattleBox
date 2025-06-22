package plugins.battlebox.managers;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import plugins.battlebox.game.Game;

/**
 * Manages countdown timers displayed in scoreboard sidebar
 */
public class TimerManager {
    private final JavaPlugin plugin;
    private final Map<String, TimerInstance> activeTimers;
    private ScoreboardManager scoreboardManager;

    public TimerManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.activeTimers = new HashMap<>();
    }

    public void setScoreboardManager(ScoreboardManager scoreboardManager) {
        this.scoreboardManager = scoreboardManager;
    }

    /**
     * Start a countdown timer for specific players
     * 
     * @param timerId    Unique identifier for this timer
     * @param players    Set of players to show timer to
     * @param seconds    Duration in seconds
     * @param title      Main title text (null for default countdown)
     * @param onComplete Runnable to execute when timer completes
     */
    public void startTimer(String timerId, Set<Player> players, int seconds, String title, Runnable onComplete) {
        // Stop existing timer with same ID
        stopTimer(timerId);

        String mainTitle = title != null ? title : "GAME STARTING";
        TimerInstance timer = new TimerInstance(timerId, players, seconds, mainTitle, onComplete);
        activeTimers.put(timerId, timer);
        timer.start();

        plugin.getLogger().info(
                "Started timer '" + timerId + "' for " + seconds + " seconds with " + players.size() + " players");
    }

    /**
     * Start a countdown timer for all online players
     */
    public void startGlobalTimer(String timerId, int seconds, String title, Runnable onComplete) {
        startTimer(timerId, Set.copyOf(Bukkit.getOnlinePlayers()), seconds, title, onComplete);
    }

    /**
     * Start a game timer for specific game
     */
    public void startGameTimer(Game game, int seconds, String title, Runnable onComplete) {
        String timerId = "game_" + game.getId();
        startTimer(timerId, game.getPlayers(), seconds, title, onComplete);
    }

    /**
     * Stop a timer by ID
     */
    public void stopTimer(String timerId) {
        TimerInstance timer = activeTimers.remove(timerId);
        if (timer != null) {
            timer.cancel();
            plugin.getLogger().info("Stopped timer '" + timerId + "'");
        }
    }

    /**
     * Stop all active timers
     */
    public void stopAllTimers() {
        for (TimerInstance timer : activeTimers.values()) {
            timer.cancel();
        }
        activeTimers.clear();
        plugin.getLogger().info("Stopped all timers");
    }

    /**
     * Get active timer count
     */
    public int getActiveTimerCount() {
        return activeTimers.size();
    }

    /**
     * Check if a timer is running
     */
    public boolean isTimerRunning(String timerId) {
        return activeTimers.containsKey(timerId);
    }

    /**
     * Get remaining time for a timer
     */
    public int getRemainingTime(String timerId) {
        TimerInstance timer = activeTimers.get(timerId);
        return timer != null ? timer.getRemainingSeconds() : -1;
    }

    /**
     * Internal timer instance class
     */
    private class TimerInstance {
        private final String id;
        private final Set<Player> players;
        private final String mainTitle;
        private final Runnable onComplete;
        private int remainingSeconds;
        private BukkitTask task;

        public TimerInstance(String id, Set<Player> players, int seconds, String mainTitle, Runnable onComplete) {
            this.id = id;
            this.players = players;
            this.remainingSeconds = seconds;
            this.mainTitle = mainTitle;
            this.onComplete = onComplete;
        }

        public void start() {
            task = new BukkitRunnable() {
                @Override
                public void run() {
                    if (remainingSeconds <= 0) {
                        // Timer finished
                        showTimerComplete();
                        activeTimers.remove(id);
                        this.cancel();

                        // Run completion callback
                        if (onComplete != null) {
                            onComplete.run();
                        }
                        return;
                    }

                    // Show countdown
                    showCountdown();
                    remainingSeconds--;
                }
            }.runTaskTimer(plugin, 0L, 20L); // Run every second (20 ticks)
        }

        public void cancel() {
            if (task != null) {
                task.cancel();
                // Clear titles and timer info for all players
                for (Player player : players) {
                    if (player.isOnline()) {
                        player.sendTitle("", "", 0, 1, 0);
                        if (scoreboardManager != null) {
                            scoreboardManager.clearTimerInfo(player);
                        }
                    }
                }
            }
        }

        public int getRemainingSeconds() {
            return remainingSeconds;
        }

        private void showCountdown() {
            String timeDisplay = formatTime(remainingSeconds);
            ChatColor color = getTimeColor(remainingSeconds);

            // Update timer info for all players in timer (they'll be updated on next
            // scoreboard refresh)
            for (Player player : players) {
                if (player.isOnline() && scoreboardManager != null) {
                    scoreboardManager.setTimerInfo(player, mainTitle, timeDisplay, color);
                }
            }
        }

        private void showTimerComplete() {
            // Clear timer info and briefly show completion message
            for (Player player : players) {
                if (player.isOnline()) {
                    if (scoreboardManager != null) {
                        scoreboardManager.setTimerInfo(player, "GAME STARTED", "GO!", ChatColor.GREEN);
                    }
                    // Send a brief title as well for the "GO!" moment
                    player.sendTitle(ChatColor.GREEN + "" + ChatColor.BOLD + "GO!",
                            ChatColor.GREEN + "Game Started!", 5, 30, 10);
                }
            }

            // After 2 seconds, clear timer info (normal scoreboard will show on next
            // update)
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                for (Player player : players) {
                    if (player.isOnline() && scoreboardManager != null) {
                        scoreboardManager.clearTimerInfo(player);
                    }
                }
            }, 40L); // 2 seconds
        }

        private String formatTime(int seconds) {
            if (seconds >= 60) {
                int minutes = seconds / 60;
                int remainderSeconds = seconds % 60;
                return String.format("%d:%02d", minutes, remainderSeconds);
            } else {
                return String.valueOf(seconds);
            }
        }

        private ChatColor getTimeColor(int seconds) {
            if (seconds <= 3) {
                return ChatColor.RED;
            } else if (seconds <= 10) {
                return ChatColor.YELLOW;
            } else {
                return ChatColor.GREEN;
            }
        }
    }
}
