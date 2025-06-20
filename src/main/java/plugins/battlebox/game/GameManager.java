package plugins.battlebox.game;

import org.bukkit.entity.Player;
import plugins.battlebox.arena.ArenaInstance;
import plugins.battlebox.managers.ArenaInstanceManager;

import java.util.HashMap;
import java.util.Set;

public class GameManager {
    private final HashMap<String, Game> activeGames;
    private final ArenaInstanceManager arenaInstanceManager;

    public GameManager(ArenaInstanceManager arenaInstanceManager) {
        this.activeGames = new HashMap<>();
        this.arenaInstanceManager = arenaInstanceManager;
    }
    
    public boolean createGame(String gameId, String arenaId, String templateId) {
        if (activeGames.containsKey(gameId)) {
            return false; // Game already exists
        }
        
        Game game = new Game(gameId, arenaId);
        
        // Assign arena instance to the game
        ArenaInstance instance = arenaInstanceManager.assignArenaToGame(templateId, game);
        if (instance == null) {
            return false; // No available arena instance
        }
        
        game.setArenaLocation(instance.getInstanceLocation());
        activeGames.put(gameId, game);
        return true;
    }
    
    public void createGame(String name, Game game) {
        activeGames.put(name, game);
    }

    public void removeGame(String gameId) {
        Game game = activeGames.get(gameId);
        if (game != null) {
            // Find and release the arena instance
            for (ArenaInstance instance : arenaInstanceManager.getInstances().values()) {
                if (instance.getCurrentGame() == game) {
                    arenaInstanceManager.releaseArena(instance);
                    break;
                }
            }
        }
        activeGames.remove(gameId);
    }

    public Game getGame(String name) {
        return activeGames.get(name);
    }
    
    public HashMap<String, Game> getActiveGames() {
        return new HashMap<>(activeGames);
    }
    
    // Player management methods
    public boolean addPlayerToGame(Player player, String gameId) {
        Game game = activeGames.get(gameId);
        if (game == null) {
            return false;
        }
        return game.addPlayer(player);
    }
    
    public boolean removePlayerFromGame(Player player) {
        for (Game game : activeGames.values()) {
            if (game.getPlayers().contains(player)) {
                game.removePlayer(player);
                return true;
            }
        }
        return false;
    }
    
    public Game getPlayerGame(Player player) {
        for (Game game : activeGames.values()) {
            if (game.getPlayers().contains(player)) {
                return game;
            }
        }
        return null;
    }
    
    // Game statistics methods
    public int getActiveGameCount() {
        return activeGames.size();
    }
    
    public Set<String> getActiveGameIds() {
        return activeGames.keySet();
    }
}
