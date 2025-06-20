package plugins.battlebox.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import plugins.battlebox.managers.ArenaCreationManager;

public class ArenaCreationListener implements Listener {
    
    private final ArenaCreationManager arenaCreationManager;
    
    public ArenaCreationListener(ArenaCreationManager arenaCreationManager) {
        this.arenaCreationManager = arenaCreationManager;
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;
        
        Player player = event.getPlayer();
        Material blockType = event.getClickedBlock().getType();
        
        // Check if it's a button and player is creating an arena
        if (isButton(blockType) && arenaCreationManager.isWaitingForKitButton(player)) {
            if (arenaCreationManager.handleKitButtonClick(player, event.getClickedBlock().getLocation())) {
                event.setCancelled(true);
            }
        }
    }
    
    private boolean isButton(Material material) {
        return material.name().contains("BUTTON") || 
               material.name().contains("PRESSURE_PLATE") ||
               material == Material.LEVER;
    }
}
