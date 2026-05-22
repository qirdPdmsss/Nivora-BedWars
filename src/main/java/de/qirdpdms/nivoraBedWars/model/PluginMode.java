package de.qirdpdms.nivoraBedWars.model;

public enum PluginMode {
    LOBBY,
    GAME;

    public static PluginMode fromString(String input) {
        if (input == null) {
            return LOBBY;
        }
        try {
            return PluginMode.valueOf(input.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return LOBBY;
        }
    }
}

