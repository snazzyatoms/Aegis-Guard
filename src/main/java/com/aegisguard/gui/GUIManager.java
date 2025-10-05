package com.aegisguard.gui;

import com.aegisguard.AegisGuard;
import com.aegisguard.expansions.ExpansionRequestAdminGUI;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class GUIManager {

    private final AegisGuard plugin;

    // Sub GUIs
    private final PlayerGUI playerGUI;
    private final TrustedGUI trustedGUI;
    private final SettingsGUI settingsGUI;
    private final AdminGUI adminGUI;
    private final ExpansionRequestAdminGUI expansionAdminGUI;

    public GUIManager(AegisGuard plugin) {
        this.plugin = plugin;
        this.playerGUI = new PlayerGUI(plugin);
        this.trustedGUI = new TrustedGUI(plugin);
        this.settingsGUI = new SettingsGUI(plugin);
        this.adminGUI = new AdminGUI(plugin);
        this.expansionAdminGUI = new ExpansionRequestAdminGUI(plugin);
    }

    // Accessors
    public PlayerGUI player() { return playerGUI; }
    public TrustedGUI trusted() { return trustedGUI; }
    public SettingsGUI settings() { return settingsGUI; }
    public AdminGUI admin() { return adminGUI; }
    public ExpansionRequestAdminGUI expansionAdmin() { return expansionAdminGUI; }

    /* -----------------------------
     * Open Main Menu (Player GUI wrapper)
     * ----------------------------- */
    public void openMain(Player player) {
        playerGUI.open(player);
    }

    /* -----------------------------
     * Handle Main Menu Clicks
     * ----------------------------- */
    public void handleClick(Player player, InventoryClickEvent e) {
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;

        final String viewTitle = normalize(e.getView().getTitle());

        // Resolve known titles once (with safe fallbacks)
        final String tPlayer   = normalize(msg("menu_title",            "AegisGuard — Menu"));
        final String tTrusted  = normalize(msg("trusted_menu_title",     "AegisGuard — Trusted"));
        final String tTrustedAdd = normalize(msg("add_trusted_title",    "AegisGuard — Add Trusted"));
        final String tTrustedRem = normalize(msg("remove_trusted_title", "AegisGuard — Remove Trusted"));
        final String tSettings = normalize(msg("settings_menu_title",    "AegisGuard — Settings"));
        final String tAdmin    = normalize(msg("admin_menu_title",       "AegisGuard — Admin"));
        final String tExpAdmin = normalize(msg("expansion_admin_title",  "AegisGuard — Expansion Requests"));

        boolean ours =
            viewTitle.equals(tPlayer) ||
            viewTitle.equals(tTrusted) ||
            viewTitle.equals(tTrustedAdd) ||
            viewTitle.equals(tTrustedRem) ||
            viewTitle.equals(tSettings) ||
            viewTitle.equals(tAdmin) ||
            viewTitle.equals(tExpAdmin);

        if (!ours) return; // don’t cancel clicks for non-Aegis inventories

        e.setCancelled(true);

        // Route by title
        if (viewTitle.equals(tPlayer)) {
            playerGUI.handleClick(player, e);
        } else if (viewTitle.equals(tTrusted) || viewTitle.equals(tTrustedAdd) || viewTitle.equals(tTrustedRem)) {
            trustedGUI.handleClick(player, e);
        } else if (viewTitle.equals(tSettings)) {
            settingsGUI.handleClick(player, e);
        } else if (viewTitle.equals(tAdmin)) {
            adminGUI.handleClick(player, e);
        } else if (viewTitle.equals(tExpAdmin)) {
            expansionAdminGUI.handleClick(player, e);
        }
    }

    /* -----------------------------
     * Helpers
     * ----------------------------- */
    private String msg(String key, String fallback) {
        // MessagesUtil.get(key) should return colored text; if missing, use fallback
        String s = plugin.msg().get(key);
        return (s == null || s.isEmpty()) ? fallback : s;
    }

    private static String normalize(String s) {
        if (s == null) return "";
        // Strip color codes and compare case-insensitively
        return ChatColor.stripColor(s).trim().toLowerCase();
    }

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
