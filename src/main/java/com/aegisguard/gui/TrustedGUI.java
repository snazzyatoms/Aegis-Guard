package com.aegisguard.gui;

import com.aegisguard.AegisGuard;
import com.aegisguard.data.PlotStore.Plot;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;
import java.util.UUID;

public class TrustedGUI {

    private final AegisGuard plugin;

    public TrustedGUI(AegisGuard plugin) {
        this.plugin = plugin;
    }

    /* -----------------------------
     * Open Trusted Menu
     * ----------------------------- */
    public void open(Player owner) {
        List<Plot> plots = plugin.store().getPlots(owner.getUniqueId());
        if (plots == null || plots.isEmpty()) {
            plugin.msg().send(owner, "no_plot_here");
            return;
        }
        Plot plot = plots.get(0);

        Inventory inv = Bukkit.createInventory(null, 54, m("trusted_menu_title", "§b§lAegisGuard §7— Trusted"));

        // Trusted players list
        int slot = 0;
        for (UUID trustedId : plot.getTrusted()) {
            if (slot >= 45) break;

            OfflinePlayer trusted = Bukkit.getOfflinePlayer(trustedId);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();

            if (meta != null) {
                meta.setOwningPlayer(trusted);
                String playerName = trusted.getName() != null ? trusted.getName() : "Unknown";
                meta.setDisplayName(plugin.msg().color("&a" + playerName));
                meta.setLore(l("trusted_menu_lore", List.of("§7Click a head to remove from trusted.")));
                head.setItemMeta(meta);
            }
            inv.setItem(slot++, head);
        }

        // Add Trusted
        inv.setItem(45, GUIManager.icon(
                Material.EMERALD,
                m("button_add_trusted", "§aAdd Trusted"),
                l("add_trusted_lore", List.of("§7Pick an online player to add."))));

        // Remove Trusted
        inv.setItem(46, GUIManager.icon(
                Material.REDSTONE_BLOCK,
                m("button_remove_trusted", "§cRemove Trusted"),
                l("remove_trusted_lore", List.of("§7Choose someone to remove."))));

        // Roles (placeholder)
        inv.setItem(47, GUIManager.icon(
                Material.NAME_TAG,
                m("button_roles", "§eRoles (Soon)"),
                l("roles_lore", List.of("§7Role-based permissions are coming."))));

        // Info & Guide
        inv.setItem(51, GUIManager.icon(
                Material.WRITABLE_BOOK,
                m("button_info", "§fInfo"),
                l("info_trusted_lore", List.of("§7Trusted players can help build on your land."))));

        // Back
        inv.setItem(52, GUIManager.icon(
                Material.ARROW,
                m("button_back", "§7Back"),
                l("back_lore", List.of("§7Go back to the main menu."))));

        // Exit
        inv.setItem(53, GUIManager.icon(
                Material.BARRIER,
                m("button_exit", "§cExit"),
                l("exit_lore", List.of("§7Close this menu."))));

        owner.openInventory(inv);
        playOpen(owner);
    }

