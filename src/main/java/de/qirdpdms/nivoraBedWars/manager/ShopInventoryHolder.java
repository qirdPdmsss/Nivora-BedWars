package de.qirdpdms.nivoraBedWars.manager;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ShopInventoryHolder implements InventoryHolder {

    private final String arenaId;
    private final String shopType;
    private final String teamId;
    private String selectedCategoryId;
    private Map<Integer, String> categorySlots;
    private Map<Integer, de.qirdpdms.nivoraBedWars.model.ShopOfferConfig> offerSlots;
    private Inventory inventory;

    public ShopInventoryHolder(String arenaId, String shopType, String teamId, String selectedCategoryId) {
        this.arenaId = arenaId;
        this.shopType = shopType;
        this.teamId = teamId;
        this.selectedCategoryId = selectedCategoryId;
        this.categorySlots = new LinkedHashMap<>();
        this.offerSlots = new LinkedHashMap<>();
    }

    public String getArenaId() {
        return arenaId;
    }

    public String getShopType() {
        return shopType;
    }

    public String getTeamId() {
        return teamId;
    }

    public String getSelectedCategoryId() {
        return selectedCategoryId;
    }

    public void setSelectedCategoryId(String selectedCategoryId) {
        this.selectedCategoryId = selectedCategoryId;
    }

    public Map<Integer, String> getCategorySlots() {
        return Collections.unmodifiableMap(categorySlots);
    }

    public void setCategorySlots(Map<Integer, String> categorySlots) {
        this.categorySlots = new LinkedHashMap<>(categorySlots);
    }

    public Map<Integer, de.qirdpdms.nivoraBedWars.model.ShopOfferConfig> getOfferSlots() {
        return Collections.unmodifiableMap(offerSlots);
    }

    public void setOfferSlots(Map<Integer, de.qirdpdms.nivoraBedWars.model.ShopOfferConfig> offerSlots) {
        this.offerSlots = new LinkedHashMap<>(offerSlots);
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}


