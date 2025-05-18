package plugins.battlebox.commands;

import org.bukkit.Material;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class TestKit implements CommandExecutor {

    @Override
    public boolean onCommand(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        if(sender instanceof Player p){
            Inventory inventory = p.getInventory();
            inventory.clear();

            //Set the player's health and food level
            p.setHealth(20);
            p.setFoodLevel(20);
            p.setSaturation(20);

            //Adding the Kit
            EntityEquipment equipment = p.getEquipment();
            equipment.setHelmet(new ItemStack(Material.LEATHER_HELMET));
            equipment.setChestplate(new ItemStack(Material.LEATHER_CHESTPLATE));
            equipment.setLeggings(new ItemStack(Material.LEATHER_LEGGINGS));
            equipment.setBoots(new ItemStack(Material.LEATHER_BOOTS));

            //Give them the weapons
            inventory.addItem(new ItemStack(Material.STONE_SWORD, 1));
            inventory.addItem(new ItemStack(Material.CROSSBOW, 1));
            inventory.addItem(new ItemStack(Material.BOW, 1));
            inventory.addItem(new ItemStack(Material.ARROW, 8));
            inventory.addItem(new ItemStack(Material.LIME_WOOL, 64));
            inventory.addItem((new ItemStack(Material.SHEARS, 1)));
        }
        return true;
    }
}
