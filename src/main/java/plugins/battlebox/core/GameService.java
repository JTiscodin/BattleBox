package plugins.battlebox.core;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitTask;

import config.ArenaConfig;
import plugins.battlebox.game.Game;
import plugins.battlebox.game.GameManager;
import plugins.battlebox.game.GameState;
import plugins.battlebox.managers.ArenaManager;
import plugins.battlebox.managers.TimerManager;

import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core service for managing game lifecycle and operations.
 * Handles game creation, player joining, and game flow.
 */
public class GameService {

    private final GameManager gameManager;
    private final ArenaManager arenaManager;
    private final TimerManager timerManager;
    private final PlayerService playerService;
    private final MusicService musicService;
    private final org.bukkit.plugin.java.JavaPlugin plugin;

    // Victory fireworks management
    private final Map<String, BukkitTask> victoryFireworksMap = new ConcurrentHashMap<>();

    public GameService(GameManager gameManager, ArenaManager arenaManager,
            TimerManager timerManager, PlayerService playerService, MusicService musicService,
            org.bukkit.plugin.java.JavaPlugin plugin) {
        this.gameManager = gameManager;
        this.arenaManager = arenaManager;
        this.timerManager = timerManager;
        this.playerService = playerService;
        this.musicService = musicService;
        this.plugin = plugin;
    }

    /**
     * Create and start a new game
     */
    public boolean createGame(Player creator, String arenaId) {
        ArenaConfig arena = arenaManager.getArena(arenaId);
        if (arena == null) {
            VirtualPlayerUtil.safeSendMessage(creator, ChatColor.RED + "Arena '" + arenaId + "' not found!");
            return false;
        }

        if (!isArenaComplete(arena)) {
            VirtualPlayerUtil.safeSendMessage(creator,
                    ChatColor.RED + "Arena is incomplete. Use /arena info " + arenaId + " to see what's missing.");
            return false;
        }

        String gameId = "game_" + System.currentTimeMillis();
        Game game = new Game(gameId, arenaId);
        gameManager.createGame(gameId, game);

        // Start music for the new game
        musicService.startGameMusic(game);

        if (!joinGame(creator, gameId)) {
            gameManager.removeGame(gameId);
            musicService.stopGameMusic(gameId);
            return false;
        }
        VirtualPlayerUtil.safeSendMessage(creator, ChatColor.GREEN + "Game created! Waiting for more players...");
        return true;
    }

    /**
     * Join an existing game
     */
    public boolean joinGame(Player player, String gameId) {
        Game game = gameManager.getGame(gameId);
        if (game == null) {
            VirtualPlayerUtil.safeSendMessage(player, ChatColor.RED + "Game not found");
            return false;
        }

        if (!game.addPlayer(player)) {
            VirtualPlayerUtil.safeSendMessage(player, ChatColor.RED + "Cannot join game (possibly full)");
            return false;
        }

        playerService.setupPlayerForGame(player, game);

        // Handle music for player joining
        musicService.onPlayerJoinGame(player, game);

        // Handle game state based on current state and player count
        handlePlayerJoinGameFlow(game, player);

        return true;
    }

    /**
     * Find and join an existing game for the given arena, or create a new one if
     * none exists
     */
    public boolean joinOrCreateGame(Player player, String arenaId) {
        // First, try to find an existing game for this arena
        Game existingGame = findJoinableGame(arenaId);

        if (existingGame != null) {
            return joinGame(player, existingGame.getId());
        } else {
            // No joinable game found, create a new one
            return createGame(player, arenaId);
        }
    }

    /**
     * Find a game that can be joined for the given arena
     */
    private Game findJoinableGame(String arenaId) {
        for (Game game : gameManager.getActiveGames().values()) {
            if (game.getArenaId().equals(arenaId) &&
                    (game.getState() == GameState.WAITING || game.getState() == GameState.KIT_SELECTION) &&
                    game.getPlayerCount() < 8) { // MAX_PLAYERS
                return game;
            }
        }
        return null;
    }

