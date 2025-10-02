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
 * - Player-based sound toggle via Settings menu
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
        plugin.sounds().playMenuOpen(player);
    }

    /* -----------------------------
     * Open Settings Menu
     * ----------------------------- */
    public void openSettings(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, plugin.msg().get("settings_menu_title"));

        // Toggle Sounds
        boolean soundsEnabled = plugin.isSoundEnabled(player);
        inv.setItem(11, createItem(
                soundsEnabled ? Material.NOTE_BLOCK : Material.BARRIER,
                soundsEnabled ? plugin.msg().get("button_sounds_on") : plugin.msg().get("button_sounds_off"),
                plugin.msg().getList("sounds_toggle_lore")
        ));

        // Back
        inv.setItem(22, createItem(
                Material.ARROW,
                plugin.msg().get("button_back"),
                plugin.msg().getList("back_lore")
        ));

        // Exit
        inv.setItem(26, createItem(
                Material.BARRIER,
                plugin.msg().get("button_exit"),
                plugin.msg().getList("exit_lore")
        ));

        player.openInventory(inv);
        plugin.sounds().playMenuFlip(player);
    }

    /* -----------------------------
     * Handle Menu Clicks
     * ----------------------------- */
    public void handleClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (e.getClickedInventory() == null) return;
        if (e.getCurrentItem() == null) return;

        String title = e.getView().getTitle();
        Material type = e.getCurrentItem().getType();

        // Main menu
        if (title.equals(plugin.msg().get("menu_title"))) {
            e.setCancelled(true);
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
                case REDSTONE_COMPARATOR -> openSettings(player);
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

        // Settings menu
        else if (title.equals(plugin.msg().get("settings_menu_title"))) {
            e.setCancelled(true);
            switch (type) {
                case NOTE_BLOCK, BARRIER -> {
                    // Toggle sounds for this player
                    boolean currentlyEnabled = plugin.isSoundEnabled(player);
                    plugin.getConfig().set("sounds.players." + player.getUniqueId(), !currentlyEnabled);
                    plugin.saveConfig();

                    plugin.msg().send(player, !currentlyEnabled ? "sound_enabled" : "sound_disabled");
                    openSettings(player); // refresh menu with new state
                }
                case ARROW -> openMain(player);
                case BARRIER -> {
                    player.closeInventory();
                    plugin.sounds().playMenuClose(player);
                }
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
