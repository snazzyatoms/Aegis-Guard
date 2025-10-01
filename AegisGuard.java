// src/main/java/com/AegisGuard/AegisGuard.java
package com.aegisguard;

import com.aegisguard.config.AGConfig;
import com.aegisguard.data.PlotStore;
import com.aegisguard.gui.GUIListener;
import com.aegisguard.gui.GUIManager;
import com.aegisguard.protection.ProtectionManager;
import com.aegisguard.selection.SelectionService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * ==============================================================
 *  AegisGuard v1.0
 *  Main plugin entry point
 *
 *  - Loads config & resources
 *  - Registers managers & listeners
 *  - Handles main /aegis command
 *  - Provides Aegis Scepter utility
 * ==============================================================
 */
public class AegisGuard extends JavaPlugin {

    /* -----------------------------
     * Managers & Services
     * ----------------------------- */
    private AGConfig configMgr;
    private PlotStore plotStore;
    private GUIManager gui;
    private ProtectionManager protection;
    private SelectionService selection;

    /* -----------------------------
     * Public Getters
     * ----------------------------- */
    public AGConfig cfg() { return configMgr; }
    public PlotStore store() { return plotStore; }
    public GUIManager gui() { return gui; }
    public ProtectionManager protection() { return protection; }
    public SelectionService selection() { return selection; }

    /* -----------------------------
     * Lifecycle
     * ----------------------------- */
    @Override
    public void onEnable() {
        // Ensure config + messages exist
        saveDefaultConfig();
        saveResource("messages.yml", false);

        // Initialize managers
        this.configMgr   = new AGConfig(this);
        this.plotStore   = new PlotStore(this);
        this.selection   = new SelectionService(this);
        this.gui         = new GUIManager(this);
        this.protection  = new ProtectionManager(this);

        // Register events
        Bukkit.getPluginManager().registerEvents(new GUIListener(this), this);
        Bukkit.getPluginManager().registerEvents(protection, this);
        Bukkit.getPluginManager().registerEvents(selection, this);

        getLogger().info("AegisGuard v" + getDescription().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        plotStore.flushSync(); // Save data before shutdown
        getLogger().info("AegisGuard disabled. Data saved.");
    }

    /* -----------------------------
     * Commands (/aegis)
     * ----------------------------- */
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("aegis")) return false;
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (args.length == 0) {
            gui.openMain(p);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "wand" -> p.getInventory().addItem(createScepter());
            case "menu" -> gui.openMain(p);
            case "claim" -> selection.confirmClaim(p);
            case "unclaim" -> selection.unclaimHere(p);
            default -> p.sendMessage(ChatColor.RED + "Usage: /aegis <wand|menu|claim|unclaim>");
        }
        return true;
    }

    /* -----------------------------
     * Utility: Create Aegis Scepter
     * ----------------------------- */
    public ItemStack createScepter() {
        ItemStack rod = new ItemStack(Material.LIGHTNING_ROD);
        ItemMeta meta = rod.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Aegis Scepter");
            meta.setLore(java.util.List.of(
                    ChatColor.GRAY + "Right-click: Open Aegis Menu",
                    ChatColor.GRAY + "Left/Right-click: Select corners",
                    ChatColor.GRAY + "Sneak + Left: Expand/Resize"
            ));
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
            rod.setItemMeta(meta);
        }
        return rod;
    }
}
