package de.qirdpdms.nivoraBedWars.listener;

import de.qirdpdms.nivoraBedWars.Main;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;

public class PlayerKickListener implements Listener {

    private final Main plugin;

    public PlayerKickListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onKick(PlayerKickEvent event) {
        event.setLeaveMessage("");
        plugin.setBuildMode(event.getPlayer().getUniqueId(), false);
    }
}


