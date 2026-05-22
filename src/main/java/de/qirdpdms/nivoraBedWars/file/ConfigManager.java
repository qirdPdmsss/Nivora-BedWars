package de.qirdpdms.nivoraBedWars.file;

import de.qirdpdms.nivoraBedWars.Main;
import de.qirdpdms.nivoraBedWars.model.GeneratorConfig;
import de.qirdpdms.nivoraBedWars.model.GeneratorTierConfig;
import de.qirdpdms.nivoraBedWars.model.MapConfig;
import de.qirdpdms.nivoraBedWars.model.PluginMode;
import de.qirdpdms.nivoraBedWars.model.ShopCategoryConfig;
import de.qirdpdms.nivoraBedWars.model.ShopOfferConfig;
import de.qirdpdms.nivoraBedWars.model.ShopConfig;
import de.qirdpdms.nivoraBedWars.model.TeamData;
import de.qirdpdms.nivoraBedWars.util.LocationUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigManager {

    private static final String DEFAULT_PLACEHOLDER_SPAWN = "world,0,80,0,0,0";

    private final Main plugin;
    private FileConfiguration config;
    private FileConfiguration bundledConfig;
    private final Map<String, List<GeneratorTierConfig>> generatorTierCache;
    private final Map<String, List<ShopCategoryConfig>> shopCategoryCache;
    private final Map<String, List<ShopOfferConfig>> shopOfferCache;

    public ConfigManager(Main plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        this.bundledConfig = loadBundledConfig();
        this.generatorTierCache = new ConcurrentHashMap<>();
        this.shopCategoryCache = new ConcurrentHashMap<>();
        this.shopOfferCache = new ConcurrentHashMap<>();
    }

    public void reload() {
        this.config = plugin.getConfig();
        this.bundledConfig = loadBundledConfig();
        this.generatorTierCache.clear();
        this.shopCategoryCache.clear();
        this.shopOfferCache.clear();
    }

    public PluginMode getPluginMode() {
        return PluginMode.fromString(config.getString("mode", "LOBBY"));
    }

    public String getPluginNameKey() {
        return config.getString("settings.plugin-name-key", "nivorabedwars");
    }

    public String getQueueTaskName() {
        return config.getString("queue.task-name", "Bedwars");
    }

    public int getQueueMinPlayers() {
        return Math.max(1, config.getInt("queue.min-players", 1));
    }

    public int getQueueMaxPlayers() {
        return Math.max(getQueueMinPlayers(), config.getInt("queue.max-players", 8));
    }

    public int getQueueBatchMaxPlayers() {
        int maxPlayers = getQueueMaxPlayers();
        for (String mapId : getMapRotation()) {
            maxPlayers = Math.max(maxPlayers, loadMapConfig(mapId).getMaxPlayers());
        }
        return maxPlayers;
    }

    public int getServiceMaxPlayers() {
        int totalCapacity = 0;
        for (String mapId : getArenaMapIds()) {
            totalCapacity += Math.max(1, loadMapConfig(mapId).getMaxPlayers());
        }
        return Math.max(getQueueMaxPlayers(), totalCapacity);
    }

    public long getQueueTickIntervalTicks() {
        return Math.max(20L, config.getLong("queue.tick-interval-ticks", 20L));
    }

    public boolean isQueueVerboseLoggingEnabled() {
        return config.getBoolean("queue.verbose-logging", false);
    }

    public boolean isLobbyJoinItemEnabled() {
        return config.getBoolean("lobby.join-item.enabled", true);
    }

    public String getJoinItemMaterial() {
        return config.getString("lobby.join-item.material", Material.COMPASS.name());
    }

    public int getJoinItemSlot() {
        return Math.max(0, Math.min(8, config.getInt("lobby.join-item.slot", 4)));
    }

    public String getJoinItemName() {
        return config.getString("lobby.join-item.name", "&7Bedwars spielen");
    }

    public List<String> getJoinItemLore() {
        return config.getStringList("lobby.join-item.lore");
    }

    public List<String> getMapRotation() {
        List<String> maps = config.getStringList("queue.map-rotation");
        return maps.isEmpty() ? List.of("bw_map1", "bw_map2", "bw_map3") : maps;
    }

    public List<String> getArenaMapIds() {
        if (getPluginMode() == PluginMode.GAME) {
            String gameMapId = getGameMapId();
            if (gameMapId != null && !gameMapId.isBlank()) {
                plugin.debug("config", "getArenaMapIds -> GAME mode, single arena map=" + gameMapId);
                return List.of(gameMapId);
            }
        }

        List<String> configuredMaps = config.getStringList("game.arena-maps");
        if (!configuredMaps.isEmpty()) {
            plugin.debug("config", "getArenaMapIds -> configured game.arena-maps=" + configuredMaps);
            return new ArrayList<>(new LinkedHashSet<>(configuredMaps));
        }

        List<String> rotationMaps = getMapRotation();
        if (!rotationMaps.isEmpty()) {
            plugin.debug("config", "getArenaMapIds -> queue.map-rotation fallback=" + rotationMaps);
            return new ArrayList<>(new LinkedHashSet<>(rotationMaps));
        }

        List<String> mapIds = getMapIds();
        if (!mapIds.isEmpty()) {
            plugin.debug("config", "getArenaMapIds -> maps-section fallback=" + mapIds);
            return new ArrayList<>(new LinkedHashSet<>(mapIds));
        }

        plugin.debug("config", "getArenaMapIds -> hardcoded fallback bw_map1");
        return List.of("bw_map1");
    }

    public boolean isDebugEnabled() {
        return config.getBoolean("game.monitoring.debug", false);
    }

    public boolean useStaticServicesOnly() {
        return config.getBoolean("cloudnet.static-only", true);
    }

    public boolean shouldUseStaticServices() {
        return useStaticServicesOnly() || !getStaticServices().isEmpty();
    }

    public String getCurrentServiceName() {
        List<String> candidates = List.of(
                System.getProperty("novira.bedwars.service-name", ""),
                System.getProperty("cloudnet.service-name", ""),
                getEnvironmentValue("NOVIRA_BEDWARS_SERVICE_NAME"),
                getEnvironmentValue("CLOUDNET_SERVICE_NAME"),
                config.getString("game.service-name", "")
        );
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                plugin.debug("config", "getCurrentServiceName -> selected='" + candidate.trim() + "' candidates=" + candidates);
                return candidate.trim();
            }
        }
        plugin.debug("config", "getCurrentServiceName -> no candidate found, returning empty string");
        return "";
    }

    public String getConfiguredMapForService(String serviceName) {
        if (serviceName == null || serviceName.isBlank()) {
            plugin.debug("config", "getConfiguredMapForService -> empty serviceName");
            return "";
        }

        String configuredMap = config.getString("cloudnet.service-map-assignments." + serviceName, "");
        if (!configuredMap.isBlank()) {
            plugin.debug("config", "getConfiguredMapForService -> direct assignment service='" + serviceName + "' map='" + configuredMap + "'");
            return configuredMap;
        }

        List<String> staticServices = getStaticServices();
        int serviceIndex = staticServices.indexOf(serviceName);
        if (serviceIndex < 0) {
            plugin.debug("config", "getConfiguredMapForService -> service not found in static services service='" + serviceName + "' staticServices=" + staticServices);
            return "";
        }

        List<String> mapRotation = getMapRotation();
        if (serviceIndex >= mapRotation.size()) {
            plugin.debug("config", "getConfiguredMapForService -> index out of bounds service='" + serviceName + "' index=" + serviceIndex + " rotation=" + mapRotation);
            return "";
        }
        plugin.debug("config", "getConfiguredMapForService -> rotation fallback service='" + serviceName + "' index=" + serviceIndex + " map='" + mapRotation.get(serviceIndex) + "'");
        return mapRotation.get(serviceIndex);
    }

    public String getGameMapId() {
        String currentServiceName = getCurrentServiceName();
        String configuredServiceMap = getConfiguredMapForService(currentServiceName);
        if (!configuredServiceMap.isBlank()) {
            plugin.debug("config", "getGameMapId -> using service assignment service='" + currentServiceName + "' map='" + configuredServiceMap + "'");
            return configuredServiceMap;
        }

        String systemMapId = System.getProperty("novira.bedwars.map-id", "");
        if (!systemMapId.isBlank()) {
            plugin.debug("config", "getGameMapId -> using system property map='" + systemMapId + "'");
            return systemMapId;
        }
        String environmentMapId = getEnvironmentValue("NOVIRA_BEDWARS_MAP_ID");
        if (!environmentMapId.isBlank()) {
            plugin.debug("config", "getGameMapId -> using environment map='" + environmentMapId + "'");
            return environmentMapId;
        }
        String configMapId = config.getString("game.map-id", "bw_map1");
        plugin.debug("config", "getGameMapId -> using config fallback map='" + configMapId + "'");
        return configMapId;
    }

    public long getServiceReservationSeconds() {
        return Math.max(5L, config.getLong("queue.service-reservation-seconds", 20L));
    }

    public int getGameStartCountdown() {
        return Math.max(3, config.getInt("game.start-countdown-seconds", 10));
    }

    public int getRespawnDelaySeconds() {
        return Math.max(1, config.getInt("game.respawn-delay-seconds", 5));
    }

    public boolean shouldRestorePurchasedSwordsOnRespawn() {
        return config.getBoolean("game.respawn-restore.swords", true);
    }

    public boolean shouldRestorePurchasedArmorOnRespawn() {
        return config.getBoolean("game.respawn-restore.armor", true);
    }

    public boolean shouldRestorePurchasedToolsOnRespawn() {
        return config.getBoolean("game.respawn-restore.tools", true);
    }

    public boolean shouldRestorePurchasedBowOnRespawn() {
        return config.getBoolean("game.respawn-restore.bow", true);
    }

    public int getArenaResetDelaySeconds() {
        return Math.max(3, config.getInt("game.reset-delay-seconds", 10));
    }

    public int getRejoinReservationSeconds() {
        return Math.max(5, config.getInt("game.rejoin-reservation-seconds", 30));
    }

    public int getBedDestructionAfterSeconds() {
        return Math.max(0, config.getInt("game.endgame.bed-destruction-after-seconds", 360));
    }

    public int getSuddenDeathAfterSeconds() {
        return Math.max(0, config.getInt("game.endgame.sudden-death-after-seconds", 600));
    }

    public boolean isHungerDisabled() {
        return config.getBoolean("game.disable-hunger", true);
    }

    public boolean isItemDropBlocked() {
        return config.getBoolean("game.block-item-drops", true);
    }

    public List<String> getGroundDropWhitelist() {
        List<String> configured = config.getStringList("game.drops.ground-whitelist");
        if (!configured.isEmpty()) {
            return new ArrayList<>(configured);
        }
        return List.of(
                "IRON_INGOT",
                "GOLD_INGOT",
                "DIAMOND",
                "EMERALD",
                "END_STONE",
                "TNT",
                "#WOOL"
        );
    }

    public boolean isFriendlyFireEnabled() {
        return config.getBoolean("game.friendly-fire", false);
    }

    public String getSpectatorCompassName() {
        return config.getString("game.spectator.compass-name", "&bSpectator-Kompass");
    }

    public boolean shouldReturnPlayersToFallback() {
        return config.getBoolean("game.post-match.return-to-fallback", true);
    }

    public int getPostMatchFallbackDelaySeconds() {
        return Math.max(0, config.getInt("game.post-match.delay-seconds", 5));
    }

    public boolean isDamageAllowedInWaiting() {
        return config.getBoolean("game.allow-damage-in-waiting", false);
    }

    public int getItemCleanupIntervalSeconds() {
        return Math.max(1, config.getInt("game.performance.item-cleanup-interval-seconds", 10));
    }

    public int getSidebarRefreshIntervalSeconds() {
        return Math.max(1, config.getInt("game.performance.sidebar-refresh-interval-seconds", 2));
    }

    public boolean shouldScanWorldForMatchEndItems() {
        return config.getBoolean("game.performance.match-end-world-item-scan", false);
    }

    public long getShopClickCooldownMillis() {
        return Math.max(0L, config.getLong("game.performance.shop-click-cooldown-ms", 120L));
    }

    public long getShopInteractCooldownMillis() {
        return Math.max(0L, config.getLong("game.performance.shop-interact-cooldown-ms", 250L));
    }

    public long getSpecialItemUseCooldownMillis(String action, long fallback) {
        if (action == null || action.isBlank()) {
            return Math.max(0L, fallback);
        }
        return Math.max(0L, config.getLong("game.special-items." + action.toLowerCase() + ".cooldown-ms", fallback));
    }

    public int getTrapMaxActiveCount() {
        return Math.max(1, config.getInt("game.traps.max-active", 2));
    }

    public long getTrapRearmMillis() {
        return Math.max(0L, config.getLong("game.traps.rearm-ms", 4_000L));
    }

    public int getTrapAlarmGlowingTicks() {
        return Math.max(20, config.getInt("game.traps.alarm.glowing-ticks", 100));
    }

    public int getTrapBlindnessTicks() {
        return Math.max(20, config.getInt("game.traps.blind.blindness-ticks", 80));
    }

    public int getTrapSlownessTicks() {
        return Math.max(20, config.getInt("game.traps.blind.slowness-ticks", 80));
    }

    public double getTrapKnockbackHorizontal() {
        return Math.max(0.1D, config.getDouble("game.traps.knock.horizontal", 1.15D));
    }

    public double getTrapKnockbackVertical() {
        return Math.max(0.0D, config.getDouble("game.traps.knock.vertical", 0.45D));
    }

    public long getTrapSilenceMillis() {
        return Math.max(0L, config.getLong("game.traps.silence.duration-ms", 5_000L));
    }

    public double getVoidRescueTeleportOffset() {
        return Math.max(1.0D, config.getDouble("game.void-rescue.teleport-y-offset", 6.0D));
    }

    public double getVoidRescueMinSafeYBuffer() {
        return Math.max(2.0D, config.getDouble("game.void-rescue.min-safe-y-buffer", 6.0D));
    }

    public double getVoidRescueUpwardVelocity() {
        return Math.max(0.2D, config.getDouble("game.void-rescue.upward-velocity", 1.1D));
    }

    public int getVoidRescueSlowFallingTicks() {
        return Math.max(20, config.getInt("game.void-rescue.slow-falling-ticks", 120));
    }

    public boolean shouldCreateVoidRescuePlatform() {
        return config.getBoolean("game.void-rescue.platform.enabled", true);
    }

    public long getVoidRescuePlatformLifetimeTicks() {
        return Math.max(10L, config.getLong("game.void-rescue.platform.lifetime-ticks", 100L));
    }

    public int getBridgeEggTrailLifetimeTicks() {
        return Math.max(20, config.getInt("game.special-items.bridge-egg.trail-lifetime-ticks", 60));
    }

    public int getBridgeEggMaxPlacedBlocks() {
        return Math.max(4, config.getInt("game.special-items.bridge-egg.max-placed-blocks", 18));
    }

    public int getBridgeEggHitPlacedBlocks() {
        return Math.max(4, config.getInt("game.special-items.bridge-egg.hit-placed-blocks", 8));
    }

    public int getTntFuseTicks() {
        return getTntFuseTicks("tnt");
    }

    public int getTntFuseTicks(String action) {
        return Math.max(10, (int) getSpecialItemLong(action, "fuse-ticks", getSpecialItemLong("tnt", "fuse-ticks", 40L)));
    }

    public float getTntYield() {
        return getTntYield("tnt");
    }

    public float getTntYield(String action) {
        return (float) Math.max(0.0D, getSpecialItemDouble(action, "yield", getSpecialItemDouble("tnt", "yield", 3.0D)));
    }

    public double getTntBlockRadius(String action) {
        double fallback = Math.max(1.5D, getTntYield(action) + 0.9D);
        return Math.max(1.5D, getSpecialItemDouble(action, "block-radius", fallback));
    }

    public double getTntDamageMultiplier(String action) {
        return Math.max(0.0D, getSpecialItemDouble(action, "damage-multiplier", 1.0D));
    }

    public double getTntSelfDamageMultiplier(String action) {
        return Math.max(0.0D, getSpecialItemDouble(action, "self-damage-multiplier", getTntDamageMultiplier(action)));
    }

    public double getTntOwnerBoostHorizontal(String action) {
        return Math.max(0.0D, getSpecialItemDouble(action, "owner-boost-horizontal", 1.45D));
    }

    public double getTntOwnerBoostVertical(String action) {
        return Math.max(0.0D, getSpecialItemDouble(action, "owner-boost-vertical", 1.05D));
    }

    public int getTntChildCount(String action) {
        return Math.max(1, (int) getSpecialItemLong(action, "child-count", 3L));
    }

    public int getTntChildFuseTicks(String action) {
        return Math.max(8, (int) getSpecialItemLong(action, "child-fuse-ticks", 14L));
    }

    public double getTntChildSpread(String action) {
        return Math.max(0.2D, getSpecialItemDouble(action, "child-spread", 0.9D));
    }

    public boolean shouldStickyTntIgnoreGravity(String action) {
        return config.getBoolean("game.special-items." + action.toLowerCase() + ".no-gravity",
                config.getBoolean("game.special-items.tnt.no-gravity", false));
    }

    public boolean shouldAllowPearlDamage() {
        return config.getBoolean("game.special-items.ender-pearl.allow-damage", true);
    }

    public double getFireballDamage() {
        return Math.max(0.0D, config.getDouble("game.special-items.fireball.damage", 8.0D));
    }

    public double getFireballDamageRadius() {
        return Math.max(1.0D, config.getDouble("game.special-items.fireball.damage-radius", 4.5D));
    }

    public double getFireballBlockRadius() {
        return Math.max(0.0D, config.getDouble("game.special-items.fireball.block-radius", 3.2D));
    }

    public double getFireballKnockbackHorizontal() {
        return Math.max(0.1D, config.getDouble("game.special-items.fireball.knockback-horizontal", 1.2D));
    }

    public double getFireballKnockbackVertical() {
        return Math.max(0.0D, config.getDouble("game.special-items.fireball.knockback-vertical", 0.58D));
    }

    public double getFireballSelfDamageMultiplier() {
        return Math.max(0.0D, config.getDouble("game.special-items.fireball.self-damage-multiplier", 0.75D));
    }

    public int getFireballFireTicks() {
        return Math.max(0, config.getInt("game.special-items.fireball.fire-ticks", 80));
    }

    private long getSpecialItemLong(String action, String path, long fallback) {
        if (action == null || action.isBlank()) {
            return fallback;
        }
        return config.getLong("game.special-items." + action.toLowerCase() + "." + path, fallback);
    }

    private double getSpecialItemDouble(String action, String path, double fallback) {
        if (action == null || action.isBlank()) {
            return fallback;
        }
        return config.getDouble("game.special-items." + action.toLowerCase() + "." + path, fallback);
    }

    public boolean isMonitoringEnabled() {
        return config.getBoolean("game.monitoring.enabled", true);
    }

    public int getMonitoringDiagnosticsIntervalSeconds() {
        return Math.max(1, config.getInt("game.monitoring.diagnostics-interval-seconds", 20));
    }

    public int getMonitoringWarnCooldownSeconds() {
        return Math.max(1, config.getInt("game.monitoring.warn-cooldown-seconds", 60));
    }

    public Sound getUiSound(String key, Sound fallback) {
        String configured = config.getString("game.ui.sounds." + key + ".sound", fallback.name());
        Sound sound;
        try {
            sound = Sound.valueOf(configured.toUpperCase());
        } catch (IllegalArgumentException ex) {
            sound = fallback;
        }
        return sound;
    }

    public float getUiSoundVolume(String key, float fallback) {
        return (float) Math.max(0.0D, config.getDouble("game.ui.sounds." + key + ".volume", fallback));
    }

    public float getUiSoundPitch(String key, float fallback) {
        return (float) Math.max(0.1D, config.getDouble("game.ui.sounds." + key + ".pitch", fallback));
    }

    public int getUpgradeCostForLevel(String action, int nextLevel, int fallbackCost) {
        if (nextLevel <= 0) {
            return Math.max(1, fallbackCost);
        }
        List<Integer> costs = getUpgradeStageCosts(action);
        if (costs.isEmpty()) {
            return Math.max(1, fallbackCost);
        }
        int index = Math.min(nextLevel - 1, costs.size() - 1);
        return Math.max(1, costs.get(index));
    }

    public int getUpgradeMaxLevel(String action, int fallbackMaxLevel) {
        List<Integer> costs = getUpgradeStageCosts(action);
        if (costs.isEmpty()) {
            return Math.max(1, fallbackMaxLevel);
        }
        return Math.max(1, costs.size());
    }

    private List<Integer> getUpgradeStageCosts(String action) {
        if (action == null || action.isBlank()) {
            return List.of();
        }

        List<Integer> configured = config.getIntegerList("game.upgrades.costs." + action.toLowerCase());
        if (!configured.isEmpty()) {
            List<Integer> sanitized = new ArrayList<>();
            for (Integer value : configured) {
                if (value != null && value > 0) {
                    sanitized.add(value);
                }
            }
            if (!sanitized.isEmpty()) {
                return sanitized;
            }
        }

        Map<String, List<Integer>> defaults = Map.of(
                "protection", List.of(3, 6, 9, 12),
                "haste", List.of(3, 5),
                "forge", List.of(3, 5, 7),
                "bow-power", List.of(5, 8, 11),
                "bow-punch", List.of(6, 10)
        );
        return defaults.getOrDefault(action.toLowerCase(), List.of());
    }

    public List<String> getStaticServices() {
        List<String> services = config.getStringList("cloudnet.static-services");
        return new ArrayList<>(new LinkedHashSet<>(services));
    }

    public String getStartCommandTemplate() {
        return config.getString("cloudnet.commands.start", "");
    }

    public String getConnectCommandTemplate() {
        return config.getString("cloudnet.commands.connect", "");
    }

    public String getFallbackService() {
        return config.getString("cloudnet.fallback-service", "");
    }

    public List<GeneratorTierConfig> getGeneratorTierConfigs(String type) {
        String normalizedType = normalizeGeneratorType(type);
        List<GeneratorTierConfig> cached = generatorTierCache.get(normalizedType);
        if (cached != null) {
            return new ArrayList<>(cached);
        }

        ConfigurationSection generatorSection = config.getConfigurationSection("game.generators." + normalizedType + ".tiers");
        if (generatorSection == null && normalizedType.equals("mid")) {
            generatorSection = config.getConfigurationSection("game.generators.emerald.tiers");
            normalizedType = "emerald";
            cached = generatorTierCache.get(normalizedType);
            if (cached != null) {
                return new ArrayList<>(cached);
            }
        }

        List<GeneratorTierConfig> tiers = new ArrayList<>();
        if (generatorSection != null) {
            Material material = parseMaterial(config.getString("game.generators." + normalizedType + ".material"), defaultGeneratorMaterial(normalizedType));
            for (String key : generatorSection.getKeys(false)) {
                ConfigurationSection tierSection = generatorSection.getConfigurationSection(key);
                if (tierSection == null) {
                    continue;
                }
                tiers.add(new GeneratorTierConfig(
                        normalizedType,
                        Math.max(0, tierSection.getInt("after-seconds", 0)),
                        material,
                        Math.max(1L, tierSection.getLong("interval-ticks", 20L)),
                        Math.max(1, tierSection.getInt("cap", 16))
                ));
            }
        }

        if (tiers.isEmpty()) {
            tiers.add(defaultGeneratorTier(normalizedType));
        }

        tiers.sort((first, second) -> Integer.compare(first.getAfterSeconds(), second.getAfterSeconds()));
        List<GeneratorTierConfig> immutable = List.copyOf(tiers);
        generatorTierCache.put(normalizedType, immutable);
        return new ArrayList<>(immutable);
    }

    public String getShopVillagerName(String shopType) {
        return config.getString("game.shops." + shopType + ".villager-name",
                shopType.equalsIgnoreCase("upgrade") ? "&bUpgrade-Shop" : "&eItem-Shop");
    }

    public String getShopGuiTitle(String shopType) {
        return config.getString("game.shops." + shopType + ".gui-title",
                shopType.equalsIgnoreCase("upgrade") ? "&8Upgrade-Shop" : "&8Item-Shop");
    }

    public List<ShopCategoryConfig> getShopCategories(String shopType) {
        String normalizedType = inferShopType(shopType);
        List<ShopCategoryConfig> cached = shopCategoryCache.get(normalizedType);
        if (cached != null) {
            return new ArrayList<>(cached);
        }

        ConfigurationSection section = config.getConfigurationSection("game.shops." + normalizedType + ".categories");
        List<ShopCategoryConfig> categories = new ArrayList<>();
        if (section != null) {
            for (String id : section.getKeys(false)) {
                ConfigurationSection categorySection = section.getConfigurationSection(id);
                if (categorySection == null) {
                    continue;
                }
                categories.add(new ShopCategoryConfig(
                        id,
                        categorySection.getString("display-name", id),
                        parseMaterial(categorySection.getString("icon"), Material.CHEST),
                        categorySection.getInt("slot", 0)
                ));
            }
        }
        List<ShopCategoryConfig> defaults = defaultShopCategories(normalizedType);
        if (categories.isEmpty()) {
            categories.addAll(defaults);
        } else {
            LinkedHashSet<String> configuredIds = new LinkedHashSet<>();
            for (ShopCategoryConfig category : categories) {
                configuredIds.add(category.getId().toLowerCase());
            }
            for (ShopCategoryConfig defaultCategory : defaults) {
                if (!configuredIds.contains(defaultCategory.getId().toLowerCase())) {
                    categories.add(defaultCategory);
                }
            }
        }
        categories.sort((first, second) -> Integer.compare(first.getSlot(), second.getSlot()));
        List<ShopCategoryConfig> immutable = List.copyOf(categories);
        shopCategoryCache.put(normalizedType, immutable);
        return new ArrayList<>(immutable);
    }

    public List<ShopOfferConfig> getShopOffers(String shopType) {
        String normalizedType = inferShopType(shopType);
        List<ShopOfferConfig> cached = shopOfferCache.get(normalizedType);
        if (cached != null) {
            return new ArrayList<>(cached);
        }

        ConfigurationSection section = config.getConfigurationSection("game.shops." + normalizedType + ".offers");
        List<ShopOfferConfig> offers = new ArrayList<>();
        if (section != null) {
            for (String id : section.getKeys(false)) {
                ConfigurationSection offerSection = section.getConfigurationSection(id);
                if (offerSection == null) {
                    continue;
                }
                offers.add(new ShopOfferConfig(
                        normalizedType,
                        offerSection.getString("category", normalizedType.equalsIgnoreCase("upgrade") ? "team-upgrades" : "specials"),
                        offerSection.getInt("slot", 10),
                        id,
                        offerSection.getString("display-name", id),
                        parseMaterial(offerSection.getString("icon"), Material.STONE),
                        Math.max(1, offerSection.getInt("amount", 1)),
                        parseMaterial(offerSection.getString("currency"), Material.IRON_INGOT),
                        Math.max(1, offerSection.getInt("cost", 1))
                ));
            }
        }
        List<ShopOfferConfig> defaults = defaultShopOffers(normalizedType);
        if (offers.isEmpty()) {
            offers.addAll(defaults);
        } else {
            LinkedHashSet<String> configuredActions = new LinkedHashSet<>();
            for (ShopOfferConfig offer : offers) {
                configuredActions.add(offer.getAction().toLowerCase());
            }
            boolean hasLegacyTrap = configuredActions.contains("trap");
            for (ShopOfferConfig defaultOffer : defaults) {
                String action = defaultOffer.getAction().toLowerCase();
                if (configuredActions.contains(action)) {
                    continue;
                }
                if (hasLegacyTrap && action.equals("alarm-trap")) {
                    continue;
                }
                offers.add(defaultOffer);
            }
        }
        offers.sort((first, second) -> Integer.compare(first.getSlot(), second.getSlot()));
        List<ShopOfferConfig> immutable = List.copyOf(offers);
        shopOfferCache.put(normalizedType, immutable);
        return new ArrayList<>(immutable);
    }

    public List<String> getMapIds() {
        ConfigurationSection runtimeSection = config.getConfigurationSection("maps");
        ConfigurationSection bundledSection = bundledConfig == null ? null : bundledConfig.getConfigurationSection("maps");
        LinkedHashSet<String> mapIds = new LinkedHashSet<>();
        if (runtimeSection != null) {
            mapIds.addAll(runtimeSection.getKeys(false));
        }
        if (bundledSection != null) {
            mapIds.addAll(bundledSection.getKeys(false));
        }
        if (mapIds.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(mapIds);
    }

    public List<String> getTeamIds(String mapId) {
        ConfigurationSection runtimeSection = config.getConfigurationSection("maps." + mapId + ".teams");
        ConfigurationSection bundledSection = bundledConfig == null ? null : bundledConfig.getConfigurationSection("maps." + mapId + ".teams");
        LinkedHashSet<String> teamIds = new LinkedHashSet<>();
        if (runtimeSection != null) {
            teamIds.addAll(runtimeSection.getKeys(false));
        }
        if (bundledSection != null) {
            teamIds.addAll(bundledSection.getKeys(false));
        }
        if (teamIds.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(teamIds);
    }

    public boolean mapExists(String mapId) {
        return config.isConfigurationSection("maps." + mapId)
                || (bundledConfig != null && bundledConfig.isConfigurationSection("maps." + mapId));
    }

    public boolean teamExists(String mapId, String teamId) {
        return config.isConfigurationSection("maps." + mapId + ".teams." + teamId)
                || (bundledConfig != null && bundledConfig.isConfigurationSection("maps." + mapId + ".teams." + teamId));
    }

    public void saveLobbySpawn(String mapId, Location location) {
        config.set("maps." + mapId + ".lobby-spawn", LocationUtil.toConfigString(location));
        persist();
    }

    public void saveTeamSpawn(String mapId, String teamId, Location location) {
        config.set("maps." + mapId + ".teams." + teamId + ".spawn", LocationUtil.toConfigString(location));
        persist();
    }

    public void saveTeamBed(String mapId, String teamId, Location location) {
        config.set("maps." + mapId + ".teams." + teamId + ".bed", LocationUtil.toConfigString(location));
        persist();
    }

    public void saveTeamGenerator(String mapId, String teamId, Location location) {
        config.set("maps." + mapId + ".teams." + teamId + ".generator", LocationUtil.toConfigString(location));
        persist();
    }

    public void saveGenerator(String mapId, String generatorId, Location location) {
        saveGenerator(mapId, inferGeneratorType(generatorId), generatorId, location);
    }

    public String saveGenerator(String mapId, String generatorType, String generatorIdOrSuffix, Location location) {
        String normalizedType = normalizeGeneratorType(generatorType);
        String configId = buildGeneratorConfigId(normalizedType, generatorIdOrSuffix);
        config.set("maps." + mapId + ".generators." + configId + ".location", LocationUtil.toConfigString(location));
        config.set("maps." + mapId + ".generators." + configId + ".type", normalizedType);
        persist();
        return configId;
    }

    public void saveTeamShop(String mapId, String teamId, String shopType, Location location) {
        String normalizedType = inferShopType(shopType);
        String pathKey = normalizedType.equals("upgrade") ? "upgrade-shop" : "item-shop";
        config.set("maps." + mapId + ".teams." + teamId + "." + pathKey, LocationUtil.toConfigString(location));
        persist();
    }

    public void saveShop(String mapId, String shopId, Location location) {
        config.set("maps." + mapId + ".shops." + shopId + ".location", LocationUtil.toConfigString(location));
        config.set("maps." + mapId + ".shops." + shopId + ".type", inferShopType(shopId));
        persist();
    }

    public MapConfig loadMapConfig(String mapId) {
        ConfigurationSection runtimeMapSection = config.getConfigurationSection("maps." + mapId);
        ConfigurationSection bundledMapSection = bundledConfig == null ? null : bundledConfig.getConfigurationSection("maps." + mapId);

        ConfigurationSection mapSection = runtimeMapSection;
        ConfigurationSection fallbackMapSection = bundledMapSection;
        if (getPluginMode() == PluginMode.GAME && bundledMapSection != null) {
            mapSection = bundledMapSection;
            fallbackMapSection = runtimeMapSection;
            plugin.debug("config", "loadMapConfig -> GAME mode prefers bundled map section for map='" + mapId + "'");
        }

        if (mapSection == null) {
            if (fallbackMapSection != null) {
                plugin.debug("config", "loadMapConfig -> runtime map missing, using bundled fallback for map='" + mapId + "'");
                mapSection = fallbackMapSection;
            } else {
            plugin.debug("config", "loadMapConfig -> map section missing for map='" + mapId + "'");
            return new MapConfig(mapId, null, getQueueMinPlayers(), getQueueMaxPlayers(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
            }
        }

        String rawLobbySpawn = resolveLocationString(
                mapSection.getString("lobby-spawn"),
                fallbackMapSection == null ? null : fallbackMapSection.getString("lobby-spawn")
        );
        plugin.debug("config", "loadMapConfig -> loading map='" + mapId + "' rawLobbySpawn='" + rawLobbySpawn
                + "' runtime='" + mapSection.getString("lobby-spawn") + "' bundled='"
                + (fallbackMapSection == null ? null : fallbackMapSection.getString("lobby-spawn")) + "'");

        List<TeamData> teams = new ArrayList<>();
        ConfigurationSection teamsSection = mapSection.getConfigurationSection("teams");
        ConfigurationSection bundledTeamsSection = fallbackMapSection == null ? null : fallbackMapSection.getConfigurationSection("teams");
        LinkedHashSet<String> teamIds = new LinkedHashSet<>();
        if (teamsSection != null) {
            teamIds.addAll(teamsSection.getKeys(false));
        }
        if (bundledTeamsSection != null) {
            teamIds.addAll(bundledTeamsSection.getKeys(false));
        }
        if (!teamIds.isEmpty()) {
            for (String teamId : teamIds) {
                ConfigurationSection teamSection = teamsSection == null ? null : teamsSection.getConfigurationSection(teamId);
                ConfigurationSection bundledTeamSection = bundledTeamsSection == null ? null : bundledTeamsSection.getConfigurationSection(teamId);
                if (teamSection == null && bundledTeamSection == null) {
                    continue;
                }
                String displayName = getString(teamSection, bundledTeamSection, "display-name", "&7" + teamId);
                int maxPlayers = Math.max(1, getInt(teamSection, bundledTeamSection, "max-players", 1));
                TeamData teamData = new TeamData(
                        teamId,
                        displayName,
                        maxPlayers,
                        LocationUtil.fromConfigString(resolveLocationString(getString(teamSection, null, "spawn", null), getString(bundledTeamSection, null, "spawn", null)), mapId),
                        LocationUtil.fromConfigString(resolveLocationString(getString(teamSection, null, "bed", null), getString(bundledTeamSection, null, "bed", null)), mapId),
                        LocationUtil.fromConfigString(resolveLocationString(getString(teamSection, null, "generator", null), getString(bundledTeamSection, null, "generator", null)), mapId),
                        LocationUtil.fromConfigString(resolveLocationString(getString(teamSection, null, "item-shop", null), getString(bundledTeamSection, null, "item-shop", null)), mapId),
                        LocationUtil.fromConfigString(resolveLocationString(getString(teamSection, null, "upgrade-shop", null), getString(bundledTeamSection, null, "upgrade-shop", null)), mapId)
                );
                teams.add(teamData);
                plugin.debug("config", "loadMapConfig -> team='" + teamId + "' maxPlayers=" + maxPlayers
                        + " spawn=" + plugin.formatLocation(teamData.getSpawn())
                        + " bed=" + plugin.formatLocation(teamData.getBed())
                        + " generator=" + plugin.formatLocation(teamData.getGenerator())
                        + " itemShop=" + plugin.formatLocation(teamData.getItemShop())
                        + " upgradeShop=" + plugin.formatLocation(teamData.getUpgradeShop()));
            }
        }

        List<GeneratorConfig> generators = new ArrayList<>();
        for (TeamData teamData : teams) {
            if (teamData.getGenerator() != null) {
                generators.add(new GeneratorConfig(teamData.getId(), "team", teamData.getGenerator(), true));
            }
        }

        ConfigurationSection generatorSection = mapSection.getConfigurationSection("generators");
        ConfigurationSection bundledGeneratorSection = fallbackMapSection == null ? null : fallbackMapSection.getConfigurationSection("generators");
        LinkedHashSet<String> generatorIds = new LinkedHashSet<>();
        if (generatorSection != null) {
            generatorIds.addAll(generatorSection.getKeys(false));
        }
        if (bundledGeneratorSection != null) {
            generatorIds.addAll(bundledGeneratorSection.getKeys(false));
        }
        if (!generatorIds.isEmpty()) {
            for (String generatorId : generatorIds) {
                ConfigurationSection singleGeneratorSection = generatorSection == null ? null : generatorSection.getConfigurationSection(generatorId);
                ConfigurationSection bundledSingleGeneratorSection = bundledGeneratorSection == null ? null : bundledGeneratorSection.getConfigurationSection(generatorId);
                if (singleGeneratorSection == null && bundledSingleGeneratorSection == null) {
                    continue;
                }
                generators.add(new GeneratorConfig(
                        generatorId,
                        getString(singleGeneratorSection, bundledSingleGeneratorSection, "type", inferGeneratorType(generatorId)),
                        LocationUtil.fromConfigString(resolveLocationString(
                                getString(singleGeneratorSection, null, "location", null),
                                getString(bundledSingleGeneratorSection, null, "location", null)
                        ), mapId),
                        false
                ));
                plugin.debug("config", "loadMapConfig -> generator='" + generatorId + "' type='"
                        + getString(singleGeneratorSection, bundledSingleGeneratorSection, "type", inferGeneratorType(generatorId)) + "' location='"
                        + plugin.formatLocation(generators.getLast().getLocation()) + "'");
            }
        }

        List<ShopConfig> shops = new ArrayList<>();
        for (TeamData teamData : teams) {
            if (teamData.getItemShop() != null) {
                shops.add(new ShopConfig(
                        teamData.getId() + "-itemshop",
                        "item",
                        teamData.getItemShop(),
                        teamData.getId()
                ));
            }
            if (teamData.getUpgradeShop() != null) {
                shops.add(new ShopConfig(
                        teamData.getId() + "-upgradeshop",
                        "upgrade",
                        teamData.getUpgradeShop(),
                        teamData.getId()
                ));
            }
        }

        MapConfig mapConfig = new MapConfig(
                mapId,
                LocationUtil.fromConfigString(rawLobbySpawn, mapId),
                Math.max(1, getInt(mapSection, fallbackMapSection, "min-players", getQueueMinPlayers())),
                Math.max(1, getInt(mapSection, fallbackMapSection, "max-players", getQueueMaxPlayers())),
                teams,
                generators,
                shops
        );
        plugin.debug("config", "loadMapConfig -> completed map='" + mapId + "' lobbySpawn=" + plugin.formatLocation(mapConfig.getLobbySpawn())
                + " minPlayers=" + mapConfig.getMinPlayers() + " maxPlayers=" + mapConfig.getMaxPlayers()
                + " teams=" + mapConfig.getTeams().size() + " generators=" + mapConfig.getGenerators().size()
                + " shops=" + mapConfig.getShops().size());
        return mapConfig;
    }

    private FileConfiguration loadBundledConfig() {
        InputStream stream = plugin.getResource("config.yml");
        if (stream == null) {
            return new YamlConfiguration();
        }
        return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }

    private String resolveLocationString(String runtimeValue, String bundledValue) {
        if (runtimeValue == null || runtimeValue.isBlank()) {
            return bundledValue;
        }
        if (bundledValue != null && !bundledValue.isBlank() && DEFAULT_PLACEHOLDER_SPAWN.equalsIgnoreCase(runtimeValue.trim())) {
            return bundledValue;
        }
        return runtimeValue;
    }

    private String getString(ConfigurationSection primary, ConfigurationSection fallback, String path, String defaultValue) {
        if (primary != null && primary.contains(path)) {
            return primary.getString(path, defaultValue);
        }
        if (fallback != null && fallback.contains(path)) {
            return fallback.getString(path, defaultValue);
        }
        return defaultValue;
    }

    private int getInt(ConfigurationSection primary, ConfigurationSection fallback, String path, int defaultValue) {
        if (primary != null && primary.contains(path)) {
            return primary.getInt(path, defaultValue);
        }
        if (fallback != null && fallback.contains(path)) {
            return fallback.getInt(path, defaultValue);
        }
        return defaultValue;
    }

    private void persist() {
        plugin.saveConfig();
        reload();
    }

    private String getEnvironmentValue(String key) {
        String value = System.getenv(key);
        return value == null ? "" : value;
    }

    private String normalizeGeneratorType(String type) {
        String lowered = type == null ? "iron" : type.toLowerCase();
        if (lowered.contains("emerald")) {
            return "emerald";
        }
        if (lowered.contains("mid")) {
            return "mid";
        }
        if (lowered.contains("diamond")) {
            return "diamond";
        }
        if (lowered.contains("gold")) {
            return "gold";
        }
        if (lowered.contains("team-iron")) {
            return "team-iron";
        }
        if (lowered.contains("team-gold")) {
            return "team-gold";
        }
        if (lowered.contains("team")) {
            return "team-iron";
        }
        return "iron";
    }

    private String buildGeneratorConfigId(String normalizedType, String generatorIdOrSuffix) {
        String raw = generatorIdOrSuffix == null ? "" : generatorIdOrSuffix.trim().toLowerCase();
        if (raw.isBlank()) {
            return normalizedType;
        }
        if (raw.equals(normalizedType) || raw.startsWith(normalizedType + "-")) {
            return raw;
        }
        return normalizedType + "-" + raw;
    }

    private Material defaultGeneratorMaterial(String type) {
        return switch (type) {
            case "gold", "team-gold" -> Material.GOLD_INGOT;
            case "diamond" -> Material.DIAMOND;
            case "emerald", "mid" -> Material.EMERALD;
            default -> Material.IRON_INGOT;
        };
    }

    private GeneratorTierConfig defaultGeneratorTier(String type) {
        return switch (type) {
            case "team-gold" -> new GeneratorTierConfig(type, 0, Material.GOLD_INGOT, 80L, 24);
            case "gold" -> new GeneratorTierConfig(type, 0, Material.GOLD_INGOT, 80L, 24);
            case "diamond" -> new GeneratorTierConfig(type, 0, Material.DIAMOND, 200L, 12);
            case "emerald", "mid" -> new GeneratorTierConfig(type, 0, Material.EMERALD, 400L, 8);
            default -> new GeneratorTierConfig(type, 0, Material.IRON_INGOT, 20L, 48);
        };
    }

    private Material parseMaterial(String input, Material fallback) {
        if (input == null || input.isBlank()) {
            return fallback;
        }
        Material material = Material.matchMaterial(input);
        return material == null ? fallback : material;
    }

    private List<ShopCategoryConfig> defaultShopCategories(String shopType) {
        if (shopType != null && shopType.equalsIgnoreCase("upgrade")) {
            return List.of(
                    new ShopCategoryConfig("team-upgrades", "&bTeam Upgrades", Material.SHIELD, 11),
                    new ShopCategoryConfig("traps", "&dTraps", Material.TRIPWIRE_HOOK, 13),
                    new ShopCategoryConfig("personal-upgrades", "&dEigene Upgrades", Material.NETHER_STAR, 15)
            );
        }
        return List.of(
                new ShopCategoryConfig("blocks", "&eBloecke", Material.WHITE_WOOL, 11),
                new ShopCategoryConfig("weapons", "&cWaffen", Material.IRON_SWORD, 12),
                new ShopCategoryConfig("tools", "&6Tools", Material.IRON_PICKAXE, 13),
                new ShopCategoryConfig("specials", "&dSpecials", Material.TNT, 14),
                new ShopCategoryConfig("abilities", "&bFaehigkeiten", Material.NETHER_STAR, 15)
        );
    }

    private List<ShopOfferConfig> defaultShopOffers(String shopType) {
        if (shopType.equalsIgnoreCase("upgrade")) {
            return List.of(
                    new ShopOfferConfig("upgrade", "team-upgrades", 20, "protection", "&bProtection", Material.IRON_CHESTPLATE, 1, Material.DIAMOND, 3),
                    new ShopOfferConfig("upgrade", "team-upgrades", 21, "sharpness", "&cSharpness", Material.IRON_SWORD, 1, Material.DIAMOND, 4),
                    new ShopOfferConfig("upgrade", "team-upgrades", 22, "haste", "&eHaste", Material.GOLDEN_PICKAXE, 1, Material.DIAMOND, 3),
                    new ShopOfferConfig("upgrade", "team-upgrades", 23, "forge", "&6Forge", Material.FURNACE, 1, Material.DIAMOND, 3),
                    new ShopOfferConfig("upgrade", "traps", 24, "alarm-trap", "&dAlarm Trap", Material.TRIPWIRE_HOOK, 1, Material.DIAMOND, 3),
                    new ShopOfferConfig("upgrade", "team-upgrades", 28, "bow-power", "&cBow Power", Material.BOW, 1, Material.DIAMOND, 5),
                    new ShopOfferConfig("upgrade", "traps", 29, "blind-trap", "&5Blind Trap", Material.INK_SAC, 1, Material.DIAMOND, 3),
                    new ShopOfferConfig("upgrade", "traps", 30, "knock-trap", "&6Knock Trap", Material.WIND_CHARGE, 1, Material.DIAMOND, 4),
                    new ShopOfferConfig("upgrade", "traps", 31, "silence-trap", "&8Silence Trap", Material.ECHO_SHARD, 1, Material.DIAMOND, 4),
                    new ShopOfferConfig("upgrade", "team-upgrades", 32, "bow-punch", "&eBow Punch", Material.ARROW, 1, Material.EMERALD, 6),
                    new ShopOfferConfig("upgrade", "team-upgrades", 33, "bow-flame", "&6Bow Flame", Material.BLAZE_POWDER, 1, Material.EMERALD, 8),
                    new ShopOfferConfig("upgrade", "personal-upgrades", 20, "iron-armor", "&fEisenrustung", Material.IRON_CHESTPLATE, 1, Material.GOLD_INGOT, 20),
                    new ShopOfferConfig("upgrade", "personal-upgrades", 21, "diamond-armor", "&bDiamantrustung", Material.DIAMOND_CHESTPLATE, 1, Material.EMERALD, 7),
                    new ShopOfferConfig("upgrade", "personal-upgrades", 22, "diamond-sword", "&bDiamantschwert", Material.DIAMOND_SWORD, 1, Material.EMERALD, 6),
                    new ShopOfferConfig("upgrade", "personal-upgrades", 23, "dash", "&bDash", Material.FEATHER, 1, Material.GOLD_INGOT, 8),
                    new ShopOfferConfig("upgrade", "personal-upgrades", 24, "berserker", "&cBerserker", Material.BLAZE_POWDER, 1, Material.EMERALD, 3),
                    new ShopOfferConfig("upgrade", "personal-upgrades", 29, "builder", "&6Builder", Material.BRICK, 1, Material.GOLD_INGOT, 6),
                    new ShopOfferConfig("upgrade", "personal-upgrades", 30, "shield", "&bShield", Material.NETHER_STAR, 1, Material.GOLD_INGOT, 9),
                    new ShopOfferConfig("upgrade", "personal-upgrades", 31, "tracker", "&aTracker", Material.COMPASS, 1, Material.EMERALD, 2),
                    new ShopOfferConfig("upgrade", "personal-upgrades", 32, "void-rescue", "&bRettungsfeder", Material.PHANTOM_MEMBRANE, 1, Material.EMERALD, 3)
            );
        }

        return List.of(
                new ShopOfferConfig("item", "blocks", 20, "wool", "&fWolle x16", Material.WHITE_WOOL, 16, Material.IRON_INGOT, 6),
                new ShopOfferConfig("item", "blocks", 21, "oak-planks", "&6Holz x12", Material.OAK_PLANKS, 12, Material.IRON_INGOT, 10),
                new ShopOfferConfig("item", "blocks", 22, "end-stone", "&7Endstein x12", Material.END_STONE, 12, Material.IRON_INGOT, 28),
                new ShopOfferConfig("item", "blocks", 23, "glass", "&bGlas x8", Material.GLASS, 8, Material.IRON_INGOT, 18),
                new ShopOfferConfig("item", "weapons", 20, "stone-sword", "&7Steinschwert", Material.STONE_SWORD, 1, Material.IRON_INGOT, 14),
                new ShopOfferConfig("item", "weapons", 21, "iron-sword", "&fEisenschwert", Material.IRON_SWORD, 1, Material.GOLD_INGOT, 9),
                new ShopOfferConfig("item", "weapons", 22, "bow", "&6Bogen", Material.BOW, 1, Material.GOLD_INGOT, 14),
                new ShopOfferConfig("item", "weapons", 23, "arrows", "&fPfeile x8", Material.ARROW, 8, Material.GOLD_INGOT, 4),
                new ShopOfferConfig("item", "weapons", 24, "fireball", "&cFeuerball", Material.FIRE_CHARGE, 1, Material.GOLD_INGOT, 12),
                new ShopOfferConfig("item", "tools", 20, "pickaxe", "&6Spitzhacke", Material.IRON_PICKAXE, 1, Material.IRON_INGOT, 8),
                new ShopOfferConfig("item", "tools", 21, "axe", "&6Axt", Material.IRON_AXE, 1, Material.IRON_INGOT, 6),
                new ShopOfferConfig("item", "tools", 22, "shears", "&fSchere", Material.SHEARS, 1, Material.IRON_INGOT, 16),
                new ShopOfferConfig("item", "specials", 20, "golden-apple", "&6Goldapfel", Material.GOLDEN_APPLE, 1, Material.GOLD_INGOT, 4),
                new ShopOfferConfig("item", "specials", 21, "tnt", "&cTNT", Material.TNT, 1, Material.GOLD_INGOT, 10),
                new ShopOfferConfig("item", "specials", 22, "ender-pearl", "&aEnderperle", Material.ENDER_PEARL, 1, Material.EMERALD, 5),
                new ShopOfferConfig("item", "specials", 23, "bridge-egg", "&eBridge Egg", Material.EGG, 1, Material.EMERALD, 3),
                new ShopOfferConfig("item", "specials", 24, "knockback-stick", "&dKnockback Stick", Material.STICK, 1, Material.GOLD_INGOT, 8),
                new ShopOfferConfig("item", "specials", 29, "breach-tnt", "&6Breach TNT", Material.TNT, 1, Material.GOLD_INGOT, 14),
                new ShopOfferConfig("item", "specials", 30, "jump-tnt", "&bJump TNT", Material.TNT, 1, Material.GOLD_INGOT, 12),
                new ShopOfferConfig("item", "specials", 31, "cluster-tnt", "&dCluster TNT", Material.TNT, 1, Material.EMERALD, 3),
                new ShopOfferConfig("item", "specials", 32, "sticky-tnt", "&eSticky TNT", Material.TNT, 1, Material.GOLD_INGOT, 16),
                new ShopOfferConfig("item", "abilities", 20, "dash", "&bDash", Material.FEATHER, 1, Material.GOLD_INGOT, 5),
                new ShopOfferConfig("item", "abilities", 21, "berserker", "&cBerserker", Material.BLAZE_POWDER, 1, Material.EMERALD, 2),
                new ShopOfferConfig("item", "abilities", 22, "shield", "&bShield", Material.NETHER_STAR, 1, Material.GOLD_INGOT, 7),
                new ShopOfferConfig("item", "abilities", 23, "builder", "&6Builder", Material.BRICK, 1, Material.GOLD_INGOT, 4),
                new ShopOfferConfig("item", "abilities", 24, "tracker", "&aTracker", Material.COMPASS, 1, Material.EMERALD, 2)
        );
    }

    private String inferGeneratorType(String generatorId) {
        String lowered = generatorId == null ? "" : generatorId.toLowerCase();
        if (lowered.contains("emerald")) {
            return "emerald";
        }
        if (lowered.contains("mid")) {
            return "mid";
        }
        if (lowered.contains("diamond")) {
            return "diamond";
        }
        if (lowered.contains("gold")) {
            return "gold";
        }
        if (lowered.contains("team")) {
            return "team";
        }
        return "iron";
    }

    private String inferShopType(String shopId) {
        String lowered = shopId == null ? "" : shopId.toLowerCase();
        if (lowered.contains("upgrade")) {
            return "upgrade";
        }
        return "item";
    }
}

