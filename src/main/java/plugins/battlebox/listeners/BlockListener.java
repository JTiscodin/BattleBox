package plugins.battlebox.listeners;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import plugins.battlebox.core.GameService;
import plugins.battlebox.core.PlayerService;
import plugins.battlebox.game.Game;
import plugins.battlebox.game.GameState;
import plugins.battlebox.managers.ArenaManager;
import config.ArenaConfig;

/**
 * Clean block placement listener
 */
public class BlockListener implements Listener {
    
    private final GameService gameService;
    private final PlayerService playerService;
    private final ArenaManager arenaManager;
    
    public BlockListener(GameService gameService, PlayerService playerService, ArenaManager arenaManager) {
        this.gameService = gameService;
        this.playerService = playerService;
        this.arenaManager = arenaManager;
    }
    
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        
        // Allow ops in creative mode
        if (player.isOp() && player.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        
        Game game = gameService.getPlayerGame(player);
        if (game == null) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You can only place blocks during BattleBox games!");
            return;
        }
        
        // Only allow placement during active game
        if (game.getState() != GameState.IN_PROGRESS) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.YELLOW + "Wait for the game to start!");
            return;
        }
        
        ArenaConfig arena = arenaManager.getArena(game.getArenaId());
        if (arena == null) {
            event.setCancelled(true);
            return;
        }
        
        // Check valid placement location (center area)
        if (!game.isValidPlacementLocation(event.getBlock().getLocation(), arena)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You can only place blocks in the center area!");
            return;
        }
        
        // Check if placing correct team wool
        Game.TeamColor team = game.getPlayerTeam(player);
        Material woolType = team == Game.TeamColor.RED ? Material.RED_WOOL : Material.BLUE_WOOL;
        
        if (event.getBlock().getType() != woolType) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You can only place your team's wool!");
            return;
        }
        
        // Auto-refill wool after placement
        playerService.refillWool(player, team);
        
        // Check for instant win
        checkForInstantWin(game, arena);
    }
    
    private void checkForInstantWin(Game game, ArenaConfig arena) {
        game.calculateWinner(arena);
        if (game.hasWinner()) {
            gameService.endGame(game);
        }
    }
}
