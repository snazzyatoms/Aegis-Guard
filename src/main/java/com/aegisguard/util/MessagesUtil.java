package com.aegisguard.util;

import com.aegisguard.AegisGuard;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * MessagesUtil
 * - Loads messages.yml
 * - Handles color codes & placeholders
 * - Safe fallbacks
 */
public class MessagesUtil {

    private final AegisGuard plugin;
    private FileConfiguration messages;

    public MessagesUtil(AegisGuard plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.saveResource("messages.yml", false);
        this.messages = plugin.getConfig(); // we’ll load messages.yml separately
        this.messages = plugin.getConfig(); // stub until we separate messages.yml
    }

    public String get(String key) {
        return color(messages.getString(key, "&cMissing message: " + key));
    }

    public String format(String key, Object... replacements) {
        String msg = get(key);
        for (int i = 0; i < replacements.length - 1; i += 2) {
            msg = msg.replace("{" + replacements[i] + "}", String.valueOf(replacements[i + 1]));
        }
        return msg;
    }

    public void send(Player p, String key, Object... replacements) {
        p.sendMessage(format(key, replacements));
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
