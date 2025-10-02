package com.aegisguard.protection;

import com.aegisguard.AegisGuard;
import com.aegisguard.data.PlotStore;
import org.bukkit.ChatColor;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.UUID;

/**
 * ProtectionManager
 * - Enforces protections inside plots
 * - Prevents griefing, mob targeting, PvP
 */
public class ProtectionManager implements Listener {

    private final AegisGuard plugin;

    public ProtectionManager(AegisGuard plugin) {
        this.plugin = plugin;
    }

    /* -----------------------------
     * Block Break
     * ----------------------------- */
    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        PlotStore.Plot plot = plugin.store().getPlotAt(e.getBlock().getLocation());

        if (plot == null) return; // not in a claim
        if (!canBuild(p, plot)) {
            e.setCancelled(true);
            p.sendMessage(ChatColor.RED + "❌ You cannot break blocks here!");
        }
    }

    /* -----------------------------
     * Block Place
     * ----------------------------- */
    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        PlotStore.Plot plot = plugin.store().getPlotAt(e.getBlock().getLocation());

        if (plot == null) return;
        if (!canBuild(p, plot)) {
            e.setCancelled(true);
            p.sendMessage(ChatColor.RED + "❌ You cannot place blocks here!");
        }
    }

    /* -----------------------------
     * Player Interact (chests, doors, etc.)
     * ----------------------------- */
    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getClickedBlock() == null) return;
        Player p = e.getPlayer();
        PlotStore.Plot plot = plugin.store().getPlotAt(e.getClickedBlock().getLocation());

        if (plot == null) return;
        if (!canBuild(p, plot)) {
            e.setCancelled(true);
            p.sendMessage(ChatColor.RED + "❌ You cannot interact here!");
        }
    }

    /* -----------------------------
     * PvP Protection
     * ----------------------------- */
    @EventHandler
    public void onPvP(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player victim)) return;
        if (!(e.getDamager() instanceof Player attacker)) return;

        PlotStore.Plot plot = plugin.store().getPlotAt(victim.getLocation());
        if (plot == null) return; // outside claim = normal PvP

        UUID owner = plot.getOwner();

        // prevent trusted from attacking owner or each other
        if (attacker.getUniqueId().equals(owner) || victim.getUniqueId().equals(owner)) {
            e.setCancelled(true);
            return;
        }
        if (plot.getTrusted().contains(attacker.getUniqueId()) && plot.getTrusted().contains(victim.getUniqueId())) {
            e.setCancelled(true);
        }
    }

    /* -----------------------------
     * Mob Target Protection
     * ----------------------------- */
    @EventHandler
    public void onTarget(EntityTargetEvent e) {
        if (!(e.getTarget() instanceof Player p)) return;
        PlotStore.Plot plot = plugin.store().getPlotAt(p.getLocation());
        if (plot == null) return;

        if (e.getEntity() instanceof LivingEntity mob) {
            if (mob.getType() == EntityType.ZOMBIE ||
                mob.getType() == EntityType.SKELETON ||
                mob.getType() == EntityType.CREEPER ||
                mob.getType() == EntityType.SPIDER ||
                mob.getType() == EntityType.ENDERMAN) {
                e.setCancelled(true); // hostile mobs ignore players inside claims
            }
        }
    }

    /* -----------------------------
     * Optional: Cancel hostile spawns in claims
     * ----------------------------- */
    @EventHandler
    public void onSpawn(EntitySpawnEvent e) {
        if (!(e.getEntity() instanceof LivingEntity)) return;
        if (!(e.getEntity() instanceof Player)) {
            Location loc = e.getLocation();
            PlotStore.Plot plot = plugin.store().getPlotAt(loc);
            if (plot != null && plugin.cfg().getBoolean("protections.no_mobs_in_claims", true)) {
                e.setCancelled(true);
            }
        }
    }

    /* -----------------------------
     * Helper: Can Player Build?
     * ----------------------------- */
    private boolean canBuild(Player p, PlotStore.Plot plot) {
        if (p.hasPermission("aegisguard.admin")) return true; // bypass
        if (p.getUniqueId().equals(plot.getOwner())) return true;
        return plot.getTrusted().contains(p.getUniqueId());
    }
}
