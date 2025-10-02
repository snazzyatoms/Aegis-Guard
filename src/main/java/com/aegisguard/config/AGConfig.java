package com.aegisguard.config;

import com.aegisguard.AegisGuard;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

/**
 * AGConfig
 * - Wraps config.yml access
 * - Provides safe lookups with defaults
 * - Syncs new features (limits, admin options, sounds, protections)
 */
public class AGConfig {

    private final AegisGuard plugin;
    private FileConfiguration cfg;

    public AGConfig(AegisGuard plugin) {
        this.plugin = plugin;
        reload();
    }

    /* -----------------------------
     * Lifecycle
     * ----------------------------- */
    public void reload() {
        plugin.reloadConfig();
        this.cfg = plugin.getConfig();
    }

    /* -----------------------------
     * Claiming & Limits
     * ----------------------------- */
    public int getMaxClaimsPerPlayer() {
        return cfg.getInt("claims.max_claims_per_player", 1);
    }

    public int getMinRadius() {
        return cfg.getInt("limits.min_radius", 1);
    }

    public int getMaxRadius() {
        return cfg.getInt("limits.max_radius", 32);
    }

    public int getMaxArea() {
        return cfg.getInt("limits.max_area", 16000);
    }

    public int getPreviewSeconds() {
        return cfg.getInt("limits.preview_seconds", 10);
    }

    /* -----------------------------
     * Economy
     * ----------------------------- */
    public boolean useVault() {
        return cfg.getBoolean("use_vault", true);
    }

    public double getClaimCost() {
        return cfg.getDouble("claim_cost", 100.0);
    }

    public String getItemCostType() {
        return cfg.getString("item_cost.type", "DIAMOND");
    }

    public int getItemCostAmount() {
        return cfg.getInt("item_cost.amount", 5);
    }

    public boolean refundOnUnclaim() {
        return cfg.getBoolean("refund_on_unclaim", false);
    }

    public int getRefundPercent() {
        return cfg.getInt("refund_percent", 0);
    }

    /* -----------------------------
     * Effects & Visuals
     * ----------------------------- */
    public boolean lightningOnClaim() {
        return cfg.getBoolean("effects.on_claim.lightning_visual", true);
    }

    public String getClaimParticle() {
        return cfg.getString("effects.on_claim.particle", "TOTEM");
    }

    /* -----------------------------
     * Protections
     * ----------------------------- */
    public boolean noMobsInClaims() {
        return cfg.getBoolean("protections.no_mobs_in_claims", true);
    }

    public boolean pvpProtectionDefault() {
        return cfg.getBoolean("protections.pvp_protection", true);
    }

    public boolean containerProtectionDefault() {
        return cfg.getBoolean("protections.container_protection", true);
    }

    /* -----------------------------
     * Sounds
     * ----------------------------- */
    public boolean globalSoundsEnabled() {
        return cfg.getBoolean("sounds.global_enabled", true);
    }

    /* -----------------------------
     * Admin Options
     * ----------------------------- */
    public boolean autoRemoveBannedPlots() {
        return cfg.getBoolean("admin.auto_remove_banned_plots", false);
    }

    public boolean adminBypassClaimLimit() {
        return cfg.getBoolean("admin.bypass_claim_limit", false);
    }

    public boolean broadcastAdminActions() {
        return cfg.getBoolean("admin.broadcast_admin_actions", false);
    }

    /* -----------------------------
     * Helpers
     * ----------------------------- */
    public FileConfiguration raw() {
        return cfg;
    }

    public void save() {
        plugin.saveConfig();
    }
}
