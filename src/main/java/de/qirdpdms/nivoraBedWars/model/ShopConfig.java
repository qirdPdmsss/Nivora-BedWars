package de.qirdpdms.nivoraBedWars.model;

import org.bukkit.Location;

public class ShopConfig {

    private final String id;
    private final String type;
    private final Location location;
    private final String teamId;

    public ShopConfig(String id, String type, Location location, String teamId) {
        this.id = id;
        this.type = type;
        this.location = location;
        this.teamId = teamId;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public Location getLocation() {
        return location;
    }

    public String getTeamId() {
        return teamId;
    }
}

