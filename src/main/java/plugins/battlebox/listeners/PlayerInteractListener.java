package plugins.battlebox.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashMap;

public class PlayerInteractListener implements Listener {

    private final HashMap<Location, String> kitMap = new HashMap<>();

    public PlayerInteractListener() {
        World world = Bukkit.getWorld("world"); // replace "world" if your world is named differently
        if (world == null) {
            Bukkit.getLogger().warning("World 'world' not found!");
            return;
        }

        // Add button locations mapped to kit names
        kitMap.put(new Location(world, 42, 16, -52), "Healer");
        kitMap.put(new Location(world, 42, 16, -54), "Swordsman");
        kitMap.put(new Location(world, 42, 16, -56), "Aquaman");
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = e.getClickedBlock();
        if (block == null || block.getType() != Material.OAK_BUTTON) return;

        Location loc = block.getLocation();
        String kitName = kitMap.get(loc);

        if (kitName != null) {
            e.getPlayer().sendMessage("You have selected the " + kitName + " kit!");
        } else {
            e.getPlayer().sendMessage("You clicked a button, but it's not linked to a kit.");
        }
    }
}
