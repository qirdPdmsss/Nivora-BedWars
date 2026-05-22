package de.qirdpdms.nivoraBedWars.listener;

import de.qirdpdms.nivoraBedWars.Main;
import de.qirdpdms.nivoraBedWars.model.PluginMode;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;

public class PlayerBedEnterListener implements Listener {

    private final Main plugin;

    public PlayerBedEnterListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBedEnter(PlayerBedEnterEvent event) {
        if (plugin.getPluginMode() != PluginMode.GAME) {
            return;
        }
        if (!plugin.getGameManager().isManagedPlayer(event.getPlayer())) {
            return;
        }
        event.setCancelled(true);
    }
}

