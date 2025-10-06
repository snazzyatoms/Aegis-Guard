package com.aegisguard.gui;

import com.aegisguard.AegisGuard;
import com.aegisguard.data.PlotStore;
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
 * ==============================================================
 * SettingsGUI
 * --------------------------------------------------------------
 *  - Unified settings menu for AegisGuard
 *  - Toggles for PvP, containers, mobs, pets, entities, farms
 *  - Safe Zone master toggle (ON by default)
 *  - Personal sound toggle (per-player)
 *  - Language style selector (Old, Hybrid, Modern English)
 *  - Instant dynamic refresh, no reload required
 * ==============================================================
 */
public class SettingsGUI {

    private final AegisGuard plugin;

    public SettingsGUI(AegisGuard plugin) {
        this.plugin = plugin;
    }

    /* -----------------------------
     * Open Settings Menu
     * ----------------------------- */
    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, plugin.msg().get(player, "settings_menu_title"));

        // --- Sounds ---
        boolean globalEnabled = plugin.getConfig().getBoolean("sounds.global_enabled", true);
        if (!globalEnabled) {
            inv.setItem(10, createItem(
                    Material.BARRIER,
                    plugin.msg().get(player, "button_sounds_disabled_global"),
                    plugin.msg().getList(player, "sounds_toggle_global_disabled_lore")
            ));
        } else {
            boolean soundsEnabled = plugin.isSoundEnabled(player);
            inv.setItem(10, createItem(
                    soundsEnabled ? Material.NOTE_BLOCK : Material.BARRIER,
                    soundsEnabled ? plugin.msg().get(player, "button_sounds_on") : plugin.msg().get(player, "button_sounds_off"),
                    plugin.msg().getList(player, "sounds_toggle_lore")
            ));
        }

        // --- PvP Protection ---
        boolean pvp = plugin.protection().isPvPEnabled(player);
        inv.setItem(11, createItem(
                pvp ? Material.IRON_SWORD : Material.WOODEN_SWORD,
                pvp ? plugin.msg().get(player, "button_pvp_on") : plugin.msg().get(player, "button_pvp_off"),
                plugin.msg().getList(player, "pvp_toggle_lore")
        ));

        // --- Container Protection ---
        boolean containers = plugin.protection().isContainersEnabled(player);
        inv.setItem(12, createItem(
                containers ? Material.CHEST : Material.TRAPPED_CHEST,
                containers ? plugin.msg().get(player, "button_containers_on") : plugin.msg().get(player, "button_containers_off"),
                plugin.msg().getList(player, "container_toggle_lore")
        ));

        // --- Mob Protection ---
        boolean mobs = plugin.protection().isMobProtectionEnabled(player);
        inv.setItem(13, createItem(
                mobs ? Material.ZOMBIE_HEAD : Material.ROTTEN_FLESH,
                mobs ? plugin.msg().get(player, "button_mobs_on") : plugin.msg().get(player, "button_mobs_off"),
                plugin.msg().getList(player, "mob_toggle_lore")
        ));

        // --- Pet Protection ---
        boolean pets = plugin.protection().isPetProtectionEnabled(player);
        inv.setItem(14, createItem(
                pets ? Material.BONE : Material.LEAD,
                pets ? plugin.msg().get(player, "button_pets_on") : plugin.msg().get(player, "button_pets_off"),
                plugin.msg().getList(player, "pet_toggle_lore")
        ));

        // --- Entity Protection ---
        boolean entity = plugin.protection().isEntityProtectionEnabled(player);
        inv.setItem(15, createItem(
                entity ? Material.ARMOR_STAND : Material.ITEM_FRAME,
                entity ? plugin.msg().get(player, "button_entity_on") : plugin.msg().get(player, "button_entity_off"),
                plugin.msg().getList(player, "entity_toggle_lore")
        ));

        // --- Farm Protection ---
        boolean farm = plugin.protection().isFarmProtectionEnabled(player);
        inv.setItem(16, createItem(
                farm ? Material.WHEAT : Material.WHEAT_SEEDS,
                farm ? plugin.msg().get(player, "button_farm_on") : plugin.msg().get(player, "button_farm_off"),
                plugin.msg().getList(player, "farm_toggle_lore")
        ));

        // --- Safe Zone (master switch) ---
        boolean safe = isSafeZoneEnabled(player);
        inv.setItem(17, createItem(
                safe ? Material.SHIELD : Material.IRON_NUGGET,
                safe ? plugin.msg().get(player, "button_safe_on") : plugin.msg().get(player, "button_safe_off"),
                plugin.msg().getList(player, "safe_toggle_lore")
        ));

        /* -----------------------------
         * Language Style Selection
         * ----------------------------- */
        String currentStyle = plugin.msg().getPlayerStyle(player);
        Material icon = switch (currentStyle) {
            case "modern_english" -> Material.BOOK;
            case "hybrid_english" -> Material.ENCHANTED_BOOK;
            default -> Material.WRITABLE_BOOK;
        };

        inv.setItem(31, createItem(
                icon,
                "§b🕮 " + plugin.msg().get(player, "language_style_title").replace("{STYLE}", formatStyle(currentStyle)),
                plugin.msg().getList(player, "language_style_lore")
        ));

        // Navigation
        inv.setItem(48, createItem(
                Material.ARROW,
                plugin.msg().get(player, "button_back"),
                plugin.msg().getList(player, "back_lore")
        ));

        inv.setItem(49, createItem(
                Material.BARRIER,
                plugin.msg().get(player, "button_exit"),
                plugin.msg().getList(player, "exit_lore")
        ));

        player.openInventory(inv);
        if (plugin.sounds() != null) plugin.sounds().playMenuFlip(player);
    }

    /* -----------------------------
     * Handle Clicks
     * ----------------------------- */
    public void handleClick(Player player, InventoryClickEvent e) {
        e.setCancelled(true);
        if (e.getCurrentItem() == null) return;

        Material type = e.getCurrentItem().getType();

        switch (type) {
            // Sounds
            case NOTE_BLOCK, BARRIER -> {
                boolean currentlyEnabled = plugin.isSoundEnabled(player);
                plugin.getConfig().set("sounds.players." + player.getUniqueId(), !currentlyEnabled);
                plugin.saveConfig();
            }

            // Toggles
            case IRON_SWORD, WOODEN_SWORD -> plugin.protection().togglePvP(player);
            case CHEST, TRAPPED_CHEST -> plugin.protection().toggleContainers(player);
            case ZOMBIE_HEAD, ROTTEN_FLESH -> plugin.protection().toggleMobProtection(player);
            case BONE, LEAD -> plugin.protection().togglePetProtection(player);
            case ARMOR_STAND, ITEM_FRAME -> plugin.protection().toggleEntityProtection(player);
            case WHEAT, WHEAT_SEEDS -> plugin.protection().toggleFarmProtection(player);

            // Safe Zone master toggle (handled directly here)
            case SHIELD, IRON_NUGGET -> toggleSafeZone(player);

            // Language Style Cycle
            case BOOK, ENCHANTED_BOOK, WRITABLE_BOOK -> {
                String current = plugin.msg().getPlayerStyle(player);
                String next = switch (current) {
                    case "old_english" -> "hybrid_english";
                    case "hybrid_english" -> "modern_english";
                    default -> "old_english";
                };
                plugin.msg().setPlayerStyle(player, next);
            }

            // Navigation
            case ARROW -> plugin.gui().openMain(player);
            case BARRIER -> {
                player.closeInventory();
                if (plugin.sounds() != null) plugin.sounds().playMenuClose(player);
                return;
            }
        }

        open(player); // Refresh GUI instantly
    }

    /* -----------------------------
     * Helpers
     * ----------------------------- */
    private ItemStack createItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }

    private String formatStyle(String style) {
        return switch (style) {
            case "modern_english" -> plugin.msg().color("&aModern English");
            case "hybrid_english" -> plugin.msg().color("&eHybrid English");
            default -> plugin.msg().color("&dOld English");
        };
    }

    private boolean isSafeZoneEnabled(Player player) {
        PlotStore.Plot plot = plugin.store().getPlotAt(player.getLocation());
        return plot != null && plot.getFlag("safe_zone", true);
    }

    private void toggleSafeZone(Player player) {
        PlotStore.Plot plot = plugin.store().getPlotAt(player.getLocation());
        if (plot == null) {
            plugin.msg().send(player, "no_plot_here");
            if (plugin.sounds() != null) plugin.sounds().playMenuClose(player);
            return;
        }
        boolean next = !plot.getFlag("safe_zone", true);
        plot.setFlag("safe_zone", next);

        // When toggling Safe Zone ON, also ensure the individual protections are ON
        if (next) {
            plot.setFlag("pvp", true);
            plot.setFlag("mobs", true);
            plot.setFlag("containers", true);
            plot.setFlag("entities", true);
            plot.setFlag("pets", true);
            plot.setFlag("farm", true);
        }

        plugin.store().flushSync();
        plugin.msg().send(player, next ? "safe_zone_enabled" : "safe_zone_disabled");
        if (plugin.sounds() != null) plugin.sounds().playMenuFlip(player);
    }
}
