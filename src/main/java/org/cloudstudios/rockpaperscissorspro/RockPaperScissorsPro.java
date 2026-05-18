package org.cloudstudios.rockpaperscissorspro;

import org.bukkit.plugin.java.JavaPlugin;
import org.cloudstudios.rockpaperscissorspro.commands.AcceptSubCommand;
import org.cloudstudios.rockpaperscissorspro.commands.ChallengeSubCommand;
import org.cloudstudios.rockpaperscissorspro.commands.CreateSubCommand;
import org.cloudstudios.rockpaperscissorspro.commands.DenySubCommand;
import org.cloudstudios.rockpaperscissorspro.commands.GamesSubCommand;
import org.cloudstudios.rockpaperscissorspro.commands.RPSCommand;
import org.cloudstudios.rockpaperscissorspro.commands.ReloadSubCommand;
import org.cloudstudios.rockpaperscissorspro.config.ConfigManager;
import org.cloudstudios.rockpaperscissorspro.config.MessageManager;
import org.cloudstudios.rockpaperscissorspro.cooldown.CooldownManager;
import org.cloudstudios.rockpaperscissorspro.core.GameLobbyManager;
import org.cloudstudios.rockpaperscissorspro.core.MatchManager;
import org.cloudstudios.rockpaperscissorspro.core.StatsManager;
import org.cloudstudios.rockpaperscissorspro.gui.GamesGui;
import org.cloudstudios.rockpaperscissorspro.gui.GamesGuiListener;
import org.cloudstudios.rockpaperscissorspro.gui.GuiListener;
import org.cloudstudios.rockpaperscissorspro.gui.RPSGui;
import org.cloudstudios.rockpaperscissorspro.listeners.PlayerQuitListener;
import org.cloudstudios.rockpaperscissorspro.match.MatchListener;
import org.cloudstudios.rockpaperscissorspro.reward.BetManager;
import org.cloudstudios.rockpaperscissorspro.reward.RewardManager;
import org.cloudstudios.rockpaperscissorspro.util.SoundUtil;

import java.util.Objects;


public final class RockPaperScissorsPro extends JavaPlugin {

    private MatchManager     matchManager;
    private GameLobbyManager lobbyManager;
    private CooldownManager  cooldownManager;
    private StatsManager     statsManager;

    @Override
    public void onEnable() {

        final ConfigManager  configManager  = new ConfigManager(this);
        final MessageManager messageManager = new MessageManager(this);
        configManager.load();
        messageManager.load();


        statsManager = new StatsManager(this);
        statsManager.load();


        cooldownManager = new CooldownManager(configManager);
        final BetManager    betManager    = new BetManager(configManager, getLogger());
        final RewardManager rewardManager = new RewardManager(configManager);
        final SoundUtil     soundUtil     = new SoundUtil(configManager, getLogger());
        final RPSGui        rpsGui        = new RPSGui(configManager);


        matchManager = new MatchManager(this, configManager, messageManager,
                cooldownManager, rewardManager, betManager, rpsGui, soundUtil, statsManager);


        lobbyManager = new GameLobbyManager(this, matchManager, messageManager,
                soundUtil, configManager, betManager);


        betManager.setupEconomy();


        final GamesGui gamesGui = new GamesGui(this, configManager, statsManager, matchManager);


        gamesGui.setLobbyManager(lobbyManager);
        lobbyManager.setGamesGui(gamesGui);
        matchManager.setGameLobbyManager(lobbyManager);


        getServer().getPluginManager().registerEvents(
                new GuiListener(matchManager, configManager, this), this);
        getServer().getPluginManager().registerEvents(
                new GamesGuiListener(gamesGui, lobbyManager, messageManager), this);
        getServer().getPluginManager().registerEvents(
                new MatchListener(matchManager), this);
        getServer().getPluginManager().registerEvents(
                new PlayerQuitListener(matchManager, lobbyManager), this);


        final ChallengeSubCommand challengeSub = new ChallengeSubCommand(
                matchManager, messageManager, cooldownManager, betManager, configManager);
        final AcceptSubCommand    acceptSub    = new AcceptSubCommand(matchManager, messageManager);
        final DenySubCommand      denySub      = new DenySubCommand(matchManager, messageManager);
        final CreateSubCommand    createSub    = new CreateSubCommand(
                lobbyManager, matchManager, messageManager, betManager);
        final GamesSubCommand     gamesSub     = new GamesSubCommand(gamesGui);
        final ReloadSubCommand    reloadSub    = new ReloadSubCommand(
                configManager, messageManager, statsManager);

        final RPSCommand rpsCommand = new RPSCommand(
                challengeSub, acceptSub, denySub, createSub, gamesSub, reloadSub, messageManager);
        Objects.requireNonNull(getCommand("rps")).setExecutor(rpsCommand);
        Objects.requireNonNull(getCommand("rps")).setTabCompleter(rpsCommand);

        getLogger().info("RockPaperScissorsPro v" + getDescription().getVersion() + " enabled!");
    }

    @Override
    public void onDisable() {
        if (statsManager   != null) statsManager.save();
        if (matchManager   != null) matchManager.shutdown();
        if (lobbyManager   != null) lobbyManager.shutdown();
        if (cooldownManager != null) cooldownManager.clearAll();
        getLogger().info("RockPaperScissorsPro disabled.");
    }
}
