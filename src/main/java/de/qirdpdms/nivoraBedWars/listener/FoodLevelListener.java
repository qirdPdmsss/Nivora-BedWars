package de.qirdpdms.nivoraBedWars.listener;

import de.qirdpdms.nivoraBedWars.Main;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;

public class FoodLevelListener implements Listener {

    private final Main plugin;

    public FoodLevelListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onFoodChange(FoodLevelChangeEvent event) {
        if (!plugin.getConfigManager().isHungerDisabled()) {
            return;
        }
        if (event.getEntity() instanceof Player) {
            event.setCancelled(true);
        }
    }
}

