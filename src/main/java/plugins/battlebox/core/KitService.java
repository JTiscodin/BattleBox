package plugins.battlebox.core;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import plugins.battlebox.game.Game;

/**
 * Service for handling special kit operations.
 * Manages the four special kits: Healer, Fighter, Sniper, Speedster
 */
public class KitService {
      /**
     * Give a special kit to player based on type
     */
    public void giveSpecialKit(Player player, String kitType, Game.TeamColor team) {
        switch (kitType.toLowerCase()) {
            case "healer" -> giveHealerKit(player, team);
            case "fighter" -> giveFighterKit(player, team);
            case "sniper" -> giveSniperKit(player, team);
            case "speedster" -> giveSpeedsterKit(player, team);
            default -> player.sendMessage(team.chatColor + "Unknown kit: " + kitType);
        }
    }
    
    private void giveHealerKit(Player player, Game.TeamColor team) {
        // 2 instant health splash potions
        ItemStack healthPotion = new ItemStack(Material.SPLASH_POTION, 2);
        // Note: In real implementation, would set potion effect to instant health
        addItem(player, healthPotion);
        
        player.sendMessage(team.chatColor + "HEALER kit equipped!");
    }
    
    private void giveFighterKit(Player player, Game.TeamColor team) {
        // Stone sword + leather chestplate
        addItem(player, new ItemStack(Material.STONE_SWORD));
        
        ItemStack chestplate = new ItemStack(Material.LEATHER_CHESTPLATE);
        LeatherArmorMeta meta = (LeatherArmorMeta) chestplate.getItemMeta();
        if (meta != null) {
            meta.setColor(team == Game.TeamColor.RED ? 
                org.bukkit.Color.RED : org.bukkit.Color.BLUE);
            meta.setUnbreakable(true);
            chestplate.setItemMeta(meta);
        }
        player.getInventory().setChestplate(chestplate);
        
        player.sendMessage(team.chatColor + "FIGHTER kit equipped!");
    }
    
    private void giveSniperKit(Player player, Game.TeamColor team) {
        // Crossbow + 2 arrows
        addItem(player, new ItemStack(Material.CROSSBOW));
        addItem(player, new ItemStack(Material.ARROW, 2));
        
        player.sendMessage(team.chatColor + "SNIPER kit equipped!");
    }
    
    private void giveSpeedsterKit(Player player, Game.TeamColor team) {
        // Speed-enchanted leather leggings + stone sword
        addItem(player, new ItemStack(Material.STONE_SWORD));
        
        ItemStack leggings = new ItemStack(Material.LEATHER_LEGGINGS);
        LeatherArmorMeta meta = (LeatherArmorMeta) leggings.getItemMeta();
        if (meta != null) {
            meta.setColor(team == Game.TeamColor.RED ? 
                org.bukkit.Color.RED : org.bukkit.Color.BLUE);
            meta.setUnbreakable(true);
            leggings.setItemMeta(meta);
        }
        player.getInventory().setLeggings(leggings);
        
        // Give speed effect for duration of game
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 120, 0)); // 2 minutes
        
        player.sendMessage(team.chatColor + "SPEEDSTER kit equipped!");
    }
      private void addItem(Player player, ItemStack item) {
        if (player.getInventory().firstEmpty() == -1) {
            player.getWorld().dropItem(player.getLocation(), item);
        } else {
            player.getInventory().addItem(item);
        }
    }
    
    /**
     * Get all available kit types
     */
    public static String[] getKitTypes() {
        return new String[]{"healer", "fighter", "sniper", "speedster"};
    }
    
    /**
     * Check if a kit type is valid
     */
    public static boolean isValidKitType(String kitType) {
        return java.util.Arrays.asList(getKitTypes()).contains(kitType.toLowerCase());
    }
}
