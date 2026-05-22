package de.qirdpdms.nivoraBedWars.listener;

import de.qirdpdms.nivoraBedWars.Main;
import de.qirdpdms.nivoraBedWars.model.PluginMode;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;

public class EntityExplodeListener implements Listener {

    private final Main plugin;

    public EntityExplodeListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (plugin.getPluginMode() != PluginMode.GAME) {
            return;
        }
        plugin.getGameManager().handleExplosion(event);
    }
}

