package de.qirdpdms.nivoraBedWars.command.completer;

import de.qirdpdms.nivoraBedWars.util.PermissionNodes;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;

public class QueueTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission(PermissionNodes.QUEUE)) {
            return List.of();
        }
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            return List.of("join", "leave", "status").stream().filter(s -> s.startsWith(input)).toList();
        }
        return List.of();
    }
}

