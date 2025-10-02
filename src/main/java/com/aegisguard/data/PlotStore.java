package com.aegisguard.data;

import com.aegisguard.AegisGuard;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * PlotStore
 * - Stores plots: owner, bounds, trusted
 * - Saves/loads from plots.yml
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
        private final String world;
        private final int x1, z1, x2, z2;
        private final Set<UUID> trusted = new HashSet<>();

        public Plot(UUID owner, String world, int x1, int z1, int x2, int z2) {
            this.owner = owner;
            this.world = world;
            this.x1 = Math.min(x1, x2);
            this.z1 = Math.min(z1, z2);
            this.x2 = Math.max(x1, x2);
            this.z2 = Math.max(z1, z2);
        }

        public UUID getOwner() { return owner; }
        public String getWorld() { return world; }
        public int getX1() { return x1; }
        public int getZ1() { return z1; }
        public int getX2() { return x2; }
        public int getZ2() { return z2; }

        public Set<UUID> getTrusted() { return trusted; }

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
                String world = data.getString(path + ".world");
                int x1 = data.getInt(path + ".x1");
                int z1 = data.getInt(path + ".z1");
                int x2 = data.getInt(path + ".x2");
                int z2 = data.getInt(path + ".z2");

                Plot plot = new Plot(owner, world, x1, z1, x2, z2);

                List<String> trustedList = data.getStringList(path + ".trusted");
                for (String uuidStr : trustedList) {
                    plot.getTrusted().add(UUID.fromString(uuidStr));
                }
                plots.put(owner, plot);
            }
        }
    }

    public void save() {
        for (UUID owner : plots.keySet()) {
            Plot plot = plots.get(owner);
            String path = "plots." + owner.toString();

            data.set(path + ".world", plot.getWorld());
            data.set(path + ".x1", plot.getX1());
            data.set(path + ".z1", plot.getZ1());
            data.set(path + ".x2", plot.getX2());
            data.set(path + ".z2", plot.getZ2());

            List<String> trustedIds = new ArrayList<>();
            for (UUID t : plot.getTrusted()) trustedIds.add(t.toString());
            data.set(path + ".trusted", trustedIds);
        }

        try { data.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    public void flushSync() { save(); }

    /* -----------------------------
     * Plot Management
     * ----------------------------- */
    public Plot getPlot(UUID owner) {
        return plots.get(owner);
    }

    public void createPlot(UUID owner, Location corner1, Location corner2) {
        Plot plot = new Plot(
                owner,
                corner1.getWorld().getName(),
                corner1.getBlockX(),
                corner1.getBlockZ(),
                corner2.getBlockX(),
                corner2.getBlockZ()
        );
        plots.put(owner, plot);
        save();
    }

    public void removePlot(UUID owner) {
        plots.remove(owner);
        data.set("plots." + owner.toString(), null);
        save();
    }

    public boolean hasPlot(UUID owner) {
        return plots.containsKey(owner);
    }

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
            p.getTrusted().add(trusted);
            save();
        }
    }

    public void removeTrusted(UUID owner, UUID trusted) {
        Plot p = plots.get(owner);
        if (p != null) {
            p.getTrusted().remove(trusted);
            save();
        }
    }

    public boolean isTrusted(UUID owner, UUID trusted) {
        Plot p = plots.get(owner);
        return p != null && p.getTrusted().contains(trusted);
    }
}
