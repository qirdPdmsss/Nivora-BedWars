package de.qirdpdms.nivoraBedWars.listener;

import de.qirdpdms.nivoraBedWars.Main;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class EntityDamageListener implements Listener {

    private final Main plugin;

    public EntityDamageListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (plugin.getGameManager().isShopEntity(event.getEntity())) {
            event.setCancelled(true);
            return;
        }
        plugin.getGameManager().handleDamage(event);
    }
}

