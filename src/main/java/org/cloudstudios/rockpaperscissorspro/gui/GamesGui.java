package org.cloudstudios.rockpaperscissorspro.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.cloudstudios.rockpaperscissorspro.RockPaperScissorsPro;
import org.cloudstudios.rockpaperscissorspro.config.ConfigManager;
import org.cloudstudios.rockpaperscissorspro.core.GameLobby;
import org.cloudstudios.rockpaperscissorspro.core.GameLobbyManager;
import org.cloudstudios.rockpaperscissorspro.core.MatchManager;
import org.cloudstudios.rockpaperscissorspro.core.PlayerStats;
import org.cloudstudios.rockpaperscissorspro.core.StatsManager;
import org.cloudstudios.rockpaperscissorspro.util.ColorUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;


public final class GamesGui {

    public final NamespacedKey LOBBY_KEY;
    public final NamespacedKey PAGE_KEY;

    private final ConfigManager  configManager;
    private final StatsManager   statsManager;
    private final MatchManager   matchManager;
    private GameLobbyManager     lobbyManager;   // injected after construction


    private final Map<UUID, Integer> viewerPages = new HashMap<>();


    private boolean refreshing = false;

    public GamesGui(final RockPaperScissorsPro plugin,
                    final ConfigManager configManager,
                    final StatsManager statsManager,
                    final MatchManager matchManager) {
        this.LOBBY_KEY    = new NamespacedKey(plugin, "rps_lobby_id");
        this.PAGE_KEY     = new NamespacedKey(plugin, "rps_page");
        this.configManager = configManager;
        this.statsManager  = statsManager;
        this.matchManager  = matchManager;
    }

    public void setLobbyManager(final GameLobbyManager lm) { this.lobbyManager = lm; }




    public void openGui(final Player player) {
        openGui(player, 0);
    }


    public void openGui(final Player player, final int page) {
        if (player == null || !player.isOnline() || lobbyManager == null) return;
        viewerPages.put(player.getUniqueId(), page);
        player.openInventory(buildInventory(player, page));
    }


    public void refreshAllViewers() {
        if (viewerPages.isEmpty()) return;
        refreshing = true;
        try {
            for (final UUID uuid : Set.copyOf(viewerPages.keySet())) {
                final Player p = Bukkit.getPlayer(uuid);
                if (p == null || !p.isOnline()) {
                    viewerPages.remove(uuid);
                    continue;
                }
                final int page = viewerPages.getOrDefault(uuid, 0);
                p.openInventory(buildInventory(p, page));
            }
        } finally {
            refreshing = false;
        }
    }


    public boolean isViewingGamesGui(final UUID uuid) {
        return viewerPages.containsKey(uuid);
    }

    public void onViewerClose(final UUID uuid) {
        if (!refreshing) viewerPages.remove(uuid);
    }


    public void setViewerPage(final UUID uuid, final int page) {
        viewerPages.put(uuid, page);
    }

    private Inventory buildInventory(final Player viewer, final int page) {

        final Component title = ColorUtil.translate(configManager.getGamesGuiTitle());
        final Inventory inv   = Bukkit.createInventory(null, 54, title);


        final ItemStack filler = buildFiller();
        for (int i = 0; i < 54; i++) inv.setItem(i, filler);

        final List<Integer> gameSlots  = configManager.getGameSlots();
        final List<GameLobby> allLobbies = lobbyManager != null
                ? lobbyManager.getAllLobbies()
                : new ArrayList<>();

        final int gamesPerPage = gameSlots.size();
        final int totalPages   = allLobbies.isEmpty()
                ? 1 : (int) Math.ceil(allLobbies.size() / (double) gamesPerPage);
        final int safePage     = Math.max(0, Math.min(page, totalPages - 1));
        final int start        = safePage * gamesPerPage;
        final int end          = Math.min(start + gamesPerPage, allLobbies.size());

        if (allLobbies.isEmpty()) {
            final int midSlot = gameSlots.get(gameSlots.size() / 2);
            inv.setItem(midSlot, buildNoGamesItem());
        } else {
            int slotIdx = 0;
            for (int i = start; i < end && slotIdx < gameSlots.size(); i++, slotIdx++) {
                inv.setItem(gameSlots.get(slotIdx), buildLobbyItem(viewer, allLobbies.get(i)));
            }
        }

        inv.setItem(configManager.getStatsSlot(), buildStatsItem(viewer));

        if (safePage > 0) {
            inv.setItem(configManager.getPrevPageSlot(), buildNavItem(false, safePage - 1));
        }
        if (safePage < totalPages - 1) {
            inv.setItem(configManager.getNextPageSlot(), buildNavItem(true, safePage + 1));
        }

        return inv;
    }


