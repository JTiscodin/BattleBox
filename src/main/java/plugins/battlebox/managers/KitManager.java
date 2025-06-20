package plugins.battlebox.managers;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import plugins.battlebox.game.Game;

public class KitManager {

    public KitManager(JavaPlugin plugin){
        // Plugin reference stored for future use if needed
    }

    /**
     * Give a kit to a player based on kit type and team color
     */
    public void giveKit(Player player, String kitType, Game.TeamColor teamColor) {
        // Clear inventory first
        player.getInventory().clear();
        player.getActivePotionEffects().clear();
        
        // Set health and food
        player.setHealth(20.0);
        player.setFoodLevel(20);
        
        // Get team wool color
        Material woolType = (teamColor == Game.TeamColor.RED) ? Material.RED_WOOL : Material.BLUE_WOOL;
        
        switch (kitType.toLowerCase()) {
            case "healer" -> giveHealerKit(player, woolType);
            case "fighter" -> giveFighterKit(player, woolType);
            case "sniper" -> giveSniperKit(player, woolType);
            case "speedster" -> giveSpeedsterKit(player, woolType);
            default -> giveBasicKit(player, woolType);
        }
        
        player.sendMessage(ChatColor.GREEN + "You received the " + ChatColor.YELLOW + kitType + ChatColor.GREEN + " kit!");
    }
    
    private void giveBasicKit(Player player, Material woolType) {
        // Basic sword
        ItemStack sword = new ItemStack(Material.IRON_SWORD);
        player.getInventory().addItem(sword);
        
        // Team wool (64 blocks)
        ItemStack wool = new ItemStack(woolType, 64);
        player.getInventory().addItem(wool);
        
        // Food
        ItemStack food = new ItemStack(Material.COOKED_BEEF, 8);
        player.getInventory().addItem(food);
    }
    
    private void giveHealerKit(Player player, Material woolType) {
        // Healer sword (weaker)
        ItemStack sword = new ItemStack(Material.STONE_SWORD);
        player.getInventory().addItem(sword);
        
        // Team wool
        ItemStack wool = new ItemStack(woolType, 64);
        player.getInventory().addItem(wool);
        
        // Healing potions
        ItemStack healingPots = new ItemStack(Material.SPLASH_POTION, 3);
        ItemMeta healMeta = healingPots.getItemMeta();
        healMeta.setDisplayName(ChatColor.RED + "Healing Potion");
        healingPots.setItemMeta(healMeta);
        player.getInventory().addItem(healingPots);
        
        // Regeneration effect
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 600, 0));
        
        // Food
        ItemStack food = new ItemStack(Material.GOLDEN_APPLE, 4);
        player.getInventory().addItem(food);
    }
    
    private void giveFighterKit(Player player, Material woolType) {
        // Strong sword
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        sword.addEnchantment(Enchantment.DAMAGE_ALL, 1);
        player.getInventory().addItem(sword);
        
        // Team wool
        ItemStack wool = new ItemStack(woolType, 64);
        player.getInventory().addItem(wool);
        
        // Strength effect
        player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 600, 0));
        
        // Food
        ItemStack food = new ItemStack(Material.COOKED_BEEF, 8);
        player.getInventory().addItem(food);
        
        // Shield
        ItemStack shield = new ItemStack(Material.SHIELD);
        player.getInventory().setItemInOffHand(shield);
    }
    
    private void giveSniperKit(Player player, Material woolType) {
        // Bow with enchantments
        ItemStack bow = new ItemStack(Material.BOW);
        bow.addEnchantment(Enchantment.ARROW_DAMAGE, 2);
        bow.addEnchantment(Enchantment.ARROW_INFINITE, 1);
        player.getInventory().addItem(bow);
        
        // Arrows (just 1 with infinity)
        ItemStack arrows = new ItemStack(Material.ARROW, 1);
        player.getInventory().addItem(arrows);
        
        // Basic sword for close combat
        ItemStack sword = new ItemStack(Material.IRON_SWORD);
        player.getInventory().addItem(sword);
        
        // Team wool
        ItemStack wool = new ItemStack(woolType, 64);
        player.getInventory().addItem(wool);
        
        // Food
        ItemStack food = new ItemStack(Material.COOKED_BEEF, 8);
        player.getInventory().addItem(food);
    }
    
    private void giveSpeedsterKit(Player player, Material woolType) {
        // Fast sword
        ItemStack sword = new ItemStack(Material.IRON_SWORD);
        sword.addEnchantment(Enchantment.KNOCKBACK, 1);
        player.getInventory().addItem(sword);
        
        // Team wool
        ItemStack wool = new ItemStack(woolType, 64);
        player.getInventory().addItem(wool);
        
        // Speed effect
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 600, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 600, 1));
        
        // Food
        ItemStack food = new ItemStack(Material.COOKED_BEEF, 8);
        player.getInventory().addItem(food);
        
        // Ender pearls for mobility
        ItemStack pearls = new ItemStack(Material.ENDER_PEARL, 2);
        player.getInventory().addItem(pearls);
    }
}
