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
 * MessagesUtil (AegisGuard v1.0)
 * - Loads messages.yml (multi-tone English)
 * - Per-player style prefs: old_english (default), hybrid_english, modern_english
 * - Persists to playerdata.yml
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
     * Accessors (Player-aware)
     * ----------------------------- */
    /** Get message for specific player style */
    public String get(Player player, String key) {
        String raw = getRawForPlayer(player, key);
        return format(raw);
    }

    /** Get message for player with placeholders (varargs: "KEY","VAL","KEY2","VAL2",...) */
    public String get(Player player, String key, String... kv) {
        String raw = getRawForPlayer(player, key);
        raw = applyPlaceholders(raw, kv);
        return format(raw);
    }

    /** Get list (GUI lore) for player style */
    public List<String> getList(Player player, String key) {
        String style = playerStyles.getOrDefault(player.getUniqueId(), defaultStyle);
        String path = style + "." + key;
        List<String> list = messages.getStringList(path);
        if (list.isEmpty()) list = messages.getStringList(defaultStyle + "." + key);
        if (list == null) list = Collections.emptyList();
        List<String> colored = new ArrayList<>(list.size());
        for (String line : list) colored.add(format(line));
        return colored;
    }

    /* -----------------------------
     * Accessors (default style)
     * ----------------------------- */
    public String get(String key) {
        String raw = messages.getString(defaultStyle + "." + key, "&c[Missing: " + key + "]");
        return format(raw);
    }

    public String get(String key, String... kv) {
        String raw = messages.getString(defaultStyle + "." + key, "&c[Missing: " + key + "]");
        raw = applyPlaceholders(raw, kv);
        return format(raw);
    }

    public String get(String key, Map<String, String> placeholders) {
        String raw = messages.getString(defaultStyle + "." + key, "&c[Missing: " + key + "]");
        raw = applyPlaceholders(raw, placeholders);
        return format(raw);
    }

    /** Non-player list accessor (default style) */
    public List<String> getList(String key) {
        List<String> list = messages.getStringList(defaultStyle + "." + key);
        if (list == null) list = Collections.emptyList();
        List<String> colored = new ArrayList<>(list.size());
        for (String line : list) colored.add(format(line));
        return colored;
    }

    /** Does the styled message exist? */
    public boolean has(String key) {
        return messages.contains(defaultStyle + "." + key);
    }

    /** Colorize a string with & codes */
    public String color(String text) {
        return format(text);
    }

    /** Prefix (top-level, not style-scoped) */
    public String prefix() {
        return format(messages.getString("prefix", "&8[&bAegisGuard&8]&r "));
    }

    /* -----------------------------
     * Senders
     * ----------------------------- */
    public void send(CommandSender sender, String key) {
        String msg = (sender instanceof Player p) ? get(p, key) : get(key);
        sender.sendMessage(prefix() + msg);
    }

    public void send(CommandSender sender, String key, Map<String, String> placeholders) {
        String msg = (sender instanceof Player p) ? get(p, key) : get(key);
        msg = applyPlaceholders(msg, placeholders); // msg is already colored; placeholders are plain
        sender.sendMessage(prefix() + msg);
    }

    public void send(CommandSender sender, String key, String... kv) {
        String msg = (sender instanceof Player p) ? get(p, key, kv) : get(key, kv);
        sender.sendMessage(prefix() + msg);
    }

    /* -----------------------------
     * Player Style System
     * ----------------------------- */
    public void setPlayerStyle(Player player, String style) {
        List<String> valid = messages.getStringList("language_styles.available");
        if (valid == null || !valid.contains(style)) {
            player.sendMessage(ChatColor.RED + "⚠ Invalid language style: " + style);
            return;
        }
        playerStyles.put(player.getUniqueId(), style);
        savePlayerPreference(player, style);
        player.sendMessage(ChatColor.GOLD + "🕮 Your speech style is now: "
                + ChatColor.AQUA + style.replace("_", " "));
        // Live GUI refresh (optional)
        if (player.getOpenInventory() != null &&
            player.getOpenInventory().getTitle() != null &&
            player.getOpenInventory().getTitle().toLowerCase().contains("aegisguard")) {
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
            try { playerDataFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        playerData = YamlConfiguration.loadConfiguration(playerDataFile);

        ConfigurationSection section = playerData.getConfigurationSection("players");
        if (section == null) return;

        for (String uuidStr : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                String style = section.getString(uuidStr + ".language_style", defaultStyle);
                playerStyles.put(uuid, style);
            } catch (IllegalArgumentException ignored) {}
        }
        plugin.getLogger().info("[AegisGuard] Loaded " + playerStyles.size() + " player language preferences.");
    }

    private void savePlayerPreference(Player player, String style) {
        if (playerDataFile == null) {
            playerDataFile = new File(plugin.getDataFolder(), "playerdata.yml");
            playerData = YamlConfiguration.loadConfiguration(playerDataFile);
        }
        playerData.set("players." + player.getUniqueId() + ".language_style", style);
        try { playerData.save(playerDataFile); } catch (IOException e) { e.printStackTrace(); }
    }

    /* -----------------------------
     * Auto-load on join
     * ----------------------------- */
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        playerStyles.putIfAbsent(player.getUniqueId(), defaultStyle);
    }

    /* -----------------------------
     * Internal helpers
     * ----------------------------- */
    private String getRawForPlayer(Player player, String key) {
        String style = playerStyles.getOrDefault(player.getUniqueId(), defaultStyle);
        String path = style + "." + key;
        if (!messages.contains(path)) path = defaultStyle + "." + key;
        return messages.getString(path, "&c[Missing: " + key + "]");
    }

    private String format(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg == null ? "" : msg);
    }

    private String applyPlaceholders(String msg, String... kv) {
        if (msg == null || kv == null || kv.length == 0) return msg;
        for (int i = 0; i + 1 < kv.length; i += 2) {
            String k = kv[i] == null ? "" : kv[i];
            String v = kv[i + 1] == null ? "" : kv[i + 1];
            msg = msg.replace("{" + k + "}", v);
            msg = msg.replace("%" + k + "%", v); // support %KEY% too
        }
        return msg;
    }

    private String applyPlaceholders(String msg, Map<String, String> map) {
        if (msg == null || map == null || map.isEmpty()) return msg;
        for (Map.Entry<String, String> e : map.entrySet()) {
            String k = e.getKey();
            String v = e.getValue() == null ? "" : e.getValue();
            msg = msg.replace("{" + k + "}", v);
            msg = msg.replace("%" + k + "%", v);
        }
        return msg;
    }
}
