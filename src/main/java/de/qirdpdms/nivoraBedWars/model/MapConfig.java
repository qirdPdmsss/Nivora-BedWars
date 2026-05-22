package de.qirdpdms.nivoraBedWars.model;

import org.bukkit.Location;

import java.util.List;

public class MapConfig {

    private final String id;
    private final Location lobbySpawn;
    private final int minPlayers;
    private final int maxPlayers;
    private final List<TeamData> teams;
    private final List<GeneratorConfig> generators;
    private final List<ShopConfig> shops;

    public MapConfig(String id, Location lobbySpawn, int minPlayers, int maxPlayers, List<TeamData> teams, List<GeneratorConfig> generators, List<ShopConfig> shops) {
        this.id = id;
        this.lobbySpawn = lobbySpawn;
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
        this.teams = teams;
        this.generators = generators;
        this.shops = shops;
    }

    public String getId() {
        return id;
    }

    public Location getLobbySpawn() {
        return lobbySpawn;
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public List<TeamData> getTeams() {
        return teams;
    }

    public List<GeneratorConfig> getGenerators() {
        return generators;
    }

    public List<ShopConfig> getShops() {
        return shops;
    }
}

