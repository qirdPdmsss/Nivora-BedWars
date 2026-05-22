package de.qirdpdms.nivoraBedWars.command;

import de.qirdpdms.nivoraBedWars.Main;
import de.qirdpdms.nivoraBedWars.command.completer.BedwarsTabCompleter;
import de.qirdpdms.nivoraBedWars.command.completer.LeaveTabCompleter;
import de.qirdpdms.nivoraBedWars.command.completer.QueueTabCompleter;
import de.qirdpdms.nivoraBedWars.command.completer.TestTabCompleter;
import org.bukkit.command.PluginCommand;

import java.util.Objects;

public class CommandRegistrar {

    private final Main plugin;

    public CommandRegistrar(Main plugin) {
        this.plugin = plugin;
    }

    public void register() {
        PluginCommand bedwars = Objects.requireNonNull(plugin.getCommand("bedwars"));
        bedwars.setExecutor(new BedwarsCommand(plugin));
        bedwars.setTabCompleter(new BedwarsTabCompleter(plugin));

        PluginCommand queue = Objects.requireNonNull(plugin.getCommand("queue"));
        queue.setExecutor(new QueueCommand(plugin));
        queue.setTabCompleter(new QueueTabCompleter());

        PluginCommand leave = Objects.requireNonNull(plugin.getCommand("leave"));
        leave.setExecutor(new LeaveCommand(plugin));
        leave.setTabCompleter(new LeaveTabCompleter());

        PluginCommand bwtest = Objects.requireNonNull(plugin.getCommand("bwtest"));
        bwtest.setExecutor(new TestCommand(plugin));
        bwtest.setTabCompleter(new TestTabCompleter(plugin));
    }
}

