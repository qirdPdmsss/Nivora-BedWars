package de.qirdpdms.nivoraBedWars.command;

import de.qirdpdms.nivoraBedWars.Main;
import de.qirdpdms.nivoraBedWars.model.PluginMode;
import de.qirdpdms.nivoraBedWars.util.PermissionNodes;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class QueueCommand implements CommandExecutor {

    private final Main plugin;

    public QueueCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(PermissionNodes.QUEUE)) {
            plugin.getMessageManager().send(sender, "general.no-permission");
            return true;
        }
        if (!(sender instanceof Player player)) {
            plugin.getMessageManager().send(sender, "general.only-player");
            return true;
        }
        if (plugin.getPluginMode() != PluginMode.LOBBY) {
            plugin.getMessageManager().send(sender, "queue.only-lobby");
            return true;
        }

        if (args.length == 0) {
            boolean joined = plugin.getQueueManager().join(player);
            if (!joined) {
                plugin.getMessageManager().send(player, "queue.already");
            }
            return true;
        }

        String sub = args[0].toLowerCase();
        if (sub.equals("join")) {
            if (!plugin.getQueueManager().join(player)) {
                plugin.getMessageManager().send(player, "queue.already");
            }
            return true;
        }
        if (sub.equals("leave")) {
            if (!plugin.getQueueManager().leave(player)) {
                plugin.getMessageManager().send(player, "queue.not-in");
            }
            return true;
        }
        if (sub.equals("status")) {
            plugin.getMessageManager().send(player, "queue.status", Map.of(
                    "value", String.valueOf(plugin.getQueueManager().size())
            ));
            return true;
        }

        plugin.getMessageManager().send(player, "command.queue.usage", Map.of("usage", "/queue <join|leave|status>"));
        return true;
    }
}

