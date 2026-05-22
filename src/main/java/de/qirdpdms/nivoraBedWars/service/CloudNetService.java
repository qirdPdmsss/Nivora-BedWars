package de.qirdpdms.nivoraBedWars.service;

import org.bukkit.entity.Player;

import java.util.List;

public interface CloudNetService {

    String startService(String serviceName, String taskName, String mapId);

    boolean sendPlayer(Player player, String serviceName);

    List<String> getAvailableServices(String taskName);
}

