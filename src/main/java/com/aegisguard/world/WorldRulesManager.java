package com.aegisguard.world;

import com.aegisguard.AegisGuard;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;

/**
 * ==============================================================
 * WorldRulesManager
 * --------------------------------------------------------------
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
 *       protections:
 *         pvp: false
 *         mobs: false
 *         containers: true
 *         pets: true
 *         farms: true
 *     world_nether:
 *       allow_claims: false
 *       protections:
 *         pvp: true
 *         mobs: true
 *     world_the_end:
 *       allow_claims: false
 *       protections:
 *         pvp: false
 *         mobs: true
 * ==============================================================
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
    public void reload() {
        plugin.reloadConfig();
        load();
    }

    /* -----------------------------
     * Load world-specific rules
     * ----------------------------- */
    public void load() {
        rules.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("claims.per_world");
        if (section == null) {
            plugin.getLogger().warning("[AegisGuard] No per-world configuration found. Using defaults.");
            return;
        }

        for (String worldName : section.getKeys(false)) {
            ConfigurationSection worldSec = section.getConfigurationSection(worldName);
            if (worldSec == null) continue;

            boolean allowClaims = worldSec.getBoolean("allow_claims", true);

            // Nested structure support (protections: ...)
            ConfigurationSection prot = worldSec.getConfigurationSection("protections");
            if (prot == null) prot = worldSec;

            WorldRuleSet set = new WorldRuleSet(
                    allowClaims,
                    prot.getBoolean("pvp", false),
                    prot.getBoolean("mobs", false),
                    prot.getBoolean("containers", true),
                    prot.getBoolean("pets", true),
                    prot.getBoolean("farms", true)
            );

            rules.put(worldName, set);

            plugin.getLogger().info(String.format(
                    "[AegisGuard] Loaded world rules for '%s': claims=%s, pvp=%s, mobs=%s, containers=%s, pets=%s, farms=%s",
                    worldName, allowClaims, set.pvp, set.mobs, set.containers, set.pets, set.farms
            ));
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

    /**
     * Generic protection lookup for dynamic checks (used by ProtectionManager)
     */
    public boolean isProtectionEnabled(World world, String key) {
        WorldRuleSet r = getRules(world);
        return switch (key.toLowerCase()) {
            case "pvp", "pvp_protection" -> r.pvp;
            case "mobs", "mobs_protection" -> r.mobs;
            case "containers", "container_protection" -> r.containers;
            case "pets", "pets_protection" -> r.pets;
            case "farms", "farm_protection" -> r.farms;
            default -> true;
        };
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
