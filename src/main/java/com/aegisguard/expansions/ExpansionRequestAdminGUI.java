package com.aegisguard.expansions;

import com.aegisguard.AegisGuard;
import com.aegisguard.gui.GUIManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * ExpansionRequestAdminGUI (Community v1.0.0)
 * ------------------------------------------------------------
 * - Safe, self-contained, and compiles without any expansion backend.
 * - Lets admins toggle a single config switch: expansions.enabled
 * - Shows a polished "About" panel. No references to radius or managers.
 * - Uses only stable APIs (no msg().has()), with sensible fallbacks.
 *
 * NOTE: Keep the real AdminGUI class in com.aegisguard.gui.AdminGUI ONLY.
 * Do NOT duplicate this class name in AdminGUI.java.
 */
public class ExpansionRequestAdminGUI {

    private final AegisGuard plugin;

    public ExpansionRequestAdminGUI(AegisGuard plugin) {
        this.plugin = plugin;
    }

    /* -----------------------------
     * Title helper (fallback-safe)
     * ----------------------------- */
    private String title(Player player) {
        String raw = plugin.msg().get(player, "expansion_admin_title");
        if (raw != null && !raw.contains("Missing:")) {
            return raw;
        }
        return "§b🛡 AegisGuard — Expansion Admin";
    }

    /* -----------------------------
     * Filler (subtle glass styling)
     * ----------------------------- */
    private ItemStack filler() {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            pane.setItemMeta(meta);
        }
        return pane;
    }

    /* -----------------------------
     * Open GUI
     * ----------------------------- */
    public void open(Player player) {
        if (!player.hasPermission("aegis.admin")) {
            plugin.msg().send(player, "no_perm");
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 27, title(player));

        // Fill background first for a polished look
        ItemStack bg = filler();
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, bg);

        boolean enabled = plugin.getConfig().getBoolean("expansions.enabled", false);

        // Toggle (left)
        inv.setItem(10, GUIManager.icon(
                enabled ? Material.AMETHYST_SHARD : Material.GRAY_DYE,
                enabled
                        ? "§aExpansion Requests: Enabled"
                        : "§7Expansion Requests: Disabled",
                List.of(
                        "§7Toggle acceptance of expansion requests.",
                        "§8(Placeholder; full system arrives later)"
                )
        ));

        // About (center)
        inv.setItem(13, GUIManager.icon(
                Material.BOOK,
                "§bAbout Expansions",
                List.of(
                        "§7This is a preview panel.",
                        "§7The complete Expansion workflow",
                        "§7(approve/deny/review/costing) will",
                        "§7ship in a future premium version."
                )
        ));

        // Back (right)
        inv.setItem(16, GUIManager.icon(
                Material.ARROW,
                plugin.msg().get(player, "button_back"),
                plugin.msg().getList(player, "back_lore")
        ));

        // Exit (bottom-center)
        inv.setItem(22, GUIManager.icon(
                Material.BARRIER,
                plugin.msg().get(player, "button_exit"),
                plugin.msg().getList(player, "exit_lore")
        ));

        player.openInventory(inv);
        plugin.sounds().playMenuOpen(player);
    }

    /* -----------------------------
     * Handle Clicks
     * ----------------------------- */
    public void handleClick(Player player, InventoryClickEvent e) {
        e.setCancelled(true);
        if (e.getCurrentItem() == null) return;

        switch (e.getCurrentItem().getType()) {
            case AMETHYST_SHARD, GRAY_DYE -> {
                boolean cur = plugin.getConfig().getBoolean("expansions.enabled", false);
                plugin.getConfig().set("expansions.enabled", !cur);
                plugin.saveConfig();
                plugin.sounds().playMenuFlip(player); // safe method that exists today
                open(player); // refresh
            }
            case BOOK -> {
                // Mirror the "About" lore into chat for clarity
                List<String> about = List.of(
                        "§b[Expansions] §7This is a preview.",
                        "§7The complete Expansion workflow (approve/deny/review/costing)",
                        "§7will be available in a future premium release."
                );
                for (String line : about) player.sendMessage(line);
                plugin.sounds().playMenuFlip(player);
            }
            case ARROW -> {
                // Return to Admin menu
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
