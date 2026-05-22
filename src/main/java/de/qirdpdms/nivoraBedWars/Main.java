package de.qirdpdms.nivoraBedWars;

import de.qirdpdms.nivoraBedWars.command.CommandRegistrar;
import de.qirdpdms.nivoraBedWars.file.ConfigManager;
import de.qirdpdms.nivoraBedWars.file.MessageManager;
import de.qirdpdms.nivoraBedWars.listener.BlockBreakListener;
import de.qirdpdms.nivoraBedWars.listener.BlockPlaceListener;
import de.qirdpdms.nivoraBedWars.listener.CraftingBlockListener;
import de.qirdpdms.nivoraBedWars.listener.EntityDamageListener;
import de.qirdpdms.nivoraBedWars.listener.EntityExplodeListener;
import de.qirdpdms.nivoraBedWars.listener.FireProtectionListener;
import de.qirdpdms.nivoraBedWars.listener.FoodLevelListener;
import de.qirdpdms.nivoraBedWars.listener.InventoryClickListener;
import de.qirdpdms.nivoraBedWars.listener.PlayerBedEnterListener;
import de.qirdpdms.nivoraBedWars.listener.PlayerDeathListener;
import de.qirdpdms.nivoraBedWars.listener.PlayerDropItemListener;
import de.qirdpdms.nivoraBedWars.listener.PlayerInteractEntityListener;
import de.qirdpdms.nivoraBedWars.listener.PlayerInteractListener;
import de.qirdpdms.nivoraBedWars.listener.PlayerJoinListener;
import de.qirdpdms.nivoraBedWars.listener.PlayerQuitListener;
import de.qirdpdms.nivoraBedWars.listener.PlayerRespawnListener;
import de.qirdpdms.nivoraBedWars.listener.PlayerTeleportListener;
import de.qirdpdms.nivoraBedWars.listener.ProjectileHitListener;
import de.qirdpdms.nivoraBedWars.manager.GameManager;
import de.qirdpdms.nivoraBedWars.manager.QueueManager;
import de.qirdpdms.nivoraBedWars.model.PluginMode;
import de.qirdpdms.nivoraBedWars.service.CloudNetService;
import de.qirdpdms.nivoraBedWars.service.ReflectiveCloudNetService;
import de.qirdpdms.nivoraBedWars.service.ServerAllocatorService;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Main extends JavaPlugin {

    private ConfigManager configManager;
    private MessageManager messageManager;
    private CloudNetService cloudNetService;
    private ServerAllocatorService serverAllocatorService;
    private QueueManager queueManager;
    private GameManager gameManager;
    private final Set<UUID> buildModePlayers = ConcurrentHashMap.newKeySet();
    private NamespacedKey bridgeEggKey;
    private NamespacedKey arenaFireballKey;
    private NamespacedKey tntVariantKey;
    private NamespacedKey tntOwnerKey;
    private NamespacedKey tntClusterGenerationKey;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResourceIfMissing("messages.yml");
        this.bridgeEggKey = new NamespacedKey(this, "bridge-egg");
        this.arenaFireballKey = new NamespacedKey(this, "arena-fireball");
        this.tntVariantKey = new NamespacedKey(this, "tnt-variant");
        this.tntOwnerKey = new NamespacedKey(this, "tnt-owner");
        this.tntClusterGenerationKey = new NamespacedKey(this, "tnt-cluster-generation");
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        this.configManager = new ConfigManager(this);
        this.messageManager = new MessageManager(this);
        this.cloudNetService = new ReflectiveCloudNetService(this);
        this.serverAllocatorService = new ServerAllocatorService(this);
        this.queueManager = new QueueManager(this);
        this.gameManager = new GameManager(this);

        new CommandRegistrar(this).register();
        registerListeners();

        PluginMode mode = getPluginMode();
        getLogger().info("[BedWars] Modus: " + mode.name());
        getLogger().info("[BedWars] Service-Name: " + configManager.getCurrentServiceName());
        getLogger().info("[BedWars] Geladene Config: " + new java.io.File(getDataFolder(), "config.yml").getAbsolutePath());
        getLogger().info("[BedWars] CloudNet-Start-Template: " + configManager.getStartCommandTemplate());
        getLogger().info("[BedWars] CloudNet-Connect-Template: " + configManager.getConnectCommandTemplate());
        getLogger().info("[BedWars] Statische Services aktiv: " + configManager.shouldUseStaticServices());
        getLogger().info("[BedWars] Konfigurierte statische Services: " + configManager.getStaticServices());
        getLogger().info("[BedWars] Min-Spieler: " + configManager.getQueueMinPlayers() + " | Max-Spieler: " + configManager.getQueueMaxPlayers());
        debug("boot", "debug=" + configManager.isDebugEnabled()
                + " service=" + configManager.getCurrentServiceName()
                + " gameMapId=" + configManager.getGameMapId()
                + " loadedWorlds=" + getServer().getWorlds().stream().map(world -> world.getName()).toList());

        if (mode == PluginMode.LOBBY) {
            getLogger().info("[BedWars] Queue gestartet. Warte auf Spieler...");
            queueManager.start();
        } else {
            getLogger().info("[BedWars] GameManager gestartet. Arenen: " + configManager.getArenaMapIds());
            gameManager.start();
        }
    }

    @Override
    public void onDisable() {
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
        buildModePlayers.clear();
        if (queueManager != null) {
            queueManager.stop();
        }
        if (gameManager != null) {
            gameManager.stop();
        }
    }

    public void reloadPlugin() {
        reloadConfig();
        configManager.reload();
        messageManager.reload();
        queueManager.reload();
        gameManager.reload();
    }

    private void registerListeners() {
        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new CraftingBlockListener(), this);
        pluginManager.registerEvents(new PlayerJoinListener(this), this);
        pluginManager.registerEvents(new PlayerQuitListener(this), this);
        pluginManager.registerEvents(new PlayerInteractListener(this), this);

        if (getPluginMode() == PluginMode.LOBBY) {
        } else {
            pluginManager.registerEvents(new BlockBreakListener(this), this);
            pluginManager.registerEvents(new BlockPlaceListener(this), this);
            pluginManager.registerEvents(new PlayerBedEnterListener(this), this);
            pluginManager.registerEvents(new PlayerDeathListener(this), this);
            pluginManager.registerEvents(new PlayerRespawnListener(this), this);
            pluginManager.registerEvents(new PlayerTeleportListener(this), this);
            pluginManager.registerEvents(new EntityDamageListener(this), this);
            pluginManager.registerEvents(new FireProtectionListener(this), this);
            pluginManager.registerEvents(new FoodLevelListener(this), this);
            pluginManager.registerEvents(new PlayerDropItemListener(this), this);
            pluginManager.registerEvents(new PlayerInteractEntityListener(this), this);
            pluginManager.registerEvents(new InventoryClickListener(this), this);
            pluginManager.registerEvents(new EntityExplodeListener(this), this);
            pluginManager.registerEvents(new ProjectileHitListener(this), this);
        }
    }

    private void saveResourceIfMissing(String path) {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        java.io.File file = new java.io.File(getDataFolder(), path);
        if (!file.exists()) {
            saveResource(path, false);
        }
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public CloudNetService getCloudNetService() {
        return cloudNetService;
    }

    public ServerAllocatorService getServerAllocatorService() {
        return serverAllocatorService;
    }

    public QueueManager getQueueManager() {
        return queueManager;
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public void handleShopEntityInteract(PlayerInteractEntityEvent event) {
        if (gameManager != null) {
            gameManager.handleEntityInteract(event);
        }
    }

    public void handleShopInventoryClick(InventoryClickEvent event) {
        if (gameManager != null) {
            gameManager.handleInventoryClick(event);
        }
    }

    public void handleExplosion(EntityExplodeEvent event) {
        if (gameManager != null) {
            gameManager.handleExplosion(event);
        }
    }

    public void handleProjectileHit(ProjectileHitEvent event) {
        if (gameManager != null) {
            gameManager.handleProjectileHit(event);
        }
    }

    public PluginMode getPluginMode() {
        return configManager.getPluginMode();
    }

    public boolean isBuildMode(UUID uniqueId) {
        return uniqueId != null && buildModePlayers.contains(uniqueId);
    }

    public boolean setBuildMode(UUID uniqueId, boolean enabled) {
        if (uniqueId == null) {
            return false;
        }
        if (enabled) {
            return buildModePlayers.add(uniqueId);
        }
        return buildModePlayers.remove(uniqueId);
    }

    public boolean toggleBuildMode(UUID uniqueId) {
        boolean enabled = !isBuildMode(uniqueId);
        setBuildMode(uniqueId, enabled);
        return enabled;
    }

    public void debug(String area, String message) {
        if (configManager == null || !configManager.isDebugEnabled()) {
            return;
        }
        getLogger().info("[BedWars][DEBUG][" + area + "] " + message);
    }

    public String formatLocation(Location location) {
        if (location == null) {
            return "null";
        }
        String worldName = location.getWorld() == null ? "null-world" : location.getWorld().getName();
        return worldName + ","
                + location.getX() + ","
                + location.getY() + ","
                + location.getZ() + ","
                + location.getYaw() + ","
                + location.getPitch();
    }

    public NamespacedKey getBridgeEggKey() {
        return bridgeEggKey;
    }

    public NamespacedKey getArenaFireballKey() {
        return arenaFireballKey;
    }


    public NamespacedKey getTntVariantKey() {
        return tntVariantKey;
    }

    public NamespacedKey getTntOwnerKey() {
        return tntOwnerKey;
    }

    public NamespacedKey getTntClusterGenerationKey() {
        return tntClusterGenerationKey;
    }
}
