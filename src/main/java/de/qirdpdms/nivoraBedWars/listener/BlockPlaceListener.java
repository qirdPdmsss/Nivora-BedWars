package de.qirdpdms.nivoraBedWars.listener;

import de.qirdpdms.nivoraBedWars.Main;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public class BlockPlaceListener implements Listener {

    private final Main plugin;

    public BlockPlaceListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (plugin.isBuildMode(event.getPlayer().getUniqueId())) {
            return;
        }
        plugin.getGameManager().noteIntruderBaseAction(event.getPlayer());
        plugin.getGameManager().handleBlockPlace(event);
    }
}

