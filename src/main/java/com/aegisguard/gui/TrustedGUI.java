package com.aegisguard.gui;

import com.aegisguard.AegisGuard;
import com.aegisguard.data.Plot;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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

        Inventory inv = Bukkit.createInventory(null, 54, ChatColor.YELLOW + "⚔ Guardian Codex ⚔ – Trusted Players");

        // Trusted players list
        int slot = 0;
        for (UUID trustedId : plot.getTrusted()) {
            if (slot >= 45) break;

            OfflinePlayer trusted = Bukkit.getOfflinePlayer(trustedId);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();

            if (meta != null) {
                meta.setOwningPlayer(trusted);
                meta.setDisplayName(ChatColor.GREEN + (trusted.getName() != null ? trusted.getName() : "Unknown"));
                meta.setLore(List.of(
                        ChatColor.GRAY + "✔ Trusted in your claim",
                        ChatColor.DARK_GRAY + "Click to remove this player"
                ));
                head.setItemMeta(meta);
            }
            inv.setItem(slot++, head);
        }

        // Add Trusted
        inv.setItem(45, GUIManager.icon(Material.EMERALD,
                ChatColor.GREEN + "Add Trusted",
                List.of(ChatColor.GRAY + "Pick an online player to trust.")));

        // Remove Trusted
        inv.setItem(46, GUIManager.icon(Material.BARRIER,
                ChatColor.RED + "Remove Trusted",
                List.of(ChatColor.GRAY + "Click on a head to remove.")));

        // Info & Guide
        inv.setItem(51, GUIManager.icon(Material.WRITABLE_BOOK,
                ChatColor.GOLD + "📖 Info & Guide",
                List.of(
                        ChatColor.GRAY + "Trusted players can build,",
                        ChatColor.GRAY + "but cannot unclaim or transfer ownership."
                )));

        // Back
        inv.setItem(52, GUIManager.icon(Material.ARROW,
                ChatColor.YELLOW + "Back",
                List.of(ChatColor.GRAY + "Return to Guardian Codex")));

        // Exit
        inv.setItem(53, GUIManager.icon(Material.BARRIER,
                ChatColor.RED + "Exit",
                List.of(ChatColor.GRAY + "Close this menu")));

        owner.openInventory(inv);
    }

    /* -----------------------------
     * Handle Trusted Menu Clicks
     * ----------------------------- */
    public void handleClick(Player player, InventoryClickEvent e) {
        e.setCancelled(true);

        if (e.getCurrentItem() == null) return;
        Material type = e.getCurrentItem().getType();

        String title = ChatColor.stripColor(e.getView().getTitle());
        Plot plot = plugin.store().getPlot(player.getUniqueId());

        // No plot
        if (plot == null) {
            plugin.msg().send(player, "no_plot_here");
            player.closeInventory();
            return;
        }

        // Trusted Players menu
        if (title.contains("Trusted Players")) {
            switch (type) {
                case PLAYER_HEAD -> {
                    // Remove trusted player
                    ItemStack head = e.getCurrentItem();
                    if (head.hasItemMeta() && head.getItemMeta() instanceof SkullMeta meta) {
                        OfflinePlayer target = meta.getOwningPlayer();
                        if (target != null && plot.isTrusted(target.getUniqueId())) {
                            plot.removeTrusted(target.getUniqueId());
                            plugin.msg().send(player, "trusted_removed", "PLAYER", target.getName());
                            open(player); // refresh GUI
                        }
                    }
                }
                case EMERALD -> {
                    // Open Add Trusted submenu
                    Inventory addMenu = Bukkit.createInventory(null, 54, ChatColor.GREEN + "Add Trusted Player");
                    int slot = 0;
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        if (slot >= 54) break;
                        if (online.getUniqueId().equals(player.getUniqueId())) continue; // skip self

                        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                        SkullMeta meta = (SkullMeta) head.getItemMeta();
                        if (meta != null) {
                            meta.setOwningPlayer(online);
                            meta.setDisplayName(ChatColor.YELLOW + online.getName());
                            meta.setLore(List.of(ChatColor.GRAY + "Click to trust this player"));
                            head.setItemMeta(meta);
                        }
                        addMenu.setItem(slot++, head);
                    }
                    player.openInventory(addMenu);
                }
                case ARROW -> plugin.gui().openMain(player); // Back
                case BARRIER -> player.closeInventory();     // Exit
                case WRITABLE_BOOK -> {
                    player.sendMessage(ChatColor.GOLD + "📖 Trusted Players Guide:");
                    player.sendMessage(ChatColor.GRAY + "Trusted players can build in your claim,");
                    player.sendMessage(ChatColor.GRAY + "but cannot unclaim or transfer ownership.");
                }
            }
        }

        // Add Trusted Player menu
        else if (title.contains("Add Trusted Player") && type == Material.PLAYER_HEAD) {
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
                        target.getPlayer().sendMessage(ChatColor.GREEN + "✔ " + player.getName() + " has trusted you in their plot.");
                    }
                    open(player); // back to main trusted GUI
                }
            }
        }
    }
}
