package com.aegisguard.protection;

import com.aegisguard.AegisGuard;
import com.aegisguard.data.PlotStore;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class ProtectionManager implements Listener {

    private final AegisGuard plugin;

    public ProtectionManager(AegisGuard plugin) {
        this.plugin = plugin;
    }

    /* -----------------------------
     * Event Handlers
     * ----------------------------- */

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        PlotStore.Plot plot = plugin.store().getPlotAt(e.getBlock().getLocation());
        if (plot == null) return;

        if (!canBuild(p, plot)) {
            e.setCancelled(true);
            plugin.msg().send(p, "cannot_break");
            denyEffect(p, e.getBlock().getLocation());
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        PlotStore.Plot plot = plugin.store().getPlotAt(e.getBlock().getLocation());
        if (plot == null) return;

        if (!canBuild(p, plot)) {
            e.setCancelled(true);
            plugin.msg().send(p, "cannot_place");
            denyEffect(p, e.getBlock().getLocation());
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getClickedBlock() == null) return;
        Player p = e.getPlayer();
        Block block = e.getClickedBlock();
        PlotStore.Plot plot = plugin.store().getPlotAt(block.getLocation());
        if (plot == null) return;

        if (!canBuild(p, plot) && isContainer(block.getType())) {
            if (plot.getFlag("containers", true)) {
                e.setCancelled(true);
                plugin.msg().send(p, "cannot_interact");
                denyEffect(p, block.getLocation());
            }
        }
    }

    @EventHandler
    public void onPvP(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player victim)) return;
        if (!(e.getDamager() instanceof Player attacker)) return;

        PlotStore.Plot plot = plugin.store().getPlotAt(victim.getLocation());
        if (plot == null) return;

        if (plot.getFlag("pvp", true)) {
            e.setCancelled(true);
            plugin.msg().send(attacker, "cannot_attack");
            denyEffect(attacker, victim.getLocation());
        }
    }

    @EventHandler
    public void onPetDamage(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player attacker)) return;
        if (!(e.getEntity() instanceof Tameable pet)) return;
        if (pet.getOwner() == null) return;

        PlotStore.Plot plot = plugin.store().getPlotAt(pet.getLocation());
        if (plot == null) return;

        if (plot.getFlag("pets", true)) {
            e.setCancelled(true);
            plugin.msg().send(attacker, "cannot_attack");
            denyEffect(attacker, pet.getLocation());
        }
    }

    @EventHandler
    public void onEntityInteract(PlayerInteractEntityEvent e) {
        if (!(e.getRightClicked() instanceof ArmorStand stand)) return;
        Player p = e.getPlayer();
        PlotStore.Plot plot = plugin.store().getPlotAt(stand.getLocation());
        if (plot == null) return;

        if (plot.getFlag("entities", true) && !canBuild(p, plot)) {
            e.setCancelled(true);
            plugin.msg().send(p, "cannot_interact");
            denyEffect(p, stand.getLocation());
        }
    }

    @EventHandler
    public void onTarget(EntityTargetEvent e) {
        if (!(e.getTarget() instanceof Player p)) return;
        PlotStore.Plot plot = plugin.store().getPlotAt(p.getLocation());
        if (plot == null) return;

        if (plot.getFlag("mobs", true) && e.getEntity() instanceof Monster) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onSpawn(EntitySpawnEvent e) {
        if (!(e.getEntity() instanceof Monster)) return;
        PlotStore.Plot plot = plugin.store().getPlotAt(e.getLocation());
        if (plot == null) return;

        if (plot.getFlag("mobs", true)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onFarmTrample(PlayerInteractEvent e) {
        if (e.getAction() != Action.PHYSICAL) return;
        if (e.getClickedBlock() == null || e.getClickedBlock().getType() != Material.FARMLAND) return;

        Player p = e.getPlayer();
        PlotStore.Plot plot = plugin.store().getPlotAt(e.getClickedBlock().getLocation());
        if (plot == null) return;

        if (plot.getFlag("farm", true)) {
            e.setCancelled(true);
            plugin.msg().send(p, "cannot_interact");
            denyEffect(p, e.getClickedBlock().getLocation());
        }
    }

    /* -----------------------------
     * Toggle Accessors for GUI
     * ----------------------------- */

    private void toggleFlag(Player player, String flag) {
        PlotStore.Plot plot = plugin.store().getPlotAt(player.getLocation());
        if (plot == null) {
            plugin.msg().send(player, "must_select");
            return;
        }
        boolean current = plot.getFlag(flag, true);
        plot.setFlag(flag, !current);
        plugin.store().save();

        if (!current) {
            successEffect(player);
            player.sendMessage(plugin.msg().color("&e⚙ " + flag.toUpperCase() + " Protection: &aON"));
        } else {
            failEffect(player);
            player.sendMessage(plugin.msg().color("&e⚙ " + flag.toUpperCase() + " Protection: &cOFF"));
        }
    }

    public boolean isPvPEnabled(Player player) { return checkFlag(player, "pvp"); }
    public void togglePvP(Player player) { toggleFlag(player, "pvp"); }

    public boolean isContainersEnabled(Player player) { return checkFlag(player, "containers"); }
    public void toggleContainers(Player player) { toggleFlag(player, "containers"); }

    public boolean isMobProtectionEnabled(Player player) { return checkFlag(player, "mobs"); }
    public void toggleMobProtection(Player player) { toggleFlag(player, "mobs"); }

    public boolean isPetProtectionEnabled(Player player) { return checkFlag(player, "pets"); }
    public void togglePetProtection(Player player) { toggleFlag(player, "pets"); }

    public boolean isEntityProtectionEnabled(Player player) { return checkFlag(player, "entities"); }
    public void toggleEntityProtection(Player player) { toggleFlag(player, "entities"); }

    public boolean isFarmProtectionEnabled(Player player) { return checkFlag(player, "farm"); }
    public void toggleFarmProtection(Player player) { toggleFlag(player, "farm"); }

    /* -----------------------------
     * Helpers
     * ----------------------------- */
    private boolean checkFlag(Player p, String flag) {
        PlotStore.Plot plot = plugin.store().getPlotAt(p.getLocation());
        return plot != null && plot.getFlag(flag, true);
    }

    private boolean canBuild(Player p, PlotStore.Plot plot) {
        if (p.hasPermission("aegisguard.admin")) return true;
        if (p.getUniqueId().equals(plot.getOwner())) return true;
        return plot.getTrusted().contains(p.getUniqueId());
    }

    private boolean isContainer(Material type) {
        return switch (type) {
            case CHEST, TRAPPED_CHEST, BARREL, FURNACE, BLAST_FURNACE,
                 SMOKER, HOPPER, DROPPER, DISPENSER, SHULKER_BOX -> true;
            default -> false;
        };
    }

    /* -----------------------------
     * Visual + Sound Effects
     * ----------------------------- */
    private void denyEffect(Player p, Location loc) {
        p.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.6f);
        loc.getWorld().spawnParticle(Particle.SMOKE_NORMAL, loc.add(0.5, 1, 0.5), 8, 0.3, 0.3, 0.3, 0.01);
    }

    private void successEffect(Player p) {
        p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
        p.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, p.getLocation().add(0, 1, 0), 12, 0.5, 0.5, 0.5, 0.1);
    }

    private void failEffect(Player p) {
        p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_LAND, 1f, 0.8f);
        p.getWorld().spawnParticle(Particle.SMOKE_NORMAL, p.getLocation().add(0, 1, 0), 12, 0.5, 0.5, 0.5, 0.05);
    }
}
