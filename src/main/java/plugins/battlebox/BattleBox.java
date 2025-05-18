package plugins.battlebox;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.World;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import plugins.battlebox.commands.TestKit;
import plugins.battlebox.listeners.BlockBreakListener;
import plugins.battlebox.listeners.BlockPlaceListener;
import plugins.battlebox.listeners.PlayerInteractListener;
import plugins.battlebox.managers.ArenaManager;

import java.io.File;
import java.io.FileInputStream;

public final class BattleBox extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().info("BattleBox plugin has been enabled!");
        getCommand("testKit").setExecutor(new TestKit());
        ArenaManager arenaManager = new ArenaManager(this);
        getServer().getPluginManager().registerEvents(new BlockPlaceListener(arenaManager), this);
        getServer().getPluginManager().registerEvents(new BlockBreakListener(arenaManager), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractListener(), this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("loadarena")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Only players can run this command.");
                
                return true;
            }

            Location pasteLocation = new Location(Bukkit.getWorld("world"), -14, 0, -120);
            pasteSchematic("BattleBox.schem", pasteLocation);
            player.sendMessage("Pasting arena at your location!");
            return true;
        }
        return true;
    }

    public void pasteSchematic(String name, Location location){
        try {
            File schematic = new File(getDataFolder().getParentFile().getParentFile(),
                    "plugins/WorldEdit/schematics/" + name);
            ClipboardFormat format = ClipboardFormats.findByFile(schematic);
            try (ClipboardReader reader = format.getReader(new FileInputStream(schematic))) {
                Clipboard clipboard = reader.read();

                World adaptedWorld = BukkitAdapter.adapt(location.getWorld());

                try (EditSession editSession = WorldEdit.getInstance().newEditSession(adaptedWorld)) {
                    BlockVector3 pasteLocation = BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ());
                    Operation operation = new ClipboardHolder(clipboard).createPaste(editSession)
                            .to(pasteLocation)
                            .ignoreAirBlocks(false)
                            .build();
                    Operations.complete(operation);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
