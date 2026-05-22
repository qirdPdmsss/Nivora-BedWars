package de.qirdpdms.nivoraBedWars.model;

import org.bukkit.Location;

public class GeneratorConfig {

    private final String id;
    private final String type;
    private final Location location;
    private final boolean teamGenerator;

    public GeneratorConfig(String id, String type, Location location, boolean teamGenerator) {
        this.id = id;
        this.type = type;
        this.location = location;
        this.teamGenerator = teamGenerator;
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

    public boolean isTeamGenerator() {
        return teamGenerator;
    }
}

