package plugins.battlebox.managers;

import config.ArenaConfig;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.common.reflect.TypeToken;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.List;

public class ArenaManager {
    private final JavaPlugin plugin;
    private List<ArenaConfig> arenas;

    public ArenaManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadArenas();
    }

    public void loadArenas() {
        Gson gson = new Gson();
        Type listType = new TypeToken<List<ArenaConfig>>() {}.getType();
        try (InputStreamReader reader = new InputStreamReader(plugin.getResource("arenas.json"))) {
            JsonObject json = gson.fromJson(reader, JsonObject.class);
            arenas = gson.fromJson(json.getAsJsonArray("arenas"), listType);
            plugin.getLogger().info("Loaded " + arenas.size() + " arenas from config.");
        } catch (Exception e) {
            throw new RuntimeException("Failed to load arenas.json", e);
        }
    }

    public List<ArenaConfig> getArenas() {
        return arenas;
    }
}
