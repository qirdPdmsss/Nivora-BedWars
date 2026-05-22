package de.qirdpdms.nivoraBedWars.service;

import de.qirdpdms.nivoraBedWars.Main;
import org.bukkit.entity.Player;

import java.util.List;

public class TypedCloudNetService implements CloudNetService {

    private final ReflectiveCloudNetService delegate;

    public TypedCloudNetService(Main plugin) {
        this.delegate = new ReflectiveCloudNetService(plugin);
    }

    @Override
    public String startService(String serviceName, String taskName, String mapId) {
        return delegate.startService(serviceName, taskName, mapId);
    }

    @Override
    public boolean sendPlayer(Player player, String serviceName) {
        return delegate.sendPlayer(player, serviceName);
    }

    @Override
    public List<String> getAvailableServices(String taskName) {
        return delegate.getAvailableServices(taskName);
    }
}
