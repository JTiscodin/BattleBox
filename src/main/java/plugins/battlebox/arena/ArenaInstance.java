package plugins.battlebox.arena;

import org.bukkit.Location;
import plugins.battlebox.game.Game;

public class ArenaInstance {
    private final String instanceId;
    private final ArenaTemplate template;
    private final Location instanceLocation;
    private Game currentGame;
    private boolean inUse;
    private long lastUsed;
    
    public ArenaInstance(String instanceId, ArenaTemplate template, Location instanceLocation) {
        this.instanceId = instanceId;
        this.template = template;
        this.instanceLocation = instanceLocation;
        this.inUse = false;
        this.lastUsed = System.currentTimeMillis();
    }
    
    public void startGame(Game game) {
        this.currentGame = game;
        this.inUse = true;
    }
    
    public void endGame() {
        this.currentGame = null;
        this.inUse = false;
        this.lastUsed = System.currentTimeMillis();
    }
    
    // Getters and setters
    public String getInstanceId() { 
        return instanceId; 
    }
    
    public ArenaTemplate getTemplate() { 
        return template; 
    }
    
    public Location getInstanceLocation() { 
        return instanceLocation; 
    }
    
    public Game getCurrentGame() { 
        return currentGame; 
    }
    
    public boolean isInUse() { 
        return inUse; 
    }
    
    public long getLastUsed() { 
        return lastUsed; 
    }
    
    @Override
    public String toString() {
        return "ArenaInstance{" +
                "instanceId='" + instanceId + '\'' +
                ", template=" + template.getTemplateId() +
                ", inUse=" + inUse +
                '}';
    }
}
