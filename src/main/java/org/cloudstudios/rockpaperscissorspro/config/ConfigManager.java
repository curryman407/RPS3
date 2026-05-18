package org.cloudstudios.rockpaperscissorspro.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.cloudstudios.rockpaperscissorspro.RockPaperScissorsPro;

import java.util.List;

public final class ConfigManager {

    private final RockPaperScissorsPro plugin;
    private FileConfiguration config;

    public ConfigManager(final RockPaperScissorsPro plugin) {
        this.plugin = plugin;
    }
    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    public boolean isCooldownEnabled()   { return config.getBoolean("cooldown.enabled", true); }
    public int     getCooldownDuration() { return config.getInt("cooldown.duration", 30); }

    public int getChallengeTimeout() { return config.getInt("challenge.timeout", 60); }

    public int getCountdownDuration() { return Math.min(config.getInt("countdown.duration", 3), 10); }

    public int getMaxTieReplays() { return config.getInt("tie.max-replays", 3); }

    public String       getGuiTitle()          { return config.getString("gui.title", "&8[RPS] &7vs &e%opponent%"); }
    public String       getFillerMaterial()    { return config.getString("gui.filler.material", "BLACK_STAINED_GLASS_PANE"); }
    public String       getFillerName()        { return config.getString("gui.filler.name", ""); }
    public String       getRockMaterial()      { return config.getString("gui.rock.material", "COBBLESTONE"); }
    public int          getRockSlot()          { return config.getInt("gui.rock.slot", 11); }
    public String       getRockName()          { return config.getString("gui.rock.name", "&7&lROCK"); }
    public List<String> getRockLore()          { return config.getStringList("gui.rock.lore"); }
    public String       getPaperMaterial()     { return config.getString("gui.paper.material", "PAPER"); }
    public int          getPaperSlot()         { return config.getInt("gui.paper.slot", 13); }
    public String       getPaperName()         { return config.getString("gui.paper.name", "&f&lPAPER"); }
    public List<String> getPaperLore()         { return config.getStringList("gui.paper.lore"); }
    public String       getScissorsMaterial()  { return config.getString("gui.scissors.material", "SHEARS"); }
    public int          getScissorsSlot()      { return config.getInt("gui.scissors.slot", 15); }
    public String       getScissorsName()      { return config.getString("gui.scissors.name", "&c&lSCISSORS"); }
    public List<String> getScissorsLore()      { return config.getStringList("gui.scissors.lore"); }

    public String       getGamesGuiTitle()         { return config.getString("games-gui.title", "&6&lAVAILABLE GAMES"); }
    public String       getGamesFillerMaterial()   { return config.getString("games-gui.filler.material", "BLACK_STAINED_GLASS_PANE"); }
    public String       getGamesFillerName()       { return config.getString("games-gui.filler.name", ""); }
    public String       getStatsMaterial()         { return config.getString("games-gui.stats.material", "BOOK"); }
    public String       getStatsName()             { return config.getString("games-gui.stats.name", "&6&lSTATS"); }
    public String       getPrevPageMaterial()      { return config.getString("games-gui.prev-page.material", "ARROW"); }
    public String       getPrevPageName()          { return config.getString("games-gui.prev-page.name", "&7« Previous Page"); }
    public String       getNextPageMaterial()      { return config.getString("games-gui.next-page.material", "ARROW"); }
    public String       getNextPageName()          { return config.getString("games-gui.next-page.name", "&7Next Page »"); }
    public String       getNoGamesMaterial()       { return config.getString("games-gui.no-games.material", "BARRIER"); }
    public String       getNoGamesName()           { return config.getString("games-gui.no-games.name", "&cNo games available"); }
    public List<String> getNoGamesLore()           { return config.getStringList("games-gui.no-games.lore"); }
    public String       getYourGameName()          { return config.getString("games-gui.your-game.name", "&a&lYOUR GAME"); }
    public List<String> getYourGameLore()          { return config.getStringList("games-gui.your-game.lore"); }
    public String       getOtherGameName()         { return config.getString("games-gui.other-game.name", "&6%player%"); }
    public List<String> getOtherGameLore()         { return config.getStringList("games-gui.other-game.lore"); }
    public String       getUnavailableGameName()   { return config.getString("games-gui.unavailable-game.name", "&c&lNOT AVAILABLE"); }
    public List<String> getUnavailableGameLore()   { return config.getStringList("games-gui.unavailable-game.lore"); }
    public int          getPrevPageSlot()          { return config.getInt("games-gui.prev-page-slot", 45); }
    public int          getStatsSlot()             { return config.getInt("games-gui.stats-slot", 49); }
    public int          getNextPageSlot()          { return config.getInt("games-gui.next-page-slot", 53); }


    public List<Integer> getGameSlots() {
        final List<Integer> fromConfig = config.getIntegerList("games-gui.game-slots");
        if (!fromConfig.isEmpty()) return fromConfig;
        return java.util.Arrays.asList(
                10,11,12,13,14,15,16,
                19,20,21,22,23,24,25,
                28,29,30,31,32,33,34,
                37,38,39,40,41,42,43);
    }

    public int getLobbyTimeout() { return config.getInt("lobby.timeout", 300); }

    public String getSound(final String key)       { return config.getString("sounds." + key, "NONE"); }
    public double getSoundVolume(final String key) { return config.getDouble("sound-settings." + key + ".volume", 1.0); }
    public double getSoundPitch(final String key)  { return config.getDouble("sound-settings." + key + ".pitch", 1.0); }

    public boolean      isRewardsEnabled()               { return config.getBoolean("rewards.enabled", true); }
    public boolean      isWinRewardEnabled()             { return config.getBoolean("rewards.win.enabled", true); }
    public List<String> getWinRewardCommands()           { return config.getStringList("rewards.win.commands"); }
    public boolean      isParticipationRewardEnabled()   { return config.getBoolean("rewards.participation.enabled", false); }
    public List<String> getParticipationRewardCommands() { return config.getStringList("rewards.participation.commands"); }

    public boolean isBettingEnabled() { return config.getBoolean("betting.enabled", true); }
    public long    getBetMin()         { return config.getLong("betting.min-bet", 1L); }
    public long    getBetMax()         { return config.getLong("betting.max-bet", 0L); }
    public String  getBetTakeCommand() { return config.getString("betting.take-command", "eco take %player% %amount%"); }
    public String  getBetGiveCommand() { return config.getString("betting.give-command", "eco give %player% %amount%"); }
}
