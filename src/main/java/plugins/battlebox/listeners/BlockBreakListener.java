package plugins.battlebox.listeners;

import config.ArenaConfig;
import config.Box;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import plugins.battlebox.managers.ArenaManager;

public class BlockBreakListener implements Listener {
    private final ArenaManager arenaManager;

    public BlockBreakListener(ArenaManager arenaManager){
        this.arenaManager = arenaManager;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e){
        if(!e.getPlayer().hasPermission("battlebox.build") && !inRegion(e.getBlock().getLocation())){
            e.setCancelled(true);
            e.getPlayer().sendMessage("You can't break blocks in this arena");
        }
        //we don't want to drop items when breaking, since we are having infinite blocks of wool.
        e.setDropItems(false);
    }

    public boolean inRegion(Location loc) {
        ArenaConfig arena = arenaManager.getArenas().get(0);
        if (!loc.getWorld().getName().equals(arena.world)) return false;

        Box box = arena.box;

        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();

        return (x >= box.x1 && x <= box.x2) &&
                (y >= box.y1 && y <= box.y2) &&
                (z >= box.z1 && z <= box.z2);
    }
}
