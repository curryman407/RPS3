package org.cloudstudios.rockpaperscissorspro.match;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.cloudstudios.rockpaperscissorspro.core.MatchManager;
import org.cloudstudios.rockpaperscissorspro.core.MatchState;
import org.cloudstudios.rockpaperscissorspro.core.RPSMatch;


public final class MatchListener implements Listener {

    private final MatchManager matchManager;

    public MatchListener(final MatchManager matchManager) {
        this.matchManager = matchManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDropItem(final PlayerDropItemEvent event) {
        final RPSMatch m = matchManager.getMatch(event.getPlayer().getUniqueId());
        if (m != null && m.getState() != MatchState.FINISHED) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(final PlayerInteractEvent event) {
        final RPSMatch m = matchManager.getMatch(event.getPlayer().getUniqueId());
        if (m != null && m.getState() == MatchState.COUNTDOWN) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemPickup(final EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof final Player player)) return;
        final RPSMatch m = matchManager.getMatch(player.getUniqueId());
        if (m != null && m.getState() != MatchState.FINISHED) event.setCancelled(true);
    }
}
