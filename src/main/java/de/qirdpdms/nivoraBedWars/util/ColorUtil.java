package de.qirdpdms.nivoraBedWars.util;

import org.bukkit.ChatColor;

public final class ColorUtil {

    private ColorUtil() {
    }

    public static String colorize(String input) {
        if (input == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', input);
    }
}

