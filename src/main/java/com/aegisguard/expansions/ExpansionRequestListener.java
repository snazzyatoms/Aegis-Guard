package com.aegisguard.expansions;

import com.aegisguard.AegisGuard;
import com.aegisguard.data.PlotStore;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * ExpansionRequestListener
 * ------------------------------------------------------------
 * Listens for admin interactions with the expansion request GUI.
 * Admins can:
 *  - Approve pending requests
 *  - Deny and refund them
 *  - Inspect queued requests
 *
 * Persistent: Requests are loaded/saved via ExpansionRequestManager.
 * Works across restarts — all pending requests remain valid.
 */
public class ExpansionRequestListener implements Listener {

    private final AegisGuard plugin;
    private final ExpansionRequestManager manager;

    public ExpansionRequestListener(AegisGuard plugin) {
        this.plugin = plugin;
        this.manager = plugin.expansion();
    }

    @EventHandler
    public void onAdminClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player admin)) return;
        if (e.getCurrentItem() == null) return;
        String title = e.getView().getTitle();
        ItemStack item = e.getCurrentItem();

        // Only handle AegisGuard-admin GUI
        if (!title.equalsIgnoreCase("🛡 AegisGuard — Expansion Requests")) return;
        e.setCancelled(true);

        Material type = item.getType();
        switch (type) {
            case EMERALD_BLOCK -> handleApprove(admin, e);
            case REDSTONE_BLOCK -> handleDeny(admin, e);
            case PAPER -> handleInspect(admin, e);
            case BARRIER -> admin.closeInventory();
        }
    }

    private void handleApprove(Player admin, InventoryClickEvent e) {
        String targetName = e.getCurrentItem().getItemMeta().getDisplayName().replace("§e", "").trim();
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            admin.sendMessage("§c⚠ Player " + targetName + " is offline. Cannot approve.");
            plugin.sounds().playMenuClose(admin);
            return;
        }

        ExpansionRequest req = manager.get(target.getUniqueId());
        if (req == null) {
            admin.sendMessage("§c❌ No active expansion request found for " + targetName);
            plugin.sounds().playMenuClose(admin);
            return;
        }

        if (manager.approve(req)) {
            admin.sendMessage("§a✔ Approved expansion for §e" + targetName);
            target.sendMessage("§a⚡ Your claim expansion has been approved!");
            plugin.sounds().playMenuFlip(admin);
        } else {
            admin.sendMessage("§c❌ Failed to process approval.");
            plugin.sounds().playMenuClose(admin);
        }
    }

    private void handleDeny(Player admin, InventoryClickEvent e) {
        String targetName = e.getCurrentItem().getItemMeta().getDisplayName().replace("§e", "").trim();
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            admin.sendMessage("§c⚠ Player " + targetName + " is offline. Cannot deny.");
            plugin.sounds().playMenuClose(admin);
            return;
        }

        ExpansionRequest req = manager.get(target.getUniqueId());
        if (req == null) {
            admin.sendMessage("§c❌ No active expansion request found for " + targetName);
            plugin.sounds().playMenuClose(admin);
            return;
        }

        manager.deny(req);
        admin.sendMessage("§e⚠ Denied expansion for §e" + targetName);
        target.sendMessage("§c❌ Your expansion request was denied by an admin.");
        plugin.sounds().playMenuClose(admin);
    }

    private void handleInspect(Player admin, InventoryClickEvent e) {
        String targetName = e.getCurrentItem().getItemMeta().getDisplayName().replace("§e", "").trim();
        Player target = Bukkit.getPlayerExact(targetName);
        ExpansionRequest req = manager.get(target.getUniqueId());
        if (req == null) {
            admin.sendMessage("§cNo data found for " + targetName);
            return;
        }

        PlotStore.Plot plot = req.getPlot();
        admin.sendMessage("§7───────────────");
        admin.sendMessage("§b📜 Expansion Request — " + targetName);
        admin.sendMessage("§7World: §f" + req.getWorld());
        admin.sendMessage("§7Current Radius: §e" + plot.getRadius());
        admin.sendMessage("§7Requested Radius: §a" + req.getNewRadius());
        admin.sendMessage("§7Cost: §6" + req.getCost());
        admin.sendMessage("§7Status: §f" + req.getStatus());
        admin.sendMessage("§7───────────────");
        plugin.sounds().playMenuOpen(admin);
    }
}
