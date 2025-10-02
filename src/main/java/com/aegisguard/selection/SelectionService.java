package com.aegisguard.selection;

import com.aegisguard.AegisGuard;
import com.aegisguard.data.PlotStore;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
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
 * - Integrates VaultHook for optional claim costs
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
        if (e.getHand() != EquipmentSlot.HAND) return; // ignore offhand
        if (!(e.getPlayer() instanceof Player p)) return;

        ItemStack item = e.getItem();
        if (item == null || item.getType() != Material.LIGHTNING_ROD) return; // must be Aegis Scepter
        if (item.getItemMeta() == null || !ChatColor.stripColor(item.getItemMeta().getDisplayName()).equalsIgnoreCase("Aegis Scepter"))
            return;

        Action action = e.getAction();
        Location loc = e.getClickedBlock() != null ? e.getClickedBlock().getLocation() : null;
        if (loc == null) return;

        if (action == Action.LEFT_CLICK_BLOCK) {
            corner1.put(p.getUniqueId(), loc);
            p.sendMessage(ChatColor.GREEN + "⚡ First corner set at " + loc.getBlockX() + ", " + loc.getBlockZ());
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f);
            e.setCancelled(true);
        } else if (action == Action.RIGHT_CLICK_BLOCK) {
            corner2.put(p.getUniqueId(), loc);
            p.sendMessage(ChatColor.AQUA + "⚡ Second corner set at " + loc.getBlockX() + ", " + loc.getBlockZ());
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
            e.setCancelled(true);
        }
    }

    /* -----------------------------
     * Confirm Claim
     * ----------------------------- */
    public void confirmClaim(Player p) {
        UUID id = p.getUniqueId();
        if (plugin.store().hasPlot(id)) {
            p.sendMessage(ChatColor.RED + "❌ You already own a plot. Unclaim it first.");
            return;
        }
        if (!corner1.containsKey(id) || !corner2.containsKey(id)) {
            p.sendMessage(ChatColor.RED + "❌ Select two corners with your Aegis Scepter first.");
            return;
        }

        // Vault cost check
        double cost = plugin.getConfig().getDouble("claim.cost", 0.0);
        if (cost > 0 && !plugin.vault().charge(p, cost)) {
            p.sendMessage(ChatColor.RED + "❌ You need at least $" + cost + " to claim land.");
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
            return;
        }

        Location c1 = corner1.get(id);
        Location c2 = corner2.get(id);

        // Save claim
        plugin.store().createPlot(id, c1, c2);

        p.sendMessage(ChatColor.GREEN + "✔ Plot created successfully!");
        if (cost > 0) {
            p.sendMessage(ChatColor.YELLOW + "💰 Cost: $" + cost + " has been deducted.");
        }
        p.playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 0.7f);

        // optional lightning effect
        if (plugin.cfg().getBoolean("effects.on_claim.lightning_visual", true)) {
            p.getWorld().strikeLightningEffect(c1);
        }
    }

    /* -----------------------------
 * Unclaim
 * ----------------------------- */
public void unclaimHere(Player p) {
    UUID id = p.getUniqueId();
    if (!plugin.store().hasPlot(id)) {
        p.sendMessage(ChatColor.RED + "❌ You do not own a plot.");
        return;
    }

    // Refund system
    boolean refundEnabled = plugin.getConfig().getBoolean("claim.refund_on_unclaim", false);
    int refundPercent = plugin.getConfig().getInt("claim.refund_percent", 0);
    boolean useVault = plugin.getConfig().getBoolean("claim.use_vault", true);
    double vaultCost = plugin.getConfig().getDouble("claim.cost", 0.0);
    String itemType = plugin.getConfig().getString("claim.item.type", "DIAMOND");
    int itemAmount = plugin.getConfig().getInt("claim.item.amount", 0);

    if (refundEnabled && refundPercent > 0) {
        if (useVault && vaultCost > 0) {
            double refundAmount = (vaultCost * refundPercent) / 100.0;
            plugin.vault().give(p, refundAmount);
            p.sendMessage(ChatColor.YELLOW + "💰 Refunded $" + refundAmount + " (" + refundPercent + "%) for unclaiming.");
        } else {
            var mat = Material.matchMaterial(itemType);
            if (mat != null && itemAmount > 0) {
                int refundCount = (int) Math.floor((itemAmount * refundPercent) / 100.0);
                if (refundCount > 0) {
                    p.getInventory().addItem(new ItemStack(mat, refundCount));
                    p.sendMessage(ChatColor.YELLOW + "💎 Refunded " + refundCount + " " + itemType + "(s) (" + refundPercent + "%) for unclaiming.");
                }
            }
        }
    }

    // Remove plot
    plugin.store().removePlot(id);
    p.sendMessage(ChatColor.YELLOW + "⚠ Your plot has been unclaimed.");
    p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_BREAK, 1f, 1f);
}
