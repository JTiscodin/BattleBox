package plugins.battlebox.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import plugins.battlebox.managers.ScoreboardManager;

public class PlayerConnectionListener implements Listener {
    private final ScoreboardManager scoreboardManager;

    public PlayerConnectionListener(ScoreboardManager scoreboardManager) {
        this.scoreboardManager = scoreboardManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Create scoreboard for the player when they join
        scoreboardManager.createScoreboard(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Clean up scoreboard when player leaves
        scoreboardManager.removeScoreboard(event.getPlayer());
    }
}
