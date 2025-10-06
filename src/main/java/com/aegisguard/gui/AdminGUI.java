package com.aegisguard.expansions;

import com.aegisguard.AegisGuard;
import com.aegisguard.gui.GUIManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.List;

/**
 * Placeholder Expansion Admin GUI (ships with Community v1.0.0)
 * - Compiles without any expansion backend.
 * - Lets admins toggle a simple config switch and read "coming soon" text.
 * - In future (paid) versions, wire this to a proper ExpansionRequestManager.
 */
public class ExpansionRequestAdminGUI {

    private final AegisGuard plugin;

    public ExpansionRequestAdminGUI(AegisGuard plugin) {
        this.plugin = plugin;
    }

    private String title() {
        return plugin.msg().has("expansion_admin_title")
                ? plugin.msg().get("expansion_admin_title")
                : "§b🛡 AegisGuard — Expansion Admin";
    }

    public void open(Player player) {
        if (!player.hasPermission("aegis.admin")) {
            plugin.msg().send(player, "no_perm");
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 27, title());

        boolean enabled = plugin.getConfig().getBoolean("expansions.enabled", false);

        // Toggle expansions (config-only for now)
        inv.setItem(10, GUIManager.icon(
                enabled ? Material.AMETHYST_SHARD : Material.GRAY_DYE,
                enabled ? "§aExpansion Requests: Enabled" : "§7Expansion Requests: Disabled",
                List.of(
                        "§7Toggle acceptance of expansion requests.",
                        "§8(Placeholder; full system arrives later)"
                )
        ));

        // Info block
        inv.setItem(13, GUIManager.icon(
                Material.BOOK,
                "§bAbout Expansions",
                List.of(
                        "§7This menu is a preview.",
                        "§7The full Expansion system will be",
                        "§7available in a future premium release.",
                        "§8You’ll be able to review, approve,",
                        "§8deny, and auto-calc costs."
                )
        ));

        // Back
        inv.setItem(16, GUIManager.icon(
                Material.ARROW,
                "§eBack",
                List.of("§7Return to Admin Menu")
        ));

        // Exit
        inv.setItem(22, GUIManager.icon(
                Material.BARRIER,
                "§cExit",
                List.of("§7Close the Codex")
        ));

        player.openInventory(inv);
        plugin.sounds().playMenuOpen(player);
    }

    public void handleClick(Player player, InventoryClickEvent e) {
        e.setCancelled(true);
        if (e.getCurrentItem() == null) return;

        switch (e.getCurrentItem().getType()) {
            case AMETHYST_SHARD, GRAY_DYE -> {
                boolean cur = plugin.getConfig().getBoolean("expansions.enabled", false);
                plugin.getConfig().set("expansions.enabled", !cur);
                plugin.saveConfig();
                plugin.sounds().ok(player);
                open(player); // refresh
            }
            case BOOK -> {
                player.sendMessage("§b[Expansions] §7This is a preview. The full system arrives later.");
                plugin.sounds().playMenuFlip(player);
            }
            case ARROW -> {
                plugin.gui().admin().open(player);
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
