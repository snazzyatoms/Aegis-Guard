package com.aegisguard.expansions;

import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;

/**
 * ExpansionRequest
 * ---------------------------------------
 * Represents a pending request to expand a player's plot.
 *
 * - Supports Vault or Item-based cost models
 * - Tracks requesting player, target plot, world context
 * - Used by ExpansionRequestManager to store and process active requests
 */
public class ExpansionRequest {

    private final UUID requester;       // Player requesting the expansion
    private final UUID plotOwner;       // Owner of the plot (for approval)
    private final String worldName;     // Target world
    private final int currentRadius;    // Current radius of the plot
    private final int requestedRadius;  // Requested new radius
    private final double cost;          // Calculated Vault cost (if applicable)
    private final long timestamp;       // Creation timestamp

    private boolean approved;           // True if approved
    private boolean denied;             // True if denied

    public ExpansionRequest(UUID requester, UUID plotOwner, String worldName,
                            int currentRadius, int requestedRadius, double cost) {
        this.requester = requester;
        this.plotOwner = plotOwner;
        this.worldName = worldName;
        this.currentRadius = currentRadius;
        this.requestedRadius = requestedRadius;
        this.cost = cost;
        this.timestamp = System.currentTimeMillis();
        this.approved = false;
        this.denied = false;
    }

    /* -----------------------------
     * Getters
     * ----------------------------- */

    public UUID getRequester() {
        return requester;
    }

    public UUID getPlotOwner() {
        return plotOwner;
    }

    public String getWorldName() {
        return worldName;
    }

    public World getWorld() {
        return Bukkit.getWorld(worldName);
    }

    public int getCurrentRadius() {
        return currentRadius;
    }

    public int getRequestedRadius() {
        return requestedRadius;
    }

    public double getCost() {
        return cost;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public OfflinePlayer getRequesterPlayer() {
        return Bukkit.getOfflinePlayer(requester);
    }

    public OfflinePlayer getOwnerPlayer() {
        return Bukkit.getOfflinePlayer(plotOwner);
    }

    public boolean isApproved() {
        return approved;
    }

    public boolean isDenied() {
        return denied;
    }

    /* -----------------------------
     * State Management
     * ----------------------------- */

    public void approve() {
        this.approved = true;
        this.denied = false;
    }

    public void deny() {
        this.denied = true;
        this.approved = false;
    }

    public boolean isPending() {
        return !approved && !denied;
    }

    /* -----------------------------
     * Utility
     * ----------------------------- */

    public String getStatus() {
        if (approved) return "APPROVED";
        if (denied) return "DENIED";
        return "PENDING";
    }

    @Override
    public String toString() {
        return "ExpansionRequest{" +
                "requester=" + requester +
                ", plotOwner=" + plotOwner +
                ", worldName='" + worldName + '\'' +
                ", currentRadius=" + currentRadius +
                ", requestedRadius=" + requestedRadius +
                ", cost=" + cost +
                ", status=" + getStatus() +
                '}';
    }
}
