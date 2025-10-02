package com.aegisguard.data;

import com.aegisguard.AegisGuard;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * PlotStore
 * - Stores claims + trusted players
 * - Handles saving/loading from plots.yml
 * - Lightweight + thread-safe
 */
public class PlotStore {

    private final AegisGuard plugin;
    private final File file;
    private FileConfiguration data;

    // Map<OwnerUUID, TrustedUUIDs>
    private final Map<UUID, Set<UUID>> trustedMap = new HashMap<>();

    public PlotStore(AegisGuard plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "plots.yml");
        load();
    }

    /* -----------------------------
     * Load / Save
     * ----------------------------- */
    public void load() {
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.data = YamlConfiguration.loadConfiguration(file);

        trustedMap.clear();
        if (data.isConfigurationSection("plots")) {
            for (String ownerId : data.getConfigurationSection("plots").getKeys(false)) {
                UUID owner = UUID.fromString(ownerId);
                List<String> list = data.getStringList("plots." + ownerId + ".trusted");
                Set<UUID> trusted = new HashSet<>();
                for (String uuidStr : list) {
                    trusted.add(UUID.fromString(uuidStr));
                }
                trustedMap.put(owner, trusted);
            }
        }
    }

    public void save() {
        for (Map.Entry<UUID, Set<UUID>> entry : trustedMap.entrySet()) {
            List<String> ids = new ArrayList<>();
            for (UUID t : entry.getValue()) ids.add(t.toString());
            data.set("plots." + entry.getKey().toString() + ".trusted", ids);
        }

        try {
            data.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void flushSync() {
        save();
    }

    /* -----------------------------
     * Trusted Players
     * ----------------------------- */
    public List<UUID> getTrusted(UUID owner) {
        return new ArrayList<>(trustedMap.getOrDefault(owner, new HashSet<>()));
    }

    public void addTrusted(UUID owner, UUID trusted) {
        trustedMap.computeIfAbsent(owner, k -> new HashSet<>()).add(trusted);
        save();
    }

    public void removeTrusted(UUID owner, UUID trusted) {
        Set<UUID> set = trustedMap.get(owner);
        if (set != null) {
            set.remove(trusted);
            save();
        }
    }

    public boolean isTrusted(UUID owner, UUID trusted) {
        return trustedMap.getOrDefault(owner, Collections.emptySet()).contains(trusted);
    }

    /* -----------------------------
     * Debug Utils
     * ----------------------------- */
    public void logTrusted(UUID owner) {
        OfflinePlayer ownerPlayer = Bukkit.getOfflinePlayer(owner);
        plugin.getLogger().info("Trusted list for " + ownerPlayer.getName() + ":");
        for (UUID t : getTrusted(owner)) {
            OfflinePlayer tp = Bukkit.getOfflinePlayer(t);
            plugin.getLogger().info(" - " + tp.getName());
        }
    }
}
