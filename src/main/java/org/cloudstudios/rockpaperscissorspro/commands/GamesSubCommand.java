package org.cloudstudios.rockpaperscissorspro.commands;

import org.bukkit.entity.Player;
import org.cloudstudios.rockpaperscissorspro.gui.GamesGui;

public final class GamesSubCommand {

    private final GamesGui gamesGui;

    public GamesSubCommand(final GamesGui gamesGui) {
        this.gamesGui = gamesGui;
    }

    public void execute(final Player player) {
        gamesGui.openGui(player, 0);
    }
}
