package plugins.battlebox.listeners;

import config.ArenaConfig;
import config.Box;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import plugins.battlebox.managers.ArenaManager;


public class BlockPlaceListener implements Listener {

    private final ArenaManager arenaManager;

    public BlockPlaceListener(ArenaManager arenaManager){
        this.arenaManager = arenaManager;
    }

    //Lobby Button here - [63, 40, -55] spruce_button

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        if (!e.getPlayer().hasPermission("battlebox.build")
                && !inRegion(e.getBlock().getLocation())) {
            e.setCancelled(true);
            e.getPlayer().sendMessage("You can only place wool blocks in this arena");
            return;
        }
        if(!e.getPlayer().hasPermission("battlebox.build") && e.getBlock().getType() != Material.LIME_WOOL && e.getBlock().getType() != Material.PINK_WOOL){
            e.setCancelled(true);
            e.getPlayer().sendMessage("You can only place team coloured wool here");
        }
        if(!e.getPlayer().hasPermission("battlebox.build") && e.getBlock().getType() == Material.LIME_WOOL || e.getBlock().getType() == Material.PINK_WOOL){
            e.getPlayer().getInventory().setItemInOffHand(new ItemStack(Material.LIME_WOOL, 64));
        }

    }

    //Block place should only be allowed in Wool Place area.
    public boolean inRegion(Location loc) {
        // Only one arena for now
        ArenaConfig arena = arenaManager.getArenas().get(0);
        if (!loc.getWorld().getName().equals(arena.world)) return false;

        Box box = arena.box;

        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();

        return (x >= box.x1 && x <= box.x2) &&
                (y == box.y2) &&
                (z >= box.z1 && z <= box.z2);
    }
}
