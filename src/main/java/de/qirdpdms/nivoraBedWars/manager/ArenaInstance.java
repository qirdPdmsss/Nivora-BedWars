package de.qirdpdms.nivoraBedWars.manager;

import de.qirdpdms.nivoraBedWars.Main;
import de.qirdpdms.nivoraBedWars.model.GeneratorConfig;
import de.qirdpdms.nivoraBedWars.model.GeneratorTierConfig;
import de.qirdpdms.nivoraBedWars.model.GamePhase;
import de.qirdpdms.nivoraBedWars.model.MapConfig;
import de.qirdpdms.nivoraBedWars.model.PlayerMatchState;
import de.qirdpdms.nivoraBedWars.model.ShopCategoryConfig;
import de.qirdpdms.nivoraBedWars.model.ShopOfferConfig;
import de.qirdpdms.nivoraBedWars.model.ShopConfig;
import de.qirdpdms.nivoraBedWars.model.TeamData;
import de.qirdpdms.nivoraBedWars.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.ChatColor;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.SmallFireball;
import org.bukkit.entity.Egg;
import org.bukkit.entity.Villager;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public class ArenaInstance {

    private static final int[] SPECTATOR_PLAYER_DISPLAY_SLOTS = new int[]{2, 3, 4, 5, 6, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19,
            20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35};
    private static final long KILL_CREDIT_WINDOW_MS = 15_000L;
    private static final int BASE_REGION_PADDING_XZ = 3;
    private static final int BASE_REGION_PADDING_Y = 2;
    private static final int ARENA_CONTAINER_CLEAR_PADDING_XZ = 12;
    private static final int ARENA_CONTAINER_CLEAR_PADDING_Y = 6;
    private static final int SPECTATOR_PLAYER_SLOTS_PER_PAGE = SPECTATOR_PLAYER_DISPLAY_SLOTS.length;
    private static final int SPECTATOR_PREV_SLOT = 47;
    private static final int SPECTATOR_INFO_SLOT = 49;
    private static final int SPECTATOR_NEXT_SLOT = 51;
    private static final int SPECTATOR_TEAM_SLOT_START = 36;
    private static final int SPECTATOR_TEAM_SLOT_END = 43;
    private static final int ITEM_SHOP_SIZE = 54;
    private static final int ITEM_SHOP_INFO_SLOT = 48;
    private static final int ITEM_SHOP_STATUS_SLOT = 49;
    private static final int ITEM_SHOP_CLOSE_SLOT = 50;

    private final Main plugin;
    private final String arenaId;
    private final Map<UUID, TeamData> playerTeams;
    private final Set<UUID> spectators;
    private final Set<UUID> respawningPlayers;
    private final Map<UUID, PlayerMatchState> playerStates;
    private final Map<UUID, Long> offlineReservations;
    private final Map<String, List<StoredBlockState>> bedSnapshotsByTeam;
    private final Map<String, StoredBlockState> modifiedBlocks;
    private final Set<String> placedBlockKeys;
    private final List<GeneratorRuntime> generatorRuntimes;
    private final Map<UUID, ShopConfig> spawnedShopEntities;
    private final Map<UUID, Integer> spectatorTargetIndices;
    private final Map<String, TeamUpgradeState> teamUpgradeStates;
    private final NamespacedKey generatorDropKey;
    private final NamespacedKey arenaDropKey;
    private final NamespacedKey bridgeEggKey;
    private final NamespacedKey arenaFireballKey;
    private final NamespacedKey tntVariantKey;
    private final NamespacedKey tntOwnerKey;
    private final NamespacedKey tntClusterGenerationKey;
    private final Set<UUID> trackedDroppedItems;
    private final Map<UUID, UUID> lastDamagerByVictim;
    private final Map<UUID, Long> lastDamageTimestampByVictim;
    private final Map<UUID, Integer> killsByPlayer;
    private final Map<UUID, Integer> finalKillsByPlayer;
    private final Map<UUID, Long> shopInteractCooldowns;
    private final Map<UUID, Long> shopClickCooldowns;
    private final Map<String, Integer> bedBreakSecondByTeam;
    private final Map<String, Long> warningLastLogAt;
    private final Set<Integer> announcedBedWarningSeconds;
    private final Map<UUID, PlayerLoadoutState> playerLoadoutStates;
    private final Map<UUID, SidebarState> sidebarStates;

    private MapConfig mapConfig;
    private GamePhase phase;
    private int countdown;
    private int resetCountdown;
    private int runningSeconds;
    private boolean endgameBedsDestroyed;
    private boolean suddenDeathActive;
    private int cleanupTickerSeconds;
    private int diagnosticsTickerSeconds;
    private int runningParticipantsAtStart;
    private int runningQuitCount;
    private int totalKillCount;
    private int totalFinalKillCount;
    private boolean matchMetricsLogged;
    private boolean soloTestMode;
    private int sidebarTickerSeconds;
    private Map<String, List<ShopCategoryConfig>> cachedShopCategoriesByType;
    private Map<String, List<ShopOfferConfig>> cachedShopOffersByType;

    public ArenaInstance(Main plugin, String arenaId) {
        this.plugin = plugin;
        this.arenaId = arenaId;
        this.playerTeams = new ConcurrentHashMap<>();
        this.spectators = ConcurrentHashMap.newKeySet();
        this.respawningPlayers = ConcurrentHashMap.newKeySet();
        this.playerStates = new ConcurrentHashMap<>();
        this.offlineReservations = new ConcurrentHashMap<>();
        this.bedSnapshotsByTeam = new LinkedHashMap<>();
        this.modifiedBlocks = new LinkedHashMap<>();
        this.placedBlockKeys = new LinkedHashSet<>();
        this.generatorRuntimes = new ArrayList<>();
        this.spawnedShopEntities = new LinkedHashMap<>();
        this.spectatorTargetIndices = new LinkedHashMap<>();
        this.teamUpgradeStates = new ConcurrentHashMap<>();
        this.generatorDropKey = new NamespacedKey(plugin, "generator-drop-id");
        this.arenaDropKey = new NamespacedKey(plugin, "arena-drop-id");
        this.bridgeEggKey = plugin.getBridgeEggKey();
        this.arenaFireballKey = plugin.getArenaFireballKey();
        this.tntVariantKey = plugin.getTntVariantKey();
        this.tntOwnerKey = plugin.getTntOwnerKey();
        this.tntClusterGenerationKey = plugin.getTntClusterGenerationKey();
        this.trackedDroppedItems = ConcurrentHashMap.newKeySet();
        this.lastDamagerByVictim = new ConcurrentHashMap<>();
        this.lastDamageTimestampByVictim = new ConcurrentHashMap<>();
        this.killsByPlayer = new ConcurrentHashMap<>();
        this.finalKillsByPlayer = new ConcurrentHashMap<>();
        this.shopInteractCooldowns = new ConcurrentHashMap<>();
        this.shopClickCooldowns = new ConcurrentHashMap<>();
        this.bedBreakSecondByTeam = new ConcurrentHashMap<>();
        this.warningLastLogAt = new ConcurrentHashMap<>();
        this.announcedBedWarningSeconds = ConcurrentHashMap.newKeySet();
        this.playerLoadoutStates = new ConcurrentHashMap<>();
        this.sidebarStates = new ConcurrentHashMap<>();
        this.endgameBedsDestroyed = false;
        this.suddenDeathActive = false;
        this.cleanupTickerSeconds = 0;
        this.diagnosticsTickerSeconds = 0;
        this.sidebarTickerSeconds = 0;
        this.runningParticipantsAtStart = 0;
        this.runningQuitCount = 0;
        this.totalKillCount = 0;
        this.totalFinalKillCount = 0;
        this.matchMetricsLogged = false;
        this.soloTestMode = false;
        this.cachedShopCategoriesByType = new LinkedHashMap<>();
        this.cachedShopOffersByType = new LinkedHashMap<>();
        loadBlueprint();
    }

    public String getArenaId() {
        return arenaId;
    }

    public MapConfig getMapConfig() {
        return mapConfig;
    }

    public GamePhase getPhase() {
        return phase;
    }

    public boolean containsPlayer(UUID uuid) {
        return playerTeams.containsKey(uuid) || spectators.contains(uuid) || offlineReservations.containsKey(uuid);
    }

    public boolean isSpectator(Player player) {
        return player != null && spectators.contains(player.getUniqueId());
    }

    public boolean isRespawning(Player player) {
        return player != null && respawningPlayers.contains(player.getUniqueId());
    }

    public boolean canAcceptPlayers() {
        return (phase == GamePhase.WAITING || phase == GamePhase.STARTING)
                && playerTeams.size() < mapConfig.getMaxPlayers()
                && mapConfig.getTeams().stream().anyMatch(TeamData::hasSpace);
    }

    public int getAssignedPlayerCount() {
        return playerTeams.size();
    }

    public int getOnlineAssignedPlayerCount() {
        int count = 0;
        for (UUID uuid : playerTeams.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                count++;
            }
        }
        return count;
    }

    public int getParticipantCount() {
        return playerTeams.size() + spectators.size();
    }

    public String getSummary() {
        return arenaId + ":" + phase.name() + "(" + getOnlineAssignedPlayerCount() + "/" + mapConfig.getMaxPlayers() + ")";
    }

    public void tick() {
        cleanupOfflinePlayers();
        sidebarTickerSeconds++;

        if (phase == GamePhase.ENDING) {
            if (!hasOnlineParticipants()) {
                resetArena();
                return;
            }
            resetCountdown--;
            if (resetCountdown <= 0) {
                resetArena();
            }
            return;
        }

        if (phase == GamePhase.RUNNING) {
            runningSeconds++;
            cleanupTickerSeconds++;
            diagnosticsTickerSeconds++;
            tickGenerators();
            tickTraps();
            tickEndgame();
            if (cleanupTickerSeconds >= plugin.getConfigManager().getItemCleanupIntervalSeconds()) {
                cleanupTickerSeconds = 0;
                cleanupTrackedDrops();
            }
            if (plugin.getConfigManager().isMonitoringEnabled()
                    && diagnosticsTickerSeconds >= plugin.getConfigManager().getMonitoringDiagnosticsIntervalSeconds()) {
                diagnosticsTickerSeconds = 0;
                runDiagnosticsWarnings();
            }
        }

        if (sidebarTickerSeconds >= plugin.getConfigManager().getSidebarRefreshIntervalSeconds()) {
            sidebarTickerSeconds = 0;
            updateSidebarForParticipants();
        }

        int onlinePlayers = getOnlineAssignedPlayerCount();
        if (phase == GamePhase.WAITING || phase == GamePhase.STARTING) {
            int effectiveMinPlayers = soloTestMode ? 1 : mapConfig.getMinPlayers();
            if (onlinePlayers < effectiveMinPlayers) {
                phase = GamePhase.WAITING;
                countdown = plugin.getConfigManager().getGameStartCountdown();
                return;
            }

            phase = GamePhase.STARTING;
            if (countdown <= 0) {
                startRunning();
                return;
            }

                if (countdown <= 10 || countdown % 5 == 0) {
                broadcast("game.countdown", Map.of(
                        "value", String.valueOf(countdown),
                        "map", arenaId,
                        "arena", arenaId
                ));
                    if (countdown <= 5) {
                        showTitleToParticipants(
                                "game.title.countdown-title",
                                "game.title.countdown-subtitle",
                                Map.of(
                                        "value", String.valueOf(countdown),
                                        "map", arenaId,
                                        "arena", arenaId
                                )
                        );
                    }
            }
            countdown--;
            return;
        }

        if (phase == GamePhase.RUNNING && onlinePlayers == 0) {
            resetArena();
        }
    }

    public void handleJoin(Player player) {
        if (player == null) {
            return;
        }

        plugin.debug("arena-join", "handleJoin -> arena=" + arenaId + " player=" + player.getName()
                + " phase=" + phase + " canAccept=" + canAcceptPlayers()
                + " onlineAssigned=" + getOnlineAssignedPlayerCount() + "/" + mapConfig.getMaxPlayers()
                + " lobbySpawn=" + plugin.formatLocation(mapConfig.getLobbySpawn()));

        if (tryRestoreOfflineReservation(player)) {
            plugin.debug("arena-join", "handleJoin -> restored offline reservation for player=" + player.getName());
            return;
        }

        if (phase == GamePhase.RUNNING || phase == GamePhase.ENDING || !canAcceptPlayers()) {
            plugin.debug("arena-join", "handleJoin -> sending player to spectator reason="
                    + (phase == GamePhase.RUNNING ? "running" : phase == GamePhase.ENDING ? "ending" : "cannot-accept"));
            setSpectator(player);
            return;
        }

        spectators.remove(player.getUniqueId());
        respawningPlayers.remove(player.getUniqueId());
        prepareWaitingPlayer(player);
        assignTeamIfNeeded(player);
        plugin.debug("arena-join", "handleJoin -> after assign player=" + player.getName()
                + " assigned=" + isAssignedPlayer(player.getUniqueId())
                + " location=" + plugin.formatLocation(player.getLocation()));
    }

    public void handleQuit(Player player) {
        if (player == null) {
            return;
        }

        UUID uuid = player.getUniqueId();
        sidebarStates.remove(uuid);
        TeamData teamData = playerTeams.get(uuid);
        boolean countedAsRunningQuit = phase == GamePhase.RUNNING && teamData != null && !spectators.contains(uuid);

        if (phase == GamePhase.RUNNING && teamData != null && teamData.isBedAlive() && !spectators.contains(uuid)) {
            markOfflineReservation(uuid);
        } else {
            spectators.remove(uuid);
            respawningPlayers.remove(uuid);
            playerStates.remove(uuid);
            offlineReservations.remove(uuid);
            teamData = playerTeams.remove(uuid);
            if (teamData != null) {
                teamData.getPlayers().remove(uuid);
            }
        }

        if (phase == GamePhase.RUNNING) {
            if (countedAsRunningQuit) {
                runningQuitCount++;
            }
            checkWin();
        }
        if (getParticipantCount() == 0) {
            resetArena();
        }
    }

    public Location getRespawnLocation(Player player) {
        if (player == null) {
            return resolveSpectatorSpawnLocation(null);
        }
        if (phase == GamePhase.WAITING || phase == GamePhase.STARTING) {
            Location lobbySpawn = mapConfig.getLobbySpawn();
            if (lobbySpawn != null) {
                plugin.debug("spawn", "getRespawnLocation -> waiting/start phase, using lobbySpawn=" + plugin.formatLocation(lobbySpawn)
                        + " player=" + player.getName() + " arena=" + arenaId);
                return lobbySpawn;
            }
        }
        TeamData teamData = playerTeams.get(player.getUniqueId());
        if (respawningPlayers.contains(player.getUniqueId())) {
            Location lobbySpawn = mapConfig.getLobbySpawn();
            if (lobbySpawn != null) {
                plugin.debug("spawn", "getRespawnLocation -> respawning lobby spawn=" + plugin.formatLocation(lobbySpawn)
                        + " team=" + (teamData == null ? "null" : teamData.getId()) + " player=" + player.getName());
                return lobbySpawn;
            }
            plugin.debug("spawn", "getRespawnLocation -> respawning spectator fallback for player=" + player.getName());
            return resolveSpectatorSpawnLocation(teamData);
        }
        if (spectators.contains(player.getUniqueId())) {
            plugin.debug("spawn", "getRespawnLocation -> spectator fallback for player=" + player.getName());
            return resolveSpectatorSpawnLocation(teamData);
        }
        if (teamData == null || teamData.getSpawn() == null) {
            plugin.debug("spawn", "getRespawnLocation -> missing team spawn for player=" + player.getName()
                    + " team=" + (teamData == null ? "null" : teamData.getId()));
            return resolveSpectatorSpawnLocation(teamData);
        }
        plugin.debug("spawn", "getRespawnLocation -> team spawn=" + plugin.formatLocation(teamData.getSpawn())
                + " team=" + teamData.getId() + " player=" + player.getName());
        return teamData.getSpawn();
    }

    public void handleDeath(Player player) {
        if (player == null) {
            return;
        }

        clearTransientAbilityState(player);

        UUID uuid = player.getUniqueId();
        if (spectators.contains(uuid)) {
            Bukkit.getScheduler().runTask(plugin, () -> setSpectator(player));
            return;
        }

        TeamData teamData = playerTeams.get(uuid);
        if (teamData == null) {
            Bukkit.getScheduler().runTask(plugin, () -> setSpectator(player));
            return;
        }

        Player killer = resolveRecentKiller(player);

        respawningPlayers.remove(uuid);
        offlineReservations.remove(uuid);

        if (!teamData.isBedAlive() || suddenDeathActive) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                respawningPlayers.remove(uuid);
                playerStates.remove(uuid);
                playerTeams.remove(uuid);
                teamData.getPlayers().remove(uuid);
                setSpectator(player);
                broadcastKillEvent(killer, player, true);
                plugin.getMessageManager().send(player, "game.eliminated", Map.of(
                        "player", player.getName(),
                        "team", teamData.getDisplayName(),
                        "map", arenaId,
                        "arena", arenaId
                ));
                checkWin();
            });
            clearCombatMarker(player.getUniqueId());
            return;
        }

        broadcastKillEvent(killer, player, false);

        int delay = plugin.getConfigManager().getRespawnDelaySeconds();
        respawningPlayers.add(uuid);
        playerStates.put(uuid, PlayerMatchState.RESPAWNING);
        plugin.getMessageManager().send(player, "game.respawn", Map.of(
                "player", player.getName(),
                "value", String.valueOf(delay),
                "map", arenaId,
                "arena", arenaId
        ));

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline() || !playerTeams.containsKey(uuid)) {
                respawningPlayers.remove(uuid);
                return;
            }
            TeamData currentTeam = playerTeams.get(uuid);
            if (currentTeam == null || !currentTeam.isBedAlive()) {
                respawningPlayers.remove(uuid);
                setSpectator(player);
                return;
            }

            respawningPlayers.remove(uuid);
            playerStates.put(uuid, PlayerMatchState.ALIVE);
            Location respawnLocation = resolveRunningSpawnLocation(currentTeam);
            if (respawnLocation != null) {
                player.teleport(respawnLocation);
            }
            resetPerLifeStates(player);
            prepareCombatPlayer(player);
            equipTeamLoadout(player, currentTeam);
        }, delay * 20L);

        clearCombatMarker(player.getUniqueId());
    }

    public void handleRespawn(Player player) {
        if (player == null || !respawningPlayers.contains(player.getUniqueId())) {
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline() && respawningPlayers.contains(player.getUniqueId())) {
                player.setGameMode(GameMode.SPECTATOR);
            }
        });
    }

    public boolean isDamageAllowed(Player player) {
        if (player == null || isSpectator(player) || isRespawning(player)) {
            return false;
        }
        return phase == GamePhase.RUNNING || plugin.getConfigManager().isDamageAllowedInWaiting();
    }

    public boolean canPlayersInteract(Player attacker, Player victim) {
        if (attacker == null || victim == null) {
            return false;
        }
        if (!containsPlayer(attacker.getUniqueId()) || !containsPlayer(victim.getUniqueId())) {
            return false;
        }
        if (isSpectator(attacker) || isRespawning(attacker) || isSpectator(victim) || isRespawning(victim) || !isDamageAllowed(victim)) {
            return false;
        }

        if (!plugin.getConfigManager().isFriendlyFireEnabled()) {
            TeamData attackerTeam = playerTeams.get(attacker.getUniqueId());
            TeamData victimTeam = playerTeams.get(victim.getUniqueId());
            if (attackerTeam != null && victimTeam != null
                    && attackerTeam.getId().equalsIgnoreCase(victimTeam.getId())) {
                return false;
            }
        }
        return true;
    }

    public void registerCombatHit(Player attacker, Player victim) {
        if (attacker == null || victim == null || attacker.getUniqueId().equals(victim.getUniqueId())) {
            return;
        }
        lastDamagerByVictim.put(victim.getUniqueId(), attacker.getUniqueId());
        lastDamageTimestampByVictim.put(victim.getUniqueId(), System.currentTimeMillis());
    }

    public boolean canModifyBlocks(Player player) {
        return player != null
                && containsPlayer(player.getUniqueId())
                && !isSpectator(player)
                && !isRespawning(player)
                && phase == GamePhase.RUNNING;
    }

    public boolean canDropItems(Player player) {
        return player != null
                && containsPlayer(player.getUniqueId())
                && !isSpectator(player)
                && !isRespawning(player)
                && phase == GamePhase.RUNNING;
    }

    public void trackDroppedItem(Item item) {
        if (item == null) {
            return;
        }
        item.getPersistentDataContainer().set(arenaDropKey, PersistentDataType.STRING, arenaId);
        trackedDroppedItems.add(item.getUniqueId());
    }

    public boolean handleBlockPlace(Block block, BlockState replacedState, Player player) {
        if (block == null || !canModifyBlocks(player) || isProtectedLocation(block.getLocation())) {
            return false;
        }
        String blockKey = getBlockKey(block);
        if (replacedState != null) {
            modifiedBlocks.putIfAbsent(blockKey, StoredBlockState.capture(replacedState));
        } else {
            modifiedBlocks.putIfAbsent(blockKey, StoredBlockState.capture(block));
        }
        placedBlockKeys.add(blockKey);
        return true;
    }

    public boolean handleRegularBlockBreak(Block block, Player player) {
        if (block == null || !canModifyBlocks(player) || isProtectedLocation(block.getLocation())) {
            return false;
        }
        String blockKey = getBlockKey(block);
        if (!placedBlockKeys.contains(blockKey)) {
            return false;
        }
        placedBlockKeys.remove(blockKey);
        return true;
    }

    public TeamData getTeamByBedLocation(Location location) {
        if (location == null) {
            return null;
        }
        List<Location> candidateLocations = resolveBedCandidateLocations(location);
        for (TeamData teamData : mapConfig.getTeams()) {
            List<StoredBlockState> bedSnapshots = bedSnapshotsByTeam.getOrDefault(teamData.getId().toLowerCase(), List.of());
            for (StoredBlockState snapshot : bedSnapshots) {
                for (Location candidate : candidateLocations) {
                    if (snapshot.matches(candidate)) {
                        return teamData;
                    }
                }
            }
        }
        return null;
    }

    public boolean isOwnBed(Player player, TeamData bedTeam) {
        if (player == null || bedTeam == null) {
            return false;
        }
        TeamData ownTeam = playerTeams.get(player.getUniqueId());
        if (ownTeam == null) {
            return false;
        }
        return ownTeam.getId().equalsIgnoreCase(bedTeam.getId());
    }

    public void destroyBed(TeamData teamData, Player breaker) {
        destroyBed(teamData, breaker, teamData == null ? null : teamData.getBed());
    }

    public void destroyBed(TeamData teamData, Player breaker, Location brokenBedLocation) {
        if (teamData == null || breaker == null || !teamData.isBedAlive()) {
            return;
        }

        teamData.setBedAlive(false);
        bedBreakSecondByTeam.putIfAbsent(teamData.getId().toLowerCase(), runningSeconds);
        clearBedBlocks(teamData, brokenBedLocation);
        forEachOnlineParticipant(online -> plugin.getMessageManager().send(online, "game.bed-destroyed", Map.of(
                "player", breaker.getName(),
                "team", teamData.getDisplayName(),
                "map", arenaId,
                "arena", arenaId
        )));
        playConfiguredSoundToParticipants("bed-break", Sound.ENTITY_ENDER_DRAGON_GROWL, 0.35F, 1.6F);
        checkWin();
    }

    public void forceStart() {
        if (phase == GamePhase.RUNNING || phase == GamePhase.ENDING || playerTeams.isEmpty()) {
            return;
        }
        countdown = 0;
        phase = GamePhase.STARTING;
        startRunning();
    }

    public boolean endMatchNow() {
        if (phase == GamePhase.ENDING || getParticipantCount() == 0) {
            return false;
        }
        resetArena();
        return true;
    }

    /** Solo-Test-Modus: Min-Spieler-Prüfung wird auf 1 gesetzt. */
    public void enableSoloTestMode() {
        this.soloTestMode = true;
    }

    public boolean isSoloTestMode() {
        return soloTestMode;
    }

    public boolean isAssignedPlayer(UUID uuid) {
        return uuid != null && playerTeams.containsKey(uuid) && !spectators.contains(uuid);
    }

    public boolean ensureAssignedForWaiting(Player player) {
        if (player == null) {
            return false;
        }

        UUID uuid = player.getUniqueId();
        plugin.debug("team", "ensureAssignedForWaiting -> arena=" + arenaId + " player=" + player.getName());
        spectators.remove(uuid);
        respawningPlayers.remove(uuid);
        offlineReservations.remove(uuid);
        clearCombatMarker(uuid);

        TeamData existing = playerTeams.get(uuid);
        if (existing != null) {
            existing.getPlayers().add(uuid);
            playerStates.put(uuid, PlayerMatchState.ALIVE);
            prepareWaitingPlayer(player);
            plugin.debug("team", "ensureAssignedForWaiting -> reused existing team=" + existing.getId());
            return true;
        }

        for (TeamData teamData : mapConfig.getTeams()) {
            teamData.getPlayers().remove(uuid);
        }
        playerTeams.remove(uuid);
        playerStates.remove(uuid);

        prepareWaitingPlayer(player);
        assignTeamIfNeeded(player);
        plugin.debug("team", "ensureAssignedForWaiting -> result assigned=" + isAssignedPlayer(uuid));
        return isAssignedPlayer(uuid);
    }

    public void removePlayerCompletely(Player player) {
        if (player == null) {
            return;
        }

        UUID uuid = player.getUniqueId();
        sidebarStates.remove(uuid);
        playerLoadoutStates.remove(uuid);
        spectators.remove(uuid);
        respawningPlayers.remove(uuid);
        offlineReservations.remove(uuid);
        playerStates.remove(uuid);
        clearCombatMarker(uuid);

        TeamData teamData = playerTeams.remove(uuid);
        if (teamData != null) {
            teamData.getPlayers().remove(uuid);
        }

        if (phase == GamePhase.RUNNING) {
            checkWin();
        }
        if (getParticipantCount() == 0 && phase != GamePhase.ENDING) {
            resetArena();
        }
    }

    /**
     * Zerstört das Bett eines Teams anhand der Team-ID.
     * Gibt true zurück wenn das Bett gefunden und zerstört wurde.
     */
    public boolean destroyBedByTeamId(String teamId, Player breaker) {
        if (teamId == null || breaker == null || phase != GamePhase.RUNNING) {
            return false;
        }
        TeamData teamData = mapConfig.getTeams().stream()
                .filter(t -> t.getId().equalsIgnoreCase(teamId))
                .findFirst()
                .orElse(null);
        if (teamData == null || !teamData.isBedAlive()) {
            return false;
        }
        destroyBed(teamData, breaker);
        return true;
    }

    /** Gibt dem Spieler ein Test-Kit (Ressourcen + Waffen). */
    public void giveTestKit(Player player) {
        if (player == null) {
            return;
        }
        org.bukkit.inventory.Inventory inv = player.getInventory();
        // Ressourcen
        inv.addItem(new ItemStack(Material.IRON_INGOT, 64));
        inv.addItem(new ItemStack(Material.GOLD_INGOT, 32));
        inv.addItem(new ItemStack(Material.DIAMOND, 16));
        inv.addItem(new ItemStack(Material.EMERALD, 8));
        // Waffen & Werkzeuge
        inv.addItem(new ItemStack(Material.DIAMOND_SWORD));
        inv.addItem(new ItemStack(Material.IRON_PICKAXE));
        inv.addItem(new ItemStack(Material.BOW));
        inv.addItem(new ItemStack(Material.ARROW, 32));
        // Blöcke
        inv.addItem(new ItemStack(Material.WHITE_WOOL, 64));
        inv.addItem(new ItemStack(Material.END_STONE, 32));
        inv.addItem(new ItemStack(Material.GOLDEN_APPLE, 8));
    }

    public void shutdown() {
        sidebarStates.clear();
        clearDroppedItemsAtMatchEnd();
        clearAllTeamIslandChests();
        restoreModifiedBlocks();
        restoreBeds();
        despawnShops();
    }

    public boolean handleSpectatorCompass(Player player, Action action, ItemStack itemStack) {
        if (player == null || !isSpectator(player) || action == null) {
            return false;
        }
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return false;
        }
        if (itemStack == null || itemStack.getType() != Material.COMPASS) {
            return false;
        }

        openSpectatorMenu(player);
        return true;
    }

    public boolean handleAbilityInteract(Player player, Action action, ItemStack itemStack) {
        if (player == null || action == null || itemStack == null || isSpectator(player) || isRespawning(player) || !containsPlayer(player.getUniqueId())) {
            return false;
        }
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return false;
        }

        PlayerLoadoutState loadoutState = playerLoadoutStates.get(player.getUniqueId());
        if (loadoutState == null) {
            return false;
        }

        if (matchesAbilityItem(itemStack, Material.FEATHER, "&bDash")) {
            return loadoutState.hasDashAbility && activateAbility(player, loadoutState, "dash", 20_000L, Material.FEATHER, () -> {
                player.setVelocity(player.getLocation().getDirection().normalize().multiply(1.35D).setY(0.35D));
                int dashLevel = Math.max(1, loadoutState.dashLevel);
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, dashLevel >= 2 ? 180 : 120, dashLevel >= 2 ? 2 : 1, true, false, true));
                sendActionbar(player, "&bDash &7bereit eingesetzt");
            }, "&cDash ist noch auf Cooldown.");
        }
        if (matchesAbilityItem(itemStack, Material.BLAZE_POWDER, "&cBerserker")) {
            return loadoutState.hasBerserkerAbility && activateAbility(player, loadoutState, "berserker", 45_000L, Material.BLAZE_POWDER, () -> {
                int berserkerLevel = Math.max(1, loadoutState.berserkerLevel);
                player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, berserkerLevel >= 2 ? 260 : 200, berserkerLevel >= 2 ? 1 : 0, true, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, berserkerLevel >= 2 ? 260 : 200, berserkerLevel >= 2 ? 1 : 0, true, false, true));
                sendActionbar(player, "&cBerserker &7aktiviert");
            }, "&cBerserker ist noch auf Cooldown.");
        }
        if (matchesAbilityItem(itemStack, Material.NETHER_STAR, "&bShield")) {
            return loadoutState.hasShieldAbility && activateAbility(player, loadoutState, "shield", 35_000L, Material.NETHER_STAR, () -> {
                int shieldLevel = Math.max(1, loadoutState.shieldLevel);
                player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, shieldLevel >= 2 ? 260 : 200, shieldLevel >= 2 ? 1 : 0, true, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, shieldLevel >= 2 ? 260 : 200, shieldLevel >= 2 ? 1 : 0, true, false, true));
                sendActionbar(player, "&bShield &7aktiviert");
            }, "&cShield ist noch auf Cooldown.");
        }
        if (matchesAbilityItem(itemStack, Material.BRICK, "&6Builder")) {
            return loadoutState.hasBuilderAbility && activateAbility(player, loadoutState, "builder", 25_000L, Material.BRICK, () -> {
                int builderLevel = Math.max(1, loadoutState.builderLevel);
                player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, builderLevel >= 2 ? 320 : 260, builderLevel >= 2 ? 2 : 1, true, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, builderLevel >= 2 ? 320 : 260, builderLevel >= 2 ? 1 : 0, true, false, true));
                sendActionbar(player, "&6Builder &7aktiviert");
            }, "&cBuilder ist noch auf Cooldown.");
        }
        if (matchesAbilityItem(itemStack, Material.COMPASS, "&aTracker")) {
            return loadoutState.hasTrackerAbility && activateAbility(player, loadoutState, "tracker", 8_000L, Material.COMPASS, () -> {
                Player target = findNearestEnemy(player);
                if (target == null) {
                    player.sendMessage(ColorUtil.colorize("&8[&bBedWars&8] &7Kein Gegner in Reichweite gefunden."));
                    return;
                }
                player.setCompassTarget(target.getLocation());
                if (loadoutState.trackerLevel >= 2 && !target.isSneaking()) {
                    target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 80, 0, true, false, true));
                } else if (loadoutState.trackerLevel >= 2) {
                    player.sendMessage(ColorUtil.colorize("&8[&bBedWars&8] &7" + target.getName() + " &7sneakt gerade &8- &7kein Glowing möglich."));
                }
                player.sendMessage(ColorUtil.colorize("&8[&bBedWars&8] &7Tracker markiert jetzt &a" + target.getName() + "&7."));
                sendActionbar(player, "&aTracker &7→ &f" + target.getName());
            }, "&cTracker kalibriert noch.");
        }
        return false;
    }

    public boolean canThrowSpecialItem(Player player) {
        return player != null
                && containsPlayer(player.getUniqueId())
                && !isSpectator(player)
                && !isRespawning(player)
                && phase == GamePhase.RUNNING;
    }

    public boolean tryUseSpecialItem(Player player, String key, long cooldownMillis, Material material, String cooldownMessage) {
        if (!canThrowSpecialItem(player)) {
            return false;
        }
        PlayerLoadoutState loadoutState = playerLoadoutStates.computeIfAbsent(player.getUniqueId(), ignored -> new PlayerLoadoutState());
        long now = System.currentTimeMillis();
        if (isSilenceTrapBlockedAction(key) && loadoutState.specialItemSilenceUntil > now) {
            long remainingMillis = loadoutState.specialItemSilenceUntil - now;
            long remaining = Math.max(1L, (remainingMillis + 999L) / 1000L);
            player.sendMessage(ColorUtil.colorize("&8[&bBedWars&8] &8Silence Trap &7blockiert Pearl, TNT und Fireball noch &c" + remaining + "s&7."));
            sendActionbar(player, "&8Silence Trap &7→ &c" + remaining + "s blockiert");
            playConfiguredSound(player, "shop-error", Sound.ENTITY_VILLAGER_NO, 0.8F, 1.0F);
            if (material != null) {
                player.setCooldown(material, Math.max(1, (int) Math.min(Integer.MAX_VALUE, remainingMillis / 50L)));
            }
            return false;
        }
        long nextAllowed = loadoutState.specialItemCooldowns.getOrDefault(key, 0L);
        if (nextAllowed > now) {
            long remaining = Math.max(1L, (nextAllowed - now + 999L) / 1000L);
            player.sendMessage(ColorUtil.colorize("&8[&bBedWars&8] " + cooldownMessage + " &7(" + remaining + "s)"));
            playConfiguredSound(player, "shop-error", Sound.ENTITY_VILLAGER_NO, 0.8F, 1.0F);
            return false;
        }
        loadoutState.specialItemCooldowns.put(key, now + cooldownMillis);
        if (material != null && cooldownMillis > 0L) {
            player.setCooldown(material, Math.max(1, (int) (cooldownMillis / 50L)));
        }
        if ("ender-pearl".equalsIgnoreCase(key)) {
            loadoutState.pearlProtectionUntil = now + 6_000L;
        }
        return true;
    }

    private boolean isSilenceTrapBlockedAction(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        return switch (key.toLowerCase(Locale.ROOT)) {
            case "ender-pearl", "fireball" -> true;
            default -> false;
        } || key.toLowerCase(Locale.ROOT).endsWith("tnt");
    }

    public String resolveTntAction(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() != Material.TNT) {
            return "tnt";
        }
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) {
            return "tnt";
        }
        String stored = itemMeta.getPersistentDataContainer().get(tntVariantKey, PersistentDataType.STRING);
        if (stored == null || stored.isBlank()) {
            return "tnt";
        }
        return stored.toLowerCase(Locale.ROOT);
    }

    public String getTntCooldownMessage(String action) {
        return switch ((action == null ? "tnt" : action.toLowerCase(Locale.ROOT))) {
            case "breach-tnt" -> "&6Breach TNT &7ist noch nicht wieder bereit.";
            case "jump-tnt" -> "&bJump TNT &7lädt noch nach.";
            case "cluster-tnt" -> "&dCluster TNT &7lädt noch nach.";
            case "sticky-tnt" -> "&eSticky TNT &7ist noch im Cooldown.";
            default -> "&cTNT ist noch nicht wieder bereit.";
        };
    }

    public Location resolveSpecialTntSpawnLocation(Player player, String action, Block clickedBlock, BlockFace blockFace) {
        String normalizedAction = action == null || action.isBlank() ? "tnt" : action.toLowerCase(Locale.ROOT);
        Location spawnLocation;
        if ("sticky-tnt".equals(normalizedAction)) {
            if (clickedBlock == null || blockFace == null) {
                player.sendMessage(ColorUtil.colorize("&8[&bBedWars&8] &eSticky TNT &7muss an einen Block oder eine Wand platziert werden."));
                playConfiguredSound(player, "shop-error", Sound.ENTITY_VILLAGER_NO, 0.8F, 1.0F);
                return null;
            }
            spawnLocation = clickedBlock.getLocation().add(0.5D, 0.5D, 0.5D)
                    .add(blockFace.getModX() * 0.64D, blockFace.getModY() * 0.64D, blockFace.getModZ() * 0.64D);
        } else if (clickedBlock != null && blockFace != null) {
            spawnLocation = clickedBlock.getRelative(blockFace).getLocation().add(0.5D, 0.0D, 0.5D);
        } else {
            spawnLocation = player.getLocation().clone().add(0.0D, 0.2D, 0.0D);
        }

        if (!canSpawnSpecialTntAt(player, spawnLocation)) {
            return null;
        }
        return spawnLocation;
    }

    public void configurePrimedTnt(TNTPrimed primed, Player owner, String action, int clusterGeneration) {
        String normalizedAction = action == null || action.isBlank() ? "tnt" : action.toLowerCase(Locale.ROOT);
        primed.setSource(owner);
        primed.setFuseTicks(plugin.getConfigManager().getTntFuseTicks(normalizedAction));
        primed.setYield(plugin.getConfigManager().getTntYield(normalizedAction));
        primed.setIsIncendiary(false);
        primed.getPersistentDataContainer().set(tntVariantKey, PersistentDataType.STRING, normalizedAction);
        if (owner != null) {
            primed.getPersistentDataContainer().set(tntOwnerKey, PersistentDataType.STRING, owner.getUniqueId().toString());
        }
        primed.getPersistentDataContainer().set(tntClusterGenerationKey, PersistentDataType.INTEGER, clusterGeneration);
        if (plugin.getConfigManager().shouldStickyTntIgnoreGravity(normalizedAction)) {
            primed.setGravity(false);
            primed.setVelocity(new Vector(0.0D, 0.0D, 0.0D));
        }
    }

    public boolean shouldIgnoreEnderPearlFallDamage(Player player) {
        if (player == null) {
            return false;
        }
        PlayerLoadoutState loadoutState = playerLoadoutStates.get(player.getUniqueId());
        if (loadoutState == null || plugin.getConfigManager().shouldAllowPearlDamage()) {
            return false;
        }
        if (loadoutState.pearlProtectionUntil <= System.currentTimeMillis()) {
            return false;
        }
        loadoutState.pearlProtectionUntil = 0L;
        return true;
    }

    public boolean tryTriggerVoidRescue(Player player) {
        if (player == null || phase != GamePhase.RUNNING || isSpectator(player) || isRespawning(player) || !containsPlayer(player.getUniqueId())) {
            return false;
        }

        PlayerLoadoutState loadoutState = playerLoadoutStates.computeIfAbsent(player.getUniqueId(), ignored -> new PlayerLoadoutState());
        if (!loadoutState.hasVoidRescueUpgrade || loadoutState.voidRescueUsedThisLife) {
            return false;
        }

        Location current = player.getLocation();
        World world = current.getWorld();
        if (world == null) {
            return false;
        }

        loadoutState.voidRescueUsedThisLife = true;
        TeamData teamData = playerTeams.get(player.getUniqueId());
        double targetY = Math.max(current.getY() + plugin.getConfigManager().getVoidRescueTeleportOffset(),
                resolveVoidRescueSafeY(world, teamData));
        Location rescueLocation = current.clone();
        rescueLocation.setY(targetY);
        player.teleport(rescueLocation);
        player.setFallDistance(0.0F);
        player.setVelocity(new org.bukkit.util.Vector(0.0D, plugin.getConfigManager().getVoidRescueUpwardVelocity(), 0.0D));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING,
                plugin.getConfigManager().getVoidRescueSlowFallingTicks(), 0, true, false, true));

        if (plugin.getConfigManager().shouldCreateVoidRescuePlatform()) {
            createVoidRescuePlatform(player, rescueLocation, teamData);
        }

        playConfiguredSound(player, "void-rescue", Sound.ITEM_TOTEM_USE, 0.8F, 1.25F);
        sendActionbar(player, "&bRettungsfeder &7hat dich vor dem Void gerettet");
        plugin.getMessageManager().send(player, "game.void-rescue-activated", Map.of(
                "map", arenaId,
                "arena", arenaId
        ));
        return true;
    }

    private void createVoidRescuePlatform(Player player, Location rescueLocation, TeamData teamData) {
        if (rescueLocation == null || rescueLocation.getWorld() == null) {
            return;
        }
        Material wool = teamData == null ? Material.WHITE_WOOL : resolveTeamWool(teamData.getId());
        long lifetimeTicks = plugin.getConfigManager().getVoidRescuePlatformLifetimeTicks();
        Block center = rescueLocation.clone().subtract(0.0D, 1.0D, 0.0D).getBlock();
        placeTemporaryTrackedBlock(center, wool, lifetimeTicks);

        org.bukkit.util.Vector facing = player.getLocation().getDirection().clone().setY(0.0D);
        if (facing.lengthSquared() < 0.0001D) {
            facing = new org.bukkit.util.Vector(1.0D, 0.0D, 0.0D);
        } else {
            facing.normalize();
        }
        boolean extendOnX = Math.abs(facing.getZ()) >= Math.abs(facing.getX());
        placeTemporaryTrackedBlock(center.getRelative(extendOnX ? BlockFace.EAST : BlockFace.NORTH), wool, lifetimeTicks);
        placeTemporaryTrackedBlock(center.getRelative(extendOnX ? BlockFace.WEST : BlockFace.SOUTH), wool, lifetimeTicks);
    }

    private void placeTemporaryTrackedBlock(Block block, Material material, long lifetimeTicks) {
        if (block == null || material == null || !isReplaceableForBridge(block)) {
            return;
        }
        placeTrackedBlock(block, material);
        String key = getBlockKey(block);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            StoredBlockState snapshot = modifiedBlocks.get(key);
            if (snapshot == null) {
                return;
            }
            World world = Bukkit.getWorld(snapshot.worldName);
            if (world == null) {
                return;
            }
            Block current = world.getBlockAt(snapshot.x, snapshot.y, snapshot.z);
            if (current.getType() != material) {
                return;
            }
            snapshot.restore();
            placedBlockKeys.remove(key);
            modifiedBlocks.remove(key);
        }, Math.max(1L, lifetimeTicks));
    }

    private void resetPerLifeStates(Player player) {
        if (player == null) {
            return;
        }
        PlayerLoadoutState loadoutState = playerLoadoutStates.computeIfAbsent(player.getUniqueId(), ignored -> new PlayerLoadoutState());
        loadoutState.voidRescueUsedThisLife = false;
    }

    private void clearTransientAbilityState(Player player) {
        if (player == null) {
            return;
        }
        PlayerLoadoutState loadoutState = playerLoadoutStates.computeIfAbsent(player.getUniqueId(), ignored -> new PlayerLoadoutState());
        loadoutState.hasDashAbility = false;
        loadoutState.hasBerserkerAbility = false;
        loadoutState.hasShieldAbility = false;
        loadoutState.hasBuilderAbility = false;
        loadoutState.hasTrackerAbility = false;
        loadoutState.dashLevel = 0;
        loadoutState.berserkerLevel = 0;
        loadoutState.shieldLevel = 0;
        loadoutState.builderLevel = 0;
        loadoutState.trackerLevel = 0;
        loadoutState.abilityCooldowns.clear();
    }

    public boolean canSpawnSpecialTntAt(Player player, Location location) {
        if (!canThrowSpecialItem(player) || location == null || location.getWorld() == null) {
            return false;
        }
        if (isProtectedLocation(location)) {
            player.sendMessage(ColorUtil.colorize("&8[&bBedWars&8] &cHier kannst du kein TNT einsetzen."));
            playConfiguredSound(player, "shop-error", Sound.ENTITY_VILLAGER_NO, 0.8F, 1.0F);
            return false;
        }
        return true;
    }

    /** TNT/Fireball-Explosion: nur gesetzte Blöcke dürfen zerstört werden. */
    public void handleExplosion(EntityExplodeEvent event) {
        List<Block> blocks = event.blockList();
        if (phase != GamePhase.RUNNING) {
            blocks.clear();
            event.setYield(0.0F);
            return;
        }
        if (blocks.isEmpty() && !(event.getEntity() instanceof TNTPrimed)) {
            return;
        }

        if (event.getEntity() instanceof TNTPrimed primedTnt) {
            String action = getPrimedTntAction(primedTnt);
            if ("cluster-tnt".equals(action) && getPrimedTntClusterGeneration(primedTnt) <= 0) {
                event.setCancelled(true);
                primedTnt.remove();
                spawnClusterChildren(event.getLocation(), resolvePrimedTntOwner(primedTnt), action);
                return;
            }
            if ("jump-tnt".equals(action)) {
                applyJumpTntBoost(primedTnt, event.getLocation());
            }
            if (action != null && !action.isBlank()) {
                addNearbyPlacedBlocks(event.getLocation(), blocks, plugin.getConfigManager().getTntBlockRadius(action));
                event.setYield(plugin.getConfigManager().getTntYield(action));
            }
        }

        filterExplosionBlocks(blocks);
    }

    public boolean handleSpecialTntDamage(EntityDamageByEntityEvent event, Player victim, TNTPrimed primedTnt) {
        String action = getPrimedTntAction(primedTnt);
        if (action == null || action.isBlank()) {
            return false;
        }

        Player owner = resolvePrimedTntOwner(primedTnt);
        if (owner != null && owner.isOnline() && containsPlayer(owner.getUniqueId()) && !canPlayersInteract(owner, victim)) {
            event.setCancelled(true);
            return true;
        }

        double damageMultiplier = owner != null && owner.getUniqueId().equals(victim.getUniqueId())
                ? plugin.getConfigManager().getTntSelfDamageMultiplier(action)
                : plugin.getConfigManager().getTntDamageMultiplier(action);
        if (damageMultiplier <= 0.0D) {
            event.setCancelled(true);
            return true;
        }

        event.setDamage(event.getDamage() * damageMultiplier);
        if (owner != null && owner.isOnline() && containsPlayer(owner.getUniqueId())) {
            registerCombatHit(owner, victim);
        }
        return true;
    }

    /** SmallFireball landet: Knockback + Schaden an umliegende Spieler. */
    public boolean handleFireballHit(SmallFireball fireball, Location hitLocation) {
        if (phase != GamePhase.RUNNING || hitLocation == null) {
            return false;
        }
        Player shooter = fireball.getShooter() instanceof Player p ? p : null;
        if (shooter != null && !containsPlayer(shooter.getUniqueId())) {
            return false;
        }

        World world = hitLocation.getWorld();
        if (world == null) {
            return false;
        }

        destroyBlocks(collectBreakableExplosionBlocks(hitLocation, plugin.getConfigManager().getFireballBlockRadius()));
        world.createExplosion(hitLocation, 0.0f, false, false);
        world.playEffect(hitLocation, org.bukkit.Effect.SMOKE, 6);

        double damageRadius = plugin.getConfigManager().getFireballDamageRadius();
        double maxDamage = plugin.getConfigManager().getFireballDamage();
        double knockbackHorizontal = plugin.getConfigManager().getFireballKnockbackHorizontal();
        double knockbackVertical = plugin.getConfigManager().getFireballKnockbackVertical();
        double selfDamageMultiplier = plugin.getConfigManager().getFireballSelfDamageMultiplier();
        int fireTicks = plugin.getConfigManager().getFireballFireTicks();
        double radiusSquared = damageRadius * damageRadius;

        for (Entity nearby : world.getNearbyEntities(hitLocation, damageRadius, damageRadius, damageRadius)) {
            if (!(nearby instanceof Player target)) {
                continue;
            }
            if (isSpectator(target) || isRespawning(target) || !containsPlayer(target.getUniqueId())) {
                continue;
            }
            if (shooter != null && !shooter.getUniqueId().equals(target.getUniqueId()) && !canPlayersInteract(shooter, target)) {
                continue;
            }

            double distanceSquared = target.getLocation().distanceSquared(hitLocation);
            if (distanceSquared > radiusSquared) {
                continue;
            }

            double distance = Math.sqrt(distanceSquared);
            double strength = Math.max(0.15D, 1.0D - (distance / Math.max(0.1D, damageRadius)));
            Vector direction = target.getLocation().toVector().subtract(hitLocation.toVector());
            if (direction.lengthSquared() < 0.0001D) {
                direction = target.getLocation().getDirection();
            }
            if (direction.lengthSquared() > 0.0001D) {
                direction.normalize();
            }
            target.setVelocity(direction.multiply(knockbackHorizontal * (0.45D + (strength * 0.9D)))
                    .setY(knockbackVertical * (0.55D + (strength * 0.7D))));

            double damage = maxDamage * (0.4D + (strength * 0.6D));
            if (shooter != null && shooter.getUniqueId().equals(target.getUniqueId())) {
                damage *= selfDamageMultiplier;
            }
            if (damage > 0.05D) {
                if (shooter != null) {
                    target.damage(damage, shooter);
                } else {
                    target.damage(damage);
                }
            }
            if (fireTicks > 0) {
                target.setFireTicks(Math.max(target.getFireTicks(), fireTicks));
            }
        }

        fireball.remove();
        return true;
    }

    public void noteIntruderBaseAction(Player player) {
        if (player == null || phase != GamePhase.RUNNING || isSpectator(player) || isRespawning(player) || !containsPlayer(player.getUniqueId())) {
            return;
        }

        TeamData defendingTeam = resolveDefendingTeamForIntruder(player);
        if (defendingTeam == null) {
            return;
        }

        TeamUpgradeState state = teamUpgradeStates.computeIfAbsent(defendingTeam.getId().toLowerCase(), ignored -> new TeamUpgradeState());
        long now = System.currentTimeMillis();
        if (state.trapQueue.isEmpty() || state.trapRearmUntil > now || state.trapQueue.getFirst() != TrapType.SILENCE) {
            return;
        }

        state.trapQueue.removeFirst();
        state.trapRearmUntil = now + plugin.getConfigManager().getTrapRearmMillis();
        triggerTrap(defendingTeam, player, TrapType.SILENCE);
    }

    public boolean isSpawnedShopEntity(Entity entity) {
        return entity != null && spawnedShopEntities.containsKey(entity.getUniqueId());
    }

    /** Bridge-Egg: Wollbrücke ab Auftreffpunkt in Wurfrichtung platzieren. */
    public void handleBridgeEggHit(Egg egg, Block hitBlock, org.bukkit.block.BlockFace hitFace, Player shooter) {
        if (phase != GamePhase.RUNNING) return;
        if (!containsPlayer(shooter.getUniqueId()) || isSpectator(shooter) || isRespawning(shooter)) return;

        TeamData teamData = playerTeams.get(shooter.getUniqueId());
        Material wool = teamData == null ? Material.WHITE_WOOL : resolveTeamWool(teamData.getId());

        org.bukkit.util.Vector dir = egg.getVelocity().clone().setY(0);
        if (dir.lengthSquared() < 0.001) {
            dir = shooter.getLocation().getDirection().clone().setY(0);
        }
        if (dir.lengthSquared() > 0.001) dir.normalize();

        Location start = hitBlock.getLocation().clone()
                .add(hitFace.getModX(), hitFace.getModY(), hitFace.getModZ());

        int maxPlacedBlocks = plugin.getConfigManager().getBridgeEggHitPlacedBlocks();
        int placed = 0;
        for (int i = 0; i < maxPlacedBlocks && placed < maxPlacedBlocks; i++) {
            Block block = start.clone().add(dir.clone().multiply(i)).getBlock();
            if (isReplaceableForBridge(block)) {
                placeTrackedBlock(block, wool);
                placed++;
            }
            Block below = block.getRelative(BlockFace.DOWN);
            if (placed < maxPlacedBlocks && isReplaceableForBridge(below)) {
                placeTrackedBlock(below, wool);
                placed++;
            }
        }
    }


    /**
     * Während das Ei fliegt, zieht es eine Brücke hinter sich her.
     */
    public void handleBridgeEggTrail(Egg egg, Player shooter) {
        if (egg == null || shooter == null || !egg.isValid() || egg.isDead()) {
            return;
        }
        if (phase != GamePhase.RUNNING || !containsPlayer(shooter.getUniqueId()) || isSpectator(shooter) || isRespawning(shooter)) {
            return;
        }

        TeamData teamData = playerTeams.get(shooter.getUniqueId());
        Material wool = teamData == null ? Material.WHITE_WOOL : resolveTeamWool(teamData.getId());

        Location eggLocation = egg.getLocation().clone();
        Block center = eggLocation.clone().subtract(0.0D, 1.0D, 0.0D).getBlock();
        Block feet = eggLocation.getBlock();

        if (isReplaceableForBridge(center)) {
            placeTrackedBlock(center, wool);
        }
        if (isReplaceableForBridge(feet) && feet.getY() <= center.getY()) {
            placeTrackedBlock(feet, wool);
        }
    }

    public boolean handleShopInteract(Player player, Entity entity) {
        if (!(entity instanceof Villager villager) || player == null || isSpectator(player) || isRespawning(player) || !containsPlayer(player.getUniqueId())) {
            return false;
        }

        if (isOnCooldown(shopInteractCooldowns, player.getUniqueId(), plugin.getConfigManager().getShopInteractCooldownMillis())) {
            return true;
        }

        ShopConfig shopConfig = spawnedShopEntities.get(villager.getUniqueId());
        if (shopConfig == null) {
            return false;
        }

        TeamData teamData = playerTeams.get(player.getUniqueId());
        if (teamData == null) {
            return false;
        }

        openShop(player, shopConfig);
        return true;
    }

    public void handleInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (event.getView().getTopInventory().getHolder() instanceof SpectatorInventoryHolder spectatorHolder) {
            if (spectatorHolder.getArenaId().equalsIgnoreCase(arenaId)) {
                handleSpectatorMenuClick(event, player, spectatorHolder);
            }
            return;
        }

        if (!(event.getView().getTopInventory().getHolder() instanceof ShopInventoryHolder holder)) {
            return;
        }
        if (!holder.getArenaId().equalsIgnoreCase(arenaId)) {
            return;
        }

        if (isOnCooldown(shopClickCooldowns, player.getUniqueId(), plugin.getConfigManager().getShopClickCooldownMillis())) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
            return;
        }

        TeamData teamData = playerTeams.get(player.getUniqueId());
        if (teamData == null || !teamData.getId().equalsIgnoreCase(holder.getTeamId())) {
            return;
        }

        if (holder.getInventory().getSize() == ITEM_SHOP_SIZE) {
            if (event.getSlot() == ITEM_SHOP_CLOSE_SLOT) {
                player.closeInventory();
                return;
            }
            if (event.getSlot() == ITEM_SHOP_INFO_SLOT || event.getSlot() == ITEM_SHOP_STATUS_SLOT) {
                return;
            }
        }

        String selectedCategory = holder.getCategorySlots().get(event.getSlot());
        if (selectedCategory != null) {
            holder.setSelectedCategoryId(selectedCategory);
            playConfiguredSound(player, "shop-category-switch", Sound.UI_BUTTON_CLICK, 0.7F, 1.2F);
            renderShopInventory(holder, player, teamData);
            return;
        }

        ShopOfferConfig offer = holder.getOfferSlots().get(event.getSlot());
        if (offer == null) {
            return;
        }

        if (holder.getShopType().equalsIgnoreCase("upgrade")) {
            UpgradeViewState state = resolveUpgradeViewState(player, teamData, offer);
            if (state.status == UpgradeViewStatus.LOCKED) {
                playConfiguredSound(player, "shop-error", Sound.ENTITY_VILLAGER_NO, 0.75F, 1.0F);
                if (isAbilityUpgradeOffer(offer)) {
                    sendActionbar(player, "&cKaufe die Fähigkeit zuerst im Item-Shop");
                    player.sendMessage(ColorUtil.colorize("&8[&bBedWars&8] &7Kaufe zuerst &f"
                            + ChatColor.stripColor(ColorUtil.colorize(offer.getDisplayName()))
                            + " &7im Item-Shop, bevor du sie hier upgradest."));
                } else {
                    sendActionbar(player, "&cTrap-Slots sind aktuell voll");
                    plugin.getMessageManager().send(player, "game.trap-queue-full", Map.of(
                            "team", teamData.getDisplayName(),
                            "map", arenaId,
                            "arena", arenaId
                    ));
                }
                renderShopInventory(holder, player, teamData);
                return;
            }
        }

        if (isTeamUpgradeOffer(offer) && isTeamUpgradeOwned(teamData, offer)) {
            playConfiguredSound(player, "shop-owned", Sound.ENTITY_VILLAGER_NO, 0.75F, 1.0F);
            sendActionbar(player, "&aBereits aktiv: &f" + ChatColor.stripColor(ColorUtil.colorize(offer.getDisplayName())));
            plugin.getMessageManager().send(player, "game.shop-upgrade-owned", Map.of(
                    "value", offer.getDisplayName(),
                    "team", teamData.getDisplayName(),
                    "map", arenaId,
                    "arena", arenaId
            ));
            return;
        }

        if (isPersonalPermanentOffer(offer) && isPersonalPermanentOwned(player, offer)) {
            playConfiguredSound(player, "shop-owned", Sound.ENTITY_VILLAGER_NO, 0.75F, 1.0F);
            sendActionbar(player, "&aBereits gekauft: &f" + ChatColor.stripColor(ColorUtil.colorize(offer.getDisplayName())));
            plugin.getMessageManager().send(player, "game.shop-owned", Map.of(
                    "value", offer.getDisplayName(),
                    "map", arenaId,
                    "arena", arenaId
            ));
            return;
        }

        int resolvedCost = resolveOfferCost(player, teamData, offer);

        if (!hasCurrency(player, offer.getCurrency(), resolvedCost)) {
            playConfiguredSound(player, "shop-error", Sound.ENTITY_VILLAGER_NO, 0.75F, 1.0F);
            sendActionbar(player, "&cZu wenig Ressourcen für &f" + ChatColor.stripColor(ColorUtil.colorize(offer.getDisplayName())));
            plugin.getMessageManager().send(player, "game.shop-no-resources", Map.of(
                    "value", String.valueOf(resolvedCost),
                    "item", prettifyMaterial(offer.getCurrency()),
                    "map", arenaId,
                    "arena", arenaId
            ));
            renderShopInventory(holder, player, teamData);
            return;
        }

        int purchaseCount = 1;
        if (event.isShiftClick() && !isTeamUpgradeOffer(offer) && !isPersonalPermanentOffer(offer)) {
            int affordable = Math.max(1, getCurrencyAmount(player, offer.getCurrency()) / resolvedCost);
            int fitByInventory = getMaxPurchasesByInventorySpace(player, offer);
            if (fitByInventory <= 0) {
                plugin.getMessageManager().send(player, "game.shop-no-space", Map.of(
                        "map", arenaId,
                        "arena", arenaId
                ));
                return;
            }
            purchaseCount = Math.min(Math.min(affordable, fitByInventory), 16);
        }

        consumeCurrency(player, offer.getCurrency(), resolvedCost * purchaseCount);
        if (isTeamUpgradeOffer(offer)) {
            unlockTeamUpgrade(teamData, offer);
            for (UUID memberId : new ArrayList<>(teamData.getPlayers())) {
                Player member = Bukkit.getPlayer(memberId);
                if (member != null && member.isOnline()) {
                    applyTeamUpgrades(member, teamData);
                }
            }
            for (UUID memberId : new ArrayList<>(teamData.getPlayers())) {
                Player member = Bukkit.getPlayer(memberId);
                if (member != null && member.isOnline()) {
                    playConfiguredSound(member, "shop-purchase", Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8F, 1.25F);
                    sendActionbar(member, "&bTeam-Upgrade aktiv: &f" + ChatColor.stripColor(ColorUtil.colorize(offer.getDisplayName())));
                    plugin.getMessageManager().send(member, "game.shop-upgrade-team", Map.of(
                            "value", offer.getDisplayName(),
                            "team", teamData.getDisplayName(),
                            "map", arenaId,
                            "arena", arenaId
                    ));
                }
            }
            renderShopInventory(holder, player, teamData);
            return;
        }

        for (int index = 0; index < purchaseCount; index++) {
            applyOffer(player, offer);
        }
        plugin.getMessageManager().send(player, "game.shop-purchased", Map.of(
                "value", offer.getDisplayName() + (purchaseCount > 1 ? " x" + purchaseCount : ""),
                "map", arenaId,
                "arena", arenaId
        ));
        playConfiguredSound(player, "shop-purchase", Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8F, 1.25F);
        sendActionbar(player, "&aGekauft: &f" + ChatColor.stripColor(ColorUtil.colorize(offer.getDisplayName())));
        renderShopInventory(holder, player, teamData);
    }

    private void loadBlueprint() {
        despawnShops();
        this.mapConfig = plugin.getConfigManager().loadMapConfig(arenaId);
        plugin.debug("arena-load", "loadBlueprint -> arena=" + arenaId
                + " lobbySpawn=" + plugin.formatLocation(mapConfig.getLobbySpawn())
                + " minPlayers=" + mapConfig.getMinPlayers() + " maxPlayers=" + mapConfig.getMaxPlayers()
                + " teamCount=" + mapConfig.getTeams().size());
        this.phase = GamePhase.WAITING;
        this.countdown = plugin.getConfigManager().getGameStartCountdown();
        this.resetCountdown = 0;
        this.runningSeconds = 0;
        this.endgameBedsDestroyed = false;
        this.suddenDeathActive = false;
        this.cleanupTickerSeconds = 0;
        this.diagnosticsTickerSeconds = 0;
        this.sidebarTickerSeconds = 0;
        this.runningParticipantsAtStart = 0;
        this.runningQuitCount = 0;
        this.totalKillCount = 0;
        this.totalFinalKillCount = 0;
        this.matchMetricsLogged = false;
        this.soloTestMode = false;
        this.modifiedBlocks.clear();
        this.placedBlockKeys.clear();
        this.respawningPlayers.clear();
        this.playerStates.clear();
        this.offlineReservations.clear();
        this.lastDamagerByVictim.clear();
        this.lastDamageTimestampByVictim.clear();
        this.killsByPlayer.clear();
        this.finalKillsByPlayer.clear();
        this.shopInteractCooldowns.clear();
        this.shopClickCooldowns.clear();
        this.bedBreakSecondByTeam.clear();
        this.warningLastLogAt.clear();
        this.announcedBedWarningSeconds.clear();
        this.playerLoadoutStates.clear();
        this.sidebarStates.clear();
        this.cachedShopCategoriesByType = new LinkedHashMap<>();
        this.cachedShopOffersByType = new LinkedHashMap<>();
        this.trackedDroppedItems.clear();
        this.spectatorTargetIndices.clear();
        this.teamUpgradeStates.clear();
        for (TeamData teamData : mapConfig.getTeams()) {
            this.teamUpgradeStates.put(teamData.getId().toLowerCase(), new TeamUpgradeState());
        }
        captureBedSnapshots();
        cacheShopData();
        initializeGenerators();
        clearAllTeamIslandChests();
        spawnShops();
    }

    private void captureBedSnapshots() {
        bedSnapshotsByTeam.clear();
        for (TeamData teamData : mapConfig.getTeams()) {
            List<StoredBlockState> snapshots = new ArrayList<>();
            Location bedLocation = teamData.getBed();
            if (bedLocation != null) {
                for (Block bedBlock : resolveBedPair(bedLocation)) {
                    snapshots.add(StoredBlockState.capture(bedBlock));
                }
                ensureCompleteBedSnapshots(snapshots);
            }
            bedSnapshotsByTeam.put(teamData.getId().toLowerCase(), snapshots);
        }
    }

    private void cleanupOfflinePlayers() {
        List<UUID> offlineSpectators = new ArrayList<>();
        for (UUID uuid : spectators) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                offlineSpectators.add(uuid);
            }
        }
        for (UUID uuid : offlineSpectators) {
            spectators.remove(uuid);
            playerStates.remove(uuid);
        }

        List<UUID> removePlayers = new ArrayList<>();
        for (Map.Entry<UUID, TeamData> entry : playerTeams.entrySet()) {
            UUID uuid = entry.getKey();
            TeamData teamData = entry.getValue();
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                if (isOfflineReservationActive(uuid)) {
                    continue;
                }

                if (phase == GamePhase.RUNNING && teamData != null && teamData.isBedAlive()) {
                    markOfflineReservation(uuid);
                    continue;
                }

                removePlayers.add(uuid);
                if (teamData != null) {
                    teamData.getPlayers().remove(uuid);
                }
            }
        }
        for (UUID uuid : removePlayers) {
            playerTeams.remove(uuid);
            respawningPlayers.remove(uuid);
            offlineReservations.remove(uuid);
            playerStates.remove(uuid);
        }

        boolean expiredReservationsRemoved = removeExpiredReservations();
        if (phase == GamePhase.RUNNING && (!removePlayers.isEmpty() || expiredReservationsRemoved)) {
            checkWin();
        }
    }

    private void prepareWaitingPlayer(Player player) {
        player.setGameMode(GameMode.ADVENTURE);
        player.setFoodLevel(20);
        player.setSaturation(20.0F);
        player.setFireTicks(0);
        player.setFallDistance(0);
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        AttributeInstance maxHealthAttribute = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        if (maxHealthAttribute != null) {
            player.setHealth(maxHealthAttribute.getValue());
        }
        for (PotionEffect potionEffect : player.getActivePotionEffects()) {
            player.removePotionEffect(potionEffect.getType());
        }
        Location lobbySpawn = mapConfig.getLobbySpawn();
        if (lobbySpawn != null) {
            player.teleport(lobbySpawn);
        }
        plugin.debug("spawn", "prepareWaitingPlayer -> arena=" + arenaId + " player=" + player.getName()
                + " teleported=" + plugin.formatLocation(lobbySpawn)
                + " currentLocation=" + plugin.formatLocation(player.getLocation()));
    }

    private void prepareCombatPlayer(Player player) {
        player.setGameMode(GameMode.SURVIVAL);
        player.setFoodLevel(20);
        player.setSaturation(20.0F);
        player.setFireTicks(0);
        player.setFallDistance(0);
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        AttributeInstance maxHealthAttribute = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        if (maxHealthAttribute != null) {
            player.setHealth(maxHealthAttribute.getValue());
        }
        for (org.bukkit.potion.PotionEffect potionEffect : player.getActivePotionEffects()) {
            player.removePotionEffect(potionEffect.getType());
        }
    }

    private void equipTeamLoadout(Player player, TeamData teamData) {
        if (teamData == null) {
            return;
        }
        Color teamColor = resolveTeamColor(teamData.getId());
        player.getInventory().setHelmet(coloredArmor(Material.LEATHER_HELMET, teamColor));
        player.getInventory().setChestplate(coloredArmor(Material.LEATHER_CHESTPLATE, teamColor));
        player.getInventory().setLeggings(coloredArmor(Material.LEATHER_LEGGINGS, teamColor));
        player.getInventory().setBoots(coloredArmor(Material.LEATHER_BOOTS, teamColor));
        giveItem(player, new ItemStack(resolveTeamWool(teamData.getId()), 16));
        giveItem(player, new ItemStack(Material.WOODEN_SWORD));
        applyPersonalLoadout(player, teamData);
        applyTeamUpgrades(player, teamData);
    }

    private void assignTeamIfNeeded(Player player) {
        if (playerTeams.containsKey(player.getUniqueId())) {
            plugin.debug("team", "assignTeamIfNeeded -> already assigned player=" + player.getName());
            return;
        }

        plugin.debug("team", "assignTeamIfNeeded -> arena=" + arenaId + " player=" + player.getName()
                + " teamStates=" + mapConfig.getTeams().stream()
                .map(team -> team.getId() + "(" + team.getPlayers().size() + "/" + team.getMaxPlayers() + ")")
                .toList());
        List<TeamData> availableTeams = mapConfig.getTeams().stream()
                .filter(TeamData::hasSpace)
                .toList();
        if (availableTeams.isEmpty()) {
            plugin.debug("team", "assignTeamIfNeeded -> no free team, switching to spectator player=" + player.getName());
            setSpectator(player);
            return;
        }

        int lowestPlayerCount = availableTeams.stream()
                .mapToInt(team -> team.getPlayers().size())
                .min()
                .orElse(Integer.MAX_VALUE);
        List<TeamData> candidateTeams = availableTeams.stream()
                .filter(team -> team.getPlayers().size() == lowestPlayerCount)
                .toList();
        TeamData teamData = candidateTeams.get(ThreadLocalRandom.current().nextInt(candidateTeams.size()));
        teamData.getPlayers().add(player.getUniqueId());
        playerTeams.put(player.getUniqueId(), teamData);
        playerStates.put(player.getUniqueId(), PlayerMatchState.ALIVE);
        plugin.debug("team", "assignTeamIfNeeded -> assigned player=" + player.getName() + " team=" + teamData.getId()
                + " teamSpawn=" + plugin.formatLocation(teamData.getSpawn()));
        plugin.getMessageManager().send(player, "game.team-assigned", Map.of(
                "player", player.getName(),
                "team", teamData.getDisplayName(),
                "map", arenaId,
                "arena", arenaId
        ));
    }

    private void setSpectator(Player player) {
        UUID uuid = player.getUniqueId();
        TeamData previousTeam = playerTeams.get(uuid);
        Location spectatorSpawn = resolveSpectatorSpawnLocation(previousTeam);
        plugin.debug("spectator", "setSpectator -> arena=" + arenaId + " player=" + player.getName()
                + " previousTeam=" + (previousTeam == null ? "null" : previousTeam.getId())
                + " spawn=" + plugin.formatLocation(spectatorSpawn));
        offlineReservations.remove(uuid);
        respawningPlayers.remove(uuid);
        TeamData teamData = playerTeams.remove(uuid);
        if (teamData != null) {
            teamData.getPlayers().remove(uuid);
        }
        boolean wasNew = spectators.add(uuid);
        playerStates.put(uuid, PlayerMatchState.SPECTATOR);
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.setGameMode(GameMode.SPECTATOR);
        player.getInventory().setItem(4, createSpectatorCompass());
        if (spectatorSpawn != null) {
            player.teleport(spectatorSpawn);
        }
        if (wasNew) {
            plugin.getMessageManager().send(player, "game.spectator", Map.of(
                    "player", player.getName(),
                    "map", arenaId,
                    "arena", arenaId
            ));
        }
    }

    private void startRunning() {
        phase = GamePhase.RUNNING;
        clearAllTeamIslandChests();
        runningParticipantsAtStart = getOnlineAssignedPlayerCount();
        runningQuitCount = 0;
        totalKillCount = 0;
        totalFinalKillCount = 0;
        diagnosticsTickerSeconds = 0;
        matchMetricsLogged = false;
        bedBreakSecondByTeam.clear();
        warningLastLogAt.clear();
        plugin.debug("start", "startRunning -> arena=" + arenaId + " players=" + playerTeams.keySet().stream()
                .map(uuid -> {
                    Player online = Bukkit.getPlayer(uuid);
                    TeamData team = playerTeams.get(uuid);
                    return (online == null ? uuid.toString() : online.getName()) + ":" + (team == null ? "null" : team.getId());
                }).toList());
        for (UUID uuid : new ArrayList<>(playerTeams.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                continue;
            }
            TeamData teamData = playerTeams.get(uuid);
            Location runningSpawn = resolveRunningSpawnLocation(teamData);
            if (runningSpawn != null) {
                player.teleport(runningSpawn);
                resetPerLifeStates(player);
                prepareCombatPlayer(player);
                equipTeamLoadout(player, teamData);
                plugin.debug("start", "startRunning -> teleported player=" + player.getName()
                        + " team=" + (teamData == null ? "null" : teamData.getId()) + " spawn=" + plugin.formatLocation(runningSpawn));
            }
            plugin.getMessageManager().send(player, "game.started", Map.of(
                    "player", player.getName(),
                    "map", arenaId,
                    "arena", arenaId
            ));
            showTitle(player,
                    "game.title.started-title",
                    "game.title.started-subtitle",
                    Map.of(
                            "player", player.getName(),
                            "map", arenaId,
                            "arena", arenaId
                    ));
        }
    }

    private void checkWin() {
        if (phase != GamePhase.RUNNING) {
            return;
        }

        List<TeamData> activeTeams = new ArrayList<>();
        for (TeamData teamData : mapConfig.getTeams()) {
            long onlinePlayers = teamData.getPlayers().stream()
                    .map(Bukkit::getPlayer)
                    .filter(player -> player != null && player.isOnline())
                    .count();
            if (onlinePlayers > 0 || hasReservedPlayers(teamData)) {
                activeTeams.add(teamData);
            }
        }

        if (activeTeams.isEmpty()) {
            logMatchMetrics("no-active-teams");
            clearDroppedItemsAtMatchEnd();
            phase = GamePhase.ENDING;
            resetCountdown = plugin.getConfigManager().getArenaResetDelaySeconds();
            return;
        }

        if (activeTeams.size() != 1) {
            return;
        }

        TeamData winner = activeTeams.getFirst();
        boolean opponentsStillAlive = mapConfig.getTeams().stream()
                .filter(teamData -> !teamData.getId().equalsIgnoreCase(winner.getId()))
                .anyMatch(teamData -> teamData.isBedAlive() || teamData.getPlayers().stream()
                        .map(Bukkit::getPlayer)
                        .anyMatch(player -> player != null && player.isOnline())
                        || hasReservedPlayers(teamData));
        if (opponentsStillAlive) {
            return;
        }

        logMatchMetrics("winner:" + winner.getId().toLowerCase());
        clearDroppedItemsAtMatchEnd();
        phase = GamePhase.ENDING;
        resetCountdown = plugin.getConfigManager().getArenaResetDelaySeconds();
        forEachOnlineParticipant(player -> {
            plugin.getMessageManager().send(player, "game.win", Map.of(
                    "team", winner.getDisplayName(),
                    "player", player.getName(),
                    "map", arenaId,
                    "arena", arenaId
            ));
            showTitle(player,
                    "game.title.win-title",
                    "game.title.win-subtitle",
                    Map.of(
                            "team", winner.getDisplayName(),
                            "player", player.getName(),
                            "map", arenaId,
                            "arena", arenaId
                    ));
        });
        playConfiguredSoundToParticipants("win", Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.7F, 1.0F);
    }

    private void resetArena() {
        logMatchMetrics("reset");
        List<Player> participants = collectOnlineParticipants();
        despawnShops();
        clearDroppedItemsAtMatchEnd();
        restoreModifiedBlocks();
        restoreBeds();
        clearAllTeamIslandChests();

        boolean returnedToFallback = sendPlayersToFallback(participants);
        this.playerTeams.clear();
        this.spectators.clear();
        loadBlueprint();
        if (returnedToFallback) {
            return;
        }
        for (Player player : participants) {
            if (player != null && player.isOnline()) {
                handleJoin(player);
            }
        }
    }

    private void tickTraps() {
        long now = System.currentTimeMillis();
        for (TeamData defendingTeam : mapConfig.getTeams()) {
            TeamUpgradeState state = teamUpgradeStates.computeIfAbsent(defendingTeam.getId().toLowerCase(), ignored -> new TeamUpgradeState());
            if (state.trapQueue.isEmpty() || state.trapRearmUntil > now) {
                continue;
            }

            Player intruder = findTrapIntruder(defendingTeam);
            if (intruder == null) {
                continue;
            }

            TrapType trapType = state.trapQueue.remove(0);
            if (trapType == null) {
                continue;
            }
            if (trapType == TrapType.SILENCE) {
                state.trapQueue.add(0, trapType);
                continue;
            }
            state.trapRearmUntil = now + plugin.getConfigManager().getTrapRearmMillis();
            triggerTrap(defendingTeam, intruder, trapType);
        }
    }

    private void triggerTrap(TeamData defendingTeam, Player intruder, TrapType trapType) {
        Map<String, String> placeholders = Map.of(
                "player", intruder.getName(),
                "team", defendingTeam.getDisplayName(),
                "trap", trapType.getDisplayName(),
                "map", arenaId,
                "arena", arenaId
        );

        for (UUID memberId : new ArrayList<>(defendingTeam.getPlayers())) {
            Player member = Bukkit.getPlayer(memberId);
            if (member == null || !member.isOnline()) {
                continue;
            }
            plugin.getMessageManager().send(member, "game.trap-triggered", placeholders);
            sendActionbar(member, "&c" + trapType.getDisplayName() + " &7→ &e" + intruder.getName());
            showTitle(member, "game.title.trap-title", "game.title.trap-subtitle", placeholders);
            playConfiguredSound(member, "trap-trigger", Sound.BLOCK_NOTE_BLOCK_BIT, 0.85F, 1.4F);
        }

        intruder.sendMessage(ColorUtil.colorize("&8[&bBedWars&8] &7Du hast &f" + trapType.getDisplayName()
                + " &7von Team " + defendingTeam.getDisplayName() + " &7ausgelöst."));

        switch (trapType) {
            case ALARM -> {
                intruder.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING,
                        plugin.getConfigManager().getTrapAlarmGlowingTicks(), 0, true, true, true));
                sendActionbar(intruder, "&dAlarm Trap &7hat dich markiert");
            }
            case BLIND -> {
                intruder.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS,
                        plugin.getConfigManager().getTrapBlindnessTicks(), 0, true, true, true));
                intruder.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,
                        plugin.getConfigManager().getTrapSlownessTicks(), 1, true, true, true));
                sendActionbar(intruder, "&5Blind Trap &7blendet dich für 4s");
            }
            case KNOCK -> {
                Location baseCenter = resolveTeamBaseCenter(defendingTeam);
                org.bukkit.util.Vector direction = intruder.getLocation().toVector().subtract(baseCenter.toVector());
                if (direction.lengthSquared() < 0.0001D) {
                    direction = intruder.getLocation().getDirection().multiply(-1);
                }
                if (direction.lengthSquared() > 0.0001D) {
                    direction.normalize();
                }
                intruder.setVelocity(direction.multiply(plugin.getConfigManager().getTrapKnockbackHorizontal())
                        .setY(plugin.getConfigManager().getTrapKnockbackVertical()));
                sendActionbar(intruder, "&6Knock Trap &7hat dich zurückgestoßen");
            }
            case SILENCE -> {
                PlayerLoadoutState state = playerLoadoutStates.computeIfAbsent(intruder.getUniqueId(), ignored -> new PlayerLoadoutState());
                long silenceUntil = System.currentTimeMillis() + plugin.getConfigManager().getTrapSilenceMillis();
                state.specialItemSilenceUntil = Math.max(state.specialItemSilenceUntil, silenceUntil);
                plugin.getMessageManager().send(intruder, "game.trap-silenced", Map.of(
                        "value", String.valueOf(Math.max(1L, (plugin.getConfigManager().getTrapSilenceMillis() + 999L) / 1000L)),
                        "map", arenaId,
                        "arena", arenaId
                ));
                sendActionbar(intruder, "&8Silence Trap &7→ &ckeine Pearl / TNT / Fireball");
            }
        }
    }

    private Player findTrapIntruder(TeamData defendingTeam) {
        if (defendingTeam == null) {
            return null;
        }

        for (UUID playerId : new ArrayList<>(playerTeams.keySet())) {
            TeamData playerTeam = playerTeams.get(playerId);
            if (playerTeam == null || playerTeam.getId().equalsIgnoreCase(defendingTeam.getId())) {
                continue;
            }

            Player candidate = Bukkit.getPlayer(playerId);
            if (candidate == null || !candidate.isOnline() || isSpectator(candidate) || isRespawning(candidate)) {
                continue;
            }
            if (isInsideTeamBaseRegion(candidate.getLocation(), defendingTeam)) {
                return candidate;
            }
        }
        return null;
    }

    private TeamData resolveDefendingTeamForIntruder(Player intruder) {
        if (intruder == null || intruder.getLocation() == null) {
            return null;
        }

        TeamData intruderTeam = playerTeams.get(intruder.getUniqueId());
        for (TeamData defendingTeam : mapConfig.getTeams()) {
            if (intruderTeam != null && defendingTeam.getId().equalsIgnoreCase(intruderTeam.getId())) {
                continue;
            }
            if (isInsideTeamBaseRegion(intruder.getLocation(), defendingTeam)) {
                return defendingTeam;
            }
        }
        return null;
    }

    private Location resolveTeamBaseCenter(TeamData teamData) {
        List<Location> anchors = new ArrayList<>();
        addAnchor(anchors, teamData.getSpawn());
        addAnchor(anchors, teamData.getBed());
        addAnchor(anchors, teamData.getGenerator());
        addAnchor(anchors, teamData.getItemShop());
        addAnchor(anchors, teamData.getUpgradeShop());
        if (anchors.isEmpty()) {
            return teamData.getSpawn() != null
                    ? teamData.getSpawn().clone()
                    : new Location(Bukkit.getWorlds().getFirst(), 0.0D, 80.0D, 0.0D);
        }

        World world = anchors.getFirst().getWorld();
        double x = 0.0D;
        double y = 0.0D;
        double z = 0.0D;
        for (Location anchor : anchors) {
            x += anchor.getX();
            y += anchor.getY();
            z += anchor.getZ();
        }
        int count = anchors.size();
        return new Location(world, x / count, y / count, z / count);
    }

    private double resolveVoidRescueSafeY(World world, TeamData teamData) {
        if (world == null) {
            return 0.0D;
        }

        double fallback = world.getMinHeight() + plugin.getConfigManager().getVoidRescueMinSafeYBuffer();
        List<Location> anchors = new ArrayList<>();
        if (teamData != null) {
            addAnchor(anchors, teamData.getSpawn());
            addAnchor(anchors, teamData.getBed());
            addAnchor(anchors, teamData.getGenerator());
            addAnchor(anchors, teamData.getItemShop());
            addAnchor(anchors, teamData.getUpgradeShop());
        }
        addAnchor(anchors, mapConfig == null ? null : mapConfig.getLobbySpawn());

        double lowestAnchorY = Double.MAX_VALUE;
        for (Location anchor : anchors) {
            if (anchor.getWorld() == null || !anchor.getWorld().getUID().equals(world.getUID())) {
                continue;
            }
            lowestAnchorY = Math.min(lowestAnchorY, anchor.getY());
        }

        if (lowestAnchorY == Double.MAX_VALUE) {
            return fallback;
        }
        return Math.max(fallback, lowestAnchorY + 1.0D);
    }

    private boolean isInsideTeamBaseRegion(Location location, TeamData teamData) {
        if (location == null || teamData == null || location.getWorld() == null) {
            return false;
        }

        List<Location> anchors = new ArrayList<>();
        addAnchor(anchors, teamData.getSpawn());
        addAnchor(anchors, teamData.getBed());
        addAnchor(anchors, teamData.getGenerator());
        addAnchor(anchors, teamData.getItemShop());
        addAnchor(anchors, teamData.getUpgradeShop());
        if (anchors.isEmpty()) {
            return false;
        }

        World world = anchors.getFirst().getWorld();
        if (world == null || !world.getUID().equals(location.getWorld().getUID())) {
            return false;
        }

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (Location anchor : anchors) {
            minX = Math.min(minX, anchor.getBlockX());
            minY = Math.min(minY, anchor.getBlockY());
            minZ = Math.min(minZ, anchor.getBlockZ());
            maxX = Math.max(maxX, anchor.getBlockX());
            maxY = Math.max(maxY, anchor.getBlockY());
            maxZ = Math.max(maxZ, anchor.getBlockZ());
        }

        minX -= BASE_REGION_PADDING_XZ;
        maxX += BASE_REGION_PADDING_XZ;
        minY -= BASE_REGION_PADDING_Y;
        maxY += BASE_REGION_PADDING_Y;
        minZ -= BASE_REGION_PADDING_XZ;
        maxZ += BASE_REGION_PADDING_XZ;

        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        return x >= minX && x <= maxX
                && y >= minY && y <= maxY
                && z >= minZ && z <= maxZ;
    }

    private void addAnchor(List<Location> anchors, Location location) {
        if (location != null && location.getWorld() != null) {
            anchors.add(location);
        }
    }

    private Location resolveSpectatorSpawnLocation(TeamData preferredTeam) {
        if (preferredTeam != null && preferredTeam.getSpawn() != null && preferredTeam.getSpawn().getWorld() != null) {
            return preferredTeam.getSpawn().clone();
        }

        for (UUID uuid : new ArrayList<>(playerTeams.keySet())) {
            Player online = Bukkit.getPlayer(uuid);
            if (online != null && online.isOnline() && !spectators.contains(uuid) && !respawningPlayers.contains(uuid)) {
                return online.getLocation().clone();
            }
        }

        if (mapConfig.getLobbySpawn() != null && mapConfig.getLobbySpawn().getWorld() != null) {
            return mapConfig.getLobbySpawn().clone();
        }

        for (TeamData teamData : mapConfig.getTeams()) {
            if (teamData.getSpawn() != null && teamData.getSpawn().getWorld() != null) {
                return teamData.getSpawn().clone();
            }
        }

        for (TeamData teamData : mapConfig.getTeams()) {
            if (teamData.getBed() != null && teamData.getBed().getWorld() != null) {
                return teamData.getBed().clone();
            }
        }

        return null;
    }

    private Location resolveRunningSpawnLocation(TeamData teamData) {
        if (teamData == null) {
            return null;
        }
        if (teamData.getSpawn() == null) {
            plugin.debug("spawn", "resolveRunningSpawnLocation -> missing team spawn arena=" + arenaId
                    + " team=" + teamData.getId());
            return mapConfig.getLobbySpawn();
        }
        return teamData.getSpawn();
    }


    private void tickEndgame() {
        int bedDestructionAfter = plugin.getConfigManager().getBedDestructionAfterSeconds();
        announceBedDestructionWarnings(bedDestructionAfter);
        if (!endgameBedsDestroyed && bedDestructionAfter > 0 && runningSeconds >= bedDestructionAfter) {
            endgameBedsDestroyed = true;
            destroyAllBedsForEndgame();
            broadcast("game.endgame-bed-destroyed", Map.of(
                    "map", arenaId,
                    "arena", arenaId
            ));
            checkWin();
        }

        int suddenDeathAfter = plugin.getConfigManager().getSuddenDeathAfterSeconds();
        if (!suddenDeathActive && suddenDeathAfter > 0 && runningSeconds >= suddenDeathAfter) {
            suddenDeathActive = true;
            if (!endgameBedsDestroyed) {
                endgameBedsDestroyed = true;
                destroyAllBedsForEndgame();
                broadcast("game.endgame-bed-destroyed", Map.of(
                        "map", arenaId,
                        "arena", arenaId
                ));
            }
            broadcast("game.endgame-sudden-death", Map.of(
                    "map", arenaId,
                    "arena", arenaId
            ));
            checkWin();
        }
    }

    private void announceBedDestructionWarnings(int bedDestructionAfter) {
        if (endgameBedsDestroyed || bedDestructionAfter <= 0) {
            return;
        }

        int remaining = bedDestructionAfter - runningSeconds;
        if (remaining <= 0) {
            return;
        }

        Set<Integer> warningMoments = Set.of(300, 60, 30, 10, 5, 4, 3, 2, 1);
        if (!warningMoments.contains(remaining) || !announcedBedWarningSeconds.add(remaining)) {
            return;
        }

        broadcast("game.endgame-bed-warning", Map.of(
                "value", formatDuration(remaining),
                "seconds", String.valueOf(remaining),
                "map", arenaId,
                "arena", arenaId
        ));
    }

    private void destroyAllBedsForEndgame() {
        for (TeamData teamData : mapConfig.getTeams()) {
            if (!teamData.isBedAlive()) {
                continue;
            }
            teamData.setBedAlive(false);
            clearBedBlocks(teamData, teamData.getBed());
        }
    }

    private void markOfflineReservation(UUID uuid) {
        if (uuid == null || !playerTeams.containsKey(uuid)) {
            return;
        }
        spectators.remove(uuid);
        respawningPlayers.remove(uuid);
        playerStates.put(uuid, PlayerMatchState.OFFLINE_RESERVED);
        long expiresAt = System.currentTimeMillis() + (plugin.getConfigManager().getRejoinReservationSeconds() * 1000L);
        offlineReservations.put(uuid, expiresAt);
    }

    private boolean isOfflineReservationActive(UUID uuid) {
        Long expiresAt = offlineReservations.get(uuid);
        return expiresAt != null && expiresAt > System.currentTimeMillis();
    }

    private boolean removeExpiredReservations() {
        long now = System.currentTimeMillis();
        List<UUID> expired = new ArrayList<>();
        for (Map.Entry<UUID, Long> entry : offlineReservations.entrySet()) {
            if (entry.getValue() <= now) {
                expired.add(entry.getKey());
            }
        }

        if (expired.isEmpty()) {
            return false;
        }

        for (UUID uuid : expired) {
            offlineReservations.remove(uuid);
            TeamData teamData = playerTeams.remove(uuid);
            if (teamData != null) {
                teamData.getPlayers().remove(uuid);
            }
            playerStates.remove(uuid);
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                plugin.getMessageManager().send(player, "game.rejoin-reserved", Map.of(
                        "map", arenaId,
                        "arena", arenaId
                ));
            }
        }
        return true;
    }

    private boolean hasReservedPlayers(TeamData teamData) {
        if (teamData == null) {
            return false;
        }
        for (UUID uuid : teamData.getPlayers()) {
            if (isOfflineReservationActive(uuid)) {
                return true;
            }
        }
        return false;
    }

    private boolean tryRestoreOfflineReservation(Player player) {
        UUID uuid = player.getUniqueId();
        if (!isOfflineReservationActive(uuid)) {
            offlineReservations.remove(uuid);
            return false;
        }

        TeamData teamData = playerTeams.get(uuid);
        offlineReservations.remove(uuid);
        if (teamData == null) {
            playerStates.remove(uuid);
            return false;
        }

        spectators.remove(uuid);
        respawningPlayers.remove(uuid);
        playerStates.put(uuid, PlayerMatchState.ALIVE);

        if (phase == GamePhase.RUNNING) {
            Location spawn = teamData.getSpawn() == null ? mapConfig.getLobbySpawn() : teamData.getSpawn();
            if (spawn != null) {
                player.teleport(spawn);
            }
            prepareCombatPlayer(player);
            equipTeamLoadout(player, teamData);
            plugin.getMessageManager().send(player, "game.rejoin-success", Map.of(
                    "team", teamData.getDisplayName(),
                    "map", arenaId,
                    "arena", arenaId
            ));
            return true;
        }

        return false;
    }

    private Player resolveRecentKiller(Player victim) {
        if (victim == null) {
            return null;
        }
        UUID victimId = victim.getUniqueId();
        UUID killerId = lastDamagerByVictim.get(victimId);
        Long damageTime = lastDamageTimestampByVictim.get(victimId);
        if (killerId == null || damageTime == null) {
            return null;
        }
        if (System.currentTimeMillis() - damageTime > KILL_CREDIT_WINDOW_MS) {
            clearCombatMarker(victimId);
            return null;
        }
        Player killer = Bukkit.getPlayer(killerId);
        if (killer == null || !killer.isOnline()) {
            return null;
        }
        return killer;
    }

    private void clearCombatMarker(UUID victimId) {
        if (victimId == null) {
            return;
        }
        lastDamagerByVictim.remove(victimId);
        lastDamageTimestampByVictim.remove(victimId);
    }

    private void broadcastKillEvent(Player killer, Player victim, boolean finalKill) {
        String killerName = killer == null ? "Umwelt" : killer.getName();
        String messagePath = finalKill ? "game.final-killfeed" : "game.killfeed";
        Map<String, String> placeholders = Map.of(
                "killer", killerName,
                "victim", victim.getName(),
                "map", arenaId,
                "arena", arenaId
        );

        broadcast(messagePath, placeholders);
        showTitleToParticipants(
                finalKill ? "game.title.final-kill-title" : "game.title.kill-title",
                finalKill ? "game.title.final-kill-subtitle" : "game.title.kill-subtitle",
                placeholders
        );

        String actionbar = ColorUtil.colorize((finalKill ? "&cFINAL &8| " : "&eKILL &8| ") + "&f" + killerName + " &7-> &f" + victim.getName());
        forEachOnlineParticipant(participant -> sendActionbar(participant, actionbar));

        totalKillCount++;
        if (finalKill) {
            totalFinalKillCount++;
        }

        if (killer != null) {
            killsByPlayer.merge(killer.getUniqueId(), 1, Integer::sum);
            if (finalKill) {
                finalKillsByPlayer.merge(killer.getUniqueId(), 1, Integer::sum);
            }
            killer.playSound(killer.getLocation(), finalKill ? Sound.ENTITY_PLAYER_LEVELUP : Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.9F, 1.1F);
        }
        if (finalKill) {
            playConfiguredSoundToParticipants("final-kill", Sound.ENTITY_WITHER_DEATH, 0.25F, 1.8F);
        }
    }

    private void clearDroppedItemsAtMatchEnd() {
        for (UUID droppedItemId : new ArrayList<>(trackedDroppedItems)) {
            Entity entity = Bukkit.getEntity(droppedItemId);
            if (entity instanceof Item item) {
                item.remove();
            }
        }
        trackedDroppedItems.clear();

        if (!plugin.getConfigManager().shouldScanWorldForMatchEndItems()) {
            return;
        }

        for (World world : Bukkit.getWorlds()) {
            for (Item item : world.getEntitiesByClass(Item.class)) {
                String arenaMarker = item.getPersistentDataContainer().get(arenaDropKey, PersistentDataType.STRING);
                if (arenaId.equalsIgnoreCase(arenaMarker)) {
                    item.remove();
                    continue;
                }

                String generatorMarker = item.getPersistentDataContainer().get(generatorDropKey, PersistentDataType.STRING);
                if (generatorMarker != null && generatorMarker.startsWith(arenaId + ":")) {
                    item.remove();
                }
            }
        }
    }

    private void clearBedBlocks(TeamData teamData, Location brokenBedLocation) {
        List<StoredBlockState> snapshots = bedSnapshotsByTeam.getOrDefault(teamData.getId().toLowerCase(), List.of());
        Set<String> clearedKeys = new HashSet<>();
        for (StoredBlockState snapshot : snapshots) {
            clearedKeys.add(toBlockKey(snapshot.worldName, snapshot.x, snapshot.y, snapshot.z));
            snapshot.clear();
        }
        for (Block block : resolveBedPair(brokenBedLocation)) {
            String blockKey = toBlockKey(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
            if (clearedKeys.add(blockKey)) {
                block.setType(Material.AIR, false);
            }
        }
        for (Block block : resolveBedPair(teamData.getBed())) {
            String blockKey = toBlockKey(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
            if (clearedKeys.add(blockKey)) {
                block.setType(Material.AIR, false);
            }
        }
    }

    private void restoreBeds() {
        for (List<StoredBlockState> snapshots : bedSnapshotsByTeam.values()) {
            ensureCompleteBedSnapshots(snapshots);
            for (StoredBlockState snapshot : snapshots) {
                World world = Bukkit.getWorld(snapshot.worldName);
                if (world == null) {
                    continue;
                }
                Block block = world.getBlockAt(snapshot.x, snapshot.y, snapshot.z);
                block.setType(Bukkit.createBlockData(snapshot.blockDataString).getMaterial(), false);
            }
            for (StoredBlockState snapshot : snapshots) {
                snapshot.restore();
            }
        }
    }

    private void restoreModifiedBlocks() {
        for (StoredBlockState storedBlockState : modifiedBlocks.values()) {
            storedBlockState.restore();
        }
        modifiedBlocks.clear();
        placedBlockKeys.clear();
    }

    private void clearAllTeamIslandChests() {
        if (mapConfig == null) {
            return;
        }
        List<Location> anchors = collectArenaContainerAnchors();
        if (anchors.isEmpty()) {
            return;
        }

        World world = anchors.getFirst().getWorld();
        if (world == null) {
            return;
        }

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (Location anchor : anchors) {
            if (anchor.getWorld() == null || !anchor.getWorld().getUID().equals(world.getUID())) {
                continue;
            }
            minX = Math.min(minX, anchor.getBlockX());
            minY = Math.min(minY, anchor.getBlockY());
            minZ = Math.min(minZ, anchor.getBlockZ());
            maxX = Math.max(maxX, anchor.getBlockX());
            maxY = Math.max(maxY, anchor.getBlockY());
            maxZ = Math.max(maxZ, anchor.getBlockZ());
        }

        minX -= ARENA_CONTAINER_CLEAR_PADDING_XZ;
        maxX += ARENA_CONTAINER_CLEAR_PADDING_XZ;
        minY -= ARENA_CONTAINER_CLEAR_PADDING_Y;
        maxY += ARENA_CONTAINER_CLEAR_PADDING_Y;
        minZ -= ARENA_CONTAINER_CLEAR_PADDING_XZ;
        maxZ += ARENA_CONTAINER_CLEAR_PADDING_XZ;

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockState blockState = world.getBlockAt(x, y, z).getState();
                    if (!(blockState instanceof Container container)) {
                        continue;
                    }
                    container.getInventory().clear();
                    container.update(true, false);
                }
            }
        }
    }

    private List<Location> collectArenaContainerAnchors() {
        List<Location> anchors = new ArrayList<>();
        addAnchor(anchors, mapConfig.getLobbySpawn());
        for (TeamData teamData : mapConfig.getTeams()) {
            addAnchor(anchors, teamData.getSpawn());
            addAnchor(anchors, teamData.getBed());
            addAnchor(anchors, teamData.getGenerator());
            addAnchor(anchors, teamData.getItemShop());
            addAnchor(anchors, teamData.getUpgradeShop());
        }
        for (GeneratorConfig generatorConfig : mapConfig.getGenerators()) {
            addAnchor(anchors, generatorConfig.getLocation());
        }
        for (ShopConfig shopConfig : mapConfig.getShops()) {
            addAnchor(anchors, shopConfig.getLocation());
        }
        return anchors;
    }

    private void initializeGenerators() {
        generatorRuntimes.clear();
        for (GeneratorConfig generatorConfig : mapConfig.getGenerators()) {
            if (generatorConfig.getLocation() == null) {
                continue;
            }

            if (generatorConfig.isTeamGenerator() || generatorConfig.getType().equalsIgnoreCase("team")) {
                generatorRuntimes.add(new GeneratorRuntime(generatorConfig.getId() + "-iron", generatorConfig.getLocation(), plugin.getConfigManager().getGeneratorTierConfigs("team-iron")));
                generatorRuntimes.add(new GeneratorRuntime(generatorConfig.getId() + "-gold", generatorConfig.getLocation(), plugin.getConfigManager().getGeneratorTierConfigs("team-gold")));
                continue;
            }

            generatorRuntimes.add(createRuntimeForGenerator(generatorConfig));
        }
    }

    private GeneratorRuntime createRuntimeForGenerator(GeneratorConfig generatorConfig) {
        return new GeneratorRuntime(generatorConfig.getId(), generatorConfig.getLocation(), plugin.getConfigManager().getGeneratorTierConfigs(generatorConfig.getType()));
    }

    private void tickGenerators() {
        for (GeneratorRuntime generatorRuntime : generatorRuntimes) {
            generatorRuntime.tick();
        }
    }

    private String formatDuration(int totalSeconds) {
        int minutes = Math.max(0, totalSeconds) / 60;
        int seconds = Math.max(0, totalSeconds) % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    private String toRoman(int value) {
        return switch (Math.max(1, value)) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(value);
        };
    }

    private void spawnShops() {
        spawnedShopEntities.clear();
        for (ShopConfig shopConfig : mapConfig.getShops()) {
            if (shopConfig.getLocation() == null || shopConfig.getLocation().getWorld() == null) {
                continue;
            }
            Villager villager = (Villager) shopConfig.getLocation().getWorld().spawnEntity(shopConfig.getLocation(), org.bukkit.entity.EntityType.VILLAGER);
            villager.setAI(false);
            villager.setInvulnerable(true);
            villager.setCollidable(false);
            villager.setCanPickupItems(false);
            villager.setSilent(true);
            villager.setCustomNameVisible(true);
            villager.setProfession(Villager.Profession.LIBRARIAN);
            villager.setAdult();
            villager.setAgeLock(true);
            villager.setPersistent(true);
            villager.setRemoveWhenFarAway(false);
            villager.setCustomName(ColorUtil.colorize(plugin.getConfigManager().getShopVillagerName(shopConfig.getType())));
            spawnedShopEntities.put(villager.getUniqueId(), shopConfig);
        }
    }

    private void despawnShops() {
        for (UUID uuid : new ArrayList<>(spawnedShopEntities.keySet())) {
            Entity entity = Bukkit.getEntity(uuid);
            if (entity != null) {
                entity.remove();
            }
        }
        spawnedShopEntities.clear();
    }

    private void openShop(Player player, ShopConfig shopConfig) {
        String shopType = shopConfig.getType().equalsIgnoreCase("upgrade") ? "upgrade" : "item";
        TeamData teamData = playerTeams.get(player.getUniqueId());
        if (teamData == null) {
            return;
        }

        String holderTeamId = teamData.getId().toLowerCase(Locale.ROOT);
        ShopInventoryHolder holder = new ShopInventoryHolder(arenaId, shopType, holderTeamId, resolveDefaultCategoryId(shopType));
        int inventorySize = ITEM_SHOP_SIZE;
        Inventory inventory = Bukkit.createInventory(holder, inventorySize,
                ColorUtil.colorize(plugin.getConfigManager().getShopGuiTitle(shopType)));
        holder.setInventory(inventory);

        renderShopInventory(holder, player, teamData);
        player.openInventory(inventory);
    }

    private void renderShopInventory(ShopInventoryHolder holder, Player player, TeamData teamData) {
        if (holder.getShopType().equalsIgnoreCase("upgrade")) {
            renderUpgradeShopInventory(holder, player, teamData);
            return;
        }

        Inventory inventory = holder.getInventory();
        inventory.clear();
        decorateShopInventory(inventory);

        List<ShopCategoryConfig> categories = getCachedShopCategories(holder.getShopType());
        String selectedCategory = holder.getSelectedCategoryId();
        if (selectedCategory == null || selectedCategory.isBlank()) {
            selectedCategory = resolveDefaultCategoryId(holder.getShopType());
            holder.setSelectedCategoryId(selectedCategory);
        }

        Map<Integer, String> categorySlots = new LinkedHashMap<>();
        for (ShopCategoryConfig categoryConfig : categories) {
            int slot = categoryConfig.getSlot();
            if (slot < 0 || slot >= inventory.getSize() || isReservedShopDecorationSlot(inventory, slot)) {
                continue;
            }
            boolean active = categoryConfig.getId().equalsIgnoreCase(selectedCategory);
            inventory.setItem(slot, createCategoryIcon(categoryConfig, active));
            categorySlots.put(slot, categoryConfig.getId());
        }
        holder.setCategorySlots(categorySlots);

        List<ShopOfferConfig> offers = getCachedShopOffers(holder.getShopType()).stream()
                .filter(offer -> offer.getCategory().equalsIgnoreCase(holder.getSelectedCategoryId()))
                .toList();
        if (offers.isEmpty()) {
            offers = getCachedShopOffers(holder.getShopType());
        }

        Map<Integer, ShopOfferConfig> offerSlots = new LinkedHashMap<>();
        for (ShopOfferConfig offer : offers) {
            int slot = offer.getSlot();
            if (slot < 0 || slot >= inventory.getSize() || categorySlots.containsKey(slot) || isReservedShopDecorationSlot(inventory, slot)) {
                continue;
            }
            inventory.setItem(slot, createShopIcon(player, teamData, offer));
            offerSlots.put(slot, offer);
        }
        holder.setOfferSlots(offerSlots);

        if (inventory.getSize() == ITEM_SHOP_SIZE) {
            inventory.setItem(ITEM_SHOP_INFO_SLOT, createShopInfoItem(holder.getShopType(), holder.getSelectedCategoryId(), offers.size()));
            inventory.setItem(ITEM_SHOP_STATUS_SLOT, createShopStatusItem(player, teamData, holder.getShopType(), holder.getSelectedCategoryId()));
            inventory.setItem(ITEM_SHOP_CLOSE_SLOT, createGuiItem(Material.BARRIER, "&cSchließen", List.of(
                    "&7Zurück ins Match.",
                    "&8Klicke zum Schließen"
            )));
        } else {
            int infoSlot = 24;
            if (!categorySlots.containsKey(infoSlot) && !offerSlots.containsKey(infoSlot) && !isReservedShopDecorationSlot(inventory, infoSlot)) {
                inventory.setItem(infoSlot, createShopInfoItem(holder.getShopType(), holder.getSelectedCategoryId(), offers.size()));
            }
        }
    }

    private void renderUpgradeShopInventory(ShopInventoryHolder holder, Player player, TeamData teamData) {
        Inventory inventory = holder.getInventory();
        inventory.clear();
        decorateShopInventory(inventory);

        List<ShopCategoryConfig> categories = getCachedShopCategories(holder.getShopType());
        String selectedCategory = holder.getSelectedCategoryId();
        if (selectedCategory == null || selectedCategory.isBlank()) {
            selectedCategory = "team-upgrades";
            holder.setSelectedCategoryId(selectedCategory);
        }
        final String effectiveSelectedCategory = selectedCategory;

        Map<Integer, String> categorySlots = new LinkedHashMap<>();
        for (ShopCategoryConfig categoryConfig : categories) {
            int slot = categoryConfig.getSlot();
            if (slot < 0 || slot >= inventory.getSize() || isReservedShopDecorationSlot(inventory, slot)) {
                continue;
            }
            boolean active = categoryConfig.getId().equalsIgnoreCase(effectiveSelectedCategory);
            inventory.setItem(slot, createCategoryIcon(categoryConfig, active));
            categorySlots.put(slot, categoryConfig.getId());
        }
        holder.setCategorySlots(categorySlots);

        List<ShopOfferConfig> offers = getCachedShopOffers(holder.getShopType()).stream()
                .filter(offer -> offer.getCategory().equalsIgnoreCase(effectiveSelectedCategory))
                .toList();

        Map<Integer, ShopOfferConfig> offerSlots = new LinkedHashMap<>();
        for (ShopOfferConfig offer : offers) {
            int slot = offer.getSlot();
            if (slot < 0 || slot >= inventory.getSize() || categorySlots.containsKey(slot) || isReservedShopDecorationSlot(inventory, slot)) {
                continue;
            }
            inventory.setItem(slot, createUpgradeShopIcon(player, teamData, offer));
            offerSlots.put(slot, offer);
        }
        holder.setOfferSlots(offerSlots);

        inventory.setItem(ITEM_SHOP_INFO_SLOT, createShopInfoItem(holder.getShopType(), effectiveSelectedCategory, offers.size()));
        inventory.setItem(ITEM_SHOP_STATUS_SLOT, createUpgradeStatusItem(player, teamData, effectiveSelectedCategory));
        inventory.setItem(ITEM_SHOP_CLOSE_SLOT, createGuiItem(Material.BARRIER, "&cSchließen", List.of(
                "&7Zurück ins Match.",
                "&8Klicke zum Schließen"
        )));
    }

    private String resolveDefaultCategoryId(String shopType) {
        List<ShopCategoryConfig> categories = getCachedShopCategories(shopType);
        if (categories.isEmpty()) {
            return shopType != null && shopType.equalsIgnoreCase("upgrade") ? "team-upgrades" : "blocks";
        }
        return categories.getFirst().getId();
    }

    private void cacheShopData() {
        cachedShopCategoriesByType.clear();
        cachedShopOffersByType.clear();
        for (String shopType : List.of("item", "upgrade")) {
            cachedShopCategoriesByType.put(shopType, List.copyOf(plugin.getConfigManager().getShopCategories(shopType)));
            cachedShopOffersByType.put(shopType, List.copyOf(plugin.getConfigManager().getShopOffers(shopType)));
        }
    }

    private List<ShopCategoryConfig> getCachedShopCategories(String shopType) {
        String normalized = shopType != null && shopType.equalsIgnoreCase("upgrade") ? "upgrade" : "item";
        return cachedShopCategoriesByType.getOrDefault(normalized, List.of());
    }

    private List<ShopOfferConfig> getCachedShopOffers(String shopType) {
        String normalized = shopType != null && shopType.equalsIgnoreCase("upgrade") ? "upgrade" : "item";
        return cachedShopOffersByType.getOrDefault(normalized, List.of());
    }

    private void decorateShopInventory(Inventory inventory) {
        if (inventory.getSize() == ITEM_SHOP_SIZE) {
            ItemStack borderPane = createGuiItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " ");
            inventory.setItem(0, createGuiItem(Material.STONE_BUTTON, " "));
            inventory.setItem(8, createGuiItem(Material.STONE_BUTTON, " "));
            inventory.setItem(45, createGuiItem(Material.STONE_BUTTON, " "));
            inventory.setItem(53, createGuiItem(Material.STONE_BUTTON, " "));
            for (int slot : new int[]{9, 18, 27, 36, 17, 26, 35, 44}) {
                inventory.setItem(slot, borderPane.clone());
            }
            for (int slot : new int[]{10, 11, 12, 13, 14, 15, 16}) {
                inventory.setItem(slot, createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " "));
            }
            return;
        }

        inventory.setItem(0, createGuiItem(Material.STONE_BUTTON, " "));
        inventory.setItem(8, createGuiItem(Material.STONE_BUTTON, " "));
        inventory.setItem(18, createGuiItem(Material.STONE_BUTTON, " "));
        inventory.setItem(26, createGuiItem(Material.STONE_BUTTON, " "));
        for (int slot : new int[]{9, 17}) {
            inventory.setItem(slot, createGuiItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " "));
        }
    }

    private boolean isReservedShopDecorationSlot(Inventory inventory, int slot) {
        if (inventory.getSize() == ITEM_SHOP_SIZE) {
            if (slot == ITEM_SHOP_INFO_SLOT || slot == ITEM_SHOP_STATUS_SLOT || slot == ITEM_SHOP_CLOSE_SLOT) {
                return true;
            }
            return slot == 0 || slot == 8 || slot == 45 || slot == 53
                    || slot == 9 || slot == 17 || slot == 18 || slot == 26 || slot == 27 || slot == 35 || slot == 36 || slot == 44;
        }
        return switch (slot) {
            case 0, 1, 7, 8, 9, 17, 18, 19, 25, 26 -> true;
            default -> false;
        };
    }

    private ItemStack createCategoryIcon(ShopCategoryConfig categoryConfig, boolean active) {
        ItemStack itemStack = new ItemStack(categoryConfig.getIcon());
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta != null) {
            itemMeta.setDisplayName(ColorUtil.colorize((active ? "&b&l» " : "&7") + categoryConfig.getDisplayName()));
            itemMeta.setLore(List.of(
                    ColorUtil.colorize("&8Kategorie"),
                    "",
                    ColorUtil.colorize("&7Klicke, um diese Ansicht zu öffnen")
            ));
            itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            itemStack.setItemMeta(itemMeta);
        }
        return itemStack;
    }

    private ItemStack createUpgradeShopIcon(Player player, TeamData teamData, ShopOfferConfig offer) {
        UpgradeViewState state = resolveUpgradeViewState(player, teamData, offer);
        ItemStack itemStack = createOfferResultTemplate(player, offer).clone();
        itemStack.setAmount(Math.max(1, offer.getAmount()));
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) {
            return itemStack;
        }

        String displayNamePrefix = switch (state.status) {
            case OWNED -> "&a";
            case AVAILABLE -> "&e";
            case NO_RESOURCES, LOCKED -> "&7";
        };
        itemMeta.setDisplayName(ColorUtil.colorize(displayNamePrefix + offer.getDisplayName()));
        if (state.status == UpgradeViewStatus.OWNED) {
            itemMeta.addEnchant(Enchantment.PROTECTION, 1, true);
            itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        List<String> lore = new ArrayList<>();
        lore.add(buildOfferDescriptionLoreLine(offer));
        lore.add("");
        lore.addAll(buildUpgradeStatusLines(offer, state));
        String cooldownLine = getAbilityCooldownLine(offer);
        if (!cooldownLine.isBlank()) {
            lore.add(cooldownLine);
        }
        lore.add(buildLoreFieldLine("Status", getUpgradeViewStatusValue(offer, state)));

        itemMeta.setLore(lore);
        itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    private List<String> buildUpgradeStatusLines(ShopOfferConfig offer, UpgradeViewState state) {
        List<String> lines = new ArrayList<>();
        lines.add(buildLoreFieldLine("Aktuell", state.currentLevel <= 0
                ? "&fKeine"
                : "&b" + buildUpgradeStageName(offer, state.currentLevel)));
        if (state.status == UpgradeViewStatus.LOCKED && isTrapOffer(offer)) {
            lines.add(buildLoreFieldLine("Status", "&cTrap-Queue ist bereits voll"));
            return lines;
        }
        if (state.maxLevel > 1 && state.currentLevel < state.maxLevel) {
            lines.add(buildLoreFieldLine("Nächste Stufe", "&b" + buildUpgradeStageName(offer, state.currentLevel + 1)));
        }
        if (state.maxLevel > 1) {
            for (int level = 1; level <= state.maxLevel; level++) {
                if (level <= state.currentLevel) {
                    lines.add(buildLoreFieldLine("Level " + level, "&aGekauft"));
                } else if (level == state.currentLevel + 1) {
                    lines.add(buildLoreFieldLine("Level " + level,
                            state.status == UpgradeViewStatus.NO_RESOURCES ? "&cZu teuer" : "&eKaufbar"));
                } else {
                    lines.add(buildLoreFieldLine("Level " + level, "&7Gesperrt"));
                }
            }
        }
        if (state.nextCost > 0) {
            lines.add(buildLoreFieldLine("Kosten", formatLoreCurrencyValue(offer.getCurrency(), state.nextCost)));
        }
        return lines;
    }

    private String buildUpgradeStageName(ShopOfferConfig offer, int level) {
        String baseName = ChatColor.stripColor(ColorUtil.colorize(offer.getDisplayName()));
        int maxLevel = resolveOfferMaxLevel(offer);
        if (maxLevel <= 1) {
            return baseName;
        }
        return baseName + " " + toRoman(level);
    }

    private UpgradeViewState resolveUpgradeViewState(Player player, TeamData teamData, ShopOfferConfig offer) {
        int currentLevel = resolveOfferCurrentLevel(player, teamData, offer);
        int maxLevel = resolveOfferMaxLevel(offer);
        if (currentLevel >= maxLevel) {
            return new UpgradeViewState(currentLevel, maxLevel, 0, UpgradeViewStatus.OWNED);
        }

        if (isAbilityUpgradeOffer(offer) && !hasPurchasedAbilityItem(player, offer)) {
            return new UpgradeViewState(currentLevel, maxLevel, 0, UpgradeViewStatus.LOCKED);
        }

        if (isTrapOffer(offer) && teamData != null) {
            TeamUpgradeState teamState = teamUpgradeStates.computeIfAbsent(teamData.getId().toLowerCase(), ignored -> new TeamUpgradeState());
            if (isTrapQueueFull(teamState)) {
                return new UpgradeViewState(currentLevel, maxLevel, 0, UpgradeViewStatus.LOCKED);
            }
        }

        int nextCost = resolveOfferCost(player, teamData, offer);
        boolean affordable = hasCurrency(player, offer.getCurrency(), nextCost);
        UpgradeViewStatus status = affordable ? UpgradeViewStatus.AVAILABLE : UpgradeViewStatus.NO_RESOURCES;
        return new UpgradeViewState(currentLevel, maxLevel, nextCost, status);
    }

    private int resolveOfferCurrentLevel(Player player, TeamData teamData, ShopOfferConfig offer) {
        PlayerLoadoutState playerState = playerLoadoutStates.computeIfAbsent(player.getUniqueId(), ignored -> new PlayerLoadoutState());
        TeamUpgradeState teamState = teamData == null ? null : teamUpgradeStates.computeIfAbsent(teamData.getId().toLowerCase(), ignored -> new TeamUpgradeState());
        return switch (offer.getAction().toLowerCase(Locale.ROOT)) {
            case "protection" -> teamState == null ? 0 : teamState.protectionLevel;
            case "sharpness" -> teamState != null && teamState.hasSharpness ? 1 : 0;
            case "haste" -> teamState == null ? 0 : teamState.hasteLevel;
            case "forge" -> teamState == null ? 0 : teamState.forgeLevel;
            case "trap", "alarm-trap", "blind-trap", "knock-trap", "silence-trap" -> {
                TrapType trapType = resolveTrapType(offer.getAction());
                yield teamState != null && trapType != null && teamState.trapQueue.contains(trapType) ? 1 : 0;
            }
            case "bow-power" -> teamState == null ? 0 : teamState.bowPowerLevel;
            case "bow-punch" -> teamState == null ? 0 : teamState.bowPunchLevel;
            case "bow-flame" -> teamState != null && teamState.hasBowFlame ? 1 : 0;
            case "iron-armor" -> Math.min(1, playerState.armorTier);
            case "diamond-armor" -> playerState.armorTier >= 2 ? 1 : 0;
            case "diamond-sword" -> playerState.swordTier >= 3 ? 1 : 0;
            case "dash" -> playerState.dashLevel;
            case "berserker" -> playerState.berserkerLevel;
            case "builder" -> playerState.builderLevel;
            case "shield" -> playerState.shieldLevel;
            case "tracker" -> playerState.trackerLevel;
            case "void-rescue" -> playerState.hasVoidRescueUpgrade ? 1 : 0;
            default -> 0;
        };
    }

    private int resolveOfferMaxLevel(ShopOfferConfig offer) {
        return switch (offer.getAction().toLowerCase(Locale.ROOT)) {
            case "protection" -> plugin.getConfigManager().getUpgradeMaxLevel("protection", 4);
            case "haste" -> plugin.getConfigManager().getUpgradeMaxLevel("haste", 2);
            case "forge" -> plugin.getConfigManager().getUpgradeMaxLevel("forge", 3);
            case "bow-power" -> plugin.getConfigManager().getUpgradeMaxLevel("bow-power", 3);
            case "bow-punch" -> plugin.getConfigManager().getUpgradeMaxLevel("bow-punch", 2);
            case "dash", "berserker", "builder", "shield", "tracker" -> 2;
            default -> 1;
        };
    }

    private ItemStack createUpgradeStatusItem(Player player, TeamData teamData, String categoryId) {
        List<String> lore = new ArrayList<>();
        lore.add("&7Eisen: &f" + getCurrencyAmount(player, Material.IRON_INGOT));
        lore.add("&7Gold: &6" + getCurrencyAmount(player, Material.GOLD_INGOT));
        lore.add("&7Diamant: &b" + getCurrencyAmount(player, Material.DIAMOND));
        lore.add("&7Emerald: &a" + getCurrencyAmount(player, Material.EMERALD));
        if (teamData != null) {
            TeamUpgradeState state = teamUpgradeStates.computeIfAbsent(teamData.getId().toLowerCase(), ignored -> new TeamUpgradeState());
            PlayerLoadoutState playerState = playerLoadoutStates.computeIfAbsent(player.getUniqueId(), ignored -> new PlayerLoadoutState());
            lore.add("&8");
            lore.add("&bTeam aktiv: &fProt " + state.protectionLevel + " &8| &fSharp " + (state.hasSharpness ? "✔" : "✘")
                    + " &8| &fHaste " + state.hasteLevel + " &8| &fForge " + state.forgeLevel);
            lore.add("&dTraps: &f" + state.trapQueue.size() + "/" + plugin.getConfigManager().getTrapMaxActiveCount()
                    + " &8| &f" + formatTrapQueue(state));
            lore.add("&6Bogen: &fPower " + state.bowPowerLevel + " &8| &fPunch " + state.bowPunchLevel
                    + " &8| &fFlame " + (state.hasBowFlame ? "✔" : "✘"));
            lore.add("&dDu: &fSword " + swordTierName(playerState.swordTier) + " &8| &fArmor " + armorTierName(playerState.armorTier)
                    + " &8| &fSkills " + countOwnedAbilities(playerState));
        }
        return createGuiItem(Material.NETHER_STAR, "&bUpgrade-Status &7• &f" + resolveCategoryDisplayName("upgrade", categoryId), lore);
    }

    private ItemStack createShopIcon(Player player, TeamData teamData, ShopOfferConfig offer) {
        ItemStack itemStack = createOfferResultTemplate(player, offer).clone();
        itemStack.setAmount(Math.max(1, offer.getAmount()));
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta != null) {
            int resolvedCost = resolveOfferCost(player, teamData, offer);
            boolean hasResources = hasCurrency(player, offer.getCurrency(), resolvedCost);
            boolean upgradeOwned = isTeamUpgradeOffer(offer) && isTeamUpgradeOwned(teamData, offer);
            boolean personalOwned = isPersonalPermanentOffer(offer) && isPersonalPermanentOwned(player, offer);
            boolean upgradeLocked = offer.getShopType().equalsIgnoreCase("upgrade")
                    && resolveUpgradeViewState(player, teamData, offer).status == UpgradeViewStatus.LOCKED;

            // Status-basierte Farben: Grün (gekauft), Gelb (verfügbar), Grau (gesperrt)
            String statusColor;
            if (upgradeOwned || personalOwned) {
                statusColor = "&a"; // Grün = gekauft
                itemMeta.addEnchant(Enchantment.PROTECTION, 1, true);
                itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            } else if (upgradeLocked) {
                statusColor = "&7";
            } else if (hasResources) {
                statusColor = "&e"; // Gelb = verfügbar
            } else {
                statusColor = "&7"; // Grau = gesperrt
            }

            itemMeta.setDisplayName(ColorUtil.colorize(statusColor + offer.getDisplayName()));
            List<String> lore = new ArrayList<>();
            lore.add(buildOfferDescriptionLoreLine(offer));
            lore.add("");
            lore.add(buildLoreFieldLine("Kosten", formatLoreCurrencyValue(offer.getCurrency(), resolvedCost)));
            if (shouldShowOfferUpgradeHint(offer)) {
                lore.add(buildLoreFieldLine("Upgrade", "&b" + getOfferUpgradeHint(offer)));
                String progressLine = getOfferProgressLine(player, teamData, offer);
                if (!progressLine.isBlank()) {
                    lore.add(progressLine);
                }
            }
            String cooldownLine = getAbilityCooldownLine(offer);
            if (!cooldownLine.isBlank()) {
                lore.add(cooldownLine);
            }
            lore.add(buildLoreFieldLine("Status", upgradeLocked ? "&cErst im Item-Shop kaufen" : getOfferStatusLine(hasResources, upgradeOwned, personalOwned)));
            if (!isTeamUpgradeOffer(offer) && !isPersonalPermanentOffer(offer)) {
                lore.add(buildLoreFieldLine("Shift-Klick", "&eMehrfachkauf"));
            }
            itemMeta.setLore(lore);
            itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            itemStack.setItemMeta(itemMeta);
        }
        return itemStack;
    }

    private ItemStack createShopInfoItem(String shopType, String selectedCategoryId, int visibleOffers) {
        Material icon = shopType.equalsIgnoreCase("upgrade") ? Material.NETHER_STAR : Material.EMERALD;
        ItemStack infoItem = new ItemStack(icon);
        ItemMeta meta = infoItem.getItemMeta();
        if (meta != null) {
            String detailLine = shopType.equalsIgnoreCase("upgrade")
                    ? "§8Level · Kosten · Effekt · Nächste Stufe"
                    : "§8Beschreibung · Kosten · Effekt · Cooldown";
            meta.setDisplayName(ColorUtil.colorize("&b&lShop-Übersicht"));
            meta.setLore(List.of(
                    "§7Typ: §f" + (shopType.equalsIgnoreCase("upgrade") ? "Upgrade-Shop" : "Item-Shop"),
                    "§7Kategorie: §f" + resolveCategoryDisplayName(shopType, selectedCategoryId),
                    "§7Sichtbare Angebote: §f" + visibleOffers,
                    detailLine
            ));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            infoItem.setItemMeta(meta);
        }
        return infoItem;
    }

    private ItemStack createShopStatusItem(Player player, TeamData teamData, String shopType, String categoryId) {
        List<String> lore = new ArrayList<>();
        lore.add("&7Eisen: &f" + getCurrencyAmount(player, Material.IRON_INGOT));
        lore.add("&7Gold: &6" + getCurrencyAmount(player, Material.GOLD_INGOT));
        lore.add("&7Diamant: &b" + getCurrencyAmount(player, Material.DIAMOND));
        lore.add("&7Emerald: &a" + getCurrencyAmount(player, Material.EMERALD));

        if (shopType.equalsIgnoreCase("upgrade") && teamData != null) {
            TeamUpgradeState state = teamUpgradeStates.computeIfAbsent(teamData.getId().toLowerCase(), ignored -> new TeamUpgradeState());
            PlayerLoadoutState playerState = playerLoadoutStates.computeIfAbsent(player.getUniqueId(), ignored -> new PlayerLoadoutState());
            lore.add("&8");
            lore.add("&bTeam: &fProt " + Math.max(0, state.protectionLevel)
                    + " &8| &fHaste " + Math.max(0, state.hasteLevel)
                    + " &8| &fForge " + Math.max(0, state.forgeLevel)
                    + " &8| &fTraps " + state.trapQueue.size() + "/" + plugin.getConfigManager().getTrapMaxActiveCount());
            lore.add("&dTrap-Queue: &f" + formatTrapQueue(state));
            lore.add("&6Bogen: &fPower " + state.bowPowerLevel
                    + " &8| &fPunch " + state.bowPunchLevel
                    + " &8| &fFlame " + (state.hasBowFlame ? "✔" : "✘"));
            lore.add("&dEigen: &fSword " + swordTierName(playerState.swordTier)
                    + " &8| &fArmor " + armorTierName(playerState.armorTier)
                    + " &8| &fSkills " + countOwnedAbilities(playerState));
        }

        Material icon = shopType.equalsIgnoreCase("upgrade") ? Material.NETHER_STAR : Material.CHEST;
        return createGuiItem(icon, "&bStatus &7• &f" + resolveCategoryDisplayName(shopType, categoryId), lore);
    }

    private ItemStack createGuiItem(Material material, String displayName) {
        return createGuiItem(material, displayName, List.of());
    }

    private ItemStack createGuiItem(Material material, String displayName, List<String> lore) {
        ItemStack itemStack = new ItemStack(material);
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta != null) {
            itemMeta.setDisplayName(ColorUtil.colorize(displayName == null ? " " : displayName));
            if (lore != null && !lore.isEmpty()) {
                itemMeta.setLore(lore.stream().map(ColorUtil::colorize).toList());
            }
            itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            itemStack.setItemMeta(itemMeta);
        }
        return itemStack;
    }

    private boolean shouldShowOfferUpgradeHint(ShopOfferConfig offer) {
        if (offer == null) {
            return false;
        }
        return switch (offer.getAction().toLowerCase(Locale.ROOT)) {
            case "protection", "sharpness", "haste", "forge", "trap", "alarm-trap", "blind-trap", "knock-trap", "silence-trap",
                 "bow-power", "bow-punch", "bow-flame", "stone-sword", "iron-sword", "pickaxe", "axe", "bow", "iron-armor",
                 "dash", "berserker", "shield", "builder", "tracker" -> true;
            default -> false;
        };
    }

    private boolean hasCurrency(Player player, Material currency, int amount) {
        return getCurrencyAmount(player, currency) >= amount;
    }

    private int getCurrencyAmount(Player player, Material currency) {
        int total = 0;
        for (ItemStack itemStack : player.getInventory().getContents()) {
            if (itemStack == null || itemStack.getType() != currency) {
                continue;
            }
            total += itemStack.getAmount();
        }
        return total;
    }

    private int resolveOfferCost(Player player, TeamData teamData, ShopOfferConfig offer) {
        if (offer == null) {
            return 1;
        }
        if (player != null) {
            PlayerLoadoutState state = playerLoadoutStates.computeIfAbsent(player.getUniqueId(), ignored -> new PlayerLoadoutState());
            switch (offer.getAction().toLowerCase(Locale.ROOT)) {
                case "pickaxe":
                    return switch (Math.max(0, state.pickaxeTier)) {
                        case 0 -> 8;
                        case 1 -> 16;
                        default -> 24;
                    };
                case "axe":
                    return switch (Math.max(0, state.axeTier)) {
                        case 0 -> 6;
                        case 1 -> 12;
                        default -> 20;
                    };
                default:
                    break;
            }
        }
        if (teamData == null || !isTeamUpgradeOffer(offer)) {
            return Math.max(1, offer.getCost());
        }

        TeamUpgradeState state = teamUpgradeStates.computeIfAbsent(teamData.getId().toLowerCase(), ignored -> new TeamUpgradeState());
        return switch (offer.getAction().toLowerCase()) {
            case "protection" -> plugin.getConfigManager().getUpgradeCostForLevel("protection", state.protectionLevel + 1, offer.getCost());
            case "haste" -> plugin.getConfigManager().getUpgradeCostForLevel("haste", state.hasteLevel + 1, offer.getCost());
            case "forge" -> plugin.getConfigManager().getUpgradeCostForLevel("forge", state.forgeLevel + 1, offer.getCost());
            case "bow-power" -> plugin.getConfigManager().getUpgradeCostForLevel("bow-power", state.bowPowerLevel + 1, offer.getCost());
            case "bow-punch" -> plugin.getConfigManager().getUpgradeCostForLevel("bow-punch", state.bowPunchLevel + 1, offer.getCost());
            default -> Math.max(1, offer.getCost());
        };
    }

    private int getMaxPurchasesByInventorySpace(Player player, ShopOfferConfig offer) {
        if (player == null || offer == null) {
            return 0;
        }

        ItemStack template = createOfferResultTemplate(player, offer);
        int perPurchaseAmount = Math.max(1, template.getAmount());
        int stackSize = Math.max(1, template.getMaxStackSize());
        int freeCapacity = 0;
        for (ItemStack slotItem : player.getInventory().getStorageContents()) {
            if (slotItem == null || slotItem.getType() == Material.AIR) {
                freeCapacity += stackSize;
                continue;
            }
            if (slotItem.isSimilar(template) && slotItem.getAmount() < stackSize) {
                freeCapacity += stackSize - slotItem.getAmount();
            }
        }

        if (freeCapacity <= 0) {
            return 0;
        }
        return freeCapacity / perPurchaseAmount;
    }

    private ItemStack createOfferResultTemplate(Player player, ShopOfferConfig offer) {
        TeamData teamData = playerTeams.get(player.getUniqueId());
        PlayerLoadoutState loadoutState = playerLoadoutStates.computeIfAbsent(player.getUniqueId(), ignored -> new PlayerLoadoutState());
        return switch (offer.getAction()) {
            case "wool" -> new ItemStack(teamData == null ? Material.WHITE_WOOL : resolveTeamWool(teamData.getId()), Math.max(1, offer.getAmount()));
            case "oak-planks" -> new ItemStack(Material.OAK_PLANKS, Math.max(1, offer.getAmount()));
            case "glass" -> new ItemStack(Material.GLASS, Math.max(1, offer.getAmount()));
            case "stone-sword" -> new ItemStack(Material.STONE_SWORD, 1);
            case "iron-sword" -> new ItemStack(Material.IRON_SWORD, 1);
            case "pickaxe" -> new ItemStack(resolvePickaxeMaterial(Math.min(3, loadoutState.pickaxeTier + 1)), 1);
            case "axe" -> new ItemStack(resolveAxeMaterial(Math.min(3, loadoutState.axeTier + 1)), 1);
            case "shears" -> new ItemStack(Material.SHEARS, 1);
            case "golden-apple" -> new ItemStack(Material.GOLDEN_APPLE, Math.max(1, offer.getAmount()));
            case "end-stone" -> new ItemStack(Material.END_STONE, Math.max(1, offer.getAmount()));
            case "bow" -> new ItemStack(Material.BOW, 1);
            case "arrows" -> new ItemStack(Material.ARROW, Math.max(1, offer.getAmount()));
            case "fireball" -> new ItemStack(Material.FIRE_CHARGE, Math.max(1, offer.getAmount()));
            case "ender-pearl" -> new ItemStack(Material.ENDER_PEARL, Math.max(1, offer.getAmount()));
            case "tnt", "breach-tnt", "jump-tnt", "cluster-tnt", "sticky-tnt" -> createSpecialTntItem(offer.getAction(), Math.max(1, offer.getAmount()));
            case "bridge-egg" -> new ItemStack(Material.EGG, Math.max(1, offer.getAmount()));
            case "knockback-stick" -> createKnockbackStick();
            case "dash", "berserker", "shield", "builder", "tracker" -> createAbilityItem(offer.getAction());
            case "bow-power" -> {
                ItemStack bow = new ItemStack(Material.BOW);
                bow.addUnsafeEnchantment(Enchantment.POWER, 1);
                yield bow;
            }
            case "bow-punch" -> {
                ItemStack bow = new ItemStack(Material.BOW);
                bow.addUnsafeEnchantment(Enchantment.PUNCH, 1);
                yield bow;
            }
            case "bow-flame" -> {
                ItemStack bow = new ItemStack(Material.BOW);
                bow.addUnsafeEnchantment(Enchantment.FLAME, 1);
                yield bow;
            }
            default -> new ItemStack(offer.getIcon(), Math.max(1, offer.getAmount()));
        };
    }

    private void consumeCurrency(Player player, Material currency, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack itemStack = contents[slot];
            if (itemStack == null || itemStack.getType() != currency) {
                continue;
            }
            int remove = Math.min(remaining, itemStack.getAmount());
            itemStack.setAmount(itemStack.getAmount() - remove);
            if (itemStack.getAmount() <= 0) {
                player.getInventory().setItem(slot, null);
            }
            remaining -= remove;
            if (remaining <= 0) {
                return;
            }
        }
    }

    private void applyOffer(Player player, ShopOfferConfig offer) {
        TeamData teamData = playerTeams.get(player.getUniqueId());
        TeamUpgradeState upgradeState = teamData == null ? null : teamUpgradeStates.computeIfAbsent(teamData.getId().toLowerCase(), ignored -> new TeamUpgradeState());
        PlayerLoadoutState loadoutState = playerLoadoutStates.computeIfAbsent(player.getUniqueId(), ignored -> new PlayerLoadoutState());
        switch (offer.getAction()) {
            case "wool" -> giveItem(player, new ItemStack(teamData == null ? Material.WHITE_WOOL : resolveTeamWool(teamData.getId()), offer.getAmount()));
            case "oak-planks" -> giveItem(player, new ItemStack(Material.OAK_PLANKS, offer.getAmount()));
            case "glass" -> giveItem(player, new ItemStack(Material.GLASS, offer.getAmount()));
            case "stone-sword" -> {
                loadoutState.swordTier = Math.max(loadoutState.swordTier, 1);
                applySwordUpgrade(player, loadoutState, upgradeState);
            }
            case "iron-sword" -> {
                loadoutState.swordTier = Math.max(loadoutState.swordTier, 2);
                applySwordUpgrade(player, loadoutState, upgradeState);
            }
            case "pickaxe" -> {
                loadoutState.pickaxeTier = Math.min(3, loadoutState.pickaxeTier + 1);
                applyPickaxeUpgrade(player, loadoutState);
            }
            case "axe" -> {
                loadoutState.axeTier = Math.min(3, loadoutState.axeTier + 1);
                applyAxeUpgrade(player, loadoutState);
            }
            case "shears" -> {
                loadoutState.hasShears = true;
                ensureItemPresent(player, Material.SHEARS, new ItemStack(Material.SHEARS));
            }
            case "golden-apple" -> giveItem(player, new ItemStack(Material.GOLDEN_APPLE, offer.getAmount()));
            case "end-stone" -> giveItem(player, new ItemStack(Material.END_STONE, offer.getAmount()));
            case "bow" -> {
                loadoutState.hasBow = true;
                if (!player.getInventory().contains(Material.BOW)) {
                    giveItem(player, new ItemStack(Material.BOW));
                }
                if (upgradeState != null) {
                    applyBowUpgrades(player, upgradeState);
                }
            }
            case "arrows" -> giveItem(player, new ItemStack(Material.ARROW, offer.getAmount()));
            case "fireball" -> giveItem(player, new ItemStack(Material.FIRE_CHARGE, offer.getAmount()));
            case "iron-armor" -> {
                loadoutState.armorTier = Math.max(loadoutState.armorTier, 1);
                if (teamData != null) {
                    applyArmorUpgrade(player, teamData, loadoutState, upgradeState);
                }
            }
            case "diamond-armor" -> {
                loadoutState.armorTier = Math.max(loadoutState.armorTier, 2);
                if (teamData != null) {
                    applyArmorUpgrade(player, teamData, loadoutState, upgradeState);
                }
            }
            case "diamond-sword" -> {
                loadoutState.swordTier = Math.max(loadoutState.swordTier, 3);
                applySwordUpgrade(player, loadoutState, upgradeState);
            }
            case "ender-pearl" -> giveItem(player, new ItemStack(Material.ENDER_PEARL));
            case "tnt", "breach-tnt", "jump-tnt", "cluster-tnt", "sticky-tnt" ->
                    giveItem(player, createSpecialTntItem(offer.getAction(), offer.getAmount()));
            case "bridge-egg" -> giveItem(player, new ItemStack(Material.EGG, offer.getAmount()));
            case "knockback-stick" -> giveItem(player, createKnockbackStick());
            case "dash" -> {
                if (offer.getShopType().equalsIgnoreCase("upgrade")) {
                    if (loadoutState.hasDashAbility && loadoutState.dashLevel >= 1) {
                        loadoutState.dashLevel = 2;
                    }
                } else {
                    loadoutState.hasDashAbility = true;
                    loadoutState.dashLevel = Math.max(1, loadoutState.dashLevel);
                    ensureAbilityItem(player, "dash");
                }
            }
            case "berserker" -> {
                if (offer.getShopType().equalsIgnoreCase("upgrade")) {
                    if (loadoutState.hasBerserkerAbility && loadoutState.berserkerLevel >= 1) {
                        loadoutState.berserkerLevel = 2;
                    }
                } else {
                    loadoutState.hasBerserkerAbility = true;
                    loadoutState.berserkerLevel = Math.max(1, loadoutState.berserkerLevel);
                    ensureAbilityItem(player, "berserker");
                }
            }
            case "shield" -> {
                if (offer.getShopType().equalsIgnoreCase("upgrade")) {
                    if (loadoutState.hasShieldAbility && loadoutState.shieldLevel >= 1) {
                        loadoutState.shieldLevel = 2;
                    }
                } else {
                    loadoutState.hasShieldAbility = true;
                    loadoutState.shieldLevel = Math.max(1, loadoutState.shieldLevel);
                    ensureAbilityItem(player, "shield");
                }
            }
            case "builder" -> {
                if (offer.getShopType().equalsIgnoreCase("upgrade")) {
                    if (loadoutState.hasBuilderAbility && loadoutState.builderLevel >= 1) {
                        loadoutState.builderLevel = 2;
                    }
                } else {
                    loadoutState.hasBuilderAbility = true;
                    loadoutState.builderLevel = Math.max(1, loadoutState.builderLevel);
                    ensureAbilityItem(player, "builder");
                }
            }
            case "tracker" -> {
                if (offer.getShopType().equalsIgnoreCase("upgrade")) {
                    if (loadoutState.hasTrackerAbility && loadoutState.trackerLevel >= 1) {
                        loadoutState.trackerLevel = 2;
                    }
                } else {
                    loadoutState.hasTrackerAbility = true;
                    loadoutState.trackerLevel = Math.max(1, loadoutState.trackerLevel);
                    ensureAbilityItem(player, "tracker");
                }
            }
            case "void-rescue" -> loadoutState.hasVoidRescueUpgrade = true;
            // Bogen-Team-Upgrades werden über unlockTeamUpgrade + applyTeamUpgrades abgewickelt,
            // aber falls direkt aufgerufen (z.B. bei respawn-apply):
            case "bow-power", "bow-punch", "bow-flame" -> {
                if (upgradeState != null) {
                    applyBowUpgrades(player, upgradeState);
                }
            }
            default -> giveItem(player, new ItemStack(offer.getIcon(), Math.max(1, offer.getAmount())));
        }
    }

    private boolean isTeamUpgradeOffer(ShopOfferConfig offer) {
        if (offer == null || !offer.getShopType().equalsIgnoreCase("upgrade")) {
            return false;
        }
        return switch (offer.getAction().toLowerCase()) {
            case "protection", "sharpness", "haste", "forge", "trap", "alarm-trap", "blind-trap", "knock-trap", "silence-trap",
                 "bow-power", "bow-punch", "bow-flame" -> true;
            default -> false;
        };
    }

    private boolean isTrapOffer(ShopOfferConfig offer) {
        return offer != null && resolveTrapType(offer.getAction()) != null;
    }

    private boolean isAbilityUpgradeOffer(ShopOfferConfig offer) {
        if (offer == null || !offer.getShopType().equalsIgnoreCase("upgrade")) {
            return false;
        }
        return switch (offer.getAction().toLowerCase(Locale.ROOT)) {
            case "dash", "berserker", "shield", "builder", "tracker" -> true;
            default -> false;
        };
    }

    private boolean hasPurchasedAbilityItem(Player player, ShopOfferConfig offer) {
        if (player == null || offer == null) {
            return false;
        }
        PlayerLoadoutState loadoutState = playerLoadoutStates.computeIfAbsent(player.getUniqueId(), ignored -> new PlayerLoadoutState());
        return switch (offer.getAction().toLowerCase(Locale.ROOT)) {
            case "dash" -> loadoutState.hasDashAbility;
            case "berserker" -> loadoutState.hasBerserkerAbility;
            case "shield" -> loadoutState.hasShieldAbility;
            case "builder" -> loadoutState.hasBuilderAbility;
            case "tracker" -> loadoutState.hasTrackerAbility;
            default -> false;
        };
    }

    private boolean isTeamUpgradeOwned(TeamData teamData, ShopOfferConfig offer) {
        if (teamData == null || offer == null) {
            return false;
        }
        TeamUpgradeState state = teamUpgradeStates.computeIfAbsent(teamData.getId().toLowerCase(), ignored -> new TeamUpgradeState());
        return switch (offer.getAction().toLowerCase()) {
            case "protection" -> state.protectionLevel >= plugin.getConfigManager().getUpgradeMaxLevel("protection", 4);
            case "sharpness" -> state.hasSharpness;
            case "haste" -> state.hasteLevel >= plugin.getConfigManager().getUpgradeMaxLevel("haste", 2);
            case "forge" -> state.forgeLevel >= plugin.getConfigManager().getUpgradeMaxLevel("forge", 3);
            case "trap", "alarm-trap", "blind-trap", "knock-trap", "silence-trap" -> {
                TrapType trapType = resolveTrapType(offer.getAction());
                yield trapType != null && state.trapQueue.contains(trapType);
            }
            case "bow-power" -> state.bowPowerLevel >= 3;
            case "bow-punch" -> state.bowPunchLevel >= 2;
            case "bow-flame" -> state.hasBowFlame;
            default -> false;
        };
    }

    private void unlockTeamUpgrade(TeamData teamData, ShopOfferConfig offer) {
        TeamUpgradeState state = teamUpgradeStates.computeIfAbsent(teamData.getId().toLowerCase(), ignored -> new TeamUpgradeState());
        TrapType trapType = resolveTrapType(offer.getAction());
        if (trapType != null) {
            if (!state.trapQueue.contains(trapType) && !isTrapQueueFull(state)) {
                state.trapQueue.add(trapType);
            }
            return;
        }
        switch (offer.getAction().toLowerCase()) {
            case "protection" -> state.protectionLevel = Math.min(plugin.getConfigManager().getUpgradeMaxLevel("protection", 4), state.protectionLevel + 1);
            case "sharpness" -> state.hasSharpness = true;
            case "haste" -> state.hasteLevel = Math.min(plugin.getConfigManager().getUpgradeMaxLevel("haste", 2), state.hasteLevel + 1);
            case "forge" -> state.forgeLevel = Math.min(plugin.getConfigManager().getUpgradeMaxLevel("forge", 3), state.forgeLevel + 1);
            case "bow-power" -> state.bowPowerLevel = Math.min(3, state.bowPowerLevel + 1);
            case "bow-punch" -> state.bowPunchLevel = Math.min(2, state.bowPunchLevel + 1);
            case "bow-flame" -> state.hasBowFlame = true;
            default -> {
            }
        }
    }

    private boolean isTrapQueueFull(TeamUpgradeState state) {
        return state != null && state.trapQueue.size() >= plugin.getConfigManager().getTrapMaxActiveCount();
    }

    private TrapType resolveTrapType(String action) {
        if (action == null || action.isBlank()) {
            return null;
        }
        return switch (action.toLowerCase(Locale.ROOT)) {
            case "trap", "alarm-trap" -> TrapType.ALARM;
            case "blind-trap" -> TrapType.BLIND;
            case "knock-trap" -> TrapType.KNOCK;
            case "silence-trap" -> TrapType.SILENCE;
            default -> null;
        };
    }

    private String formatTrapQueue(TeamUpgradeState state) {
        if (state == null || state.trapQueue.isEmpty()) {
            return "Keine";
        }
        List<String> names = new ArrayList<>();
        for (TrapType trapType : state.trapQueue) {
            names.add(trapType.getDisplayName());
        }
        return String.join(", ", names);
    }

    private void applyTeamUpgrades(Player player, TeamData teamData) {
        if (player == null || teamData == null) {
            return;
        }

        TeamUpgradeState state = teamUpgradeStates.computeIfAbsent(teamData.getId().toLowerCase(), ignored -> new TeamUpgradeState());
        applyProtectionEnchant(player, state.protectionLevel);
        if (state.hasSharpness) {
            applySharpnessToInventorySwords(player, 1);
        }
        applyHasteUpgrade(player, state);
        applyBowUpgrades(player, state);
    }

    private void applyArmorUpgrade(Player player, TeamData teamData, PlayerLoadoutState loadoutState, TeamUpgradeState state) {
        Color teamColor = resolveTeamColor(teamData.getId());
        if (loadoutState.armorTier >= 2) {
            player.getInventory().setHelmet(coloredArmor(Material.LEATHER_HELMET, teamColor));
            player.getInventory().setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
            player.getInventory().setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
            player.getInventory().setBoots(new ItemStack(Material.DIAMOND_BOOTS));
            applyProtectionEnchant(player, state.protectionLevel);
            return;
        }
        if (loadoutState.armorTier >= 1) {
            player.getInventory().setHelmet(coloredArmor(Material.LEATHER_HELMET, teamColor));
            player.getInventory().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
            player.getInventory().setLeggings(new ItemStack(Material.IRON_LEGGINGS));
            player.getInventory().setBoots(new ItemStack(Material.IRON_BOOTS));
            applyProtectionEnchant(player, state.protectionLevel);
            return;
        }
        player.getInventory().setHelmet(coloredArmor(Material.LEATHER_HELMET, teamColor));
        player.getInventory().setChestplate(coloredArmor(Material.LEATHER_CHESTPLATE, teamColor));
        player.getInventory().setLeggings(coloredArmor(Material.LEATHER_LEGGINGS, teamColor));
        player.getInventory().setBoots(coloredArmor(Material.LEATHER_BOOTS, teamColor));
        applyProtectionEnchant(player, state.protectionLevel);
    }

    private void applySwordUpgrade(Player player, PlayerLoadoutState loadoutState, TeamUpgradeState state) {
        removeMaterial(player, Material.WOODEN_SWORD);
        removeMaterial(player, Material.STONE_SWORD);
        removeMaterial(player, Material.IRON_SWORD);
        removeMaterial(player, Material.GOLDEN_SWORD);
        removeMaterial(player, Material.DIAMOND_SWORD);
        giveItem(player, new ItemStack(resolveSwordMaterial(loadoutState.swordTier)));
        if (state.hasSharpness) {
            applySharpnessToInventorySwords(player, 1);
        }
    }

    private void applyHasteUpgrade(Player player, TeamUpgradeState state) {
        player.removePotionEffect(PotionEffectType.HASTE);
        if (state.hasteLevel <= 0) {
            return;
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, Integer.MAX_VALUE, Math.max(0, state.hasteLevel - 1), true, false, false));
    }

    private void applyBowUpgrades(Player player, TeamUpgradeState state) {
        ItemStack[] contents = player.getInventory().getContents();
        for (ItemStack item : contents) {
            if (item == null || item.getType() != Material.BOW) continue;
            if (state.bowPowerLevel > 0) {
                item.addUnsafeEnchantment(Enchantment.POWER, state.bowPowerLevel);
            }
            if (state.bowPunchLevel > 0) {
                item.addUnsafeEnchantment(Enchantment.PUNCH, state.bowPunchLevel);
            }
            if (state.hasBowFlame) {
                item.addUnsafeEnchantment(Enchantment.FLAME, 1);
            }
        }
    }

    private void applyPersonalLoadout(Player player, TeamData teamData) {
        if (player == null || teamData == null) {
            return;
        }
        PlayerLoadoutState loadoutState = playerLoadoutStates.computeIfAbsent(player.getUniqueId(), ignored -> new PlayerLoadoutState());
        TeamUpgradeState teamState = teamUpgradeStates.computeIfAbsent(teamData.getId().toLowerCase(), ignored -> new TeamUpgradeState());

        if (plugin.getConfigManager().shouldRestorePurchasedArmorOnRespawn()) {
            applyArmorUpgrade(player, teamData, loadoutState, teamState);
        }
        if (plugin.getConfigManager().shouldRestorePurchasedSwordsOnRespawn()) {
            applySwordUpgrade(player, loadoutState, teamState);
        } else if (teamState.hasSharpness) {
            applySharpnessToInventorySwords(player, 1);
        }
        if (plugin.getConfigManager().shouldRestorePurchasedToolsOnRespawn()) {
            if (loadoutState.pickaxeTier > 0) {
                applyPickaxeUpgrade(player, loadoutState);
            }
            if (loadoutState.axeTier > 0) {
                applyAxeUpgrade(player, loadoutState);
            }
            if (loadoutState.hasShears && !player.getInventory().contains(Material.SHEARS)) {
                giveItem(player, new ItemStack(Material.SHEARS));
            }
        }
        if (plugin.getConfigManager().shouldRestorePurchasedBowOnRespawn() && loadoutState.hasBow
                && !player.getInventory().contains(Material.BOW)) {
            giveItem(player, new ItemStack(Material.BOW));
        }
        if (loadoutState.hasBow) {
            applyBowUpgrades(player, teamState);
        }
    }

    private boolean isPersonalPermanentOffer(ShopOfferConfig offer) {
        if (offer == null) {
            return false;
        }
        return switch (offer.getAction().toLowerCase()) {
            case "stone-sword", "iron-sword", "diamond-sword", "iron-armor", "diamond-armor", "pickaxe", "axe",
                 "shears", "bow", "dash", "berserker", "shield", "builder", "tracker", "void-rescue" -> true;
            default -> false;
        };
    }

    private boolean isPersonalPermanentOwned(Player player, ShopOfferConfig offer) {
        if (player == null || offer == null) {
            return false;
        }
        PlayerLoadoutState loadoutState = playerLoadoutStates.computeIfAbsent(player.getUniqueId(), ignored -> new PlayerLoadoutState());
        boolean upgradeShopOffer = offer.getShopType().equalsIgnoreCase("upgrade");
        return switch (offer.getAction().toLowerCase()) {
            case "stone-sword" -> loadoutState.swordTier >= 1;
            case "iron-sword" -> loadoutState.swordTier >= 2;
            case "diamond-sword" -> loadoutState.swordTier >= 3;
            case "iron-armor" -> loadoutState.armorTier >= 1;
            case "diamond-armor" -> loadoutState.armorTier >= 2;
            case "pickaxe" -> loadoutState.pickaxeTier >= 3;
            case "axe" -> loadoutState.axeTier >= 3;
            case "shears" -> loadoutState.hasShears;
            case "bow" -> loadoutState.hasBow;
            case "dash" -> upgradeShopOffer ? loadoutState.dashLevel >= 2 : loadoutState.hasDashAbility;
            case "berserker" -> upgradeShopOffer ? loadoutState.berserkerLevel >= 2 : loadoutState.hasBerserkerAbility;
            case "shield" -> upgradeShopOffer ? loadoutState.shieldLevel >= 2 : loadoutState.hasShieldAbility;
            case "builder" -> upgradeShopOffer ? loadoutState.builderLevel >= 2 : loadoutState.hasBuilderAbility;
            case "tracker" -> upgradeShopOffer ? loadoutState.trackerLevel >= 2 : loadoutState.hasTrackerAbility;
            case "void-rescue" -> loadoutState.hasVoidRescueUpgrade;
            default -> false;
        };
    }

    private Material resolveSwordMaterial(int swordTier) {
        return switch (Math.max(0, swordTier)) {
            case 1 -> Material.STONE_SWORD;
            case 2 -> Material.IRON_SWORD;
            case 3 -> Material.DIAMOND_SWORD;
            default -> Material.WOODEN_SWORD;
        };
    }

    private Material resolvePickaxeMaterial(int tier) {
        return switch (Math.max(1, Math.min(3, tier))) {
            case 1 -> Material.WOODEN_PICKAXE;
            case 2 -> Material.STONE_PICKAXE;
            default -> Material.IRON_PICKAXE;
        };
    }

    private Material resolveAxeMaterial(int tier) {
        return switch (Math.max(1, Math.min(3, tier))) {
            case 1 -> Material.WOODEN_AXE;
            case 2 -> Material.STONE_AXE;
            default -> Material.IRON_AXE;
        };
    }

    private void applyPickaxeUpgrade(Player player, PlayerLoadoutState loadoutState) {
        removeMaterial(player, Material.WOODEN_PICKAXE);
        removeMaterial(player, Material.STONE_PICKAXE);
        removeMaterial(player, Material.IRON_PICKAXE);
        giveItem(player, new ItemStack(resolvePickaxeMaterial(loadoutState.pickaxeTier)));
    }

    private void applyAxeUpgrade(Player player, PlayerLoadoutState loadoutState) {
        removeMaterial(player, Material.WOODEN_AXE);
        removeMaterial(player, Material.STONE_AXE);
        removeMaterial(player, Material.IRON_AXE);
        giveItem(player, new ItemStack(resolveAxeMaterial(loadoutState.axeTier)));
    }


    private void ensureAbilityItem(Player player, String action) {
        ItemStack abilityItem = createAbilityItem(action);
        if (!hasDisplayItem(player, abilityItem)) {
            giveItem(player, abilityItem);
        }
    }

    private boolean hasDisplayItem(Player player, ItemStack reference) {
        if (player == null || reference == null || !reference.hasItemMeta() || reference.getItemMeta() == null) {
            return false;
        }
        String displayName = reference.getItemMeta().getDisplayName();
        for (ItemStack itemStack : player.getInventory().getContents()) {
            if (itemStack == null || itemStack.getType() != reference.getType() || !itemStack.hasItemMeta() || itemStack.getItemMeta() == null) {
                continue;
            }
            if (displayName.equals(itemStack.getItemMeta().getDisplayName())) {
                return true;
            }
        }
        return false;
    }

    private void ensureItemPresent(Player player, Material material, ItemStack itemStack) {
        if (!player.getInventory().contains(material)) {
            giveItem(player, itemStack);
        }
    }

    private void placeTrackedBlock(Block block, Material material) {
        if (block == null || isProtectedLocation(block.getLocation())) return;
        String key = getBlockKey(block);
        modifiedBlocks.putIfAbsent(key, StoredBlockState.capture(block));
        placedBlockKeys.add(key);
        block.setType(material, false);
    }

    private boolean isReplaceableForBridge(Block block) {
        if (block == null) {
            return false;
        }
        Material type = block.getType();
        return type == Material.AIR || type == Material.CAVE_AIR || type.name().equals("VOID_AIR");
    }

    private ItemStack createKnockbackStick() {
        ItemStack itemStack = new ItemStack(Material.STICK);
        itemStack.addUnsafeEnchantment(Enchantment.KNOCKBACK, 2);
        return itemStack;
    }


    private ItemStack createSpecialTntItem(String action, int amount) {
        String normalizedAction = action == null || action.isBlank() ? "tnt" : action.toLowerCase(Locale.ROOT);
        String displayName = switch (normalizedAction) {
            case "breach-tnt" -> "&6Breach TNT";
            case "jump-tnt" -> "&bJump TNT";
            case "cluster-tnt" -> "&dCluster TNT";
            case "sticky-tnt" -> "&eSticky TNT";
            default -> "&cTNT";
        };
        List<String> lore = switch (normalizedAction) {
            case "breach-tnt" -> List.of(
                    "&7Mehr Blockdruck gegen platzierte Defense",
                    "&7Weniger Spielerschaden als Standard-TNT"
            );
            case "jump-tnt" -> List.of(
                    "&7Weniger Zerstörung, mehr Boost für dich",
                    "&7Ideal für TNT-Jumps und Clip-Plays"
            );
            case "cluster-tnt" -> List.of(
                    "&7Splittert vor der Explosion in 3 TNTs",
                    "&7Wirkt trotzdem nur auf platzierte Blöcke"
            );
            case "sticky-tnt" -> List.of(
                    "&7Klebt an Wänden und Decken fest",
                    "&7Perfekt unter oder neben der Defense"
            );
            default -> List.of(
                    "&7Klassisches Bedwars-TNT",
                    "&7Zerstört nur platzierte Blöcke"
            );
        };

        ItemStack itemStack = createGuiItem(Material.TNT, displayName, lore);
        itemStack.setAmount(Math.max(1, amount));
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta != null) {
            itemMeta.getPersistentDataContainer().set(tntVariantKey, PersistentDataType.STRING, normalizedAction);
            itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            itemStack.setItemMeta(itemMeta);
        }
        return itemStack;
    }

    private String getPrimedTntAction(TNTPrimed primedTnt) {
        if (primedTnt == null) {
            return null;
        }
        String stored = primedTnt.getPersistentDataContainer().get(tntVariantKey, PersistentDataType.STRING);
        return stored == null || stored.isBlank() ? null : stored.toLowerCase(Locale.ROOT);
    }

    private int getPrimedTntClusterGeneration(TNTPrimed primedTnt) {
        if (primedTnt == null) {
            return 0;
        }
        Integer generation = primedTnt.getPersistentDataContainer().get(tntClusterGenerationKey, PersistentDataType.INTEGER);
        return generation == null ? 0 : generation;
    }

    private Player resolvePrimedTntOwner(TNTPrimed primedTnt) {
        if (primedTnt == null) {
            return null;
        }
        String rawOwner = primedTnt.getPersistentDataContainer().get(tntOwnerKey, PersistentDataType.STRING);
        if (rawOwner != null && !rawOwner.isBlank()) {
            try {
                return Bukkit.getPlayer(UUID.fromString(rawOwner));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return primedTnt.getSource() instanceof Player player ? player : null;
    }

    private void applyJumpTntBoost(TNTPrimed primedTnt, Location origin) {
        Player owner = resolvePrimedTntOwner(primedTnt);
        if (owner == null || !owner.isOnline() || !containsPlayer(owner.getUniqueId()) || origin == null || origin.getWorld() == null
                || owner.getWorld() != origin.getWorld()) {
            return;
        }
        if (owner.getLocation().distanceSquared(origin) > 49.0D) {
            return;
        }

        Vector boost = owner.getLocation().toVector().subtract(origin.toVector()).setY(0.0D);
        if (boost.lengthSquared() < 0.01D) {
            boost = owner.getLocation().getDirection().clone().setY(0.0D);
        }
        if (boost.lengthSquared() < 0.01D) {
            boost = new Vector(1.0D, 0.0D, 0.0D);
        }
        boost.normalize().multiply(plugin.getConfigManager().getTntOwnerBoostHorizontal("jump-tnt"));
        boost.setY(plugin.getConfigManager().getTntOwnerBoostVertical("jump-tnt"));
        owner.setVelocity(boost);
        owner.setFallDistance(0.0F);
        sendActionbar(owner, "&bJump TNT &7Boost!");
    }

    private void spawnClusterChildren(Location origin, Player owner, String action) {
        if (origin == null || origin.getWorld() == null) {
            return;
        }
        World world = origin.getWorld();
        int childCount = plugin.getConfigManager().getTntChildCount(action);
        int childFuseTicks = plugin.getConfigManager().getTntChildFuseTicks(action);
        double childSpread = plugin.getConfigManager().getTntChildSpread(action);
        List<Vector> baseDirections = List.of(
                new Vector(1.0D, 0.0D, 0.0D),
                new Vector(-0.7D, 0.0D, 0.7D),
                new Vector(-0.7D, 0.0D, -0.7D),
                new Vector(0.0D, 0.0D, 1.0D)
        );

        for (int index = 0; index < childCount; index++) {
            Vector direction = baseDirections.get(index % baseDirections.size()).clone().normalize();
            Location spawnLocation = origin.clone().add(direction.clone().multiply(Math.max(0.15D, childSpread * 0.35D))).add(0.0D, 0.15D, 0.0D);
            world.spawn(spawnLocation, TNTPrimed.class, child -> {
                configurePrimedTnt(child, owner, action, 1);
                child.setFuseTicks(childFuseTicks);
                child.setYield(Math.max(0.8F, plugin.getConfigManager().getTntYield(action) * 0.82F));
                child.setVelocity(direction.multiply(Math.max(0.12D, childSpread * 0.18D)).setY(0.22D));
            });
        }
        world.playSound(origin, Sound.ENTITY_TNT_PRIMED, 1.0F, 1.35F);
    }

    private void addNearbyPlacedBlocks(Location origin, List<Block> blocks, double radius) {
        if (origin == null || origin.getWorld() == null || radius <= 0.0D) {
            return;
        }
        Set<String> knownBlocks = new HashSet<>();
        for (Block block : blocks) {
            knownBlocks.add(getBlockKey(block));
        }

        int ceil = (int) Math.ceil(radius);
        double radiusSquared = radius * radius;
        World world = origin.getWorld();
        for (int x = -ceil; x <= ceil; x++) {
            for (int y = -ceil; y <= ceil; y++) {
                for (int z = -ceil; z <= ceil; z++) {
                    Block block = world.getBlockAt(origin.getBlockX() + x, origin.getBlockY() + y, origin.getBlockZ() + z);
                    if (block.getLocation().add(0.5D, 0.5D, 0.5D).distanceSquared(origin) > radiusSquared) {
                        continue;
                    }
                    String key = getBlockKey(block);
                    if (placedBlockKeys.contains(key) && knownBlocks.add(key)) {
                        blocks.add(block);
                    }
                }
            }
        }
    }

    private void filterExplosionBlocks(List<Block> blocks) {
        blocks.removeIf(block -> {
            String key = getBlockKey(block);
            if (placedBlockKeys.contains(key) && !isExplosionProofPlacedBlock(block)) {
                placedBlockKeys.remove(key);
                modifiedBlocks.remove(key);
                return false;
            }
            return true;
        });
    }

    private List<Block> collectBreakableExplosionBlocks(Location origin, double radius) {
        List<Block> candidates = new ArrayList<>();
        addNearbyPlacedBlocks(origin, candidates, radius);

        List<Block> breakableBlocks = new ArrayList<>();
        Set<String> processed = new HashSet<>();
        for (Block block : candidates) {
            String key = getBlockKey(block);
            if (!processed.add(key) || !placedBlockKeys.contains(key) || isExplosionProofPlacedBlock(block)) {
                continue;
            }
            placedBlockKeys.remove(key);
            modifiedBlocks.remove(key);
            breakableBlocks.add(block);
        }
        return breakableBlocks;
    }

    private void destroyBlocks(List<Block> blocks) {
        for (Block block : blocks) {
            if (block != null) {
                block.setType(Material.AIR, false);
            }
        }
    }

    private boolean isExplosionProofPlacedBlock(Block block) {
        if (block == null) {
            return false;
        }
        return block.getType() == Material.GLASS;
    }

    private ItemStack createAbilityItem(String action) {
        return switch (action.toLowerCase(Locale.ROOT)) {
            case "dash" -> createGuiItem(Material.FEATHER, "&bDash", List.of("&7Rechtsklick für einen kurzen Schub", "&8Stufe I: &b6s Speed II", "&8Stufe II: &b9s Speed III", "&8Cooldown: &b20s"));
            case "berserker" -> createGuiItem(Material.BLAZE_POWDER, "&cBerserker", List.of("&7Rechtsklick für Schaden + Tempo", "&8Stufe I: &c10s Stärke I", "&8Stufe II: &c13s Stärke II + Speed II", "&8Cooldown: &c45s"));
            case "shield" -> createGuiItem(Material.NETHER_STAR, "&bShield", List.of("&7Rechtsklick für Schutz", "&8Stufe I: &b10s Resistenz I", "&8Stufe II: &b13s Resistenz II", "&8Cooldown: &b35s"));
            case "builder" -> createGuiItem(Material.BRICK, "&6Builder", List.of("&7Rechtsklick für Haste + Speed", "&8Stufe I: &610s Haste II", "&8Stufe II: &616s Haste III", "&8Cooldown: &625s"));
            case "tracker" -> createGuiItem(Material.COMPASS, "&aTracker", List.of("&7Rechtsklick markiert den nächsten Gegner", "&8Stufe II: &a2s Glowing auf dem Ziel", "&8Cooldown: &a8s"));
            default -> createGuiItem(Material.NETHER_STAR, "&bAbility");
        };
    }

    private boolean matchesAbilityItem(ItemStack itemStack, Material type, String displayName) {
        if (itemStack.getType() != type || !itemStack.hasItemMeta() || itemStack.getItemMeta() == null) {
            return false;
        }
        return ColorUtil.colorize(displayName).equals(itemStack.getItemMeta().getDisplayName());
    }

    private boolean activateAbility(Player player, PlayerLoadoutState state, String key, long cooldownMillis, Material cooldownMaterial,
                                    Runnable effect, String cooldownMessage) {
        long now = System.currentTimeMillis();
        long nextAllowed = state.abilityCooldowns.getOrDefault(key, 0L);
        if (nextAllowed > now) {
            long remaining = Math.max(1L, (nextAllowed - now + 999L) / 1000L);
            player.sendMessage(ColorUtil.colorize("&8[&bBedWars&8] " + cooldownMessage + " &7(" + remaining + "s)"));
            return true;
        }
        state.abilityCooldowns.put(key, now + cooldownMillis);
        player.setCooldown(cooldownMaterial, Math.max(1, (int) (cooldownMillis / 50L)));
        effect.run();
        return true;
    }

    private Player findNearestEnemy(Player player) {
        TeamData ownTeam = playerTeams.get(player.getUniqueId());
        Player nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (Map.Entry<UUID, TeamData> entry : playerTeams.entrySet()) {
            Player candidate = Bukkit.getPlayer(entry.getKey());
            if (candidate == null || !candidate.isOnline() || candidate.equals(player) || isSpectator(candidate) || isRespawning(candidate)) {
                continue;
            }
            if (ownTeam != null && entry.getValue() != null && ownTeam.getId().equalsIgnoreCase(entry.getValue().getId())) {
                continue;
            }
            if (!candidate.getWorld().equals(player.getWorld())) {
                continue;
            }
            double distance = candidate.getLocation().distanceSquared(player.getLocation());
            if (distance < nearestDistance) {
                nearest = candidate;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private String resolveCategoryDisplayName(String shopType, String categoryId) {
        return getCachedShopCategories(shopType).stream()
                .filter(category -> category.getId().equalsIgnoreCase(categoryId))
                .map(category -> ChatColor.stripColor(ColorUtil.colorize(category.getDisplayName())))
                .findFirst()
                .orElse(categoryId);
    }

    private String swordTierName(int swordTier) {
        return switch (Math.max(0, swordTier)) {
            case 1 -> "Stein";
            case 2 -> "Eisen";
            case 3 -> "Diamant";
            default -> "Holz";
        };
    }

    private String armorTierName(int armorTier) {
        return switch (Math.max(0, armorTier)) {
            case 1 -> "Eisen";
            case 2 -> "Diamant";
            default -> "Basis";
        };
    }

    private int countOwnedAbilities(PlayerLoadoutState state) {
        int count = 0;
        if (state.hasDashAbility) count++;
        if (state.hasBerserkerAbility) count++;
        if (state.hasShieldAbility) count++;
        if (state.hasBuilderAbility) count++;
        if (state.hasTrackerAbility) count++;
        return count;
    }

    private String formatCurrency(Material currency, int cost) {
        return switch (currency) {
            case IRON_INGOT -> "§f" + cost + " Eisen";
            case GOLD_INGOT -> "§6" + cost + " Gold";
            case DIAMOND -> "§b" + cost + " Diamant";
            case EMERALD -> "§a" + cost + " Emerald";
            default -> "§f" + cost + " " + prettifyMaterial(currency);
        };
    }

    private String getOfferDescription(ShopOfferConfig offer) {
        return switch (offer.getAction().toLowerCase(Locale.ROOT)) {
            case "protection" -> "Reduziert eingehenden Schaden für das gesamte Team.";
            case "sharpness" -> "Erhöht den Schwertschaden aller Teammitglieder.";
            case "haste" -> "Beschleunigt Abbau- und Rush-Aktionen im Team.";
            case "forge" -> "Verbessert Eisen- und Goldproduktion an eurer Base.";
            case "trap", "alarm-trap" -> "Warnt das Team sofort, zeigt Titel an und markiert den Eindringling.";
            case "blind-trap" -> "Blendet und verlangsamt Eindringlinge für vier Sekunden.";
            case "knock-trap" -> "Schleudert Eindringlinge aus dem Bettbereich heraus.";
            case "silence-trap" -> "Blockiert Pearl, TNT und Fireball des Eindringlings für fünf Sekunden.";
            case "iron-armor" -> "Dauerhafte Rüstung für defensive Midgame-Kämpfe.";
            case "diamond-armor" -> "Spätes Luxus-Upgrade für hohe Überlebenschancen.";
            case "diamond-sword" -> "Maximales persönliches Schwert-Upgrade für Clutches.";
            case "wool" -> "Schneller Standardblock für Rush und erste Verteidigung.";
            case "oak-planks" -> "Solide Holzschicht gegen frühe Angriffe.";
            case "end-stone" -> "Klassische Midgame-Verteidigung mit hoher Stabilität.";
            case "glass" -> "Saubere Anti-TNT-Außenhülle für euer Bett.";
            case "stone-sword" -> "Günstiges Upgrade für frühe Nahkämpfe.";
            case "iron-sword" -> "Starkes Midgame-Schwert für saubere Trades.";
            case "bow" -> "Fernkampfdruck für Insel- und Bridge-Kontrolle.";
            case "arrows" -> "Munition für konstanten Bogendruck.";
            case "fireball" -> "Sofortiger Explosivdruck mit Knockback-Potenzial.";
            case "pickaxe" -> "Verbessert deine Abbaustufe bis zur Eisenstufe.";
            case "axe" -> "Schneller durch Holz-Defensive und Türen.";
            case "shears" -> "Perfekt gegen Wolle und schnelle Re-Entries.";
            case "golden-apple" -> "Sofortiger Sustain für knappe Kämpfe.";
            case "tnt" -> "Klassischer Bettöffner gegen kompakte Defense.";
            case "breach-tnt" -> "Mehr Druck auf platzierte Bettverteidigung bei reduziertem Spielerschaden.";
            case "jump-tnt" -> "TNT für aggressive Jumps, Height-Gains und Clip-Plays.";
            case "cluster-tnt" -> "Explodiert chaotisch in drei Sub-TNTs nur gegen gesetzte Blöcke.";
            case "sticky-tnt" -> "Klebt an Wänden und Decken für präzise Defense-Öffnungen.";
            case "ender-pearl" -> "Late-Game-Rotate oder riskanter Finisher.";
            case "bridge-egg" -> "Erstellt schnell eine Brücke beim Crossing.";
            case "knockback-stick" -> "Kontrolle auf Brücken und an Kanten.";
            case "dash" -> "Aktive Mobilitäts-Fähigkeit, die auf Stufe II länger und stärker wirkt.";
            case "berserker" -> "Aktiver Kampfboost, der auf Stufe II mehr Stärke und Dauer erhält.";
            case "shield" -> "Aktiver Schutz, der auf Stufe II länger und robuster wird.";
            case "builder" -> "Aktiver Bau-Boost, der auf Stufe II längere Dauer und stärkeres Haste gibt.";
            case "tracker" -> "Zeigt Gegner an und markiert sie auf Stufe II kurz mit Glowing.";
            case "void-rescue" -> "Rettet dich einmal pro Leben vor dem Void und gibt dir einen sicheren Re-Entry.";
            case "bow-power" -> "Erhöht Bogenschaden für das gesamte Team.";
            case "bow-punch" -> "Erhöht Knockback beim Bogen für das Team.";
            case "bow-flame" -> "Alle Team-Bögen setzen Gegner in Brand.";
            default -> "Erweitert dein Loadout für die nächste Matchphase.";
        };
    }

    private String buildOfferDescriptionLoreLine(ShopOfferConfig offer) {
        return ColorUtil.colorize("&8" + getOfferDescription(offer));
    }

    private String buildLoreFieldLine(String label, String value) {
        return ColorUtil.colorize("&7" + label + ": " + value);
    }

    private String formatLoreCurrencyValue(Material currency, int cost) {
        return "&e" + cost + " " + prettifyMaterial(currency);
    }

    private String getOfferUpgradeHint(ShopOfferConfig offer) {
        return switch (offer.getAction().toLowerCase(Locale.ROOT)) {
            case "protection" -> "Nächstes Team-Level mit stärkerer Schadensreduktion";
            case "sharpness" -> "Einmalig, danach permanent für euer Team";
            case "haste" -> "Mehr Abbauspeed pro Stufe";
            case "forge" -> "Höhere Drop-Geschwindigkeit pro Stufe";
            case "trap", "alarm-trap", "blind-trap", "knock-trap", "silence-trap" -> "Bis zu " + plugin.getConfigManager().getTrapMaxActiveCount() + " Fallen gleichzeitig in eurer Trap-Queue";
            case "iron-armor" -> "Diamantrüstung als nächster persönlicher Schritt";
            case "diamond-armor" -> "Endstufe erreicht";
            case "diamond-sword" -> "Endstufe erreicht";
            case "wool" -> "Verstärkte Wolle · Holz · Endstein";
            case "oak-planks" -> "Gehärtetes Holz · Endstein";
            case "end-stone" -> "Verdichteter Endstein · Obsidian";
            case "glass" -> "Explosionsfestes Glas";
            case "stone-sword" -> "Eisenschwert oder Sharpness";
            case "iron-sword" -> "Diamantschwert oder Lifesteal-Playstyle";
            case "bow" -> "Power · Punch · Flame";
            case "fireball" -> "Stärkere Knockback-Variante";
            case "pickaxe" -> "Holz → Stein → Eisen";
            case "axe" -> "Mehr Tempo gegen Holzdefense";
            case "shears" -> "Verbesserte Rush-Kontrolle";
            case "tnt" -> "Cluster TNT als späte Spezialvariante";
            case "breach-tnt" -> "Mehr Blockdruck gegen platzierte Defense statt rohem PvP-Schaden";
            case "jump-tnt" -> "Mehr Owner-Knockback für TNT-Jumps und Clips";
            case "cluster-tnt" -> "Spaltet sich in 3 Mini-TNTs für chaotischen Flächendruck";
            case "sticky-tnt" -> "Klebt an Wänden/Decken für präzise Unter-Defense-Plays";
            case "ender-pearl" -> "Silent Pearl als sichere Variation";
            case "bridge-egg" -> "Schnellere Bridge-Route";
            case "knockback-stick" -> "Stärkere Void-Kontrolle";
            case "dash" -> "Stufe II verlängert Speed und verstärkt den Burst";
            case "berserker" -> "Stufe II verlängert Dauer und erhöht Stärke/Tempo";
            case "shield" -> "Stufe II verlängert das Schutzfenster und erhöht Resistenz";
            case "builder" -> "Stufe II bringt längere Dauer und stärkeres Haste";
            case "tracker" -> "Stufe II markiert das Ziel kurz mit Glowing";
            case "void-rescue" -> "Einmal pro Leben automatische Rettung mit Slow Falling";
            case "bow-power" -> "Power I → II → III für alle Bögen im Team";
            case "bow-punch" -> "Punch I → II für alle Bögen im Team";
            case "bow-flame" -> "Einmalig, danach permanent Flame auf allen Team-Bögen";
            default -> "Siehe nächstes Match-Upgrade";
        };
    }

    private String getOfferProgressLine(Player player, TeamData teamData, ShopOfferConfig offer) {
        if (player == null || offer == null) {
            return "";
        }
        PlayerLoadoutState state = playerLoadoutStates.computeIfAbsent(player.getUniqueId(), ignored -> new PlayerLoadoutState());
        return switch (offer.getAction().toLowerCase(Locale.ROOT)) {
            case "protection" -> teamData == null ? "" : formatTeamLevelLine("Protection", teamUpgradeStates.computeIfAbsent(teamData.getId().toLowerCase(), ignored -> new TeamUpgradeState()).protectionLevel,
                    plugin.getConfigManager().getUpgradeMaxLevel("protection", 4));
            case "haste" -> teamData == null ? "" : formatTeamLevelLine("Haste", teamUpgradeStates.computeIfAbsent(teamData.getId().toLowerCase(), ignored -> new TeamUpgradeState()).hasteLevel,
                    plugin.getConfigManager().getUpgradeMaxLevel("haste", 2));
            case "forge" -> teamData == null ? "" : formatTeamLevelLine("Forge", teamUpgradeStates.computeIfAbsent(teamData.getId().toLowerCase(), ignored -> new TeamUpgradeState()).forgeLevel,
                    plugin.getConfigManager().getUpgradeMaxLevel("forge", 3));
            case "sharpness" -> teamData == null ? "" : buildLoreFieldLine("Status",
                    teamUpgradeStates.computeIfAbsent(teamData.getId().toLowerCase(), ignored -> new TeamUpgradeState()).hasSharpness ? "&aAktiv" : "&eVerfügbar");
            case "trap", "alarm-trap", "blind-trap", "knock-trap", "silence-trap" -> {
                if (teamData == null) {
                    yield "";
                }
                TeamUpgradeState teamState = teamUpgradeStates.computeIfAbsent(teamData.getId().toLowerCase(), ignored -> new TeamUpgradeState());
                yield buildLoreFieldLine("Trap-Queue", "&f" + teamState.trapQueue.size() + "/" + plugin.getConfigManager().getTrapMaxActiveCount()
                        + " &8| &f" + formatTrapQueue(teamState));
            }
            case "iron-armor" -> buildLoreFieldLine("Level", "&fBasisrüstung &8→ &bEisenrüstung");
            case "diamond-armor" -> buildLoreFieldLine("Level", "&fEisenrüstung &8→ &bDiamantrüstung");
            case "diamond-sword" -> buildLoreFieldLine("Level", "&fEisenschwert &8→ &bDiamantschwert");
            case "pickaxe" -> buildLoreFieldLine("Fortschritt", "&f" + prettifyMaterial(resolvePickaxeMaterial(Math.max(1, state.pickaxeTier == 0 ? 1 : state.pickaxeTier))) + " &8→ &bEisen");
            case "axe" -> buildLoreFieldLine("Fortschritt", "&f" + prettifyMaterial(resolveAxeMaterial(Math.max(1, state.axeTier == 0 ? 1 : state.axeTier))) + " &8→ &bEisen");
            case "stone-sword" -> buildLoreFieldLine("Pfad", "&fHolzschwert &8→ &7Steinschwert");
            case "iron-sword" -> buildLoreFieldLine("Pfad", "&7Steinschwert &8→ &fEisenschwert");
            case "bow" -> buildLoreFieldLine("Upgrade-Pfad", "&fBasisbogen &8→ &bPower/Punch");
            case "dash" -> buildLoreFieldLine("Skill-Pfad", "&f6s Speed II &8→ &b9s Speed III");
            case "berserker" -> buildLoreFieldLine("Skill-Pfad", "&f10s Stärke I &8→ &b13s Stärke II");
            case "shield" -> buildLoreFieldLine("Skill-Pfad", "&f10s Resist I &8→ &b13s Resist II");
            case "builder" -> buildLoreFieldLine("Skill-Pfad", "&f13s Haste II &8→ &b16s Haste III");
            case "tracker" -> buildLoreFieldLine("Skill-Pfad", "&fKompass &8→ &b+2s Glowing");
            case "void-rescue" -> {
                PlayerLoadoutState loadoutState = playerLoadoutStates.computeIfAbsent(player.getUniqueId(), ignored -> new PlayerLoadoutState());
                yield loadoutState.hasVoidRescueUpgrade
                        ? buildLoreFieldLine("Status", loadoutState.voidRescueUsedThisLife
                        ? "&cIn diesem Leben verbraucht &8→ &7Reset bei Respawn"
                        : "&aBereit &8→ &fEinmal pro Leben Void-Rettung")
                        : buildLoreFieldLine("Status", "&fEinmal pro Leben vor dem Void gerettet");
            }
            case "bow-power" -> teamData == null ? "" : formatTeamLevelLine("Bow-Power",
                    teamUpgradeStates.computeIfAbsent(teamData.getId().toLowerCase(), ignored -> new TeamUpgradeState()).bowPowerLevel, 3);
            case "bow-punch" -> teamData == null ? "" : formatTeamLevelLine("Bow-Punch",
                    teamUpgradeStates.computeIfAbsent(teamData.getId().toLowerCase(), ignored -> new TeamUpgradeState()).bowPunchLevel, 2);
            case "bow-flame" -> teamData == null ? "" : buildLoreFieldLine("Status",
                    teamUpgradeStates.computeIfAbsent(teamData.getId().toLowerCase(), ignored -> new TeamUpgradeState()).hasBowFlame ? "&aAktiv" : "&eVerfügbar");
            default -> "";
        };
    }

    private String formatTeamLevelLine(String name, int currentLevel, int maxLevel) {
        if (currentLevel >= maxLevel) {
            return buildLoreFieldLine("Aktuell", "&b" + name + " " + toRoman(maxLevel) + " &8→ &aMaximal");
        }
        String current = currentLevel <= 0 ? "&fKeine" : "&f" + name + " " + toRoman(currentLevel);
        return buildLoreFieldLine("Aktuell", current + " &8→ &b" + name + " " + toRoman(currentLevel + 1));
    }

    private String getAbilityCooldownLine(ShopOfferConfig offer) {
        return switch (offer.getAction().toLowerCase(Locale.ROOT)) {
            case "dash" -> buildLoreFieldLine("Cooldown", "&b20 Sekunden");
            case "berserker" -> buildLoreFieldLine("Cooldown", "&c45 Sekunden");
            case "shield" -> buildLoreFieldLine("Cooldown", "&b35 Sekunden");
            case "builder" -> buildLoreFieldLine("Cooldown", "&625 Sekunden");
            case "tracker" -> buildLoreFieldLine("Cooldown", "&a8 Sekunden");
            default -> "";
        };
    }

    private String getUpgradeViewStatusValue(ShopOfferConfig offer, UpgradeViewState state) {
        return switch (state.status) {
            case OWNED -> "&aBereits gekauft / maximiert";
            case AVAILABLE -> "&aKaufbar";
            case NO_RESOURCES -> "&cZu wenig Ressourcen";
            case LOCKED -> isTrapOffer(offer) ? "&cTrap-Slots voll" : "&cErst im Item-Shop kaufen";
        };
    }

    private String getOfferStatusLine(boolean hasResources, boolean upgradeOwned, boolean personalOwned) {
        if (upgradeOwned) {
            return "&aBereits freigeschaltet";
        }
        if (personalOwned) {
            return "&aBereits gekauft / maximiert";
        }
        return hasResources ? "&aKaufbar" : "&cZu wenig Ressourcen";
    }

    private void sendActionbar(Player player, String text) {
        if (player == null || !player.isOnline() || text == null || text.isBlank()) {
            return;
        }
        String colored = ColorUtil.colorize(text);
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(colored));
    }

    private void applyProtectionEnchant(Player player, int protectionLevel) {
        if (protectionLevel <= 0) {
            return;
        }
        ItemStack[] armor = player.getInventory().getArmorContents();
        for (ItemStack piece : armor) {
            if (piece == null || piece.getType() == Material.AIR) {
                continue;
            }
            piece.addUnsafeEnchantment(Enchantment.PROTECTION, protectionLevel);
        }
        player.getInventory().setArmorContents(armor);
    }

    private void applySharpnessToInventorySwords(Player player, int level) {
        ItemStack[] contents = player.getInventory().getContents();
        for (ItemStack itemStack : contents) {
            if (itemStack == null) {
                continue;
            }
            Material type = itemStack.getType();
            if (type != Material.WOODEN_SWORD && type != Material.STONE_SWORD && type != Material.IRON_SWORD
                    && type != Material.GOLDEN_SWORD && type != Material.DIAMOND_SWORD && type != Material.NETHERITE_SWORD) {
                continue;
            }
            itemStack.addUnsafeEnchantment(Enchantment.SHARPNESS, level);
        }
    }

    private void removeMaterial(Player player, Material material) {
        if (player == null || material == null) {
            return;
        }
        ItemStack[] contents = player.getInventory().getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack itemStack = contents[slot];
            if (itemStack != null && itemStack.getType() == material) {
                player.getInventory().setItem(slot, null);
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Monitoring & Diagnostics
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Logs match metrics once per match lifecycle.
     * Called at match-end / reset with a short reason tag.
     *
     * Metrics logged:
     *   – Match duration (mm:ss)
     *   – Total kills / final-kills
     *   – Bed-break times per team
     *   – Quit-rate (players who left during RUNNING phase)
     *   – Per-player kill leaderboard (top 5)
     */
    private void logMatchMetrics(String reason) {
        if (matchMetricsLogged || phase != GamePhase.RUNNING && runningSeconds == 0) {
            return;
        }
        matchMetricsLogged = true;

        StringBuilder sb = new StringBuilder();
        sb.append("\n[BedWars] ═══ MATCH METRICS [").append(arenaId).append("] reason=").append(reason).append(" ═══");
        sb.append("\n  Dauer           : ").append(formatDuration(runningSeconds));
        sb.append("\n  Kills gesamt    : ").append(totalKillCount);
        sb.append("\n  Final-Kills     : ").append(totalFinalKillCount);
        sb.append("\n  Teilnehmer Start: ").append(runningParticipantsAtStart);
        sb.append("\n  Quit-Rate       : ").append(runningParticipantsAtStart > 0
                ? String.format("%.1f%%", (runningQuitCount * 100.0 / runningParticipantsAtStart))
                : "n/a")
                .append(" (").append(runningQuitCount).append(" von ").append(runningParticipantsAtStart).append(")");

        // Bed-break timeline
        if (!bedBreakSecondByTeam.isEmpty()) {
            sb.append("\n  Bett-Zeitpunkte :");
            for (Map.Entry<String, Integer> entry : bedBreakSecondByTeam.entrySet()) {
                sb.append("\n    Team ").append(entry.getKey())
                  .append(" -> ").append(formatDuration(entry.getValue()))
                  .append(" (Sekunde ").append(entry.getValue()).append(")");
            }
        } else {
            sb.append("\n  Bett-Zeitpunkte : keine Betten zerstört");
        }

        // Top-killer leaderboard (top 5)
        List<Map.Entry<UUID, Integer>> topKillers = new ArrayList<>(killsByPlayer.entrySet());
        topKillers.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        if (!topKillers.isEmpty()) {
            sb.append("\n  Top-Killer      :");
            int max = Math.min(5, topKillers.size());
            for (int i = 0; i < max; i++) {
                Map.Entry<UUID, Integer> entry = topKillers.get(i);
                String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
                if (name == null) name = entry.getKey().toString().substring(0, 8);
                int finals = finalKillsByPlayer.getOrDefault(entry.getKey(), 0);
                sb.append("\n    ").append(i + 1).append(". ").append(name)
                  .append("  Kills=").append(entry.getValue())
                  .append("  Finals=").append(finals);
            }
        }

        sb.append("\n[BedWars] ════════════════════════════════════════════════════════");
        plugin.getLogger().info(sb.toString());
    }

    /**
     * Periodic diagnostics check – runs every {@code diagnostics-interval-seconds}.
     * Emits throttled WARN entries for:
     *   – Teams in RUNNING phase that have no players AND no bed → should have ended
     *   – Teams whose in-memory bedAlive flag doesn't match the physical world
     *   – Players tracked in playerTeams but whose PlayerMatchState is missing / wrong
     */
    private void runDiagnosticsWarnings() {
        long now = System.currentTimeMillis();
        long warnCooldownMs = plugin.getConfigManager().getMonitoringWarnCooldownSeconds() * 1000L;

        // 1. Teams mit stale player-state: intern noch Spieler gesetzt, aber keiner online/reserviert
        for (TeamData teamData : mapConfig.getTeams()) {
            long online = teamData.getPlayers().stream()
                    .map(Bukkit::getPlayer)
                    .filter(p -> p != null && p.isOnline())
                    .count();
            boolean hasReservation = hasReservedPlayers(teamData);

            if (online == 0 && !hasReservation && !teamData.getPlayers().isEmpty()) {
                String warnKey = "stale-team-players:" + teamData.getId().toLowerCase();
                Long lastWarn = warningLastLogAt.get(warnKey);
                if (lastWarn == null || now - lastWarn >= warnCooldownMs) {
                    warningLastLogAt.put(warnKey, now);
                    plugin.getLogger().warning("[BedWars][DIAG][" + arenaId + "] "
                            + "Team '" + teamData.getId() + "' hat noch interne Spieler-Referenzen, aber keinen Online-/Reserve-Spieler mehr. "
                            + "Mögliche State-Inkonsistenz! (Sekunde=" + runningSeconds + ")");
                }
            }
        }

        // 2. Bett-Status inkonsistent: bedAlive=true but no physical bed block at the configured location
        for (TeamData teamData : mapConfig.getTeams()) {
            if (!teamData.isBedAlive()) {
                continue;
            }
            Location bedLoc = teamData.getBed();
            if (bedLoc == null || bedLoc.getWorld() == null) {
                continue;
            }
            Block bedBlock = bedLoc.getBlock();
            boolean physicalBedPresent = bedBlock.getType().name().endsWith("_BED");
            if (!physicalBedPresent) {
                // Check nearby (bed may be off-by-one)
                Block nearby = findNearbyBedBlock(bedLoc);
                physicalBedPresent = (nearby != null);
            }
            if (!physicalBedPresent) {
                String warnKey = "bed-missing-physical:" + teamData.getId().toLowerCase();
                Long lastWarn = warningLastLogAt.get(warnKey);
                if (lastWarn == null || now - lastWarn >= warnCooldownMs) {
                    warningLastLogAt.put(warnKey, now);
                    plugin.getLogger().warning("[BedWars][DIAG][" + arenaId + "] "
                            + "Bett von Team '" + teamData.getId() + "' ist als 'alive' markiert, "
                            + "aber kein physischer Bett-Block gefunden an "
                            + bedLoc.getBlockX() + "," + bedLoc.getBlockY() + "," + bedLoc.getBlockZ()
                            + " (Welt=" + bedLoc.getWorld().getName() + ", Sekunde=" + runningSeconds + ")");
                }
            }
        }

        // 3. Fehlerhafte States: players tracked in playerTeams with missing PlayerMatchState
        for (Map.Entry<UUID, TeamData> entry : playerTeams.entrySet()) {
            UUID uuid = entry.getKey();
            PlayerMatchState state = playerStates.get(uuid);
            if (state == null) {
                String warnKey = "missing-state:" + uuid;
                Long lastWarn = warningLastLogAt.get(warnKey);
                if (lastWarn == null || now - lastWarn >= warnCooldownMs) {
                    warningLastLogAt.put(warnKey, now);
                    Player player = Bukkit.getPlayer(uuid);
                    String name = player != null ? player.getName() : uuid.toString().substring(0, 8);
                    plugin.getLogger().warning("[BedWars][DIAG][" + arenaId + "] "
                            + "Spieler '" + name + "' ist in playerTeams registriert, hat aber keinen PlayerMatchState. "
                            + "(online=" + (player != null && player.isOnline()) + ", Sekunde=" + runningSeconds + ")");
                    // Auto-heal: assign ALIVE state
                    playerStates.put(uuid, PlayerMatchState.ALIVE);
                }
            }
        }

        // 4. Spectators / respawning players with inconsistent GameMode
        for (UUID uuid : new ArrayList<>(spectators)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                continue;
            }
            if (player.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
                String warnKey = "spectator-wrong-gamemode:" + uuid;
                Long lastWarn = warningLastLogAt.get(warnKey);
                if (lastWarn == null || now - lastWarn >= warnCooldownMs) {
                    warningLastLogAt.put(warnKey, now);
                    plugin.getLogger().warning("[BedWars][DIAG][" + arenaId + "] "
                            + "Spectator '" + player.getName() + "' hat falschen GameMode: " + player.getGameMode()
                            + " (erwartet: SPECTATOR). Auto-Korrektur wird angewendet.");
                    player.setGameMode(org.bukkit.GameMode.SPECTATOR);
                }
            }
        }
    }

    private boolean sendPlayersToFallback(List<Player> participants) {
        if (!plugin.getConfigManager().shouldReturnPlayersToFallback()) {
            return false;
        }

        String fallback = plugin.getConfigManager().getFallbackService();
        if (fallback == null || fallback.isBlank()) {
            return false;
        }

        int delaySeconds = plugin.getConfigManager().getPostMatchFallbackDelaySeconds();
        for (Player player : participants) {
            if (player == null || !player.isOnline()) {
                continue;
            }
            plugin.getMessageManager().send(player, "game.returning-lobby", Map.of(
                    "value", fallback,
                    "map", arenaId,
                    "arena", arenaId
            ));
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    plugin.getCloudNetService().sendPlayer(player, fallback);
                }
            }, delaySeconds * 20L);
        }
        return true;
    }

    private boolean isProtectedLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }

        for (ShopConfig shopConfig : mapConfig.getShops()) {
            if (sameBlock(location, shopConfig.getLocation())) {
                return true;
            }
        }
        for (GeneratorConfig generatorConfig : mapConfig.getGenerators()) {
            if (sameBlock(location, generatorConfig.getLocation())) {
                return true;
            }
        }
        for (TeamData teamData : mapConfig.getTeams()) {
            if (sameBlock(location, teamData.getGenerator())) {
                return true;
            }
        }
        return false;
    }

    private boolean sameBlock(Location first, Location second) {
        if (first == null || second == null || first.getWorld() == null || second.getWorld() == null) {
            return false;
        }
        return first.getWorld().getUID().equals(second.getWorld().getUID())
                && first.getBlockX() == second.getBlockX()
                && first.getBlockY() == second.getBlockY()
                && first.getBlockZ() == second.getBlockZ();
    }

    private ItemStack createSpectatorCompass() {
        ItemStack itemStack = new ItemStack(Material.COMPASS);
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta != null) {
            itemMeta.setDisplayName(ColorUtil.colorize(plugin.getConfigManager().getSpectatorCompassName()));
            itemMeta.setLore(List.of("§7Rechtsklick zum Wechseln des Ziels"));
            itemStack.setItemMeta(itemMeta);
        }
        return itemStack;
    }

    private ItemStack coloredArmor(Material material, Color color) {
        ItemStack itemStack = new ItemStack(material);
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta instanceof LeatherArmorMeta leatherArmorMeta) {
            leatherArmorMeta.setColor(color);
            itemStack.setItemMeta(leatherArmorMeta);
        }
        return itemStack;
    }

    private Color resolveTeamColor(String teamId) {
        return switch (teamId.toLowerCase()) {
            case "green" -> Color.GREEN;
            case "blue" -> Color.BLUE;
            case "yellow" -> Color.YELLOW;
            case "red" -> Color.RED;
            case "aqua" -> Color.AQUA;
            case "white" -> Color.WHITE;
            case "pink" -> Color.FUCHSIA;
            case "gray" -> Color.GRAY;
            default -> Color.WHITE;
        };
    }

    private Material resolveTeamWool(String teamId) {
        return switch (teamId.toLowerCase()) {
            case "green" -> Material.GREEN_WOOL;
            case "blue" -> Material.BLUE_WOOL;
            case "yellow" -> Material.YELLOW_WOOL;
            case "red" -> Material.RED_WOOL;
            case "aqua" -> Material.LIGHT_BLUE_WOOL;
            case "white" -> Material.WHITE_WOOL;
            case "pink" -> Material.PINK_WOOL;
            case "gray" -> Material.GRAY_WOOL;
            default -> Material.WHITE_WOOL;
        };
    }

    private void giveItem(Player player, ItemStack itemStack) {
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(itemStack);
        for (ItemStack leftover : leftovers.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
    }

    private String prettifyMaterial(Material material) {
        return switch (material) {
            case IRON_INGOT -> "Eisen";
            case GOLD_INGOT -> "Gold";
            case DIAMOND -> "Diamant";
            case EMERALD -> "Smaragd";
            default -> material.name();
        };
    }

    private void broadcast(String path, Map<String, String> placeholders) {
        forEachOnlineParticipant(player -> plugin.getMessageManager().send(player, path, placeholders));
    }

    private void showTitleToParticipants(String titlePath, String subtitlePath, Map<String, String> placeholders) {
        forEachOnlineParticipant(player -> showTitle(player, titlePath, subtitlePath, placeholders));
    }

    private void showTitle(Player player, String titlePath, String subtitlePath, Map<String, String> placeholders) {
        if (player == null || !player.isOnline()) {
            return;
        }

        String title = plugin.getMessageManager().get(titlePath, false, placeholders);
        String subtitle = plugin.getMessageManager().get(subtitlePath, false, placeholders);
        player.sendTitle(title, subtitle, 5, 30, 10);
    }

    private void updateSidebarForParticipants() {
        forEachOnlineParticipant(this::updateSidebar);
    }

    private void updateSidebar(Player player) {
        org.bukkit.scoreboard.ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) {
            return;
        }

        List<String> lines = new ArrayList<>();
        lines.add("&7xxx &8• &f" + formatSidebarTimer());
        lines.add("&8 ");
        lines.add("&e⚔ Phase &7| &f" + formatSidebarPhase());
        lines.add("&7 ");

        String generatorLine = buildSidebarCombinedGeneratorLine();
        if (!generatorLine.isBlank()) {
            lines.add(generatorLine);
        }

        lines.add("&f ");

        lines.add("&7Teams");
        int maxTeamLines = Math.max(0, 15 - lines.size() - 3);
        for (int index = 0; index < mapConfig.getTeams().size() && (index / 2) < maxTeamLines; index += 2) {
            TeamData leftTeam = mapConfig.getTeams().get(index);
            TeamData rightTeam = index + 1 < mapConfig.getTeams().size() ? mapConfig.getTeams().get(index + 1) : null;
            String left = formatSidebarTeamEntry(leftTeam);
            String right = rightTeam == null ? "" : " &8• " + formatSidebarTeamEntry(rightTeam);
            lines.add(left + right);
        }

        lines.add("&0 ");
        lines.add("&7K: &f" + killsByPlayer.getOrDefault(player.getUniqueId(), 0)
                + "  &8•  &7F: &f" + finalKillsByPlayer.getOrDefault(player.getUniqueId(), 0));
        lines.add("&1 ");
        lines.add("&8nivoramc.de");

        SidebarState sidebarState = sidebarStates.computeIfAbsent(player.getUniqueId(), ignored -> createSidebarState(manager));
        if (sidebarState.lines.equals(lines)) {
            if (player.getScoreboard() != sidebarState.scoreboard) {
                player.setScoreboard(sidebarState.scoreboard);
            }
            return;
        }

        for (String entry : sidebarState.entries) {
            sidebarState.scoreboard.resetScores(entry);
        }

        int score = lines.size();
        for (int i = 0; i < sidebarState.entries.size(); i++) {
            Team lineTeam = sidebarState.lineTeams.get(i);
            if (i < lines.size()) {
                lineTeam.setPrefix(ColorUtil.colorize(lines.get(i)));
                sidebarState.objective.getScore(sidebarState.entries.get(i)).setScore(score--);
            } else {
                lineTeam.setPrefix("");
            }
        }

        sidebarState.lines = List.copyOf(lines);
        player.setScoreboard(sidebarState.scoreboard);
    }

    private String formatSidebarTimer() {
        return switch (phase) {
            case WAITING, STARTING -> formatDuration(countdown);
            case ENDING -> formatDuration(resetCountdown);
            case RUNNING -> formatDuration(runningSeconds);
        };
    }

    private String formatSidebarPhase() {
        return switch (phase) {
            case WAITING -> "Wartend";
            case STARTING -> "Startet";
            case RUNNING -> "Laufend";
            case ENDING -> "Endet";
        };
    }

    private String buildSidebarGeneratorLine(String generatorType, String color, String icon) {
        for (GeneratorRuntime runtime : generatorRuntimes) {
            if (!runtime.getGeneratorType().equalsIgnoreCase(generatorType)) {
                continue;
            }
            int nextTierLevel = runtime.getNextTierLevel();
            int secondsUntilNextTier = runtime.getSecondsUntilNextTier();
            if (nextTierLevel <= 0 || secondsUntilNextTier < 0) {
                continue;
            }
            return color + icon + " &7" + toRoman(nextTierLevel) + " in &f" + formatDuration(secondsUntilNextTier);
        }
        return "";
    }

    private String buildSidebarCombinedGeneratorLine() {
        String diamond = buildSidebarGeneratorLine("diamond", "&b", "💎");
        String emerald = buildSidebarGeneratorLine("emerald", "&a", "💚");
        if (diamond.isBlank()) {
            return emerald;
        }
        if (emerald.isBlank()) {
            return diamond;
        }
        return diamond + " &8• " + emerald;
    }

    private String formatSidebarTeamEntry(TeamData teamData) {
        if (teamData == null) {
            return "";
        }
        String teamColor = getSidebarTeamColorCode(teamData.getId());
        String shortName = getTeamShortName(teamData.getId());
        String statusColor = teamData.isBedAlive() ? "&a" : "&c";
        String statusSymbol = teamData.isBedAlive() ? "✔" : "✘";
        return statusColor + statusSymbol + " " + teamColor + shortName;
    }

    private String getSidebarTeamColorCode(String teamId) {
        return switch (teamId.toLowerCase()) {
            case "green" -> "&a";
            case "blue" -> "&9";
            case "yellow" -> "&e";
            case "red" -> "&c";
            case "aqua" -> "&3";
            case "white" -> "&f";
            case "pink" -> "&d";
            case "gray" -> "&7";
            default -> "&7";
        };
    }

    private SidebarState createSidebarState(org.bukkit.scoreboard.ScoreboardManager manager) {
        Scoreboard scoreboard = manager.getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("bw", "dummy", ColorUtil.colorize("&b&lBEDWARS"));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        List<String> entries = new ArrayList<>();
        List<Team> lineTeams = new ArrayList<>();
        ChatColor[] colors = ChatColor.values();
        for (int i = 0; i < 15; i++) {
            String entry = colors[i].toString() + ChatColor.RESET;
            Team lineTeam = scoreboard.registerNewTeam("l" + i);
            lineTeam.addEntry(entry);
            lineTeam.setPrefix("");
            entries.add(entry);
            lineTeams.add(lineTeam);
        }
        return new SidebarState(scoreboard, objective, entries, lineTeams);
    }

    private void openSpectatorMenu(Player spectator) {
        SpectatorInventoryHolder holder = new SpectatorInventoryHolder(arenaId);
        holder.setPage(0);
        Inventory inventory = Bukkit.createInventory(holder, 54, ColorUtil.colorize("&8Spectator-Menue"));
        holder.setInventory(inventory);
        renderSpectatorMenu(holder);
        spectator.openInventory(inventory);
        playConfiguredSound(spectator, "spectator-menu-open", Sound.UI_BUTTON_CLICK, 0.7F, 1.2F);
    }

    private void renderSpectatorMenu(SpectatorInventoryHolder holder) {
        Inventory inventory = holder.getInventory();
        inventory.clear();
        decorateSpectatorInventory(inventory);

        List<Player> targets = playerTeams.keySet().stream()
                .map(Bukkit::getPlayer)
                .filter(target -> target != null && target.isOnline())
                .sorted(java.util.Comparator.comparing(Player::getName))
                .toList();

        int maxPage = Math.max(0, (targets.size() - 1) / SPECTATOR_PLAYER_SLOTS_PER_PAGE);
        holder.setMaxPage(maxPage);
        holder.setPage(Math.min(holder.getPage(), holder.getMaxPage()));

        Map<Integer, UUID> playerSlots = new LinkedHashMap<>();
        int start = holder.getPage() * SPECTATOR_PLAYER_SLOTS_PER_PAGE;
        int end = Math.min(targets.size(), start + SPECTATOR_PLAYER_SLOTS_PER_PAGE);
        for (int index = start; index < end; index++) {
            int slot = SPECTATOR_PLAYER_DISPLAY_SLOTS[index - start];
            Player target = targets.get(index);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta rawMeta = head.getItemMeta();
            if (rawMeta instanceof SkullMeta meta) {
                meta.setOwningPlayer(target);
                meta.setDisplayName(ColorUtil.colorize("&b" + target.getName()));
                meta.setLore(List.of("§7Teleport per Kompass ist deaktiviert"));
                head.setItemMeta(meta);
            }
            inventory.setItem(slot, head);
            playerSlots.put(slot, target.getUniqueId());
        }
        holder.setPlayerSlots(playerSlots);

        Map<Integer, String> teamSlots = new LinkedHashMap<>();
        int teamSlot = SPECTATOR_TEAM_SLOT_START;
        for (TeamData teamData : mapConfig.getTeams()) {
            if (teamSlot > SPECTATOR_TEAM_SLOT_END) {
                break;
            }
            ItemStack icon = new ItemStack(resolveTeamWool(teamData.getId()));
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ColorUtil.colorize("&eTeam " + teamData.getDisplayName()));
                meta.setLore(List.of("§7Teleport per Kompass ist deaktiviert"));
                icon.setItemMeta(meta);
            }
            inventory.setItem(teamSlot, icon);
            teamSlots.put(teamSlot, teamData.getId().toLowerCase());
            teamSlot++;
        }
        holder.setTeamSlots(teamSlots);

        inventory.setItem(SPECTATOR_INFO_SLOT, createMenuInfoItem(holder.getPage(), holder.getMaxPage(), targets.size()));
        if (holder.getPage() > 0) {
            inventory.setItem(SPECTATOR_PREV_SLOT, createNavigationItem("&eZuruck", Material.ARROW));
        }
        if (holder.getPage() < holder.getMaxPage()) {
            inventory.setItem(SPECTATOR_NEXT_SLOT, createNavigationItem("&eWeiter", Material.ARROW));
        }
    }

    private void decorateSpectatorInventory(Inventory inventory) {
        inventory.setItem(0, createGuiItem(Material.STONE_BUTTON, " "));
        inventory.setItem(8, createGuiItem(Material.STONE_BUTTON, " "));
        inventory.setItem(45, createGuiItem(Material.STONE_BUTTON, " "));
        inventory.setItem(53, createGuiItem(Material.STONE_BUTTON, " "));
        for (int slot : new int[]{9, 18, 27, 36, 17, 26, 35, 44}) {
            inventory.setItem(slot, createGuiItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " "));
        }
    }

    private void handleSpectatorMenuClick(InventoryClickEvent event, Player spectator, SpectatorInventoryHolder holder) {
        event.setCancelled(true);
        if (!isSpectator(spectator)) {
            return;
        }
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
            return;
        }

        if (event.getSlot() == SPECTATOR_PREV_SLOT && holder.getPage() > 0) {
            holder.setPage(holder.getPage() - 1);
            renderSpectatorMenu(holder);
            return;
        }
        if (event.getSlot() == SPECTATOR_NEXT_SLOT && holder.getPage() < holder.getMaxPage()) {
            holder.setPage(holder.getPage() + 1);
            renderSpectatorMenu(holder);
            return;
        }

        UUID targetId = holder.getPlayerSlots().get(event.getSlot());
        if (targetId != null) {
            Player target = Bukkit.getPlayer(targetId);
            if (target != null && target.isOnline()) {
                spectator.sendMessage(ColorUtil.colorize("&8[&bBedWars&8] &7Kompass-Teleport ist deaktiviert. Ziel wäre: &f" + target.getName()));
            }
            return;
        }

        String teamId = holder.getTeamSlots().get(event.getSlot());
        if (teamId == null) {
            return;
        }
        Player teamTarget = playerTeams.entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue().getId().equalsIgnoreCase(teamId))
                .map(Map.Entry::getKey)
                .map(Bukkit::getPlayer)
                .filter(player -> player != null && player.isOnline())
                .findFirst()
                .orElse(null);
        if (teamTarget != null) {
            spectator.sendMessage(ColorUtil.colorize("&8[&bBedWars&8] &7Kompass-Teleport ist deaktiviert. Team-Ziel wäre: &f" + teamTarget.getName()));
        }
    }

    private ItemStack createNavigationItem(String title, Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.colorize(title));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createMenuInfoItem(int page, int maxPage, int totalPlayers) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.colorize("&bSeite " + (page + 1) + "/" + (maxPage + 1)));
            meta.setLore(List.of("§7Spieler: §f" + totalPlayers));
            item.setItemMeta(meta);
        }
        return item;
    }

    private boolean isOnCooldown(Map<UUID, Long> cooldowns, UUID uuid, long cooldownMillis) {
        if (uuid == null || cooldownMillis <= 0L) {
            return false;
        }
        long now = System.currentTimeMillis();
        Long last = cooldowns.get(uuid);
        if (last != null && now - last < cooldownMillis) {
            return true;
        }
        cooldowns.put(uuid, now);
        return false;
    }

    private void cleanupTrackedDrops() {
        List<UUID> removeIds = new ArrayList<>();
        for (UUID itemId : trackedDroppedItems) {
            Entity entity = Bukkit.getEntity(itemId);
            if (!(entity instanceof Item item) || item.isDead() || !item.isValid()) {
                removeIds.add(itemId);
            }
        }
        if (!removeIds.isEmpty()) {
            trackedDroppedItems.removeAll(removeIds);
        }
    }

    private void playConfiguredSoundToParticipants(String key, Sound fallback, float fallbackVolume, float fallbackPitch) {
        forEachOnlineParticipant(player -> playConfiguredSound(player, key, fallback, fallbackVolume, fallbackPitch));
    }

    private void playConfiguredSound(Player player, String key, Sound fallback, float fallbackVolume, float fallbackPitch) {
        if (player == null || !player.isOnline()) {
            return;
        }
        Sound sound = plugin.getConfigManager().getUiSound(key, fallback);
        float volume = plugin.getConfigManager().getUiSoundVolume(key, fallbackVolume);
        float pitch = plugin.getConfigManager().getUiSoundPitch(key, fallbackPitch);
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    private String getTeamColorCode(String teamId) {
        return switch (teamId.toLowerCase()) {
            case "green" -> "&a";
            case "blue" -> "&9";
            case "yellow" -> "&e";
            case "red" -> "&c";
            case "aqua" -> "&b";
            case "white" -> "&f";
            case "pink" -> "&d";
            case "gray" -> "&8";
            default -> "&7";
        };
    }

    private String getTeamShortName(String teamId) {
        return switch (teamId.toLowerCase()) {
            case "green" -> "GRN";
            case "blue" -> "BLU";
            case "yellow" -> "YLW";
            case "red" -> "RED";
            case "aqua" -> "AQU";
            case "white" -> "WHT";
            case "pink" -> "PNK";
            case "gray" -> "GRY";
            default -> teamId.length() <= 3 ? teamId.toUpperCase() : teamId.substring(0, 3).toUpperCase();
        };
    }

    private String getBlockKey(Block block) {
        return block.getWorld().getName() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
    }

    private List<Block> resolveBedPair(Location bedLocation) {
        if (bedLocation == null || bedLocation.getWorld() == null) {
            return List.of();
        }

        Block origin = findNearbyBedBlock(bedLocation);
        if (origin == null) {
            return List.of();
        }

        List<Block> blocks = new ArrayList<>();
        blocks.add(origin);
        Block counterpart = resolveBedCounterpart(origin);
        if (counterpart != null && !sameBlock(origin.getLocation(), counterpart.getLocation())) {
            blocks.add(counterpart);
        }
        blocks.sort((first, second) -> {
            Bed firstBed = first.getBlockData() instanceof Bed data ? data : null;
            Bed secondBed = second.getBlockData() instanceof Bed data ? data : null;
            boolean firstIsHead = firstBed != null && firstBed.getPart() == Bed.Part.HEAD;
            boolean secondIsHead = secondBed != null && secondBed.getPart() == Bed.Part.HEAD;
            return Boolean.compare(firstIsHead, secondIsHead);
        });
        return blocks;
    }

    private List<Location> resolveBedCandidateLocations(Location location) {
        List<Location> candidates = new ArrayList<>();
        candidates.add(location);
        for (Block bedBlock : resolveBedPair(location)) {
            Location bedLocation = bedBlock.getLocation();
            if (candidates.stream().noneMatch(existing -> sameBlock(existing, bedLocation))) {
                candidates.add(bedLocation);
            }
        }
        return candidates;
    }

    private Block findNearbyBedBlock(Location origin) {
        Block originBlock = origin.getBlock();
        if (originBlock.getType().name().endsWith("_BED")) {
            return originBlock;
        }

        Block nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (int x = -4; x <= 4; x++) {
            for (int y = -4; y <= 4; y++) {
                for (int z = -4; z <= 4; z++) {
                    Block block = origin.clone().add(x, y, z).getBlock();
                    if (!block.getType().name().endsWith("_BED")) {
                        continue;
                    }
                    double distance = block.getLocation().add(0.5D, 0.5D, 0.5D).distanceSquared(origin);
                    if (distance < nearestDistance) {
                        nearest = block;
                        nearestDistance = distance;
                    }
                }
            }
        }
        return nearest;
    }

    private Block resolveBedCounterpart(Block block) {
        if (!(block.getBlockData() instanceof Bed bedData)) {
            return null;
        }

        BlockFace direction = bedData.getPart() == Bed.Part.FOOT
                ? bedData.getFacing()
                : bedData.getFacing().getOppositeFace();
        Block counterpart = block.getRelative(direction);
        return counterpart.getType().name().endsWith("_BED") ? counterpart : null;
    }

    private void ensureCompleteBedSnapshots(List<StoredBlockState> snapshots) {
        if (snapshots == null || snapshots.isEmpty()) {
            return;
        }

        Set<String> snapshotKeys = new HashSet<>();
        for (StoredBlockState snapshot : snapshots) {
            snapshotKeys.add(toBlockKey(snapshot.worldName, snapshot.x, snapshot.y, snapshot.z));
        }

        List<StoredBlockState> additionalSnapshots = new ArrayList<>();
        for (StoredBlockState snapshot : snapshots) {
            StoredBlockState counterpart = createBedCounterpartSnapshot(snapshot);
            if (counterpart == null) {
                continue;
            }
            String counterpartKey = toBlockKey(counterpart.worldName, counterpart.x, counterpart.y, counterpart.z);
            if (snapshotKeys.add(counterpartKey)) {
                additionalSnapshots.add(counterpart);
            }
        }
        snapshots.addAll(additionalSnapshots);
    }

    private StoredBlockState createBedCounterpartSnapshot(StoredBlockState snapshot) {
        BlockData blockData = Bukkit.createBlockData(snapshot.blockDataString);
        if (!(blockData instanceof Bed bedData)) {
            return null;
        }

        BlockFace direction = bedData.getPart() == Bed.Part.FOOT
                ? bedData.getFacing()
                : bedData.getFacing().getOppositeFace();

        Bed counterpartData = (Bed) bedData.clone();
        counterpartData.setPart(bedData.getPart() == Bed.Part.FOOT ? Bed.Part.HEAD : Bed.Part.FOOT);

        return new StoredBlockState(
                snapshot.worldName,
                snapshot.x + direction.getModX(),
                snapshot.y + direction.getModY(),
                snapshot.z + direction.getModZ(),
                counterpartData.getAsString()
        );
    }

    private String toBlockKey(String worldName, int x, int y, int z) {
        return worldName + ":" + x + ":" + y + ":" + z;
    }

    private void forEachOnlineParticipant(Consumer<Player> consumer) {
        if (consumer == null) {
            return;
        }
        for (UUID uuid : playerTeams.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                consumer.accept(player);
            }
        }
        for (UUID uuid : spectators) {
            if (playerTeams.containsKey(uuid)) {
                continue;
            }
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                consumer.accept(player);
            }
        }
    }

    private boolean hasOnlineParticipants() {
        final boolean[] found = {false};
        forEachOnlineParticipant(player -> found[0] = true);
        return found[0];
    }

    private List<Player> collectOnlineParticipants() {
        List<Player> players = new ArrayList<>();
        forEachOnlineParticipant(players::add);
        return players;
    }

    private static final class StoredBlockState {
        private final String worldName;
        private final int x;
        private final int y;
        private final int z;
        private final String blockDataString;

        private StoredBlockState(String worldName, int x, int y, int z, String blockDataString) {
            this.worldName = worldName;
            this.x = x;
            this.y = y;
            this.z = z;
            this.blockDataString = blockDataString;
        }

        private static StoredBlockState capture(Block block) {
            BlockData blockData = block.getBlockData();
            return new StoredBlockState(
                    block.getWorld().getName(),
                    block.getX(),
                    block.getY(),
                    block.getZ(),
                    blockData.getAsString()
            );
        }

        private static StoredBlockState capture(BlockState blockState) {
            BlockData blockData = blockState.getBlockData();
            return new StoredBlockState(
                    blockState.getWorld().getName(),
                    blockState.getX(),
                    blockState.getY(),
                    blockState.getZ(),
                    blockData.getAsString()
            );
        }

        private void restore() {
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                return;
            }
            Block block = world.getBlockAt(x, y, z);
            block.setBlockData(Bukkit.createBlockData(blockDataString), false);
        }

        private void clear() {
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                return;
            }
            world.getBlockAt(x, y, z).setType(org.bukkit.Material.AIR, false);
        }

        private boolean matches(Location location) {
            return location != null
                    && location.getWorld() != null
                    && location.getWorld().getName().equals(worldName)
                    && location.getBlockX() == x
                    && location.getBlockY() == y
                    && location.getBlockZ() == z;
        }
    }

    private final class GeneratorRuntime {
        private final String id;
        private final String dropMarker;
        private final Location location;
        private final List<GeneratorTierConfig> tiers;
        private long nextDropTick;

        private GeneratorRuntime(String id, Location location, List<GeneratorTierConfig> tiers) {
            this.id = id;
            this.dropMarker = arenaId + ":" + id;
            this.location = location;
            this.tiers = tiers;
            this.nextDropTick = tiers.isEmpty() ? 20L : tiers.getFirst().getIntervalTicks();
        }

        private void tick() {
            if (location == null || location.getWorld() == null) {
                return;
            }
            GeneratorTierConfig tier = resolveTier();
            nextDropTick--;
            if (nextDropTick > 0) {
                return;
            }
            nextDropTick = resolveEffectiveInterval(tier);

            int nearbyAmount = 0;
            for (Entity entity : location.getWorld().getNearbyEntities(location, 1.5D, 1.5D, 1.5D)) {
                if (entity instanceof Item item && isTrackedGeneratorItem(item, tier.getMaterial())) {
                    nearbyAmount += item.getItemStack().getAmount();
                }
            }
            if (nearbyAmount >= tier.getItemCap()) {
                return;
            }

            int dropAmount = Math.max(1, Math.min(resolveEffectiveDropAmount(tier), tier.getItemCap() - nearbyAmount));
            if (dropAmount <= 0) {
                return;
            }
            ItemStack itemStack = new ItemStack(tier.getMaterial(), dropAmount);
            Item droppedItem = location.getWorld().dropItem(location.clone().add(0.5D, 1.0D, 0.5D), itemStack);
            droppedItem.getPersistentDataContainer().set(generatorDropKey, PersistentDataType.STRING, dropMarker);
            trackedDroppedItems.add(droppedItem.getUniqueId());
        }

        private long resolveEffectiveInterval(GeneratorTierConfig tier) {
            long baseInterval = Math.max(1L, tier.getIntervalTicks());
            String owningTeamId = resolveOwningTeamId();
            if (owningTeamId == null) {
                return baseInterval;
            }

            TeamUpgradeState state = teamUpgradeStates.computeIfAbsent(owningTeamId.toLowerCase(), ignored -> new TeamUpgradeState());
            if (state.forgeLevel <= 0) {
                return baseInterval;
            }

            double modifier = switch (Math.min(3, state.forgeLevel)) {
                case 1 -> 0.70D;
                case 2 -> 0.45D;
                default -> 0.25D;
            };
            return Math.max(1L, Math.round(baseInterval * modifier));
        }

        private int resolveEffectiveDropAmount(GeneratorTierConfig tier) {
            String owningTeamId = resolveOwningTeamId();
            if (owningTeamId == null) {
                return 1;
            }

            TeamUpgradeState state = teamUpgradeStates.computeIfAbsent(owningTeamId.toLowerCase(), ignored -> new TeamUpgradeState());
            int forgeLevel = Math.max(0, state.forgeLevel);
            if (forgeLevel <= 0) {
                return 1;
            }

            if (tier.getMaterial() == Material.IRON_INGOT) {
                return switch (Math.min(3, forgeLevel)) {
                    case 1 -> 1;
                    case 2 -> 2;
                    default -> 3;
                };
            }
            if (tier.getMaterial() == Material.GOLD_INGOT) {
                return Math.min(2, Math.max(1, forgeLevel));
            }
            return 1;
        }

        private String resolveOwningTeamId() {
            if (id.endsWith("-iron")) {
                return id.substring(0, id.length() - 5);
            }
            if (id.endsWith("-gold")) {
                return id.substring(0, id.length() - 5);
            }
            return null;
        }

        private String getGeneratorType() {
            if (tiers.isEmpty()) {
                return "";
            }
            return tiers.getFirst().getType();
        }

        private int getNextTierLevel() {
            int currentIndex = getCurrentTierIndex();
            int nextIndex = currentIndex + 1;
            if (nextIndex >= tiers.size()) {
                return -1;
            }
            return nextIndex + 1;
        }

        private int getSecondsUntilNextTier() {
            int currentIndex = getCurrentTierIndex();
            int nextIndex = currentIndex + 1;
            if (nextIndex >= tiers.size()) {
                return -1;
            }
            return Math.max(0, tiers.get(nextIndex).getAfterSeconds() - runningSeconds);
        }

        private int getCurrentTierIndex() {
            int currentIndex = 0;
            for (int index = 0; index < tiers.size(); index++) {
                if (runningSeconds >= tiers.get(index).getAfterSeconds()) {
                    currentIndex = index;
                }
            }
            return currentIndex;
        }

        private boolean isTrackedGeneratorItem(Item item, Material material) {
            if (item.getItemStack().getType() != material) {
                return false;
            }

            String marker = item.getPersistentDataContainer().get(generatorDropKey, PersistentDataType.STRING);
            if (marker != null) {
                return marker.equals(dropMarker);
            }

            Location itemLocation = item.getLocation();
            return itemLocation.getWorld() != null
                    && location.getWorld() != null
                    && itemLocation.getWorld().getUID().equals(location.getWorld().getUID())
                    && itemLocation.distanceSquared(location) <= 0.75D;
        }

        private GeneratorTierConfig resolveTier() {
            GeneratorTierConfig current = tiers.getFirst();
            for (GeneratorTierConfig tier : tiers) {
                if (runningSeconds >= tier.getAfterSeconds()) {
                    current = tier;
                }
            }
            return current;
        }
    }

    private static final class TeamUpgradeState {
        private int protectionLevel;
        private boolean hasSharpness;
        private int hasteLevel;
        private int forgeLevel;
        private final List<TrapType> trapQueue = new ArrayList<>();
        private long trapRearmUntil;
        // Bogen-Upgrades
        private int bowPowerLevel;
        private int bowPunchLevel;
        private boolean hasBowFlame;
    }

    private static final class PlayerLoadoutState {
        private int swordTier;
        private int armorTier;
        private int pickaxeTier;
        private int axeTier;
        private boolean hasShears;
        private boolean hasBow;
        private int dashLevel;
        private int berserkerLevel;
        private int shieldLevel;
        private int builderLevel;
        private int trackerLevel;
        private boolean hasDashAbility;
        private boolean hasBerserkerAbility;
        private boolean hasShieldAbility;
        private boolean hasBuilderAbility;
        private boolean hasTrackerAbility;
        private boolean hasVoidRescueUpgrade;
        private boolean voidRescueUsedThisLife;
        private final Map<String, Long> abilityCooldowns = new ConcurrentHashMap<>();
        private final Map<String, Long> specialItemCooldowns = new ConcurrentHashMap<>();
        private long specialItemSilenceUntil;
        private long pearlProtectionUntil;
    }

    private enum TrapType {
        ALARM("Alarm Trap"),
        BLIND("Blind Trap"),
        KNOCK("Knock Trap"),
        SILENCE("Silence Trap");

        private final String displayName;

        TrapType(String displayName) {
            this.displayName = displayName;
        }

        private String getDisplayName() {
            return displayName;
        }
    }

    private enum UpgradeViewStatus {
        OWNED,
        AVAILABLE,
        NO_RESOURCES,
        LOCKED
    }

    private static final class UpgradeViewState {
        private final int currentLevel;
        private final int maxLevel;
        private final int nextCost;
        private final UpgradeViewStatus status;

        private UpgradeViewState(int currentLevel, int maxLevel, int nextCost, UpgradeViewStatus status) {
            this.currentLevel = currentLevel;
            this.maxLevel = maxLevel;
            this.nextCost = nextCost;
            this.status = status;
        }
    }

    private static final class SidebarState {
        private final Scoreboard scoreboard;
        private final Objective objective;
        private final List<String> entries;
        private final List<Team> lineTeams;
        private List<String> lines;

        private SidebarState(Scoreboard scoreboard, Objective objective, List<String> entries, List<Team> lineTeams) {
            this.scoreboard = scoreboard;
            this.objective = objective;
            this.entries = entries;
            this.lineTeams = lineTeams;
            this.lines = List.of();
        }
    }
}


