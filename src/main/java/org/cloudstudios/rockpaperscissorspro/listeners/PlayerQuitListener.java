package org.cloudstudios.rockpaperscissorspro.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.cloudstudios.rockpaperscissorspro.core.GameLobbyManager;
import org.cloudstudios.rockpaperscissorspro.core.MatchManager;


public final class PlayerQuitListener implements Listener {

    private final MatchManager     matchManager;
    private final GameLobbyManager lobbyManager;

    public PlayerQuitListener(final MatchManager matchManager,
                               final GameLobbyManager lobbyManager) {
        this.matchManager = matchManager;
        this.lobbyManager = lobbyManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(final PlayerQuitEvent event) {
        final java.util.UUID uuid = event.getPlayer().getUniqueId();
        lobbyManager.cancelAllLobbiesForPlayer(uuid, false);
        matchManager.handlePlayerQuit(uuid);
    }
}
