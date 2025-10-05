package com.aegisguard.gui;

import com.aegisguard.AegisGuard;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class AdminGUI {

    private final AegisGuard plugin;

    public AdminGUI(AegisGuard plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        if (!player.hasPermission("aegis.admin")) { // standardized perm
            player.sendMessage(m("no_perm", "§cYou don't have permission for that."));
            return;
        }

        String title = m("admin_menu_title", "§b§lAegisGuard §7— Admin");
        Inventory inv = Bukkit.createInventory(null, 27, title);

        inv.setItem(10, createItem(
                Material.TNT,
                m("button_admin_auto_remove", "§cAuto-remove Banned"),
                l("admin_auto_remove_lore", List.of("§7Toggle auto removal of claims by banned players."))));

        inv.setItem(12, createItem(
                Material.NETHER_STAR,
                m("button_admin_bypass_limit", "§eBypass Claim Limit"),
                l("admin_bypass_limit_lore", List.of("§7Allow admins to bypass player claim limits."))));

        inv.setItem(14, createItem(
                Material.BEACON,
                m("button_admin_broadcast", "§bBroadcast Admin Actions"),
                l("admin_broadcast_lore", List.of("§7Announce admin actions server-wide."))));

        inv.setItem(22, createItem(
                Material.BARRIER,
                m("button_exit", "§cExit"),
                l("exit_lore", List.of("§7Close this menu."))));

        player.openInventory(inv);
        playOpen(player);
    }

    public void handleClick(Player player, InventoryClickEvent e) {
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;

        switch (clicked.getType()) {
            case TNT -> {
                boolean autoRemove = plugin.getConfig().getBoolean("admin.auto_remove_banned", false);
                plugin.getConfig().set("admin.auto_remove_banned", !autoRemove);
                plugin.saveConfig();
                player.sendMessage(m(!autoRemove ? "admin_auto_remove_enabled" : "admin_auto_remove_disabled",
                        !autoRemove ? "§aAuto-remove enabled." : "§eAuto-remove disabled."));
                playFlip(player);
            }
            case NETHER_STAR -> {
                boolean bypass = plugin.getConfig().getBoolean("admin.bypass_claim_limit", false);
                plugin.getConfig().set("admin.bypass_claim_limit", !bypass);
                plugin.saveConfig();
                player.sendMessage(m(!bypass ? "admin_bypass_enabled" : "admin_bypass_disabled",
                        !bypass ? "§aBypass enabled." : "§eBypass disabled."));
                playFlip(player);
            }
            case BEACON -> {
                boolean broadcast = plugin.getConfig().getBoolean("admin.broadcast_admin_actions", false);
                plugin.getConfig().set("admin.broadcast_admin_actions", !broadcast);
                plugin.saveConfig();
                player.sendMessage(m(!broadcast ? "admin_broadcast_enabled" : "admin_broadcast_disabled",
                        !broadcast ? "§aBroadcasts enabled." : "§eBroadcasts disabled."));
                playFlip(player);
            }
            case BARRIER -> {
                player.closeInventory();
                playClose(player);
                return; // don't refresh if we just closed
            }
            default -> { /* ignore */ }
        }
        open(player); // refresh state after toggle
    }

    private ItemStack createItem(Material mat, String name, List<String> lore) {
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

    /* -----------------------------
     * Message helpers
     * ----------------------------- */
    private String m(String key, String fallback) {
        String v = plugin.msg().get(key);
        return (v == null || v.isEmpty()) ? fallback : v;
    }
    private List<String> l(String key, List<String> fallback) {
        List<String> v = plugin.msg().getList(key);
        return (v == null || v.isEmpty()) ? fallback : v;
    }

    /* -----------------------------
     * Sound helpers (no external manager)
     * ----------------------------- */
    private void playOpen(Player p) { if (plugin.isSoundEnabled(p)) p.playSound(p.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.7f, 1.0f); }
    private void playClose(Player p){ if (plugin.isSoundEnabled(p)) p.playSound(p.getLocation(), Sound.BLOCK_CHEST_CLOSE, 0.7f, 1.0f); }
    private void playFlip(Player p) { if (plugin.isSoundEnabled(p)) p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f); }
}
