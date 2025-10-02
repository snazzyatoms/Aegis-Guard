package com.aegisguard.gui;

import com.aegisguard.AegisGuard;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * TrustedGUI
 * - Shows trusted players with heads
 * - Allows add/remove management
 */
public class TrustedGUI {

    private final AegisGuard plugin;

    public TrustedGUI(AegisGuard plugin) {
        this.plugin = plugin;
    }

    /* -----------------------------
     * Open Trusted Players Menu
     * ----------------------------- */
    public void open(Player player) {
        List<UUID> trusted = plugin.store().getTrusted(player.getUniqueId());
        Inventory inv = Bukkit.createInventory(null, 54, ChatColor.YELLOW + "Trusted Players");

        int i = 0;
        for (UUID uuid : trusted) {
            if (i >= 45) break; // max 45 heads per page
            OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
            inv.setItem(i, createHead(offline));
            i++;
        }

        // Add trusted
        inv.setItem(48, createButton(Material.EMERALD, ChatColor.GREEN + "Add Trusted",
                List.of(ChatColor.GRAY + "Right-click a player with scepter to trust")));

        // Remove trusted
        inv.setItem(49, createButton(Material.REDSTONE, ChatColor.RED + "Remove Trusted",
                List.of(ChatColor.GRAY + "Click a head to remove trust")));

        // Back button
        inv.setItem(53, createButton(Material.ARROW, ChatColor.RED + "Back",
                List.of(ChatColor.GRAY + "Return to main menu")));

        player.openInventory(inv);
    }

    /* -----------------------------
     * Handle Trusted Menu Clicks
     * ----------------------------- */
    public void handleClick(Player player, InventoryClickEvent e) {
        String title = ChatColor.stripColor(e.getView().getTitle());
        if (!title.equalsIgnoreCase("Trusted Players")) return;

        e.setCancelled(true);

        if (e.getCurrentItem() == null) return;

        switch (e.getCurrentItem().getType()) {
            case PLAYER_HEAD -> {
                SkullMeta meta = (SkullMeta) e.getCurrentItem().getItemMeta();
                if (meta != null && meta.getOwningPlayer() != null) {
                    UUID target = meta.getOwningPlayer().getUniqueId();
                    plugin.store().removeTrusted(player.getUniqueId(), target);
                    player.sendMessage(ChatColor.RED + "❌ Removed trust: " + meta.getOwningPlayer().getName());
                    open(player);
                }
            }
            case EMERALD -> player.sendMessage(ChatColor.GREEN + "✔ Use your Aegis Scepter to trust players!");
            case REDSTONE -> player.sendMessage(ChatColor.RED + "❌ Click a player head to remove trust.");
            case ARROW -> plugin.gui().openMain(player);
        }
    }

    /* -----------------------------
     * Helpers
     * ----------------------------- */
    private ItemStack createHead(OfflinePlayer offline) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(offline);
            meta.setDisplayName(ChatColor.YELLOW + offline.getName());
            meta.setLore(List.of(ChatColor.GRAY + "Trusted Member"));
            head.setItemMeta(meta);
        }
        return head;
    }

    private ItemStack createButton(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
