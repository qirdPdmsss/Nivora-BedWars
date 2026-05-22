package de.qirdpdms.nivoraBedWars.listener;

import de.qirdpdms.nivoraBedWars.Main;
import de.qirdpdms.nivoraBedWars.model.PluginMode;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

public class PlayerTeleportListener implements Listener {

    private final Main plugin;

    public PlayerTeleportListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (plugin.getPluginMode() != PluginMode.GAME) {
            return;
        }
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.SPECTATE) {
            return;
        }
        if (!plugin.getGameManager().isSpectator(event.getPlayer())) {
            return;
        }
        event.setCancelled(true);
    }
}