    /* -----------------------------
     * Handle Trusted Menu Clicks
     * ----------------------------- */
    public void handleClick(Player player, InventoryClickEvent e) {
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;

        String title = e.getView().getTitle();
        List<Plot> plots = plugin.store().getPlots(player.getUniqueId());
        if (plots == null || plots.isEmpty()) {
            plugin.msg().send(player, "no_plot_here");
            player.closeInventory();
            playClose(player);
            return;
        }
        Plot plot = plots.get(0);

        String tMain    = m("trusted_menu_title", "AegisGuard — Trusted");
        String tAdd     = m("add_trusted_title", "AegisGuard — Add Trusted");
        String tRemove  = m("remove_trusted_title", "AegisGuard — Remove Trusted");

        // MAIN Trusted menu
        if (title.equals(tMain)) {
            switch (clicked.getType()) {
                case PLAYER_HEAD -> { // quick remove shortcut
                    SkullMeta meta = (SkullMeta) clicked.getItemMeta();
                    if (meta != null) {
                        OfflinePlayer target = meta.getOwningPlayer();
                        if (target != null && plot.getTrusted().contains(target.getUniqueId())) {
                            plot.getTrusted().remove(target.getUniqueId());
                            plugin.msg().send(player, "trusted_removed", "PLAYER", safeName(target));
                            playFlip(player);
                            open(player);
                        }
                    }
                }
                case EMERALD -> { // open Add menu
                    openAddMenu(player);
                    playFlip(player);
                }
                case REDSTONE_BLOCK -> { // open Remove menu
                    openRemoveMenu(player, plot);
                    playFlip(player);
                }
                case NAME_TAG -> {
                    // Placeholder
                    playClose(player);
                }
                case ARROW -> {
                    plugin.gui().openMain(player);
                    playFlip(player);
                }
                case BARRIER -> {
                    player.closeInventory();
                    playClose(player);
                }
                case WRITABLE_BOOK -> {
                    for (String line : l("info_trusted_lore", List.of("§7Trusted players can help build on your land."))) {
                        player.sendMessage(line);
                    }
                    playFlip(player);
                }
                default -> {}
            }
            return;
        }

        // ADD Trusted menu
        if (title.equals(tAdd)) {
            if (clicked.getType() == Material.PLAYER_HEAD) {
                SkullMeta meta = (SkullMeta) clicked.getItemMeta();
                if (meta != null) {
                    OfflinePlayer target = meta.getOwningPlayer();
                    if (target != null && !target.getUniqueId().equals(player.getUniqueId())) {
                        if (plot.getTrusted().add(target.getUniqueId())) {
                            plugin.msg().send(player, "trusted_added", "PLAYER", safeName(target));
                            playFlip(player);
                        } else {
                            plugin.msg().send(player, "trusted_already", "PLAYER", safeName(target));
                        }
                        open(player); // back to main trusted list
                    }
                }
            }
            return;
        }

        // REMOVE Trusted menu
        if (title.equals(tRemove)) {
            if (clicked.getType() == Material.PLAYER_HEAD) {
                SkullMeta meta = (SkullMeta) clicked.getItemMeta();
                if (meta != null) {
                    OfflinePlayer target = meta.getOwningPlayer();
                    if (target != null && plot.getTrusted().remove(target.getUniqueId())) {
                        plugin.msg().send(player, "trusted_removed", "PLAYER", safeName(target));
                        playFlip(player);
                    }
                    open(player); // back to main trusted list
                }
            }
        }
    }

    /* -----------------------------
     * Sub-menus
     * ----------------------------- */
    private void openAddMenu(Player player) {
        String addTitle = m("add_trusted_title", "§b§lAegisGuard §7— Add Trusted");
        Inventory addMenu = Bukkit.createInventory(null, 54, addTitle);

        int slot = 0;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (slot >= 54) break;
            if (online.getUniqueId().equals(player.getUniqueId())) continue;

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(online);
                meta.setDisplayName(plugin.msg().color("&e" + online.getName()));
                meta.setLore(l("add_trusted_lore", List.of("§7Click to add to trusted.")));
                head.setItemMeta(meta);
            }
            addMenu.setItem(slot++, head);
        }
        player.openInventory(addMenu);
    }

    private void openRemoveMenu(Player player, Plot plot) {
        String removeTitle = m("remove_trusted_title", "§b§lAegisGuard §7— Remove Trusted");
        Inventory removeMenu = Bukkit.createInventory(null, 54, removeTitle);

        int slot = 0;
        for (UUID trustedId : plot.getTrusted()) {
            if (slot >= 54) break;
            OfflinePlayer trusted = Bukkit.getOfflinePlayer(trustedId);

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(trusted);
                meta.setDisplayName(plugin.msg().color("&c" + safeName(trusted)));
                meta.setLore(l("remove_trusted_lore", List.of("§7Click to remove from trusted.")));
                head.setItemMeta(meta);
            }
            removeMenu.setItem(slot++, head);
        }
        player.openInventory(removeMenu);
    }

    /* -----------------------------
     * Helpers
     * ----------------------------- */
    private String safeName(OfflinePlayer p) {
        String n = p.getName();
        return (n == null || n.isEmpty()) ? "Unknown" : n;
    }

    private String m(String key, String fallback) {
        String v = plugin.msg().get(key);
        return (v == null || v.isEmpty()) ? fallback : v;
    }
    private List<String> l(String key, List<String> fallback) {
        List<String> v = plugin.msg().getList(key);
        return (v == null || v.isEmpty()) ? fallback : v;
    }

    private void playOpen(Player p) { if (plugin.isSoundEnabled(p)) p.playSound(p.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.7f, 1.0f); }
    private void playClose(Player p){ if (plugin.isSoundEnabled(p)) p.playSound(p.getLocation(), Sound.BLOCK_CHEST_CLOSE, 0.7f, 1.0f); }
    private void playFlip(Player p) { if (plugin.isSoundEnabled(p)) p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f); }
}
