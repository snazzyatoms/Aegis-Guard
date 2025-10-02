package com.aegisguard.gui;

import com.aegisguard.AegisGuard;
import com.aegisguard.data.Plot;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
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
            if (slot >= 45) break; // top 5 rows only

            OfflinePlayer trusted = Bukkit.getOfflinePlayer(trustedId);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();

            if (meta != null) {
                meta.setOwningPlayer(trusted);
                meta.setDisplayName(ChatColor.GREEN + (trusted.getName() != null ? trusted.getName() : "Unknown"));

                // Lore with player info
                meta.setLore(List.of(
                        ChatColor.GRAY + "✔ Trusted in your claim",
                        ChatColor.DARK_GRAY + "Shift + Right-click: remove"
                ));

                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                head.setItemMeta(meta);
            }

            inv.setItem(slot++, head);
        }

        // Add Trusted
        inv.setItem(45, GUIManager.icon(Material.EMERALD,
                ChatColor.GREEN + "Add Trusted",
                List.of(ChatColor.GRAY + "Pick a player to add as trusted.")));

        // Remove Trusted
        inv.setItem(46, GUIManager.icon(Material.BARRIER,
                ChatColor.RED + "Remove Trusted",
                List.of(ChatColor.GRAY + "Select a player head to remove them.")));

        // Info & Guide
        inv.setItem(51, GUIManager.icon(Material.WRITABLE_BOOK,
                ChatColor.GOLD + "📖 Info & Guide",
                List.of(
                        ChatColor.GRAY + "Manage who can build in your claim.",
                        ChatColor.GRAY + "Trusted players can help you, but",
                        ChatColor.GRAY + "cannot unclaim or transfer ownership."
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
}
