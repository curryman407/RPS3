package org.cloudstudios.rockpaperscissorspro.core;

import org.bukkit.configuration.file.YamlConfiguration;
import org.cloudstudios.rockpaperscissorspro.RockPaperScissorsPro;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;


public final class StatsManager {

    private final RockPaperScissorsPro           plugin;
    private final Map<UUID, PlayerStats> statsMap = new ConcurrentHashMap<>();

    public StatsManager(final RockPaperScissorsPro plugin) {
        this.plugin = plugin;
    }

    public void load() {
        final File file = new File(plugin.getDataFolder(), "stats.yml");
        if (!file.exists()) return;
        final YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (final String uuidStr : yaml.getKeys(false)) {
            try {
                final UUID uuid   = UUID.fromString(uuidStr);
                final int played  = yaml.getInt(uuidStr + ".played", 0);
                final int won     = yaml.getInt(uuidStr + ".won",    0);
                final int lost    = yaml.getInt(uuidStr + ".lost",   0);
                final int tied    = yaml.getInt(uuidStr + ".tied",   0);
                statsMap.put(uuid, new PlayerStats(played, won, lost, tied));
            } catch (final IllegalArgumentException ignored) {}
        }
    }

    public void save() {
        final File file = new File(plugin.getDataFolder(), "stats.yml");
        final YamlConfiguration yaml = new YamlConfiguration();
        statsMap.forEach((uuid, stats) -> {
            final String k = uuid.toString();
            yaml.set(k + ".played", stats.getPlayed());
            yaml.set(k + ".won",    stats.getWon());
            yaml.set(k + ".lost",   stats.getLost());
            yaml.set(k + ".tied",   stats.getTied());
        });
        try {
            yaml.save(file);
        } catch (final IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save stats.yml", e);
        }
    }

    public PlayerStats getStats(final UUID uuid) {
        return statsMap.computeIfAbsent(uuid, k -> new PlayerStats());
    }

    public void recordWin(final UUID uuid)  { PlayerStats s = getStats(uuid); s.incrementPlayed(); s.incrementWon(); }
    public void recordLoss(final UUID uuid) { PlayerStats s = getStats(uuid); s.incrementPlayed(); s.incrementLost(); }
    public void recordTie(final UUID uuid)  { PlayerStats s = getStats(uuid); s.incrementPlayed(); s.incrementTied(); }
}
