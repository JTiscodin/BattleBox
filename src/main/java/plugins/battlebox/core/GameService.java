package plugins.battlebox.core;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import plugins.battlebox.game.Game;
import plugins.battlebox.game.GameManager;
import plugins.battlebox.game.GameState;
import plugins.battlebox.managers.ArenaManager;
import plugins.battlebox.managers.TimerManager;
import config.ArenaConfig;

/**
 * Core service for managing game lifecycle and operations.
 * Handles game creation, player joining, and game flow.
 */
public class GameService {
    
    private final GameManager gameManager;
    private final ArenaManager arenaManager;
    private final TimerManager timerManager;
    private final PlayerService playerService;
    
    public GameService(GameManager gameManager, ArenaManager arenaManager, 
                      TimerManager timerManager, PlayerService playerService) {
        this.gameManager = gameManager;
        this.arenaManager = arenaManager;
        this.timerManager = timerManager;
        this.playerService = playerService;
    }    /**
     * Create and start a new game
     */
    public boolean createGame(Player creator, String arenaId) {
        ArenaConfig arena = arenaManager.getArena(arenaId);
        if (arena == null) {
            creator.sendMessage(ChatColor.RED + "Arena '" + arenaId + "' not found!");
            return false;
        }
        
        if (!isArenaComplete(arena)) {
            creator.sendMessage(ChatColor.RED + "Arena is incomplete. Use /arena info " + arenaId + " to see what's missing.");
            return false;
        }
        
        String gameId = "game_" + System.currentTimeMillis();
        Game game = new Game(gameId, arenaId);
        gameManager.createGame(gameId, game);
        
        if (!joinGame(creator, gameId)) {
            gameManager.removeGame(gameId);
            return false;
        }
        
        startKitSelection(game);
        creator.sendMessage(ChatColor.GREEN + "Game created! Preparing arena...");
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
        return true;
    }
    
    /**
     * Start the actual battle phase
     */
    public void startBattle(Game game) {
        game.setState(GameState.IN_PROGRESS);
        
        ArenaConfig arena = arenaManager.getArena(game.getArenaId());
        for (Player player : game.getPlayers()) {
            playerService.teleportToGamePosition(player, game, arena);
        }
        
        // Start 2-minute game timer
        timerManager.startTimer("game_" + game.getId(), game.getPlayers(), 120, 
            "BATTLE BOX", () -> endGame(game));
        
        broadcastToGame(game, ChatColor.GREEN + "BATTLE STARTED! Fill the center with your wool!");
    }
    
    /**
     * End the game and determine winner
     */
    public void endGame(Game game) {
        ArenaConfig arena = arenaManager.getArena(game.getArenaId());
        game.calculateWinner(arena);
        
        announceResults(game);
        cleanupGame(game);
    }
    
    private void startKitSelection(Game game) {
        ArenaConfig arena = arenaManager.getArena(game.getArenaId());
        
        for (Player player : game.getPlayers()) {
            playerService.teleportToSpawn(player, game, arena);
            playerService.giveBaseKit(player, game.getPlayerTeam(player));
        }
        
        // Give 30 seconds for kit selection (3 seconds in single player)
        int duration = game.getPlayerCount() == 1 ? 3 : 30;
        
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
        if (game == null) return false;
        
        game.removePlayer(player);
        gameManager.removePlayerFromGame(player);
        return true;
    }
}
