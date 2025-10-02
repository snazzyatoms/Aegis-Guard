package com.aegisguard.selection;

import com.aegisguard.AegisGuard;
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
 * - Opens Guardian Codex GUI via right-click air
 * - Integrates VaultHook & refund system
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
        if (item.getItemMeta() == null || !plugin.msg().isScepter(item.getItemMeta())) return;

        Action action = e.getAction();
        Location loc = e.getClickedBlock() != null ? e.getClickedBlock().getLocation() : null;

        // Right-click air → Open Guardian Codex GUI
        if (action == Action.RIGHT_CLICK_AIR) {
            plugin.gui().openMain(p);
            p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
            return;
        }

        // Sneak + right-click block → Open Guardian Codex GUI
        if (action == Action.RIGHT_CLICK_BLOCK && p.isSneaking()) {
            plugin.gui().openMain(p);
            p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 0.8f);
            e.setCancelled(true);
            return;
        }

        // Handle claim corner selection
        if (loc == null) return;

        if (action == Action.LEFT_CLICK_BLOCK) {
            corner1.put(p.getUniqueId(), loc);
            plugin.msg().send(p, "corner1_set", "{X}", String.valueOf(loc.getBlockX()), "{Z}", String.valueOf(loc.getBlockZ()));
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f);
            e.setCancelled(true);
        } else if (action == Action.RIGHT_CLICK_BLOCK) {
            corner2.put(p.getUniqueId(), loc);
            plugin.msg().send(p, "corner2_set", "{X}", String.valueOf(loc.getBlockX()), "{Z}", String.valueOf(loc.getBlockZ()));
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
            plugin.msg().send(p, "already_has_plot");
            return;
        }
        if (!corner1.containsKey(id) || !corner2.containsKey(id)) {
            plugin.msg().send(p, "must_select");
            return;
        }

        // Economy config: prioritize quick defaults, fallback to claim.*
        boolean useVault = plugin.getConfig().getBoolean("use_vault", plugin.getConfig().getBoolean("claim.use_vault", true));
        double cost = plugin.getConfig().getDouble("claim_cost", plugin.getConfig().getDouble("claim.cost", 0.0));
        String itemType = plugin.getConfig().getString("item_cost.type", plugin.getConfig().getString("claim.item.type", "DIAMOND"));
        int itemAmount = plugin.getConfig().getInt("item_cost.amount", plugin.getConfig().getInt("claim.item.amount", 0));

        if (useVault && cost > 0 && !plugin.vault().charge(p, cost)) {
            plugin.msg().send(p, "need_vault", "{AMOUNT}", String.valueOf(cost));
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
            return;
        } else if (!useVault && itemAmount > 0) {
            Material mat = Material.matchMaterial(itemType);
            if (mat != null) {
                ItemStack required = new ItemStack(mat, itemAmount);
                if (!p.getInventory().containsAtLeast(required, itemAmount)) {
                    plugin.msg().send(p, "need_items", "{AMOUNT}", String.valueOf(itemAmount), "{ITEM}", itemType);
                    return;
                }
                p.getInventory().removeItem(required);
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
            plugin.msg().send(p, "no_plot_here");
            return;
        }

        // Refund system: defaults first, then claim.*
        boolean refundEnabled = plugin.getConfig().getBoolean("refund_on_unclaim", plugin.getConfig().getBoolean("claim.refund_on_unclaim", false));
        int refundPercent = plugin.getConfig().getInt("refund_percent", plugin.getConfig().getInt("claim.refund_percent", 0));
        boolean useVault = plugin.getConfig().getBoolean("use_vault", plugin.getConfig().getBoolean("claim.use_vault", true));
        double vaultCost = plugin.getConfig().getDouble("claim_cost", plugin.getConfig().getDouble("claim.cost", 0.0));
        String itemType = plugin.getConfig().getString("item_cost.type", plugin.getConfig().getString("claim.item.type", "DIAMOND"));
        int itemAmount = plugin.getConfig().getInt("item_cost.amount", plugin.getConfig().getInt("claim.item.amount", 0));

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

        // Remove plot
        plugin.store().removePlot(id);
        plugin.msg().send(p, "plot_unclaimed");
        p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_BREAK, 1f, 1f);
    }
}
