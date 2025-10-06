package com.aegisguard.gui;

import com.aegisguard.AegisGuard;
import com.aegisguard.expansions.ExpansionRequestAdminGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * Central router for all GUIs (now includes Expansion Admin placeholder).
 */
public class GUIManager {

    private final AegisGuard plugin;

    // Sub GUIs
    private final PlayerGUI playerGUI;
    private final TrustedGUI trustedGUI;
    private final SettingsGUI settingsGUI;
    private final AdminGUI adminGUI;
    private final ExpansionRequestAdminGUI expansionAdminGUI; // placeholder "coming soon"

    public GUIManager(AegisGuard plugin) {
        this.plugin = plugin;
        this.playerGUI = new PlayerGUI(plugin);
        this.trustedGUI = new TrustedGUI(plugin);
        this.settingsGUI = new SettingsGUI(plugin);
        this.adminGUI = new AdminGUI(plugin);
        this.expansionAdminGUI = new ExpansionRequestAdminGUI(plugin); // compiles without backend
    }

    // Accessors
    public PlayerGUI player() { return playerGUI; }
    public TrustedGUI trusted() { return trustedGUI; }
    public SettingsGUI settings() { return settingsGUI; }
    public AdminGUI admin() { return adminGUI; }
    public ExpansionRequestAdminGUI expansionAdmin() { return expansionAdminGUI; }

    /* -----------------------------
     * Open Main Menu (Player GUI)
     * ----------------------------- */
    public void openMain(Player player) { playerGUI.open(player); }

    /* -----------------------------
     * Route clicks by inventory title
     * ----------------------------- */
    public void handleClick(Player player, InventoryClickEvent e) {
        e.setCancelled(true);
        if (e.getCurrentItem() == null) return;

        String title = e.getView().getTitle();

        if (title.equals(plugin.msg().get("menu_title"))) {
            playerGUI.handleClick(player, e);
        }
        else if (title.equals(plugin.msg().get("trusted_menu_title"))
              || title.equals(plugin.msg().get("add_trusted_title"))
              || title.equals(plugin.msg().get("remove_trusted_title"))) {
            trustedGUI.handleClick(player, e);
        }
        else if (title.equals(plugin.msg().get("settings_menu_title"))) {
            settingsGUI.handleClick(player, e);
        }
        else if (title.equals(plugin.msg().get("admin_menu_title"))) {
            adminGUI.handleClick(player, e);
        }
        else {
            // Expansion Admin title (fallback to default if key missing)
            String expTitle = plugin.msg().has("expansion_admin_title")
                    ? plugin.msg().get("expansion_admin_title")
                    : "§b🛡 AegisGuard — Expansion Admin";
            if (title.equals(expTitle)) {
                expansionAdminGUI.handleClick(player, e);
            }
        }
    }

    /* -----------------------------
     * Helper: Build Icon
     * ----------------------------- */
    public static ItemStack icon(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }
}
