package com.aegisguard.economy;

import com.aegisguard.AegisGuard;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * VaultHook
 * - Optional economy via Vault. If absent, AegisGuard runs "free".
 * - Safe against late-loading Vault and failed transactions.
 */
public class VaultHook implements Listener {

    private final AegisGuard plugin;
    private Economy economy;

    public VaultHook(AegisGuard plugin) {
        this.plugin = plugin;
        // Try immediately (softdepend ensures order, but this keeps it resilient)
        setupEconomy();
        // Also watch for Vault loading later
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /** Re-try when Vault enables after us */
    @EventHandler
    public void onPluginEnable(PluginEnableEvent e) {
        if (economy == null && "Vault".equalsIgnoreCase(e.getPlugin().getName())) {
            setupEconomy();
        }
    }

    private void setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().info("Vault not found; economy features disabled (free mode).");
            economy = null;
            return;
        }
        RegisteredServiceProvider<Economy> rsp = plugin.getServer()
                .getServicesManager().getRegistration(Economy.class);
        if (rsp != null) {
            this.economy = rsp.getProvider();
            plugin.getLogger().info("Vault economy hooked: " + economy.getName());
        } else {
            plugin.getLogger().warning("Vault present but no Economy provider found yet.");
        }
    }

    public boolean isEnabled() {
        return economy != null;
    }

    /** Nicely format currency for messages. Falls back to plain amount. */
    public String format(double amount) {
        if (!Double.isFinite(amount)) amount = 0.0;
        if (economy != null) {
            try { return economy.format(amount); } catch (Throwable ignored) {}
        }
        // Simple fallback
        return String.format("$%,.2f", amount);
    }

    public double balance(OfflinePlayer p) {
        if (economy == null) return Double.POSITIVE_INFINITY; // treat as unlimited in free mode
        try { return economy.getBalance(p); } catch (Throwable t) { return 0.0; }
    }

    /** Charge a player. Returns true on success. Free if Vault missing or amount <= 0. */
    public boolean charge(Player p, double amount) {
        if (economy == null || amount <= 0) return true;
        if (!Double.isFinite(amount) || amount < 0) return false;

        OfflinePlayer op = p; // Player implements OfflinePlayer
        if (!economy.has(op, amount)) return false;

        EconomyResponse res = economy.withdrawPlayer(op, amount);
        if (!res.transactionSuccess()) {
            plugin.getLogger().warning("[Vault] withdraw failed for " + p.getName() + ": " + res.errorMessage);
            return false;
        }
        return true;
    }

    /** Give money to a player (no-op if Vault missing or amount <= 0). */
    public void give(Player p, double amount) {
        if (economy == null || amount <= 0 || !Double.isFinite(amount)) return;
        OfflinePlayer op = p;
        EconomyResponse res = economy.depositPlayer(op, amount);
        if (!res.transactionSuccess()) {
            plugin.getLogger().warning("[Vault] deposit failed for " + p.getName() + ": " + res.errorMessage);
        }
    }
}
