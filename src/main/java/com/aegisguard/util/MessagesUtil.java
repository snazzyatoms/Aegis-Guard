package com.aegisguard.util;

import com.aegisguard.AegisGuard;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class MessagesUtil {

    private final AegisGuard plugin;
    private FileConfiguration messages;
    private String style; // "old_english" or "modern"

    public MessagesUtil(AegisGuard plugin) {
        this.plugin = plugin;
        reload();
    }

    /* -----------------------------
     * Reload configuration
     * ----------------------------- */
    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) plugin.saveResource("messages.yml", false);
        this.messages = YamlConfiguration.loadConfiguration(file);
        this.style = plugin.getConfig().getString("messages.language_style", "modern").toLowerCase();
    }

    /* -----------------------------
     * Core Message Fetching
     * ----------------------------- */
    public String get(String key) {
        String val = messages.getString(key);
        return val != null ? color(val) : "§c[Missing: " + key + "]";
    }

    public List<String> getList(String key) {
        List<String> list = messages.getStringList(key);
        if (list == null || list.isEmpty()) return Collections.singletonList("§c[Missing: " + key + "]");
        List<String> colored = new ArrayList<>();
        for (String line : list) colored.add(color(line));
        return colored;
    }

    /* -----------------------------
     * Expansion Request Messages
     * ----------------------------- */
    public String expansion(String key, Map<String, String> placeholders) {
        String section = style.equals("old_english") ? "expansion_messages_old_english." : "expansion_messages_modern.";
        String msg = messages.getString(section + key, "§c[Missing expansion message: " + key + "]");
        if (msg == null) return "";
        for (Map.Entry<String, String> e : placeholders.entrySet())
            msg = msg.replace("{" + e.getKey() + "}", e.getValue());
        return color(msg);
    }

    /* -----------------------------
     * On-the-Fly Style Switching
     * ----------------------------- */
    public void toggleStyle() {
        if (!plugin.getConfig().getBoolean("messages.allow_runtime_switch", false)) {
            return;
        }
        this.style = this.style.equals("old_english") ? "modern" : "old_english";
        plugin.getConfig().set("messages.language_style", this.style);
        plugin.saveConfig();
        plugin.getLogger().info("[AegisGuard] Language style switched to: " + this.style);
    }

    /* -----------------------------
     * Utility Helpers
     * ----------------------------- */
    public void send(CommandSender target, String key) {
        String msg = get(key);
        if (msg != null && !msg.isEmpty()) {
            target.sendMessage(color(plugin.getConfig().getString("prefix", "") + msg));
        }
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
