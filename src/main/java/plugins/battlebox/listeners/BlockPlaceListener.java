package plugins.battlebox.listeners;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import plugins.battlebox.game.Game;
import plugins.battlebox.game.GameManager;
import plugins.battlebox.managers.ArenaManager;
import config.ArenaConfig;

public class BlockPlaceListener implements Listener {

    private final GameManager gameManager;
    private final ArenaManager arenaManager;

    public BlockPlaceListener(GameManager gameManager, ArenaManager arenaManager) {
        this.gameManager = gameManager;
        this.arenaManager = arenaManager;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        Player player = e.getPlayer();
        
        // Allow operators in creative mode to place blocks for building
        if (player.isOp() && player.getGameMode() == GameMode.CREATIVE) {
            return; // Allow placement
        }
        
        // Check if player is in a game
        Game game = gameManager.getPlayerGame(player);
        if (game == null) {
            e.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You can only place blocks while in a BattleBox game!");
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
            player.sendMessage(ChatColor.YELLOW + "You can only place blocks when the game is active! Current state: " + game.getState());
            return;
        }
        
        // Check if block placement is in valid area (center 3x3)
        if (!game.isValidPlacementLocation(e.getBlockPlaced().getLocation(), arena)) {
            e.setCancelled(true);
            org.bukkit.Location loc = e.getBlockPlaced().getLocation();
            player.sendMessage(ChatColor.RED + "You can only place blocks in the center area!");
            player.sendMessage(ChatColor.GRAY + "Attempted location: " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
            if (arena.centerBox != null) {
                player.sendMessage(ChatColor.GRAY + "Center area: " + arena.centerBox.x1 + "," + arena.centerBox.y1 + "," + arena.centerBox.z1 + 
                                  " to " + arena.centerBox.x2 + "," + arena.centerBox.y2 + "," + arena.centerBox.z2);
            } else {
                player.sendMessage(ChatColor.RED + "Center box not configured for this arena!");
            }
            return;
        }
        
        // Check if player is placing their team's wool
        Game.TeamColor playerTeam = game.getPlayerTeam(player);
        Material placedMaterial = e.getBlockPlaced().getType();
        
        boolean validWool = (playerTeam == Game.TeamColor.RED && placedMaterial == Material.RED_WOOL) ||
                           (playerTeam == Game.TeamColor.BLUE && placedMaterial == Material.BLUE_WOOL);
        
        if (!validWool) {
            e.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You can only place your team's wool blocks!");
            return;
        }
          // Auto-refill wool in inventory
        refillWool(player, playerTeam);
        
        player.sendMessage(ChatColor.GREEN + "Wool placed successfully!");
        
        // Check for instant win after successful placement
        org.bukkit.Bukkit.getScheduler().runTaskLater(
            org.bukkit.Bukkit.getPluginManager().getPlugin("BattleBox"), 
            () -> checkInstantWin(game, arena), 1L);
    }
    
    private void refillWool(Player player, Game.TeamColor team) {
        Material woolType = team == Game.TeamColor.RED ? Material.RED_WOOL : Material.BLUE_WOOL;
        
        // Check if player has wool in inventory
        boolean hasWool = false;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == woolType) {
                hasWool = true;
                break;
            }
        }
        
        // If no wool found, give them a new stack
        if (!hasWool) {
            ItemStack wool = new ItemStack(woolType, 64);
            player.getInventory().addItem(wool);
            player.sendMessage(team.chatColor + "Wool refilled!");
        }
    }
    
    private void checkInstantWin(Game game, ArenaConfig arena) {
        // Recalculate to check for instant win
        game.calculateWinner(arena);
        
        if (game.hasWinner()) {
            // Announce instant win
            for (Player player : game.getPlayers()) {
                Game.TeamColor winner = game.getWinner();
                player.sendMessage(ChatColor.GOLD + "=== INSTANT WIN! ===");
                player.sendMessage(winner.chatColor + "" + ChatColor.BOLD + winner.displayName + " TEAM WINS!");
                player.sendMessage(ChatColor.YELLOW + game.getWinReason());
            }
              // End the game
            game.setState(plugins.battlebox.game.GameState.ENDING);
            gameManager.removeGame(game.getId());
        }
    }
}
