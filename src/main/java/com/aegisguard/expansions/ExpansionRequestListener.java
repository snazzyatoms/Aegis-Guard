package com.aegisguard.expansions;

import com.aegisguard.AegisGuard;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * ExpansionRequestListener
 * ------------------------------------------------------
 * Handles all GUI interactions and events related to
 * Expansion Requests for AegisGuard.
 *
 * Features:
 *  - Integrated multilingual message system
 *  - Admin approval / denial with broadcast
 *  - GUI refresh and persistence support
 *  - Multi-world & Vault compatible
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

        // Match GUI title (using language tone)
        String guiTitle = ChatColor.stripColor(plugin.msg().get(player, "expansion_admin_title"));
        if (!title.equalsIgnoreCase(guiTitle)) return;

        e.setCancelled(true);
        var item = e.getCurrentItem();

        UUID requesterId = manager.getRequesterFromItem(item);
        if (requesterId == null) {
            plugin.msg().send(player, "expansion_invalid");
            plugin.sounds().playError(player);
            return;
        }

        Player target = Bukkit.getPlayer(requesterId);
        ExpansionRequest request = manager.getRequest(requesterId);

        if (request == null) {
            plugin.msg().send(player, "expansion_invalid");
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

        // Refresh GUI
        Bukkit.getScheduler().runTaskLater(plugin, () ->
                plugin.gui().expansionAdmin().open(player), 2L);
    }

    /* ------------------------------------------------------
     * Notifications
     * ------------------------------------------------------ */
    private void notifyPlayers(Player requester, Player admin, boolean approved) {
        if (requester != null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("PLAYER", admin.getName());

            plugin.msg().send(requester,
                    approved ? "expansion_approved" : "expansion_denied",
                    placeholders);
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("PLAYER", requester != null ? requester.getName() : "Unknown");

        plugin.msg().send(admin,
                approved ? "expansion_approved" : "expansion_denied",
                placeholders);

        if (plugin.getConfig().getBoolean("admin.broadcast_admin_actions", false)) {
            Map<String, String> broadcastPlaceholders = new HashMap<>();
            broadcastPlaceholders.put("PLAYER", admin.getName());

            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.msg().get(admin,
                            approved ? "expansion_broadcast_approved" : "expansion_broadcast_denied")
                            .replace("{PLAYER}", admin.getName())));
        }
    }

    /* ------------------------------------------------------
     * Logging
     * ------------------------------------------------------ */
    private void logAction(Player admin, Player requester, boolean approved) {
        String status = approved ? "APPROVED" : "DENIED";
        plugin.getLogger().info("[Expansion] " + admin.getName() + " " + status +
                " expansion request for " + requester.getName());

        // Persist all current expansion requests
        plugin.getExpansionRequestManager().saveAll();
    }
}
