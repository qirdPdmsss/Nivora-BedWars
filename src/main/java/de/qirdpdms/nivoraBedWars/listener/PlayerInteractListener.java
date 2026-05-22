package de.qirdpdms.nivoraBedWars.listener;

import de.qirdpdms.nivoraBedWars.Main;
import de.qirdpdms.nivoraBedWars.model.PluginMode;
import de.qirdpdms.nivoraBedWars.util.ColorUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;

public class PlayerInteractListener implements Listener {

    private final Main plugin;

    public PlayerInteractListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null || item.getType().name().equals("AIR")) {
            return;
        }

        if (plugin.getPluginMode() == PluginMode.GAME) {
            Player player = event.getPlayer();
            plugin.getGameManager().noteIntruderBaseAction(player);

            if (item.getType() == Material.TNT) {
                event.setCancelled(true);
                plugin.getGameManager().useTnt(player, item, event.getHand(), event.getClickedBlock(), event.getBlockFace());
                return;
            }

            // Fireball (FIRE_CHARGE) → eigenen SmallFireball werfen
            if (item.getType() == Material.FIRE_CHARGE) {
                event.setCancelled(true);
                plugin.getGameManager().throwFireball(player, item, event.getHand());
                return;
            }

            // Bridge-Egg (EGG) → getaggtes Ei werfen
            if (item.getType() == Material.EGG) {
                event.setCancelled(true);
                plugin.getGameManager().throwBridgeEgg(player, item, event.getHand());
                return;
            }


            if (item.getType() == Material.ENDER_PEARL) {
                if (plugin.getGameManager().handleEnderPearlUse(player, item.getType())) {
                    event.setCancelled(true);
                }
                return;
            }

            boolean handled = plugin.getGameManager().handleSpectatorCompass(player, action, item);
            if (handled) {
                event.setCancelled(true);
                return;
            }

            boolean abilityHandled = plugin.getGameManager().handleAbilityInteract(player, action, item);
            if (abilityHandled) {
                event.setCancelled(true);
            }
            return;
        }

        if (!plugin.getConfigManager().isLobbyJoinItemEnabled()) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        String configuredName = ColorUtil.colorize(plugin.getConfigManager().getJoinItemName());
        if (!configuredName.equals(meta.getDisplayName())) {
            return;
        }

        event.setCancelled(true);
        boolean joined = plugin.getQueueManager().join(event.getPlayer());
        if (!joined) {
            plugin.getMessageManager().send(event.getPlayer(), "queue.already");
        } else {
            plugin.getMessageManager().send(event.getPlayer(), "queue.waiting", Map.of(
                    "value", String.valueOf(plugin.getQueueManager().size())
            ));
        }
    }
}
