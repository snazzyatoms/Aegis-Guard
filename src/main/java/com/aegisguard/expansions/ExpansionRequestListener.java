package com.aegisguard.expansions;

import com.aegisguard.AegisGuard;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.UUID;

/**
 * ExpansionRequestListener
 * ------------------------------------------------------
 * Handles all GUI interactions and events related to
 * Expansion Requests for AegisGuard.
 *
 * Features:
 *  - Persistent ExpansionRequestManager integration
 *  - Admin approval / denial with broadcast
 *  - Automatic GUI routing
 *  - Multi-world & Vault support
 *  - Console + optional global logging
 */
public class ExpansionRequestListener implements Listener {

    private final AegisGuard plugin;

    public ExpansionRequestListener(AegisGuard plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (e.getCurrentItem() == null) return;

        String title = ChatColor.stripColor(e.getView().getTitle());
        ExpansionRequestManager manager = plugin.getExpansionRequestManager();

        // Match theme title
        String guiTitle = ChatColor.stripColor(
                plugin.msg().has("expansion_admin_title")
                        ? plugin.msg().get("expansion_admin_title")
                        : "AegisGuard — Expansion Requests"
        );

        // --- Admin Expansion Menu Handling ---
        if (title.equalsIgnoreCase(guiTitle)) {
            e.setCancelled(true);
            var item = e.getCurrentItem();

            // Each expansion request corresponds to a player’s request item
            UUID requesterId = manager.getRequesterFromItem(item);
            if (requesterId == null) {
                player.sendMessage(ChatColor.RED + "⚠ Invalid request item!");
                plugin.sounds().playError(player);
                return;
            }

            Player target = Bukkit.getPlayer(requesterId);
            ExpansionRequest request = manager.getRequest(requesterId);

            if (request == null) {
                player.sendMessage(ChatColor.RED + "❌ Request no longer exists!");
                plugin.sounds().playError(player);
                return;
            }

            switch (item.getType()) {
                case EMERALD_BLOCK -> { // Approve
                    manager.approveRequest(request);
                    notifyPlayers(target, player, true);
                    logAction(player, target, true);
                    plugin.sounds().playConfirm(player);
                }

                case REDSTONE_BLOCK -> { // Deny
                    manager.denyRequest(request);
                    notifyPlayers(target, player, false);
                    logAction(player, target, false);
                    plugin.sounds().playError(player);
                }

                default -> plugin.sounds().playClick(player);
            }

            // Auto-refresh admin GUI
            Bukkit.getScheduler().runTaskLater(plugin, () ->
                    plugin.gui().expansionAdmin().open(player), 2L);
        }
    }

    /* ------------------------------------------------------
     * Player + Admin Notifications
     * ------------------------------------------------------ */
    private void notifyPlayers(Player requester, Player admin, boolean approved) {
        if (requester != null) {
            requester.sendMessage(approved
                    ? ChatColor.GREEN + "✔ Your expansion request was approved by " + admin.getName() + "!"
                    : ChatColor.RED + "❌ Your expansion request was denied by " + admin.getName() + ".");
        }
        admin.sendMessage(approved
                ? ChatColor.GREEN + "✔ Approved expansion for " + requester.getName() + "."
                : ChatColor.RED + "❌ Denied expansion for " + requester.getName() + ".");

        if (plugin.getConfig().getBoolean("admin.broadcast_admin_actions", false)) {
            Bukkit.broadcastMessage(approved
                    ? ChatColor.GOLD + "[AegisGuard] " + ChatColor.YELLOW + requester.getName() + "’s expansion request was approved by " + admin.getName() + "!"
                    : ChatColor.RED + "[AegisGuard] " + ChatColor.YELLOW + requester.getName() + "’s expansion request was denied by " + admin.getName() + ".");
        }
    }

    /* ------------------------------------------------------
     * Console + Persistence Logging
     * ------------------------------------------------------ */
    private void logAction(Player admin, Player requester, boolean approved) {
        String status = approved ? "APPROVED" : "DENIED";
        plugin.getLogger().info("[Expansion] " + admin.getName() + " " + status +
                " expansion request for " + requester.getName());

        // Persist result to disk
        plugin.getExpansionRequestManager().saveAll();
    }
}
