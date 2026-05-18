package org.cloudstudios.rockpaperscissorspro.config;

import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.cloudstudios.rockpaperscissorspro.RockPaperScissorsPro;
import org.cloudstudios.rockpaperscissorspro.util.ColorUtil;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class MessageManager {

    private final RockPaperScissorsPro plugin;
    private FileConfiguration messages;

    public MessageManager(final RockPaperScissorsPro plugin) {
        this.plugin = plugin;
    }

    public void load() {
        final File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) plugin.saveResource("messages.yml", false);
        this.messages = YamlConfiguration.loadConfiguration(file);
        final InputStream def = plugin.getResource("messages.yml");
        if (def != null) {
            messages.setDefaults(YamlConfiguration.loadConfiguration(
                    new InputStreamReader(def, StandardCharsets.UTF_8)));
        }
    }

    public String getPrefix() {
        return messages.getString("prefix", "&8[&c&lRPS&8] &r");
    }

    private String buildRaw(final String key, final Map<String, String> replacements) {
        String raw = messages.getString(key, "");
        if (raw.isEmpty()) return "";
        if (!key.equals("prefix")) raw = getPrefix() + raw;
        for (final Map.Entry<String, String> e : replacements.entrySet()) {
            raw = raw.replace("%" + e.getKey() + "%", e.getValue());
        }
        return raw;
    }

    public Component get(final String key, final Map<String, String> replacements) {
        final String raw = buildRaw(key, replacements);
        return raw.isEmpty() ? Component.empty() : ColorUtil.translate(raw);
    }

    public Component get(final String key) {
        return get(key, Map.of());
    }

    public void send(final Player player, final String key, final Map<String, String> replacements) {
        if (player == null || !player.isOnline()) return;
        final Component msg = get(key, replacements);
        if (!msg.equals(Component.empty())) player.sendMessage(msg);
    }

    public void send(final Player player, final String key) {
        send(player, key, Map.of());
    }

    public void send(final CommandSender sender, final String key) {
        final Component msg = get(key);
        if (!msg.equals(Component.empty())) sender.sendMessage(msg);
    }


    public void sendActionBar(final Player player, final String key, final Map<String, String> replacements) {
        if (player == null || !player.isOnline()) return;
        String raw = messages.getString(key, "");
        if (raw.isEmpty()) return;
        for (final Map.Entry<String, String> e : replacements.entrySet()) {
            raw = raw.replace("%" + e.getKey() + "%", e.getValue());
        }
        player.sendActionBar(ColorUtil.translate(raw));
    }

    public void sendActionBar(final Player player, final String key) {
        sendActionBar(player, key, Map.of());
    }
}
