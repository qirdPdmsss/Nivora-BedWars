package de.qirdpdms.nivoraBedWars.command;

import de.qirdpdms.nivoraBedWars.Main;
import de.qirdpdms.nivoraBedWars.model.GeneratorConfig;
import de.qirdpdms.nivoraBedWars.model.MapConfig;
import de.qirdpdms.nivoraBedWars.model.PluginMode;
import de.qirdpdms.nivoraBedWars.model.TeamData;
import de.qirdpdms.nivoraBedWars.util.ColorUtil;
import de.qirdpdms.nivoraBedWars.util.PermissionNodes;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BedwarsCommand implements CommandExecutor {

    private final Main plugin;

    public BedwarsCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(PermissionNodes.ADMIN)) {
            plugin.getMessageManager().send(sender, "general.no-permission");
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        if (sub.equals("reload")) {
            if (!sender.hasPermission(PermissionNodes.RELOAD)) {
                plugin.getMessageManager().send(sender, "general.no-permission");
                return true;
            }
            plugin.reloadPlugin();
            plugin.getMessageManager().send(sender, "command.bedwars.reload");
            return true;
        }

        if (sub.equals("status")) {
            if (plugin.getPluginMode() == PluginMode.LOBBY) {
                plugin.getMessageManager().send(sender, "command.bedwars.status-lobby", Map.of(
                        "value", String.valueOf(plugin.getQueueManager().size())
                ));
            } else {
                plugin.getMessageManager().send(sender, "command.bedwars.status-game-header", Map.of(
                        "value", plugin.getGameManager().getStatusSummary()
                ));
                for (Map<String, String> placeholders : plugin.getGameManager().getArenaStatusPlaceholders()) {
                    plugin.getMessageManager().send(sender, "command.bedwars.status-game-line", placeholders);
                }
            }
            return true;
        }

        if (sub.equals("forcestart")) {
            if (!sender.hasPermission(PermissionNodes.FORCE_START)) {
                plugin.getMessageManager().send(sender, "general.no-permission");
                return true;
            }
            if (plugin.getPluginMode() == PluginMode.LOBBY) {
                plugin.getMessageManager().send(sender, "command.bedwars.forcestart-lobby");
            } else {
                plugin.getGameManager().forceStart();
                plugin.getMessageManager().send(sender, "command.bedwars.forcestart-game");
            }
            return true;
        }

        if (sub.equals("matchend")) {
            if (!sender.hasPermission(PermissionNodes.MATCH_END)) {
                plugin.getMessageManager().send(sender, "general.no-permission");
                return true;
            }
            if (plugin.getPluginMode() == PluginMode.LOBBY) {
                plugin.getMessageManager().send(sender, "command.bedwars.matchend-lobby");
            } else {
                int endedArenas = plugin.getGameManager().endActiveMatches();
                if (endedArenas > 0) {
                    plugin.getMessageManager().send(sender, "command.bedwars.matchend-game", Map.of(
                            "value", String.valueOf(endedArenas)
                    ));
                } else {
                    plugin.getMessageManager().send(sender, "command.bedwars.matchend-none");
                }
            }
            return true;
        }

        if (sub.equals("build")) {
            if (!sender.hasPermission(PermissionNodes.BUILD)) {
                plugin.getMessageManager().send(sender, "general.no-permission");
                return true;
            }
            if (!(sender instanceof Player player)) {
                plugin.getMessageManager().send(sender, "general.only-player");
                return true;
            }

            boolean enabled;
            if (args.length >= 2) {
                String state = args[1].toLowerCase();
                if (state.equals("on")) {
                    enabled = true;
                } else if (state.equals("off")) {
                    enabled = false;
                } else {
                    plugin.getMessageManager().send(sender, "command.bedwars.build-usage", Map.of(
                            "usage", "/bedwars build [on|off]"
                    ));
                    return true;
                }
                plugin.setBuildMode(player.getUniqueId(), enabled);
            } else {
                enabled = plugin.toggleBuildMode(player.getUniqueId());
            }

            plugin.getMessageManager().send(player, enabled
                    ? "command.bedwars.build-enabled"
                    : "command.bedwars.build-disabled");
            return true;
        }

        if (sub.equals("tp")) {
            if (!sender.hasPermission(PermissionNodes.SETUP)) {
                plugin.getMessageManager().send(sender, "general.no-permission");
                return true;
            }
            if (!(sender instanceof Player player)) {
                plugin.getMessageManager().send(sender, "general.only-player");
                return true;
            }
            if (plugin.getPluginMode() == PluginMode.LOBBY) {
                plugin.getMessageManager().send(sender, "command.bedwars.tp-only-game");
                return true;
            }
            if (args.length < 2) {
                plugin.getMessageManager().send(sender, "command.bedwars.tp-usage", Map.of(
                        "usage", "/bedwars tp <team>"
                ));
                return true;
            }

            String mapId = resolveTeleportMapId(player);
            if (mapId == null || mapId.isBlank() || !plugin.getConfigManager().mapExists(mapId)) {
                plugin.getMessageManager().send(sender, "command.bedwars.map-not-found", Map.of(
                        "value", player.getWorld().getName()
                ));
                return true;
            }

            String teamId = args[1].toLowerCase();
            MapConfig mapConfig = plugin.getConfigManager().loadMapConfig(mapId);
            TeamData teamData = mapConfig.getTeams().stream()
                    .filter(team -> team.getId().equalsIgnoreCase(teamId))
                    .findFirst()
                    .orElse(null);
            if (teamData == null) {
                plugin.getMessageManager().send(sender, "command.bedwars.team-not-found", Map.of(
                        "map", mapId,
                        "team", teamId,
                        "value", teamId
                ));
                return true;
            }
            if (teamData.getSpawn() == null) {
                plugin.getMessageManager().send(sender, "command.bedwars.tp-no-spawn", Map.of(
                        "map", mapId,
                        "team", teamData.getDisplayName(),
                        "value", teamData.getDisplayName()
                ));
                return true;
            }

            player.teleport(teamData.getSpawn());
            plugin.getMessageManager().send(player, "command.bedwars.tp-success", Map.of(
                    "map", mapId,
                    "team", teamData.getDisplayName(),
                    "value", teamData.getDisplayName()
            ));
            return true;
        }

        if (sub.equals("setup")) {
            return handleSetup(sender, args);
        }

        if (sub.equals("validate")) {
            return handleValidate(sender, args);
        }

        sendUsage(sender);
        return true;
    }

    private boolean handleValidate(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PermissionNodes.SETUP)) {
            plugin.getMessageManager().send(sender, "general.no-permission");
            return true;
        }
        if (args.length < 2) {
            msg(sender, "&cNutze: &e/bedwars validate <map>");
            return true;
        }

        String mapId = args[1];
        if (!plugin.getConfigManager().mapExists(mapId)) {
            plugin.getMessageManager().send(sender, "command.bedwars.map-not-found", Map.of("value", mapId));
            return true;
        }

        MapConfig mapConfig = plugin.getConfigManager().loadMapConfig(mapId);
        List<String> ok = new ArrayList<>();
        List<String> warn = new ArrayList<>();

        if (mapConfig.getLobbySpawn() != null) {
            ok.add("Lobbyspawn: " + plugin.formatLocation(mapConfig.getLobbySpawn()));
        } else {
            warn.add("Lobbyspawn fehlt oder konnte nicht geladen werden.");
        }

        if (mapConfig.getTeams().isEmpty()) {
            warn.add("Keine Teams geladen.");
        }

        for (TeamData teamData : mapConfig.getTeams()) {
            validateLocation(warn, ok, "Team " + teamData.getId() + " Spawn", teamData.getSpawn(), false);
            validateLocation(warn, ok, "Team " + teamData.getId() + " Bett", teamData.getBed(), true);
            validateLocation(warn, ok, "Team " + teamData.getId() + " Generator", teamData.getGenerator(), false);
            validateLocation(warn, ok, "Team " + teamData.getId() + " Item-Shop", teamData.getItemShop(), false);
            validateLocation(warn, ok, "Team " + teamData.getId() + " Upgrade-Shop", teamData.getUpgradeShop(), false);
        }

        long teamGenerators = mapConfig.getGenerators().stream().filter(GeneratorConfig::isTeamGenerator).count();
        long mapGenerators = mapConfig.getGenerators().stream().filter(generator -> !generator.isTeamGenerator()).count();
        if (mapGenerators > 0) {
            ok.add("Map-Generatoren: " + mapGenerators);
        } else {
            warn.add("Keine Map-Generatoren konfiguriert.");
        }
        if (teamGenerators > 0) {
            ok.add("Team-Generatoren: " + teamGenerators);
        } else {
            warn.add("Keine Team-Generatoren konfiguriert.");
        }

        if (!mapConfig.getShops().isEmpty()) {
            ok.add("Shops: " + mapConfig.getShops().size());
        } else {
            warn.add("Keine Shops konfiguriert.");
        }

        msg(sender, "&8&m--------------------------------------------------");
        msg(sender, "&eMap-Validierung für &f" + mapId);
        msg(sender, "&7Min-Spieler: &f" + mapConfig.getMinPlayers() + " &8| &7Max-Spieler: &f" + mapConfig.getMaxPlayers());
        for (String line : ok) {
            msg(sender, "&a✔ &7" + line);
        }
        if (warn.isEmpty()) {
            msg(sender, "&aKeine Probleme gefunden.");
        } else {
            for (String line : warn) {
                msg(sender, "&c✖ &7" + line);
            }
        }
        msg(sender, "&7Zusammenfassung: &a" + ok.size() + " OK &8/ &c" + warn.size() + " Probleme");
        msg(sender, "&8&m--------------------------------------------------");
        return true;
    }

    private boolean handleSetup(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PermissionNodes.SETUP)) {
            plugin.getMessageManager().send(sender, "general.no-permission");
            return true;
        }
        if (!(sender instanceof Player player)) {
            plugin.getMessageManager().send(sender, "general.only-player");
            return true;
        }
        if (args.length < 3) {
            plugin.getMessageManager().send(sender, "command.bedwars.setup-usage", Map.of(
                    "usage", "/bedwars setup <lobbyspawn|teamspawn|bed|teamgenerator|teamshop|generator> <map> [team|id]"
            ));
            return true;
        }

        String action = args[1].toLowerCase();
        String mapId = args[2];
        if (!plugin.getConfigManager().mapExists(mapId)) {
            plugin.getMessageManager().send(sender, "command.bedwars.map-not-found", Map.of("value", mapId));
            return true;
        }

        if (action.equals("lobbyspawn")) {
            plugin.getConfigManager().saveLobbySpawn(mapId, player.getLocation());
            plugin.getMessageManager().send(sender, "command.bedwars.setup-lobbyspawn", Map.of(
                    "map", mapId,
                    "value", player.getWorld().getName()
            ));
            return true;
        }

        if (action.equals("generator")) {
            if (args.length < 4) {
                plugin.getMessageManager().send(sender, "command.bedwars.setup-usage", Map.of(
                        "usage", "/bedwars setup generator <map> <type> <id> oder /bedwars setup generator <map> <id>"
                ));
                return true;
            }

            String generatorId;
            if (args.length >= 5) {
                generatorId = plugin.getConfigManager().saveGenerator(mapId, args[3], args[4], player.getLocation());
            } else {
                generatorId = args[3].toLowerCase();
                plugin.getConfigManager().saveGenerator(mapId, generatorId, player.getLocation());
            }
            plugin.getMessageManager().send(sender, "command.bedwars.setup-generator", Map.of(
                    "map", mapId,
                    "id", generatorId,
                    "value", player.getWorld().getName()
            ));
            return true;
        }

        if (args.length < 4) {
            plugin.getMessageManager().send(sender, "command.bedwars.setup-usage", Map.of(
                    "usage", "/bedwars setup <lobbyspawn|teamspawn|bed|teamgenerator|teamshop|generator> <map> [team|id]"
            ));
            return true;
        }

        String teamId = args[3].toLowerCase();
        if (!plugin.getConfigManager().teamExists(mapId, teamId)) {
            plugin.getMessageManager().send(sender, "command.bedwars.team-not-found", Map.of(
                    "map", mapId,
                    "team", teamId,
                    "value", teamId
            ));
            return true;
        }

        if (action.equals("teamspawn")) {
            plugin.getConfigManager().saveTeamSpawn(mapId, teamId, player.getLocation());
            plugin.getMessageManager().send(sender, "command.bedwars.setup-teamspawn", Map.of(
                    "map", mapId,
                    "team", teamId,
                    "value", player.getWorld().getName()
            ));
            return true;
        }

        if (action.equals("bed")) {
            Location bedLocation = resolveBedSetupLocation(player);
            plugin.getConfigManager().saveTeamBed(mapId, teamId, bedLocation);
            plugin.getMessageManager().send(sender, "command.bedwars.setup-bed", Map.of(
                    "map", mapId,
                    "team", teamId,
                    "value", player.getWorld().getName()
            ));
            return true;
        }

        if (action.equals("teamgenerator")) {
            plugin.getConfigManager().saveTeamGenerator(mapId, teamId, player.getLocation());
            plugin.getMessageManager().send(sender, "command.bedwars.setup-teamgenerator", Map.of(
                    "map", mapId,
                    "team", teamId,
                    "value", player.getWorld().getName()
            ));
            return true;
        }

        if (action.equals("teamshop")) {
            if (args.length < 5) {
                plugin.getMessageManager().send(sender, "command.bedwars.setup-usage", Map.of(
                        "usage", "/bedwars setup teamshop <map> <team> <itemshop|upgradeshop>"
                ));
                return true;
            }
            String shopType = args[4].toLowerCase();
            plugin.getConfigManager().saveTeamShop(mapId, teamId, shopType, player.getLocation());
            plugin.getMessageManager().send(sender, "command.bedwars.setup-teamshop", Map.of(
                    "map", mapId,
                    "team", teamId,
                    "id", shopType,
                    "value", player.getWorld().getName()
            ));
            return true;
        }

        plugin.getMessageManager().send(sender, "command.bedwars.setup-usage", Map.of(
                "usage", "/bedwars setup <lobbyspawn|teamspawn|bed|teamgenerator|teamshop|generator> <map> [team|id]"
        ));
        return true;
    }

    private void sendUsage(CommandSender sender) {
        plugin.getMessageManager().send(sender, "command.bedwars.usage", Map.of(
                "usage", "/bedwars <reload|status|forcestart|matchend|build|tp|setup|validate>"
        ));
    }

    private void validateLocation(List<String> warn, List<String> ok, String label, Location location, boolean expectBed) {
        if (location == null || location.getWorld() == null) {
            warn.add(label + " fehlt.");
            return;
        }

        Block block = location.getBlock();
        if (expectBed) {
            boolean isBed = block.getType().name().endsWith("_BED");
            if (!isBed) {
                boolean nearbyBed = false;
                for (int x = -4; x <= 4 && !nearbyBed; x++) {
                    for (int y = -4; y <= 4 && !nearbyBed; y++) {
                        for (int z = -4; z <= 4; z++) {
                            Material type = location.clone().add(x, y, z).getBlock().getType();
                            if (type.name().endsWith("_BED")) {
                                nearbyBed = true;
                                break;
                            }
                        }
                    }
                }
                if (!nearbyBed) {
                    warn.add(label + " zeigt auf kein Bett: " + plugin.formatLocation(location));
                    return;
                }
            }
        }

        ok.add(label + ": " + plugin.formatLocation(location));
    }

    private void msg(CommandSender sender, String message) {
        sender.sendMessage(ColorUtil.colorize("&8[&bBedwars-Validate&8] &7" + message));
    }

    private String resolveTeleportMapId(Player player) {
        String worldName = player.getWorld().getName();
        if (plugin.getConfigManager().mapExists(worldName)) {
            return worldName;
        }

        String configuredMapId = plugin.getConfigManager().getGameMapId();
        if (configuredMapId != null && !configuredMapId.isBlank()) {
            return configuredMapId;
        }
        return "";
    }

    private Location resolveBedSetupLocation(Player player) {
        Block targetBlock = player.getTargetBlockExact(6);
        if (targetBlock != null && targetBlock.getType().name().endsWith("_BED")) {
            return targetBlock.getLocation();
        }
        return player.getLocation();
    }
}
