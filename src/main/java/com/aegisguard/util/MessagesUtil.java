package com.aegisguard.util;

import com.aegisguard.AegisGuard;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * ==============================================================
 * MessagesUtil (AegisGuard v1.0)
 * --------------------------------------------------------------
 *  - Loads messages.yml (supports multi-tone English)
 *  - Handles per-player style preferences:
 *       🏰 old_english  (Default)
 *       📜 hybrid_english
 *       💬 modern_english
 *  - Instant GUI & chat update (no restart needed)
 *  - Persists preferences via playerdata.yml
 * ==============================================================
 */
public class MessagesUtil implements Listener {

    private final AegisGuard plugin;
    private FileConfiguration messages;
    private final Map<UUID, String> playerStyles = new HashMap<>();
    private String defaultStyle;

    private File playerDataFile;
    private FileConfiguration playerData;

    public MessagesUtil(AegisGuard plugin) {
        this.plugin = plugin;
        reload();
        loadPlayerPreferences();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
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
     * Accessors
     * ----------------------------- */

    /** Get message for specific player style */
    public String get(Player player, String key) {
        String style = playerStyles.getOrDefault(player.getUniqueId(), defaultStyle);
        String path = style + "." + key;

        if (!messages.contains(path)) {
            path = defaultStyle + "." + key; // fallback
        }

        String msg = messages.getString(path, "&c[Missing: " + key + "]");
        return format(msg);
    }

    /** Get message using default style */
    public String get(String key) {
        String path = defaultStyle + "." + key;
        String msg = messages.getString(path, "&c[Missing: " + key + "]");
        return format(msg);
    }

    /** Get list of strings (for GUI lore) */
    public List<String> getList(Player player, String key) {
        String style = playerStyles.getOrDefault(player.getUniqueId(), defaultStyle);
        String path = style + "." + key;
        List<String> list = messages.getStringList(path);
        if (list.isEmpty()) list = messages.getStringList(defaultStyle + "." + key);
        List<String> colored = new ArrayList<>();
        for (String line : list) colored.add(format(line));
        return colored;
    }

    /* -----------------------------
     * Senders
     * ----------------------------- */
    public void send(CommandSender sender, String key) {
        String prefix = messages.getString("prefix", "&8[&bAegisGuard&8]&r ");
        String msg = (sender instanceof Player p)
                ? get(p, key)
                : get(key);
        sender.sendMessage(format(prefix + msg));
    }

    public void send(CommandSender sender, String key, Map<String, String> placeholders) {
        String prefix = messages.getString("prefix", "&8[&bAegisGuard&8]&r ");
        String msg = (sender instanceof Player p)
                ? get(p, key)
                : get(key);

        for (Map.Entry<String, String> e : placeholders.entrySet()) {
            msg = msg.replace("{" + e.getKey() + "}", e.getValue());
        }
        sender.sendMessage(format(prefix + msg));
    }

    /* -----------------------------
     * Player Style System
     * ----------------------------- */

    public void setPlayerStyle(Player player, String style) {
        List<String> valid = messages.getStringList("language_styles.available");
        if (!valid.contains(style)) {
            player.sendMessage(ChatColor.RED + "⚠ Invalid language style: " + style);
            return;
        }

        playerStyles.put(player.getUniqueId(), style);
        savePlayerPreference(player, style);

        player.sendMessage(ChatColor.GOLD + "🕮 Your speech style is now: "
                + ChatColor.AQUA + style.replace("_", " "));

        // Live GUI refresh if open
        if (player.getOpenInventory() != null &&
            player.getOpenInventory().getTitle().contains("Codex")) {
            plugin.gui().settings().open(player);
        }
    }

    public String getPlayerStyle(Player player) {
        return playerStyles.getOrDefault(player.getUniqueId(), defaultStyle);
    }

    /* -----------------------------
     * Player Preferences
     * ----------------------------- */

    public void loadPlayerPreferences() {
        playerDataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        if (!playerDataFile.exists()) {
            try {
                playerDataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        playerData = YamlConfiguration.loadConfiguration(playerDataFile);

        ConfigurationSection section = playerData.getConfigurationSection("players");
        if (section == null) return;

        for (String uuidStr : section.getKeys(false)) {
            UUID uuid = UUID.fromString(uuidStr);
            String style = section.getString(uuidStr + ".language_style", defaultStyle);
            playerStyles.put(uuid, style);
        }

        plugin.getLogger().info("[AegisGuard] Loaded " + playerStyles.size() + " player language preferences.");
    }

    private void savePlayerPreference(Player player, String style) {
        if (playerDataFile == null) {
            playerDataFile = new File(plugin.getDataFolder(), "playerdata.yml");
            playerData = YamlConfiguration.loadConfiguration(playerDataFile);
        }

        playerData.set("players." + player.getUniqueId() + ".language_style", style);

        try {
            playerData.save(playerDataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /* -----------------------------
     * Auto-load on join
     * ----------------------------- */
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        if (!playerStyles.containsKey(player.getUniqueId())) {
            playerStyles.put(player.getUniqueId(), defaultStyle);
        }
    }

    /* -----------------------------
     * Utility
     * ----------------------------- */
    private String format(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg == null ? "" : msg);
    }
}
