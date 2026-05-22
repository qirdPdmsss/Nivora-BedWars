package de.qirdpdms.nivoraBedWars.listener;

import de.qirdpdms.nivoraBedWars.Main;
import de.qirdpdms.nivoraBedWars.model.PluginMode;
import org.bukkit.entity.Egg;
import org.bukkit.entity.Player;
import org.bukkit.entity.SmallFireball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.persistence.PersistentDataType;

public class ProjectileHitListener implements Listener {

    private final Main plugin;

    public ProjectileHitListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (plugin.getPluginMode() != PluginMode.GAME) {
            return;
        }

        if (event.getEntity() instanceof SmallFireball fireball) {
            Byte tag = fireball.getPersistentDataContainer().get(plugin.getArenaFireballKey(), PersistentDataType.BYTE);
            if (tag != null) {
                event.setCancelled(true);
                plugin.getGameManager().handleFireballHit(fireball);
            }
            return;
        }

        if (event.getEntity() instanceof Egg egg) {
            Byte tag = egg.getPersistentDataContainer().get(plugin.getBridgeEggKey(), PersistentDataType.BYTE);
            if (tag != null && !(event.getHitEntity() instanceof Player)) {
                if (event.getHitBlock() != null && event.getHitBlockFace() != null) {
                    if (egg.getShooter() instanceof Player shooter) {
                        plugin.getGameManager().handleBridgeEggHit(egg, event.getHitBlock(), event.getHitBlockFace(), shooter);
                    }
                }
                egg.remove();
            }
        }
    }
}

