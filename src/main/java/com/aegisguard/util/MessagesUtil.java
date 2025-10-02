package com.aegisguard.util;

import com.aegisguard.AegisGuard;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.List;

/**
 * MessagesUtil
 * - Loads messages.yml
 * - Handles color codes & placeholders
 * - Safe fallbacks if keys are missing
 */
public class MessagesUtil {

    private final AegisGuard plugin;
    private FileConfiguration messages;

    public MessagesUtil(AegisGuard plugin) {
        this.plugin = plugin;
        reload();
    }

    /* -----------------------------
     * Load messages.yml
     * ----------------------------- */
    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        this.messages = YamlConfiguration.loadConfiguration(file);
    }

    /* -----------------------------
     * Get message by key
     * ----------------------------- */
    public String get(String key) {
        String msg = messages.getString(key);
        if (msg == null) {
            return color("&cMissing message: " + key);
        }
        return color(msg);
    }

    /* -----------------------------
     * Format message with placeholders
     * ----------------------------- */
    public String format(String key, Object... replacements) {
        String msg = get(key);
        for (int i = 0; i < replacements.length - 1; i += 2) {
            msg = msg.replace("{" + replacements[i] + "}", String.valueOf(replacements[i + 1]));
        }
        // Add prefix automatically (unless this IS the prefix key)
        if (!key.equalsIgnoreCase("prefix")) {
            msg = get("prefix") + msg;
        }
        return msg;
    }

    /* -----------------------------
     * Send message to a player
     * ----------------------------- */
    public void send(Player p, String key, Object... replacements) {
        p.sendMessage(format(key, replacements));
    }

    /* -----------------------------
     * Utility: Color codes
     * ----------------------------- */
    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
