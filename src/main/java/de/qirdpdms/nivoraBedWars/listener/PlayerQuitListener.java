package de.qirdpdms.nivoraBedWars.listener;

import de.qirdpdms.nivoraBedWars.Main;
import de.qirdpdms.nivoraBedWars.model.PluginMode;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

    private final Main plugin;

    public PlayerQuitListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        event.setQuitMessage(null);
        plugin.setBuildMode(event.getPlayer().getUniqueId(), false);
        if (plugin.getPluginMode() == PluginMode.LOBBY) {
            plugin.getQueueManager().removeById(event.getPlayer().getUniqueId());
            return;
        }
        plugin.getGameManager().handleQuit(event.getPlayer());
    }
}

