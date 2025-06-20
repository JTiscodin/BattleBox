package plugins.battlebox.arena;

import org.bukkit.Location;
import config.ArenaConfig;

public class ArenaTemplate {
    private final String templateId;
    private final String schematicName;
    private final Location baseLocation;
    private final ArenaConfig config;
    private final int maxInstances;
    
    public ArenaTemplate(String templateId, String schematicName, Location baseLocation, ArenaConfig config, int maxInstances) {
        this.templateId = templateId;
        this.schematicName = schematicName;
        this.baseLocation = baseLocation;
        this.config = config;
        this.maxInstances = maxInstances;
    }
    
    // Getters
    public String getTemplateId() { 
        return templateId; 
    }
    
    public String getSchematicName() { 
        return schematicName; 
    }
    
    public Location getBaseLocation() { 
        return baseLocation; 
    }
    
    public ArenaConfig getConfig() { 
        return config; 
    }
    
    public int getMaxInstances() { 
        return maxInstances; 
    }
    
    @Override
    public String toString() {
        return "ArenaTemplate{" +
                "templateId='" + templateId + '\'' +
                ", schematicName='" + schematicName + '\'' +
                ", maxInstances=" + maxInstances +
                '}';
    }
}
