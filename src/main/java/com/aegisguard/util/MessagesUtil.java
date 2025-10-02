package com.aegisguard.util;

import com.aegisguard.AegisGuard;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * MessagesUtil
 * - Loads messages.yml
 * - Handles color codes & placeholders
 * - Safe fallbacks if keys are missing
 * - Supports both single-line and list messages
 * - Supports admin/console-only notifications
 * - Supports optional broadcast-to-all via config
 */
public class MessagesUtil {

    private final AegisGuard plugin;
    private FileConfiguration messages;
    private String prefix; // cached prefix

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
        this.prefix = color(messages.getString("prefix", "&8[&bAegisGuard&8]&r "));
    }

    /* -----------------------------
     * Get single-line message
     * ----------------------------- */
    public String get(String key) {
        String msg = messages.getString(key);
        if (msg == null) {
            return color("&cMissing message: " + key);
        }
        return color(msg);
    }

    /* -----------------------------
     * Get multi-line message (list)
     * ----------------------------- */
    public List<String> getList(String key) {
        List<String> raw = messages.getStringList(key);
        if (raw == null || raw.isEmpty()) {
            List<String> fallback = new ArrayList<>();
            fallback.add(color("&cMissing message list: " + key));
            return fallback;
        }
        List<String> out = new ArrayList<>();
        for (String line : raw) {
            out.add(color(line));
        }
        return out;
    }

    /* -----------------------------
     * Format placeholders into a string
     * ----------------------------- */
    public String format(String key, Object... replacements) {
        String msg = get(key);
        for (int i = 0; i < replacements.length - 1; i += 2) {
            msg = msg.replace("{" + replacements[i] + "}", String.valueOf(replacements[i + 1]));
        }
        // Add prefix unless it's the prefix itself
        if (!key.equalsIgnoreCase("prefix")) {
            msg = prefix + msg;
        }
        return msg;
    }

    /* -----------------------------
     * Format placeholders into a list
     * ----------------------------- */
    public List<String> formatList(String key, Object... replacements) {
        List<String> lines = getList(key);
        List<String> formatted = new ArrayList<>();
        for (String line : lines) {
            String replaced = line;
            for (int i = 0; i < replacements.length - 1; i += 2) {
                replaced = replaced.replace("{" + replacements[i] + "}", String.valueOf(replacements[i + 1]));
            }
            formatted.add(replaced);
        }
        return formatted;
    }

    /* -----------------------------
     * Send to player
     * ----------------------------- */
    public void send(Player p, String key, Object... replacements) {
        p.sendMessage(format(key, replacements));
    }

    /* -----------------------------
     * Send to admins + console only
     * OR broadcast if enabled in config
     * ----------------------------- */
    public void sendAdmin(String key, Object... replacements) {
        String msg = format(key, replacements);

        // Check config for broadcast
        boolean broadcast = plugin.getConfig().getBoolean("admin.broadcast_to_all", false);

        if (broadcast) {
            Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(msg));
            Bukkit.getConsoleSender().sendMessage(msg);
        } else {
            // Console
            Bukkit.getConsoleSender().sendMessage(msg);
            // Online admins only
            Bukkit.getOnlinePlayers().stream()
                    .filter(p -> p.hasPermission("aegisguard.admin"))
                    .forEach(p -> p.sendMessage(msg));
        }
    }

    /* -----------------------------
     * Utility: Color codes
     * ----------------------------- */
    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
