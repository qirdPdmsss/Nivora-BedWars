package de.qirdpdms.nivoraBedWars.command.completer;

import de.qirdpdms.nivoraBedWars.Main;
import de.qirdpdms.nivoraBedWars.util.PermissionNodes;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class BedwarsTabCompleter implements TabCompleter {

    private final Main plugin;

    public BedwarsTabCompleter(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission(PermissionNodes.ADMIN)) {
            return List.of();
        }
        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            if (sender.hasPermission(PermissionNodes.RELOAD)) {
                list.add("reload");
            }
            list.add("status");
            if (sender.hasPermission(PermissionNodes.FORCE_START)) {
                list.add("forcestart");
            }
            if (sender.hasPermission(PermissionNodes.MATCH_END)) {
                list.add("matchend");
            }
            if (sender.hasPermission(PermissionNodes.BUILD)) {
                list.add("build");
            }
            if (sender.hasPermission(PermissionNodes.SETUP)) {
                list.add("tp");
                list.add("setup");
                list.add("validate");
            }
            return filter(list, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("build") && sender.hasPermission(PermissionNodes.BUILD)) {
            return filter(List.of("on", "off"), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("tp") && sender.hasPermission(PermissionNodes.SETUP)
                && sender instanceof org.bukkit.entity.Player player) {
            String mapId = resolveTeleportMapId(player);
            if (!mapId.isBlank()) {
                return filter(plugin.getConfigManager().getTeamIds(mapId), args[1]);
            }
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("setup") && sender.hasPermission(PermissionNodes.SETUP)) {
            return filter(List.of("lobbyspawn", "teamspawn", "bed", "teamgenerator", "teamshop", "generator"), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("setup") && sender.hasPermission(PermissionNodes.SETUP)) {
            return filter(plugin.getConfigManager().getMapIds(), args[2]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("validate") && sender.hasPermission(PermissionNodes.SETUP)) {
            return filter(plugin.getConfigManager().getMapIds(), args[1]);
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("setup") && sender.hasPermission(PermissionNodes.SETUP)) {
            String action = args[1].toLowerCase();
            if (action.equals("teamspawn") || action.equals("bed") || action.equals("teamgenerator") || action.equals("teamshop")) {
                return filter(plugin.getConfigManager().getTeamIds(args[2]), args[3]);
            }
            if (action.equals("generator")) {
                return filter(List.of("iron", "gold", "diamond", "emerald", "mid"), args[3]);
            }
        }
        if (args.length == 5 && args[0].equalsIgnoreCase("setup") && args[1].equalsIgnoreCase("teamshop")
                && sender.hasPermission(PermissionNodes.SETUP)) {
            return filter(List.of("itemshop", "upgradeshop"), args[4]);
        }
        if (args.length == 5 && args[0].equalsIgnoreCase("setup") && args[1].equalsIgnoreCase("generator")
                && sender.hasPermission(PermissionNodes.SETUP)) {
            return filter(List.of("1", "2", "3", "4", "left", "right", "top", "bottom", "center"), args[4]);
        }
        return List.of();
    }

    private List<String> filter(List<String> base, String input) {
        String lowered = input.toLowerCase();
        return base.stream().filter(entry -> entry.toLowerCase().startsWith(lowered)).toList();
    }

    private String resolveTeleportMapId(org.bukkit.entity.Player player) {
        String worldName = player.getWorld().getName();
        if (plugin.getConfigManager().mapExists(worldName)) {
            return worldName;
        }

        String configuredMapId = plugin.getConfigManager().getGameMapId();
        return configuredMapId == null ? "" : configuredMapId;
    }
}
