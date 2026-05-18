package org.cloudstudios.rockpaperscissorspro.gui;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.plugin.Plugin;
import org.cloudstudios.rockpaperscissorspro.config.ConfigManager;
import org.cloudstudios.rockpaperscissorspro.core.MatchManager;
import org.cloudstudios.rockpaperscissorspro.core.MatchState;
import org.cloudstudios.rockpaperscissorspro.core.RPSChoice;
import org.cloudstudios.rockpaperscissorspro.core.RPSMatch;

import org.bukkit.Bukkit;


public final class GuiListener implements Listener {

    private final MatchManager  matchManager;
    private final ConfigManager configManager;
    private final Plugin        plugin;

    public GuiListener(final MatchManager matchManager,
                        final ConfigManager configManager,
                        final Plugin plugin) {
        this.matchManager  = matchManager;
        this.configManager = configManager;
        this.plugin        = plugin;
    }

    private boolean isChoiceGui(final org.bukkit.inventory.InventoryView view) {
        if (view == null || view.getTopInventory().getSize() != 27) return false;
        final String plain = PlainTextComponentSerializer.plainText().serialize(view.title());
        return plain.contains("[RPS]");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(final InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof final Player player)) return;
        if (!isChoiceGui(event.getView())) return;
        event.setCancelled(true);

        if (event.getClickedInventory() == null
                || event.getClickedInventory().getType() == InventoryType.PLAYER) return;

        if (event.getClick() == ClickType.DOUBLE_CLICK || event.isShiftClick()) return;

        final RPSMatch match = matchManager.getMatch(player.getUniqueId());
        if (match == null || match.getState() != MatchState.SELECTING) return;
        if (match.hasChosen(player.getUniqueId())) return;

        final RPSChoice choice = getChoice(event.getRawSlot());
        if (choice == null) return;


        matchManager.submitChoice(player.getUniqueId(), choice);
        player.closeInventory();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryDrag(final InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (isChoiceGui(event.getView())) event.setCancelled(true);
    }


    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(final InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof final Player player)) return;
        if (!isChoiceGui(event.getView())) return;

        final RPSMatch match = matchManager.getMatch(player.getUniqueId());
        if (match == null) return;
        if (match.getState() != MatchState.SELECTING) return;

        if (match.hasChosen(player.getUniqueId())) return;

        Bukkit.getScheduler().runTask(plugin, () -> matchManager.handlePlayerQuit(player.getUniqueId()));
    }

    private RPSChoice getChoice(final int slot) {
        if (slot == configManager.getRockSlot())     return RPSChoice.ROCK;
        if (slot == configManager.getPaperSlot())    return RPSChoice.PAPER;
        if (slot == configManager.getScissorsSlot()) return RPSChoice.SCISSORS;
        return null;
    }
}
