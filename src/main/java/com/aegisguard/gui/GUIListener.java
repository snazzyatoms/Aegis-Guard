package com.aegisguard.gui;

import com.aegisguard.AegisGuard;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class GUIListener implements Listener {

    private final AegisGuard plugin;

    public GUIListener(AegisGuard plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;

        // Only handle clicks in the TOP inventory (your GUI), not the player's own inventory
        Inventory top = player.getOpenInventory().getTopInventory();
        if (e.getClickedInventory() == null || !e.getClickedInventory().equals(top)) return;

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;

        String viewTitle = normalize(e.getView().getTitle());

        // Known titles (with safe fallbacks). If any match, hand off to GUIManager.
        String tPlayer     = normalize(msg("menu_title",            "AegisGuard — Menu"));
        String tTrusted    = normalize(msg("trusted_menu_title",     "AegisGuard — Trusted"));
        String tTrustedAdd = normalize(msg("add_trusted_title",      "AegisGuard — Add Trusted"));
        String tTrustedRem = normalize(msg("remove_trusted_title",   "AegisGuard — Remove Trusted"));
        String tSettings   = normalize(msg("settings_menu_title",    "AegisGuard — Settings"));
        String tAdmin      = normalize(msg("admin_menu_title",       "AegisGuard — Admin"));
        String tExpAdmin   = normalize(msg("expansion_admin_title",  "AegisGuard — Expansion Requests"));

        boolean ours =
                viewTitle.equals(tPlayer) ||
                viewTitle.equals(tTrusted) ||
                viewTitle.equals(tTrustedAdd) ||
                viewTitle.equals(tTrustedRem) ||
                viewTitle.equals(tSettings) ||
                viewTitle.equals(tAdmin) ||
                viewTitle.equals(tExpAdmin);

        if (!ours) return;

        // Centralized routing + cancellation lives in GUIManager
        plugin.gui().handleClick(player, e);
    }

    private String msg(String key, String fallback) {
        String s = plugin.msg().get(key);
        return (s == null || s.isEmpty()) ? fallback : s;
    }

    private static String normalize(String s) {
        if (s == null) return "";
        return ChatColor.stripColor(s).trim().toLowerCase();
    }
}
