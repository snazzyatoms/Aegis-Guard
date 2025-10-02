package com.aegisguard.gui;

import com.aegisguard.AegisGuard;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * GUIManager
 * - Handles opening GUIs for players
 * - Main menu with claim tools & trusted players
 * - Lightweight design (no crazy routing)
 */
public class GUIManager {

    private final AegisGuard plugin;

    public GUIManager(AegisGuard plugin) {
        this.plugin = plugin;
    }

    /* -----------------------------
     * Open Main Menu
     * ----------------------------- */
    public void openMain(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, ChatColor.AQUA + "Aegis Menu");

        // Claim Tool
        inv.setItem(11, createItem(Material.LIGHTNING_ROD,
                ChatColor.GREEN + "Claim Land",
                List.of(ChatColor.GRAY + "Select and confirm a protected area")));

        // Trusted Players
        inv.setItem(13, createItem(Material.PLAYER_HEAD,
                ChatColor.YELLOW + "Trusted Players",
                List.of(ChatColor.GRAY + "Manage who can build in your claim")));

        // Settings / Config
        inv.setItem(15, createItem(Material.BOOK,
                ChatColor.BLUE + "Settings",
                List.of(ChatColor.GRAY + "Toggle protections, preferences, etc.")));

        // Exit
        inv.setItem(26, createItem(Material.BARRIER,
                ChatColor.RED + "Exit",
                List.of(ChatColor.GRAY + "Close this menu")));

        player.openInventory(inv);
    }

    /* -----------------------------
     * Handle Clicks
     * ----------------------------- */
    public void handleClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (e.getClickedInventory() == null) return;

        String title = e.getView().getTitle();
        if (!ChatColor.stripColor(title).equalsIgnoreCase("Aegis Menu")) return;

        e.setCancelled(true);

        if (e.getCurrentItem() == null) return;
        Material type = e.getCurrentItem().getType();

        switch (type) {
            case LIGHTNING_ROD -> player.sendMessage(ChatColor.GREEN + "⚡ Claim tool selected!");
            case PLAYER_HEAD -> player.sendMessage(ChatColor.YELLOW + "👥 Opening Trusted Players...");
            case BOOK -> player.sendMessage(ChatColor.BLUE + "⚙ Settings coming soon!");
            case BARRIER -> player.closeInventory();
        }
    }

    /* -----------------------------
     * Helper: Build Icon
     * ----------------------------- */
    private ItemStack createItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
