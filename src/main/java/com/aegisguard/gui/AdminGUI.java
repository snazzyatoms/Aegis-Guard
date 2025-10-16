package com.aegisguard.gui;

import com.aegisguard.AegisGuard;
import com.aegisguard.expansions.ExpansionRequestAdminGUI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Objects;

public class AdminGUI {

    private final AegisGuard plugin;

    public AdminGUI(AegisGuard plugin) {
        this.plugin = plugin;
    }

    /** Tag holder so click handler only reacts to this GUI */
    private static class AdminHolder implements InventoryHolder {
        @Override public Inventory getInventory() { return null; }
    }

    private String title(Player player) {
        String raw = plugin.msg().get(player, "admin_menu_title");
        if (raw != null && !raw.contains("[Missing")) return raw;
        return "§b🛡 AegisGuard — Admin";
    }

    private ItemStack bg() {
        return GUIManager.icon(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
    }

    private boolean getBool(String path, boolean def) {
        return plugin.getConfig().getBoolean(path, def);
    }

    /** Flip a boolean at path, save, and return the NEW value */
    private boolean flipBool(String path, boolean def) {
        boolean cur = getBool(path, def);
        boolean next = !cur;
        plugin.getConfig().set(path, next);
        plugin.saveConfig();
        return next;
    }

    public void open(Player player) {
        if (!player.hasPermission("aegis.admin")) {
            plugin.msg().send(player, "no_perm");
            return;
        }

        Inventory inv = Bukkit.createInventory(new AdminHolder(), 45, title(player));
        // background
        var bg = bg();
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, bg);

        // Read toggles
        boolean autoRemove = getBool("admin.auto_remove_banned", false);
        boolean bypass     = getBool("admin.bypass_claim_limit", false);
        boolean broadcast  = getBool("admin.broadcast_admin_actions", false);
        boolean unlimited  = getBool("admin.unlimited_plots", true); // new: admin can create unlimited plots
        boolean proxySync  = getBool("sync.proxy.enabled", false);   // new: bungee/proxy sync toggle
        boolean perfMode   = getBool("performance.low_overhead_mode", false); // optional: trim cosmetics

        // Row 2 — core toggles
        inv.setItem(10, GUIManager.icon(
                autoRemove ? Material.TNT : Material.GUNPOWDER,
                autoRemove ? "§aAuto-Remove Banned: §aON" : "§7Auto-Remove Banned: §cOFF",
                List.of("§7Removes banned players’ plots on load/ban.")
        ));

        inv.setItem(12, GUIManager.icon(
                bypass ? Material.NETHER_STAR : Material.IRON_NUGGET,
                bypass ? "§aBypass Claim Limit (OP): §aON" : "§7Bypass Claim Limit (OP): §cOFF",
                List.of("§7OPs can exceed the per-player claim limit.")
        ));

        inv.setItem(14, GUIManager.icon(
                broadcast ? Material.BEACON : Material.LIGHT,
                broadcast ? "§aBroadcast Admin Actions: §aON" : "§7Broadcast Admin Actions: §cOFF",
                List.of("§7Announce important admin actions in chat.")
        ));

        // Row 3 — admin power & sync
        inv.setItem(19, GUIManager.icon(
                unlimited ? Material.EMERALD_BLOCK : Material.EMERALD,
                unlimited ? "§aUnlimited Plots (Admin): §aON" : "§7Unlimited Plots (Admin): §cOFF",
                List.of("§7Admins can create unlimited plots/claims.")
        ));

        inv.setItem(21, GUIManager.icon(
                proxySync ? Material.ENDER_EYE : Material.ENDER_PEARL,
                proxySync ? "§aGlobal Sync (Proxy): §aON" : "§7Global Sync (Proxy): §cOFF",
                List.of(
                        "§7Enable Bungee/proxy sync for claims/flags.",
                        "§8(Requires SyncBridge setup; see config)"
                )
        ));

        inv.setItem(23, GUIManager.icon(
                perfMode ? Material.REDSTONE_BLOCK : Material.REDSTONE,
                perfMode ? "§aPerformance Mode: §aON" : "§7Performance Mode: §cOFF",
                List.of(
                        "§7Disables non-essential cosmetics for speed.",
                        "§7Great for large servers or heavy plugin stacks."
                )
        ));

        // Row 4 — tools & navigation
        inv.setItem(28, GUIManager.icon(
                Material.AMETHYST_CLUSTER,
                "§dExpansion Admin",
                List.of("§7Open Expansion admin preview.",
                        "§8(Community build — full workflow later)")
        ));

        inv.setItem(30, GUIManager.icon(
                Material.COMPASS,
                "§bDiagnostics",
                List.of(
                        "§7Show TPS, listener counts, last sync time,",
                        "§7and adapter statuses (Vault/Towny/Proxy)."
                )
        ));

        inv.setItem(31, GUIManager.icon(
                Material.REPEATER,
                "§eReload Config",
                List.of("§7Reload config, messages, and plots.yml",
                        "§7without restarting the server.")
        ));

        inv.setItem(34, GUIManager.icon(
                Material.ARROW,
                plugin.msg().get(player, "button_back"),
                plugin.msg().getList(player, "back_lore")
        ));

        inv.setItem(40, GUIManager.icon(
                Material.BARRIER,
                plugin.msg().get(player, "button_exit"),
                plugin.msg().getList(player, "exit_lore")
        ));

        player.openInventory(inv);
        plugin.sounds().playMenuOpen(player);
    }

