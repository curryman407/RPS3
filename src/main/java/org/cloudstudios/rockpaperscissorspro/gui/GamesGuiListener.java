package org.cloudstudios.rockpaperscissorspro.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.cloudstudios.rockpaperscissorspro.config.MessageManager;
import org.cloudstudios.rockpaperscissorspro.core.GameLobbyManager;

import java.util.UUID;


public final class GamesGuiListener implements Listener {

    private final GamesGui         gamesGui;
    private final GameLobbyManager lobbyManager;
    private final MessageManager   messageManager;

    public GamesGuiListener(final GamesGui gamesGui,
                             final GameLobbyManager lobbyManager,
                             final MessageManager messageManager) {
        this.gamesGui      = gamesGui;
        this.lobbyManager  = lobbyManager;
        this.messageManager = messageManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(final InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof final Player player)) return;
        if (!gamesGui.isViewingGamesGui(player.getUniqueId())) return;
        event.setCancelled(true);

        if (event.getClickedInventory() == null
                || event.getClickedInventory().getType() == InventoryType.PLAYER) return;

        final ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        final ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;
        final PersistentDataContainer pdc = meta.getPersistentDataContainer();

        if (pdc.has(gamesGui.PAGE_KEY, PersistentDataType.INTEGER)) {
            final int targetPage = pdc.get(gamesGui.PAGE_KEY, PersistentDataType.INTEGER);
            gamesGui.setViewerPage(player.getUniqueId(), targetPage);
            gamesGui.openGui(player, targetPage);
            return;
        }

        if (!pdc.has(gamesGui.LOBBY_KEY, PersistentDataType.STRING)) return;
        final String idStr = pdc.get(gamesGui.LOBBY_KEY, PersistentDataType.STRING);
        if (idStr == null) return;

        final UUID lobbyId;
        try { lobbyId = UUID.fromString(idStr); }
        catch (final IllegalArgumentException e) { return; }

        final var lobby = lobbyManager.getLobby(lobbyId);
        if (lobby == null) {
            messageManager.send(player, "game-not-found");
            gamesGui.openGui(player, 0);
            return;
        }

        if (lobby.getCreatorUuid().equals(player.getUniqueId())) {
            player.closeInventory();
            lobbyManager.cancelLobby(lobbyId, true);
        } else {
            player.closeInventory();
            lobbyManager.acceptLobby(lobbyId, player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryDrag(final InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (gamesGui.isViewingGamesGui(event.getWhoClicked().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(final InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof final Player player)) return;
        gamesGui.onViewerClose(player.getUniqueId());
    }
}
