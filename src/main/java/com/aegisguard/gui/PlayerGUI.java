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

public class PlayerGUI {

    private final AegisGuard plugin;

    public PlayerGUI(AegisGuard plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        String title = m(player, "menu_title", "§b§lAegisGuard §7— Menu");
        Inventory inv = Bukkit.createInventory(null, 27, title);

        // Claim Land
        inv.setItem(11, createItem(
                Material.LIGHTNING_ROD,
                m(player, "button_claim_land", "§aClaim Land"),
                l(player, "claim_land_lore", List.of("§7Select a region and confirm."))
        ));

        // Trusted Players
        inv.setItem(13, createItem(
                Material.PLAYER_HEAD,
                m(player, "button_trusted_players", "§bTrusted Players"),
                l(player, "trusted_players_lore", List.of("§7Manage who can build on your land."))
        ));

        // Settings
        inv.setItem(15, createItem(
                Material.COMPARATOR, // 1.20+ correct enum (fixes REDSTONE_COMPARATOR issue)
                m(player, "button_settings", "§eSettings"),
                l(player, "settings_lore", List.of("§7Toggle options for your claims."))
        ));

        // Info
        inv.setItem(22, createItem(
                Material.WRITABLE_BOOK,
                m(player, "button_info", "§fInfo"),
                l(player, "info_lore", List.of("§7Learn about AegisGuard features."))
        ));

        // Exit
        inv.setItem(26, createItem(
                Material.BARRIER,
                m(player, "button_exit", "§cExit"),
                plugin.msg().getList(player, "exit_lore")
        ));

        player.openInventory(inv);
        playOpen(player);
    }

    public void handleClick(Player player, InventoryClickEvent e) {
        e.setCancelled(true); // prevent item pickup/move
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;

        switch (clicked.getType()) {
            case LIGHTNING_ROD -> {
                player.closeInventory();
                plugin.selection().confirmClaim(player);
                playFlip(player);
            }
            case PLAYER_HEAD -> {
                plugin.gui().trusted().open(player);
                playFlip(player);
            }
            case COMPARATOR -> {
                plugin.gui().settings().open(player);
                playFlip(player);
            }
            case WRITABLE_BOOK -> {
                player.sendMessage(m(player, "info_message",
                        "§7AegisGuard: lightweight land protection with roles, safe zones, and GUIs."));
                playFlip(player);
            }
            case BARRIER -> {
                player.closeInventory();
                playClose(player);
            }
            default -> { /* ignore other slots */ }
        }
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
     * Message helpers (per-player tone + fallbacks)
     * ----------------------------- */
    private String m(Player player, String key, String fallback) {
        String v = plugin.msg().get(player, key);
        return (v == null || v.isEmpty()) ? fallback : v;
    }

    private List<String> l(Player player, String key, List<String> fallback) {
        List<String> v = plugin.msg().getList(player, key);
        return (v == null || v.isEmpty()) ? fallback : v;
    }

    /* -----------------------------
     * Sound helpers (no external manager required)
     * ----------------------------- */
    private void playOpen(Player p) { if (plugin.isSoundEnabled(p)) p.playSound(p.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.7f, 1.0f); }
    private void playClose(Player p){ if (plugin.isSoundEnabled(p)) p.playSound(p.getLocation(), Sound.BLOCK_CHEST_CLOSE, 0.7f, 1.0f); }
    private void playFlip(Player p) { if (plugin.isSoundEnabled(p)) p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f); }
}