    /**
     * Handle game flow when a player joins
     */
    private void handlePlayerJoinGameFlow(Game game, Player player) {
        ArenaConfig arena = arenaManager.getArena(game.getArenaId());

        if (game.getState() == GameState.WAITING) {
            // Teleport to waiting area (center of map)
            playerService.teleportToWaitingArea(player, arena);

            // If we now have 2 or more players, start the 30-second timer for kit selection
            if (game.getPlayerCount() >= 2) {
                startWaitingTimer(game);
            }
        } else if (game.getState() == GameState.KIT_SELECTION) { // Game is already in kit selection, teleport to spawn
                                                                 // and give base kit
            playerService.teleportToSpawn(player, game, arena);
            playerService.giveBaseKit(player, game.getPlayerTeam(player));
            VirtualPlayerUtil.safeSendMessage(player,
                    ChatColor.YELLOW + "Kit selection is in progress! Choose your kit!");
        }
        // Note: Players cannot join during IN_PROGRESS or ENDING states
    }

    /**
     * Start 30-second waiting timer before kit selection
     */
    private void startWaitingTimer(Game game) {
        broadcastToGame(game, ChatColor.GREEN + "Minimum players reached! Kit selection starts in 30 seconds...");
        broadcastToGame(game, ChatColor.GRAY + "More players can still join (max 8 players)");

        // 30-second timer before kit selection starts
        timerManager.startTimer("waiting_" + game.getId(), game.getPlayers(), 30,
                "WAITING FOR PLAYERS", () -> startKitSelection(game));
    }

    /**
     * Start the actual battle phase
     */
    public void startBattle(Game game) {
        ArenaConfig arena = arenaManager.getArena(game.getArenaId());

        // Teleport players to their battle positions
        for (Player player : game.getPlayers()) {
            playerService.teleportToGamePosition(player, game, arena);
        }

        // Brief countdown before setting to IN_PROGRESS
        timerManager.startTimer("starting_" + game.getId(), game.getPlayers(), 5,
                "GAME STARTING", () -> {
                    game.setState(GameState.IN_PROGRESS);
                    // Note: Music is already set to BATTLE in startKitSelection, no need to update
                    // here

                    // Start 2-minute game timer
                    timerManager.startTimer("game_" + game.getId(), game.getPlayers(), 120,
                            "BATTLE BOX", () -> endGame(game));

                    broadcastToGame(game, ChatColor.GREEN + "BATTLE STARTED! Fill the center with your wool!");
                });

        broadcastToGame(game, ChatColor.YELLOW + "Get ready! Battle starting soon...");
    }

    /**
     * End the game and determine winner
     */
    public void endGame(Game game) {
        ArenaConfig arena = arenaManager.getArena(game.getArenaId());
        game.setState(GameState.ENDING);
        game.calculateWinner(arena);
        plugin.getLogger().info("Game " + game.getId() + " ended - starting victory phase");

        // Announce results immediately
        announceResults(game);

        // Start victory music and play victory sounds
        startVictoryPhase(game);

        // Start 10-second victory timer before cleanup
        timerManager.startTimer("victory_" + game.getId(), game.getPlayers(), 20,
                "VICTORY", () -> cleanupGame(game));

        broadcastToGame(game, ChatColor.YELLOW + "Returning to lobby in 10 seconds...");
    }

    /**
     * Start the victory phase with appropriate music and sound effects
     */
    private void startVictoryPhase(Game game) {
        // Play victory sound effects based on game result
        if (game.hasWinner()) {
            Game.TeamColor winner = game.getWinner();
            // Play team victory sound
            musicService.onGameEnd(game, winner);
            // Start victory fireworks for the winning team
            startVictoryFireworks(game, winner);
        } else if (game.isDraw()) {
            // Play draw sound
            musicService.onGameEnd(game, null);
        }

        // Update music to victory/ending music
        musicService.updateGameMusic(game);

        plugin.getLogger().info("Victory phase started for game " + game.getId());
    }

    private void startKitSelection(Game game) {
        game.setState(GameState.KIT_SELECTION);

        // Update music for state change - ONLY call this once per state change
        musicService.updateGameMusic(game);

        ArenaConfig arena = arenaManager.getArena(game.getArenaId());

        for (Player player : game.getPlayers()) {
            playerService.teleportToSpawn(player, game, arena);
            playerService.giveBaseKit(player, game.getPlayerTeam(player));
        }

        // Give 30 seconds for kit selection
        int duration = 30;

        timerManager.startTimer("kit_" + game.getId(), game.getPlayers(), duration,
                "KIT SELECTION", () -> startBattle(game));

        broadcastToGame(game, ChatColor.YELLOW + "Select your kit! Battle starts in " + duration + " seconds!");
    }

