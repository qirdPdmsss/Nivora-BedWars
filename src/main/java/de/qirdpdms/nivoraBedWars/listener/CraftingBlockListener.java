package de.qirdpdms.nivoraBedWars.listener;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

public class CraftingBlockListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        CraftingInventory craftingInventory = event.getInventory();
        craftingInventory.setResult(new ItemStack(Material.AIR));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        event.setCancelled(true);
        CraftingInventory craftingInventory = event.getInventory();
        craftingInventory.setResult(new ItemStack(Material.AIR));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryView view = event.getView();
        InventoryType topType = view.getTopInventory().getType();
        if (topType != InventoryType.CRAFTING && topType != InventoryType.WORKBENCH) {
            return;
        }
        if (event.getRawSlot() != 0) {
            return;
        }
        event.setCancelled(true);
        if (view.getTopInventory() instanceof CraftingInventory craftingInventory) {
            craftingInventory.setResult(new ItemStack(Material.AIR));
        }
    }
}


