package plugins.battlebox.commands;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import config.ArenaConfig;
import config.Box;
import config.Kit.Button;
import config.Kit.Kit;
import plugins.battlebox.managers.ArenaCreationManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ArenaCommand implements CommandExecutor, TabCompleter {
    
    private final ArenaCreationManager arenaCreationManager;
    private final JavaPlugin plugin;
    
    public ArenaCommand(ArenaCreationManager arenaCreationManager, JavaPlugin plugin) {
        this.arenaCreationManager = arenaCreationManager;
        this.plugin = plugin;
    }
      @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Debug logging
        plugin.getLogger().info("Arena command received from " + sender.getName() + " with args: " + String.join(" ", args));
        
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use arena commands!");
            return true;
        }
        
        if (!player.hasPermission("battlebox.arena.admin")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use arena commands!");
            plugin.getLogger().info("Player " + player.getName() + " lacks permission battlebox.arena.admin");
            return true;
        }
        
        if (args.length == 0) {
            sendHelpMessage(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "create" -> handleCreate(player, args);
            case "setregion" -> handleSetRegion(player, args);
            case "addkit" -> handleAddKit(player, args);
            case "save" -> handleSave(player, args);
            case "cancel" -> handleCancel(player);
            case "list" -> handleList(player);
            case "info" -> handleInfo(player, args);
            case "delete" -> handleDelete(player, args);
            case "wizard" -> handleWizard(player);
            case "highlight", "show" -> handleHighlight(player, args);
            case "tp", "teleport" -> handleTeleport(player, args);
            case "whereami" -> handleWhereAmI(player);
            default -> sendHelpMessage(player);
        }
        
        return true;
    }
    
    private void handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /arena create <name>");
            return;
        }
        
        String arenaName = args[1];
        boolean success = arenaCreationManager.startCreation(player, arenaName);
        
        if (success) {
            player.sendMessage(ChatColor.GREEN + "Started creating arena: " + ChatColor.YELLOW + arenaName);
            player.sendMessage(ChatColor.AQUA + "Use " + ChatColor.WHITE + "/arena setregion" + ChatColor.AQUA + " to define the arena boundaries!");
        } else {
            player.sendMessage(ChatColor.RED + "Failed to start arena creation. Arena name might already exist or you're already creating an arena.");
        }
    }
    
    private void handleSetRegion(Player player, String[] args) {
        if (!arenaCreationManager.isCreating(player)) {
            player.sendMessage(ChatColor.RED + "You're not currently creating an arena! Use " + ChatColor.WHITE + "/arena create <name>" + ChatColor.RED + " first.");
            return;
        }
        
        boolean success = arenaCreationManager.setRegionFromSelection(player);
        
        if (success) {
            player.sendMessage(ChatColor.GREEN + "Arena region set successfully!");
            player.sendMessage(ChatColor.AQUA + "Next, add kits using " + ChatColor.WHITE + "/arena addkit <kitname>" + ChatColor.AQUA + " or save with " + ChatColor.WHITE + "/arena save");
        } else {
            player.sendMessage(ChatColor.RED + "Failed to set region. Make sure you have a WorldEdit selection!");
            player.sendMessage(ChatColor.YELLOW + "Use WorldEdit's wand (//wand) to select the arena boundaries.");
        }
    }
    
    private void handleAddKit(Player player, String[] args) {
        if (!arenaCreationManager.isCreating(player)) {
            player.sendMessage(ChatColor.RED + "You're not currently creating an arena!");
            return;
        }
        
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /arena addkit <kitname>");
            return;
        }
        
        String kitName = args[1];
        boolean success = arenaCreationManager.addKitButton(player, kitName);
        
        if (success) {
            player.sendMessage(ChatColor.GREEN + "Kit button added for: " + ChatColor.YELLOW + kitName);
            player.sendMessage(ChatColor.AQUA + "Click on a button block to set its location for this kit!");
        } else {
            player.sendMessage(ChatColor.RED + "Failed to add kit. Kit might already exist for this arena.");
        }
    }
    
    private void handleSave(Player player, String[] args) {
        if (!arenaCreationManager.isCreating(player)) {
            player.sendMessage(ChatColor.RED + "You're not currently creating an arena!");
            return;
        }
        
        boolean success = arenaCreationManager.saveArena(player);
        
        if (success) {
            player.sendMessage(ChatColor.GREEN + "Arena saved successfully!");
            player.sendMessage(ChatColor.AQUA + "Arena is now ready for use!");
        } else {
            player.sendMessage(ChatColor.RED + "Failed to save arena. Make sure region is set!");
        }
    }
    
    private void handleCancel(Player player) {
        if (!arenaCreationManager.isCreating(player)) {
            player.sendMessage(ChatColor.RED + "You're not currently creating an arena!");
            return;
        }
        
        arenaCreationManager.cancelCreation(player);
        player.sendMessage(ChatColor.YELLOW + "Arena creation cancelled.");
    }
    
    private void handleList(Player player) {
        List<String> arenas = arenaCreationManager.getArenaList();
        
        if (arenas.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "No arenas found.");
            return;
        }
        
        player.sendMessage(ChatColor.GOLD + "=== Available Arenas ===");
        for (String arena : arenas) {
            player.sendMessage(ChatColor.AQUA + "- " + ChatColor.WHITE + arena);
        }
    }
    
    private void handleInfo(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /arena info <name>");
            return;
        }
        
        String arenaName = args[1];
        String info = arenaCreationManager.getArenaInfo(arenaName);
        
        if (info != null) {
            player.sendMessage(info);
        } else {
            player.sendMessage(ChatColor.RED + "Arena not found: " + arenaName);
        }
    }
    
    private void handleDelete(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /arena delete <name>");
            return;
        }
        
        String arenaName = args[1];
        boolean success = arenaCreationManager.deleteArena(arenaName);
        
        if (success) {
            player.sendMessage(ChatColor.GREEN + "Arena deleted: " + ChatColor.YELLOW + arenaName);
        } else {
            player.sendMessage(ChatColor.RED + "Failed to delete arena. Arena might not exist.");
        }
    }
    
    private void handleWizard(Player player) {
        arenaCreationManager.startWizard(player);
    }
    
    private void handleHighlight(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /arena highlight <arena-name>");
            return;
        }
        highlightArena(player, args[1]);
    }
    
    private void handleTeleport(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /arena tp <arena-name>");
            return;
        }
        teleportToArena(player, args[1]);
    }
    
    private void handleWhereAmI(Player player) {
        showPlayerLocation(player);
    }
    
    private void highlightArena(Player player, String arenaName) {
        String info = arenaCreationManager.getArenaInfo(arenaName);
        if (info == null) {
            player.sendMessage(ChatColor.RED + "Arena '" + arenaName + "' not found!");
            return;
        }

        ArenaConfig arena = arenaCreationManager.getArena(arenaName);
        if (arena == null) {
            player.sendMessage(ChatColor.RED + "Arena '" + arenaName + "' not found!");
            return;
        }

        World world = player.getServer().getWorld(arena.world);
        if (world == null) {
            player.sendMessage(ChatColor.RED + "World '" + arena.world + "' not found!");
            return;
        }

        Box box = arena.box;
        
        // Show arena boundaries with particles for 10 seconds
        new BukkitRunnable() {
            int duration = 200; // 10 seconds (20 ticks per second)
            
            @Override
            public void run() {
                if (duration <= 0 || !player.isOnline()) {
                    this.cancel();
                    return;
                }
                
                // Draw outline of the arena
                drawBoxOutline(world, box);
                
                // Highlight kit buttons
                if (arena.Kits != null) {
                    for (Kit kit : arena.Kits) {
                        if (kit.buttons != null) {
                            for (Button button : kit.buttons) {
                                Location buttonLoc = new Location(world, button.x, button.y, button.z);
                                world.spawnParticle(Particle.VILLAGER_HAPPY, buttonLoc.add(0.5, 1, 0.5), 5);
                            }
                        }
                    }
                }
                
                duration -= 10; // Reduce by 10 ticks (0.5 seconds)
            }
        }.runTaskTimer(plugin, 0L, 10L);
        
        player.sendMessage(ChatColor.GREEN + "Highlighting arena '" + arenaName + "' for 10 seconds!");
        player.sendMessage(info);
    }
    
    private void drawBoxOutline(World world, Box box) {
        int x1 = box.x1, y1 = box.y1, z1 = box.z1;
        int x2 = box.x2, y2 = box.y2, z2 = box.z2;
        
        Particle.DustOptions greenDust = new Particle.DustOptions(Color.LIME, 1.0f);
        
        // Draw edges of the box
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            world.spawnParticle(Particle.REDSTONE, new Location(world, x + 0.5, y1, Math.min(z1, z2) + 0.5), 1, greenDust);
            world.spawnParticle(Particle.REDSTONE, new Location(world, x + 0.5, y1, Math.max(z1, z2) + 0.5), 1, greenDust);
            world.spawnParticle(Particle.REDSTONE, new Location(world, x + 0.5, y2, Math.min(z1, z2) + 0.5), 1, greenDust);
            world.spawnParticle(Particle.REDSTONE, new Location(world, x + 0.5, y2, Math.max(z1, z2) + 0.5), 1, greenDust);
        }
        
        for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
            world.spawnParticle(Particle.REDSTONE, new Location(world, Math.min(x1, x2) + 0.5, y1, z + 0.5), 1, greenDust);
            world.spawnParticle(Particle.REDSTONE, new Location(world, Math.max(x1, x2) + 0.5, y1, z + 0.5), 1, greenDust);
            world.spawnParticle(Particle.REDSTONE, new Location(world, Math.min(x1, x2) + 0.5, y2, z + 0.5), 1, greenDust);
            world.spawnParticle(Particle.REDSTONE, new Location(world, Math.max(x1, x2) + 0.5, y2, z + 0.5), 1, greenDust);
        }
        
        for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
            world.spawnParticle(Particle.REDSTONE, new Location(world, Math.min(x1, x2) + 0.5, y, Math.min(z1, z2) + 0.5), 1, greenDust);
            world.spawnParticle(Particle.REDSTONE, new Location(world, Math.max(x1, x2) + 0.5, y, Math.min(z1, z2) + 0.5), 1, greenDust);
            world.spawnParticle(Particle.REDSTONE, new Location(world, Math.min(x1, x2) + 0.5, y, Math.max(z1, z2) + 0.5), 1, greenDust);
            world.spawnParticle(Particle.REDSTONE, new Location(world, Math.max(x1, x2) + 0.5, y, Math.max(z1, z2) + 0.5), 1, greenDust);
        }
    }
    
    private void teleportToArena(Player player, String arenaName) {
        ArenaConfig arena = arenaCreationManager.getArena(arenaName);
        if (arena == null) {
            player.sendMessage(ChatColor.RED + "Arena '" + arenaName + "' not found!");
            return;
        }

        World world = player.getServer().getWorld(arena.world);
        if (world == null) {
            player.sendMessage(ChatColor.RED + "World '" + arena.world + "' not found!");
            return;
        }

        Box box = arena.box;
        // Teleport to center of arena, slightly above
        double centerX = (box.x1 + box.x2) / 2.0;
        double centerY = Math.max(box.y1, box.y2) + 2;
        double centerZ = (box.z1 + box.z2) / 2.0;
        
        Location teleportLoc = new Location(world, centerX, centerY, centerZ);
        player.teleport(teleportLoc);
        player.sendMessage(ChatColor.GREEN + "Teleported to arena '" + arenaName + "'!");
        
        // Automatically show arena info and highlight
        String info = arenaCreationManager.getArenaInfo(arenaName);
        if (info != null) {
            player.sendMessage(info);
        }
        highlightArena(player, arenaName);
    }
    
    private void showPlayerLocation(Player player) {
        Location loc = player.getLocation();
        player.sendMessage(ChatColor.YELLOW + "=== Your Current Location ===");
        player.sendMessage(ChatColor.GOLD + "World: " + ChatColor.WHITE + loc.getWorld().getName());
        player.sendMessage(ChatColor.GOLD + "X: " + ChatColor.WHITE + loc.getBlockX());
        player.sendMessage(ChatColor.GOLD + "Y: " + ChatColor.WHITE + loc.getBlockY());
        player.sendMessage(ChatColor.GOLD + "Z: " + ChatColor.WHITE + loc.getBlockZ());
        
        // Check if player is in any arena
        ArenaConfig arena = arenaCreationManager.getArenaAt(loc);
        if (arena != null) {
            player.sendMessage(ChatColor.GREEN + "You are inside arena: " + ChatColor.YELLOW + arena.id);
        } else {
            player.sendMessage(ChatColor.RED + "You are not inside any arena");
        }
    }

    private void sendHelpMessage(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== BattleBox Arena Commands ===");
        player.sendMessage(ChatColor.AQUA + "/arena create <name>" + ChatColor.WHITE + " - Start creating a new arena");
        player.sendMessage(ChatColor.AQUA + "/arena setregion" + ChatColor.WHITE + " - Set arena boundaries from WorldEdit selection");
        player.sendMessage(ChatColor.AQUA + "/arena addkit <name>" + ChatColor.WHITE + " - Add a kit button to the arena");
        player.sendMessage(ChatColor.AQUA + "/arena save" + ChatColor.WHITE + " - Save the current arena");
        player.sendMessage(ChatColor.AQUA + "/arena cancel" + ChatColor.WHITE + " - Cancel arena creation");
        player.sendMessage(ChatColor.AQUA + "/arena list" + ChatColor.WHITE + " - List all arenas");
        player.sendMessage(ChatColor.AQUA + "/arena info <name>" + ChatColor.WHITE + " - Show arena information");
        player.sendMessage(ChatColor.AQUA + "/arena delete <name>" + ChatColor.WHITE + " - Delete an arena");
        player.sendMessage(ChatColor.AQUA + "/arena wizard" + ChatColor.WHITE + " - Start step-by-step arena creation");
        player.sendMessage(ChatColor.AQUA + "/arena highlight" + ChatColor.WHITE + " - Highlight the arena area with particles");
        player.sendMessage(ChatColor.AQUA + "/arena tp <name>" + ChatColor.WHITE + " - Teleport to the arena spawn location");
        player.sendMessage(ChatColor.AQUA + "/arena whereami" + ChatColor.WHITE + " - Show your current coordinates");
    }
      @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        plugin.getLogger().info("Tab completion requested by " + sender.getName() + " with args: " + String.join(",", args));
        
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("create", "setregion", "addkit", "save", "cancel", "list", "info", "delete", "wizard", "highlight", "tp", "teleport", "whereami");
            String partial = args[0].toLowerCase();
            
            for (String subCommand : subCommands) {
                if (subCommand.startsWith(partial)) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            
            if (subCommand.equals("info") || subCommand.equals("delete") || subCommand.equals("tp")) {
                // Tab complete with existing arena names
                List<String> arenas = arenaCreationManager.getArenaList();
                String partial = args[1].toLowerCase();
                
                for (String arena : arenas) {
                    if (arena.toLowerCase().startsWith(partial)) {
                        completions.add(arena);
                    }
                }
            }
        }
        
        plugin.getLogger().info("Returning " + completions.size() + " completions: " + String.join(",", completions));
        return completions;
    }
}
