package com.aegisguard.data;

import com.aegisguard.AegisGuard;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * PlotStore
 * - Stores plots: owner, bounds, trusted
 * - Saves/loads from plots.yml with UUID + usernames
 */
public class PlotStore {

    private final AegisGuard plugin;
    private final File file;
    private FileConfiguration data;

    // Map<OwnerUUID, Plot>
    private final Map<UUID, Plot> plots = new HashMap<>();

    public PlotStore(AegisGuard plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "plots.yml");
        load();
    }

    /* -----------------------------
     * Data Structures
     * ----------------------------- */
    public static class Plot {
        private final UUID owner;
        private String ownerName;
        private final String world;
        private final int x1, z1, x2, z2;
        private final Set<UUID> trusted = new HashSet<>();
        private final Map<UUID, String> trustedNames = new HashMap<>();

        public Plot(UUID owner, String ownerName, String world, int x1, int z1, int x2, int z2) {
            this.owner = owner;
            this.ownerName = ownerName;
            this.world = world;
            this.x1 = Math.min(x1, x2);
            this.z1 = Math.min(z1, z2);
            this.x2 = Math.max(x1, x2);
            this.z2 = Math.max(z1, z2);
        }

        public UUID getOwner() { return owner; }
        public String getOwnerName() { return ownerName; }
        public String getWorld() { return world; }
        public int getX1() { return x1; }
        public int getZ1() { return z1; }
        public int getX2() { return x2; }
        public int getZ2() { return z2; }

        public Set<UUID> getTrusted() { return trusted; }
        public Map<UUID, String> getTrustedNames() { return trustedNames; }

        public void setOwnerName(String name) { this.ownerName = name; }

        public boolean isInside(Location loc) {
            if (!loc.getWorld().getName().equals(world)) return false;
            int x = loc.getBlockX();
            int z = loc.getBlockZ();
            return x >= x1 && x <= x2 && z >= z1 && z <= z2;
        }
    }

    /* -----------------------------
     * Load / Save
     * ----------------------------- */
    public void load() {
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        this.data = YamlConfiguration.loadConfiguration(file);

        plots.clear();
        if (data.isConfigurationSection("plots")) {
            for (String ownerId : data.getConfigurationSection("plots").getKeys(false)) {
                UUID owner = UUID.fromString(ownerId);
                String path = "plots." + ownerId;
                String ownerName = data.getString(path + ".owner-name", "Unknown");
                String world = data.getString(path + ".world");
                int x1 = data.getInt(path + ".x1");
                int z1 = data.getInt(path + ".z1");
                int x2 = data.getInt(path + ".x2");
                int z2 = data.getInt(path + ".z2");

                Plot plot = new Plot(owner, ownerName, world, x1, z1, x2, z2);

                if (data.isConfigurationSection(path + ".trusted")) {
                    for (String uuidStr : data.getConfigurationSection(path + ".trusted").getKeys(false)) {
                        UUID t = UUID.fromString(uuidStr);
                        String tName = data.getString(path + ".trusted." + uuidStr, "Unknown");
                        plot.getTrusted().add(t);
                        plot.getTrustedNames().put(t, tName);
                    }
                }
                plots.put(owner, plot);
            }
        }
    }

    public void save() {
        for (UUID owner : plots.keySet()) {
            Plot plot = plots.get(owner);
            String path = "plots." + owner.toString();

            // Always refresh usernames before save
            OfflinePlayer op = Bukkit.getOfflinePlayer(owner);
            plot.setOwnerName(op.getName() != null ? op.getName() : "Unknown");

            data.set(path + ".owner-name", plot.getOwnerName());
            data.set(path + ".world", plot.getWorld());
            data.set(path + ".x1", plot.getX1());
            data.set(path + ".z1", plot.getZ1());
            data.set(path + ".x2", plot.getX2());
            data.set(path + ".z2", plot.getZ2());

            for (UUID t : plot.getTrusted()) {
                OfflinePlayer tp = Bukkit.getOfflinePlayer(t);
                plot.getTrustedNames().put(t, tp.getName() != null ? tp.getName() : "Unknown");
            }

            for (UUID t : plot.getTrustedNames().keySet()) {
                data.set(path + ".trusted." + t.toString(), plot.getTrustedNames().get(t));
            }
        }

        try { data.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    public void flushSync() { save(); }

    /* -----------------------------
     * Plot Management
     * ----------------------------- */
    public Plot getPlot(UUID owner) { return plots.get(owner); }

    public void createPlot(UUID owner, Location c1, Location c2) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(owner);
        Plot plot = new Plot(
                owner,
                op.getName() != null ? op.getName() : "Unknown",
                c1.getWorld().getName(),
                c1.getBlockX(),
                c1.getBlockZ(),
                c2.getBlockX(),
                c2.getBlockZ()
        );
        plots.put(owner, plot);
        save();
    }

    public void removePlot(UUID owner) {
        plots.remove(owner);
        data.set("plots." + owner.toString(), null);
        save();
    }

    public boolean hasPlot(UUID owner) { return plots.containsKey(owner); }

    public Plot getPlotAt(Location loc) {
        for (Plot p : plots.values()) {
            if (p.isInside(loc)) return p;
        }
        return null;
    }

    /* -----------------------------
     * Trusted Management
     * ----------------------------- */
    public List<UUID> getTrusted(UUID owner) {
        Plot p = plots.get(owner);
        if (p == null) return Collections.emptyList();
        return new ArrayList<>(p.getTrusted());
    }

    public void addTrusted(UUID owner, UUID trusted) {
        Plot p = plots.get(owner);
        if (p != null) {
            OfflinePlayer tp = Bukkit.getOfflinePlayer(trusted);
            p.getTrusted().add(trusted);
            p.getTrustedNames().put(trusted, tp.getName() != null ? tp.getName() : "Unknown");
            save();
        }
    }

    public void removeTrusted(UUID owner, UUID trusted) {
        Plot p = plots.get(owner);
        if (p != null) {
            p.getTrusted().remove(trusted);
            p.getTrustedNames().remove(trusted);
            save();
        }
    }

    public boolean isTrusted(UUID owner, UUID trusted) {
        Plot p = plots.get(owner);
        return p != null && p.getTrusted().contains(trusted);
    }
}
