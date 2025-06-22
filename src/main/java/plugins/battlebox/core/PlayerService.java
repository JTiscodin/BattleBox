package plugins.battlebox.core;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.plugin.java.JavaPlugin;

import config.ArenaConfig;
import plugins.battlebox.game.Game;

/**
 * Service for handling player operations in games
 */
public class PlayerService {

    private final JavaPlugin plugin;

    public PlayerService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Setup player when joining a game
     */
    public void setupPlayerForGame(Player player, Game game) {
        Game.TeamColor team = game.getPlayerTeam(player);
        player.sendMessage(team.chatColor + "You are on " + team.displayName + " team!");
    }

    /**
     * Teleport player to game Map
     * This method teleports player to the game map while the game is still in
     * waiting state.
     */
    public void teleportToMap(Player player, Game game, ArenaConfig arena) {
        Game.TeamColor team = game.getPlayerTeam(player);
        ArenaConfig.Location mapLocation = team == Game.TeamColor.RED ? arena.teamSpawns.redSpawn
                : arena.teamSpawns.blueSpawn;

        Location location = createBukkitLocation(arena.world, mapLocation);
        safeTeleport(player, location);
    }

    /**
     * Teleport player to spawn position
     */
    public void teleportToSpawn(Player player, Game game, ArenaConfig arena) {
        Game.TeamColor team = game.getPlayerTeam(player);
        ArenaConfig.Location spawn = team == Game.TeamColor.RED ? arena.teamSpawns.redSpawn
                : arena.teamSpawns.blueSpawn;

        Location location = createBukkitLocation(arena.world, spawn);
        safeTeleport(player, location);
    }

    /**
     * Teleport player to game battle position
     */
    public void teleportToGamePosition(Player player, Game game, ArenaConfig arena) {
        Game.TeamColor team = game.getPlayerTeam(player);
        ArenaConfig.Location teleport = team == Game.TeamColor.RED ? arena.teamSpawns.redTeleport
                : arena.teamSpawns.blueTeleport;

        Location location = createBukkitLocation(arena.world, teleport);
        safeTeleport(player, location);

        // Play music when entering battle
        playBattleMusic(player, location);
    }

    /**
     * Teleport player to center area during waiting state
     */
    public void teleportToWaitingArea(Player player, ArenaConfig arena) {
        // Calculate center of the center box for waiting area
        double centerX = (arena.centerBox.x1 + arena.centerBox.x2) / 2.0;
        double centerZ = (arena.centerBox.z1 + arena.centerBox.z2) / 2.0;
        double waitingY = arena.centerBox.y1 + 5; // 5 blocks above the center box

        World world = Bukkit.getWorld(arena.world);
        if (world == null) {
            plugin.getLogger().warning("World " + arena.world + " not found for waiting area teleport");
            return;
        }

        Location waitingLocation = new Location(world, centerX, waitingY, centerZ, 0, 0);
        safeTeleport(player, waitingLocation);

        player.sendMessage(ChatColor.GRAY + "Waiting for more players to join...");
    }

    /**
     * Give base kit to player
     */
    public void giveBaseKit(Player player, Game.TeamColor team) {
        player.getInventory().clear();

        // Base items as per BattleBox.md
        addItem(player, new ItemStack(Material.WOODEN_SWORD));
        addItem(player, new ItemStack(Material.BOW));
        addItem(player, new ItemStack(Material.ARROW, 6));
        addItem(player, createUnbreakableItem(Material.SHEARS));

        // Team wool (unlimited - will auto-refill)
        Material woolType = team == Game.TeamColor.RED ? Material.RED_WOOL : Material.BLUE_WOOL;
        addItem(player, new ItemStack(woolType, 64));

        // Team boots
        player.getInventory().setBoots(createTeamBoots(team));

        player.sendMessage(team.chatColor + "Base kit equipped!");
    }

    /**
     * Refill wool when player runs out
     */
    public void refillWool(Player player, Game.TeamColor team) {
        Material woolType = team == Game.TeamColor.RED ? Material.RED_WOOL : Material.BLUE_WOOL;

        // Check if player has wool
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == woolType) {
                return; // Already has wool
            }
        }

        // Give new wool stack
        addItem(player, new ItemStack(woolType, 64));
        player.sendMessage(team.chatColor + "Wool refilled!");
    }

    private Location createBukkitLocation(String worldName, ArenaConfig.Location loc) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            throw new IllegalStateException("World not found: " + worldName);
        }

        Location location = new Location(world, loc.x, loc.y, loc.z);
        location.setYaw((float) loc.yaw);
        location.setPitch((float) loc.pitch);
        return location;
    }

    private void safeTeleport(Player player, Location location) {
        // Ensure chunk is loaded before teleporting
        if (!location.getChunk().isLoaded()) {
            location.getChunk().load();
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            player.teleport(location);
        });
    }

    private ItemStack createTeamBoots(Game.TeamColor team) {
        ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);
        LeatherArmorMeta meta = (LeatherArmorMeta) boots.getItemMeta();

        if (meta != null) {
            meta.setColor(team == Game.TeamColor.RED ? org.bukkit.Color.RED : org.bukkit.Color.BLUE);
            meta.addEnchant(Enchantment.DURABILITY, 3, true);
            meta.setUnbreakable(true);
            boots.setItemMeta(meta);
        }

        return boots;
    }

    private ItemStack createUnbreakableItem(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setUnbreakable(true);
            item.setItemMeta(meta);
        }

        return item;
    }

    private void addItem(Player player, ItemStack item) {
        if (player.getInventory().firstEmpty() == -1) {
            player.getWorld().dropItem(player.getLocation(), item);
        } else {
            player.getInventory().addItem(item);
        }
    }

    private void playBattleMusic(Player player, Location location) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (plugin instanceof plugins.battlebox.BattleBox battleBoxPlugin) {
                battleBoxPlugin.playBattleMusic(player, location);
            }
        }, 20L);
    }
}
