package com.aegisguard.protection;

import com.aegisguard.AegisGuard;
import com.aegisguard.data.PlotStore;
import org.bukkit.ChatColor;
import org.bukkit.Material;
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
            p.sendMessage(plugin.msg().get("cannot_break"));
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        PlotStore.Plot plot = plugin.store().getPlotAt(e.getBlock().getLocation());
        if (plot == null) return;

        if (!canBuild(p, plot)) {
            e.setCancelled(true);
            p.sendMessage(plugin.msg().get("cannot_place"));
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
                p.sendMessage(plugin.msg().get("cannot_interact"));
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
            attacker.sendMessage(plugin.msg().get("cannot_attack"));
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
            attacker.sendMessage(ChatColor.RED + "❌ You cannot hurt pets here!");
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
            p.sendMessage(ChatColor.RED + "❌ You cannot modify entities here!");
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
            p.sendMessage(ChatColor.RED + "❌ Crops are protected here!");
        }
    }

    /* -----------------------------
     * Toggle Accessors for GUI
     * ----------------------------- */

    private void toggleFlag(Player player, String flag) {
        PlotStore.Plot plot = plugin.store().getPlotAt(player.getLocation());
        if (plot == null) {
            player.sendMessage(ChatColor.RED + "❌ You are not standing in your claim!");
            return;
        }
        boolean current = plot.getFlag(flag, true);
        plot.setFlag(flag, !current);

        String status = !current ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF";
        player.sendMessage(ChatColor.YELLOW + "⚙ " + flag.toUpperCase() + " Protection is now " + status);
    }

    public boolean isPvPEnabled(Player player) {
        PlotStore.Plot plot = plugin.store().getPlotAt(player.getLocation());
        return plot != null && plot.getFlag("pvp", true);
    }
    public void togglePvP(Player player) { toggleFlag(player, "pvp"); }

    public boolean isContainersEnabled(Player player) {
        PlotStore.Plot plot = plugin.store().getPlotAt(player.getLocation());
        return plot != null && plot.getFlag("containers", true);
    }
    public void toggleContainers(Player player) { toggleFlag(player, "containers"); }

    public boolean isMobProtectionEnabled(Player player) {
        PlotStore.Plot plot = plugin.store().getPlotAt(player.getLocation());
        return plot != null && plot.getFlag("mobs", true);
    }
    public void toggleMobProtection(Player player) { toggleFlag(player, "mobs"); }

    public boolean isPetProtectionEnabled(Player player) {
        PlotStore.Plot plot = plugin.store().getPlotAt(player.getLocation());
        return plot != null && plot.getFlag("pets", true);
    }
    public void togglePetProtection(Player player) { toggleFlag(player, "pets"); }

    public boolean isEntityProtectionEnabled(Player player) {
        PlotStore.Plot plot = plugin.store().getPlotAt(player.getLocation());
        return plot != null && plot.getFlag("entities", true);
    }
    public void toggleEntityProtection(Player player) { toggleFlag(player, "entities"); }

    public boolean isFarmProtectionEnabled(Player player) {
        PlotStore.Plot plot = plugin.store().getPlotAt(player.getLocation());
        return plot != null && plot.getFlag("farm", true);
    }
    public void toggleFarmProtection(Player player) { toggleFlag(player, "farm"); }

    /* -----------------------------
     * Helpers
     * ----------------------------- */
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
}
