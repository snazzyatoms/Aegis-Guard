package com.aegisguard.gui;

import com.aegisguard.AegisGuard;
import com.aegisguard.data.Plot;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
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
        Plot plot = plugin.store().getPlot(owner.getUniqueId());
        if (plot == null) {
            plugin.msg().send(owner, "no_plot_here");
            return;
        }

        String title = plugin.msg().get("trusted_menu_title");
        Inventory inv = Bukkit.createInventory(null, 54, title);

        // Trusted players list
        int slot = 0;
        for (UUID trustedId : plot.getTrusted()) {
            if (slot >= 45) break;

            OfflinePlayer trusted = Bukkit.getOfflinePlayer(trustedId);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();

            if (meta != null) {
                meta.setOwningPlayer(trusted);
                meta.setDisplayName(plugin.msg().color("&a" + (trusted.getName() != null ? trusted.getName() : "Unknown")));
                meta.setLore(plugin.msg().getList("trusted_menu_lore"));
                head.setItemMeta(meta);
            }
            inv.setItem(slot++, head);
        }

        // Add Trusted
        inv.setItem(45, GUIManager.icon(Material.EMERALD,
                plugin.msg().get("button_add_trusted"),
                plugin.msg().getList("add_trusted_lore")));

        // Remove Trusted
        inv.setItem(46, GUIManager.icon(Material.BARRIER,
                plugin.msg().get("button_remove_trusted"),
                plugin.msg().getList("remove_trusted_lore")));

        // Info & Guide
        inv.setItem(51, GUIManager.icon(Material.WRITABLE_BOOK,
                plugin.msg().get("button_info"),
                plugin.msg().getList("info_trusted_lore")));

        // Back
        inv.setItem(52, GUIManager.icon(Material.ARROW,
                plugin.msg().get("button_back"),
                List.of(plugin.msg().get("menu_title"))));

        // Exit
        inv.setItem(53, GUIManager.icon(Material.BARRIER,
                plugin.msg().get("button_exit"),
                List.of(plugin.msg().color("&7Close this menu"))));

        owner.openInventory(inv);
    }

    /* -----------------------------
     * Handle Trusted Menu Clicks
     * ----------------------------- */
    public void handleClick(Player player, InventoryClickEvent e) {
        e.setCancelled(true);

        if (e.getCurrentItem() == null) return;
        Material type = e.getCurrentItem().getType();

        String title = e.getView().getTitle();
        Plot plot = plugin.store().getPlot(player.getUniqueId());

        if (plot == null) {
            plugin.msg().send(player, "no_plot_here");
            player.closeInventory();
            return;
        }

        // Trusted Players menu
        if (title.equalsIgnoreCase(plugin.msg().get("trusted_menu_title"))) {
            switch (type) {
                case PLAYER_HEAD -> {
                    ItemStack head = e.getCurrentItem();
                    if (head.hasItemMeta() && head.getItemMeta() instanceof SkullMeta meta) {
                        OfflinePlayer target = meta.getOwningPlayer();
                        if (target != null && plot.isTrusted(target.getUniqueId())) {
                            plot.removeTrusted(target.getUniqueId());
                            plugin.msg().send(player, "trusted_removed", "PLAYER", target.getName());
                            open(player);
                        }
                    }
                }
                case EMERALD -> {
                    // Open Add Trusted submenu
                    String addTitle = plugin.msg().get("add_trusted_title");
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
                            meta.setLore(List.of(plugin.msg().color("&7Click to trust this player")));
                            head.setItemMeta(meta);
                        }
                        addMenu.setItem(slot++, head);
                    }
                    player.openInventory(addMenu);
                }
                case ARROW -> plugin.gui().openMain(player);
                case BARRIER -> player.closeInventory();
                case WRITABLE_BOOK -> {
                    for (String line : plugin.msg().getList("info_trusted_lore")) {
                        player.sendMessage(line);
                    }
                }
            }
        }

        // Add Trusted Player menu
        else if (title.equalsIgnoreCase(plugin.msg().get("add_trusted_title")) && type == Material.PLAYER_HEAD) {
            ItemStack head = e.getCurrentItem();
            if (head.hasItemMeta() && head.getItemMeta() instanceof SkullMeta meta) {
                OfflinePlayer target = meta.getOwningPlayer();
                if (target != null) {
                    if (target.getUniqueId().equals(player.getUniqueId())) {
                        plugin.msg().send(player, "trusted_self");
                        return;
                    }
                    if (plot.isTrusted(target.getUniqueId())) {
                        plugin.msg().send(player, "trusted_already", "PLAYER", target.getName());
                        return;
                    }
                    plot.addTrusted(target.getUniqueId());
                    plugin.msg().send(player, "trusted_added", "PLAYER", target.getName());
                    if (target.isOnline()) {
                        plugin.msg().send(target.getPlayer(), "trusted_added_target", "PLAYER", player.getName());
                    }
                    open(player);
                }
            }
        }
    }
}
