package de.qirdpdms.nivoraBedWars.service;

import de.qirdpdms.nivoraBedWars.Main;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ReflectiveCloudNetService implements CloudNetService {

    private static final String INJECTION_LAYER_CLASS = "eu.cloudnetservice.driver.inject.InjectionLayer";
    private static final String SERVICE_REGISTRY_CLASS = "eu.cloudnetservice.driver.registry.ServiceRegistry";
    private static final String CLOUD_SERVICE_PROVIDER_CLASS = "eu.cloudnetservice.driver.provider.CloudServiceProvider";
    private static final String SERVICE_TASK_PROVIDER_CLASS = "eu.cloudnetservice.driver.provider.ServiceTaskProvider";
    private static final String PLAYER_MANAGER_CLASS = "eu.cloudnetservice.modules.bridge.player.PlayerManager";

    private final Main plugin;
    private final CloudNetService fallback;
    private final Set<String> emittedWarnings;

    public ReflectiveCloudNetService(Main plugin) {
        this.plugin = plugin;
        this.fallback = new CommandCloudNetService(plugin);
        this.emittedWarnings = new HashSet<>();
    }

    @Override
    public String startService(String serviceName, String taskName, String mapId) {
        if (plugin.getConfigManager().shouldUseStaticServices()) {
            plugin.getLogger().info("[BedWars] Statische Services sind aktiv. Es wird kein neuer CloudNet-Service per API gestartet.");
            return null;
        }

        try {
            Object taskProvider = resolveInstance(SERVICE_TASK_PROVIDER_CLASS);
            Object cloudServiceProvider = resolveInstance(CLOUD_SERVICE_PROVIDER_CLASS);
            if (taskProvider == null || cloudServiceProvider == null) {
                warnOnce("cloudnet.api.start.unavailable", "[BedWars] CloudNet TaskProvider oder CloudServiceProvider nicht verfugbar. Nutze Command-Fallback fur den Start.");
                return fallback.startService(serviceName, taskName, mapId);
            }

            Object serviceTask = invokeIfExists(taskProvider, "serviceTask", new Class[]{String.class}, taskName);
            if (serviceTask == null) {
                warnOnce("cloudnet.api.task.missing." + taskName.toLowerCase(), "[BedWars] Der CloudNet-Task " + taskName + " wurde nicht gefunden. Nutze Command-Fallback fur den Start.");
                return fallback.startService(serviceName, taskName, mapId);
            }

            Object createResult = firstNonNull(
                    invokeCompatibleMethod(cloudServiceProvider, "createCloudService", serviceTask),
                    invokeCompatibleMethod(cloudServiceProvider, "createCloudService", serviceTask, serviceName)
            );
            if (createResult == null) {
                warnOnce("cloudnet.api.start.failed." + taskName.toLowerCase(), "[BedWars] CloudNet konnte fur Task " + taskName + " keinen Service per API erstellen. Nutze Command-Fallback fur den Start.");
                return fallback.startService(serviceName, taskName, mapId);
            }

            Object state = invokeIfExists(createResult, "state", new Class[0]);
            if (state != null && !String.valueOf(state).equalsIgnoreCase("CREATED") && !String.valueOf(state).equalsIgnoreCase("SUCCESS")) {
                plugin.getLogger().warning("[BedWars] CloudNet konnte keinen neuen Service fur Task " + taskName + " erstellen. State=" + state + ". Nutze Command-Fallback.");
                return fallback.startService(serviceName, taskName, mapId);
            }

            Object serviceInfo = firstNonNull(
                    invokeIfExists(createResult, "serviceInfo", new Class[0]),
                    invokeIfExists(createResult, "serviceInfoSnapshot", new Class[0]),
                    createResult
            );
            String createdServiceName = extractServiceName(serviceInfo);
            if (createdServiceName == null || createdServiceName.isBlank()) {
                plugin.getLogger().warning("[BedWars] CloudNet hat einen Service erstellt, aber kein Name konnte gelesen werden. Nutze den angeforderten Namen " + serviceName + ".");
                return serviceName;
            }

            plugin.getLogger().info("[BedWars] CloudNet-API Start erfolgreich: " + createdServiceName + " fur Task " + taskName + " mit Map " + mapId);
            return createdServiceName;
        } catch (Exception exception) {
            plugin.getLogger().warning("[BedWars] CloudNet-API Startfehler: " + exception.getMessage());
            return fallback.startService(serviceName, taskName, mapId);
        }
    }

    @Override
    public boolean sendPlayer(Player player, String serviceName) {
        if (player == null || !player.isOnline()) {
            return false;
        }

        try {
            Object playerManager = resolveInstance(PLAYER_MANAGER_CLASS);
            if (playerManager == null) {
                plugin.getLogger().warning("[BedWars] CloudNet PlayerManager nicht verfugbar, nutze Proxy-Fallback.");
                return fallback.sendPlayer(player, serviceName);
            }

            Object executor = invokeIfExists(playerManager, "playerExecutor", new Class[]{UUID.class}, player.getUniqueId());
            if (executor == null) {
                plugin.getLogger().warning("[BedWars] Kein CloudNet PlayerExecutor fur " + player.getName() + " gefunden, nutze Proxy-Fallback.");
                return fallback.sendPlayer(player, serviceName);
            }

            Object connectResult = invokeIfExists(executor, "connect", new Class[]{String.class}, serviceName);
            if (connectResult instanceof Boolean connected && !connected) {
                plugin.getLogger().warning("[BedWars] CloudNet-API Transfer wurde fur " + player.getName() + " -> " + serviceName + " abgelehnt. Nutze Proxy-Fallback.");
                return fallback.sendPlayer(player, serviceName);
            }
            if (connectResult == null) {
                plugin.getLogger().warning("[BedWars] CloudNet PlayerExecutor.connect(String) nicht verfugbar, nutze Proxy-Fallback.");
                return fallback.sendPlayer(player, serviceName);
            }

            plugin.getLogger().info("[BedWars] CloudNet-API Transfer abgesetzt fur " + player.getName() + " -> " + serviceName);
            return true;
        } catch (Exception exception) {
            plugin.getLogger().warning("[BedWars] CloudNet-API Transferfehler: " + exception.getMessage());
            return fallback.sendPlayer(player, serviceName);
        }
    }

    @Override
    public List<String> getAvailableServices(String taskName) {
        try {
            Object cloudServiceProvider = resolveInstance(CLOUD_SERVICE_PROVIDER_CLASS);
            if (cloudServiceProvider == null) {
                warnOnce("cloudnet.api.services.unavailable", "[BedWars] CloudNet CloudServiceProvider nicht verfugbar.");
                return List.of();
            }

            Object runningServices = firstNonNull(
                    invokeCompatibleMethod(cloudServiceProvider, "servicesByTask", taskName),
                    invokeIfExists(cloudServiceProvider, "servicesByTask", new Class[]{String.class}, taskName),
                    invokeIfExists(cloudServiceProvider, "runningServices", new Class[0]),
                    invokeIfExists(cloudServiceProvider, "services", new Class[0])
            );
            if (!(runningServices instanceof Collection<?> collection)) {
                plugin.getLogger().warning("[BedWars] CloudNet lieferte keine lesbare Service-Collection fur Task " + taskName + ".");
                return List.of();
            }

            List<String> services = new ArrayList<>();
            for (Object snapshot : collection) {
                if (snapshot == null) {
                    continue;
                }
                String snapshotTaskName = extractTaskName(snapshot);
                if (snapshotTaskName != null && !snapshotTaskName.equalsIgnoreCase(taskName)) {
                    continue;
                }
                if (!isConnected(snapshot) || !isRunning(snapshot)) {
                    continue;
                }
                String serviceName = extractServiceName(snapshot);
                if (serviceName != null && !serviceName.isBlank()) {
                    services.add(serviceName);
                }
            }

            List<String> uniqueServices = services.stream().distinct().sorted(Comparator.naturalOrder()).toList();
            if (!uniqueServices.isEmpty()) {
                plugin.getLogger().info("[BedWars] CloudNet-API laufende Services fur Task " + taskName + ": " + uniqueServices);
            } else {
                plugin.getLogger().warning("[BedWars] Es konnten keine laufenden CloudNet-Services fur Task " + taskName + " per API ermittelt werden.");
            }
            return uniqueServices;
        } catch (Exception exception) {
            plugin.getLogger().warning("[BedWars] CloudNet-API Serviceabfrage fehlgeschlagen: " + exception.getMessage());
            return List.of();
        }
    }

    private Object getServiceRegistry() {
        Class<?> serviceRegistryClass = loadClass(SERVICE_REGISTRY_CLASS);
        if (serviceRegistryClass == null) {
            return null;
        }
        Object registryFromExt = resolveFromInjectionLayer(serviceRegistryClass, "ext");
        if (registryFromExt != null) {
            return registryFromExt;
        }
        Object registryFromBoot = resolveFromInjectionLayer(serviceRegistryClass, "boot");
        if (registryFromBoot != null) {
            return registryFromBoot;
        }
        try {
            Method registryMethod = serviceRegistryClass.getMethod("registry");
            return registryMethod.invoke(null);
        } catch (ReflectiveOperationException ignored) {
        }
        try {
            Method instanceMethod = serviceRegistryClass.getMethod("instance");
            return instanceMethod.invoke(null);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private Object resolveInstance(String targetClassName) {
        Class<?> targetClass = loadClass(targetClassName);
        if (targetClass == null) {
            return null;
        }

        Object injectedInstance = resolveFromInjectionLayer(targetClass, "boot");
        if (injectedInstance != null) {
            return injectedInstance;
        }

        injectedInstance = resolveFromInjectionLayer(targetClass, "ext");
        if (injectedInstance != null) {
            return injectedInstance;
        }

        Object serviceRegistry = getServiceRegistry();
        return getDefaultInstance(serviceRegistry, targetClass);
    }

    private Object resolveFromInjectionLayer(Class<?> targetClass, String methodName) {
        Class<?> injectionLayerClass = loadClass(INJECTION_LAYER_CLASS);
        if (injectionLayerClass == null) {
            return null;
        }
        try {
            Method layerMethod = injectionLayerClass.getMethod(methodName);
            Object layer = layerMethod.invoke(null);
            if (layer == null) {
                return null;
            }
            Method instanceMethod = layer.getClass().getMethod("instance", Class.class);
            return instanceMethod.invoke(layer, targetClass);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private Object getDefaultInstance(Object serviceRegistry, Class<?> targetClass) {
        if (serviceRegistry == null || targetClass == null) {
            return null;
        }
        try {
            Method firstProviderMethod = serviceRegistry.getClass().getMethod("firstProvider", Class.class);
            Object provided = firstProviderMethod.invoke(serviceRegistry, targetClass);
            if (provided != null) {
                return provided;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        try {
            Method defaultInstanceMethod = serviceRegistry.getClass().getMethod("defaultInstance", Class.class);
            return defaultInstanceMethod.invoke(serviceRegistry, targetClass);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private boolean isConnected(Object snapshot) {
        Object connected = invokeIfExists(snapshot, "connected", new Class[0]);
        return !(connected instanceof Boolean bool) || bool;
    }

    private boolean isRunning(Object snapshot) {
        Object lifeCycle = invokeIfExists(snapshot, "lifeCycle", new Class[0]);
        return lifeCycle != null && String.valueOf(lifeCycle).equalsIgnoreCase("RUNNING");
    }

    private String extractServiceName(Object snapshot) {
        Object directName = invokeIfExists(snapshot, "name", new Class[0]);
        if (directName != null) {
            return String.valueOf(directName);
        }
        Object serviceId = invokeIfExists(snapshot, "serviceId", new Class[0]);
        Object idName = invokeIfExists(serviceId, "name", new Class[0]);
        return idName == null ? null : String.valueOf(idName);
    }

    private String extractTaskName(Object snapshot) {
        Object serviceId = invokeIfExists(snapshot, "serviceId", new Class[0]);
        Object taskName = invokeIfExists(serviceId, "taskName", new Class[0]);
        return taskName == null ? null : String.valueOf(taskName);
    }

    private void warnOnce(String key, String message) {
        synchronized (emittedWarnings) {
            if (emittedWarnings.add(key)) {
                plugin.getLogger().warning(message);
            }
        }
    }

    private Object invokeIfExists(Object target, String methodName, Class<?>[] parameterTypes, Object... args) {
        if (target == null) {
            return null;
        }
        try {
            Method method = target.getClass().getMethod(methodName, parameterTypes);
            return method.invoke(target, args);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private Object invokeCompatibleMethod(Object target, String methodName, Object... args) {
        if (target == null) {
            return null;
        }
        Method[] methods = target.getClass().getMethods();
        for (Method method : methods) {
            if (!method.getName().equals(methodName) || method.getParameterCount() != args.length) {
                continue;
            }

            Class<?>[] parameterTypes = method.getParameterTypes();
            boolean compatible = true;
            for (int index = 0; index < parameterTypes.length; index++) {
                Object arg = args[index];
                if (arg == null) {
                    continue;
                }
                if (!wrap(parameterTypes[index]).isAssignableFrom(arg.getClass())) {
                    compatible = false;
                    break;
                }
            }

            if (!compatible) {
                continue;
            }

            try {
                return method.invoke(target, args);
            } catch (ReflectiveOperationException ignored) {
                return null;
            }
        }
        return null;
    }

    private Object firstNonNull(Object... values) {
        for (Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private Class<?> loadClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException ignored) {
            return null;
        }
    }

    private Class<?> wrap(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == boolean.class) {
            return Boolean.class;
        }
        if (type == byte.class) {
            return Byte.class;
        }
        if (type == char.class) {
            return Character.class;
        }
        if (type == short.class) {
            return Short.class;
        }
        if (type == int.class) {
            return Integer.class;
        }
        if (type == long.class) {
            return Long.class;
        }
        if (type == float.class) {
            return Float.class;
        }
        if (type == double.class) {
            return Double.class;
        }
        return Void.class;
    }
}

