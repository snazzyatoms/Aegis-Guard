package com.aegisguard.world;

import com.aegisguard.AegisGuard;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;

/**
 * WorldRulesManager
 * ---------------------------
 * Handles per-world configuration for AegisGuard.
 * Allows each world to define unique claim and protection behavior:
 *   - PvP toggle
 *   - Mob spawning
 *   - Claiming availability
 *   - Container, pet, farm protection defaults
 *
 * Config format example:
 *
 * claims:
 *   per_world:
 *     world:
 *       allow_claims: true
 *       pvp: false
 *       mobs: false
 *       containers: true
 *       pets: true
 *       farms: true
 *     world_nether:
 *       allow_claims: false
 *       pvp: true
 *       mobs: true
 *     world_the_end:
 *       allow_claims: false
 *       pvp: false
 *       mobs: true
 */
public class WorldRulesManager {

    private final AegisGuard plugin;
    private final Map<String, WorldRuleSet> rules = new HashMap<>();

    public WorldRulesManager(AegisGuard plugin) {
        this.plugin = plugin;
        load();
    }

    /* -----------------------------
     * Reload configuration
     * ----------------------------- */
    public void load() {
        rules.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("claims.per_world");
        if (section == null) return;

        for (String worldName : section.getKeys(false)) {
            ConfigurationSection w = section.getConfigurationSection(worldName);
            if (w == null) continue;

            WorldRuleSet set = new WorldRuleSet(
                    w.getBoolean("allow_claims", true),
                    w.getBoolean("pvp", false),
                    w.getBoolean("mobs", false),
                    w.getBoolean("containers", true),
                    w.getBoolean("pets", true),
                    w.getBoolean("farms", true)
            );
            rules.put(worldName, set);
        }

        plugin.getLogger().info("[AegisGuard] Loaded " + rules.size() + " per-world rule sets.");
    }

    /* -----------------------------
     * Accessors
     * ----------------------------- */
    private WorldRuleSet getRules(World world) {
        return rules.getOrDefault(world.getName(), WorldRuleSet.defaultRules());
    }

    public boolean allowClaims(World world) {
        return getRules(world).allowClaims;
    }

    public boolean isPvPAllowed(World world) {
        return getRules(world).pvp;
    }

    public boolean allowMobs(World world) {
        return getRules(world).mobs;
    }

    public boolean allowContainers(World world) {
        return getRules(world).containers;
    }

    public boolean allowPets(World world) {
        return getRules(world).pets;
    }

    public boolean allowFarms(World world) {
        return getRules(world).farms;
    }

    /* -----------------------------
     * Inner Class: WorldRuleSet
     * ----------------------------- */
    public static class WorldRuleSet {
        public final boolean allowClaims;
        public final boolean pvp;
        public final boolean mobs;
        public final boolean containers;
        public final boolean pets;
        public final boolean farms;

        public WorldRuleSet(boolean allowClaims, boolean pvp, boolean mobs,
                            boolean containers, boolean pets, boolean farms) {
            this.allowClaims = allowClaims;
            this.pvp = pvp;
            this.mobs = mobs;
            this.containers = containers;
            this.pets = pets;
            this.farms = farms;
        }

        public static WorldRuleSet defaultRules() {
            return new WorldRuleSet(true, false, false, true, true, true);
        }
    }
}