    private ItemStack buildFiller() {
        final Material  mat  = RPSGui.parseMaterial(configManager.getGamesFillerMaterial(), Material.BLACK_STAINED_GLASS_PANE);
        final ItemStack item = new ItemStack(mat);
        final ItemMeta  meta = item.getItemMeta();
        if (meta != null) {
            final String name = configManager.getGamesFillerName();
            meta.displayName(name == null || name.isEmpty() ? Component.empty() : ColorUtil.translate(name));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack buildLobbyItem(final Player viewer, final GameLobby lobby) {
        final boolean isOwner   = lobby.getCreatorUuid().equals(viewer.getUniqueId());
        final Player  creator   = Bukkit.getPlayer(lobby.getCreatorUuid());
        final boolean available = creator != null && creator.isOnline()
                && !matchManager.isInMatch(lobby.getCreatorUuid());

        final String amtStr = String.format("%,d", lobby.getBetAmount());
        final String winStr = String.format("%,d", lobby.getBetAmount() * 2);

        final ItemStack item;
        final String    displayName;
        final List<String> rawLore;

        if (!available && !isOwner) {
            item        = new ItemStack(Material.BARRIER);
            displayName = configManager.getUnavailableGameName();
            rawLore     = configManager.getUnavailableGameLore();
        } else if (isOwner) {
            item = buildSkullForPlayer(creator != null ? creator : viewer);
            displayName = configManager.getYourGameName();
            rawLore     = configManager.getYourGameLore();
        } else {
            item = buildSkullForPlayer(creator);
            displayName = configManager.getOtherGameName().replace("%player%", lobby.getCreatorName());
            rawLore     = configManager.getOtherGameLore();
        }

        final ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(ColorUtil.translate(displayName));

        final List<Component> loreComponents = new ArrayList<>();
        for (final String line : rawLore) {
            loreComponents.add(ColorUtil.translate(
                    line.replace("%amount%",   amtStr)
                        .replace("%winnings%", winStr)
                        .replace("%player%",   lobby.getCreatorName())));
        }
        meta.lore(loreComponents);

        meta.getPersistentDataContainer()
            .set(LOBBY_KEY, PersistentDataType.STRING, lobby.getLobbyId().toString());

        item.setItemMeta(meta);
        return item;
    }


    private ItemStack buildSkullForPlayer(final Player player) {
        final ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        if (player == null) return skull;
        final SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta == null) return skull;
        meta.setOwningPlayer(player);
        skull.setItemMeta(meta);
        return skull;
    }

    private ItemStack buildNoGamesItem() {
        final ItemStack item = new ItemStack(RPSGui.parseMaterial(configManager.getNoGamesMaterial(), Material.BARRIER));
        final ItemMeta  meta = item.getItemMeta();
        if (meta == null) return item;
        meta.displayName(ColorUtil.translate(configManager.getNoGamesName()));
        final List<Component> lore = new ArrayList<>();
        for (final String line : configManager.getNoGamesLore()) lore.add(ColorUtil.translate(line));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildStatsItem(final Player viewer) {
        final ItemStack  item  = new ItemStack(RPSGui.parseMaterial(configManager.getStatsMaterial(), Material.BOOK));
        final ItemMeta   meta  = item.getItemMeta();
        if (meta == null) return item;
        final PlayerStats stats = statsManager.getStats(viewer.getUniqueId());
        meta.displayName(ColorUtil.translate(configManager.getStatsName()));
        final List<Component> lore = new ArrayList<>();
        lore.add(ColorUtil.translate("&8│ &7Played: &e" + stats.getPlayed()));
        lore.add(ColorUtil.translate("&8│ &7Won:    &a" + stats.getWon()    + " &8(" + stats.getWinPercent()  + ")"));
        lore.add(ColorUtil.translate("&8│ &7Lost:   &c" + stats.getLost()   + " &8(" + stats.getLossPercent() + ")"));
        lore.add(ColorUtil.translate("&8│ &7Tied:   &e" + stats.getTied()));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildNavItem(final boolean next, final int targetPage) {
        final String matName = next ? configManager.getNextPageMaterial() : configManager.getPrevPageMaterial();
        final ItemStack item = new ItemStack(RPSGui.parseMaterial(matName, Material.ARROW));
        final ItemMeta  meta = item.getItemMeta();
        if (meta == null) return item;
        meta.displayName(ColorUtil.translate(next ? configManager.getNextPageName() : configManager.getPrevPageName()));
        meta.getPersistentDataContainer().set(PAGE_KEY, PersistentDataType.INTEGER, targetPage);
        item.setItemMeta(meta);
        return item;
    }
}
