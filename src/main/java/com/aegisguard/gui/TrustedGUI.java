package com.aegisguard.gui;

import com.aegisguard.AegisGuard;
import com.aegisguard.data.Plot;
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
        Plot plot = plugin.store().getPlot(owner.getUniqueId());
        if (plot == null) {
            plugin.msg().send(owner, "no_plot_here");
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 54, plugin.msg().get("trusted_menu_title"));

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
                meta.setLore(plugin.msg().getList("trusted_menu_lore"));
                head.setItemMeta(meta);
            }
            inv.setItem(slot++, head);
        }

        // Add Trusted
        inv.setItem(45, GUIManager.icon(
                Material.EMERALD,
                plugin.msg().get("button_add_trusted"),
                plugin.msg().getList("add_trusted_lore")
        ));

        // Remove Trusted
        inv.setItem(46, GUIManager.icon(
                Material.BARRIER,
                plugin.msg().get("button_remove_trusted"),
                plugin.msg().getList("remove_trusted_lore")
        ));

        // Info & Guide
        inv.setItem(51, GUIManager.icon(
                Material.WRITABLE_BOOK,
                plugin.msg().get("button_info"),
                plugin.msg().getList("info_trusted_lore")
        ));

        // Back
        inv.setItem(52, GUIManager.icon(
                Material.ARROW,
                plugin.msg().get("button_back"),
                List.of(plugin.msg().color("&7" + plugin.msg().get("menu_title")))
        ));

        // Exit
        inv.setItem(53, GUIManager.icon(
                Material.BARRIER,
                plugin.msg().get("button_exit"),
                plugin.msg().getList("exit_lore")
        ));

        owner.openInventory(inv);
        owner.playSound(owner.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f); // 📖 page flip
    }

    /* -----------------------------
     * Open Remove Trusted Menu
     * ----------------------------- */
    private void openRemoveMenu(Player owner, Plot plot) {
        String title = plugin.msg().get("remove_trusted_title");
        Inventory inv = Bukkit.createInventory(null, 54, title);

        int slot = 0;
        for (UUID trustedId : plot.getTrusted()) {
            if (slot >= 54) break;

            OfflinePlayer trusted = Bukkit.getOfflinePlayer(trustedId);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();

            if (meta != null) {
                meta.setOwningPlayer(trusted);
                String playerName = trusted.getName() != null ? trusted.getName() : "Unknown";
                meta.setDisplayName(plugin.msg().color("&c" + playerName));
                meta.setLore(plugin.msg().getList("remove_trusted_lore"));
                head.setItemMeta(meta);
            }
            inv.setItem(slot++, head);
        }

        inv.setItem(52, GUIManager.icon(Material.ARROW, plugin.msg().get("button_back")));
        inv.setItem(53, GUIManager.icon(Material.BARRIER, plugin.msg().get("button_exit")));

        owner.openInventory(inv);
        owner.playSound(owner.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f); // 📖 page flip
    }

    /* -----------------------------
     * Open Confirmation Menu
     * ----------------------------- */
    private void openConfirmMenu(Player player, Plot plot, OfflinePlayer target) {
        Inventory confirm = Bukkit.createInventory(null, 27, "Confirm Remove: " + target.getName());

        // Player head
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(target);
            meta.setDisplayName(plugin.msg().color("&c" + target.getName()));
            meta.setLore(List.of(plugin.msg().color("&7Click Confirm to remove this player.")));
            head.setItemMeta(meta);
        }
        confirm.setItem(13, head);

        confirm.setItem(11, GUIManager.icon(Material.GREEN_WOOL, "§aConfirm Remove"));
        confirm.setItem(15, GUIManager.icon(Material.RED_WOOL, "§cCancel"));

        player.openInventory(confirm);
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f); // 📖 page flip
    }

    /* -----------------------------
     * Handle Clicks
     * ----------------------------- */
    public void handleClick(Player player, InventoryClickEvent e) {
        e.setCancelled(true);
        if (e.getCurrentItem() == null) return;

        Plot plot = plugin.store().getPlot(player.getUniqueId());
        if (plot == null) {
            player.closeInventory();
            plugin.msg().send(player, "no_plot_here");
            return;
        }

        String title = e.getView().getTitle();
        Material type = e.getCurrentItem().getType();

        // Trusted Players menu
        if (title.equals(plugin.msg().get("trusted_menu_title"))) {
            switch (type) {
                case EMERALD -> {
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
                            meta.setLore(plugin.msg().getList("add_trusted_lore"));
                            head.setItemMeta(meta);
                        }
                        addMenu.setItem(slot++, head);
                    }
                    player.openInventory(addMenu);
                    player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f); // page flip
                }
                case BARRIER -> openRemoveMenu(player, plot);
                case ARROW -> {
                    plugin.gui().openMain(player);
                    player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f); // page flip
                }
                case WRITABLE_BOOK -> {
                    plugin.msg().getList("info_trusted_lore").forEach(player::sendMessage);
                    player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1.2f); // info page turn
                }
                case Material.BARRIER -> {
                    player.closeInventory();
                    player.playSound(player.getLocation(), Sound.ITEM_BOOK_PUT, 1f, 0.8f); // book closing
                }
            }
        }

        // Add Trusted Player
        else if (title.equals(plugin.msg().get("add_trusted_title")) && type == Material.PLAYER_HEAD) {
            ItemStack head = e.getCurrentItem();
            if (head.getItemMeta() instanceof SkullMeta meta) {
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
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                    open(player);
                }
            }
        }

        // Remove Trusted submenu
        else if (title.equals(plugin.msg().get("remove_trusted_title")) && type == Material.PLAYER_HEAD) {
            ItemStack head = e.getCurrentItem();
            if (head.getItemMeta() instanceof SkullMeta meta) {
                OfflinePlayer target = meta.getOwningPlayer();
                if (target != null && plot.isTrusted(target.getUniqueId())) {
                    openConfirmMenu(player, plot, target);
                }
            }
        }

        // Confirmation Menu
        else if (title.startsWith("Confirm Remove:")) {
            if (type == Material.GREEN_WOOL) {
                ItemStack targetHead = e.getInventory().getItem(13);
                if (targetHead != null && targetHead.getItemMeta() instanceof SkullMeta meta) {
                    OfflinePlayer target = meta.getOwningPlayer();
                    if (target != null && plot.isTrusted(target.getUniqueId())) {
                        plot.removeTrusted(target.getUniqueId());
                        plugin.msg().send(player, "trusted_removed", "PLAYER", target.getName());
                        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1f, 1f); // confirm = anvil
                        openRemoveMenu(player, plot);
                    }
                }
            } else if (type == Material.RED_WOOL) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f); // cancel = bass
                openRemoveMenu(player, plot);
            }
        }
    }
}
