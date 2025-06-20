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
    
    // Pre-defined kit types from BattleBox.md
    private static final String[] KIT_TYPES = {"healer", "fighter", "sniper", "speedster"};
    
    public ArenaCreationManager(JavaPlugin plugin, ArenaManager arenaManager) {
        this.arenaManager = arenaManager;
    }
    
    /**
     * Start creating a new arena
     */
    public boolean startArenaCreation(Player player, String arenaName) {
        if (arenaExists(arenaName)) {
            player.sendMessage(ChatColor.RED + "Arena '" + arenaName + "' already exists!");
            return false;
        }
        
        if (activeBuilders.containsKey(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You're already creating an arena! Use /arena cancel to stop.");
            return false;
        }
        
        activeBuilders.put(player.getUniqueId(), new ArenaBuilder(arenaName, player.getWorld().getName()));
        
        player.sendMessage(ChatColor.GOLD + "=== Arena Creation Started ===");
        player.sendMessage(ChatColor.GREEN + "Arena: " + ChatColor.YELLOW + arenaName);
        player.sendMessage(ChatColor.AQUA + "World: " + ChatColor.WHITE + player.getWorld().getName());
        player.sendMessage("");
        player.sendMessage(ChatColor.YELLOW + "Next Steps:");
        player.sendMessage(ChatColor.WHITE + "1. Use WorldEdit to select the 3x3 center area");
        player.sendMessage(ChatColor.WHITE + "2. Run: " + ChatColor.AQUA + "/arena setcenter");
        player.sendMessage(ChatColor.WHITE + "3. Set team spawn points and teleport locations");
        player.sendMessage(ChatColor.WHITE + "4. Set kit selection buttons");
        player.sendMessage(ChatColor.WHITE + "5. Save the arena");
        
        return true;
    }
    
    /**
     * Set the center 3x3 wool placement area from WorldEdit selection
     */
    public boolean setCenterFromSelection(Player player) {
        ArenaBuilder builder = activeBuilders.get(player.getUniqueId());
        if (builder == null) {
            player.sendMessage(ChatColor.RED + "You're not creating an arena!");
            return false;
        }
        
        try {
            SessionManager sessionManager = WorldEdit.getInstance().getSessionManager();
            LocalSession session = sessionManager.get(BukkitAdapter.adapt(player));
            Region selection = session.getSelection(BukkitAdapter.adapt(player.getWorld()));
            
            if (selection == null) {
                player.sendMessage(ChatColor.RED + "You need to make a WorldEdit selection first!");
                return false;
            }
            
            BlockVector3 min = selection.getMinimumPoint();
            BlockVector3 max = selection.getMaximumPoint();
            
            // Validate it's a 3x3 area (on one Y level)
            int sizeX = Math.abs(max.x() - min.x()) + 1;
            int sizeZ = Math.abs(max.z() - min.z()) + 1;
            
            if (sizeX != 3 || sizeZ != 3) {
                player.sendMessage(ChatColor.RED + "Center area must be exactly 3x3! Current size: " + sizeX + "x" + sizeZ);
                return false;
            }
            
            Box centerBox = new Box(min.x(), min.y(), min.z(), max.x(), max.y(), max.z());
            builder.setCenterBox(centerBox);
            
            player.sendMessage(ChatColor.GREEN + "Center area set successfully!");
            player.sendMessage(ChatColor.AQUA + "Location: " + ChatColor.WHITE + 
                min.x() + "," + min.y() + "," + min.z() + " to " + max.x() + "," + max.y() + "," + max.z());
            
            showNextSteps(player, builder);
            return true;
            
        } catch (IncompleteRegionException e) {
            player.sendMessage(ChatColor.RED + "You need to make a WorldEdit selection first!");
            return false;
        }
    }
    
    /**
     * Set team spawn location
     */
    public boolean setTeamSpawn(Player player, String team) {
        ArenaBuilder builder = activeBuilders.get(player.getUniqueId());
        if (builder == null) {
            player.sendMessage(ChatColor.RED + "You're not creating an arena!");
            return false;
        }
        
        if (!team.equalsIgnoreCase("red") && !team.equalsIgnoreCase("blue")) {
            player.sendMessage(ChatColor.RED + "Team must be 'red' or 'blue'!");
            return false;
        }
        
        Location loc = player.getLocation();
        ArenaConfig.Location spawn = ArenaConfig.Location.fromBukkitLocation(loc);
        
        if (team.equalsIgnoreCase("red")) {
            builder.setRedSpawn(spawn);
            player.sendMessage(ChatColor.RED + "Red team spawn" + ChatColor.GREEN + " set at your location!");
        } else {
            builder.setBlueSpawn(spawn);
            player.sendMessage(ChatColor.BLUE + "Blue team spawn" + ChatColor.GREEN + " set at your location!");
        }
        
        player.sendMessage(ChatColor.AQUA + "Location: " + ChatColor.WHITE + 
            String.format("%.1f, %.1f, %.1f (Yaw: %.1f)", loc.getX(), loc.getY(), loc.getZ(), loc.getYaw()));
        
        showNextSteps(player, builder);
        return true;
    }
    
    /**
     * Set team teleport location (game start position)
     */
    public boolean setTeamTeleport(Player player, String team) {
        ArenaBuilder builder = activeBuilders.get(player.getUniqueId());
        if (builder == null) {
            player.sendMessage(ChatColor.RED + "You're not creating an arena!");
            return false;
        }
        
        if (!team.equalsIgnoreCase("red") && !team.equalsIgnoreCase("blue")) {
            player.sendMessage(ChatColor.RED + "Team must be 'red' or 'blue'!");
            return false;
        }
        
        Location loc = player.getLocation();
        ArenaConfig.Location teleport = ArenaConfig.Location.fromBukkitLocation(loc);
        
        if (team.equalsIgnoreCase("red")) {
            builder.setRedTeleport(teleport);
            player.sendMessage(ChatColor.RED + "Red team teleport" + ChatColor.GREEN + " set at your location!");
        } else {
            builder.setBlueTeleport(teleport);
            player.sendMessage(ChatColor.BLUE + "Blue team teleport" + ChatColor.GREEN + " set at your location!");
        }
        
        player.sendMessage(ChatColor.AQUA + "Location: " + ChatColor.WHITE + 
            String.format("%.1f, %.1f, %.1f (Yaw: %.1f)", loc.getX(), loc.getY(), loc.getZ(), loc.getYaw()));
        
        showNextSteps(player, builder);
        return true;
    }
    
    /**
     * Start setting kit buttons for a specific team and kit type
     */
    public boolean startKitSetup(Player player, String team, String kitType) {
        ArenaBuilder builder = activeBuilders.get(player.getUniqueId());
        if (builder == null) {
            player.sendMessage(ChatColor.RED + "You're not creating an arena!");
            return false;
        }
        
        if (!team.equalsIgnoreCase("red") && !team.equalsIgnoreCase("blue")) {
            player.sendMessage(ChatColor.RED + "Team must be 'red' or 'blue'!");
            return false;
        }
        
        if (!Arrays.asList(KIT_TYPES).contains(kitType.toLowerCase())) {
            player.sendMessage(ChatColor.RED + "Invalid kit type! Available: healer, fighter, sniper, speedster");
            return false;
        }
        
        String kitName = team.toLowerCase() + "_" + kitType.toLowerCase();
        
        // Check if kit already exists
        if (builder.hasKit(kitName)) {
            player.sendMessage(ChatColor.RED + "Kit " + kitName + " already exists!");
            return false;
        }
        
        Kit kit = new Kit(kitName, team.toLowerCase());
        builder.addKit(kit);
        builder.setWaitingForKit(kitName);
        
        player.sendMessage(ChatColor.GREEN + "Kit setup started: " + ChatColor.YELLOW + kitName);
        player.sendMessage(ChatColor.AQUA + "Now click on a button/pressure plate to set as kit selection button");
        player.sendMessage(ChatColor.GRAY + "Use " + ChatColor.WHITE + "/arena cancelkit" + ChatColor.GRAY + " to cancel");
        
        return true;
    }
    
    /**
     * Handle button click during kit setup
     */
    public boolean handleKitButtonClick(Player player, Location location) {
        ArenaBuilder builder = activeBuilders.get(player.getUniqueId());
        if (builder == null || builder.getWaitingForKit() == null) return false;
        
        String kitName = builder.getWaitingForKit();
        Kit kit = builder.getKit(kitName);
        
        if (kit == null) return false;
        
        Button newButton = new Button();
        newButton.x = location.getBlockX();
        newButton.y = location.getBlockY();
        newButton.z = location.getBlockZ();
        
        // Add button to kit
        Button[] newButtons = Arrays.copyOf(kit.buttons, kit.buttons.length + 1);
        newButtons[kit.buttons.length] = newButton;
        kit.buttons = newButtons;
        
        builder.setWaitingForKit(null);
        
        player.sendMessage(ChatColor.GREEN + "Kit button set for: " + ChatColor.YELLOW + kitName);
        player.sendMessage(ChatColor.AQUA + "Location: " + ChatColor.WHITE + 
            location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ());
        
        showNextSteps(player, builder);
        return true;
    }
    
    /**
     * Cancel current kit setup
     */
    public boolean cancelKit(Player player) {
        ArenaBuilder builder = activeBuilders.get(player.getUniqueId());
        if (builder == null) {
            player.sendMessage(ChatColor.RED + "You're not creating an arena!");
            return false;
        }
        
        if (builder.getWaitingForKit() == null) {
            player.sendMessage(ChatColor.RED + "You're not currently setting up a kit!");
            return false;
        }
        
        String kitName = builder.getWaitingForKit();
        builder.removeKit(kitName);
        builder.setWaitingForKit(null);
        
        player.sendMessage(ChatColor.YELLOW + "Kit setup cancelled: " + kitName);
        return true;
    }
    
    /**
     * Save the arena
     */
    public boolean saveArena(Player player) {
        ArenaBuilder builder = activeBuilders.get(player.getUniqueId());
        if (builder == null) {
            player.sendMessage(ChatColor.RED + "You're not creating an arena!");
            return false;
        }
        
        if (!builder.isComplete()) {
            player.sendMessage(ChatColor.RED + "Arena is not complete! Missing:");
            for (String missing : builder.getMissingComponents()) {
                player.sendMessage(ChatColor.YELLOW + "- " + missing);
            }
            return false;
        }
        
        ArenaConfig config = builder.build();
        arenaManager.addArena(config);
        arenaManager.saveArenas();
        
        activeBuilders.remove(player.getUniqueId());
        
        player.sendMessage(ChatColor.GOLD + "=== Arena Saved Successfully ===");
        player.sendMessage(ChatColor.GREEN + "Arena: " + ChatColor.YELLOW + config.id);
        player.sendMessage(ChatColor.GREEN + "World: " + ChatColor.WHITE + config.world);
        player.sendMessage(ChatColor.GREEN + "You can now use this arena to create games!");
        
        return true;
    }
    
    /**
     * Cancel arena creation
     */
    public void cancelCreation(Player player) {
        ArenaBuilder builder = activeBuilders.remove(player.getUniqueId());
        if (builder != null) {
            player.sendMessage(ChatColor.YELLOW + "Arena creation cancelled: " + builder.getArenaName());
        }
    }
    
    /**
     * Check if player is creating an arena
     */
    public boolean isCreating(Player player) {
        return activeBuilders.containsKey(player.getUniqueId());
    }
    
    /**
     * Check if player is waiting for kit button click
     */
    public boolean isWaitingForKitButton(Player player) {
        ArenaBuilder builder = activeBuilders.get(player.getUniqueId());
        return builder != null && builder.getWaitingForKit() != null;
    }
    
    /**
     * Get arena progress info
     */
    public String getArenaProgress(Player player) {
        ArenaBuilder builder = activeBuilders.get(player.getUniqueId());
        if (builder == null) {
            return ChatColor.RED + "You're not creating an arena!";
        }
        
        return builder.getProgressInfo();
    }
    
    /**
     * List all existing arenas
     */
    public List<String> getArenaList() {
        List<String> arenaNames = new ArrayList<>();
        for (ArenaConfig arena : arenaManager.getArenas()) {
            arenaNames.add(arena.id);
        }
        return arenaNames;
    }
    
    /**
     * Get detailed arena information
     */
    public String getArenaInfo(String arenaName) {
        for (ArenaConfig arena : arenaManager.getArenas()) {
            if (arena.id.equals(arenaName)) {
                StringBuilder info = new StringBuilder();
                info.append(ChatColor.GOLD).append("=== Arena Info: ").append(ChatColor.YELLOW).append(arenaName).append(ChatColor.GOLD).append(" ===\n");
                info.append(ChatColor.AQUA).append("World: ").append(ChatColor.WHITE).append(arena.world).append("\n");
                
                if (arena.centerBox != null) {
                    info.append(ChatColor.AQUA).append("Center Box: ").append(ChatColor.WHITE)
                        .append(arena.centerBox.x1).append(",").append(arena.centerBox.y1).append(",").append(arena.centerBox.z1)
                        .append(" to ")
                        .append(arena.centerBox.x2).append(",").append(arena.centerBox.y2).append(",").append(arena.centerBox.z2).append("\n");
                }
                
                if (arena.teamSpawns != null) {
                    if (arena.teamSpawns.redSpawn != null) {
                        info.append(ChatColor.RED).append("Red Spawn: ").append(ChatColor.WHITE)
                            .append(String.format("%.1f, %.1f, %.1f", arena.teamSpawns.redSpawn.x, arena.teamSpawns.redSpawn.y, arena.teamSpawns.redSpawn.z)).append("\n");
                    }
                    if (arena.teamSpawns.blueSpawn != null) {
                        info.append(ChatColor.BLUE).append("Blue Spawn: ").append(ChatColor.WHITE)
                            .append(String.format("%.1f, %.1f, %.1f", arena.teamSpawns.blueSpawn.x, arena.teamSpawns.blueSpawn.y, arena.teamSpawns.blueSpawn.z)).append("\n");
                    }
                }
                
                if (arena.kits != null && arena.kits.length > 0) {
                    info.append(ChatColor.AQUA).append("Kits: ").append(ChatColor.WHITE).append(arena.kits.length).append("\n");
                    for (Kit kit : arena.kits) {
                        info.append(ChatColor.WHITE).append("  - ").append(kit.name).append(" (").append(kit.buttons.length).append(" buttons)\n");
                    }
                }
                
                return info.toString();
            }
        }
        return ChatColor.RED + "Arena '" + arenaName + "' not found!";
    }
    
    /**
     * Delete an arena
     */
    public boolean deleteArena(String arenaName) {
        boolean removed = arenaManager.removeArena(arenaName);
        if (removed) {
            arenaManager.saveArenas();
        }
        return removed;
    }
    
    /**
     * Get arena by name
     */
    public ArenaConfig getArena(String arenaName) {
        for (ArenaConfig arena : arenaManager.getArenas()) {
            if (arena.id.equals(arenaName)) {
                return arena;
            }
        }
        return null;
    }
    
    private boolean arenaExists(String arenaName) {
        return getArena(arenaName) != null;
    }
    
    private void showNextSteps(Player player, ArenaBuilder builder) {
        List<String> missing = builder.getMissingComponents();
        if (missing.isEmpty()) {
            player.sendMessage(ChatColor.GREEN + "Arena is complete! Use " + ChatColor.YELLOW + "/arena save" + ChatColor.GREEN + " to finish.");
        } else {
            player.sendMessage(ChatColor.YELLOW + "Still needed:");
            for (String component : missing) {
                player.sendMessage(ChatColor.WHITE + "- " + component);
            }
        }
    }
    
    // Inner class to track arena building progress
    private static class ArenaBuilder {
        private final String arenaName;
        private final String worldName;
        private Box centerBox;
        private ArenaConfig.Location redSpawn, blueSpawn, redTeleport, blueTeleport;
        private final List<Kit> kits = new ArrayList<>();
        private String waitingForKit;
        
        public ArenaBuilder(String arenaName, String worldName) {
            this.arenaName = arenaName;
            this.worldName = worldName;
        }
        
        public String getArenaName() { return arenaName; }
        
        public void setCenterBox(Box box) { this.centerBox = box; }
        public void setRedSpawn(ArenaConfig.Location spawn) { this.redSpawn = spawn; }
        public void setBlueSpawn(ArenaConfig.Location spawn) { this.blueSpawn = spawn; }
        public void setRedTeleport(ArenaConfig.Location teleport) { this.redTeleport = teleport; }
        public void setBlueTeleport(ArenaConfig.Location teleport) { this.blueTeleport = teleport; }
        
        public void addKit(Kit kit) { kits.add(kit); }
        public void removeKit(String kitName) { kits.removeIf(kit -> kit.name.equals(kitName)); }
        public boolean hasKit(String kitName) { return kits.stream().anyMatch(kit -> kit.name.equals(kitName)); }
        public Kit getKit(String kitName) { return kits.stream().filter(kit -> kit.name.equals(kitName)).findFirst().orElse(null); }
        
        public void setWaitingForKit(String kitName) { this.waitingForKit = kitName; }
        public String getWaitingForKit() { return waitingForKit; }
        
        public boolean isComplete() {
            return centerBox != null && 
                   redSpawn != null && blueSpawn != null &&
                   redTeleport != null && blueTeleport != null &&
                   hasAllRequiredKits();
        }
        
        private boolean hasAllRequiredKits() {
            Set<String> requiredKits = new HashSet<>();
            for (String team : new String[]{"red", "blue"}) {
                for (String kitType : KIT_TYPES) {
                    requiredKits.add(team + "_" + kitType);
                }
            }
            
            Set<String> existingKits = new HashSet<>();
            for (Kit kit : kits) {
                if (kit.buttons.length > 0) { // Only count kits with buttons
                    existingKits.add(kit.name);
                }
            }
            
            return existingKits.containsAll(requiredKits);
        }
        
        public List<String> getMissingComponents() {
            List<String> missing = new ArrayList<>();
            
            if (centerBox == null) missing.add("Center 3x3 area (/arena setcenter)");
            if (redSpawn == null) missing.add("Red team spawn (/arena setspawn red)");
            if (blueSpawn == null) missing.add("Blue team spawn (/arena setspawn blue)");
            if (redTeleport == null) missing.add("Red team teleport (/arena setteleport red)");
            if (blueTeleport == null) missing.add("Blue team teleport (/arena setteleport blue)");
            
            // Check for missing kits
            Set<String> requiredKits = new HashSet<>();
            for (String team : new String[]{"red", "blue"}) {
                for (String kitType : KIT_TYPES) {
                    requiredKits.add(team + "_" + kitType);
                }
            }
            
            Set<String> existingKits = new HashSet<>();
            for (Kit kit : kits) {
                if (kit.buttons.length > 0) {
                    existingKits.add(kit.name);
                }
            }
            
            for (String required : requiredKits) {
                if (!existingKits.contains(required)) {
                    String[] parts = required.split("_");
                    missing.add(parts[0].toUpperCase() + " " + parts[1] + " kit (/arena addkit " + parts[0] + " " + parts[1] + ")");
                }
            }
            
            return missing;
        }
        
        public String getProgressInfo() {
            StringBuilder info = new StringBuilder();
            info.append(ChatColor.GOLD).append("=== Arena Progress: ").append(ChatColor.YELLOW).append(arenaName).append(ChatColor.GOLD).append(" ===\n");
            info.append(ChatColor.AQUA).append("World: ").append(ChatColor.WHITE).append(worldName).append("\n");
            
            info.append(centerBox != null ? ChatColor.GREEN + "✓" : ChatColor.RED + "✗").append(ChatColor.WHITE).append(" Center area\n");
            info.append(redSpawn != null ? ChatColor.GREEN + "✓" : ChatColor.RED + "✗").append(ChatColor.WHITE).append(" Red spawn\n");
            info.append(blueSpawn != null ? ChatColor.GREEN + "✓" : ChatColor.RED + "✗").append(ChatColor.WHITE).append(" Blue spawn\n");
            info.append(redTeleport != null ? ChatColor.GREEN + "✓" : ChatColor.RED + "✗").append(ChatColor.WHITE).append(" Red teleport\n");
            info.append(blueTeleport != null ? ChatColor.GREEN + "✓" : ChatColor.RED + "✗").append(ChatColor.WHITE).append(" Blue teleport\n");
            
            info.append(ChatColor.AQUA).append("Kits configured: ").append(ChatColor.WHITE);
            int kitsWithButtons = (int) kits.stream().filter(kit -> kit.buttons.length > 0).count();
            info.append(kitsWithButtons).append("/8\n");
            
            if (waitingForKit != null) {
                info.append(ChatColor.YELLOW).append("Waiting for kit button: ").append(waitingForKit).append("\n");
            }
            
            List<String> missing = getMissingComponents();
            if (!missing.isEmpty()) {
                info.append(ChatColor.YELLOW).append("Still needed:\n");
                for (String component : missing) {
                    info.append(ChatColor.WHITE).append("- ").append(component).append("\n");
                }
            } else {
                info.append(ChatColor.GREEN).append("Arena is complete! Use /arena save to finish.\n");
            }
            
            return info.toString();
        }
        
        public ArenaConfig build() {
            ArenaConfig config = new ArenaConfig();
            config.id = arenaName;
            config.world = worldName;
            config.centerBox = centerBox;
            
            config.teamSpawns = new ArenaConfig.TeamSpawns();
            config.teamSpawns.redSpawn = redSpawn;
            config.teamSpawns.blueSpawn = blueSpawn;
            config.teamSpawns.redTeleport = redTeleport;
            config.teamSpawns.blueTeleport = blueTeleport;
            
            config.kits = kits.toArray(new Kit[0]);
            
            return config;
        }
    }
}
