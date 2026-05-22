package de.qirdpdms.nivoraBedWars.model;

import org.bukkit.Material;

public class ShopOfferConfig {

    private final String shopType;
    private final String category;
    private final int slot;
    private final String action;
    private final String displayName;
    private final Material icon;
    private final int amount;
    private final Material currency;
    private final int cost;

    public ShopOfferConfig(String shopType, String category, int slot, String action, String displayName, Material icon, int amount, Material currency, int cost) {
        this.shopType = shopType;
        this.category = category;
        this.slot = slot;
        this.action = action;
        this.displayName = displayName;
        this.icon = icon;
        this.amount = amount;
        this.currency = currency;
        this.cost = cost;
    }

    public String getShopType() {
        return shopType;
    }

    public String getCategory() {
        return category;
    }

    public int getSlot() {
        return slot;
    }

    public String getAction() {
        return action;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getIcon() {
        return icon;
    }

    public int getAmount() {
        return amount;
    }

    public Material getCurrency() {
        return currency;
    }

    public int getCost() {
        return cost;
    }
}

