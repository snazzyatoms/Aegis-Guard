package com.aegisguard.gui;

import com.aegisguard.AegisGuard;
import org.bukkit.Bukkit;
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
 * - Fully synced with messages.yml for customization
 * - Uses SoundManager for immersive effects
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
        plugin.sounds().playMenuOpen(player); // 📖 magical open
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
                plugin.sounds().playMenuFlip(player);
            }
            case PLAYER_HEAD -> {
                trustedGUI.open(player);
                plugin.sounds().playMenuFlip(player);
            }
            case REDSTONE_COMPARATOR -> {
                player.sendMessage(plugin.msg().get("settings_coming_soon"));
                plugin.sounds().playMenuFlip(player);
            }
            case WRITABLE_BOOK -> {
                player.sendMessage(plugin.msg().get("info_message"));
                plugin.sounds().playMenuFlip(player);
            }
            case BARRIER -> {
                player.closeInventory();
                plugin.sounds().playMenuClose(player);
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
