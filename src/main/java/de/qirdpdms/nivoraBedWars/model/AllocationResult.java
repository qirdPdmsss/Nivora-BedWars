package de.qirdpdms.nivoraBedWars.model;

public class AllocationResult {

    private final String serviceName;
    private final String mapId;

    public AllocationResult(String serviceName, String mapId) {
        this.serviceName = serviceName;
        this.mapId = mapId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getMapId() {
        return mapId;
    }
}

