package com.aegisguard;

import com.aegisguard.admin.AdminCommand;
import com.aegisguard.config.AGConfig;
import com.aegisguard.data.PlotStore;
import com.aegisguard.economy.VaultHook;
import com.aegisguard.gui.GUIListener;
import com.aegisguard.gui.GUIManager;
import com.aegisguard.protection.ProtectionManager;
import com.aegisguard.selection.SelectionService;
import com.aegisguard.util.MessagesUtil;
import com.aegisguard.world.WorldRulesManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class AegisGuard extends JavaPlugin {

    private AGConfig configMgr;
    private PlotStore plotStore;
    private GUIManager gui;
    private ProtectionManager protection;
    private SelectionService selection;
    private VaultHook vault;
    private MessagesUtil messages;
    private WorldRulesManager worldRules;

    public AGConfig cfg() { return configMgr; }
    public PlotStore store() { return plotStore; }
    public GUIManager gui() { return gui; }
    public ProtectionManager protection() { return protection; }
    public SelectionService selection() { return selection; }
    public VaultHook vault() { return vault; }
    public MessagesUtil msg() { return messages; }
    public WorldRulesManager worldRules() { return worldRules; }

    @Override
    public void onEnable() {
        // Ensure bundled resources exist
        saveDefaultConfig();
        saveResource("messages.yml", false);

        // Core systems
        this.configMgr  = new AGConfig(this);
        this.plotStore  = new PlotStore(this);
        this.selection  = new SelectionService(this);
        this.gui        = new GUIManager(this);
        this.vault      = new VaultHook(this);
        this.messages   = new MessagesUtil(this);
        this.worldRules = new WorldRulesManager(this);
        this.protection = new ProtectionManager(this);

        // Listeners
        Bukkit.getPluginManager().registerEvents(new GUIListener(this), this);
        Bukkit.getPluginManager().registerEvents(protection, this);
        Bukkit.getPluginManager().registerEvents(selection, this);
        Bukkit.getPluginManager().registerEvents(plotStore, this); // if PlotStore implements Listener

        // Commands (null-safe)
        PluginCommand aegis = getCommand("aegis");
        if (aegis == null) {
            getLogger().severe("Missing /aegis command in plugin.yml. Disabling plugin.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        aegis.setExecutor(this);

        PluginCommand admin = getCommand("aegisadmin");
        if (admin != null) {
            admin.setExecutor(new AdminCommand(this));
        } else {
            getLogger().warning("No /aegisadmin command defined; admin tools will be unavailable.");
        }

        getLogger().info("AegisGuard v" + getDescription().getVersion() + " enabled.");
        getLogger().info("WorldRulesManager initialized for per-world protections.");
    }

    @Override
    public void onDisable() {
        if (plotStore != null) {
            plotStore.flushSync();
        }
        getLogger().info("AegisGuard disabled. Data saved.");
    }

    /* -----------------------------
     * Commands (/aegis)
     * ----------------------------- */
    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
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
                // Admin: global toggle only (per-player toggles can be added later)
                if (!p.hasPermission("aegis.admin")) {
                    msg().send(p, "no_perm");
                    return true;
                }
                if (args.length < 2 || !args[1].equalsIgnoreCase("global")) {
                    p.sendMessage("§eUsage:");
                    p.sendMessage("§7/aegis sound global <on|off>");
                    return true;
                }
                if (args.length < 3) {
                    p.sendMessage("§cUsage: /aegis sound global <on|off>");
                    return true;
                }
                boolean enable = args[2].equalsIgnoreCase("on");
                getConfig().set("sounds.global_enabled", enable);
                saveConfig();
                msg().send(p, enable ? "sound_global_enabled" : "sound_global_disabled");
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
