package com.aegisguard.util;

import com.aegisguard.AegisGuard;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * SoundUtil (AegisGuard)
 * ------------------------------------------------------------
 * Centralized sound helper with config + per-player toggles.
 *
 * Config keys (with sensible defaults if missing):
 *
 * sounds:
 *   global_enabled: true
 *   menu:
 *     open: UI_BUTTON_CLICK
 *     flip: UI_BUTTON_CLICK
 *     close: UI_BUTTON_CLICK
 *   claim:
 *     success: ENTITY_PLAYER_LEVELUP
 *     unclaim: BLOCK_AMETHYST_BLOCK_CHIME
 *   notify:
 *     ok: ENTITY_EXPERIENCE_ORB_PICKUP
 *     error: BLOCK_NOTE_BLOCK_BASS
 *
 * Per-player overrides:
 *   sounds.players.<uuid>: true|false
 *
 * NOTE:
 *  - This class only plays sounds if both the global flag AND the player's
 *    personal preference (AegisGuard#isSoundEnabled) are enabled.
 *  - All enum lookups are safe; invalid names fall back to defaults.
 */
public class SoundUtil {

    private final AegisGuard plugin;

    public SoundUtil(AegisGuard plugin) {
        this.plugin = plugin;
    }

    /* ------------------------------------------------------------
     * Public helpers used around the plugin
     * ------------------------------------------------------------ */

    public void playMenuOpen(Player p) {
        playKey(p, "sounds.menu.open", "UI_BUTTON_CLICK", 1f, 1f);
    }

    public void playMenuFlip(Player p) {
        playKey(p, "sounds.menu.flip", "UI_BUTTON_CLICK", 1f, 1.1f);
    }

    public void playMenuClose(Player p) {
        playKey(p, "sounds.menu.close", "UI_BUTTON_CLICK", 1f, 0.9f);
    }

    public void playClaimMagic(Player p) {
        // pleasant "success" flourish
        playKey(p, "sounds.claim.success", "ENTITY_PLAYER_LEVELUP", 1f, 1.0f);
    }

    public void playUnclaim(Player p) {
        playKey(p, "sounds.claim.unclaim", "BLOCK_AMETHYST_BLOCK_CHIME", 1f, 1.0f);
    }

    public void playOk(Player p) {
        playKey(p, "sounds.notify.ok", "ENTITY_EXPERIENCE_ORB_PICKUP", 1f, 1.0f);
    }

    public void playError(Player p) {
        playKey(p, "sounds.notify.error", "BLOCK_NOTE_BLOCK_BASS", 1f, 1.0f);
    }

    /* ------------------------------------------------------------
     * Low-level helpers
     * ------------------------------------------------------------ */

    /**
     * Plays a configured sound to a player if sounds are enabled for them.
     */
    public void playKey(Player p, String configPath, String defEnumName, float volume, float pitch) {
        if (!shouldPlay(p)) return;

        String name = plugin.getConfig().getString(configPath, defEnumName);
        Sound sound = safeSound(name, defEnumName);
        try {
            p.playSound(p.getLocation(), sound, volume, pitch);
        } catch (Throwable ignored) {
            // Guard against rare NPEs or API quirks
        }
    }

    /**
     * Plays a sound at a specific location (rarely needed, but handy).
     */
    public void playAt(Player p, Location loc, String configPath, String defEnumName, float volume, float pitch) {
        if (!shouldPlay(p) || loc == null) return;

        String name = plugin.getConfig().getString(configPath, defEnumName);
        Sound sound = safeSound(name, defEnumName);
        try {
            loc.getWorld().playSound(loc, sound, volume, pitch);
        } catch (Throwable ignored) {
        }
    }

    /**
     * Determines whether we should emit any sound for the player.
     */
    public boolean shouldPlay(Player p) {
        if (p == null) return false;
        // Global flag already defaults to true in AegisGuard#isSoundEnabled
        return plugin.isSoundEnabled(p);
    }

    /**
     * Converts a config string to a Sound enum safely.
     * Falls back to the default if the provided name is invalid.
     */
    private Sound safeSound(String candidate, String defEnumName) {
        Sound def = Sound.UI_BUTTON_CLICK;
        try {
            def = Sound.valueOf(defEnumName);
        } catch (IllegalArgumentException ignored) {
            // Keep the hardcoded default defined above if defEnumName is invalid
        }

        if (candidate == null || candidate.isEmpty()) {
            return def;
        }
        try {
            return Sound.valueOf(candidate.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return def;
        }
    }
}
