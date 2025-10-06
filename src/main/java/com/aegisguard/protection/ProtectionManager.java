package com.aegisguard.protection;

import com.aegisguard.AegisGuard;
import com.aegisguard.data.PlotStore;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * ProtectionManager (AegisGuard)
 * ---------------------------------------------
 * Enforces per-plot flags:
 *  - pvp, containers, mobs, pets, entities, farm
 *  - safe_zone (master switch: if true, all protections apply)
 *
 * Owners + Trusted bypass build/container/entity protections.
 * PvP protection blocks PvP regardless of trust.
 */
public class ProtectionManager implements Listener {

    private final AegisGuard plugin;

    public ProtectionManager(AegisGuard plugin) {
        this.plugin = plugin;
    }

    /* -----------------------------------------------------
     *  EVENT HANDLERS — Build & Interact
     * ----------------------------------------------------- */

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

        // Container access is protected for non-trusted when flag active
        if (!canBuild(p, plot) && isContainer(block.getType()) && enabled(plot, "containers")) {
            e.setCancelled(true);
            p.sendMessage(plugin.msg().get("cannot_interact"));
            playEffect("containers", "deny", p, block.getLocation());
        }
    }

    /* -----------------------------------------------------
     *  PVP & Entity Protections
     * ----------------------------------------------------- */

    @EventHandler
    public void onPvP(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player victim)) return;
        if (!(e.getDamager() instanceof Player attacker)) return;

        PlotStore.Plot plot = plugin.store().getPlotAt(victim.getLocation());
        if (plot == null) return;

        if (enabled(plot, "pvp")) {
            e.setCancelled(true);
            attacker.sendMessage(plugin.msg().get("cannot_attack"));
            playEffect("pvp", "deny", attacker, victim.getLocation());
        }
    }

    @EventHandler
    public void onPetDamage(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player attacker)) return;
        if (!(e.getEntity() instanceof Tameable pet)) return;

        PlotStore.Plot plot = plugin.store().getPlotAt(e.getEntity().getLocation());
        if (plot == null) return;

        if (enabled(plot, "pets")) {
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

        // Protect decorative entities for non-trusted
        if (!canBuild(p, plot) && enabled(plot, "entities")) {
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

        if (enabled(plot, "mobs") && e.getEntity() instanceof Monster) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onSpawn(EntitySpawnEvent e) {
        if (!(e.getEntity() instanceof Monster)) return;

        PlotStore.Plot plot = plugin.store().getPlotAt(e.getLocation());
        if (plot == null) return;

        if (enabled(plot, "mobs")) {
            e.setCancelled(true);
        }
    }

    /* -----------------------------------------------------
     *  Crop / Farm Protections
     * ----------------------------------------------------- */

    @EventHandler
    public void onFarmTrample(PlayerInteractEvent e) {
        if (e.getAction() != Action.PHYSICAL) return;
        if (e.getClickedBlock() == null || e.getClickedBlock().getType() != Material.FARMLAND) return;

        Player p = e.getPlayer();
        PlotStore.Plot plot = plugin.store().getPlotAt(e.getClickedBlock().getLocation());
        if (plot == null) return;

        if (enabled(plot, "farm")) {
            e.setCancelled(true);
            p.sendMessage(ChatColor.RED + "❌ Crops are protected here!");
            playEffect("farm", "deny", p, e.getClickedBlock().getLocation());
        }
    }

    /* -----------------------------------------------------
     *  FLAG API (used by Settings GUI)
     * ----------------------------------------------------- */

    private void toggleFlag(Player player, String flag) {
        PlotStore.Plot plot = plugin.store().getPlotAt(player.getLocation());
        if (plot == null) {
            player.sendMessage(ChatColor.RED + "❌ You are not standing in your claim!");
            playEffect(flag, "fail", player, player.getLocation());
            return;
        }

        boolean current = plot.getFlag(flag, true);
        plot.setFlag(flag, !current);
        plugin.store().flushSync();

        String status = !current ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF";
        player.sendMessage(ChatColor.YELLOW + "⚙ " + flag.toUpperCase() + " protection is now " + status);
        playEffect(flag, "success", player, player.getLocation());
    }

    // Exposed to SettingsGUI
    public boolean isPvPEnabled(Player player)        { return hasFlag(player, "pvp"); }
    public void    togglePvP(Player player)           { toggleFlag(player, "pvp"); }

    public boolean isContainersEnabled(Player player) { return hasFlag(player, "containers"); }
    public void    toggleContainers(Player player)    { toggleFlag(player, "containers"); }

    public boolean isMobProtectionEnabled(Player p)   { return hasFlag(p, "mobs"); }
    public void    toggleMobProtection(Player p)      { toggleFlag(p, "mobs"); }

    public boolean isPetProtectionEnabled(Player p)   { return hasFlag(p, "pets"); }
    public void    togglePetProtection(Player p)      { toggleFlag(p, "pets"); }

    public boolean isEntityProtectionEnabled(Player p){ return hasFlag(p, "entities"); }
    public void    toggleEntityProtection(Player p)   { toggleFlag(p, "entities"); }

    public boolean isFarmProtectionEnabled(Player p)  { return hasFlag(p, "farm"); }
    public void    toggleFarmProtection(Player p)     { toggleFlag(p, "farm"); }

    public boolean isSafeZoneEnabled(Player p)        { return hasFlag(p, "safe_zone"); }
    public void    toggleSafeZone(Player p)           { toggleFlag(p, "safe_zone"); }

    /* -----------------------------------------------------
     *  HELPERS
     * ----------------------------------------------------- */

    /** Master enable: true if safe_zone OR the flag itself is true. */
    private boolean enabled(PlotStore.Plot plot, String flag) {
        return plot.getFlag("safe_zone", false) || plot.getFlag(flag, true);
    }

    private boolean canBuild(Player p, PlotStore.Plot plot) {
        if (p.hasPermission("aegis.admin")) return true; // unified admin perm
        if (p.getUniqueId().equals(plot.getOwner())) return true;
        return plot.getTrusted().contains(p.getUniqueId());
    }

    private boolean hasFlag(Player p, String flag) {
        PlotStore.Plot plot = plugin.store().getPlotAt(p.getLocation());
        return plot != null && enabled(plot, flag);
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
        String def  = "protection_effects." + type + "_";

        String soundKey = plugin.getConfig().getString(base + "sound",
                plugin.getConfig().getString(def + "sound", "BLOCK_NOTE_BLOCK_BASS"));
        String particleKey = plugin.getConfig().getString(base + "particle",
                plugin.getConfig().getString(def + "particle", "SMOKE_NORMAL"));

        try {
            Sound sound = Sound.valueOf(soundKey);
            Particle particle = Particle.valueOf(particleKey);
            p.playSound(loc, sound, 1f, 1f);
            loc.getWorld().spawnParticle(particle, loc.clone().add(0.5, 1, 0.5), 10, 0.3, 0.3, 0.3, 0.05);
        } catch (IllegalArgumentException ignored) {
            // invalid sound/particle fallback
        }
    }
}
