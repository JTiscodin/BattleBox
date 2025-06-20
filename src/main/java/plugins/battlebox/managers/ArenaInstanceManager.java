package plugins.battlebox.managers;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import plugins.battlebox.BattleBox;
import plugins.battlebox.arena.ArenaInstance;
import plugins.battlebox.arena.ArenaTemplate;
import plugins.battlebox.game.Game;
import config.ArenaConfig;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ArenaInstanceManager {
    private final BattleBox plugin;
    private final Map<String, ArenaTemplate> templates;
    private final Map<String, ArenaInstance> instances;
    private final Map<String, List<String>> templateInstances; // templateId -> list of instanceIds
    private final int INSTANCE_SPACING = 150; // blocks apart
    
    public ArenaInstanceManager(BattleBox plugin) {
        this.plugin = plugin;
        this.templates = new ConcurrentHashMap<>();
        this.instances = new ConcurrentHashMap<>();
        this.templateInstances = new ConcurrentHashMap<>();
    }
    
    /**
     * Register a new arena template
     */
    public boolean registerTemplate(String templateId, String schematicName, Location baseLocation, 
                                   ArenaConfig config, int maxInstances) {
        if (templates.containsKey(templateId)) {
            return false;
        }
        
        ArenaTemplate template = new ArenaTemplate(templateId, schematicName, baseLocation, config, maxInstances);
        templates.put(templateId, template);
        templateInstances.put(templateId, new ArrayList<>());
        
        plugin.getLogger().info("Registered arena template: " + templateId);
        return true;
    }
    
    /**
     * Create a new arena instance from a template
     */
    public ArenaInstance createInstance(String templateId) {
        ArenaTemplate template = templates.get(templateId);
        if (template == null) {
            return null;
        }
        
        List<String> instanceIds = templateInstances.get(templateId);
        if (instanceIds.size() >= template.getMaxInstances()) {
            plugin.getLogger().warning("Maximum instances reached for template: " + templateId);
            return null;
        }
        
        String instanceId = templateId + "_instance_" + (instanceIds.size() + 1);
        Location instanceLocation = calculateInstanceLocation(template.getBaseLocation(), instanceIds.size());
        
        // Paste the schematic at the instance location
        plugin.pasteSchematic(template.getSchematicName(), instanceLocation);
        
        ArenaInstance instance = new ArenaInstance(instanceId, template, instanceLocation);
        instances.put(instanceId, instance);
        instanceIds.add(instanceId);
        
        plugin.getLogger().info("Created arena instance: " + instanceId + " at " + 
                               instanceLocation.getBlockX() + ", " + instanceLocation.getBlockY() + ", " + instanceLocation.getBlockZ());
        return instance;
    }
    
    /**
     * Get an available arena instance for a template
     */
    public ArenaInstance getAvailableInstance(String templateId) {
        List<String> instanceIds = templateInstances.get(templateId);
        if (instanceIds == null) {
            return null;
        }
        
        // Look for an available instance
        for (String instanceId : instanceIds) {
            ArenaInstance instance = instances.get(instanceId);
            if (instance != null && !instance.isInUse()) {
                return instance;
            }
        }
        
        // No available instance, try to create a new one
        return createInstance(templateId);
    }
    
    /**
     * Assign an arena to a game
     */
    public ArenaInstance assignArenaToGame(String templateId, Game game) {
        ArenaInstance instance = getAvailableInstance(templateId);
        if (instance != null) {
            instance.startGame(game);
            plugin.getLogger().info("Assigned arena instance " + instance.getInstanceId() + " to game " + game.getId());
        }
        return instance;
    }
    
    /**
     * Release an arena from a game and reset it
     */
    public void releaseArena(ArenaInstance instance) {
        if (instance == null) return;
        
        Game game = instance.getCurrentGame();
        String gameId = game != null ? game.getId() : "unknown";
        
        instance.endGame();
        resetArena(instance);
        
        plugin.getLogger().info("Released arena instance " + instance.getInstanceId() + " from game " + gameId);
    }
    
    /**
     * Reset an arena instance by re-pasting the schematic
     */
    public void resetArena(ArenaInstance instance) {
        if (instance == null) return;
        
        ArenaTemplate template = instance.getTemplate();
        plugin.pasteSchematic(template.getSchematicName(), instance.getInstanceLocation());
        
        plugin.getLogger().info("Reset arena instance: " + instance.getInstanceId());
    }
    
    /**
     * Calculate the location for a new instance based on spacing
     */
    private Location calculateInstanceLocation(Location baseLocation, int instanceIndex) {
        // Arrange instances in a grid pattern
        int gridSize = 5; // 5x5 grid max
        int row = instanceIndex / gridSize;
        int col = instanceIndex % gridSize;
        
        double x = baseLocation.getX() + (col * INSTANCE_SPACING);
        double z = baseLocation.getZ() + (row * INSTANCE_SPACING);
        
        return new Location(baseLocation.getWorld(), x, baseLocation.getY(), z);
    }
    
    /**
     * Get all registered templates
     */
    public Map<String, ArenaTemplate> getTemplates() {
        return new HashMap<>(templates);
    }
    
    /**
     * Get all instances
     */
    public Map<String, ArenaInstance> getInstances() {
        return new HashMap<>(instances);
    }
    
    /**
     * Get instances for a specific template
     */
    public List<ArenaInstance> getInstancesForTemplate(String templateId) {
        List<String> instanceIds = templateInstances.get(templateId);
        if (instanceIds == null) {
            return new ArrayList<>();
        }
        
        List<ArenaInstance> result = new ArrayList<>();
        for (String instanceId : instanceIds) {
            ArenaInstance instance = instances.get(instanceId);
            if (instance != null) {
                result.add(instance);
            }
        }
        return result;
    }
    
    /**
     * Get an arena instance by ID
     */
    public ArenaInstance getInstance(String instanceId) {
        return instances.get(instanceId);
    }
    
    /**
     * Get an arena template by ID
     */
    public ArenaTemplate getTemplate(String templateId) {
        return templates.get(templateId);
    }
    
    /**
     * Remove a template and all its instances
     */
    public boolean removeTemplate(String templateId) {
        ArenaTemplate template = templates.get(templateId);
        if (template == null) {
            return false;
        }
        
        // Remove all instances of this template
        List<String> instanceIds = templateInstances.get(templateId);
        if (instanceIds != null) {
            for (String instanceId : instanceIds) {
                ArenaInstance instance = instances.get(instanceId);
                if (instance != null && instance.isInUse()) {
                    // Can't remove template with active instances
                    return false;
                }
            }
            
            // Remove all instances
            for (String instanceId : instanceIds) {
                instances.remove(instanceId);
            }
        }
        
        templates.remove(templateId);
        templateInstances.remove(templateId);
        
        plugin.getLogger().info("Removed arena template: " + templateId);
        return true;
    }
    
    /**
     * Get usage statistics
     */
    public Map<String, Object> getUsageStats() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalTemplates = templates.size();
        int totalInstances = instances.size();
        int activeInstances = 0;
        
        for (ArenaInstance instance : instances.values()) {
            if (instance.isInUse()) {
                activeInstances++;
            }
        }
        
        stats.put("totalTemplates", totalTemplates);
        stats.put("totalInstances", totalInstances);
        stats.put("activeInstances", activeInstances);
        stats.put("availableInstances", totalInstances - activeInstances);
        
        return stats;
    }
    
    /**
     * Send detailed info about templates and instances to a player
     */
    public void sendDetailedInfo(Player player) {
        player.sendMessage("§6=== Arena Instance Manager ===");
        
        Map<String, Object> stats = getUsageStats();
        player.sendMessage("§7Templates: §f" + stats.get("totalTemplates"));
        player.sendMessage("§7Total Instances: §f" + stats.get("totalInstances"));
        player.sendMessage("§7Active: §a" + stats.get("activeInstances") + " §7Available: §2" + stats.get("availableInstances"));
        
        if (templates.isEmpty()) {
            player.sendMessage("§cNo templates registered.");
            return;
        }
        
        player.sendMessage("\n§6Templates:");
        for (ArenaTemplate template : templates.values()) {
            List<ArenaInstance> templateInstances = getInstancesForTemplate(template.getTemplateId());
            int active = 0;
            for (ArenaInstance instance : templateInstances) {
                if (instance.isInUse()) active++;
            }
            
            player.sendMessage("§7- §f" + template.getTemplateId() + " §7(§a" + active + "§7/§2" + 
                             templateInstances.size() + "§7/§6" + template.getMaxInstances() + "§7)");
        }
    }
}