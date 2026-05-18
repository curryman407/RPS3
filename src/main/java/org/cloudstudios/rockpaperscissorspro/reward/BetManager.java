package org.cloudstudios.rockpaperscissorspro.reward;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.cloudstudios.rockpaperscissorspro.config.ConfigManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;


public final class BetManager {

    private final ConfigManager configManager;
    private final Logger        logger;


    private Economy economy;


    private final Map<UUID, Long> pendingBets = new HashMap<>();

    public BetManager(final ConfigManager configManager, final Logger logger) {
        this.configManager = configManager;
        this.logger        = logger;
    }



    public boolean setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            logger.warning("[RPS] Vault not found — balance checks are DISABLED. "
                    + "Players may be put into debt if their economy plugin allows it.");
            return false;
        }
        final RegisteredServiceProvider<Economy> rsp =
                Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            logger.warning("[RPS] Vault found but no Economy provider registered — "
                    + "balance checks are DISABLED.");
            return false;
        }
        this.economy = rsp.getProvider();
        logger.info("[RPS] Vault economy hooked: " + economy.getName());
        return true;
    }

    public boolean isVaultAvailable() {
        return economy != null;
    }


    public boolean hasBalance(final OfflinePlayer player, final long amount) {
        if (amount <= 0)     return true;
        if (economy == null) return true;
        return economy.has(player, amount);
    }



    public void setPendingBet(final UUID challenged, final long amount) {
        if (amount > 0) pendingBets.put(challenged, amount);
    }


    public long getPendingBet(final UUID challenged) {
        return pendingBets.getOrDefault(challenged, 0L);
    }


    public void clearPendingBet(final UUID challenged) {
        pendingBets.remove(challenged);
    }


    public void clearAll() { pendingBets.clear(); }



    public boolean isEnabled() { return configManager.isBettingEnabled(); }


    public ValidationResult validate(final long amount) {
        if (!configManager.isBettingEnabled())  return ValidationResult.DISABLED;
        if (amount <= 0)                         return ValidationResult.BELOW_MIN;
        if (amount < configManager.getBetMin())  return ValidationResult.BELOW_MIN;
        final long max = configManager.getBetMax();
        if (max > 0 && amount > max)             return ValidationResult.ABOVE_MAX;
        return ValidationResult.OK;
    }

    public long getMinBet() { return configManager.getBetMin(); }
    public long getMaxBet() { return configManager.getBetMax(); }




    public void takeBet(final Player player, final long amount) {
        if (player == null || amount <= 0) return;
        dispatch(configManager.getBetTakeCommand(), player.getName(), amount);
    }


    public void giveWinnings(final Player player, final long amount) {
        if (player == null || amount <= 0) return;
        dispatch(configManager.getBetGiveCommand(), player.getName(), amount * 2);
    }


    public void refundBet(final Player player, final long amount) {
        if (player == null || amount <= 0) return;
        dispatch(configManager.getBetGiveCommand(), player.getName(), amount);
    }

    private void dispatch(final String template, final String playerName, final long amount) {
        final String cmd = template
                .replace("%player%", playerName)
                .replace("%amount%", String.valueOf(amount));
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
    }



    public enum ValidationResult { OK, DISABLED, BELOW_MIN, ABOVE_MAX }
}
