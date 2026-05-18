package org.cloudstudios.rockpaperscissorspro.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.cloudstudios.rockpaperscissorspro.config.ConfigManager;
import org.cloudstudios.rockpaperscissorspro.util.ColorUtil;

import java.util.ArrayList;
import java.util.List;

public final class RPSGui {

    private final ConfigManager configManager;

    public RPSGui(final ConfigManager configManager) {
        this.configManager = configManager;
    }

    public void openGui(final Player player, final String opponentName) {
        if (player == null || !player.isOnline()) return;
        final String rawTitle = configManager.getGuiTitle().replace("%opponent%", opponentName);
        final Inventory inv   = Bukkit.createInventory(null, 27, ColorUtil.translate(rawTitle));

        final ItemStack filler = buildFiller();
        for (int i = 0; i < 27; i++) inv.setItem(i, filler);

        inv.setItem(configManager.getRockSlot(),
                buildItem(configManager.getRockMaterial(),     configManager.getRockName(),     configManager.getRockLore()));
        inv.setItem(configManager.getPaperSlot(),
                buildItem(configManager.getPaperMaterial(),    configManager.getPaperName(),    configManager.getPaperLore()));
        inv.setItem(configManager.getScissorsSlot(),
                buildItem(configManager.getScissorsMaterial(), configManager.getScissorsName(), configManager.getScissorsLore()));

        player.openInventory(inv);
    }

    private ItemStack buildFiller() {
        final ItemStack item = new ItemStack(parseMaterial(configManager.getFillerMaterial(), Material.BLACK_STAINED_GLASS_PANE));
        final ItemMeta  meta = item.getItemMeta();
        if (meta != null) {
            final String name = configManager.getFillerName();
            meta.displayName(name == null || name.isEmpty() ? Component.empty() : ColorUtil.translate(name));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack buildItem(final String materialName,
                                 final String displayName,
                                 final List<String> lore) {
        final ItemStack item = new ItemStack(parseMaterial(materialName, Material.STONE));
        final ItemMeta  meta = item.getItemMeta();
        if (meta == null) return item;
        meta.displayName(ColorUtil.translate(displayName));
        final List<Component> loreComponents = new ArrayList<>();
        for (final String line : lore) loreComponents.add(ColorUtil.translate(line));
        meta.lore(loreComponents);
        item.setItemMeta(meta);
        return item;
    }


    static Material parseMaterial(final String name, final Material fallback) {
        if (name == null || name.isBlank()) return fallback;
        try { return Material.valueOf(name.toUpperCase()); }
        catch (final IllegalArgumentException e) { return fallback; }
    }
}
