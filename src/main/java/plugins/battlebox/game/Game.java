package plugins.battlebox.game;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Game {
    private final String id;
    private final String arenaId;
    private GameState state;
    private final Set<UUID> players;
    private final int MAX_PLAYERS = 8;

    public Game(String id, String arenaId){
        this.id = id;
        this.arenaId = arenaId;
        this.state = GameState.WAITING;
        this.players = new HashSet<>();
    }

    public String getId(){
        return id;
    }

    public String getArenaId(){
        return arenaId;
    }

    public GameState getState(){
        return state;
    }

    public void setState(GameState newState){
        this.state = newState;
    }

    public boolean addPlayer(Player player){
        if(players.contains(player.getUniqueId())){
            player.sendMessage("You are already in the game!");
            return false;
        }
        if(players.size() >= MAX_PLAYERS){
            player.sendMessage("The game is full!");
            return false;
        }
        players.add(player.getUniqueId());
        //Handle teleportation here.
        player.sendMessage("§aYou joined the game: §e" + id);
        Bukkit.getLogger().info(player.getName() + " joined game " + id);
        return true;
    }
    public void removePlayer(Player player){
        players.remove(player.getUniqueId());
    }

    public void startGame(){

        // Teleport the players to the arena spawn points (below)
        // Start a countdown, the GameState will be set to STARTING
        // After the countdown, set the GameState to IN_PROGRESS and the barrier will be removed, Game Starts
        // Handle the game logic here
        for(UUID playerId: players){
            Player player = Bukkit.getPlayer(playerId);
            if(player != null){
                //TODO: tp the player to the arena spawn point (get it from the config)
            }
        }
    }
}
