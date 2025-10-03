package com.aegisguard.selection;

import com.aegisguard.AegisGuard;
import com.aegisguard.data.PlotStore;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * SelectionService
 * - Handles Aegis Scepter interactions
 * - Selects corners and creates plots
 * - Integrates VaultHook & refund system
 * - Reads defaults from config.yml (global + per-world)
 * - Supports multi-plot & claim limits
 */
public class SelectionService implements Listener {

    private final AegisGuard plugin;
    private final Map<UUID, Location> corner1 = new HashMap<>();
    private final Map<UUID, Location> corner2 = new HashMap<>();

    public SelectionService(AegisGuard plugin) {
        this.plugin = plugin;
    }

    /* -----------------------------
     * Handle Wand Clicks
     * ----------------------------- */
    @EventHandler
    public void onSelect(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        Player p = e.getPlayer();

        ItemStack item = e.getItem();
        if (item == null || item.getType() != Material.LIGHTNING_ROD) return;
        if (item.getItemMeta() == null || !plugin.msg().isScepter(item.getItemMeta())) return;

        Location loc = e.getClickedBlock() != null ? e.getClickedBlock().getLocation() : null;
        if (loc == null) return;

        if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
            corner1.put(p.getUniqueId(), loc);
            plugin.msg().send(p, "corner1_set", "{X}", String.valueOf(loc.getBlockX()), "{Z}", String.valueOf(loc.getBlockZ()));
            plugin.sounds().playMenuFlip(p);
            e.setCancelled(true);
        } else if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            corner2.put(p.getUniqueId(), loc);
            plugin.msg().send(p, "corner2_set", "{X}", String.valueOf(loc.getBlockX()), "{Z}", String.valueOf(loc.getBlockZ()));
            plugin.sounds().playMenuFlip(p);
            e.setCancelled(true);
        }
    }

    /* -----------------------------
     * Confirm Claim
     * ----------------------------- */
    public void confirmClaim(Player p) {
        UUID id = p.getUniqueId();
        String worldName = p.getWorld().getName();

        // --- Load per-world or global claim settings ---
        int maxClaims = plugin.cfg().getInt("claims.per_world." + worldName + ".max_claims_per_player",
                plugin.cfg().getInt("claims.max_claims_per_player", 1));

        int currentClaims = plugin.store().getPlots(id).size();
        if (currentClaims >= maxClaims && maxClaims > 0) {
            plugin.msg().send(p, "max_claims_reached", "{AMOUNT}", String.valueOf(maxClaims));
            plugin.sounds().playMenuClose(p);
            return;
        }

        if (!corner1.containsKey(id) || !corner2.containsKey(id)) {
            plugin.msg().send(p, "must_select");
            plugin.sounds().playMenuClose(p);
            return;
        }

        // --- Economy setup ---
        boolean useVault = plugin.cfg().getBoolean("use_vault", true);
        double cost = plugin.cfg().getDouble("claim_cost", 0.0);
        String itemType = plugin.cfg().getString("item_cost.type", "DIAMOND");
        int itemAmount = plugin.cfg().getInt("item_cost.amount", 0);

        if (useVault && cost > 0 && !plugin.vault().charge(p, cost)) {
            plugin.msg().send(p, "need_vault", "{AMOUNT}", String.valueOf(cost));
            plugin.sounds().playMenuClose(p);
            return;
        } else if (!useVault && itemAmount > 0) {
            Material mat = Material.matchMaterial(itemType);
            if (mat != null) {
                if (!p.getInventory().containsAtLeast(new ItemStack(mat), itemAmount)) {
                    plugin.msg().send(p, "need_items", "{AMOUNT}", String.valueOf(itemAmount), "{ITEM}", itemType);
                    plugin.sounds().playMenuClose(p);
                    return;
                }
                p.getInventory().removeItem(new ItemStack(mat, itemAmount));
            }
        }

        Location c1 = corner1.get(id);
        Location c2 = corner2.get(id);

        // Save claim
        plugin.store().createPlot(id, c1, c2);

        plugin.msg().send(p, "plot_created");
        if (useVault && cost > 0) {
            plugin.msg().send(p, "cost_deducted", "{AMOUNT}", String.valueOf(cost));
        } else if (!useVault && itemAmount > 0) {
            plugin.msg().send(p, "items_deducted", "{AMOUNT}", String.valueOf(itemAmount), "{ITEM}", itemType);
        }

        plugin.sounds().playClaimMagic(p);

        if (plugin.cfg().getBoolean("effects.on_claim.lightning_visual", true)) {
            p.getWorld().strikeLightningEffect(c1);
        }
    }

    /* -----------------------------
     * Unclaim (only plot you’re standing in)
     * ----------------------------- */
    public void unclaimHere(Player p) {
        UUID id = p.getUniqueId();
        PlotStore.Plot plot = plugin.store().getPlotAt(p.getLocation());

        if (plot == null || !plot.getOwner().equals(id)) {
            plugin.msg().send(p, "no_plot_here");
            plugin.sounds().playMenuClose(p);
            return;
        }

        // Refund system
        boolean refundEnabled = plugin.cfg().getBoolean("refund_on_unclaim", false);
        int refundPercent = plugin.cfg().getInt("refund_percent", 0);
        boolean useVault = plugin.cfg().getBoolean("use_vault", true);
        double vaultCost = plugin.cfg().getDouble("claim_cost", 0.0);
        String itemType = plugin.cfg().getString("item_cost.type", "DIAMOND");
        int itemAmount = plugin.cfg().getInt("item_cost.amount", 0);

        if (refundEnabled && refundPercent > 0) {
            if (useVault && vaultCost > 0) {
                double refundAmount = (vaultCost * refundPercent) / 100.0;
                plugin.vault().give(p, refundAmount);
                plugin.msg().send(p, "vault_refund", "{AMOUNT}", String.valueOf(refundAmount), "{PERCENT}", String.valueOf(refundPercent));
            } else {
                Material mat = Material.matchMaterial(itemType);
                if (mat != null && itemAmount > 0) {
                    int refundCount = (int) Math.floor((itemAmount * refundPercent) / 100.0);
                    if (refundCount > 0) {
                        p.getInventory().addItem(new ItemStack(mat, refundCount));
                        plugin.msg().send(p, "item_refund", "{AMOUNT}", String.valueOf(refundCount), "{ITEM}", itemType, "{PERCENT}", String.valueOf(refundPercent));
                    }
                }
            }
        }

        plugin.store().removePlot(plot.getOwner(), plot.getPlotId());
        plugin.msg().send(p, "plot_unclaimed");

        plugin.sounds().playUnclaim(p);
    }
}
