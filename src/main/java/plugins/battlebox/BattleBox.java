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
import plugins.battlebox.commands.ArenaCommand;
import plugins.battlebox.commands.GameTestCommand;
import plugins.battlebox.commands.TestKit;
import plugins.battlebox.commands.TimerTestCommand;
import plugins.battlebox.game.GameManager;
import plugins.battlebox.listeners.ArenaCreationListener;
import plugins.battlebox.listeners.BlockBreakListener;
import plugins.battlebox.listeners.BlockPlaceListener;
import plugins.battlebox.listeners.PlayerConnectionListener;
import plugins.battlebox.listeners.PlayerInteractListener;
import plugins.battlebox.managers.ArenaCreationManager;
import plugins.battlebox.managers.ArenaManager;
import plugins.battlebox.managers.ScoreboardManager;
import plugins.battlebox.managers.TimerManager;

import java.io.File;
import java.io.FileInputStream;

public final class BattleBox extends JavaPlugin {
    
    private GameManager gameManager;
    private ScoreboardManager scoreboardManager;
    private ArenaCreationManager arenaCreationManager;
    private TimerManager timerManager;

    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().info("BattleBox plugin has been enabled!");
        
        // Initialize managers
        gameManager = new GameManager();
        ArenaManager arenaManager = new ArenaManager(this);
        arenaCreationManager = new ArenaCreationManager(this, arenaManager);
        timerManager = new TimerManager(this);
        scoreboardManager = new ScoreboardManager(this, gameManager);
        
        // Register commands
        getCommand("testKit").setExecutor(new TestKit());
        ArenaCommand arenaCommand = new ArenaCommand(arenaCreationManager, this);
        getCommand("arena").setExecutor(arenaCommand);
        getCommand("arena").setTabCompleter(arenaCommand);
        
        GameTestCommand gameTestCommand = new GameTestCommand(gameManager, arenaManager);
        getCommand("gametest").setExecutor(gameTestCommand);
        getCommand("gametest").setTabCompleter(gameTestCommand);
        
        TimerTestCommand timerTestCommand = new TimerTestCommand(this);
        getCommand("timertest").setExecutor(timerTestCommand);
        getCommand("timertest").setTabCompleter(timerTestCommand);
        
        // Register event listeners
        getServer().getPluginManager().registerEvents(new BlockPlaceListener(arenaManager), this);
        getServer().getPluginManager().registerEvents(new BlockBreakListener(arenaManager), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(scoreboardManager), this);
        getServer().getPluginManager().registerEvents(new ArenaCreationListener(arenaCreationManager), this);
        
        // Create scoreboards for already online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            scoreboardManager.createScoreboard(player);
        }
        
        // Start scoreboard update task
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            scoreboardManager.updateAllScoreboards();
        }, 20L, 20L); // Update every second (20 ticks)
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }
    
    public TimerManager getTimerManager() {
        return timerManager;
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
            
            if (format == null) {
                getLogger().warning("Could not find clipboard format for schematic: " + name);
                return;
            }
            
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
        if (timerManager != null) {
            timerManager.stopAllTimers();
        }
        if (scoreboardManager != null) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                scoreboardManager.removeScoreboard(player);
            }
        }
        getLogger().info("BattleBox plugin has been disabled!");
    }
}
