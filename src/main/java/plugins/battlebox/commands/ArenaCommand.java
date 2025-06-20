package plugins.battlebox.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import plugins.battlebox.BattleBox;
import plugins.battlebox.managers.ArenaCreationManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ArenaCommand implements CommandExecutor, TabCompleter {
    private final BattleBox plugin;
    private final ArenaCreationManager arenaManager;
    
    // Available kit types from BattleBox.md
    private static final String[] KIT_TYPES = {"healer", "fighter", "sniper", "speedster"};
    private static final String[] TEAMS = {"red", "blue"};
    
    public ArenaCommand(BattleBox plugin, ArenaCreationManager arenaManager) {
        this.plugin = plugin;
        this.arenaManager = arenaManager;
    }
      @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use arena commands!");
            return true;
        }
        
        if (!player.hasPermission("battlebox.arena")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to manage arenas!");
            return true;
        }
        
        if (args.length == 0) {
            sendHelpMessage(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "create" -> handleCreate(player, args);
            case "setcenter" -> handleSetCenter(player);
            case "setspawn" -> handleSetSpawn(player, args);
            case "setteleport" -> handleSetTeleport(player, args);
            case "addkit" -> handleAddKit(player, args);
            case "cancelkit" -> handleCancelKit(player);
            case "progress" -> handleProgress(player);
            case "save" -> handleSave(player);
            case "cancel" -> handleCancel(player);
            case "list" -> handleList(player);
            case "info" -> handleInfo(player, args);
            case "delete" -> handleDelete(player, args);
            default -> sendHelpMessage(player);
        }
        
        return true;
    }
    
    private void handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /arena create <arena-name>");
            return;
        }
        
        String arenaName = args[1];
        
        if (arenaManager.startArenaCreation(player, arenaName)) {
            plugin.getLogger().info("Player " + player.getName() + " started creating arena: " + arenaName);
        }
    }
    
    private void handleSetCenter(Player player) {
        if (!arenaManager.isCreating(player)) {
            player.sendMessage(ChatColor.RED + "You're not creating an arena! Use /arena create <name> first.");
            return;
        }
        
        if (arenaManager.setCenterFromSelection(player)) {
            plugin.getLogger().info("Player " + player.getName() + " set center area for their arena");
        } else {
            player.sendMessage(ChatColor.RED + "Failed to set center area. Make sure you have a 3x3 WorldEdit selection!");
        }
    }
    
    private void handleSetSpawn(Player player, String[] args) {
        if (!arenaManager.isCreating(player)) {
            player.sendMessage(ChatColor.RED + "You're not creating an arena! Use /arena create <name> first.");
            return;
        }
        
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /arena setspawn <red|blue>");
            return;
        }
        
        String team = args[1];
        if (arenaManager.setTeamSpawn(player, team)) {
            plugin.getLogger().info("Player " + player.getName() + " set " + team + " spawn for their arena");
        }
    }
    
    private void handleSetTeleport(Player player, String[] args) {
        if (!arenaManager.isCreating(player)) {
            player.sendMessage(ChatColor.RED + "You're not creating an arena! Use /arena create <name> first.");
            return;
        }
        
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /arena setteleport <red|blue>");
            return;
        }
        
        String team = args[1];
        if (arenaManager.setTeamTeleport(player, team)) {
            plugin.getLogger().info("Player " + player.getName() + " set " + team + " teleport for their arena");
        }
    }
    
    private void handleAddKit(Player player, String[] args) {
        if (!arenaManager.isCreating(player)) {
            player.sendMessage(ChatColor.RED + "You're not creating an arena! Use /arena create <name> first.");
            return;
        }
        
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: /arena addkit <red|blue> <healer|fighter|sniper|speedster>");
            return;
        }
        
        String team = args[1];
        String kitType = args[2];
        
        if (arenaManager.startKitSetup(player, team, kitType)) {
            plugin.getLogger().info("Player " + player.getName() + " started setting up " + team + " " + kitType + " kit");
        }
    }
    
    private void handleCancelKit(Player player) {
        if (!arenaManager.isCreating(player)) {
            player.sendMessage(ChatColor.RED + "You're not creating an arena!");
            return;
        }
        
        if (arenaManager.cancelKit(player)) {
            plugin.getLogger().info("Player " + player.getName() + " cancelled kit setup");
        }
    }
    
    private void handleProgress(Player player) {
        if (!arenaManager.isCreating(player)) {
            player.sendMessage(ChatColor.RED + "You're not creating an arena! Use /arena create <name> first.");
            return;
        }
        
        String progress = arenaManager.getArenaProgress(player);
        player.sendMessage(progress);
    }
    
    private void handleSave(Player player) {
        if (!arenaManager.isCreating(player)) {
            player.sendMessage(ChatColor.RED + "You're not creating an arena! Use /arena create <name> first.");
            return;
        }
        
        if (arenaManager.saveArena(player)) {
            plugin.getLogger().info("Player " + player.getName() + " saved their arena");
        }
    }
    
    private void handleCancel(Player player) {
        if (!arenaManager.isCreating(player)) {
            player.sendMessage(ChatColor.RED + "You're not creating an arena!");
            return;
        }
        
        arenaManager.cancelCreation(player);
        plugin.getLogger().info("Player " + player.getName() + " cancelled arena creation");
    }
    
    private void handleList(Player player) {
        List<String> arenas = arenaManager.getArenaList();
        
        if (arenas.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "No arenas found. Create one with /arena create <name>");
            return;
        }
        
        player.sendMessage(ChatColor.GOLD + "=== Available Arenas ===");
        for (String arena : arenas) {
            player.sendMessage(ChatColor.AQUA + "- " + ChatColor.WHITE + arena);
        }
        player.sendMessage(ChatColor.GRAY + "Use /arena info <name> for details");
    }
    
    private void handleInfo(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /arena info <arena-name>");
            return;
        }
        
        String arenaName = args[1];
        String info = arenaManager.getArenaInfo(arenaName);
        player.sendMessage(info);
    }
    
    private void handleDelete(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /arena delete <arena-name>");
            return;
        }
        
        String arenaName = args[1];
        
        if (arenaManager.deleteArena(arenaName)) {
            player.sendMessage(ChatColor.GREEN + "Arena '" + arenaName + "' deleted successfully!");
            plugin.getLogger().info("Player " + player.getName() + " deleted arena: " + arenaName);
        } else {
            player.sendMessage(ChatColor.RED + "Arena '" + arenaName + "' not found!");
        }
    }
    
    private void sendHelpMessage(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Arena Commands ===");
        player.sendMessage(ChatColor.YELLOW + "Arena Creation:");
        player.sendMessage(ChatColor.AQUA + "/arena create <name>" + ChatColor.WHITE + " - Start creating a new arena");
        player.sendMessage(ChatColor.AQUA + "/arena setcenter" + ChatColor.WHITE + " - Set 3x3 center area (use WorldEdit selection)");
        player.sendMessage(ChatColor.AQUA + "/arena setspawn <red|blue>" + ChatColor.WHITE + " - Set team spawn location");
        player.sendMessage(ChatColor.AQUA + "/arena setteleport <red|blue>" + ChatColor.WHITE + " - Set team game start location");
        player.sendMessage(ChatColor.AQUA + "/arena addkit <red|blue> <kit-type>" + ChatColor.WHITE + " - Add kit button (then click button)");
        player.sendMessage(ChatColor.GRAY + "  Kit types: healer, fighter, sniper, speedster");
        player.sendMessage(ChatColor.AQUA + "/arena cancelkit" + ChatColor.WHITE + " - Cancel current kit setup");
        player.sendMessage(ChatColor.AQUA + "/arena progress" + ChatColor.WHITE + " - Show creation progress");
        player.sendMessage(ChatColor.AQUA + "/arena save" + ChatColor.WHITE + " - Save the arena");
        player.sendMessage(ChatColor.AQUA + "/arena cancel" + ChatColor.WHITE + " - Cancel arena creation");
        player.sendMessage("");
        player.sendMessage(ChatColor.YELLOW + "Arena Management:");
        player.sendMessage(ChatColor.AQUA + "/arena list" + ChatColor.WHITE + " - List all arenas");
        player.sendMessage(ChatColor.AQUA + "/arena info <name>" + ChatColor.WHITE + " - Show arena details");
        player.sendMessage(ChatColor.AQUA + "/arena delete <name>" + ChatColor.WHITE + " - Delete an arena");
        player.sendMessage("");
        player.sendMessage(ChatColor.YELLOW + "Required for each arena:");
        player.sendMessage(ChatColor.WHITE + "• 3x3 center area for wool placement");        player.sendMessage(ChatColor.GRAY + "Kits: healer, fighter, sniper, speedster");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("create", "setcenter", "setspawn", "setteleport", 
                "addkit", "cancelkit", "progress", "save", "cancel", "list", "info", "delete");
            String partial = args[0].toLowerCase();
            
            for (String subCommand : subCommands) {
                if (subCommand.startsWith(partial)) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            String partial = args[1].toLowerCase();
            
            switch (subCommand) {
                case "setspawn", "setteleport" -> {
                    for (String team : TEAMS) {
                        if (team.startsWith(partial)) {
                            completions.add(team);
                        }
                    }
                }
                case "addkit" -> {
                    for (String team : TEAMS) {
                        if (team.startsWith(partial)) {
                            completions.add(team);
                        }
                    }
                }
                case "info", "delete" -> {
                    List<String> arenas = arenaManager.getArenaList();
                    for (String arena : arenas) {
                        if (arena.toLowerCase().startsWith(partial)) {
                            completions.add(arena);
                        }
                    }
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("addkit")) {
            String partial = args[2].toLowerCase();
            for (String kitType : KIT_TYPES) {
                if (kitType.startsWith(partial)) {
                    completions.add(kitType);
                }
            }
        }
        
        return completions;
    }
}
