package plugins.battlebox.listeners;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import plugins.battlebox.core.GameService;
import plugins.battlebox.core.KitService;
import plugins.battlebox.game.Game;
import plugins.battlebox.managers.ArenaManager;
import config.ArenaConfig;

/**
 * Clean kit selection listener
 */
public class KitSelectionListener implements Listener {
    
    private final GameService gameService;
    private final KitService kitService;
    private final ArenaManager arenaManager;
    
    public KitSelectionListener(GameService gameService, KitService kitService, ArenaManager arenaManager) {
        this.gameService = gameService;
        this.kitService = kitService;
        this.arenaManager = arenaManager;
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;
        if (!isButton(event.getClickedBlock().getType())) return;
        
        Player player = event.getPlayer();
        Game game = gameService.getPlayerGame(player);
        
        if (game == null) {
            player.sendMessage(ChatColor.GRAY + "Available kits: Healer, Fighter, Sniper, Speedster");
            return;
        }
        
        ArenaConfig arena = arenaManager.getArena(game.getArenaId());
        if (arena == null || arena.kits == null) return;
        
        Game.TeamColor team = game.getPlayerTeam(player);
        if (team == null) return;
        
        String kitType = findKitForButton(event.getClickedBlock().getLocation(), arena, team);
        if (kitType != null) {
            kitService.giveSpecialKit(player, kitType, team);
            player.sendMessage(ChatColor.GREEN + "✓ " + kitType.toUpperCase() + " KIT SELECTED!");
        }
    }
    
    private boolean isButton(Material material) {
        return material.name().contains("BUTTON") || 
               material.name().contains("PRESSURE_PLATE") ||
               material == Material.LEVER;
    }
    
    private String findKitForButton(Location clickedLoc, ArenaConfig arena, Game.TeamColor team) {
        String teamPrefix = team == Game.TeamColor.RED ? "red_" : "blue_";
        
        for (config.Kit.Kit kit : arena.kits) {
            if (kit.name.startsWith(teamPrefix) && kit.buttons != null) {
                for (config.Kit.Button button : kit.buttons) {
                    if (clickedLoc.getBlockX() == button.x && 
                        clickedLoc.getBlockY() == button.y && 
                        clickedLoc.getBlockZ() == button.z) {
                        // Extract kit type (remove team prefix)
                        return kit.name.substring(teamPrefix.length());
                    }
                }
            }
        }
        return null;
    }
}
