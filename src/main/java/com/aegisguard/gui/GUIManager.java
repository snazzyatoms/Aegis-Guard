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
 * - Guardian Codex main menu hub
 * - Access to claim tools, trusted players, and settings
 */
public class GUIManager {

    private final AegisGuard plugin;
    private final TrustedGUI trustedGUI;

    public GUIManager(AegisGuard plugin) {
        this.plugin = plugin;
        this.trustedGUI = new TrustedGUI(plugin);
    }

    /* -----------------------------
     * Open Main Menu
     * ----------------------------- */
    public void openMain(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, ChatColor.AQUA + "⚔ Guardian Codex ⚔");

        // Claim Land
        inv.setItem(11, createItem(Material.LIGHTNING_ROD,
                ChatColor.GREEN + "Claim Land",
                List.of(
                        ChatColor.GRAY + "Select and confirm a protected area.",
                        ChatColor.DARK_GRAY + "⚡ Use your Aegis Scepter to mark land."
                )));

        // Trusted Players
        inv.setItem(13, createItem(Material.PLAYER_HEAD,
                ChatColor.YELLOW + "Trusted Players",
                List.of(
                        ChatColor.GRAY + "Manage who can build in your claim.",
                        ChatColor.DARK_GRAY + "✔ Add or remove trusted players"
                )));

        // Settings
        inv.setItem(15, createItem(Material.REDSTONE_COMPARATOR,
                ChatColor.BLUE + "Settings",
                List.of(
                        ChatColor.GRAY + "Toggle protections & preferences.",
                        ChatColor.DARK_GRAY + "(PvP, containers, mob spawning)"
                )));

        // Info & Guidebook (slot 22)
        inv.setItem(22, createItem(Material.WRITABLE_BOOK,
                ChatColor.GOLD + "📖 Info & Guide",
                List.of(
                        ChatColor.GRAY + "This menu is your all-in-one land protection hub.",
                        ChatColor.GRAY + "Use it to claim land, manage trusted players,",
                        ChatColor.GRAY + "and configure protections."
                )));

        // Exit (slot 26)
        inv.setItem(26, createItem(Material.BARRIER,
                ChatColor.RED + "Exit",
                List.of(ChatColor.GRAY + "Close the Guardian Codex")));

        player.openInventory(inv);
    }

    /* -----------------------------
     * Handle Main Menu Clicks
     * ----------------------------- */
    public void handleClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (e.getClickedInventory() == null) return;

        String title = ChatColor.stripColor(e.getView().getTitle());
        if (!title.equalsIgnoreCase("⚔ Guardian Codex ⚔")) return;

        e.setCancelled(true);

        if (e.getCurrentItem() == null) return;
        Material type = e.getCurrentItem().getType();

        switch (type) {
            case LIGHTNING_ROD -> {
                player.closeInventory();
                plugin.selection().confirmClaim(player);
            }
            case PLAYER_HEAD -> trustedGUI.open(player);
            case REDSTONE_COMPARATOR -> player.sendMessage(ChatColor.BLUE + "⚙ Settings GUI coming soon!");
            case WRITABLE_BOOK -> player.sendMessage(ChatColor.GOLD + "📖 The Guardian Codex explains everything about land protection.");
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
