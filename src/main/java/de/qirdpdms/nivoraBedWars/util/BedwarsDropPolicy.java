package de.qirdpdms.nivoraBedWars.util;

import de.qirdpdms.nivoraBedWars.file.ConfigManager;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.inventory.ItemStack;

public final class BedwarsDropPolicy {

    private BedwarsDropPolicy() {
    }

    public static boolean isAllowedGroundDrop(ConfigManager configManager, ItemStack itemStack) {
        if (itemStack == null) {
            return false;
        }
        return isAllowedGroundDrop(configManager, itemStack.getType());
    }

    public static boolean isAllowedGroundDrop(ConfigManager configManager, Material material) {
        if (material == null || material == Material.AIR) {
            return false;
        }

        if (configManager == null) {
            return Tag.WOOL.isTagged(material)
                    || material == Material.END_STONE
                    || material == Material.TNT
                    || material == Material.IRON_INGOT
                    || material == Material.GOLD_INGOT
                    || material == Material.DIAMOND
                    || material == Material.EMERALD;
        }

        for (String entry : configManager.getGroundDropWhitelist()) {
            if (matches(material, entry)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matches(Material material, String entry) {
        if (entry == null || entry.isBlank()) {
            return false;
        }
        String normalized = entry.trim().toUpperCase();
        return switch (normalized) {
            case "#WOOL" -> Tag.WOOL.isTagged(material);
            default -> {
                Material configured = Material.matchMaterial(normalized);
                yield configured == material;
            }
        };
    }
}
