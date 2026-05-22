package de.qirdpdms.nivoraBedWars.model;

import org.bukkit.Location;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class TeamData {

    private final String id;
    private final String displayName;
    private final int maxPlayers;
    private final Location spawn;
    private final Location bed;
    private final Location generator;
    private final Location itemShop;
    private final Location upgradeShop;
    private final Set<UUID> players;
    private boolean bedAlive;

    public TeamData(String id, String displayName, int maxPlayers, Location spawn, Location bed, Location generator,
                    Location itemShop, Location upgradeShop) {
        this.id = id;
        this.displayName = displayName;
        this.maxPlayers = maxPlayers;
        this.spawn = spawn;
        this.bed = bed;
        this.generator = generator;
        this.itemShop = itemShop;
        this.upgradeShop = upgradeShop;
        this.bedAlive = true;
        this.players = new HashSet<>();
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public Location getSpawn() {
        return spawn;
    }

    public Location getBed() {
        return bed;
    }

    public Location getGenerator() {
        return generator;
    }

    public Location getItemShop() {
        return itemShop;
    }

    public Location getUpgradeShop() {
        return upgradeShop;
    }

    public Set<UUID> getPlayers() {
        return players;
    }

    public boolean isBedAlive() {
        return bedAlive;
    }

    public void setBedAlive(boolean bedAlive) {
        this.bedAlive = bedAlive;
    }

    public boolean hasSpace() {
        return players.size() < maxPlayers;
    }
}

