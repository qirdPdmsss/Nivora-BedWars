package de.qirdpdms.nivoraBedWars.model;

import org.bukkit.Material;

public class GeneratorTierConfig {

    private final String type;
    private final int afterSeconds;
    private final Material material;
    private final long intervalTicks;
    private final int itemCap;

    public GeneratorTierConfig(String type, int afterSeconds, Material material, long intervalTicks, int itemCap) {
        this.type = type;
        this.afterSeconds = afterSeconds;
        this.material = material;
        this.intervalTicks = intervalTicks;
        this.itemCap = itemCap;
    }

    public String getType() {
        return type;
    }

    public int getAfterSeconds() {
        return afterSeconds;
    }

    public Material getMaterial() {
        return material;
    }

    public long getIntervalTicks() {
        return intervalTicks;
    }

    public int getItemCap() {
        return itemCap;
    }
}

