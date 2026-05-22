package de.qirdpdms.nivoraBedWars.listener;

import de.qirdpdms.nivoraBedWars.Main;
import de.qirdpdms.nivoraBedWars.model.PluginMode;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockSpreadEvent;

public class FireProtectionListener implements Listener {

    private final Main plugin;

    public FireProtectionListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent event) {
        if (plugin.getPluginMode() != PluginMode.GAME) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBurn(BlockBurnEvent event) {
        if (plugin.getPluginMode() != PluginMode.GAME) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSpread(BlockSpreadEvent event) {
        if (plugin.getPluginMode() != PluginMode.GAME) {
            return;
        }
        if (event.getSource().getType() == Material.FIRE || event.getSource().getType() == Material.SOUL_FIRE
                || event.getNewState().getType() == Material.FIRE || event.getNewState().getType() == Material.SOUL_FIRE) {
            event.setCancelled(true);
            event.getBlock().setType(Material.AIR, false);
        }
    }
}

