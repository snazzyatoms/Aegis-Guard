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
 *
 * TODO:
 *  - Add persistence (YAML or JSON)
 *  - Add GUI integration (ExpansionRequestGUI)
 *  - Add admin override and cooldowns
 */
public class ExpansionRequestManager {

    private final AegisGuard plugin;
    private final Map<UUID, ExpansionRequest> activeRequests = new HashMap<>();

    public ExpansionRequestManager(AegisGuard plugin) {
        this.plugin = plugin;
    }

    /* -----------------------------
     * Core Logic
     * ----------------------------- */

    /**
     * Create a new expansion request for a player's plot.
     * Handles cost validation and world checks.
     */
    public boolean createRequest(Player requester, PlotStore.Plot plot, int newRadius) {
        if (plot == null) {
            plugin.msg().send(requester, "no_plot_here");
            return false;
        }

        // Prevent self-request if not owner
        if (!plot.getOwner().equals(requester.getUniqueId())) {
            plugin.msg().send(requester, "no_perm");
            return false;
        }

        // Check per-world restrictions
        WorldRulesManager rules = plugin.worldRules();
        if (!rules.allowClaims(requester.getWorld())) {
            requester.sendMessage("§c❌ Claims and expansions are disabled in this world.");
            return false;
        }

        int currentRadius = plot.getRadius();
        if (newRadius <= currentRadius) {
            requester.sendMessage("§e⚠ Requested radius must be larger than current (" + currentRadius + ").");
            return false;
        }

        // Calculate cost (Vault or item)
        double cost = calculateCost(requester.getWorld().getName(), currentRadius, newRadius);

        // Create request model
        ExpansionRequest request = new ExpansionRequest(
                requester.getUniqueId(),
                plot.getOwner(),
                requester.getWorld().getName(),
                currentRadius,
                newRadius,
                cost
        );

        activeRequests.put(requester.getUniqueId(), request);
        requester.sendMessage("§a📜 Expansion request created! Awaiting confirmation...");
        return true;
    }

    /**
     * Approve an existing expansion request.
     * Deducts currency or items and applies radius increase.
     */
    public boolean approveRequest(UUID requesterId) {
        ExpansionRequest req = activeRequests.get(requesterId);
        if (req == null) return false;

        OfflinePlayer requester = Bukkit.getOfflinePlayer(req.getRequester());
        if (!(requester.isOnline())) return false;

        Player p = requester.getPlayer();
        if (p == null) return false;

        // Deduct cost
        if (!chargePlayer(p, req.getCost(), req.getWorldName())) {
            p.sendMessage("§c❌ Expansion failed: insufficient funds.");
            return false;
        }

        // Apply expansion (temporary logic)
        PlotStore.Plot plot = plugin.store().getPlotAt(p.getLocation());
        if (plot == null) {
            p.sendMessage("§c❌ Plot not found for expansion.");
            return false;
        }

        plot.setRadius(req.getRequestedRadius());
        plugin.store().savePlot(plot);
        req.approve();

        p.sendMessage("§a✔ Your plot has been successfully expanded!");
        plugin.getLogger().info("[AegisGuard] Expansion approved for " + p.getName() +
                " → Radius: " + req.getCurrentRadius() + " → " + req.getRequestedRadius());
        activeRequests.remove(requesterId);
        return true;
    }

    /**
     * Deny an existing expansion request.
     */
    public boolean denyRequest(UUID requesterId, Player admin) {
        ExpansionRequest req = activeRequests.get(requesterId);
        if (req == null) return false;

        req.deny();
        Player target = Bukkit.getPlayer(requesterId);
        if (target != null)
            target.sendMessage("§c❌ Your expansion request was denied by an admin.");

        admin.sendMessage("§e⚠ Denied expansion request for " + (target != null ? target.getName() : requesterId));
        plugin.getLogger().info("[AegisGuard] Expansion request denied for " + requesterId);
        activeRequests.remove(requesterId);
        return true;
    }

    /* -----------------------------
     * Cost Logic
     * ----------------------------- */

    private double calculateCost(String world, int currentRadius, int newRadius) {
        double baseCost = plugin.getConfig().getDouble("claim_cost", 100.0);
        double worldModifier = plugin.getConfig().getDouble("claims.per_world." + world + ".vault_cost", baseCost);

        // Simple growth model: cost scales with radius increase
        int delta = newRadius - currentRadius;
        return worldModifier * (delta / 4.0);
    }

    private boolean chargePlayer(Player player, double amount, String worldName) {
        if (plugin.getConfig().getBoolean("use_vault", true)) {
            VaultHook vault = plugin.vault();
            if (!vault.has(player, amount)) return false;
            vault.withdraw(player, amount);
            return true;
        } else {
            // Fallback: Item-based cost
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

    public ExpansionRequest get(UUID requesterId) {
        return activeRequests.get(requesterId);
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
}
