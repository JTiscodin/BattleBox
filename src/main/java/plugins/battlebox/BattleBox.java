package plugins.battlebox;

import java.io.File;
import java.io.FileInputStream;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

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

import plugins.battlebox.commands.ArenaCommand;
import plugins.battlebox.commands.BattleBoxCommand;
import plugins.battlebox.core.GameService;
import plugins.battlebox.core.KitService;
import plugins.battlebox.core.MusicService;
import plugins.battlebox.core.PlayerService;
import plugins.battlebox.core.VirtualPlayerUtil;
import plugins.battlebox.game.GameManager;
import plugins.battlebox.listeners.ArenaCreationListener;
import plugins.battlebox.listeners.BlockBreakListener;
import plugins.battlebox.listeners.BlockPlaceListener;
import plugins.battlebox.listeners.PlayerConnectionListener;
import plugins.battlebox.listeners.PlayerInteractListener;
import plugins.battlebox.managers.ArenaCreationManager;
import plugins.battlebox.managers.ArenaInstanceManager;
import plugins.battlebox.managers.ArenaManager;
import plugins.battlebox.managers.ScoreboardManager;
import plugins.battlebox.managers.TimerManager;

public final class BattleBox extends JavaPlugin {

    private GameService gameService;
    private PlayerService playerService;
    private KitService kitService;
    private MusicService musicService;
    private GameManager gameManager;
    private ArenaInstanceManager arenaInstanceManager;
    private ArenaCreationManager arenaCreationManager;
    private TimerManager timerManager;
    private ScoreboardManager scoreboardManager;

    @Override
    public void onEnable() {
        getLogger().info("BattleBox plugin starting...");

        // Initialize virtual player utility
        VirtualPlayerUtil.initialize(this);

        // Initialize managers
        ArenaManager arenaManager = new ArenaManager(this);
        arenaInstanceManager = new ArenaInstanceManager(this);
        gameManager = new GameManager(arenaInstanceManager);

        // Initialize services
        playerService = new PlayerService(this);
        kitService = new KitService();
        musicService = new MusicService(this);
        timerManager = new TimerManager(this);
        scoreboardManager = new ScoreboardManager(this, gameManager);
        arenaCreationManager = new ArenaCreationManager(this, arenaManager);
        gameService = new GameService(gameManager, arenaManager, timerManager, playerService, musicService, this);

        // Link managers
        timerManager.setScoreboardManager(scoreboardManager);

        // Register commands
        registerCommands(arenaManager);

        // Register listeners
        registerListeners(gameManager, arenaManager);

        // Setup scoreboards for online players
        setupOnlinePlayers();

        getLogger().info("BattleBox plugin enabled successfully!");
    }

    private void registerCommands(ArenaManager arenaManager) {
        // Main game commands
        BattleBoxCommand battleBoxCommand = new BattleBoxCommand(gameService, arenaManager, musicService);
        getCommand("battlebox").setExecutor(battleBoxCommand);
        getCommand("battlebox").setTabCompleter(battleBoxCommand);

        // Arena creation commands
        ArenaCommand arenaCommand = new ArenaCommand(this, arenaCreationManager);
        getCommand("arena").setExecutor(arenaCommand);
        getCommand("arena").setTabCompleter(arenaCommand);
    }

    private void registerListeners(GameManager gameManager, ArenaManager arenaManager) {
        var pm = getServer().getPluginManager();
        pm.registerEvents(new BlockPlaceListener(gameManager, gameService, musicService, arenaManager, timerManager),
                this);
        pm.registerEvents(new BlockBreakListener(gameManager, arenaManager), this);
        pm.registerEvents(new PlayerInteractListener(gameManager, kitService, arenaManager), this);
        pm.registerEvents(new PlayerConnectionListener(scoreboardManager), this);
        pm.registerEvents(new ArenaCreationListener(arenaCreationManager), this);
    }

    private void setupOnlinePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            scoreboardManager.createScoreboard(player);
        }

        // Start scoreboard update task
        Bukkit.getScheduler().runTaskTimer(this,
                scoreboardManager::updateAllScoreboards, 20L, 20L);
    }

    public GameService getGameService() {
        return gameService;
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    public ArenaInstanceManager getArenaInstanceManager() {
        return arenaInstanceManager;
    }

    public TimerManager getTimerManager() {
        return timerManager;
    }

    public KitService getKitService() {
        return kitService;
    }

    public MusicService getMusicService() {
        return musicService;
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
            player.sendMessage("Arena pasted at world spawn!");
            return true;
        }
        return false;
    }

    public void pasteSchematic(String name, Location location) {
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
                    BlockVector3 pasteLocation = BlockVector3.at(location.getBlockX(), location.getBlockY(),
                            location.getBlockZ());
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
        if (musicService != null) {
            musicService.shutdown();
        }
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
