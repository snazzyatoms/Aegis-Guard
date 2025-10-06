package com.aegisguard.data;

import com.aegisguard.AegisGuard;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * PlotStore
 * - Multi-plot support (per player configurable via config)
 * - Stores: owner, bounds, trusted, flags
 * - Persists to plots.yml
 * - Auto-removes banned players' plots (if enabled)
 */
public class PlotStore {

    private final AegisGuard plugin;
    private final File file;
    private FileConfiguration data;

    // Map<OwnerUUID, List<Plot>>
    private final Map<UUID, List<Plot>> plots = new HashMap<>();

    public PlotStore(AegisGuard plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "plots.yml");
        load();

        // Register ban listener (Paper API). If not present at runtime, no issue—class exists when compiling against paper-api.
        Bukkit.getPluginManager().registerEvents(new BanListener(), plugin);
    }

    /* -----------------------------
     * Data Structures
     * ----------------------------- */
    public static class Plot {
        private final UUID plotId;
        private final UUID owner;
        private String ownerName;
        private final String world;
        private final int x1, z1, x2, z2;
        private final Set<UUID> trusted = new HashSet<>();
        private final Map<UUID, String> trustedNames = new HashMap<>();
        private final Map<String, Boolean> flags = new HashMap<>();

        public Plot(UUID plotId, UUID owner, String ownerName, String world, int x1, int z1, int x2, int z2) {
            this.plotId = plotId;
            this.owner = owner;
            this.ownerName = ownerName;
            this.world = world;
            this.x1 = Math.min(x1, x2);
            this.z1 = Math.min(z1, z2);
            this.x2 = Math.max(x1, x2);
            this.z2 = Math.max(z1, z2);

            // Default protections ON (can be overridden by config defaults via ProtectionManager.def)
            flags.put("pvp", true);
            flags.put("containers", true);
            flags.put("mobs", true);
            flags.put("pets", true);
            flags.put("entities", true);
            flags.put("farm", true);
        }

        public UUID getPlotId() { return plotId; }
        public UUID getOwner() { return owner; }
        public String getOwnerName() { return ownerName; }
        public void setOwnerName(String name) { this.ownerName = name; }
        public String getWorld() { return world; }
        public int getX1() { return x1; }
        public int getZ1() { return z1; }
        public int getX2() { return x2; }
        public int getZ2() { return z2; }

        public Set<UUID> getTrusted() { return trusted; }
        public Map<UUID, String> getTrustedNames() { return trustedNames; }

        public boolean isInside(Location loc) {
            if (loc == null || loc.getWorld() == null) return false;
            if (!loc.getWorld().getName().equals(world)) return false;
            int x = loc.getBlockX();
            int z = loc.getBlockZ();
            return x >= x1 && x <= x2 && z >= z1 && z <= z2;
        }

        // Flags
        public boolean getFlag(String key, boolean def) { return flags.getOrDefault(key, def); }
        public void setFlag(String key, boolean value) { flags.put(key, value); }
        public Map<String, Boolean> getFlags() { return flags; }
    }

    /* -----------------------------
     * Load / Save
     * ----------------------------- */
    public void load() {
        try {
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
        } catch (IOException ignored) {}
        this.data = YamlConfiguration.loadConfiguration(file);
        plots.clear();

        if (data.isConfigurationSection("plots")) {
            for (String ownerId : data.getConfigurationSection("plots").getKeys(false)) {
                UUID owner;
                try { owner = UUID.fromString(ownerId); }
                catch (IllegalArgumentException ex) { continue; }

                String ownerPath = "plots." + ownerId;

                // Legacy single-plot format (plots.<owner>.*)
                if (data.isSet(ownerPath + ".x1")) {
                    migrateLegacy(owner);
                    continue;
                }

                // Multi-plot format (plots.<owner>.<plotId>.*)
                for (String plotIdStr : data.getConfigurationSection(ownerPath).getKeys(false)) {
                    String path = ownerPath + "." + plotIdStr;
                    UUID plotId;
                    try { plotId = UUID.fromString(plotIdStr); }
                    catch (IllegalArgumentException ex) { continue; }

                    String ownerName = data.getString(path + ".owner-name", "Unknown");
                    String world = data.getString(path + ".world");
                    int x1 = data.getInt(path + ".x1");
                    int z1 = data.getInt(path + ".z1");
                    int x2 = data.getInt(path + ".x2");
                    int z2 = data.getInt(path + ".z2");

                    if (world == null) continue; // skip corrupt entries

                    Plot plot = new Plot(plotId, owner, ownerName, world, x1, z1, x2, z2);

                    // Trusted
                    if (data.isConfigurationSection(path + ".trusted")) {
                        for (String uuidStr : data.getConfigurationSection(path + ".trusted").getKeys(false)) {
                            try {
                                UUID t = UUID.fromString(uuidStr);
                                String tName = data.getString(path + ".trusted." + uuidStr, "Unknown");
                                plot.getTrusted().add(t);
                                plot.getTrustedNames().put(t, tName);
                            } catch (IllegalArgumentException ignored) {}
                        }
                    }

                    // Flags
                    if (data.isConfigurationSection(path + ".flags")) {
                        for (String flagKey : data.getConfigurationSection(path + ".flags").getKeys(false)) {
                            boolean val = data.getBoolean(path + ".flags." + flagKey, true);
                            plot.setFlag(flagKey, val);
                        }
                    }

                    plots.computeIfAbsent(owner, k -> new ArrayList<>()).add(plot);
                }
            }
        }

        // Auto-remove banned plots if enabled
        if (plugin.getConfig().getBoolean("admin.auto_remove_banned", false)) {
            removeBannedPlots();
        }
    }

    private void migrateLegacy(UUID owner) {
        String base = "plots." + owner;
        String ownerName = data.getString(base + ".owner-name", "Unknown");
        String world = data.getString(base + ".world");
        int x1 = data.getInt(base + ".x1");
        int z1 = data.getInt(base + ".z1");
        int x2 = data.getInt(base + ".x2");
        int z2 = data.getInt(base + ".z2");

        if (world == null) return;

        UUID plotId = UUID.randomUUID();
        Plot plot = new Plot(plotId, owner, ownerName, world, x1, z1, x2, z2);

        // migrate legacy trusted (if present)
        if (data.isConfigurationSection(base + ".trusted")) {
            for (String uuidStr : data.getConfigurationSection(base + ".trusted").getKeys(false)) {
                try {
                    UUID t = UUID.fromString(uuidStr);
                    String tName = data.getString(base + ".trusted." + uuidStr, "Unknown");
                    plot.getTrusted().add(t);
                    plot.getTrustedNames().put(t, tName);
                } catch (IllegalArgumentException ignored) {}
            }
        }
        // migrate legacy flags (if present)
        if (data.isConfigurationSection(base + ".flags")) {
            for (String flagKey : data.getConfigurationSection(base + ".flags").getKeys(false)) {
                boolean val = data.getBoolean(base + ".flags." + flagKey, true);
                plot.setFlag(flagKey, val);
            }
        }

        plots.computeIfAbsent(owner, k -> new ArrayList<>()).add(plot);
        data.set(base, null); // clear old
        save(); // re-save in new format
    }

    public void save() {
        data.set("plots", null); // clear
        for (Map.Entry<UUID, List<Plot>> entry : plots.entrySet()) {
            UUID owner = entry.getKey();
            List<Plot> list = entry.getValue();
            if (list == null || list.isEmpty()) continue;

            for (Plot plot : list) {
                String path = "plots." + owner + "." + plot.getPlotId();

                OfflinePlayer op = Bukkit.getOfflinePlayer(owner);
                plot.setOwnerName(op.getName() != null ? op.getName() : "Unknown");

                data.set(path + ".owner-name", plot.getOwnerName());
                data.set(path + ".world", plot.getWorld());
                data.set(path + ".x1", plot.getX1());
                data.set(path + ".z1", plot.getZ1());
                data.set(path + ".x2", plot.getX2());
                data.set(path + ".z2", plot.getZ2());

                // Trusted names refresh (ensures latest names)
                for (UUID t : new HashSet<>(plot.getTrusted())) {
                    OfflinePlayer tp = Bukkit.getOfflinePlayer(t);
                    plot.getTrustedNames().put(t, tp.getName() != null ? tp.getName() : "Unknown");
                }
                for (Map.Entry<UUID, String> tn : plot.getTrustedNames().entrySet()) {
                    data.set(path + ".trusted." + tn.getKey(), tn.getValue());
                }

                for (Map.Entry<String, Boolean> flag : plot.getFlags().entrySet()) {
                    data.set(path + ".flags." + flag.getKey(), flag.getValue());
                }
            }
        }
        try {
            data.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void flushSync() { save(); }

    /* -----------------------------
     * Plot Management
     * ----------------------------- */
    public List<Plot> getPlots(UUID owner) {
        List<Plot> list = plots.get(owner);
        return (list == null) ? Collections.emptyList() : Collections.unmodifiableList(list);
    }

    public Plot getPlot(UUID owner, UUID plotId) {
        return plots.getOrDefault(owner, Collections.emptyList())
                .stream().filter(p -> p.getPlotId().equals(plotId)).findFirst().orElse(null);
    }

    public void createPlot(UUID owner, Location c1, Location c2) {
        if (c1 == null || c2 == null || c1.getWorld() == null || c2.getWorld() == null) return;
        if (!c1.getWorld().equals(c2.getWorld())) return; // must be same world

        boolean bypass = plugin.getConfig().getBoolean("admin.bypass_claim_limit", false);
        boolean isOp = Bukkit.getOfflinePlayer(owner).isOp();
        List<Plot> owned = plots.computeIfAbsent(owner, k -> new ArrayList<>());

        // Enforce claim limit unless admin bypass applies (bypass must be enabled AND player must be op)
        if (!(bypass && isOp)) {
            int max = plugin.getConfig().getInt("claims.max_claims_per_player", 1);
            if (owned.size() >= max) return;
        }

        OfflinePlayer op = Bukkit.getOfflinePlayer(owner);
        Plot plot = new Plot(
                UUID.randomUUID(),
                owner,
                op.getName() != null ? op.getName() : "Unknown",
                c1.getWorld().getName(),
                c1.getBlockX(),
                c1.getBlockZ(),
                c2.getBlockX(),
                c2.getBlockZ()
        );
        owned.add(plot);
        save();
    }

    public void removePlot(UUID owner, UUID plotId) {
        List<Plot> owned = plots.get(owner);
        if (owned == null) return;
        owned.removeIf(p -> p.getPlotId().equals(plotId));
        if (owned.isEmpty()) plots.remove(owner);
        save();
    }

    public void removeAllPlots(UUID owner) {
        if (plots.remove(owner) != null) {
            save();
        }
    }

    public boolean hasPlots(UUID owner) { return !getPlots(owner).isEmpty(); }

    public Plot getPlotAt(Location loc) {
        if (loc == null) return null;
        for (List<Plot> list : plots.values()) {
            for (Plot p : list) {
                if (p.isInside(loc)) return p;
            }
        }
        return null;
    }

    public Set<UUID> owners() { return Collections.unmodifiableSet(plots.keySet()); }

    /* -----------------------------
     * Trusted Management
     * ----------------------------- */
    public void addTrusted(UUID owner, UUID plotId, UUID trusted) {
        Plot p = getPlot(owner, plotId);
        if (p != null) {
            OfflinePlayer tp = Bukkit.getOfflinePlayer(trusted);
            p.getTrusted().add(trusted);
            p.getTrustedNames().put(trusted, tp.getName() != null ? tp.getName() : "Unknown");
            save();
        }
    }

    public void removeTrusted(UUID owner, UUID plotId, UUID trusted) {
        Plot p = getPlot(owner, plotId);
        if (p != null) {
            p.getTrusted().remove(trusted);
            p.getTrustedNames().remove(trusted);
            save();
        }
    }

    public boolean isTrusted(UUID owner, UUID plotId, UUID trusted) {
        Plot p = getPlot(owner, plotId);
        return p != null && p.getTrusted().contains(trusted);
    }

    /* -----------------------------
     * Admin Helpers
     * ----------------------------- */
    public void removeBannedPlots() {
        boolean broadcast = plugin.getConfig().getBoolean("admin.broadcast_admin_actions", false);

        Set<UUID> toRemove = new HashSet<>();
        for (UUID owner : new HashSet<>(plots.keySet())) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(owner);
            if (op.isBanned()) {
                toRemove.add(owner);
                plugin.getLogger().info("[AegisGuard] Removed plots of banned player: " + op.getName());

                String msg = plugin.msg().get("admin_removed_banned", "{PLAYER}", op.getName());
                if (broadcast) {
                    Bukkit.broadcastMessage(plugin.msg().prefix() + msg);
                } else {
                    Bukkit.getOnlinePlayers().stream()
                            .filter(p -> p.hasPermission("aegis.admin")) // standardized perm
                            .forEach(p -> p.sendMessage(plugin.msg().prefix() + msg));
                }
            }
        }
        for (UUID id : toRemove) plots.remove(id);
        if (!toRemove.isEmpty()) save();
    }

    /* -----------------------------
     * Ban Listener (Paper API)
     * ----------------------------- */
    private class BanListener implements Listener {
        @EventHandler
        public void onPlayerBan(PlayerBanEvent e) {
            if (!plugin.getConfig().getBoolean("admin.auto_remove_banned", false)) return;
            OfflinePlayer banned = Bukkit.getOfflinePlayer(e.getPlayer().getUniqueId());
            if (banned == null) return;

            removeAllPlots(banned.getUniqueId());
            plugin.getLogger().info("[AegisGuard] Instantly removed plots for banned player: " + banned.getName());
        }
    }
}
