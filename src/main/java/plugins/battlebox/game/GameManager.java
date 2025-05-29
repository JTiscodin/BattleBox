package plugins.battlebox.game;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

public class GameManager {
    private final HashMap<String, Game> activeGames;
    private final HashMap<UUID, String> playerGameMap; // Track which game each player is in 

    public GameManager() {
        this.activeGames = new HashMap<>();
        this.playerGameMap = new HashMap<>();
    }
    
    public void createGame(String name, Game game){
        activeGames.put(name, game);
    }

    public void removeGame(String name){
        Game game = activeGames.remove(name);
        if (game != null) {
            // Remove all players from the game tracking
            playerGameMap.values().removeIf(gameName -> gameName.equals(name));
        }
    }

    public Game getGame(String name){
        return activeGames.get(name);
    }
    
    public void addPlayerToGame(Player player, String gameName) {
        playerGameMap.put(player.getUniqueId(), gameName);
    }
    
    public void removePlayerFromGame(Player player) {
        playerGameMap.remove(player.getUniqueId());
    }
    
    public Game getPlayerGame(Player player) {
        String gameName = playerGameMap.get(player.getUniqueId());
        return gameName != null ? activeGames.get(gameName) : null;
    }
      public int getActiveGameCount() {
        return activeGames.size();
    }
    
    public HashMap<String, Game> getAllGames() {
        return new HashMap<>(activeGames);
    }
    
    public java.util.Set<String> getActiveGameIds() {
        return activeGames.keySet();
    }

}
