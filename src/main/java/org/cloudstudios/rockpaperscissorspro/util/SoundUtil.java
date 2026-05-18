package org.cloudstudios.rockpaperscissorspro.util;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.cloudstudios.rockpaperscissorspro.config.ConfigManager;

import java.util.logging.Logger;

public final class SoundUtil {

    private final ConfigManager configManager;
    private final Logger        logger;

    public SoundUtil(final ConfigManager configManager, final Logger logger) {
        this.configManager = configManager;
        this.logger        = logger;
    }


    public void play(final Player player, final String soundKey) {
        if (player == null || !player.isOnline()) return;
        final String soundName = configManager.getSound(soundKey);
        if (soundName == null || soundName.equalsIgnoreCase("NONE")) return;
        try {
            final Sound sound = Sound.valueOf(soundName.toUpperCase());
            player.playSound(player.getLocation(), sound,
                    (float) configManager.getSoundVolume(soundKey),
                    (float) configManager.getSoundPitch(soundKey));
        } catch (final IllegalArgumentException e) {
            logger.warning("[RPS] Invalid sound '" + soundName + "' for key '" + soundKey + "'");
        }
    }
}
