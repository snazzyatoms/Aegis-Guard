package com.aegisguard.expansions;

import com.aegisguard.AegisGuard;
import com.aegisguard.data.PlotStore;
import com.aegisguard.economy.VaultHook;
import com.aegisguard.world.WorldRulesManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * ExpansionRequestManager
 * ------------------------------------------------------------
 * Handles creation, approval, denial, and tracking of plot
 * expansion requests.
 *
 * Integrations:
 *  - Vault economy (via VaultHook)
 *  - Item-based economy (config-driven)
 *  - Per-world restrictions (via WorldRulesManager)
 *  - Multilingual tone support via MessagesUtil
 *
 * Features:
 *  - Player & admin notifications
 *  - Placeholder formatting
 *  - Dynamic cost scaling per world
 */
public class ExpansionRequestManager {

    private final AegisGuard plugin;
    private final Map<UUID, ExpansionRequest> activeRequests = new HashMap<>();

    public ExpansionRequestManager(AegisGuard plugin) {
        this.plugin = plugin;
    }

    /* -----------------------------
     * Create Request
     * ----------------------------- */
    public boolean createRequest(Player requester, PlotStore.Plot plot, int newRadius) {
        if (plot == null) {
            plugin.msg().send(requester, "no_plot_here");
            return false;
        }

        // Only owners may expand
        if (!plot.getOwner().equals(requester.getUniqueId())) {
            plugin.msg().send(requester, "no_perm");
            return false;
        }

        // Check if this player already has a pending expansion
        if (hasActiveRequest(requester.getUniqueId())) {
            plugin.msg().send(requester, "expansion_exists");
            return false;
        }

        // Check per-world rules
        WorldRulesManager rules = plugin.worldRules();
        if (!rules.allowClaims(requester.getWorld())) {
            requester.sendMessage("§c❌ Claims and expansions are disabled in this world.");
            return false;
        }

        int currentRadius = plot.getRadius();
        if (newRadius <= currentRadius) {
            requester.sendMessage("§e⚠ Requested radius must exceed the current radius (" + currentRadius + ").");
            return false;
        }

        // Calculate cost dynamically
        double cost = calculateCost(requester.getWorld().getName(), currentRadius, newRadius);

        // Create request record
        ExpansionRequest request = new ExpansionRequest(
                requester.getUniqueId(),
                plot.getOwner(),
                requester.getWorld().getName(),
                currentRadius,
                newRadius,
                cost
        );

        activeRequests.put(requester.getUniqueId(), request);

        Map<String, String> placeholders = Map.of(
                "PLAYER", requester.getName(),
                "AMOUNT", String.format("%.2f", cost)
        );

        plugin.msg().send(requester, "expansion_submitted", placeholders);
        plugin.getLogger().info("[AegisGuard] Expansion request submitted by " + requester.getName() +
                " → Radius: " + currentRadius + " → " + newRadius);
        return true;
    }

    /* -----------------------------
     * Approve Request
     * ----------------------------- */
    public boolean approveRequest(ExpansionRequest req) {
        if (req == null) return false;

        OfflinePlayer requester = Bukkit.getOfflinePlayer(req.getRequester());
        if (!requester.isOnline()) return false;

        Player p = requester.getPlayer();
        if (p == null) return false;

        // Charge cost
        if (!chargePlayer(p, req.getCost(), req.getWorldName())) {
            plugin.msg().send(p, "need_vault", Map.of("AMOUNT", String.format("%.2f", req.getCost())));
            return false;
        }

        // Apply expansion
        PlotStore.Plot plot = plugin.store().getPlotAt(p.getLocation());
        if (plot == null) {
            plugin.msg().send(p, "expansion_invalid");
            return false;
        }

        plot.setRadius(req.getRequestedRadius());
        plugin.store().savePlot(plot);
        req.approve();

        plugin.msg().send(p, "expansion_approved", Map.of("PLAYER", "Admin"));
        plugin.getLogger().info("[AegisGuard] Expansion approved for " + p.getName() +
                " (" + req.getCurrentRadius() + " → " + req.getRequestedRadius() + ")");
        activeRequests.remove(p.getUniqueId());
        return true;
    }

    /* -----------------------------
     * Deny Request
     * ----------------------------- */
    public boolean denyRequest(ExpansionRequest req) {
        if (req == null) return false;

        req.deny();
        Player target = Bukkit.getPlayer(req.getRequester());
        if (target != null) {
            plugin.msg().send(target, "expansion_denied", Map.of("PLAYER", "Admin"));
        }

        plugin.getLogger().info("[AegisGuard] Expansion request denied for " + req.getRequester());
        activeRequests.remove(req.getRequester());
        return true;
    }

    /* -----------------------------
     * Cost Logic
     * ----------------------------- */
    private double calculateCost(String world, int currentRadius, int newRadius) {
        double baseCost = plugin.getConfig().getDouble("claim_cost", 100.0);
        double worldModifier = plugin.getConfig().getDouble("claims.per_world." + world + ".vault_cost", baseCost);
        int delta = newRadius - currentRadius;

        return Math.max(worldModifier * (delta / 4.0), 0);
    }

    private boolean chargePlayer(Player player, double amount, String worldName) {
        if (plugin.getConfig().getBoolean("use_vault", true)) {
            VaultHook vault = plugin.vault();
            if (!vault.has(player, amount)) return false;
            vault.withdraw(player, amount);
            return true;
        } else {
            // Item-based payment
            String path = "claims.per_world." + worldName + ".item_cost.";
            Material item = Material.matchMaterial(plugin.getConfig().getString(path + "type", "DIAMOND"));
            int amountRequired = plugin.getConfig().getInt(path + "amount", 5);

            ItemStack costItem = new ItemStack(item, amountRequired);
            if (!player.getInventory().containsAtLeast(costItem, amountRequired)) return false;

            player.getInventory().removeItem(costItem);
            return true;
        }
    }

    /* -----------------------------
     * Utilities
     * ----------------------------- */
    public ExpansionRequest getRequest(UUID requesterId) {
        return activeRequests.get(requesterId);
    }

    public UUID getRequesterFromItem(org.bukkit.inventory.ItemStack item) {
        // Placeholder for GUI integration
        // Will be replaced when ExpansionRequestGUI is implemented
        return null;
    }

    public boolean hasActiveRequest(UUID requesterId) {
        return activeRequests.containsKey(requesterId);
    }

    public void clear(UUID requesterId) {
        activeRequests.remove(requesterId);
    }

    public void clearAll() {
        activeRequests.clear();
    }

    public Collection<ExpansionRequest> getActiveRequests() {
        return activeRequests.values();
    }

    /* -----------------------------
     * Persistence (Placeholder)
     * ----------------------------- */
    public void saveAll() {
        // TODO: Add YAML persistence support for expansion requests
    }
}
