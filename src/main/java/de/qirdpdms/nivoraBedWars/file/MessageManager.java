package de.qirdpdms.nivoraBedWars.file;

import de.qirdpdms.nivoraBedWars.Main;
import de.qirdpdms.nivoraBedWars.util.ColorUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MessageManager {

    private final Main plugin;
    private File file;
    private YamlConfiguration messages;

    public MessageManager(Main plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        this.file = new File(plugin.getDataFolder(), "messages.yml");
        this.messages = YamlConfiguration.loadConfiguration(file);
    }

    public String getRaw(String path) {
        return messages.getString(path, path);
    }

    public String get(String path) {
        return get(path, true, Collections.emptyMap());
    }

    public String get(String path, boolean withPrefix) {
        return get(path, withPrefix, Collections.emptyMap());
    }

    public String get(String path, boolean withPrefix, Map<String, String> placeholders) {
        String text = getRaw(path);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            text = text.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        if (withPrefix) {
            text = getRaw("prefix") + text;
        }
        return ColorUtil.colorize(text);
    }

    public List<String> getList(String path, boolean withPrefix) {
        List<String> list = messages.getStringList(path);
        String prefix = withPrefix ? getRaw("prefix") : "";
        return list.stream().map(line -> ColorUtil.colorize(prefix + line)).toList();
    }

    public List<String> getList(String path, boolean withPrefix, Map<String, String> placeholders) {
        List<String> list = messages.getStringList(path);
        String prefix = withPrefix ? getRaw("prefix") : "";
        return list.stream().map(line -> {
            String result = line;
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                result = result.replace("{" + entry.getKey() + "}", entry.getValue());
            }
            return ColorUtil.colorize(prefix + result);
        }).toList();
    }

    public void send(CommandSender sender, String path) {
        sender.sendMessage(get(path));
    }

    public void send(CommandSender sender, String path, boolean withPrefix) {
        sender.sendMessage(get(path, withPrefix));
    }

    public void send(CommandSender sender, String path, Map<String, String> placeholders) {
        sender.sendMessage(get(path, true, placeholders));
    }

    public void send(CommandSender sender, String path, boolean withPrefix, Map<String, String> placeholders) {
        sender.sendMessage(get(path, withPrefix, placeholders));
    }

    public void sendLines(CommandSender sender, List<String> lines) {
        sender.sendMessage(lines.toArray(String[]::new));
    }

    public void sendList(CommandSender sender, String path, boolean withPrefix, Map<String, String> placeholders) {
        sendLines(sender, getList(path, withPrefix, placeholders));
    }
}

