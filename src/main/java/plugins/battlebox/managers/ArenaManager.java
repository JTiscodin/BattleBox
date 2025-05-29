package plugins.battlebox.managers;

import config.ArenaConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.common.reflect.TypeToken;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArenaManager {
    private final JavaPlugin plugin;
    private List<ArenaConfig> arenas;
    private final File configFile;

    public ArenaManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "arenas.json");
        ensureConfigExists();
        loadArenas();
    }
    
    private void ensureConfigExists() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        
        if (!configFile.exists()) {
            plugin.saveResource("arenas.json", false);
        }
    }

    public void loadArenas() {
        Gson gson = new Gson();
        Type listType = new TypeToken<List<ArenaConfig>>() {}.getType();
        
        try {
            // Try to load from data folder first
            if (configFile.exists()) {
                try (FileReader reader = new FileReader(configFile)) {
                    JsonObject json = gson.fromJson(reader, JsonObject.class);
                    if (json != null && json.has("arenas")) {
                        arenas = gson.fromJson(json.getAsJsonArray("arenas"), listType);
                    } else {
                        arenas = new ArrayList<>();
                    }
                }
            } else {
                // Fallback to resource
                try (InputStreamReader reader = new InputStreamReader(plugin.getResource("arenas.json"))) {
                    JsonObject json = gson.fromJson(reader, JsonObject.class);
                    arenas = gson.fromJson(json.getAsJsonArray("arenas"), listType);
                }
            }
            
            if (arenas == null) {
                arenas = new ArrayList<>();
            }
            
            plugin.getLogger().info("Loaded " + arenas.size() + " arenas from config.");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load arenas.json: " + e.getMessage());
            arenas = new ArrayList<>();
        }
    }
    
    public void saveArenas() {
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            
            Map<String, Object> data = new HashMap<>();
            data.put("arenas", arenas);
            
            try (FileWriter writer = new FileWriter(configFile)) {
                gson.toJson(data, writer);
            }
            
            plugin.getLogger().info("Saved " + arenas.size() + " arenas to config.");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save arenas.json: " + e.getMessage());
        }
    }
    
    public void addArena(ArenaConfig arena) {
        // Remove existing arena with same ID if it exists
        arenas.removeIf(existing -> existing.id.equals(arena.id));
        arenas.add(arena);
    }
    
    public boolean removeArena(String arenaId) {
        boolean removed = arenas.removeIf(arena -> arena.id.equals(arenaId));
        if (removed) {
            saveArenas();
        }
        return removed;
    }
    
    public ArenaConfig getArena(String arenaId) {
        return arenas.stream()
                .filter(arena -> arena.id.equals(arenaId))
                .findFirst()
                .orElse(null);
    }

    public List<ArenaConfig> getArenas() {
        return arenas;
    }
    
    public void reloadArenas() {
        loadArenas();
    }
}
