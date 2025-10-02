package com.aegisguard.economy;

import com.aegisguard.AegisGuard;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * VaultHook
 * - Handles economy checks/payments through Vault
 * - Optional: if no Vault installed, plugin works free
 */
public class VaultHook {

    private final AegisGuard plugin;
    private Economy economy;

    public VaultHook(AegisGuard plugin) {
        this.plugin = plugin;
        setupEconomy();
    }

    private void setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            var rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp != null) {
                this.economy = rsp.getProvider();
                plugin.getLogger().info("Vault economy hooked: " + economy.getName());
            }
        }
    }

    public boolean isEnabled() {
        return economy != null;
    }

    public boolean charge(Player p, double amount) {
        if (!isEnabled() || amount <= 0) return true; // free if Vault missing
        if (economy.has(p, amount)) {
            economy.withdrawPlayer(p, amount);
            return true;
        }
        return false;
    }

    public void give(Player p, double amount) {
        if (isEnabled() && amount > 0) {
            economy.depositPlayer(p, amount);
        }
    }
}
