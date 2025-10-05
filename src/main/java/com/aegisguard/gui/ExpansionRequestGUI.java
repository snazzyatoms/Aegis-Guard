package com.aegisguard.gui;

import com.aegisguard.AegisGuard;
import com.aegisguard.data.PlotStore;
import com.aegisguard.expansions.ExpansionRequestManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

/**
 * ExpansionRequestGUI
 * ------------------------------------------------------------
 * Player-facing interface for submitting expansion requests.
 * Fully supports tone system (old / hybrid / modern English).
 *
 * Flow:
 *  - Displays current & new radius + cost
 *  - Confirm to submit, Cancel to withdraw
 *  - Instant tone update — no reload needed
 */
public class ExpansionRequestGUI {

    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.##");

    private final AegisGuard plugin;
    private final ExpansionRequestManager manager;

    public ExpansionRequestGUI(AegisGuard plugin) {
        this.plugin = plugin;
        this.manager = plugin.getExpansionRequestManager();
    }

    /* -----------------------------
     * Open GUI
     * ----------------------------- */
    public void open(Player player, PlotStore.Plot plot) {
        if (plot == null) {
            plugin.msg().send(player, "no_plot_here");
            return;
        }

        String title = plugin.msg().has("expansion_request_title")
                ? plugin.msg().get(player, "expansion_request_title")
                : "📏 Expansion Request";

        Inventory inv = Bukkit.createInventory(null, 27, title);

        int currentRadius = plot.getRadius();
        int nextRadius = Math.min(currentRadius + 8,
                plugin.getConfig().getInt("claims.max_radius", 64));

        double cost = manager.calculateTempCost(
                player.getWorld().getName(), currentRadius, nextRadius);

        String currency = plugin.getConfig().getBoolean("use_vault", true)
                ? "$" + MONEY.format(cost)
                : MONEY.format(cost) + " items";

        // Info Item
        inv.setItem(11, createItem(Material.PAPER,
                plugin.msg().color("&b" + plugin.msg().get(player, "expansion_info_title")),
                List.of(
                        plugin.msg().color("&7World: &f" + player.getWorld().getName()),
                        plugin.msg().color("&7Current Radius: &e" + currentRadius),
                        plugin.msg().color("&7Requested Radius: &a" + nextRadius),
                        plugin.msg().color("&7Cost: &6" + currency),
                        plugin.msg().color("&8" + plugin.msg().get(player, "expansion_info_note"))
                )));

        // Confirm Button
        inv.setItem(13, createItem(Material.EMERALD_BLOCK,
                plugin.msg().color("&a" + plugin.msg().get(player, "expansion_confirm_button")),
                List.of(plugin.msg().color("&7" + plugin.msg().get(player, "expansion_confirm_lore")))));

        // Cancel Button
        inv.setItem(15, createItem(Material.REDSTONE_BLOCK,
                plugin.msg().color("&c" + plugin.msg().get(player, "expansion_cancel_button")),
                List.of(plugin.msg().color("&7" + plugin.msg().get(player, "expansion_cancel_lore")))));

        // Exit Button
        inv.setItem(26, createItem(Material.BARRIER,
                plugin.msg().get(player, "button_exit"),
                plugin.msg().getList(player, "exit_lore")));

        player.openInventory(inv);
        plugin.sounds().playMenuOpen(player);
    }

    /* -----------------------------
     * Handle Clicks
     * ----------------------------- */
    public void handleClick(Player player, InventoryClickEvent e) {
        e.setCancelled(true);
        if (e.getCurrentItem() == null) return;

        String title = e.getView().getTitle();
        if (!title.equalsIgnoreCase(plugin.msg().get(player, "expansion_request_title"))) return;

        Material type = e.getCurrentItem().getType();
        PlotStore.Plot plot = plugin.store().getPlotAt(player.getLocation());
        if (plot == null) {
            plugin.msg().send(player, "no_plot_here");
            player.closeInventory();
            return;
        }

        switch (type) {
            case EMERALD_BLOCK -> {
                int newRadius = Math.min(plot.getRadius() + 8,
                        plugin.getConfig().getInt("claims.max_radius", 64));

                if (manager.hasActiveRequest(player.getUniqueId())) {
                    plugin.msg().send(player, "expansion_exists");
                    plugin.sounds().playError(player);
                    return;
                }

                boolean success = manager.createRequest(player, plot, newRadius);
                if (success) {
                    plugin.msg().send(player, "expansion_submitted");
                    plugin.sounds().playMenuFlip(player);
                } else {
                    plugin.msg().send(player, "expansion_invalid");
                    plugin.sounds().playError(player);
                }
                player.closeInventory();
            }

            case REDSTONE_BLOCK -> {
                if (manager.hasActiveRequest(player.getUniqueId())) {
                    manager.clear(player.getUniqueId());
                    plugin.msg().send(player, "expansion_denied",
                            Map.of("PLAYER", player.getName()));
                } else {
                    plugin.msg().send(player, "expansion_invalid");
                }
                plugin.sounds().playMenuClose(player);
                player.closeInventory();
            }

            case BARRIER -> {
                player.closeInventory();
                plugin.sounds().playMenuClose(player);
            }
        }
    }

    /* -----------------------------
     * Utility
     * ----------------------------- */
    private ItemStack createItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }
}
