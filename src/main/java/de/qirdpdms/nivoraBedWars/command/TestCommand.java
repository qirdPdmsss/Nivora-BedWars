package de.qirdpdms.nivoraBedWars.command;

import de.qirdpdms.nivoraBedWars.Main;
import de.qirdpdms.nivoraBedWars.model.PluginMode;
import de.qirdpdms.nivoraBedWars.util.ColorUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /bwtest – Solo-Test-Command für Admins.
 *
 *   /bwtest start [mapId]   – Tritt der Arena bei, aktiviert Solo-Modus (min=1) und startet sofort
 *   /bwtest stop            – Beendet das laufende Match sofort
 *   /bwtest destroybed <team> – Zerstört das Bett des angegebenen Teams
 *   /bwtest kit             – Gibt ein vollständiges Test-Kit (Ressourcen, Waffen, Blöcke)
 *   /bwtest help            – Zeigt diese Hilfe
 */
public class TestCommand implements CommandExecutor {

    private static final String PERM = "novira.nivorabedwars.admin";
    private static final String PREFIX = "&8[&6BW-Test&8] &r";

    private final Main plugin;

    public TestCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(PERM)) {
            msg(sender, "&cKeine Berechtigung.");
            return true;
        }
        if (!(sender instanceof Player player)) {
            msg(sender, "&cNur für Spieler.");
            return true;
        }
        if (plugin.getPluginMode() == PluginMode.LOBBY) {
            msg(sender, "&cDieser Command funktioniert nur im GAME-Modus.");
            return true;
        }

        String sub = args.length > 0 ? args[0].toLowerCase() : "help";

        switch (sub) {
            case "start" -> handleStart(player, args);
            case "stop"  -> handleStop(player);
            case "destroybed", "bed" -> handleDestroyBed(player, args);
            case "kit"   -> handleKit(player);
            default      -> sendHelp(player);
        }
        return true;
    }

    // ─── Sub-Commands ────────────────────────────────────────────────────────

    private void handleStart(Player player, String[] args) {
        String mapId = args.length >= 2 ? args[1] : null;
        plugin.debug("bwtest", "command /bwtest start -> player=" + player.getName() + " mapId=" + mapId);

        if (mapId != null && !plugin.getConfigManager().mapExists(mapId)) {
            plugin.debug("bwtest", "command /bwtest start -> map not found mapId=" + mapId);
            msg(player, "&cMap &e" + mapId + " &cnicht gefunden.");
            msg(player, "&7Verfügbare Maps: &f" + String.join(", ", plugin.getConfigManager().getArenaMapIds()));
            return;
        }

        String error = plugin.getGameManager().enableSoloTestAndStart(player, mapId);
        if (error != null) {
            plugin.debug("bwtest", "command /bwtest start -> failed error='" + error + "'");
            msg(player, "&c" + error);
            return;
        }

        plugin.debug("bwtest", "command /bwtest start -> success player=" + player.getName());
        msg(player, "&aSolo-Test-Match gestartet!" + (mapId != null ? " &7(Map: &f" + mapId + "&7)" : ""));
        msg(player, "&7Tipp: &f/bwtest kit &7für Test-Items,  &f/bwtest destroybed <team> &7zum Bett zerstören");
    }

    private void handleStop(Player player) {
        boolean ended = plugin.getGameManager().endMatchForPlayer(player);
        if (ended) {
            msg(player, "&aMatch erfolgreich beendet.");
        } else {
            msg(player, "&cKein laufendes Match gefunden (bist du in einer Arena?).");
        }
    }

    private void handleDestroyBed(Player player, String[] args) {
        if (args.length < 2) {
            msg(player, "&cVerwendung: &f/bwtest destroybed <team>");
            msg(player, "&7Teams: green, blue, red, yellow, aqua, white, pink, gray");
            return;
        }
        String teamId = args[1].toLowerCase();
        boolean success = plugin.getGameManager().destroyBedForTest(player, teamId);
        if (success) {
            msg(player, "&aBett von Team &e" + teamId + " &azerstört.");
        } else {
            msg(player, "&cKonnte Bett nicht zerstören. Bist du in einem laufenden Spiel? Existiert das Team &e"
                    + teamId + "&c?");
        }
    }

    private void handleKit(Player player) {
        boolean success = plugin.getGameManager().giveTestKit(player);
        if (success) {
            msg(player, "&aTest-Kit erhalten! &7(64x Eisen, 32x Gold, 16x Diamant, 8x Smaragd, Waffen, Blöcke)");
        } else {
            msg(player, "&cKein Kit – bist du in einer Arena?");
        }
    }

    private void sendHelp(Player player) {
        msg(player, "&e/bwtest start &7[mapId]       &8– &fSolo-Match starten (min=1, sofort)");
        msg(player, "&e/bwtest stop                  &8– &fMatch sofort beenden");
        msg(player, "&e/bwtest destroybed &7<team>   &8– &fBett eines Teams zerstören");
        msg(player, "&e/bwtest kit                   &8– &fTest-Items bekommen");
    }

    // ─── Util ────────────────────────────────────────────────────────────────

    private void msg(CommandSender sender, String text) {
        sender.sendMessage(ColorUtil.colorize(PREFIX + text));
    }
}

