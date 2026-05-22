package de.qirdpdms.nivoraBedWars.listener;

import de.qirdpdms.nivoraBedWars.Main;
import de.qirdpdms.nivoraBedWars.model.PluginMode;
import de.qirdpdms.nivoraBedWars.util.ColorUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Map;

public class PlayerJoinListener implements Listener {

    private final Main plugin;

    public PlayerJoinListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        event.setJoinMessage(null);
        Player player = event.getPlayer();
        plugin.debug("join", "PlayerJoinEvent -> player=" + player.getName()
                + " world=" + player.getWorld().getName()
                + " location=" + plugin.formatLocation(player.getLocation())
                + " mode=" + plugin.getPluginMode());
        if (plugin.getPluginMode() == PluginMode.LOBBY) {
            if (plugin.getConfigManager().isLobbyJoinItemEnabled()) {
                giveJoinItem(player);
            }
            return;
        }
        plugin.getGameManager().handleJoin(player);
    }

    private void giveJoinItem(Player player) {
        Material material = Material.matchMaterial(plugin.getConfigManager().getJoinItemMaterial());
        if (material == null || material == Material.AIR) {
            return;
        }
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.colorize(plugin.getConfigManager().getJoinItemName()));
            List<String> lore = plugin.getConfigManager().getJoinItemLore().stream().map(ColorUtil::colorize).toList();
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        player.getInventory().setItem(plugin.getConfigManager().getJoinItemSlot(), item);
    }
}

