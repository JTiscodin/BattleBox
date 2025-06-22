package plugins.battlebox.core;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import config.ArenaConfig;
import plugins.battlebox.game.Game;
import plugins.battlebox.game.GameManager;
import plugins.battlebox.game.GameState;
import plugins.battlebox.managers.ArenaManager;
import plugins.battlebox.managers.TimerManager;

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

    public GameService(GameManager gameManager, ArenaManager arenaManager,
            TimerManager timerManager, PlayerService playerService, MusicService musicService) {
        this.gameManager = gameManager;
        this.arenaManager = arenaManager;
        this.timerManager = timerManager;
        this.playerService = playerService;
        this.musicService = musicService;
    }

    /**
     * Create and start a new game
     */
    public boolean createGame(Player creator, String arenaId) {
        ArenaConfig arena = arenaManager.getArena(arenaId);
        if (arena == null) {
            creator.sendMessage(ChatColor.RED + "Arena '" + arenaId + "' not found!");
            return false;
        }

        if (!isArenaComplete(arena)) {
            creator.sendMessage(
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

        creator.sendMessage(ChatColor.GREEN + "Game created! Waiting for more players...");
        return true;
    }

    /**
     * Join an existing game
     */
    public boolean joinGame(Player player, String gameId) {
        Game game = gameManager.getGame(gameId);
        if (game == null) {
            player.sendMessage(ChatColor.RED + "Game not found");
            return false;
        }

        if (!game.addPlayer(player)) {
            player.sendMessage(ChatColor.RED + "Cannot join game (possibly full)");
            return false;
        }

        gameManager.addPlayerToGame(player, gameId);
        playerService.setupPlayerForGame(player, game);

        // Handle music for player joining
        musicService.onPlayerJoinGame(player, game);

        // Handle game state based on current state and player count
        handlePlayerJoinGameFlow(game, player);

        return true;
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
        } else if (game.getState() == GameState.KIT_SELECTION) {
            // Game is already in kit selection, teleport to spawn and give base kit
            playerService.teleportToSpawn(player, game, arena);
            playerService.giveBaseKit(player, game.getPlayerTeam(player));
            player.sendMessage(ChatColor.YELLOW + "Kit selection is in progress! Choose your kit!");
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
                    musicService.updateGameMusic(game);

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

        announceResults(game);
        cleanupGame(game);
    }

    private void startKitSelection(Game game) {
        game.setState(GameState.KIT_SELECTION);

        // Update music for state change
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
        gameManager.removeGame(game.getId());
        // TODO: Arena should be reset in that world
    }

    private void broadcastToGame(Game game, String message) {
        for (Player player : game.getPlayers()) {
            player.sendMessage(message);
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

        game.removePlayer(player);
        gameManager.removePlayerFromGame(player);
        return true;
    }
}
