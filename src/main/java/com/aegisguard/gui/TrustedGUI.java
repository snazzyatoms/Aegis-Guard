package com.aegisguard.gui;

import com.aegisguard.AegisGuard;
import com.aegisguard.data.Plot;
import org.bukkit.Bukkit;
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

        Inventory inv = Bukkit.createInventory(null, 54, plugin.msg().get("trusted_menu_title"));

        // Trusted players list
        int slot = 0;
        for (UUID trustedId : plot.getTrusted()) {
            if (slot >= 45) break; // only top 5 rows for players

            OfflinePlayer trusted = Bukkit.getOfflinePlayer(trustedId);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();

            if (meta != null) {
                meta.setOwningPlayer(trusted);
                meta.setDisplayName("§a" + trusted.getName());

                List<String> lore = plugin.msg().formatList("trusted_menu_lore",
                        "PLAYER", trusted.getName());
                meta.setLore(lore);

                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                head.setItemMeta(meta);
            }

            inv.setItem(slot++, head);
        }

        // Add trusted button
        inv.setItem(45, GUIManager.icon(Material.EMERALD, plugin.msg().get("button_add_trusted")));

        // Remove trusted button
        inv.setItem(46, GUIManager.icon(Material.BARRIER, plugin.msg().get("button_remove_trusted")));

        // Back
        inv.setItem(52, GUIManager.icon(Material.ARROW, plugin.msg().get("button_back")));

        // Exit
        inv.setItem(53, GUIManager.icon(Material.BOOK, plugin.msg().get("button_exit")));

        owner.openInventory(inv);
    }
}
