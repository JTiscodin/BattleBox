package plugins.battlebox.commands;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import plugins.battlebox.arena.ArenaInstance;
import plugins.battlebox.arena.ArenaTemplate;
import plugins.battlebox.managers.ArenaInstanceManager;
import plugins.battlebox.managers.ArenaManager;
import config.ArenaConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ArenaInstanceCommand implements CommandExecutor, TabCompleter {
    private final ArenaInstanceManager arenaInstanceManager;
    private final ArenaManager arenaManager;
    
    public ArenaInstanceCommand(ArenaInstanceManager arenaInstanceManager, ArenaManager arenaManager) {
        this.arenaInstanceManager = arenaInstanceManager;
        this.arenaManager = arenaManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }
        
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "register" -> handleRegister(player, args);
            case "create" -> handleCreate(player, args);
            case "list" -> handleList(player, args);
            case "info" -> handleInfo(player, args);
            case "reset" -> handleReset(player, args);
            case "remove" -> handleRemove(player, args);
            case "tp" -> handleTeleport(player, args);
            case "stats" -> handleStats(player);
            default -> sendHelp(player);
        }
        
        return true;
    }
    
    private void handleRegister(Player player, String[] args) {
        if (args.length < 4) {
            player.sendMessage("§cUsage: /arenainstance register <templateId> <schematicName> <maxInstances> [arenaId]");
            return;
        }
        
        String templateId = args[1];
        String schematicName = args[2];
        
        int maxInstances;
        try {
            maxInstances = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            player.sendMessage("§cMax instances must be a number.");
            return;
        }
          // Get arena config if arena ID is provided
        ArenaConfig config = null;
        if (args.length > 4) {
            config = arenaManager.getArena(args[4]);
            if (config == null) {
                player.sendMessage("§cArena '" + args[4] + "' not found.");
                return;
            }
        }
        
        Location baseLocation = player.getLocation();
        
        if (arenaInstanceManager.registerTemplate(templateId, schematicName, baseLocation, config, maxInstances)) {
            player.sendMessage("§aTemplate '" + templateId + "' registered successfully!");
            player.sendMessage("§7Base location: " + baseLocation.getBlockX() + ", " + 
                             baseLocation.getBlockY() + ", " + baseLocation.getBlockZ());
            player.sendMessage("§7Max instances: " + maxInstances);
        } else {
            player.sendMessage("§cTemplate '" + templateId + "' already exists.");
        }
    }
    
    private void handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /arenainstance create <templateId>");
            return;
        }
        
        String templateId = args[1];
        ArenaInstance instance = arenaInstanceManager.createInstance(templateId);
        
        if (instance != null) {
            player.sendMessage("§aCreated instance '" + instance.getInstanceId() + "'!");
            Location loc = instance.getInstanceLocation();
            player.sendMessage("§7Location: " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
        } else {
            player.sendMessage("§cFailed to create instance. Template may not exist or max instances reached.");
        }
    }
    
    private void handleList(Player player, String[] args) {
        if (args.length > 1) {
            // List instances for specific template
            String templateId = args[1];
            ArenaTemplate template = arenaInstanceManager.getTemplate(templateId);
            if (template == null) {
                player.sendMessage("§cTemplate '" + templateId + "' not found.");
                return;
            }
            
            List<ArenaInstance> instances = arenaInstanceManager.getInstancesForTemplate(templateId);
            player.sendMessage("§6=== Instances for " + templateId + " ===");
            
            if (instances.isEmpty()) {
                player.sendMessage("§7No instances created yet.");
                return;
            }
            
            for (ArenaInstance instance : instances) {
                String status = instance.isInUse() ? "§cIn Use" : "§aAvailable";
                String gameInfo = instance.getCurrentGame() != null ? " (Game: " + instance.getCurrentGame().getId() + ")" : "";
                player.sendMessage("§7- §f" + instance.getInstanceId() + " " + status + gameInfo);
            }
        } else {
            // List all templates
            player.sendMessage("§6=== Arena Templates ===");
            var templates = arenaInstanceManager.getTemplates();
            
            if (templates.isEmpty()) {
                player.sendMessage("§7No templates registered.");
                return;
            }
            
            for (ArenaTemplate template : templates.values()) {
                List<ArenaInstance> instances = arenaInstanceManager.getInstancesForTemplate(template.getTemplateId());
                int active = (int) instances.stream().mapToInt(i -> i.isInUse() ? 1 : 0).sum();
                
                player.sendMessage("§7- §f" + template.getTemplateId() + " §7(§a" + active + "§7/§2" + 
                                 instances.size() + "§7/§6" + template.getMaxInstances() + "§7)");
            }
        }
    }
    
    private void handleInfo(Player player, String[] args) {
        if (args.length < 2) {
            arenaInstanceManager.sendDetailedInfo(player);
            return;
        }
        
        String id = args[1];
        
        // Check if it's a template or instance
        ArenaTemplate template = arenaInstanceManager.getTemplate(id);
        ArenaInstance instance = arenaInstanceManager.getInstance(id);
        
        if (template != null) {
            player.sendMessage("§6=== Template: " + template.getTemplateId() + " ===");
            player.sendMessage("§7Schematic: §f" + template.getSchematicName());
            player.sendMessage("§7Max Instances: §f" + template.getMaxInstances());
            Location loc = template.getBaseLocation();
            player.sendMessage("§7Base Location: §f" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
            
            List<ArenaInstance> instances = arenaInstanceManager.getInstancesForTemplate(id);
            player.sendMessage("§7Created Instances: §f" + instances.size());
        } else if (instance != null) {
            player.sendMessage("§6=== Instance: " + instance.getInstanceId() + " ===");
            player.sendMessage("§7Template: §f" + instance.getTemplate().getTemplateId());
            player.sendMessage("§7Status: " + (instance.isInUse() ? "§cIn Use" : "§aAvailable"));
            Location loc = instance.getInstanceLocation();
            player.sendMessage("§7Location: §f" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
            
            if (instance.getCurrentGame() != null) {
                player.sendMessage("§7Current Game: §f" + instance.getCurrentGame().getId());
            }
            player.sendMessage("§7Last Used: §f" + (System.currentTimeMillis() - instance.getLastUsed()) / 1000 + "s ago");
        } else {
            player.sendMessage("§cTemplate or instance '" + id + "' not found.");
        }
    }
    
    private void handleReset(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /arenainstance reset <instanceId>");
            return;
        }
        
        String instanceId = args[1];
        ArenaInstance instance = arenaInstanceManager.getInstance(instanceId);
        
        if (instance == null) {
            player.sendMessage("§cInstance '" + instanceId + "' not found.");
            return;
        }
        
        if (instance.isInUse()) {
            player.sendMessage("§cCannot reset instance that is currently in use.");
            return;
        }
        
        arenaInstanceManager.resetArena(instance);
        player.sendMessage("§aReset instance '" + instanceId + "'!");
    }
    
    private void handleRemove(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /arenainstance remove <templateId>");
            return;
        }
        
        String templateId = args[1];
        
        if (arenaInstanceManager.removeTemplate(templateId)) {
            player.sendMessage("§aRemoved template '" + templateId + "' and all its instances.");
        } else {
            player.sendMessage("§cCannot remove template. It may not exist or have active instances.");
        }
    }
    
    private void handleTeleport(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /arenainstance tp <instanceId>");
            return;
        }
        
        String instanceId = args[1];
        ArenaInstance instance = arenaInstanceManager.getInstance(instanceId);
        
        if (instance == null) {
            player.sendMessage("§cInstance '" + instanceId + "' not found.");
            return;
        }
        
        player.teleport(instance.getInstanceLocation());
        player.sendMessage("§aTeleported to instance '" + instanceId + "'!");
    }
    
    private void handleStats(Player player) {
        arenaInstanceManager.sendDetailedInfo(player);
    }
    
    private void sendHelp(Player player) {
        player.sendMessage("§6=== Arena Instance Commands ===");
        player.sendMessage("§7/arenainstance register <templateId> <schematicName> <maxInstances> [arenaId] §f- Register template");
        player.sendMessage("§7/arenainstance create <templateId> §f- Create instance");
        player.sendMessage("§7/arenainstance list [templateId] §f- List templates/instances");
        player.sendMessage("§7/arenainstance info [templateId/instanceId] §f- Show detailed info");
        player.sendMessage("§7/arenainstance reset <instanceId> §f- Reset instance");
        player.sendMessage("§7/arenainstance remove <templateId> §f- Remove template");
        player.sendMessage("§7/arenainstance tp <instanceId> §f- Teleport to instance");
        player.sendMessage("§7/arenainstance stats §f- Show usage statistics");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.addAll(Arrays.asList("register", "create", "list", "info", "reset", "remove", "tp", "stats"));
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "create", "list", "remove" -> {
                    completions.addAll(arenaInstanceManager.getTemplates().keySet());
                }
                case "info", "reset", "tp" -> {
                    completions.addAll(arenaInstanceManager.getTemplates().keySet());
                    completions.addAll(arenaInstanceManager.getInstances().keySet());
                }
            }        } else if (args.length == 5 && args[0].equalsIgnoreCase("register")) {
            // Arena IDs for template registration
            for (ArenaConfig arena : arenaManager.getArenas()) {
                completions.add(arena.id);
            }
        }
        
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .toList();
    }
}