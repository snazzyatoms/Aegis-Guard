package com.aegisguard.selection;

import com.aegisguard.AegisGuard;
import com.aegisguard.data.PlotStore;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SelectionService implements Listener {

    private final AegisGuard plugin;
    private final Map<UUID, Location> corner1 = new HashMap<>();
    private final Map<UUID, Location> corner2 = new HashMap<>();
    private final NamespacedKey SCEPTER_KEY;

    public SelectionService(AegisGuard plugin) {
        this.plugin = plugin;
        this.SCEPTER_KEY = new NamespacedKey(plugin, "scepter");
    }

    /* -----------------------------
     * Handle Wand Clicks
     * ----------------------------- */
    @EventHandler
    public void onSelect(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getItem() == null) return;

        Player p = e.getPlayer();
        ItemStack item = e.getItem();
        if (!isScepter(item)) return;

        // Only when clicking blocks (air clicks ignored)
        if (e.getClickedBlock() == null) return;

        Location loc = e.getClickedBlock().getLocation();
        Action action = e.getAction();

        if (action == Action.LEFT_CLICK_BLOCK) {
            corner1.put(p.getUniqueId(), loc);
            plugin.msg().send(p, "corner1_set", "X", String.valueOf(loc.getBlockX()), "Z", String.valueOf(loc.getBlockZ()));
            playFlip(p);
            e.setCancelled(true);
        } else if (action == Action.RIGHT_CLICK_BLOCK) {
            corner2.put(p.getUniqueId(), loc);
            plugin.msg().send(p, "corner2_set", "X", String.valueOf(loc.getBlockX()), "Z", String.valueOf(loc.getBlockZ()));
            playFlip(p);
            e.setCancelled(true);
        }
    }

    /* -----------------------------
     * Confirm Claim
     * ----------------------------- */
    public void confirmClaim(Player p) {
        UUID id = p.getUniqueId();
        String worldName = p.getWorld().getName();

        // Must have both corners
        Location c1 = corner1.get(id);
        Location c2 = corner2.get(id);
        if (c1 == null || c2 == null) {
            plugin.msg().send(p, "must_select");
            playClose(p);
            return;
        }

        // Same world & current world only
        if (c1.getWorld() == null || c2.getWorld() == null || !c1.getWorld().equals(c2.getWorld()) || !c1.getWorld().equals(p.getWorld())) {
            plugin.msg().send(p, "selection_same_world");
            playClose(p);
            return;
        }

        // Normalize bounds
        int x1 = Math.min(c1.getBlockX(), c2.getBlockX());
        int z1 = Math.min(c1.getBlockZ(), c2.getBlockZ());
        int x2 = Math.max(c1.getBlockX(), c2.getBlockX());
        int z2 = Math.max(c1.getBlockZ(), c2.getBlockZ());

        int width  = (x2 - x1) + 1;
        int height = (z2 - z1) + 1;
        int area   = width * height;

        // Per-world overrides → global fallback
        int maxClaims = plugin.cfg().getInt("claims.per_world." + worldName + ".max_claims_per_player",
                        plugin.cfg().getInt("claims.max_claims_per_player", 1));
        int minSize   = plugin.cfg().getInt("claims.per_world." + worldName + ".min_size",
                        plugin.cfg().getInt("claims.min_size", 1)); // min side length
        int maxArea   = plugin.cfg().getInt("claims.per_world." + worldName + ".max_area",
                        plugin.cfg().getInt("claims.max_area", 0)); // 0 = unlimited

        // Claim count check
        int currentClaims = plugin.store().getPlots(id).size();
        if (maxClaims > 0 && currentClaims >= maxClaims) {
            plugin.msg().send(p, "max_claims_reached", "AMOUNT", String.valueOf(maxClaims));
            playClose(p);
            return;
        }

        // Size checks
        if (width < minSize || height < minSize) {
            plugin.msg().send(p, "min_size_fail", "MIN", String.valueOf(minSize));
            playClose(p);
            return;
        }
        if (maxArea > 0 && area > maxArea) {
            plugin.msg().send(p, "max_area_fail", "MAX", String.valueOf(maxArea), "AREA", String.valueOf(area));
            playClose(p);
            return;
        }

        // Overlap check against existing plots
        if (overlapsExisting(c1.getWorld().getName(), x1, z1, x2, z2)) {
            plugin.msg().send(p, "overlap_fail");
            playClose(p);
            return;
        }

        // --- Costs (validated AFTER checks) ---
        boolean useVault = plugin.cfg().getBoolean("claims.per_world." + worldName + ".use_vault",
                            plugin.cfg().getBoolean("use_vault", true));

        double cost = plugin.cfg().getDouble("claims.per_world." + worldName + ".vault_cost",
                        plugin.cfg().getDouble("claim_cost", 0.0));

        String itemType = plugin.cfg().getString("claims.per_world." + worldName + ".item_cost.type",
                        plugin.cfg().getString("item_cost.type", "DIAMOND"));

        int itemAmount = plugin.cfg().getInt("claims.per_world." + worldName + ".item_cost.amount",
                        plugin.cfg().getInt("item_cost.amount", 0));

        if (useVault && cost > 0) {
            if (!plugin.vault().charge(p, cost)) {
                plugin.msg().send(p, "need_vault", "AMOUNT", String.valueOf(cost));
                playClose(p);
                return;
            }
        } else if (!useVault && itemAmount > 0) {
            Material mat = Material.matchMaterial(itemType);
            if (mat == null) {
                plugin.msg().send(p, "need_items", "AMOUNT", String.valueOf(itemAmount), "ITEM", itemType);
                playClose(p);
                return;
            }
            if (!hasItems(p.getInventory(), mat, itemAmount)) {
                plugin.msg().send(p, "need_items", "AMOUNT", String.valueOf(itemAmount), "ITEM", itemType);
                playClose(p);
                return;
            }
            removeItems(p.getInventory(), mat, itemAmount);
        }

        // Create plot
        plugin.store().createPlot(id, c1, c2);
        plugin.msg().send(p, "plot_created", "AREA", String.valueOf(area));

        if (useVault && cost > 0) {
            plugin.msg().send(p, "cost_deducted", "AMOUNT", String.valueOf(cost));
        } else if (!useVault && itemAmount > 0) {
            plugin.msg().send(p, "items_deducted", "AMOUNT", String.valueOf(itemAmount), "ITEM", itemType);
        }

        // Feedback
        playClaim(p);
        if (plugin.cfg().getBoolean("effects.on_claim.lightning_visual", true)) {
            p.getWorld().strikeLightningEffect(c1);
        }

        // Optionally clear selection
        corner1.remove(id);
        corner2.remove(id);
    }

    /* -----------------------------
     * Unclaim (only plot you’re standing in)
     * ----------------------------- */
    public void unclaimHere(Player p) {
        UUID id = p.getUniqueId();
        PlotStore.Plot plot = plugin.store().getPlotAt(p.getLocation());

        if (plot == null || !plot.getOwner().equals(id)) {
            plugin.msg().send(p, "no_plot_here");
            playClose(p);
            return;
        }

        String worldName = p.getWorld().getName();

        // Refund system (per-world > global)
        boolean refundEnabled = plugin.cfg().getBoolean("claims.per_world." + worldName + ".refund_on_unclaim",
                plugin.cfg().getBoolean("refund_on_unclaim", false));

        int refundPercent = plugin.cfg().getInt("claims.per_world." + worldName + ".refund_percent",
                plugin.cfg().getInt("refund_percent", 0));

        boolean useVault = plugin.cfg().getBoolean("claims.per_world." + worldName + ".use_vault",
                plugin.cfg().getBoolean("use_vault", true));

        double vaultCost = plugin.cfg().getDouble("claims.per_world." + worldName + ".vault_cost",
                plugin.cfg().getDouble("claim_cost", 0.0));

        String itemType = plugin.cfg().getString("claims.per_world." + worldName + ".item_cost.type",
                plugin.cfg().getString("item_cost.type", "DIAMOND"));

        int itemAmount = plugin.cfg().getInt("claims.per_world." + worldName + ".item_cost.amount",
                plugin.cfg().getInt("item_cost.amount", 0));

        if (refundEnabled && refundPercent > 0) {
            if (useVault && vaultCost > 0) {
                double refundAmount = (vaultCost * refundPercent) / 100.0;
                plugin.vault().give(p, refundAmount);
                plugin.msg().send(p, "vault_refund", "AMOUNT", String.valueOf(refundAmount), "PERCENT", String.valueOf(refundPercent));
            } else {
                Material mat = Material.matchMaterial(itemType);
                if (mat != null && itemAmount > 0) {
                    int refundCount = (int) Math.floor((itemAmount * refundPercent) / 100.0);
                    if (refundCount > 0) {
                        p.getInventory().addItem(new ItemStack(mat, refundCount));
                        plugin.msg().send(p, "item_refund", "AMOUNT", String.valueOf(refundCount), "ITEM", itemType, "PERCENT", String.valueOf(refundPercent));
                    }
                }
            }
        }

        plugin.store().removePlot(plot.getOwner(), plot.getPlotId());
        plugin.msg().send(p, "plot_unclaimed");
        playUnclaim(p);
    }

    /* -----------------------------
     * Helpers
     * ----------------------------- */

    private boolean isScepter(ItemStack item) {
        if (item.getType() != Material.LIGHTNING_ROD) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        // Prefer PDC tag
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Byte tag = pdc.get(SCEPTER_KEY, PersistentDataType.BYTE);
        if (tag != null && tag == (byte) 1) return true;

        // Fallback to display name check (matches AegisGuard#createScepter default)
        String dn = meta.getDisplayName();
        if (dn == null) return false;
        String plain = org.bukkit.ChatColor.stripColor(dn).trim().toLowerCase();
        return plain.equals("aegis scepter");
    }

    private boolean overlapsExisting(String world, int x1, int z1, int x2, int z2) {
        for (UUID owner : plugin.store().owners()) {
            for (PlotStore.Plot p : plugin.store().getPlots(owner)) {
                if (!p.getWorld().equals(world)) continue;
                // axis-aligned rectangle overlap test
                boolean separated = x2 < p.getX1() || p.getX2() < x1 || z2 < p.getZ1() || p.getZ2() < z1;
                if (!separated) return true;
            }
        }
        return false;
    }

    private boolean hasItems(PlayerInventory inv, Material mat, int amount) {
        int count = 0;
        for (ItemStack stack : inv.getContents()) {
            if (stack != null && stack.getType() == mat) count += stack.getAmount();
            if (count >= amount) return true;
        }
        return false;
    }

    private void removeItems(PlayerInventory inv, Material mat, int amount) {
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s == null || s.getType() != mat) continue;
            int take = Math.min(amount, s.getAmount());
            s.setAmount(s.getAmount() - take);
            if (s.getAmount() <= 0) inv.setItem(i, null);
            amount -= take;
            if (amount <= 0) break;
        }
    }

    private void playFlip(Player p)  { if (plugin.isSoundEnabled(p)) p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f); }
    private void playClose(Player p) { if (plugin.isSoundEnabled(p)) p.playSound(p.getLocation(), Sound.BLOCK_CHEST_CLOSE, 0.7f, 1.0f); }
    private void playClaim(Player p) { if (plugin.isSoundEnabled(p)) p.playSound(p.getLocation(), Sound.ITEM_TOTEM_USE, 0.8f, 1.0f); }
    private void playUnclaim(Player p){ if (plugin.isSoundEnabled(p)) p.playSound(p.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.8f, 1.0f); }
}
