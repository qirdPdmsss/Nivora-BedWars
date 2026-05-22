package de.qirdpdms.nivoraBedWars.command;

import de.qirdpdms.nivoraBedWars.Main;
import de.qirdpdms.nivoraBedWars.model.PluginMode;
import de.qirdpdms.nivoraBedWars.util.PermissionNodes;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LeaveCommand implements CommandExecutor {

    private final Main plugin;

    public LeaveCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(PermissionNodes.LEAVE)) {
            plugin.getMessageManager().send(sender, "general.no-permission");
            return true;
        }
        if (!(sender instanceof Player player)) {
            plugin.getMessageManager().send(sender, "general.only-player");
            return true;
        }

        if (plugin.getPluginMode() == PluginMode.LOBBY) {
            if (!plugin.getQueueManager().leave(player)) {
                plugin.getMessageManager().send(player, "queue.not-in");
            }
            return true;
        }

        player.kickPlayer(plugin.getMessageManager().get("game.leave", false));
        return true;
    }
}

