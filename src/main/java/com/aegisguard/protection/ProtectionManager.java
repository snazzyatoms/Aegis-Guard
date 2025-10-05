package com.aegisguard.protection;

import com.aegisguard.AegisGuard;
import com.aegisguard.data.PlotStore;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class ProtectionManager implements Listener {

    private final AegisGuard plugin;

    public ProtectionManager(AegisGuard plugin) {
        this.plugin = plugin;
    }

    /* -----------------------------------------------------
     *  EVENT HANDLERS — Player & Entity Protections
     *  World rules = baseline; plot flags can only ADD protection.
     * ----------------------------------------------------- */

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
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

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
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

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getClickedBlock() == null) return;

        Player p = e.getPlayer();
        Block block = e.getClickedBlock();
        PlotStore.Plot plot = plugin.store().getPlotAt(block.getLocation());
        if (plot == null) return;

        if (!canBuild(p, plot) && isContainer(block.getType())) {
            // CANCEL if global disallows OR plot flag protects
            boolean globalDisallow = !plugin.worldRules().allowContainers(block.getWorld());
            boolean flagProtects   = plot.getFlag("containers", def("containers"));
            if (globalDisallow || flagProtects) {
                e.setCancelled(true);
                p.sendMessage(plugin.msg().get("cannot_interact"));
                playEffect("containers", "deny", p, block.getLocation());
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onPvP(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player victim)) return;

        // Resolve attacker as Player (handles melee & most projectiles)
        Player attacker = null;
        if (e.getDamager() instanceof Player p) attacker = p;
        else if (e.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p) attacker = p;
        if (attacker == null) return;

        PlotStore.Plot plot = plugin.store().getPlotAt(victim.getLocation());
        if (plot == null) return;

        boolean globalDisallow = !plugin.worldRules().isPvPAllowed(victim.getWorld());
        boolean flagProtects   = plot.getFlag("pvp", def("pvp"));

        if (globalDisallow || flagProtects) {
            e.setCancelled(true);
            attacker.sendMessage(plugin.msg().get("cannot_attack"));
            playEffect("pvp", "deny", attacker, victim.getLocation());
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onPetDamage(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player attacker)) return;
        if (!(e.getEntity() instanceof Tameable pet)) return;
        if (pet.getOwner() == null) return;

        PlotStore.Plot plot = plugin.store().getPlotAt(pet.getLocation());
        if (plot == null) return;

        boolean globalDisallow = !plugin.worldRules().allowPets(pet.getWorld());
        boolean flagProtects   = plot.getFlag("pets", def("pets"));

        if (globalDisallow || flagProtects) {
            e.setCancelled(true);
            attacker.sendMessage(ChatColor.RED + "❌ You cannot hurt pets here!");
            playEffect("pets", "deny", attacker, pet.getLocation());
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onEntityInteract(PlayerInteractEntityEvent e) {
        if (!(e.getRightClicked() instanceof ArmorStand stand)) return;
        Player p = e.getPlayer();
        PlotStore.Plot plot = plugin.store().getPlotAt(stand.getLocation());
        if (plot == null) return;

        // No explicit world toggle for "entities" — reuse containers baseline until a dedicated flag exists.
        boolean globalDisallow = !plugin.worldRules().allowContainers(stand.getWorld());
        boolean flagProtects   = plot.getFlag("entities", def("entities"));

        if (!canBuild(p, plot) && (globalDisallow || flagProtects)) {
            e.setCancelled(true);
            p.sendMessage(ChatColor.RED + "❌ You cannot modify entities here!");
            playEffect("entities", "deny", p, stand.getLocation());
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onTarget(EntityTargetEvent e) {
        if (!(e.getTarget() instanceof Player p)) return;
        PlotStore.Plot plot = plugin.store().getPlotAt(p.getLocation());
        if (plot == null) return;

        boolean globalDisallow = !plugin.worldRules().allowMobs(p.getWorld());
        boolean flagProtects   = plot.getFlag("mobs", def("mobs"));

        if ((globalDisallow || flagProtects) && e.getEntity() instanceof Monster) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onSpawn(EntitySpawnEvent e) {
        if (!(e.getEntity() instanceof Monster)) return;
        PlotStore.Plot plot = plugin.store().getPlotAt(e.getLocation());
        if (plot == null) return;

        boolean globalDisallow = !plugin.worldRules().allowMobs(e.getLocation().getWorld());
        boolean flagProtects   = plot.getFlag("mobs", def("mobs"));

        if (globalDisallow || flagProtects) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onFarmTrample(PlayerInteractEvent e) {
        if (e.getAction() != Action.PHYSICAL) return;
        if (e.getClickedBlock() == null || e.getClickedBlock().getType() != Material.FARMLAND) return;

        Player p = e.getPlayer();
        PlotStore.Plot plot = plugin.store().getPlotAt(e.getClickedBlock().getLocation());
        if (plot == null) return;

        boolean globalDisallow = !plugin.worldRules().allowFarms(p.getWorld());
        boolean flagProtects   = plot.getFlag("farm", def("farm"));

        if (globalDisallow || flagProtects) {
            e.setCancelled(true);
            p.sendMessage(ChatColor.RED + "❌ Crops are protected here!");
            playEffect("farm", "deny", p, e.getClickedBlock().getLocation());
        }
    }

    /* -----------------------------------------------------
     *  FLAG TOGGLES (Used in Settings GUI)
     *  Toggle only affects the plot; world baseline remains.
     * ----------------------------------------------------- */

    private void toggleFlag(Player player, String flag) {
        PlotStore.Plot plot = plugin.store().getPlotAt(player.getLocation());
        if (plot == null) {
            player.sendMessage(ChatColor.RED + "❌ You are not standing in your claim!");
            playEffect(flag, "fail", player, player.getLocation());
            return;
        }

        boolean current = plot.getFlag(flag, def(flag));
        plot.setFlag(flag, !current);

        String status = !current ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF";
        player.sendMessage(ChatColor.YELLOW + "⚙ " + flag.toUpperCase() + " protection is now " + status);
        playEffect(flag, "success", player, player.getLocation());
    }

    // Exposed API used by SettingsGUI — return TRUE when protection is active (global OR plot flag)
    public boolean isPvPEnabled(Player player) {
        return (!plugin.worldRules().isPvPAllowed(player.getWorld())) || hasFlag(player, "pvp");
    }
    public void togglePvP(Player player) { toggleFlag(player, "pvp"); }

    public boolean isContainersEnabled(Player player) {
        return (!plugin.worldRules().allowContainers(player.getWorld())) || hasFlag(player, "containers");
    }
    public void toggleContainers(Player player) { toggleFlag(player, "containers"); }

    public boolean isMobProtectionEnabled(Player player) {
        return (!plugin.worldRules().allowMobs(player.getWorld())) || hasFlag(player, "mobs");
    }
    public void toggleMobProtection(Player player) { toggleFlag(player, "mobs"); }

    public boolean isPetProtectionEnabled(Player player) {
        return (!plugin.worldRules().allowPets(player.getWorld())) || hasFlag(player, "pets");
    }
    public void togglePetProtection(Player player) { toggleFlag(player, "pets"); }

    public boolean isEntityProtectionEnabled(Player player) {
        // Reuse containers baseline until a dedicated world toggle exists.
        return (!plugin.worldRules().allowContainers(player.getWorld())) || hasFlag(player, "entities");
    }
    public void toggleEntityProtection(Player player) { toggleFlag(player, "entities"); }

    public boolean isFarmProtectionEnabled(Player player) {
        return (!plugin.worldRules().allowFarms(player.getWorld())) || hasFlag(player, "farm");
    }
    public void toggleFarmProtection(Player player) { toggleFlag(player, "farm"); }

    /* -----------------------------------------------------
     *  EFFECTS & HELPERS
     * ----------------------------------------------------- */

    private void playEffect(String category, String type, Player p, Location loc) {
        if (!plugin.getConfig().getBoolean("protection_effects.enabled", true)) return;

        String base = "protection_effects." + category + "." + type + "_";
        String def  = "protection_effects." + type + "_";

        String soundKey = plugin.getConfig().getString(base + "sound",
                plugin.getConfig().getString(def + "sound", "BLOCK_NOTE_BLOCK_BASS"));
        String particleKey = plugin.getConfig().getString(base + "particle",
                plugin.getConfig().getString(def + "particle", "SMOKE"));

        try {
            Sound sound = Sound.valueOf(soundKey);
            Particle particle = Particle.valueOf(particleKey);
            p.playSound(loc, sound, 1f, 1f);
            if (loc.getWorld() != null) {
                loc.getWorld().spawnParticle(particle, loc.clone().add(0.5, 1, 0.5), 10, 0.3, 0.3, 0.3, 0.05);
            }
        } catch (IllegalArgumentException ignored) {
            // Invalid sound/particle name; ignore silently.
        }
    }

    private boolean canBuild(Player p, PlotStore.Plot plot) {
        if (p.hasPermission("aegis.admin")) return true; // standardized perm
        if (p.getUniqueId().equals(plot.getOwner())) return true;
        return plot.getTrusted().contains(p.getUniqueId());
    }

    private boolean hasFlag(Player p, String flag) {
        PlotStore.Plot plot = plugin.store().getPlotAt(p.getLocation());
        boolean def = def(flag);
        return plot != null ? plot.getFlag(flag, def) : def;
    }

    private boolean isContainer(Material type) {
        // Cover common containers + all shulker variants via Tag
        return switch (type) {
            case CHEST, TRAPPED_CHEST, BARREL, FURNACE, BLAST_FURNACE,
                 SMOKER, HOPPER, DROPPER, DISPENSER -> true;
            default -> Tag.SHULKER_BOXES.isTagged(type);
        };
    }

    private boolean def(String flag) {
        // Fallback to config defaults if not inside a plot
        return plugin.getConfig().getBoolean("protection.defaults." + flag, true);
    }
}
