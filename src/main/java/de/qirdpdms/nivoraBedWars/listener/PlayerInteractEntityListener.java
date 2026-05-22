package de.qirdpdms.nivoraBedWars.listener;

import de.qirdpdms.nivoraBedWars.Main;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class PlayerInteractEntityListener implements Listener {

    private final Main plugin;

    public PlayerInteractEntityListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        plugin.getGameManager().noteIntruderBaseAction(event.getPlayer());
        plugin.handleShopEntityInteract(event);
    }
}


