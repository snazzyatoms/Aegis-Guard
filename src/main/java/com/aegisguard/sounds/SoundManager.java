package com.aegisguard.sounds;

import com.aegisguard.AegisGuard;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * SoundManager
 * - Central handler for all plugin sounds
 * - Reads config.yml -> sounds: section
 * - Supports global + per-player disable
 */
public class SoundManager {

    private final AegisGuard plugin;
    private boolean enabled;
    private final Set<UUID> mutedPlayers = new HashSet<>();

    // Config values
    private String menuOpenSound;
    private float menuOpenVolume;
    private float menuOpenPitch;

    private String menuFlipSound;
    private float menuFlipVolume;
    private float menuFlipPitch;

    private String menuCloseSound;
    private float menuCloseVolume;
    private float menuClosePitch;

    private String claimMagicSound;
    private float claimMagicVolume;
    private float claimMagicPitch;

    private String unclaimSound;
    private float unclaimVolume;
    private float unclaimPitch;

    public SoundManager(AegisGuard plugin) {
        this.plugin = plugin;
        reload();
    }

    /** Reload sounds from config.yml */
    public void reload() {
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("sounds");
        if (sec == null) {
            enabled = false;
            return;
        }

        enabled = sec.getBoolean("enabled", true);

        menuOpenSound = sec.getString("menu.open.sound", "ITEM_BOOK_PAGE_TURN");
        menuOpenVolume = (float) sec.getDouble("menu.open.volume", 1.0);
        menuOpenPitch = (float) sec.getDouble("menu.open.pitch", 1.0);

        menuFlipSound = sec.getString("menu.flip.sound", "ITEM_BOOK_PAGE_TURN");
        menuFlipVolume = (float) sec.getDouble("menu.flip.volume", 1.0);
        menuFlipPitch = (float) sec.getDouble("menu.flip.pitch", 1.0);

        menuCloseSound = sec.getString("menu.close.sound", "ITEM_BOOK_PUT");
        menuCloseVolume = (float) sec.getDouble("menu.close.volume", 1.0);
        menuClosePitch = (float) sec.getDouble("menu.close.pitch", 1.0);

        claimMagicSound = sec.getString("on_claim.magic.sound", "ENTITY_PLAYER_LEVELUP");
        claimMagicVolume = (float) sec.getDouble("on_claim.magic.volume", 1.2);
        claimMagicPitch = (float) sec.getDouble("on_claim.magic.pitch", 0.8);

        unclaimSound = sec.getString("on_unclaim.sound", "BLOCK_ANVIL_BREAK");
        unclaimVolume = (float) sec.getDouble("on_unclaim.volume", 1.0);
        unclaimPitch = (float) sec.getDouble("on_unclaim.pitch", 1.0);
    }

    /* -----------------------------
     * Public API
     * ----------------------------- */

    public void playMenuOpen(Player player) {
        play(player, menuOpenSound, menuOpenVolume, menuOpenPitch);
    }

    public void playMenuFlip(Player player) {
        play(player, menuFlipSound, menuFlipVolume, menuFlipPitch);
    }

    public void playMenuClose(Player player) {
        play(player, menuCloseSound, menuCloseVolume, menuClosePitch);
    }

    public void playClaimMagic(Player player) {
        play(player, claimMagicSound, claimMagicVolume, claimMagicPitch);
    }

    public void playUnclaim(Player player) {
        play(player, unclaimSound, unclaimVolume, unclaimPitch);
    }

    /* -----------------------------
     * Core Sound Logic
     * ----------------------------- */
    private void play(Player player, String soundName, float volume, float pitch) {
        if (!enabled) return;
        if (mutedPlayers.contains(player.getUniqueId())) return;

        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid sound name in config.yml: " + soundName);
        }
    }

    /* -----------------------------
     * Admin Control
     * ----------------------------- */
    public void mutePlayer(UUID id) {
        mutedPlayers.add(id);
    }

    public void unmutePlayer(UUID id) {
        mutedPlayers.remove(id);
    }

    public boolean isMuted(UUID id) {
        return mutedPlayers.contains(id);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