    public void handleClick(Player player, InventoryClickEvent e) {
        // Hard guard: only handle if this is OUR menu
        if (!(e.getInventory().getHolder() instanceof AdminHolder)) return;

        e.setCancelled(true);
        if (e.getCurrentItem() == null) return;

        switch (Objects.requireNonNull(e.getCurrentItem().getType())) {
            // Toggles
            case TNT, GUNPOWDER -> {
                boolean now = flipBool("admin.auto_remove_banned", false);
                player.sendMessage(plugin.msg().get(now ? "admin_auto_remove_enabled" : "admin_auto_remove_disabled"));
                plugin.sounds().playMenuFlip(player);
                open(player);
            }
            case NETHER_STAR, IRON_NUGGET -> {
                boolean now = flipBool("admin.bypass_claim_limit", false);
                player.sendMessage(plugin.msg().get(now ? "admin_bypass_enabled" : "admin_bypass_disabled"));
                plugin.sounds().playMenuFlip(player);
                open(player);
            }
            case BEACON, LIGHT -> {
                boolean now = flipBool("admin.broadcast_admin_actions", false);
                player.sendMessage(plugin.msg().get(now ? "admin_broadcast_enabled" : "admin_broadcast_disabled"));
                plugin.sounds().playMenuFlip(player);
                open(player);
            }
            case EMERALD_BLOCK, EMERALD -> {
                boolean now = flipBool("admin.unlimited_plots", true);
                player.sendMessage(now
                        ? "§a[Admin] Unlimited plots enabled."
                        : "§e[Admin] Unlimited plots disabled.");
                plugin.sounds().playMenuFlip(player);
                open(player);
            }
            case ENDER_EYE, ENDER_PEARL -> {
                boolean now = flipBool("sync.proxy.enabled", false);
                player.sendMessage(now
                        ? "§aGlobal proxy sync enabled."
                        : "§eGlobal proxy sync disabled.");
                // (Later: kick off an initial handshake to SyncBridge here)
                plugin.sounds().playMenuFlip(player);
                open(player);
            }
            case REDSTONE_BLOCK, REDSTONE -> {
                boolean now = flipBool("performance.low_overhead_mode", false);
                player.sendMessage(now
                        ? "§aPerformance mode enabled."
                        : "§ePerformance mode disabled.");
                plugin.sounds().playMenuFlip(player);
                open(player);
            }

            // Expansion Admin (preview)
            case AMETHYST_CLUSTER -> new ExpansionRequestAdminGUI(plugin).open(player);

            // Diagnostics
            case COMPASS -> {
                plugin.gui().openDiagnostics(player); // implement a simple DiagnosticsGUI
                plugin.sounds().playMenuFlip(player);
            }

            // Reload
            case REPEATER -> {
                plugin.reloadConfig();
                plugin.msg().reload();
                plugin.store().load();
                player.sendMessage("§a✔ AegisGuard reloaded.");
                plugin.sounds().playMenuFlip(player);
                open(player);
            }

            // Back / Exit
            case ARROW -> {
                plugin.gui().openMain(player);
                plugin.sounds().playMenuFlip(player);
            }
            case BARRIER -> {
                player.closeInventory();
                plugin.sounds().playMenuClose(player);
            }
            default -> { /* ignore */ }
        }
    }
}
