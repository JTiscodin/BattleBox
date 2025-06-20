package plugins.battlebox.listeners;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import plugins.battlebox.game.Game;
import plugins.battlebox.game.GameManager;
import plugins.battlebox.managers.ArenaManager;
import config.ArenaConfig;

public class BlockBreakListener implements Listener {

    private final GameManager gameManager;
    private final ArenaManager arenaManager;

    public BlockBreakListener(GameManager gameManager, ArenaManager arenaManager) {
        this.gameManager = gameManager;
        this.arenaManager = arenaManager;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        Player player = e.getPlayer();
        
        // Allow operators in creative mode to break blocks
        if (player.isOp() && player.getGameMode() == GameMode.CREATIVE) {
            e.setDropItems(false); // Don't drop items when breaking in creative
            return;
        }
        
        // Check if player is in a game
        Game game = gameManager.getPlayerGame(player);
        if (game == null) {
            e.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You can only break blocks while in a BattleBox game!");
            return;
        }
        
        // Get arena config
        ArenaConfig arena = arenaManager.getArena(game.getArenaId());
        if (arena == null) {
            e.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Arena configuration not found!");
            return;
        }
        
        // Check if game is in progress
        if (game.getState() != plugins.battlebox.game.GameState.IN_PROGRESS) {
            e.setCancelled(true);
            player.sendMessage(ChatColor.YELLOW + "You can only break blocks when the game is active!");
            return;
        }
        
        // Check if block breaking is in valid area (center 3x3)
        if (!game.isValidPlacementLocation(e.getBlock().getLocation(), arena)) {
            e.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You can only break blocks in the center area!");
            return;
        }
        
        // Allow breaking and don't drop items (to prevent item farming)
        e.setDropItems(false);
        player.sendMessage(ChatColor.GREEN + "Block broken!");
    }
}
