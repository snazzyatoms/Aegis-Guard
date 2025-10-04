package com.aegisguard.gui;

import com.aegisguard.AegisGuard;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

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
                    plugin.msg().getList("sounds_toggle_global_disabled_lore")
            ));
        } else {
            boolean soundsEnabled = plugin.isSoundEnabled(player);
            inv.setItem(10, createItem(
                    soundsEnabled ? Material.NOTE_BLOCK : Material.BARRIER,
                    soundsEnabled ? plugin.msg().get(player, "button_sounds_on") : plugin.msg().get(player, "button_sounds_off"),
                    plugin.msg().getList("sounds_toggle_lore")
            ));
        }

        // --- PvP Protection ---
        boolean pvp = plugin.protection().isPvPEnabled(player);
        inv.setItem(11, createItem(
                pvp ? Material.IRON_SWORD : Material.WOODEN_SWORD,
                pvp ? plugin.msg().get(player, "button_pvp_on") : plugin.msg().get(player, "button_pvp_off"),
                plugin.msg().getList("pvp_toggle_lore")
        ));

        // --- Container Protection ---
        boolean containers = plugin.protection().isContainersEnabled(player);
        inv.setItem(12, createItem(
                containers ? Material.CHEST : Material.TRAPPED_CHEST,
                containers ? plugin.msg().get(player, "button_containers_on") : plugin.msg().get(player, "button_containers_off"),
                plugin.msg().getList("container_toggle_lore")
        ));

        // --- Mob Protection ---
        boolean mobs = plugin.protection().isMobProtectionEnabled(player);
        inv.setItem(13, createItem(
                mobs ? Material.ZOMBIE_HEAD : Material.ROTTEN_FLESH,
                mobs ? plugin.msg().get(player, "button_mobs_on") : plugin.msg().get(player, "button_mobs_off"),
                plugin.msg().getList("mob_toggle_lore")
        ));

        // --- Pet Protection ---
        boolean pets = plugin.protection().isPetProtectionEnabled(player);
        inv.setItem(14, createItem(
                pets ? Material.BONE : Material.LEAD,
                pets ? plugin.msg().get(player, "button_pets_on") : plugin.msg().get(player, "button_pets_off"),
                plugin.msg().getList("pet_toggle_lore")
        ));

        // --- Entity Protection ---
        boolean entity = plugin.protection().isEntityProtectionEnabled(player);
        inv.setItem(15, createItem(
                entity ? Material.ARMOR_STAND : Material.ITEM_FRAME,
                entity ? plugin.msg().get(player, "button_entity_on") : plugin.msg().get(player, "button_entity_off"),
                plugin.msg().getList("entity_toggle_lore")
        ));

        // --- Farm Protection ---
        boolean farm = plugin.protection().isFarmProtectionEnabled(player);
        inv.setItem(16, createItem(
                farm ? Material.WHEAT : Material.WHEAT_SEEDS,
                farm ? plugin.msg().get(player, "button_farm_on") : plugin.msg().get(player, "button_farm_off"),
                plugin.msg().getList("farm_toggle_lore")
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
                "§b🕮 Language Style: §f" + formatStyle(currentStyle),
                plugin.msg().getList("language_style_lore")
        ));

        // Navigation
        inv.setItem(48, createItem(
                Material.ARROW,
                plugin.msg().get(player, "button_back"),
                plugin.msg().getList("back_lore")
        ));

        inv.setItem(49, createItem(
                Material.BARRIER,
                plugin.msg().get(player, "button_exit"),
                plugin.msg().getList("exit_lore")
        ));

        player.openInventory(inv);
        plugin.sounds().playMenuFlip(player);
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
                plugin.sounds().playMenuClose(player);
                return;
            }
        }

        open(player); // refresh GUI instantly
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
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private String formatStyle(String style) {
        return switch (style) {
            case "modern_english" -> "§aModern English";
            case "hybrid_english" -> "§eHybrid English";
            default -> "§dOld English";
        };
    }
}
