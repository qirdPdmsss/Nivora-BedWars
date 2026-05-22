package de.qirdpdms.nivoraBedWars.command.completer;

import de.qirdpdms.nivoraBedWars.util.PermissionNodes;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;

public class LeaveTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission(PermissionNodes.LEAVE)) {
            return List.of();
        }
        return List.of();
    }
}

