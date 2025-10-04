package com.aegisguard.gui;

import com.aegisguard.AegisGuard;
import com.aegisguard.data.PlotStore;
import com.aegisguard.expansions.ExpansionRequest;
import com.aegisguard.expansions.ExpansionRequestManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * ExpansionRequestGUI
 * ------------------------------------------------------------
 * Interactive menu for creating, confirming, or denying
 * expansion requests.
 *
 * Stages:
 *  - Owner requests new size
 *  - GUI previews cost & new radius
 *  - Owner clicks "Confirm" or "Cancel"
 *
 * Integrations:
 *  - Vault / Item economy via ExpansionRequestManager
 *  - WorldRulesManager (auto-disabled where not allowed)
 */
public class ExpansionRequestGUI {

    private final AegisGuard plugin;
    private final ExpansionRequestManager manager;

    public ExpansionRequestGUI(AegisGuard plugin) {
        this.plugin = plugin;
        this.manager = plugin.expansion();
    }

    /* -----------------------------
     * Open GUI
     * ----------------------------- */
    public void open(Player player, PlotStore.Plot plot) {
        if (plot == null) {
            plugin.msg().send(player, "no_plot_here");
            return;
        }

        // Create inventory
        Inventory inv = Bukkit.createInventory(null, 27, "📏 Expansion Request");

        // Base info
        int currentRadius = plot.getRadius();
        int nextRadius = Math.min(currentRadius + 8, plugin.getConfig().getInt("claims.max_radius", 64));
        double cost = manager.createTempCost(player.getWorld().getName(), currentRadius, nextRadius);

        // Info item
        inv.setItem(11, createItem(Material.PAPER,
                "§bPlot Expansion Details",
                List.of(
                        "§7World: §f" + player.getWorld().getName(),
                        "§7Current Radius: §e" + currentRadius,
                        "§7New Radius: §a" + nextRadius,
                        "§7Cost: §6" + (plugin.getConfig().getBoolean("use_vault", true)
                                ? "$" + cost
                                : cost + " items"),
                        "§8Configure scaling in config.yml"
                )));

        // Confirm button
        inv.setItem(13, createItem(Material.EMERALD_BLOCK,
                "§aConfirm Expansion",
                List.of("§7Click to confirm request", "§7and pay cost upon approval")));

        // Cancel button
        inv.setItem(15, createItem(Material.REDSTONE_BLOCK,
                "§cCancel Request",
                List.of("§7Cancel and return to menu")));

        // Exit button
        inv.setItem(26, createItem(Material.BARRIER,
                "§cExit",
                List.of("§7Close this menu")));

        player.openInventory(inv);
        plugin.sounds().playMenuOpen(player);
    }

    /* -----------------------------
     * Handle Clicks
     * ----------------------------- */
    public void handleClick(Player player, InventoryClickEvent e) {
        e.setCancelled(true);
        if (e.getCurrentItem() == null) return;

        Material type = e.getCurrentItem().getType();
        String title = e.getView().getTitle();
        if (!title.equals("📏 Expansion Request")) return;

        PlotStore.Plot plot = plugin.store().getPlotAt(player.getLocation());
        if (plot == null) {
            plugin.msg().send(player, "no_plot_here");
            player.closeInventory();
            return;
        }

        switch (type) {
            case EMERALD_BLOCK -> {
                int newRadius = Math.min(plot.getRadius() + 8, plugin.getConfig().getInt("claims.max_radius", 64));
                if (manager.hasActiveRequest(player.getUniqueId())) {
                    player.sendMessage("§e⚠ You already have an active request.");
                    return;
                }

                if (manager.createRequest(player, plot, newRadius)) {
                    player.sendMessage("§a✔ Expansion request submitted!");
                    player.closeInventory();
                    plugin.sounds().playMenuFlip(player);
                } else {
                    plugin.sounds().playMenuClose(player);
                }
            }

            case REDSTONE_BLOCK -> {
                if (manager.hasActiveRequest(player.getUniqueId())) {
                    manager.clear(player.getUniqueId());
                    player.sendMessage("§c❌ Expansion request cancelled.");
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
