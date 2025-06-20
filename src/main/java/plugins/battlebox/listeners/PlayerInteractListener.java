package plugins.battlebox.listeners;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import plugins.battlebox.core.KitService;
import plugins.battlebox.game.Game;
import plugins.battlebox.game.GameManager;
import plugins.battlebox.managers.ArenaManager;
import config.ArenaConfig;

public class PlayerInteractListener implements Listener {

    private final GameManager gameManager;
    private final KitService kitService;
    private final ArenaManager arenaManager;

    public PlayerInteractListener(GameManager gameManager, KitService kitService, ArenaManager arenaManager) {
        this.gameManager = gameManager;
        this.kitService = kitService;
        this.arenaManager = arenaManager;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = e.getClickedBlock();
        if (block == null) return;
        
        // Check if it's any type of button
        Material blockType = block.getType();
        if (!blockType.toString().contains("BUTTON")) return;

        Player player = e.getPlayer();
        Location clickedLoc = block.getLocation();
        
        // Check if player is in a game
        Game game = gameManager.getPlayerGame(player);
        if (game == null) {
            player.sendMessage(ChatColor.YELLOW + "Click buttons to select kits when you're in a BattleBox game!");
            player.sendMessage(ChatColor.GRAY + "Available kits: Healer, Fighter, Sniper, Speedster");
            return;
        }

        // Get arena config
        ArenaConfig arena = arenaManager.getArena(game.getArenaId());
        if (arena == null || arena.kits == null) {
            player.sendMessage(ChatColor.RED + "Arena configuration not found!");
            return;
        }

        // Get player's team
        Game.TeamColor playerTeam = game.getPlayerTeam(player);
        if (playerTeam == null) {
            player.sendMessage(ChatColor.RED + "You are not assigned to a team!");
            return;
        }

        // Find matching kit button
        String teamPrefix = playerTeam == Game.TeamColor.RED ? "red_" : "blue_";
        
        for (config.Kit.Kit kit : arena.kits) {
            if (kit.name.startsWith(teamPrefix) && kit.buttons != null) {                for (config.Kit.Button button : kit.buttons) {
                    if (clickedLoc.getBlockX() == button.x && 
                        clickedLoc.getBlockY() == button.y && 
                        clickedLoc.getBlockZ() == button.z) {
                        
                        // Extract kit type from name (remove team prefix)
                        String kitType = kit.name.substring(teamPrefix.length());
                        
                        // Give the kit using KitService
                        kitService.giveSpecialKit(player, kitType, playerTeam);
                        return;
                    }
                }            }
        }
        
        // If no kit found, show available kits
        player.sendMessage(ChatColor.YELLOW + "This button is not configured for a kit in your arena.");
        player.sendMessage(ChatColor.GRAY + "Available kits: Healer, Fighter, Sniper, Speedster");
    }
}
