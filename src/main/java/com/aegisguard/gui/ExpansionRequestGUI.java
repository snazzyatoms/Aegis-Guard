package com.aegisguard.gui;

import com.aegisguard.AegisGuard;
import com.aegisguard.data.PlotStore;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * ExpansionRequestGUI (Community placeholder)
 * ------------------------------------------------------------
 * - Pretty, tone-aware UI for "Expansion Requests"
 * - No hard dependency on ExpansionRequestManager or plot radius
 * - Reads a simple toggle from config: expansions.enabled
 * - Safe to ship now; paid version can wire the backend later
 */
public class ExpansionRequestGUI {

    private final AegisGuard plugin;

    public ExpansionRequestGUI(AegisGuard plugin) {
        this.plugin = plugin;
    }

    /* -----------------------------
     * Title helper (fallback-safe)
     * ----------------------------- */
    private String title(Player player) {
        String t = plugin.msg().get(player, "expansion_request_title");
        if (t != null && !t.contains("[Missing")) return t;
        return "§b📏 Expansion Request";
    }

    /* -----------------------------
     * Simple background filler
     * ----------------------------- */
    private ItemStack filler() {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            pane.setItemMeta(meta);
        }
        return pane;
    }

    /* -----------------------------
     * Open GUI
     * ----------------------------- */
    public void open(Player player, PlotStore.Plot plot) {
        if (plot == null) {
            plugin.msg().send(player, "no_plot_here");
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 27, title(player));

        // Fill background for a polished look
        ItemStack bg = filler();
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, bg);

        boolean enabled = plugin.getConfig().getBoolean("expansions.enabled", false);

        // --- Info card (center-left) ---
        List<String> infoLore = new ArrayList<>();
        infoLore.add("§7World: §f" + plot.getWorld());
        infoLore.add("§7Bounds: §e(" + plot.getX1() + ", " + plot.getZ1() + ") §7→ §a(" + plot.getX2() + ", " + plot.getZ2() + ")");
        String note = plugin.msg().get(player, "expansion_info_note");
        if (note != null && !note.contains("[Missing")) {
            infoLore.add("§8" + note);
        } else {
            infoLore.add("§8Your request will be reviewed by an admin.");
        }

        inv.setItem(11, icon(
                Material.PAPER,
                safeText(plugin.msg().get(player, "expansion_info_title"), "§bExpansion Details"),
                infoLore
        ));

        // --- Confirm (center) ---
        inv.setItem(13, icon(
                Material.EMERALD_BLOCK,
                safeText(plugin.msg().get(player, "expansion_confirm_button"), "§aConfirm Expansion"),
                List.of(safeText(plugin.msg().get(player, "expansion_confirm_lore"), "§7Click to submit your expansion request."))
        ));

        // --- Cancel (center-right) ---
        inv.setItem(15, icon(
                Material.REDSTONE_BLOCK,
                safeText(plugin.msg().get(player, "expansion_cancel_button"), "§cCancel"),
                List.of(safeText(plugin.msg().get(player, "expansion_cancel_lore"), "§7Return without sending your request."))
        ));

        // --- Exit (bottom-center) ---
        inv.setItem(22, icon(
                Material.BARRIER,
                plugin.msg().get(player, "button_exit"),
                plugin.msg().getList(player, "exit_lore")
        ));

        // If expansions are disabled, show a subtle disabled tile in the bottom-left
        if (!enabled) {
            inv.setItem(18, icon(
                    Material.GRAY_DYE,
                    "§7Requests Disabled",
                    List.of("§8An admin has disabled expansion requests.")
            ));
        }

        player.openInventory(inv);
        plugin.sounds().playMenuOpen(player);
    }

    /* -----------------------------
     * Handle Clicks
     * ----------------------------- */
    public void handleClick(Player player, InventoryClickEvent e) {
        e.setCancelled(true);
        if (e.getCurrentItem() == null) return;

        // Ensure we're handling the right GUI by matching the localized title
        String expected = title(player);
        if (!expected.equals(e.getView().getTitle())) return;

        Material type = e.getCurrentItem().getType();

        // We only operate on the plot the player is standing in (for context)
        PlotStore.Plot plot = plugin.store().getPlotAt(player.getLocation());
        if (plot == null) {
            plugin.msg().send(player, "no_plot_here");
            player.closeInventory();
            plugin.sounds().playMenuClose(player);
            return;
        }

        switch (type) {
            case EMERALD_BLOCK -> {
                boolean enabled = plugin.getConfig().getBoolean("expansions.enabled", false);
                if (!enabled) {
                    // Feature disabled
                    String key = "expansion_invalid";
                    plugin.msg().send(player, key);
                    plugin.sounds().playMenuClose(player);
                    player.closeInventory();
                    return;
                }

                // Placeholder “submitted” flow (no backend yet)
                plugin.msg().send(player, "expansion_submitted");
                plugin.sounds().playMenuFlip(player);
                player.closeInventory();
            }
            case REDSTONE_BLOCK -> {
                // Soft-cancel path (no backend state to clear)
                // Use an existing key if you prefer; otherwise just close politely
                plugin.sounds().playMenuClose(player);
                player.closeInventory();
            }
            case BARRIER -> {
                player.closeInventory();
                plugin.sounds().playMenuClose(player);
            }
            default -> { /* ignore other clicks */ }
        }
    }

    /* -----------------------------
     * Helpers
     * ----------------------------- */
    private ItemStack icon(Material mat, String name, List<String> lore) {
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

    private String safeText(String fromMsg, String fallback) {
        if (fromMsg == null) return fallback;
        if (fromMsg.contains("[Missing")) return fallback;
        return fromMsg;
    }
}
