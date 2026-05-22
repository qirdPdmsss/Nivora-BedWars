package de.qirdpdms.nivoraBedWars.manager;

import de.qirdpdms.nivoraBedWars.Main;
import de.qirdpdms.nivoraBedWars.model.AllocationResult;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class QueueManager {

    private final Main plugin;
    private final Set<UUID> queue;
    private BukkitTask task;

    public QueueManager(Main plugin) {
        this.plugin = plugin;
        this.queue = new LinkedHashSet<>();
    }

    public synchronized void start() {
        stop();
        this.task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, plugin.getConfigManager().getQueueTickIntervalTicks());
    }

    public synchronized void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        queue.clear();
    }

    public synchronized void reload() {
        start();
    }

    public synchronized boolean join(Player player) {
        if (queue.contains(player.getUniqueId())) {
            return false;
        }
        queue.add(player.getUniqueId());
        plugin.getMessageManager().send(player, "queue.joined", Map.of(
                "player", player.getName(),
                "value", String.valueOf(queue.size())
        ));
        return true;
    }

    public synchronized boolean leave(Player player) {
        boolean removed = queue.remove(player.getUniqueId());
        if (removed) {
            plugin.getMessageManager().send(player, "queue.left", Map.of(
                    "player", player.getName(),
                    "value", String.valueOf(queue.size())
            ));
        }
        return removed;
    }

    public synchronized boolean isQueued(Player player) {
        return queue.contains(player.getUniqueId());
    }

    public synchronized int size() {
        return queue.size();
    }

    public synchronized void removeById(UUID uuid) {
        queue.remove(uuid);
    }

    private synchronized void tick() {
        int minPlayers = plugin.getConfigManager().getQueueMinPlayers();
        int maxPlayers = plugin.getConfigManager().getQueueBatchMaxPlayers();

        logVerbose("Queue-Tick: " + queue.size() + " Spieler in Queue, Min=" + minPlayers);

        if (queue.size() < minPlayers) {
            return;
        }

        List<Player> players = new ArrayList<>();
        Iterator<UUID> iterator = queue.iterator();
        while (iterator.hasNext()) {
            if (players.size() >= maxPlayers) {
                break;
            }
            UUID uuid = iterator.next();
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                logVerbose("Spieler offline, wird aus Queue entfernt: " + uuid);
                iterator.remove();
                continue;
            }
            players.add(player);
        }

        if (players.size() < minPlayers) {
            logVerbose("Zu wenige online Spieler nach Bereinigung: " + players.size() + "/" + minPlayers);
            return;
        }

        plugin.getLogger().info("[BedWars] Batch von " + players.size() + " Spielern wird allokiert...");
        AllocationResult allocationResult = plugin.getServerAllocatorService().allocate(players.size());
        if (allocationResult == null) {
            plugin.getLogger().warning("[BedWars] Keine Service-Allokation moglich. Spieler bleiben in der Queue.");
            return;
        }
        plugin.getLogger().info("[BedWars] Allokation: Service=" + allocationResult.getServiceName() + ", Map=" + allocationResult.getMapId());

        for (Player player : players) {
            UUID uuid = player.getUniqueId();
            queue.remove(uuid);
            plugin.getMessageManager().send(player, "queue.match-found", Map.of(
                    "player", player.getName(),
                    "map", allocationResult.getMapId(),
                    "value", allocationResult.getServiceName()
            ));

            plugin.getLogger().info("[BedWars] Versuche " + player.getName() + " auf " + allocationResult.getServiceName() + " zu senden...");
            boolean connected = plugin.getCloudNetService().sendPlayer(player, allocationResult.getServiceName());
            if (!connected) {
                plugin.getLogger().warning("[BedWars] Transfer von " + player.getName() + " fehlgeschlagen! Prüfe CloudNet-Bridge und Connect-Template.");
                plugin.getServerAllocatorService().release(allocationResult.getServiceName(), 1);
                String fallback = plugin.getConfigManager().getFallbackService();
                if (!fallback.isBlank()) {
                    plugin.getLogger().info("[BedWars] Versuche Fallback-Service: " + fallback);
                    plugin.getCloudNetService().sendPlayer(player, fallback);
                }
                continue;
            }

            long releaseDelay = plugin.getConfigManager().getServiceReservationSeconds() * 20L;
            Bukkit.getScheduler().runTaskLater(plugin, () ->
                    plugin.getServerAllocatorService().release(allocationResult.getServiceName(), 1), releaseDelay);
        }
    }

    private void logVerbose(String message) {
        if (!plugin.getConfigManager().isQueueVerboseLoggingEnabled()) {
            return;
        }
        plugin.getLogger().info("[BedWars] " + message);
    }
}

