package de.qirdpdms.nivoraBedWars.command.completer;

import de.qirdpdms.nivoraBedWars.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class TestTabCompleter implements TabCompleter {

    private static final List<String> SUBS = List.of("start", "stop", "destroybed", "kit", "help");
    private static final List<String> TEAMS = List.of("green", "blue", "red", "yellow", "aqua", "white", "pink", "gray");
    private final Main plugin;

    public TestTabCompleter(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(SUBS, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("start")) {
            return filter(plugin.getConfigManager().getArenaMapIds(), args[1]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("destroybed") || args[0].equalsIgnoreCase("bed"))) {
            return filter(TEAMS, args[1]);
        }
        return List.of();
    }

    private List<String> filter(List<String> list, String prefix) {
        List<String> result = new ArrayList<>();
        for (String s : list) {
            if (s.startsWith(prefix.toLowerCase())) {
                result.add(s);
            }
        }
        return result;
    }
}

