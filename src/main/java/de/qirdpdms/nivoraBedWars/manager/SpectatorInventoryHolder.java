package de.qirdpdms.nivoraBedWars.manager;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class SpectatorInventoryHolder implements InventoryHolder {

    private final String arenaId;
    private final Map<Integer, UUID> playerSlots;
    private final Map<Integer, String> teamSlots;
    private int page;
    private int maxPage;
    private Inventory inventory;

    public SpectatorInventoryHolder(String arenaId) {
        this.arenaId = arenaId;
        this.playerSlots = new LinkedHashMap<>();
        this.teamSlots = new LinkedHashMap<>();
        this.page = 0;
        this.maxPage = 0;
    }

    public String getArenaId() {
        return arenaId;
    }

    public Map<Integer, UUID> getPlayerSlots() {
        return Collections.unmodifiableMap(playerSlots);
    }

    public void setPlayerSlots(Map<Integer, UUID> playerSlots) {
        this.playerSlots.clear();
        this.playerSlots.putAll(playerSlots);
    }

    public Map<Integer, String> getTeamSlots() {
        return Collections.unmodifiableMap(teamSlots);
    }

    public void setTeamSlots(Map<Integer, String> teamSlots) {
        this.teamSlots.clear();
        this.teamSlots.putAll(teamSlots);
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = Math.max(0, page);
    }

    public int getMaxPage() {
        return maxPage;
    }

    public void setMaxPage(int maxPage) {
        this.maxPage = Math.max(0, maxPage);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return Objects.requireNonNull(inventory, "Inventory wurde noch nicht gesetzt.");
    }
}

