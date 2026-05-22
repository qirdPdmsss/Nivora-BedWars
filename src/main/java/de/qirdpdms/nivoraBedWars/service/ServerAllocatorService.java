package de.qirdpdms.nivoraBedWars.service;

import de.qirdpdms.nivoraBedWars.Main;
import de.qirdpdms.nivoraBedWars.model.AllocationResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class ServerAllocatorService {

    private final Main plugin;
    private final AtomicInteger serviceCounter;
    private final AtomicInteger mapCounter;
    private final Map<String, Integer> reservations;
    private final Map<String, String> mapByService;
    private final Set<String> dynamicServices;

    public ServerAllocatorService(Main plugin) {
        this.plugin = plugin;
        this.serviceCounter = new AtomicInteger(1);
        this.mapCounter = new AtomicInteger(0);
        this.reservations = new LinkedHashMap<>();
        this.mapByService = new LinkedHashMap<>();
        this.dynamicServices = new HashSet<>();
    }

    public synchronized AllocationResult allocate(int playerCount) {
        String taskName = plugin.getConfigManager().getQueueTaskName();
        List<String> configuredServices = plugin.getConfigManager().getStaticServices();
        List<String> availableServices = plugin.getCloudNetService().getAvailableServices(taskName);
        boolean staticRouting = plugin.getConfigManager().shouldUseStaticServices();
        boolean useConfiguredServicesAsFallback = availableServices.isEmpty() && !configuredServices.isEmpty();

        if (useConfiguredServicesAsFallback) {
            plugin.getLogger().info("[BedWars] Keine CloudNet-Service-Liste verfugbar. Nutze konfigurierte statische Services als Fallback: " + configuredServices);
            availableServices = new ArrayList<>(configuredServices);
        }

        for (String service : availableServices) {
            reservations.putIfAbsent(service, 0);
        }

        int maxPlayers = plugin.getConfigManager().getServiceMaxPlayers();
        List<String> orderedCandidates = new ArrayList<>();
        for (String service : configuredServices) {
            if (availableServices.contains(service)) {
                orderedCandidates.add(service);
            }
        }
        for (String service : availableServices) {
            if (!orderedCandidates.contains(service)) {
                orderedCandidates.add(service);
            }
        }

        orderedCandidates.sort((first, second) -> {
            int reservationCompare = Integer.compare(
                    reservations.getOrDefault(first, 0),
                    reservations.getOrDefault(second, 0)
            );
            if (reservationCompare != 0) {
                return reservationCompare;
            }

            int firstConfiguredIndex = configuredServices.indexOf(first);
            int secondConfiguredIndex = configuredServices.indexOf(second);
            if (firstConfiguredIndex < 0) {
                firstConfiguredIndex = Integer.MAX_VALUE;
            }
            if (secondConfiguredIndex < 0) {
                secondConfiguredIndex = Integer.MAX_VALUE;
            }
            return Integer.compare(firstConfiguredIndex, secondConfiguredIndex);
        });

        for (String service : orderedCandidates) {
            int reserved = reservations.getOrDefault(service, 0);
            if (reserved + playerCount <= maxPlayers) {
                reservations.put(service, reserved + playerCount);
                String mapId = mapByService.computeIfAbsent(service, this::pickMapId);
                plugin.getLogger().info("[BedWars] Verwende verfugbaren Service " + service + " mit Map " + mapId);
                return new AllocationResult(service, mapId);
            }
        }

        if (staticRouting) {
            if (configuredServices.isEmpty()) {
                plugin.getLogger().warning("[BedWars] Statischer Service-Modus ist aktiv, aber `cloudnet.static-services` ist leer. Trage dort deine echten Service-Namen ein, z.B. Bedwars-1-1, Bedwars-2-1, Bedwars-3-1.");
            } else {
                plugin.getLogger().warning("[BedWars] Alle statischen BedWars-Services sind aktuell belegt oder die konfigurierten Namen stimmen nicht. Es wird kein dynamischer Service gestartet.");
            }
            return null;
        }

        String serviceName = taskName.toLowerCase() + "-" + serviceCounter.getAndIncrement();
        String mapId = pickMapId(serviceName);
        String createdServiceName = plugin.getCloudNetService().startService(serviceName, taskName, mapId);
        if (createdServiceName != null && !createdServiceName.isBlank()) {
            reservations.put(createdServiceName, playerCount);
            mapByService.put(createdServiceName, mapId);
            dynamicServices.add(createdServiceName);
            plugin.getLogger().info("[BedWars] Neuer Service gestartet: " + createdServiceName + " mit Map " + mapId);
            return new AllocationResult(createdServiceName, mapId);
        }

        if (useConfiguredServicesAsFallback) {
            plugin.getLogger().warning("[BedWars] Statische Services sind bereits ausgelastet oder nicht erreichbar. Ein neuer Service konnte ebenfalls nicht gestartet werden.");
        }
        plugin.getLogger().warning("[BedWars] Es konnte kein neuer Service gestartet werden und es sind keine laufenden Services verfugbar.");
        return null;
    }

    public synchronized void release(String service, int amount) {
        if (service == null || service.isBlank()) {
            return;
        }
        int current = reservations.getOrDefault(service, 0);
        int next = Math.max(0, current - amount);
        if (next == 0) {
            if (dynamicServices.contains(service)) {
                reservations.remove(service);
                dynamicServices.remove(service);
            } else {
                reservations.put(service, 0);
            }
            mapByService.remove(service);
        } else {
            reservations.put(service, next);
        }
    }

    private String pickMapId(String serviceName) {
        String configuredMap = plugin.getConfigManager().getConfiguredMapForService(serviceName);
        if (configuredMap != null && !configuredMap.isBlank()) {
            return configuredMap;
        }

        List<String> mapRotation = plugin.getConfigManager().getMapRotation();
        if (mapRotation.isEmpty()) {
            return "bw_map1";
        }
        int index = Math.abs(mapCounter.getAndIncrement()) % mapRotation.size();
        return mapRotation.get(index);
    }
}

