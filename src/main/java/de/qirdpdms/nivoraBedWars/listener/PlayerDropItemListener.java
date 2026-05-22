package de.qirdpdms.nivoraBedWars.listener;

import de.qirdpdms.nivoraBedWars.Main;
import de.qirdpdms.nivoraBedWars.model.PluginMode;
import de.qirdpdms.nivoraBedWars.util.BedwarsDropPolicy;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;

public class PlayerDropItemListener implements Listener {

    private final Main plugin;

    public PlayerDropItemListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (plugin.getPluginMode() != PluginMode.GAME) {
            return;
        }
        if (plugin.getGameManager() == null) {
            event.setCancelled(true);
            return;
        }
        if (!plugin.getGameManager().canDropItems(event.getPlayer())) {
            event.setCancelled(true);
            return;
        }

        if (!BedwarsDropPolicy.isAllowedGroundDrop(plugin.getConfigManager(), event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
            return;
        }

        plugin.getGameManager().trackDroppedItem(event.getPlayer(), event.getItemDrop());
    }
}

