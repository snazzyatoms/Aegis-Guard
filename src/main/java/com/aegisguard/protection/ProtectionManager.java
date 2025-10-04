package com.aegisguard.protection;

import com.aegisguard.AegisGuard;
import com.aegisguard.data.PlotStore;
import com.aegisguard.world.WorldRulesManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
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

/**
 * ProtectionManager
 * ---------------------------
 * Handles all protection rules for plots and integrates with
 * the new per-world system from WorldRulesManager.
 */
public class ProtectionManager implements Listener {

    private final AegisGuard plugin;
    private final WorldRulesManager worldRules;

    public ProtectionManager(AegisGuard plugin) {
        this.plugin = plugin;
        this.worldRules = plugin.worldRules(); // new getter in main plugin
    }

    /* -----------------------------
     * Block & Interaction Events
     * ----------------------------- */

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        PlotStore.Plot plot = plugin.store().getPlotAt(e.getBlock().getLocation());
        if (plot == null) return;

        if (!canBuild(p, plot)) {
            e.setCancelled(true);
            p.sendMessage(plugin.msg().get("cannot_break"));
            playEffect("build", "deny", p, e.getBlock().getLocation());
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
            playEffect("build", "deny", p, e.getBlock().getLocation());
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
            if (worldRules.allowContainers(block.getWorld()) && plot.getFlag("containers", true)) {
                e.setCancelled(true);
                p.sendMessage(plugin.msg().get("cannot_interact"));
                playEffect("containers", "deny", p, block.getLocation());
            }
        }
    }

    @EventHandler
    public void onPvP(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player victim)) return;
        if (!(e.getDamager() instanceof Player attacker)) return;

        PlotStore.Plot plot = plugin.store().getPlotAt(victim.getLocation());
        if (plot == null) return;

        if (!worldRules.isPvPAllowed(victim.getWorld()) && plot.getFlag("pvp", true)) {
            e.setCancelled(true);
            attacker.sendMessage(plugin.msg().get("cannot_attack"));
            playEffect("pvp", "deny", attacker, victim.getLocation());
        }
    }

    @EventHandler
    public void onPetDamage(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player attacker)) return;
        if (!(e.getEntity() instanceof Tameable pet)) return;
        if (pet.getOwner() == null) return;

        PlotStore.Plot plot = plugin.store().getPlotAt(pet.getLocation());
        if (plot == null) return;

        if (!worldRules.allowPets(pet.getWorld()) && plot.getFlag("pets", true)) {
            e.setCancelled(true);
            attacker.sendMessage(ChatColor.RED + "❌ You cannot hurt pets here!");
            playEffect("pets", "deny", attacker, pet.getLocation());
        }
    }

    @EventHandler
    public void onEntityInteract(PlayerInteractEntityEvent e) {
        if (!(e.getRightClicked() instanceof ArmorStand stand)) return;
        Player p = e.getPlayer();
        PlotStore.Plot plot = plugin.store().getPlotAt(stand.getLocation());
        if (plot == null) return;

        if (!worldRules.allowContainers(stand.getWorld()) &&
            plot.getFlag("entities", true) && !canBuild(p, plot)) {
            e.setCancelled(true);
            p.sendMessage(ChatColor.RED + "❌ You cannot modify entities here!");
            playEffect("entities", "deny", p, stand.getLocation());
        }
    }

    @EventHandler
    public void onTarget(EntityTargetEvent e) {
        if (!(e.getTarget() instanceof Player p)) return;
        PlotStore.Plot plot = plugin.store().getPlotAt(p.getLocation());
        if (plot == null) return;

        if (!worldRules.allowMobs(p.getWorld()) && plot.getFlag("mobs", true) && e.getEntity() instanceof Monster) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onSpawn(EntitySpawnEvent e) {
        if (!(e.getEntity() instanceof Monster)) return;
        PlotStore.Plot plot = plugin.store().getPlotAt(e.getLocation());
        if (plot == null) return;

        if (!worldRules.allowMobs(e.getLocation().getWorld()) && plot.getFlag("mobs", true)) {
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

        if (!worldRules.allowFarms(p.getWorld()) && plot.getFlag("farm", true)) {
            e.setCancelled(true);
            p.sendMessage(ChatColor.RED + "❌ Crops are protected here!");
            playEffect("farm", "deny", p, e.getClickedBlock().getLocation());
        }
    }

    /* -----------------------------
     * GUI Toggles
     * ----------------------------- */

    public boolean isPvPEnabled(Player player) {
        return hasFlag(player, "pvp") && worldRules.isPvPAllowed(player.getWorld());
    }
    public void togglePvP(Player player) { toggleFlag(player, "pvp"); }

    public boolean isContainersEnabled(Player player) {
        return hasFlag(player, "containers") && worldRules.allowContainers(player.getWorld());
    }
    public void toggleContainers(Player player) { toggleFlag(player, "containers"); }

    public boolean isMobProtectionEnabled(Player player) {
        return hasFlag(player, "mobs") && worldRules.allowMobs(player.getWorld());
    }
    public void toggleMobProtection(Player player) { toggleFlag(player, "mobs"); }

    public boolean isPetProtectionEnabled(Player player) {
        return hasFlag(player, "pets") && worldRules.allowPets(player.getWorld());
    }
    public void togglePetProtection(Player player) { toggleFlag(player, "pets"); }

    public boolean isEntityProtectionEnabled(Player player) {
        return hasFlag(player, "entities") && worldRules.allowContainers(player.getWorld());
    }
    public void toggleEntityProtection(Player player) { toggleFlag(player, "entities"); }

    public boolean isFarmProtectionEnabled(Player player) {
        return hasFlag(player, "farm") && worldRules.allowFarms(player.getWorld());
    }
    public void toggleFarmProtection(Player player) { toggleFlag(player, "farm"); }

    /* -----------------------------
     * Helpers
     * ----------------------------- */

    private void toggleFlag(Player player, String flag) {
        PlotStore.Plot plot = plugin.store().getPlotAt(player.getLocation());
        if (plot == null) {
            player.sendMessage(ChatColor.RED + "❌ You are not standing in your claim!");
            playEffect(flag, "fail", player, player.getLocation());
            return;
        }
        boolean current = plot.getFlag(flag, true);
        plot.setFlag(flag, !current);

        String status = !current ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF";
        player.sendMessage(ChatColor.YELLOW + "⚙ " + flag.toUpperCase() + " Protection is now " + status);
        playEffect(flag, "success", player, player.getLocation());
    }

    private boolean canBuild(Player p, PlotStore.Plot plot) {
        if (p.hasPermission("aegisguard.admin")) return true;
        if (p.getUniqueId().equals(plot.getOwner())) return true;
        return plot.getTrusted().contains(p.getUniqueId());
    }

    private boolean hasFlag(Player p, String flag) {
        PlotStore.Plot plot = plugin.store().getPlotAt(p.getLocation());
        return plot != null && plot.getFlag(flag, true);
    }

    private boolean isContainer(Material type) {
        return switch (type) {
            case CHEST, TRAPPED_CHEST, BARREL, FURNACE, BLAST_FURNACE,
                 SMOKER, HOPPER, DROPPER, DISPENSER, SHULKER_BOX -> true;
            default -> false;
        };
    }

    private void playEffect(String category, String type, Player p, Location loc) {
        if (!plugin.getConfig().getBoolean("protection_effects.enabled", true)) return;

        String base = "protection_effects." + category + "." + type + "_";
        String def = "protection_effects." + type + "_";

        String soundKey = plugin.getConfig().getString(base + "sound",
                plugin.getConfig().getString(def + "sound", "BLOCK_NOTE_BLOCK_BASS"));
        String particleKey = plugin.getConfig().getString(base + "particle",
                plugin.getConfig().getString(def + "particle", "SMOKE_NORMAL"));

        Sound sound = Sound.valueOf(soundKey);
        Particle particle = Particle.valueOf(particleKey);

        p.playSound(loc, sound, 1f, 1f);
        loc.getWorld().spawnParticle(particle, loc.clone().add(0.5, 1, 0.5), 10, 0.3, 0.3, 0.3, 0.05);
    }
}
