package de.qirdpdms.nivoraBedWars.model;

import org.bukkit.Material;

public class ShopCategoryConfig {

    private final String id;
    private final String displayName;
    private final Material icon;
    private final int slot;

    public ShopCategoryConfig(String id, String displayName, Material icon, int slot) {
        this.id = id;
        this.displayName = displayName;
        this.icon = icon;
        this.slot = slot;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getIcon() {
        return icon;
    }

    public int getSlot() {
        return slot;
    }
}

