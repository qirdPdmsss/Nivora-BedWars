package de.qirdpdms.nivoraBedWars.service;

import de.qirdpdms.nivoraBedWars.Main;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class CommandCloudNetService implements CloudNetService {

    private final Main plugin;
    private boolean startCommandAvailable = true;

    public CommandCloudNetService(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public String startService(String serviceName, String taskName, String mapId) {
        if (plugin.getConfigManager().shouldUseStaticServices()) {
            plugin.getLogger().info("[BedWars] Statischer Service-Modus aktiv. Es wird kein neuer Service per Konsole gestartet.");
            return null;
        }

        if (!startCommandAvailable) {
            plugin.getLogger().warning("[BedWars] Der konfigurierte CloudNet-Startbefehl ist auf diesem Server nicht verfugbar. Uberspringe erneuten Startversuch.");
            return null;
        }

        String template = plugin.getConfigManager().getStartCommandTemplate();
        if (template == null || template.isBlank()) {
            plugin.getLogger().warning("[BedWars] cloudnet.commands.start ist nicht konfiguriert!");
            return null;
        }
        String command = apply(template, Map.of(
                "service", serviceName,
                "task", taskName,
                "map", mapId
        ));
        plugin.getLogger().info("[BedWars] Starte Service via Konsole: " + command);
        boolean result = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        if (!result) {
            startCommandAvailable = false;
            plugin.getLogger().warning("[BedWars] Der Befehl `" + command + "` wurde vom Bukkit-Server nicht erkannt. Dynamische CloudNet-Starts per Konsole sind hier nicht verfugbar.");
            plugin.getLogger().warning("[BedWars] Befehl nicht erkannt oder fehlgeschlagen: " + command);
            return null;
        }
        return serviceName;
    }

    @Override
    public boolean sendPlayer(Player player, String serviceName) {
        if (player == null || !player.isOnline()) {
            plugin.getLogger().warning("[BedWars] Spielertransfer abgebrochen, weil der Spieler nicht online ist.");
            return false;
        }

        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
            dataOutputStream.writeUTF("Connect");
            dataOutputStream.writeUTF(serviceName);
            player.sendPluginMessage(plugin, "BungeeCord", byteArrayOutputStream.toByteArray());
            plugin.getLogger().info("[BedWars] Proxy-Transfer abgesetzt für " + player.getName() + " -> " + serviceName);
            return true;
        } catch (IOException exception) {
            plugin.getLogger().warning("[BedWars] Proxy-Transfer für " + player.getName() + " nach " + serviceName + " fehlgeschlagen: " + exception.getMessage());
            return false;
        }
    }

    @Override
    public List<String> getAvailableServices(String taskName) {
        return plugin.getConfigManager().getStaticServices();
    }

    private String apply(String template, Map<String, String> placeholders) {
        String result = template;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }
}

