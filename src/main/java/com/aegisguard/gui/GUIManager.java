package com.aegisguard.gui;

import com.aegisguard.AegisGuard;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
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
 * - Fully synced with messages.yml for customization
 * - Immersive sounds for book-like experience
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
        String title = plugin.msg().get("menu_title");
        Inventory inv = Bukkit.createInventory(null, 27, title);

        // Claim Land
        inv.setItem(11, createItem(
                Material.LIGHTNING_ROD,
                plugin.msg().get("button_claim_land"),
                plugin.msg().getList("claim_land_lore")
        ));

        // Trusted Players
        inv.setItem(13, createItem(
                Material.PLAYER_HEAD,
                plugin.msg().get("button_trusted_players"),
                plugin.msg().getList("trusted_players_lore")
        ));

        // Settings
        inv.setItem(15, createItem(
                Material.REDSTONE_COMPARATOR,
                plugin.msg().get("button_settings"),
                plugin.msg().getList("settings_lore")
        ));

        // Info & Guidebook
        inv.setItem(22, createItem(
                Material.WRITABLE_BOOK,
                plugin.msg().get("button_info"),
                plugin.msg().getList("info_lore")
        ));

        // Exit
        inv.setItem(26, createItem(
                Material.BARRIER,
                plugin.msg().get("button_exit"),
                plugin.msg().getList("exit_lore")
        ));

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f); // 📖 open menu
    }

    /* -----------------------------
     * Handle Main Menu Clicks
     * ----------------------------- */
    public void handleClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (e.getClickedInventory() == null) return;

        String title = e.getView().getTitle();
        if (!title.equals(plugin.msg().get("menu_title"))) return;

        e.setCancelled(true);

        if (e.getCurrentItem() == null) return;
        Material type = e.getCurrentItem().getType();

        switch (type) {
            case LIGHTNING_ROD -> {
                player.closeInventory();
                plugin.selection().confirmClaim(player);
                player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1.1f);
            }
            case PLAYER_HEAD -> {
                trustedGUI.open(player);
                player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
            }
            case REDSTONE_COMPARATOR -> {
                player.sendMessage(plugin.msg().get("settings_coming_soon"));
                player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1.2f);
            }
            case WRITABLE_BOOK -> {
                player.sendMessage(plugin.msg().get("info_message"));
                player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1.3f);
            }
            case BARRIER -> {
                player.closeInventory();
                player.playSound(player.getLocation(), Sound.ITEM_BOOK_PUT, 1f, 0.8f); // closing book
            }
        }
    }

    /* -----------------------------
     * Helper: Build Icon
     * ----------------------------- */
    public static ItemStack icon(Material mat, String name) {
        return icon(mat, name, null);
    }

    public static ItemStack icon(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createItem(Material mat, String name, List<String> lore) {
        return icon(mat, name, lore);
    }
}
