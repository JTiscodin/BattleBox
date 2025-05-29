package plugins.battlebox.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.AsyncPlayerChatEvent;
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
        if (isButton(blockType) && arenaCreationManager.isCreating(player)) {
            if (arenaCreationManager.handleButtonClick(player, event.getClickedBlock().getLocation())) {
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        
        if (arenaCreationManager.isInWizard(player)) {
            event.setCancelled(true);
            
            // Handle wizard input on main thread
            String message = event.getMessage();
            player.getServer().getScheduler().runTask(
                player.getServer().getPluginManager().getPlugin("BattleBox"),
                () -> arenaCreationManager.handleWizardInput(player, message)
            );
        }
    }
    
    private boolean isButton(Material material) {
        return material.name().contains("BUTTON");
    }
}
