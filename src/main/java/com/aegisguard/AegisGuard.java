package com.aegisguard;

import com.aegisguard.config.AGConfig;
import com.aegisguard.data.PlotStore;
import com.aegisguard.economy.VaultHook;
import com.aegisguard.gui.GUIListener;
import com.aegisguard.gui.GUIManager;
import com.aegisguard.protection.ProtectionManager;
import com.aegisguard.selection.SelectionService;
import com.aegisguard.util.MessagesUtil;
import org.bukkit.Bukkit;
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
 *
 *  NOTE: /aegis sound is kept as a fallback admin override
 *        for global toggles only. Players control their own
 *        sounds through the Settings GUI.
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
    private VaultHook vault;
    private MessagesUtil messages;

    /* -----------------------------
     * Public Getters
     * ----------------------------- */
    public AGConfig cfg() { return configMgr; }
    public PlotStore store() { return plotStore; }
    public GUIManager gui() { return gui; }
    public ProtectionManager protection() { return protection; }
    public SelectionService selection() { return selection; }
    public VaultHook vault() { return vault; }
    public MessagesUtil msg() { return messages; }

    /* -----------------------------
     * Lifecycle
     * ----------------------------- */
    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("messages.yml", false);

        this.configMgr   = new AGConfig(this);
        this.plotStore   = new PlotStore(this);
        this.selection   = new SelectionService(this);
        this.gui         = new GUIManager(this);
        this.protection  = new ProtectionManager(this);
        this.vault       = new VaultHook(this);
        this.messages    = new MessagesUtil(this);

        Bukkit.getPluginManager().registerEvents(new GUIListener(this), this);
        Bukkit.getPluginManager().registerEvents(protection, this);
        Bukkit.getPluginManager().registerEvents(selection, this);

        getLogger().info("AegisGuard v" + getDescription().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        plotStore.flushSync();
        getLogger().info("AegisGuard disabled. Data saved.");
    }

    /* -----------------------------
     * Commands (/aegis)
     * ----------------------------- */
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("aegis")) return false;
        if (!(sender instanceof Player p)) {
            sender.sendMessage(msg().get("players_only"));
            return true;
        }

        if (args.length == 0) {
            gui.openMain(p);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "wand" -> {
                p.getInventory().addItem(createScepter());
                msg().send(p, "wand_given");
            }
            case "menu" -> gui.openMain(p);
            case "claim" -> selection.confirmClaim(p);
            case "unclaim" -> selection.unclaimHere(p);

            case "sound" -> {
                // Admin fallback: global toggle only
                if (!p.hasPermission("aegisguard.admin")) {
                    msg().send(p, "no_perm");
                    return true;
                }
                if (args.length < 2) {
                    p.sendMessage("§eUsage:");
                    p.sendMessage("§7/aegis sound global <on|off>");
                    return true;
                }
                if (args[1].equalsIgnoreCase("global")) {
                    if (args.length < 3) {
                        p.sendMessage("§cUsage: /aegis sound global <on|off>");
                        return true;
                    }
                    boolean enable = args[2].equalsIgnoreCase("on");
                    getConfig().set("sounds.global_enabled", enable);
                    saveConfig();
                    msg().send(p, enable ? "sound_global_enabled" : "sound_global_disabled");
                } else {
                    p.sendMessage("§cInvalid mode. Use §7global");
                }
            }

            default -> msg().send(p, "usage_main");
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
            meta.setDisplayName("§bAegis Scepter");
            meta.setLore(java.util.List.of(
                    "§7Right-click: Open Aegis Menu",
                    "§7Left/Right-click: Select corners",
                    "§7Sneak + Left: Expand/Resize"
            ));
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
            rod.setItemMeta(meta);
        }
        return rod;
    }

    /* -----------------------------
     * Utility: Sound Control
     * ----------------------------- */
    public boolean isSoundEnabled(Player player) {
        if (!getConfig().getBoolean("sounds.global_enabled", true)) {
            return false;
        }
        String key = "sounds.players." + player.getUniqueId();
        if (getConfig().isSet(key)) {
            return getConfig().getBoolean(key, true);
        }
        return true;
    }
}