    private boolean isArenaComplete(ArenaConfig arena) {
        return arena.teamSpawns != null &&
                arena.teamSpawns.redSpawn != null &&
                arena.teamSpawns.blueSpawn != null &&
                arena.teamSpawns.redTeleport != null &&
                arena.teamSpawns.blueTeleport != null &&
                arena.centerBox != null;
    }

    private void announceResults(Game game) {
        String message;
        if (game.hasWinner()) {
            Game.TeamColor winner = game.getWinner();
            message = winner.chatColor + winner.displayName + " TEAM WINS!";
        } else if (game.isDraw()) {
            message = ChatColor.YELLOW + "DRAW!";
        } else {
            message = ChatColor.RED + "No winner determined";
        }

        broadcastToGame(game, ChatColor.GOLD + "=== GAME OVER ===");
        broadcastToGame(game, message);
        broadcastToGame(game, ChatColor.GRAY + game.getWinReason());
    }

    private void cleanupGame(Game game) {
        // Stop all active timers for this game
        timerManager.stopTimer("game_" + game.getId());
        timerManager.stopTimer("kit_" + game.getId());
        timerManager.stopTimer("waiting_" + game.getId());
        timerManager.stopTimer("starting_" + game.getId());
        timerManager.stopTimer("victory_" + game.getId());

        // Clean up players FIRST (before removing game so we have access to game data)
        cleanupPlayers(game);

        // Reset arena to clean state BEFORE removing game (need game reference for
        // arena)
        resetGameArena(game);

        // Stop victory fireworks BEFORE removing game
        stopVictoryFireworks(game.getId());

        // Stop music BEFORE removing game
        musicService.stopGameMusic(game.getId());

        // Remove game LAST
        gameManager.removeGame(game.getId());

        // Final message to remaining players (if any)
        plugin.getLogger().info("Game cleanup completed for game " + game.getId());
    }

    /**
     * Reset the arena to a clean state after game ends
     */
    private void resetGameArena(Game game) {
        ArenaConfig arena = arenaManager.getArena(game.getArenaId());
        if (arena == null) {
            plugin.getLogger().warning("Cannot reset arena - arena config not found for game " + game.getId());
            return;
        }

        // Reset center blocks (main reset - removes wool)
        resetCenterBlocks(arena);

        // Clean up projectiles and dropped items
        cleanupArenaEntities(arena);

        plugin.getLogger().info("Arena reset completed for game " + game.getId());
    }

