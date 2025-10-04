package com.aegisguard.util;

import com.aegisguard.AegisGuard;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * ==============================================================
 * MessagesUtil
 * --------------------------------------------------------------
 *  - Handles all message lookups and color formatting
 *  - Supports per-player tone selection:
 *      🏰 old_english  (default)
 *      📜 hybrid_english
 *      💬 modern_english
 *  - Reloads dynamically without restart
 *  - Caches language choices for instant updates
 * ==============================================================
 */
public class MessagesUtil {

    private final AegisGuard plugin;
    private FileConfiguration messages;
    private final Map<UUID, String> playerStyles = new HashMap<>(); // Player UUID → style
    private String defaultStyle;

    public MessagesUtil(AegisGuard plugin) {
        this.plugin = plugin;
        reload();
    }

    /* -----------------------------
     * Reload from file
     * ----------------------------- */
    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) plugin.saveResource("messages.yml", false);
        this.messages = YamlConfiguration.loadConfiguration(file);

        this.defaultStyle = messages.getString("language_styles.default", "old_english");

        plugin.getLogger().info("[AegisGuard] Messages loaded. Default style: " + defaultStyle);
    }

    /* -----------------------------
     * Public Accessors
     * ----------------------------- */

    /**
     * Retrieves a formatted message for a player
     */
    public String get(Player player, String key) {
        String style = playerStyles.getOrDefault(player.getUniqueId(), defaultStyle);
        String path = style + "." + key;

        if (!messages.contains(path)) {
            // fallback to default
            path = defaultStyle + "." + key;
        }

        String msg = messages.getString(path, "&c[Missing message: " + key + "]");
        return format(msg);
    }

    /**
     * Retrieves a message with fallback for non-player senders
     */
    public String get(String key) {
        String path = defaultStyle + "." + key;
        String msg = messages.getString(path, "&c[Missing message: " + key + "]");
        return format(msg);
    }

    /**
     * Sends a prefixed message to sender
     */
    public void send(CommandSender sender, String key) {
        String prefix = messages.getString("prefix", "&8[&bAegisGuard&8]&r ");
        String msg = (sender instanceof Player p)
                ? get(p, key)
                : get(key);
        sender.sendMessage(format(prefix + msg));
    }

    /**
     * Sends a message with placeholders replaced
     */
    public void send(CommandSender sender, String key, Map<String, String> placeholders) {
        String msg = (sender instanceof Player p)
                ? get(p, key)
                : get(key);

        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            msg = msg.replace("{" + entry.getKey() + "}", entry.getValue());
        }

        String prefix = messages.getString("prefix", "&8[&bAegisGuard&8]&r ");
        sender.sendMessage(format(prefix + msg));
    }

    /* -----------------------------
     * Player Language Management
     * ----------------------------- */

    public void setPlayerStyle(Player player, String style) {
        List<String> valid = messages.getStringList("language_styles.available");
        if (!valid.contains(style)) {
            player.sendMessage(ChatColor.RED + "⚠ Invalid language style.");
            return;
        }

        playerStyles.put(player.getUniqueId(), style);
        savePlayerPreference(player, style);

        player.sendMessage(ChatColor.GOLD + "🕮 Your message style is now set to: " +
                ChatColor.AQUA + style.replace("_", " "));
    }

    public String getPlayerStyle(Player player) {
        return playerStyles.getOrDefault(player.getUniqueId(), defaultStyle);
    }

    public void loadPlayerPreferences() {
        File file = new File(plugin.getDataFolder(), "playerdata.yml");
        if (!file.exists()) return;

        FileConfiguration data = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = data.getConfigurationSection("players");
        if (section == null) return;

        for (String uuidStr : section.getKeys(false)) {
            UUID uuid = UUID.fromString(uuidStr);
            String style = section.getString(uuidStr + ".language_style", defaultStyle);
            playerStyles.put(uuid, style);
        }

        plugin.getLogger().info("[AegisGuard] Loaded " + playerStyles.size() + " player language preferences.");
    }

    private void savePlayerPreference(Player player, String style) {
        File file = new File(plugin.getDataFolder(), "playerdata.yml");
        FileConfiguration data = YamlConfiguration.loadConfiguration(file);

        data.set("players." + player.getUniqueId() + ".language_style", style);

        try {
            data.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /* -----------------------------
     * Formatting
     * ----------------------------- */
    private String format(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg == null ? "" : msg);
    }
}
