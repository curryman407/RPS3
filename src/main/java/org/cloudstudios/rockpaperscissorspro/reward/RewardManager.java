package org.cloudstudios.rockpaperscissorspro.reward;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.cloudstudios.rockpaperscissorspro.config.ConfigManager;


public final class RewardManager {

    private final ConfigManager configManager;

    public RewardManager(final ConfigManager configManager) {
        this.configManager = configManager;
    }


    public void executeMatchRewards(final Player winner, final Player loser) {
        if (!configManager.isRewardsEnabled()) return;

        if (configManager.isWinRewardEnabled() && winner != null && winner.isOnline()) {
            for (final String cmd : configManager.getWinRewardCommands()) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), replace(cmd, winner, loser));
            }
        }

        if (configManager.isParticipationRewardEnabled() && loser != null && loser.isOnline()) {
            for (final String cmd : configManager.getParticipationRewardCommands()) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), replace(cmd, winner, loser));
            }
        }
    }

    private String replace(final String cmd, final Player winner, final Player loser) {
        String r = cmd;
        if (winner != null) r = r.replace("%winner%", winner.getName());
        if (loser  != null) r = r.replace("%loser%",  loser.getName());
        return r;
    }
}
