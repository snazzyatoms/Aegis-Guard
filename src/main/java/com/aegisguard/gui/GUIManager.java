package com.aegisguard.gui;

import com.aegisguard.AegisGuard;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class GUIManager {

    private final AegisGuard plugin;
    private final TrustedGUI trustedGUI;
    private final SettingsGUI settingsGUI;   // NEW

    public GUIManager(AegisGuard plugin) {
        this.plugin = plugin;
        this.trustedGUI = new TrustedGUI(plugin);
        this.settingsGUI = new SettingsGUI(plugin); // NEW
    }

    public TrustedGUI trusted() { return trustedGUI; }
    public SettingsGUI settings() { return settingsGUI; }

    /* -----------------------------
     * Open Main Menu
     * ----------------------------- */
    public void openMain(Player player) {
        String title = plugin.msg().get("menu_title");
        Inventory inv = Bukkit.createInventory(null, 27, title);

        inv.setItem(11, GUIManager.icon(
                Material.LIGHTNING_ROD,
                plugin.msg().get("button_claim_land"),
                plugin.msg().getList("claim_land_lore")
        ));

        inv.setItem(13, GUIManager.icon(
                Material.PLAYER_HEAD,
                plugin.msg().get("button_trusted_players"),
                plugin.msg().getList("trusted_players_lore")
        ));

        inv.setItem(15, GUIManager.icon(
                Material.REDSTONE_COMPARATOR,
                plugin.msg().get("button_settings"),
                plugin.msg().getList("settings_lore")
        ));

        inv.setItem(22, GUIManager.icon(
                Material.WRITABLE_BOOK,
                plugin.msg().get("button_info"),
                plugin.msg().getList("info_lore")
        ));

        inv.setItem(26, GUIManager.icon(
                Material.BARRIER,
                plugin.msg().get("button_exit"),
                plugin.msg().getList("exit_lore")
        ));

        player.openInventory(inv);
        plugin.sounds().playMenuOpen(player);
    }

    /* -----------------------------
     * Handle Main Menu Clicks
     * ----------------------------- */
    public void handleClick(Player player, InventoryClickEvent e) {
        e.setCancelled(true);
        if (e.getCurrentItem() == null) return;

        Material type = e.getCurrentItem().getType();
        String title = e.getView().getTitle();

        if (title.equals(plugin.msg().get("menu_title"))) {
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
                    settingsGUI.open(player); // DELEGATED TO NEW FILE
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
    }

    /* -----------------------------
     * Helper: Build Icon
     * ----------------------------- */
    public static ItemStack icon(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) meta.setLore(lore);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }
}
