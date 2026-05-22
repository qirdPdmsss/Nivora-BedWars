package de.qirdpdms.nivoraBedWars.manager;

import de.qirdpdms.nivoraBedWars.Main;
import de.qirdpdms.nivoraBedWars.model.GamePhase;
import de.qirdpdms.nivoraBedWars.model.TeamData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Egg;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SmallFireball;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class GameManager {

    private final Main plugin;
    private final Map<String, ArenaInstance> arenas;
    private BukkitTask task;

    public GameManager(Main plugin) {
        this.plugin = plugin;
        this.arenas = new LinkedHashMap<>();
        loadArenas();
    }

    public void start() {
        stop();
        loadArenas();
        this.task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
        for (Player player : Bukkit.getOnlinePlayers()) {
            handleJoin(player);
        }
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (ArenaInstance arena : arenas.values()) {
            arena.shutdown();
        }
        arenas.clear();
    }

    public void reload() {
        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        loadArenas();
        for (Player player : onlinePlayers) {
            handleJoin(player);
        }
    }

    public GamePhase getPhase() {
        boolean anyRunning = false;
        boolean anyStarting = false;
        boolean anyWaiting = false;
        boolean anyEnding = false;

        for (ArenaInstance arena : arenas.values()) {
            switch (arena.getPhase()) {
                case RUNNING -> anyRunning = true;
                case STARTING -> anyStarting = true;
                case WAITING -> anyWaiting = true;
                case ENDING -> anyEnding = true;
            }
        }

        if (anyRunning) {
            return GamePhase.RUNNING;
        }
        if (anyStarting) {
            return GamePhase.STARTING;
        }
        if (anyWaiting) {
            return GamePhase.WAITING;
        }
        return anyEnding ? GamePhase.ENDING : GamePhase.WAITING;
    }

    public String getStatusSummary() {
        if (arenas.isEmpty()) {
            return "keine Arenen geladen";
        }
        return arenas.values().stream()
                .map(ArenaInstance::getSummary)
                .collect(Collectors.joining(", "));
    }

    public List<Map<String, String>> getArenaStatusPlaceholders() {
        List<Map<String, String>> statusLines = new ArrayList<>();
        for (ArenaInstance arena : arenas.values()) {
            statusLines.add(Map.of(
                    "arena", arena.getArenaId(),
                    "map", arena.getArenaId(),
                    "phase", arena.getPhase().name(),
                    "players", String.valueOf(arena.getOnlineAssignedPlayerCount()),
                    "max", String.valueOf(arena.getMapConfig().getMaxPlayers())
            ));
        }
        return statusLines;
    }

    public void forceStart() {
        for (ArenaInstance arena : arenas.values()) {
            arena.forceStart();
        }
    }

    public int endActiveMatches() {
        int endedArenas = 0;
        for (ArenaInstance arena : arenas.values()) {
            if (arena.endMatchNow()) {
                endedArenas++;
            }
        }
        return endedArenas;
    }

    /**
     * Aktiviert den Solo-Test-Modus für eine Arena (min-player = 1) und startet sofort.
     * Optional kann eine bestimmte Arena-ID angegeben werden.
     */
    public String enableSoloTestAndStart(Player player, String arenaId) {
        ArenaInstance currentArena = findArena(player);
        ArenaInstance target = null;
        plugin.debug("bwtest", "enableSoloTestAndStart -> player=" + player.getName()
                + " currentArena=" + (currentArena == null ? "null" : currentArena.getArenaId())
                + " requestedArena=" + arenaId + " arenas=" + arenas.keySet());
        if (arenaId != null && !arenaId.isBlank()) {
            target = arenas.get(arenaId);
            if (target == null) {
                plugin.debug("bwtest", "enableSoloTestAndStart -> requested arena not found");
                return "Arena '" + arenaId + "' nicht gefunden.";
            }
        } else {
            target = currentArena;
            if (target == null) {
                target = arenas.values().stream()
                        .filter(arena -> arena.getPhase() == GamePhase.WAITING || arena.getPhase() == GamePhase.STARTING)
                        .findFirst()
                        .orElseGet(() -> arenas.values().stream().findFirst().orElse(null));
            }
        }
        if (target == null) {
            plugin.debug("bwtest", "enableSoloTestAndStart -> no target arena found");
            return "Keine Arena verfügbar.";
        }

        if (target.getPhase() == GamePhase.ENDING) {
            return "Arena '" + target.getArenaId() + "' wird gerade zurückgesetzt. Bitte kurz erneut versuchen.";
        }
        if (target.getPhase() == GamePhase.RUNNING) {
            return "Arena '" + target.getArenaId() + "' läuft bereits. Nutze zuerst /bwtest stop oder wähle eine andere Map.";
        }

        if (currentArena != null && currentArena != target) {
            plugin.debug("bwtest", "enableSoloTestAndStart -> removing player from old arena=" + currentArena.getArenaId());
            currentArena.removePlayerCompletely(player);
        }

        target.enableSoloTestMode();
        target.handleJoin(player);
        if (!target.isAssignedPlayer(player.getUniqueId())) {
            target.ensureAssignedForWaiting(player);
        }
        if (!target.isAssignedPlayer(player.getUniqueId())) {
            plugin.debug("bwtest", "enableSoloTestAndStart -> assignment failed target=" + target.getArenaId());
            return "Du konntest keinem Team in Arena '" + target.getArenaId() + "' zugewiesen werden.";
        }
        target.forceStart();

        if (target.getPhase() != GamePhase.RUNNING) {
            plugin.debug("bwtest", "enableSoloTestAndStart -> forceStart failed phase=" + target.getPhase());
            return "Die Arena '" + target.getArenaId() + "' konnte nicht gestartet werden.";
        }
        plugin.debug("bwtest", "enableSoloTestAndStart -> success target=" + target.getArenaId() + " phase=" + target.getPhase());
        return null; // null = Erfolg
    }

    /** Zerstört das Bett eines Teams in der Arena des Spielers. */
    public boolean destroyBedForTest(Player player, String teamId) {
        ArenaInstance arena = findArena(player);
        if (arena == null) {
            return false;
        }
        return arena.destroyBedByTeamId(teamId, player);
    }

    /** Gibt dem Spieler ein Test-Kit in seiner aktuellen Arena. */
    public boolean giveTestKit(Player player) {
        ArenaInstance arena = findArena(player);
        if (arena == null) {
            return false;
        }
        arena.giveTestKit(player);
        return true;
    }

    /** Beendet das Match des Spielers sofort. */
    public boolean endMatchForPlayer(Player player) {
        ArenaInstance arena = findArena(player);
        if (arena == null) {
            return false;
        }
        return arena.endMatchNow();
    }

    public boolean isSpectator(Player player) {
        ArenaInstance arena = findArena(player);
        return arena != null && (arena.isSpectator(player) || arena.isRespawning(player));
    }

    public boolean isManagedPlayer(Player player) {
        return findArena(player) != null;
    }

    public boolean isDamageAllowed(Player player) {
        ArenaInstance arena = findArena(player);
        return arena != null && arena.isDamageAllowed(player);
    }

    public boolean canDropItems(Player player) {
        ArenaInstance arena = findArena(player);
        return arena != null && arena.canDropItems(player);
    }

    public void noteIntruderBaseAction(Player player) {
        ArenaInstance arena = findArena(player);
        if (arena != null) {
            arena.noteIntruderBaseAction(player);
        }
    }

    public boolean isShopEntity(Entity entity) {
        if (entity == null) {
            return false;
        }
        for (ArenaInstance arena : arenas.values()) {
            if (arena.isSpawnedShopEntity(entity)) {
                return true;
            }
        }
        return false;
    }

    public void trackDroppedItem(Player player, Item item) {
        ArenaInstance arena = findArena(player);
        if (arena != null) {
            arena.trackDroppedItem(item);
        }
    }

    public void handleDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        ArenaInstance victimArena = findArena(victim);
        if (victimArena == null) {
            event.setCancelled(true);
            return;
        }

        if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
            event.setCancelled(true);
            if (victimArena.tryTriggerVoidRescue(victim)) {
                return;
            }
            if (victimArena.isDamageAllowed(victim)) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (victim.isOnline()) {
                        victim.setHealth(0.0D);
                    }
                });
            } else {
                Location safeLocation = victimArena.getRespawnLocation(victim);
                if (safeLocation != null) {
                    victim.teleport(safeLocation);
                }
            }
            return;
        }

        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            if (victimArena.shouldIgnoreEnderPearlFallDamage(victim)) {
                event.setCancelled(true);
                return;
            }
        }

        if (victimArena.isSpectator(victim) || !victimArena.isDamageAllowed(victim)) {
            event.setCancelled(true);
            return;
        }

        if (event instanceof EntityDamageByEntityEvent byEntityEvent) {
            if (byEntityEvent.getDamager() instanceof TNTPrimed primedTnt
                    && victimArena.handleSpecialTntDamage(byEntityEvent, victim, primedTnt)) {
                return;
            }
            Player attacker = resolveAttacker(byEntityEvent.getDamager());
            if (attacker != null && !victimArena.canPlayersInteract(attacker, victim)) {
                event.setCancelled(true);
                return;
            }
            if (attacker != null) {
                victimArena.registerCombatHit(attacker, victim);
            }
        }
    }

    public void handleJoin(Player player) {
        if (arenas.isEmpty()) {
            plugin.debug("join", "handleJoin -> no arenas loaded, kicking player=" + player.getName());
            player.kickPlayer("Keine BedWars-Arena verfugbar.");
            return;
        }

        ArenaInstance currentArena = findArena(player);
        if (currentArena != null) {
            plugin.debug("join", "handleJoin -> player already in arena=" + currentArena.getArenaId() + " phase=" + currentArena.getPhase());
            currentArena.handleJoin(player);
            return;
        }

        ArenaInstance targetArena = selectJoinArena();
        if (targetArena == null) {
            plugin.debug("join", "handleJoin -> no join arena, trying fallback for player=" + player.getName());
            targetArena = selectFallbackArena();
        }

        if (targetArena == null) {
            plugin.debug("join", "handleJoin -> no fallback arena, kicking player=" + player.getName());
            player.kickPlayer("Keine BedWars-Arena verfugbar.");
            return;
        }
        plugin.debug("join", "handleJoin -> targetArena=" + targetArena.getArenaId() + " phase=" + targetArena.getPhase()
                + " summary=" + targetArena.getSummary());
        targetArena.handleJoin(player);
    }

    public void handleQuit(Player player) {
        ArenaInstance arena = findArena(player);
        if (arena != null) {
            arena.handleQuit(player);
        }
    }

    public Location getRespawnLocation(Player player) {
        ArenaInstance arena = findArena(player);
        return arena == null ? null : arena.getRespawnLocation(player);
    }

    public void handleDeath(Player player) {
        ArenaInstance arena = findArena(player);
        if (arena != null) {
            arena.handleDeath(player);
        }
    }

    public void handleRespawn(Player player) {
        ArenaInstance arena = findArena(player);
        if (arena != null) {
            arena.handleRespawn(player);
        }
    }

    public TeamData getTeamByBedLocation(Location location) {
        ArenaInstance arena = findArenaByBedLocation(location);
        return arena == null ? null : arena.getTeamByBedLocation(location);
    }

    public boolean isOwnBed(Player player, TeamData bedTeam) {
        ArenaInstance arena = findArena(player);
        return arena != null && arena.isOwnBed(player, bedTeam);
    }

    public void destroyBed(TeamData teamData, Player breaker) {
        ArenaInstance arena = findArena(teamData);
        if (arena != null) {
            arena.destroyBed(teamData, breaker);
        }
    }

    public void handleBedBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!block.getType().name().endsWith("_BED")) {
            handleBlockBreak(event);
            return;
        }

        ArenaInstance arena = findArenaByBedLocation(block.getLocation());
        if (arena == null) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();
        TeamData teamData = arena.getTeamByBedLocation(block.getLocation());
        if (teamData == null || !teamData.isBedAlive()) {
            return;
        }
        if (!arena.canModifyBlocks(player)) {
            return;
        }
        if (arena.isOwnBed(player, teamData)) {
            plugin.getMessageManager().send(player, "game.own-bed", Map.of(
                    "map", arena.getArenaId(),
                    "arena", arena.getArenaId()
            ));
            return;
        }

        arena.destroyBed(teamData, player, block.getLocation());
        plugin.getMessageManager().send(player, "game.bed-broken", Map.of(
                "team", teamData.getDisplayName(),
                "player", player.getName(),
                "map", arena.getArenaId(),
                "arena", arena.getArenaId()
        ));
    }

    public void handleBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ArenaInstance arena = findArena(player);
        if (arena == null) {
            event.setCancelled(true);
            return;
        }

        if (!arena.handleRegularBlockBreak(event.getBlock(), player)) {
            event.setCancelled(true);
        }
    }

    public void handleBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        ArenaInstance arena = findArena(player);
        if (arena == null) {
            event.setCancelled(true);
            return;
        }

        if (!arena.handleBlockPlace(event.getBlockPlaced(), event.getBlockReplacedState(), player)) {
            event.setCancelled(true);
        }
    }

    public void handleEntityInteract(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        ArenaInstance arena = findArena(player);
        if (arena == null) {
            return;
        }

        if (arena.handleShopInteract(player, event.getRightClicked())) {
            event.setCancelled(true);
        }
    }

    public void handleInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        ArenaInstance arena = findArena(player);
        if (arena == null) {
            return;
        }
        arena.handleInventoryClick(event);
    }

    public boolean handleSpectatorCompass(Player player, Action action, org.bukkit.inventory.ItemStack itemStack) {
        ArenaInstance arena = findArena(player);
        return arena != null && arena.handleSpectatorCompass(player, action, itemStack);
    }

    public boolean handleAbilityInteract(Player player, Action action, org.bukkit.inventory.ItemStack itemStack) {
        ArenaInstance arena = findArena(player);
        return arena != null && arena.handleAbilityInteract(player, action, itemStack);
    }

    public void handleExplosion(EntityExplodeEvent event) {
        for (ArenaInstance arena : arenas.values()) {
            arena.handleExplosion(event);
        }
    }

    public void handleFireballHit(SmallFireball fireball) {
        Location loc = fireball.getLocation();
        for (ArenaInstance arena : arenas.values()) {
            if (arena.handleFireballHit(fireball, loc)) {
                return;
            }
        }
    }

    public void handleBridgeEggHit(Egg egg, Block hitBlock, BlockFace hitFace, Player shooter) {
        ArenaInstance arena = findArena(shooter);
        if (arena != null) {
            arena.handleBridgeEggHit(egg, hitBlock, hitFace, shooter);
        }
    }

    public void throwFireball(Player player, ItemStack item, EquipmentSlot hand) {
        ArenaInstance arena = findArena(player);
        if (arena == null || !arena.canThrowSpecialItem(player)
                || !arena.tryUseSpecialItem(player, "fireball", plugin.getConfigManager().getSpecialItemUseCooldownMillis("fireball", 600L),
                Material.FIRE_CHARGE, "&cFireball lädt noch nach.")) {
            return;
        }
        decrementHeldItem(player, item, hand);
        player.getWorld().spawn(player.getEyeLocation(), SmallFireball.class, fb -> {
            fb.setShooter(player);
            fb.setDirection(player.getLocation().getDirection().normalize().multiply(1.5D));
            fb.getPersistentDataContainer().set(plugin.getArenaFireballKey(), PersistentDataType.BYTE, (byte) 1);
            fb.setIsIncendiary(false);
            fb.setYield(0f);
        });
        player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.2f);
    }

    public void throwBridgeEgg(Player player, ItemStack item, EquipmentSlot hand) {
        ArenaInstance arena = findArena(player);
        if (arena == null || !arena.canThrowSpecialItem(player)
                || !arena.tryUseSpecialItem(player, "bridge-egg", plugin.getConfigManager().getSpecialItemUseCooldownMillis("bridge-egg", 300L),
                Material.EGG, "&cBridge Egg lädt noch nach.")) {
            return;
        }
        decrementHeldItem(player, item, hand);
        Egg egg = player.getWorld().spawn(player.getEyeLocation(), Egg.class, e -> {
            e.setShooter(player);
            e.setVelocity(player.getLocation().getDirection().normalize().multiply(1.3D));
            e.getPersistentDataContainer().set(plugin.getBridgeEggKey(), PersistentDataType.BYTE, (byte) 1);
        });

        final BukkitTask[] taskRef = new BukkitTask[1];
        final int[] livedTicks = {0};
        taskRef[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!egg.isValid() || egg.isDead() || livedTicks[0]++ > plugin.getConfigManager().getBridgeEggTrailLifetimeTicks()) {
                if (taskRef[0] != null) {
                    taskRef[0].cancel();
                }
                return;
            }
            arena.handleBridgeEggTrail(egg, player);
        }, 1L, 1L);
    }


    public void useTnt(Player player, ItemStack item, EquipmentSlot hand, Block clickedBlock, BlockFace blockFace) {
        ArenaInstance arena = findArena(player);
        if (arena == null || !arena.canThrowSpecialItem(player)) {
            return;
        }

        String action = arena.resolveTntAction(item);
        Location spawnLocation = arena.resolveSpecialTntSpawnLocation(player, action, clickedBlock, blockFace);
        if (spawnLocation == null) {
            return;
        }

        if (!arena.tryUseSpecialItem(player, action, plugin.getConfigManager().getSpecialItemUseCooldownMillis(action, getDefaultTntCooldownMillis(action)),
                Material.TNT, arena.getTntCooldownMessage(action))) {
            return;
        }

        decrementHeldItem(player, item, hand);

        player.getWorld().spawn(spawnLocation, TNTPrimed.class, primed -> {
            arena.configurePrimedTnt(primed, player, action, 0);
        });
        player.getWorld().playSound(spawnLocation, org.bukkit.Sound.ENTITY_TNT_PRIMED, 1.0F, 1.0F);
    }

    public boolean handleEnderPearlUse(Player player, Material material) {
        ArenaInstance arena = findArena(player);
        if (arena == null) {
            return true;
        }
        return !arena.tryUseSpecialItem(player, "ender-pearl", plugin.getConfigManager().getSpecialItemUseCooldownMillis("ender-pearl", 6000L),
                material, "&cEnderperle ist noch auf Cooldown.");
    }

    private void decrementHeldItem(Player player, ItemStack item, EquipmentSlot hand) {
        if (item == null || player == null) {
            return;
        }
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
            return;
        }

        if (hand == EquipmentSlot.OFF_HAND) {
            player.getInventory().setItemInOffHand(null);
            return;
        }
        player.getInventory().setItemInMainHand(null);
    }

    private long getDefaultTntCooldownMillis(String action) {
        return switch (action == null ? "tnt" : action.toLowerCase()) {
            case "breach-tnt" -> 750L;
            case "jump-tnt" -> 650L;
            case "cluster-tnt" -> 1400L;
            case "sticky-tnt" -> 850L;
            default -> 600L;
        };
    }

    public void handleProjectileHit(org.bukkit.event.entity.ProjectileHitEvent event) {
        // Handled via specific methods above; no-op needed here
    }

    private void loadArenas() {
        for (ArenaInstance arena : arenas.values()) {
            arena.shutdown();
        }
        arenas.clear();
        List<String> arenaIds = plugin.getConfigManager().getArenaMapIds();
        plugin.debug("arena-load", "loadArenas -> pluginMode=" + plugin.getPluginMode()
                + " service=" + plugin.getConfigManager().getCurrentServiceName()
                + " gameMapId=" + plugin.getConfigManager().getGameMapId()
                + " arenaIds=" + arenaIds);
        for (String arenaId : arenaIds) {
            if (!plugin.getConfigManager().mapExists(arenaId)) {
                plugin.getLogger().warning("[BedWars] Arena-Map " + arenaId + " ist nicht konfiguriert und wird ubersprungen.");
                continue;
            }
            arenas.put(arenaId, new ArenaInstance(plugin, arenaId));
            plugin.debug("arena-load", "loadArenas -> created arena=" + arenaId);
        }
    }

    private void tick() {
        for (ArenaInstance arena : arenas.values()) {
            arena.tick();
        }
    }

    private ArenaInstance selectJoinArena() {
        List<ArenaInstance> joinableArenas = arenas.values().stream()
                .filter(ArenaInstance::canAcceptPlayers)
                .toList();
        plugin.debug("join", "selectJoinArena -> candidates=" + joinableArenas.stream().map(ArenaInstance::getSummary).toList());
        if (joinableArenas.isEmpty()) {
            return null;
        }

        return joinableArenas.stream()
                .max(Comparator
                        .comparingInt(this::getJoinPriority)
                        .thenComparingInt(ArenaInstance::getAssignedPlayerCount))
                .orElse(null);
    }

    private ArenaInstance selectFallbackArena() {
        ArenaInstance arena = arenas.values().stream()
                .min(Comparator.comparingInt(ArenaInstance::getParticipantCount))
                .orElse(null);
        plugin.debug("join", "selectFallbackArena -> result=" + (arena == null ? "null" : arena.getSummary()));
        return arena;
    }

    private int getJoinPriority(ArenaInstance arena) {
        return switch (arena.getPhase()) {
            case STARTING -> 2;
            case WAITING -> 1;
            default -> 0;
        };
    }

    private ArenaInstance findArena(Player player) {
        if (player == null) {
            return null;
        }
        return findArena(player.getUniqueId());
    }

    private ArenaInstance findArena(UUID uuid) {
        for (ArenaInstance arena : arenas.values()) {
            if (arena.containsPlayer(uuid)) {
                return arena;
            }
        }
        return null;
    }

    private ArenaInstance findArena(TeamData teamData) {
        if (teamData == null) {
            return null;
        }
        for (ArenaInstance arena : arenas.values()) {
            if (arena.getMapConfig().getTeams().contains(teamData)) {
                return arena;
            }
        }
        return null;
    }

    private ArenaInstance findArenaByBedLocation(Location location) {
        if (location == null) {
            return null;
        }
        for (ArenaInstance arena : arenas.values()) {
            if (arena.getTeamByBedLocation(location) != null) {
                return arena;
            }
        }
        return null;
    }

    private Player resolveAttacker(Entity entity) {
        if (entity instanceof Player player) {
            return player;
        }
        if (entity instanceof Projectile projectile) {
            ProjectileSource source = projectile.getShooter();
            if (source instanceof Player player) {
                return player;
            }
        }
        return null;
    }
}

