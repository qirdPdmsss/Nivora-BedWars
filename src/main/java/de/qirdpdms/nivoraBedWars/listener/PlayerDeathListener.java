package de.qirdpdms.nivoraBedWars.listener;

import de.qirdpdms.nivoraBedWars.Main;
import de.qirdpdms.nivoraBedWars.util.BedwarsDropPolicy;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class PlayerDeathListener implements Listener {

    private final Main plugin;

    public PlayerDeathListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        event.setDeathMessage(null);
        List<ItemStack> drops = new ArrayList<>();
        for (ItemStack itemStack : event.getDrops()) {
            if (BedwarsDropPolicy.isAllowedGroundDrop(plugin.getConfigManager(), itemStack)) {
                drops.add(itemStack.clone());
            }
        }

        Location deathLocation = event.getEntity().getLocation();
        World world = deathLocation.getWorld();
        if (world != null) {
            for (ItemStack itemStack : drops) {
                Item dropped = world.dropItemNaturally(deathLocation, itemStack);
                plugin.getGameManager().trackDroppedItem(event.getEntity(), dropped);
            }
        }

        event.getDrops().clear();
        event.setDroppedExp(0);
        event.setKeepInventory(true);
        event.getEntity().getInventory().clear();
        event.getEntity().getInventory().setArmorContents(null);
        plugin.getGameManager().handleDeath(event.getEntity());
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (event.getEntity().isOnline()) {
                event.getEntity().spigot().respawn();
            }
        });
    }
}

