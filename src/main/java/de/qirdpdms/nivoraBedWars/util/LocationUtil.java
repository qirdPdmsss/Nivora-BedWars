package de.qirdpdms.nivoraBedWars.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Locale;

public final class LocationUtil {

    private LocationUtil() {
    }

    public static Location fromConfigString(String input) {
        return fromConfigString(input, null);
    }

    public static Location fromConfigString(String input, String preferredWorldName) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String[] split = input.split(",");
        if (split.length < 4) {
            return null;
        }

        World world = resolveWorld(split[0].trim(), preferredWorldName);
        if (world == null) {
            return null;
        }

        double x = parseDouble(split[1]);
        double y = parseDouble(split[2]);
        double z = parseDouble(split[3]);
        float yaw = split.length >= 5 ? (float) parseDouble(split[4]) : 0.0F;
        float pitch = split.length >= 6 ? (float) parseDouble(split[5]) : 0.0F;
        return new Location(world, x, y, z, yaw, pitch);
    }

    private static World resolveWorld(String configuredName, String preferredWorldName) {
        World preferred = null;
        if (preferredWorldName != null && !preferredWorldName.isBlank()) {
            preferred = Bukkit.getWorld(preferredWorldName);
            if (preferred == null) {
                for (World world : Bukkit.getWorlds()) {
                    if (world.getName().equalsIgnoreCase(preferredWorldName)) {
                        preferred = world;
                        break;
                    }
                }
            }
        }

        if (configuredName == null || configuredName.isBlank()) {
            return preferred != null ? preferred : (Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().getFirst());
        }

        String trimmed = configuredName.trim();
        String normalized = trimmed.toLowerCase(Locale.ROOT);
        if (preferred != null && (normalized.equals("world") || normalized.equals("map") || normalized.equals("arena"))) {
            return preferred;
        }

        World exact = Bukkit.getWorld(trimmed);
        if (exact != null) {
            return exact;
        }

        for (World world : Bukkit.getWorlds()) {
            if (world.getName().equalsIgnoreCase(trimmed)) {
                return world;
            }
        }

        if (preferred != null) {
            return preferred;
        }

        if (normalized.equals("world") || normalized.equals("map") || normalized.startsWith("world_")) {
            for (World world : Bukkit.getWorlds()) {
                if (world.getEnvironment() == World.Environment.NORMAL) {
                    return world;
                }
            }
        }

        return Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().getFirst();
    }

    public static String toConfigString(Location location) {
        if (location == null || location.getWorld() == null) {
            return "";
        }
        return location.getWorld().getName() + "," + location.getX() + "," + location.getY() + "," + location.getZ() + "," + location.getYaw() + "," + location.getPitch();
    }

    private static double parseDouble(String input) {
        try {
            return Double.parseDouble(input.trim());
        } catch (NumberFormatException ignored) {
            return 0.0D;
        }
    }
}

