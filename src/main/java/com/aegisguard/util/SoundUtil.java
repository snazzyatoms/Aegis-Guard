package com.aegisguard.util;

import com.aegisguard.AegisGuard;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class SoundUtil {

    private final AegisGuard plugin;

    public SoundUtil(AegisGuard plugin) {
        this.plugin = plugin;
    }

    /* -----------------------------
     * Helpers
     * ----------------------------- */
    private boolean enabled(String path) {
        return plugin.getConfig().getBoolean("sounds." + path + ".enabled", true);
    }

    private Sound getSound(String path, String def) {
        String raw = plugin.getConfig().getString("sounds." + path + ".sound", def);
        try {
            return Sound.valueOf(raw.toUpperCase());
        } catch (Exception e) {
            return Sound.valueOf(def); // fallback if invalid
        }
    }

    private float getPitch(String path, float def) {
        return (float) plugin.getConfig().getDouble("sounds." + path + ".pitch", def);
    }

    private float getVolume(String path, float def) {
        return (float) plugin.getConfig().getDouble("sounds." + path + ".volume", def);
    }

    private boolean playerEnabled(Player p) {
        if (!plugin.getConfig().getBoolean("sounds.per_player", false)) return true;
        return plugin.getPlayerData().getBoolean("players." + p.getUniqueId() + ".sounds", true);
    }

    private void play(Player p, String path, String def, float defPitch, float defVol) {
        if (!enabled(path) || !playerEnabled(p)) return;
        Sound s = getSound(path, def);
        float pitch = getPitch(path, defPitch);
        float vol = getVolume(path, defVol);
        p.playSound(p.getLocation(), s, vol, pitch);
    }

    /* -----------------------------
     * Menu Sounds
     * ----------------------------- */
    public void playMenuOpen(Player p) {
        play(p, "menus.open", "ENTITY_ILLUSIONER_CAST_SPELL", 1f, 1f);
    }

    public void playMenuFlip(Player p) {
        play(p, "menus.flip", "ITEM_BOOK_PAGE_TURN", 1f, 1f);
    }

    public void playMenuClose(Player p) {
        play(p, "menus.close", "ITEM_BOOK_PUT", 1f, 1f);
    }

    /* -----------------------------
     * Claim / Unclaim
     * ----------------------------- */
    public void playClaim(Player p) {
        play(p, "claim", "ENTITY_EVOKER_PREPARE_SUMMON", 1f, 1f);
        play(p, "claim.extra", "ENTITY_LIGHTNING_BOLT_THUNDER", 1f, 0.8f);
    }

    public void playUnclaim(Player p) {
        play(p, "unclaim", "ENTITY_WITHER_SPAWN", 0.8f, 0.7f);
        play(p, "unclaim.extra", "ENTITY_ZOMBIE_VILLAGER_CURE", 1f, 1f);
    }
}
