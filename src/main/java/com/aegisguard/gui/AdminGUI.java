package com.aegisguard.gui;

import com.aegisguard.AegisGuard;
import com.aegisguard.expansions.ExpansionRequestAdminGUI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.List;

/**
 * AdminGUI
 * ---------------------------------------------
 * - Clean, polished admin panel
 * - Safe fallbacks for titles/messages
 * - Toggles:
 *    • Auto-remove banned players' plots
 *    • Bypass claim limit for OPs
 *    • Broadcast admin actions
 * - Shortcuts:
 *    • Open Expansion Admin preview
 *    • Reload config/messages/data
 *    • Back / Exit
 */
public class AdminGUI {

    private final AegisGuard plugin;

    public AdminGUI(AegisGuard plugin) {
        this.plugin = plugin;
    }

    private String title(Player player) {
        String raw = plugin.msg().get(player, "admin_menu_title");
        if (raw != null && !raw.contains("[Missing")) return raw;
        return "§b🛡 AegisGuard — Admin";
    }

    public void open(Player player) {
        if (!player.hasPermission("aegis.admin")) {
            plugin.msg().send(player, "no_perm");
            return;
        }

        // 5 rows for a nice layout
        Inventory inv = Bukkit.createInventory(null, 45, title(player));

        // Background
        var bg = GUIManager.icon(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, bg);

        // Read toggles
        boolean autoRemove = plugin.getConfig().getBoolean("admin.auto_remove_banned", false);
        boolean bypass     = plugin.getConfig().getBoolean("admin.bypass_claim_limit", false);
        boolean broadcast  = plugin.getConfig().getBoolean("admin.broadcast_admin_actions", false);

        // Row 2 — main toggles
        inv.setItem(10, GUIManager.icon(
                autoRemove ? Material.TNT : Material.GUNPOWDER,
                autoRemove ? "§aAuto-Remove Banned: §aON" : "§7Auto-Remove Banned: §cOFF",
                List.of(
                        "§7When enabled, banned players' plots are removed",
                        "§7on load and on ban events."
                )
        ));

        inv.setItem(12, GUIManager.icon(
                bypass ? Material.NETHER_STAR : Material.IRON_NUGGET,
                bypass ? "§aBypass Claim Limit (OP): §aON" : "§7Bypass Claim Limit (OP): §cOFF",
                List.of(
                        "§7When enabled, OPs can exceed the per-player",
                        "§7claim limit set in config."
                )
        ));

        inv.setItem(14, GUIManager.icon(
                broadcast ? Material.BEACON : Material.LIGHT,
                broadcast ? "§aBroadcast Admin Actions: §aON" : "§7Broadcast Admin Actions: §cOFF",
                List.of(
                        "§7If enabled, important admin actions will",
                        "§7announce in chat (or to admins only)."
                )
        ));

        // Row 4 — tools & navigation
        inv.setItem(28, GUIManager.icon(
                Material.AMETHYST_CLUSTER,
                "§dExpansion Admin",
                List.of(
                        "§7Open the Expansion admin preview.",
                        "§8(Community build — full workflow later)"
                )
        ));

        inv.setItem(31, GUIManager.icon(
                Material.REPEATER, // 1.20+ correct material name
                "§eReload Config",
                List.of(
                        "§7Reloads config, messages, and plots.yml",
                        "§7without restarting the server."
                )
        ));

        inv.setItem(34, GUIManager.icon(
                Material.ARROW,
                plugin.msg().get(player, "button_back"),
                plugin.msg().getList(player, "back_lore")
        ));

        // Exit at bottom-center
        inv.setItem(40, GUIManager.icon(
                Material.BARRIER,
                plugin.msg().get(player, "button_exit"),
                plugin.msg().getList(player, "exit_lore")
        ));

        player.openInventory(inv);
        plugin.sounds().playMenuOpen(player);
    }

    public void handleClick(Player player, InventoryClickEvent e) {
        e.setCancelled(true);
        if (e.getCurrentItem() == null) return;

        switch (e.getCurrentItem().getType()) {
            // Toggles
            case TNT, GUNPOWDER -> {
                boolean cur = plugin.getConfig().getBoolean("admin.auto_remove_banned", false);
                plugin.getConfig().set("admin.auto_remove_banned", !cur);
                plugin.saveConfig();
                // message keys expected in messages.yml (fallbacks handled by MessagesUtil)
                player.sendMessage(plugin.msg().get(!cur ? "admin_auto_remove_enabled" : "admin_auto_remove_disabled"));
                plugin.sounds().playMenuFlip(player);
                open(player);
            }
            case NETHER_STAR, IRON_NUGGET -> {
                boolean cur = plugin.getConfig().getBoolean("admin.bypass_claim_limit", false);
                plugin.getConfig().set("admin.bypass_claim_limit", !cur);
                plugin.saveConfig();
                player.sendMessage(plugin.msg().get(!cur ? "admin_bypass_enabled" : "admin_bypass_disabled"));
                plugin.sounds().playMenuFlip(player);
                open(player);
            }
            case BEACON, LIGHT -> {
                boolean cur = plugin.getConfig().getBoolean("admin.broadcast_admin_actions", false);
                plugin.getConfig().set("admin.broadcast_admin_actions", !cur);
                plugin.saveConfig();
                player.sendMessage(plugin.msg().get(!cur ? "admin_broadcast_enabled" : "admin_broadcast_disabled"));
                plugin.sounds().playMenuFlip(player);
                open(player);
            }

            // Expansion Admin (preview)
            case AMETHYST_CLUSTER -> {
                new com.aegisguard.expansions.ExpansionRequestAdminGUI(plugin).open(player);
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
