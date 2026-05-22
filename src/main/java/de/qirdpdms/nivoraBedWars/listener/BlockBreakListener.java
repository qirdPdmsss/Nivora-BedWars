package de.qirdpdms.nivoraBedWars.listener;

import de.qirdpdms.nivoraBedWars.Main;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class BlockBreakListener implements Listener {

    private final Main plugin;

    public BlockBreakListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (plugin.isBuildMode(event.getPlayer().getUniqueId())) {
            return;
        }
        plugin.getGameManager().noteIntruderBaseAction(event.getPlayer());
        plugin.getGameManager().handleBedBreak(event);
    }
}

