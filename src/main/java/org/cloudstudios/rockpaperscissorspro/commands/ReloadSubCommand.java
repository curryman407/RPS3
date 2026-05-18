package org.cloudstudios.rockpaperscissorspro.commands;

import org.bukkit.command.CommandSender;
import org.cloudstudios.rockpaperscissorspro.config.ConfigManager;
import org.cloudstudios.rockpaperscissorspro.config.MessageManager;
import org.cloudstudios.rockpaperscissorspro.core.StatsManager;
import org.cloudstudios.rockpaperscissorspro.util.ColorUtil;

public final class ReloadSubCommand {

    private final ConfigManager  configManager;
    private final MessageManager messageManager;
    private final StatsManager   statsManager;

    public ReloadSubCommand(final ConfigManager configManager,
                             final MessageManager messageManager,
                             final StatsManager statsManager) {
        this.configManager  = configManager;
        this.messageManager = messageManager;
        this.statsManager   = statsManager;
    }

    public void execute(final CommandSender sender) {
        statsManager.save();
        configManager.load();
        messageManager.load();
        statsManager.load();
        sender.sendMessage(ColorUtil.translate(
                "&8[&c&lRPS&8] &aConfiguration and stats reloaded successfully."));
    }
}