    /**
     * Reset only the center 3x3 area blocks to clean state
     */
    private void resetCenterBlocks(ArenaConfig arena) {
        if (arena.centerBox == null) {
            plugin.getLogger().warning("Cannot reset center blocks - centerBox is null");
            return;
        }

        org.bukkit.World world = org.bukkit.Bukkit.getWorld(arena.world);
        if (world == null) {
            plugin.getLogger().warning("Cannot reset center blocks - world '" + arena.world + "' not found");
            return;
        }

        int minX = Math.min(arena.centerBox.x1, arena.centerBox.x2);
        int maxX = Math.max(arena.centerBox.x1, arena.centerBox.x2);
        int minZ = Math.min(arena.centerBox.z1, arena.centerBox.z2);
        int maxZ = Math.max(arena.centerBox.z1, arena.centerBox.z2);
        int y = arena.centerBox.y1;

        int blocksReset = 0;

        // Reset all blocks in center area to WHITE WOOL (clean state)
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                org.bukkit.block.Block block = world.getBlockAt(x, y, z);
                // Reset any wool or non-air blocks to white wool
                if (block.getType() == org.bukkit.Material.RED_WOOL ||
                        block.getType() == org.bukkit.Material.BLUE_WOOL ||
                        block.getType() != org.bukkit.Material.WHITE_WOOL) {
                    block.setType(org.bukkit.Material.WHITE_WOOL);
                    blocksReset++;
                }
            }
        }

        plugin.getLogger().fine("Reset " + blocksReset + " blocks to white wool in center area");
    }

    /**
     * Clean up arrows, projectiles, and dropped items around the arena
     */
    private void cleanupArenaEntities(ArenaConfig arena) {
        if (arena.centerBox == null)
            return;

        org.bukkit.World world = org.bukkit.Bukkit.getWorld(arena.world);
        if (world == null)
            return;

        // Calculate center point for cleanup
        int centerX = (arena.centerBox.x1 + arena.centerBox.x2) / 2;
        int centerZ = (arena.centerBox.z1 + arena.centerBox.z2) / 2;
        int centerY = arena.centerBox.y1;

        org.bukkit.Location center = new org.bukkit.Location(world, centerX, centerY, centerZ);

        // Cleanup radius around center (adjust based on arena size)
        int cleanupRadius = 30; // blocks from center

        int entitiesRemoved = 0;

        // Remove arrows, projectiles, and dropped items in radius
        for (org.bukkit.entity.Entity entity : world.getNearbyEntities(center, cleanupRadius, cleanupRadius,
                cleanupRadius)) {
            if (isCleanupTarget(entity)) {
                entity.remove();
                entitiesRemoved++;
            }
        }

        if (entitiesRemoved > 0) {
            plugin.getLogger().fine("Removed " + entitiesRemoved + " entities during arena cleanup");
        }
    }

    /**
     * Check if an entity should be removed during arena cleanup
     */
    private boolean isCleanupTarget(org.bukkit.entity.Entity entity) {
        return entity instanceof org.bukkit.entity.Arrow || // Arrows from bows/crossbows
                entity instanceof org.bukkit.entity.Trident || // Tridents (if added later)
                entity instanceof org.bukkit.entity.Snowball || // Snowballs (if added)
                entity instanceof org.bukkit.entity.Egg || // Eggs (if added)
                entity instanceof org.bukkit.entity.Item || // Dropped items
                entity instanceof org.bukkit.entity.ExperienceOrb; // XP orbs from deaths
    }

    /**
     * Clean up players after game ends - restore them to normal state
     */
    private void cleanupPlayers(Game game) {
        for (org.bukkit.entity.Player player : game.getPlayers()) {
            if (!VirtualPlayerUtil.canPerformNetworkOperations(player)) {
                plugin.getLogger().info("Skipping cleanup for virtual player: " + player.getName());
                continue;
            }

            // Clear player inventory completely
            player.getInventory().clear();
            player.getInventory().setArmorContents(null);

            // Clear any active potion effects
            player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));

            // Reset player health and hunger
            player.setHealth(20);
            player.setFoodLevel(20);
            player.setSaturation(20.0f);

            // Use consistent teleportation method - use PlayerService teleportToLobby
            teleportPlayerToLobby(player);

            VirtualPlayerUtil.safeSendMessage(player, ChatColor.GREEN + "Thanks for playing BattleBox!");
            VirtualPlayerUtil.safeSendMessage(player, ChatColor.GRAY + "You have been returned to the lobby.");
        }
    }

    /**
     * Teleport player to the test world (lobby) using consistent method
     */
    private void teleportPlayerToLobby(org.bukkit.entity.Player player) {
        org.bukkit.World testWorld = org.bukkit.Bukkit.getWorld("test");
        if (testWorld == null) {
            plugin.getLogger()
                    .warning("Test world not found! Cannot teleport player " + player.getName() + " to lobby");
            VirtualPlayerUtil.safeSendMessage(player,
                    ChatColor.RED + "Lobby world not available. Please contact an admin.");
            return;
        }

        // Get spawn location of test world
        org.bukkit.Location lobbySpawn = testWorld.getSpawnLocation();

        // Use PlayerService's safe teleport method for consistency
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            // Ensure chunk is loaded
            if (!lobbySpawn.getChunk().isLoaded()) {
                lobbySpawn.getChunk().load();
            }

            player.teleport(lobbySpawn);
            VirtualPlayerUtil.safeSendMessage(player, ChatColor.YELLOW + "Welcome back to the lobby!");
        });

        plugin.getLogger().info("Teleported player " + player.getName() + " to test world lobby");
    }

    /**
     * Clean up a single player (used when player leaves manually)
     */
    private void cleanupSinglePlayer(org.bukkit.entity.Player player) {
        if (!VirtualPlayerUtil.canPerformNetworkOperations(player)) {
            plugin.getLogger().info("Skipping cleanup for virtual player: " + player.getName());
            return;
        }

        // Clear player inventory completely
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);

        // Clear any active potion effects
        player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));

        // Reset player health and hunger
        player.setHealth(20);
        player.setFoodLevel(20);
        player.setSaturation(20.0f);

        // Use consistent teleportation method
        teleportPlayerToLobby(player);
    }

    private void broadcastToGame(Game game, String message) {
        for (Player player : game.getPlayers()) {
            VirtualPlayerUtil.safeSendMessage(player, message);
        }
    }

    public Game getPlayerGame(Player player) {
        return gameManager.getPlayerGame(player);
    }

    public boolean leaveGame(Player player) {
        Game game = getPlayerGame(player);
        if (game == null)
            return false;

        // Handle music for player leaving
        musicService.onPlayerLeaveGame(player, game);

        // Clean up the player before removing from game
        cleanupSinglePlayer(player);

        game.removePlayer(player);
        gameManager.removePlayerFromGame(player);

        VirtualPlayerUtil.safeSendMessage(player, ChatColor.YELLOW + "You have left the game.");
        return true;
    }

    // =================================================================
    // VICTORY FIREWORKS - VISUAL EFFECTS FOR WINNING TEAM
    // =================================================================

    /**
     * Start victory fireworks for the winning team players
     * Fireworks will spawn at player locations throughout the victory phase
     */
    private void startVictoryFireworks(Game game, Game.TeamColor winner) {
        String gameId = game.getId();
        plugin.getLogger().info("Starting victory fireworks for " + winner.displayName + " team in game " + gameId);

        // Get winning team players
        Set<Player> winningPlayers = new HashSet<>();
        for (Player player : game.getRealPlayers()) {
            if (game.getPlayerTeam(player) == winner && VirtualPlayerUtil.canPerformNetworkOperations(player)) {
                winningPlayers.add(player);
            }
        }

        if (winningPlayers.isEmpty()) {
            plugin.getLogger().info("No winning players found for fireworks in game " + gameId);
            return;
        }

        // Create fireworks task that runs every 30 ticks (1.5 seconds) for 20 seconds
        BukkitTask fireworksTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : winningPlayers) {
                if (VirtualPlayerUtil.canPerformNetworkOperations(player) && player.isOnline()) {
                    spawnVictoryFirework(player, winner);
                }
            }
        }, 20L, 30L); // Start after 1 second, repeat every 1.5 seconds

        // Store the task for cleanup
        victoryFireworksMap.put(gameId, fireworksTask);

        // Stop fireworks after 20 seconds (victory phase duration)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            stopVictoryFireworks(gameId);
        }, 400L); // 20 seconds = 400 ticks

        plugin.getLogger()
                .info("Victory fireworks started for " + winningPlayers.size() + " players in game " + gameId);
    }

    /**
     * Stop victory fireworks for a game
     */
    private void stopVictoryFireworks(String gameId) {
        BukkitTask fireworksTask = victoryFireworksMap.remove(gameId);
        if (fireworksTask != null && !fireworksTask.isCancelled()) {
            fireworksTask.cancel();
            plugin.getLogger().info("Stopped victory fireworks for game " + gameId);
        }
    }

    /**
     * Spawn a single firework at the player's location with team colors
     */
    private void spawnVictoryFirework(Player player, Game.TeamColor team) {
        try {
            Location loc = player.getLocation().add(0, 2, 0); // Spawn 2 blocks above player
            Firework firework = loc.getWorld().spawn(loc, Firework.class);
            FireworkMeta meta = firework.getFireworkMeta();

            // Create team-colored firework effect
            FireworkEffect.Builder effectBuilder = FireworkEffect.builder()
                    .with(FireworkEffect.Type.BURST)
                    .flicker(true)
                    .trail(true);

            // Set team colors
            if (team == Game.TeamColor.RED) {
                effectBuilder.withColor(Color.RED, Color.ORANGE)
                        .withFade(Color.YELLOW, Color.WHITE);
            } else { // BLUE team
                effectBuilder.withColor(Color.BLUE, Color.AQUA)
                        .withFade(Color.BLUE, Color.WHITE);
            }

            meta.addEffect(effectBuilder.build());
            meta.setPower(1); // Medium height
            firework.setFireworkMeta(meta);

            plugin.getLogger()
                    .fine("Spawned victory firework for " + player.getName() + " (" + team.displayName + " team)");

        } catch (Exception e) {
            plugin.getLogger()
                    .warning("Failed to spawn victory firework for " + player.getName() + ": " + e.getMessage());
        }
    }
}
