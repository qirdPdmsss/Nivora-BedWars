package de.qirdpdms.nivoraBedWars.listener;

import de.qirdpdms.nivoraBedWars.Main;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

public class PlayerRespawnListener implements Listener {

    private final Main plugin;

    public PlayerRespawnListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Location location = plugin.getGameManager().getRespawnLocation(event.getPlayer());
        if (location != null) {
            event.setRespawnLocation(location);
        }
        plugin.getGameManager().handleRespawn(event.getPlayer());
    }
}

