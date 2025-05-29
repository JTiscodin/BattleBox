package plugins.battlebox.managers;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.SessionManager;
import config.ArenaConfig;
import config.Box;
import config.Kit.Button;
import config.Kit.Kit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class ArenaCreationManager {
    private final ArenaManager arenaManager;
    
    // Track players currently creating arenas
    private final Map<UUID, ArenaBuilder> activeBuilders = new HashMap<>();
    
    // Track players in wizard mode
    private final Map<UUID, WizardState> wizardPlayers = new HashMap<>();
    
    public ArenaCreationManager(JavaPlugin plugin, ArenaManager arenaManager) {
        this.arenaManager = arenaManager;
    }
    
    public boolean startCreation(Player player, String arenaName) {
        // Check if arena already exists
        if (arenaExists(arenaName)) {
            return false;
        }
        
        // Check if player is already creating an arena
        if (activeBuilders.containsKey(player.getUniqueId())) {
            return false;
        }
        
        activeBuilders.put(player.getUniqueId(), new ArenaBuilder(arenaName, player.getWorld().getName()));
        return true;
    }
    
    public boolean setRegionFromSelection(Player player) {
        ArenaBuilder builder = activeBuilders.get(player.getUniqueId());
        if (builder == null) return false;
        
        try {
            SessionManager sessionManager = WorldEdit.getInstance().getSessionManager();
            LocalSession session = sessionManager.get(BukkitAdapter.adapt(player));
            Region selection = session.getSelection(BukkitAdapter.adapt(player.getWorld()));
            
            if (selection == null) {
                return false;
            }
              BlockVector3 min = selection.getMinimumPoint();
            BlockVector3 max = selection.getMaximumPoint();
            
            Box box = new Box();
            box.x1 = min.x();
            box.y1 = min.y();
            box.z1 = min.z();
            box.x2 = max.x();
            box.y2 = max.y();
            box.z2 = max.z();
            
            builder.setBox(box);
            return true;
            
        } catch (IncompleteRegionException e) {
            return false;
        }
    }
    
    public boolean addKitButton(Player player, String kitName) {
        ArenaBuilder builder = activeBuilders.get(player.getUniqueId());
        if (builder == null) return false;
        
        // Check if kit already exists
        for (Kit kit : builder.getKits()) {
            if (kit.name.equals(kitName)) {
                return false;
            }
        }
        
        Kit newKit = new Kit();
        newKit.name = kitName;
        newKit.buttons = new Button[0]; // Will be populated when player clicks buttons
        
        builder.addKit(newKit);
        builder.setWaitingForKit(kitName);
        
        return true;
    }
    
    public boolean handleButtonClick(Player player, Location location) {
        ArenaBuilder builder = activeBuilders.get(player.getUniqueId());
        if (builder == null || builder.getWaitingForKit() == null) return false;
        
        String kitName = builder.getWaitingForKit();
        
        // Find the kit and add the button location
        for (Kit kit : builder.getKits()) {
            if (kit.name.equals(kitName)) {
                Button newButton = new Button();
                newButton.x = location.getBlockX();
                newButton.y = location.getBlockY();
                newButton.z = location.getBlockZ();
                
                // Add button to existing array
                Button[] newButtons = Arrays.copyOf(kit.buttons, kit.buttons.length + 1);
                newButtons[kit.buttons.length] = newButton;
                kit.buttons = newButtons;
                
                builder.setWaitingForKit(null);
                
                player.sendMessage(ChatColor.GREEN + "Button location set for kit: " + ChatColor.YELLOW + kitName);
                player.sendMessage(ChatColor.AQUA + "Location: " + ChatColor.WHITE + 
                    location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ());
                
                return true;
            }
        }
        
        return false;
    }
    
    public boolean saveArena(Player player) {
        ArenaBuilder builder = activeBuilders.get(player.getUniqueId());
        if (builder == null || !builder.isComplete()) return false;
        
        ArenaConfig config = builder.build();
        arenaManager.addArena(config);
        arenaManager.saveArenas();
        
        activeBuilders.remove(player.getUniqueId());
        return true;
    }
    
    public void cancelCreation(Player player) {
        activeBuilders.remove(player.getUniqueId());
        wizardPlayers.remove(player.getUniqueId());
    }
    
    public boolean isCreating(Player player) {
        return activeBuilders.containsKey(player.getUniqueId());
    }
    
    public List<String> getArenaList() {
        List<String> arenaNames = new ArrayList<>();
        for (ArenaConfig arena : arenaManager.getArenas()) {
            arenaNames.add(arena.id);
        }
        return arenaNames;
    }
    
    public String getArenaInfo(String arenaName) {
        for (ArenaConfig arena : arenaManager.getArenas()) {
            if (arena.id.equals(arenaName)) {
                StringBuilder info = new StringBuilder();
                info.append(ChatColor.GOLD).append("=== Arena Info: ").append(ChatColor.YELLOW).append(arenaName).append(ChatColor.GOLD).append(" ===\n");
                info.append(ChatColor.AQUA).append("World: ").append(ChatColor.WHITE).append(arena.world).append("\n");
                info.append(ChatColor.AQUA).append("Region: ").append(ChatColor.WHITE)
                    .append(arena.box.x1).append(",").append(arena.box.y1).append(",").append(arena.box.z1)
                    .append(" to ")
                    .append(arena.box.x2).append(",").append(arena.box.y2).append(",").append(arena.box.z2).append("\n");
                
                if (arena.Kits != null && arena.Kits.length > 0) {
                    info.append(ChatColor.AQUA).append("Kits:\n");
                    for (Kit kit : arena.Kits) {
                        info.append(ChatColor.WHITE).append("  - ").append(kit.name)
                            .append(" (").append(kit.buttons.length).append(" buttons)\n");
                    }
                } else {
                    info.append(ChatColor.YELLOW).append("No kits configured\n");
                }
                
                return info.toString();
            }
        }
        return null;
    }
    
    public boolean deleteArena(String arenaName) {
        return arenaManager.removeArena(arenaName);
    }
    
    public void startWizard(Player player) {
        if (activeBuilders.containsKey(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You're already creating an arena! Use /arena cancel to stop.");
            return;
        }
        
        wizardPlayers.put(player.getUniqueId(), new WizardState(WizardStep.ARENA_NAME));
        
        player.sendMessage(ChatColor.GOLD + "=== Arena Creation Wizard ===");
        player.sendMessage(ChatColor.AQUA + "Step 1: Enter arena name in chat:");
        player.sendMessage(ChatColor.YELLOW + "Type the name for your new arena (or 'cancel' to stop)");
    }
    
    public boolean handleWizardInput(Player player, String input) {
        WizardState state = wizardPlayers.get(player.getUniqueId());
        if (state == null) return false;
        
        if (input.equalsIgnoreCase("cancel")) {
            wizardPlayers.remove(player.getUniqueId());
            player.sendMessage(ChatColor.YELLOW + "Arena creation wizard cancelled.");
            return true;
        }
          switch (state.getStep()) {
            case ARENA_NAME -> {
                if (arenaExists(input)) {
                    player.sendMessage(ChatColor.RED + "Arena with that name already exists! Try another name:");
                    return true;
                }
                
                activeBuilders.put(player.getUniqueId(), new ArenaBuilder(input, player.getWorld().getName()));
                state.setStep(WizardStep.SET_REGION);
                
                player.sendMessage(ChatColor.GREEN + "Arena name set: " + ChatColor.YELLOW + input);
                player.sendMessage(ChatColor.AQUA + "Step 2: Use WorldEdit to select the arena region");
                player.sendMessage(ChatColor.WHITE + "Use //wand to get selection tool, then use /arena setregion when ready");
                
                return true;
            }
            case SET_REGION -> {
                player.sendMessage(ChatColor.YELLOW + "Please set your region first using /arena setregion");
                return true;
            }
            case ADD_KITS -> {
                player.sendMessage(ChatColor.YELLOW + "Use /arena addkit <name> to add kits");
                return true;
            }
            case COMPLETE -> {
                player.sendMessage(ChatColor.GREEN + "Arena creation is complete! Use /arena save to finish");
                return true;
            }
        }
        
        return false;
    }
      public boolean isInWizard(Player player) {
        return wizardPlayers.containsKey(player.getUniqueId());
    }
    
    // Get arena by name for info/delete commands
    public ArenaConfig getArena(String arenaName) {
        for (ArenaConfig arena : arenaManager.getArenas()) {
            if (arena.id.equals(arenaName)) {
                return arena;
            }
        }
        return null;
    }
    
    // Get arena at specific location for coordinate checking
    public ArenaConfig getArenaAt(Location location) {
        for (ArenaConfig arena : arenaManager.getArenas()) {
            if (arena.world.equals(location.getWorld().getName())) {
                Box box = arena.box;
                int x = location.getBlockX();
                int y = location.getBlockY();
                int z = location.getBlockZ();
                
                if (x >= Math.min(box.x1, box.x2) && x <= Math.max(box.x1, box.x2) &&
                    y >= Math.min(box.y1, box.y2) && y <= Math.max(box.y1, box.y2) &&
                    z >= Math.min(box.z1, box.z2) && z <= Math.max(box.z1, box.z2)) {
                    return arena;
                }
            }
        }
        return null;
    }
    
    private boolean arenaExists(String arenaName) {
        for (ArenaConfig arena : arenaManager.getArenas()) {
            if (arena.id.equals(arenaName)) {
                return true;
            }
        }
        return false;
    }
    
    // Inner class to track arena building progress
    private static class ArenaBuilder {
        private final String arenaName;
        private final String worldName;
        private Box box;
        private final List<Kit> kits = new ArrayList<>();
        private String waitingForKit; // Kit name waiting for button click
        
        public ArenaBuilder(String arenaName, String worldName) {
            this.arenaName = arenaName;
            this.worldName = worldName;
        }
        
        public void setBox(Box box) {
            this.box = box;
        }
        
        public void addKit(Kit kit) {
            kits.add(kit);
        }
        
        public List<Kit> getKits() {
            return kits;
        }
        
        public void setWaitingForKit(String kitName) {
            this.waitingForKit = kitName;
        }
        
        public String getWaitingForKit() {
            return waitingForKit;
        }
        
        public boolean isComplete() {
            return box != null; // Minimum requirement is a region
        }
        
        public ArenaConfig build() {
            ArenaConfig config = new ArenaConfig();
            config.id = arenaName;
            config.world = worldName;
            config.box = box;
            config.Kits = kits.toArray(new Kit[0]);
            
            return config;
        }
    }
    
    // Wizard state tracking
    private static class WizardState {
        private WizardStep step;
        
        public WizardState(WizardStep step) {
            this.step = step;
        }
        
        public WizardStep getStep() {
            return step;
        }
        
        public void setStep(WizardStep step) {
            this.step = step;
        }
    }
    
    private enum WizardStep {
        ARENA_NAME,
        SET_REGION,
        ADD_KITS,
        COMPLETE
    }
}
