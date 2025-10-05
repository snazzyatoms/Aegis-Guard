package com.aegisguard.gui;

import com.aegisguard.AegisGuard;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * SettingsGUI
 *  - Toggles for PvP, containers, mobs, pets, entities, farms
 *  - Per-player sound toggle (respects global switch)
 *  - Language style selector (Old / Hybrid / Modern English)
 *  - Back/Exit navigation
 */
public class SettingsGUI {

    private final AegisGuard plugin;

    // Slots we rely on (prevents BARRIER conflicts)
    private static final int SLOT_SOUND = 10;
    private static final int SLOT_BACK  = 48;
    private static final int SLOT_EXIT  = 49;
    private static final int SLOT_LANG  = 31;

    public SettingsGUI(AegisGuard plugin) {
        this.plugin = plugin;
    }

    /* -----------------------------
     * Open Settings Menu
     * ----------------------------- */
    public void open(Player player) {
        String title = m(player, "settings_menu_title", "§b§lAegisGuard §7— Settings");
        Inventory inv = Bukkit.createInventory(null, 54, title);

        // --- Sounds (per-player, respects global) ---
        boolean globalEnabled = plugin.getConfig().getBoolean("sounds.global_enabled", true);
        if (!globalEnabled) {
            inv.setItem(SLOT_SOUND, createItem(
                    Material.BARRIER,
                    m(player, "button_sounds_disabled_global", "§7Sounds disabled by admin"),
                    l(player, "sounds_toggle_global_disabled_lore", List.of("§7Global sounds are off."))));
        } else {
            boolean soundsEnabled = plugin.isSoundEnabled(player);
            inv.setItem(SLOT_SOUND, createItem(
                    soundsEnabled ? Material.NOTE_BLOCK : Material.BARRIER,
                    soundsEnabled ? m(player, "button_sounds_on",  "§aSounds: ON")
                                  : m(player, "button_sounds_off", "§eSounds: OFF"),
                    l(player, "sounds_toggle_lore", List.of("§7Click to toggle your sounds."))));
        }

        // --- PvP Protection ---
        boolean pvp = plugin.protection().isPvPEnabled(player);
        inv.setItem(11, createItem(
                pvp ? Material.IRON_SWORD : Material.WOODEN_SWORD,
                pvp ? m(player, "button_pvp_on",  "§aPvP: ON")
                    : m(player, "button_pvp_off", "§ePvP: OFF"),
                l(player, "pvp_toggle_lore", List.of("§7Toggle player-vs-player in your claims."))));

        // --- Container Protection ---
        boolean containers = plugin.protection().isContainersEnabled(player);
        inv.setItem(12, createItem(
                containers ? Material.CHEST : Material.TRAPPED_CHEST,
                containers ? m(player, "button_containers_on",  "§aContainers: ON")
                           : m(player, "button_containers_off", "§eContainers: OFF"),
                l(player, "container_toggle_lore", List.of("§7Toggle access to chests & containers."))));

        // --- Mob Protection ---
        boolean mobs = plugin.protection().isMobProtectionEnabled(player);
        inv.setItem(13, createItem(
                mobs ? Material.ZOMBIE_HEAD : Material.ROTTEN_FLESH,
                mobs ? m(player, "button_mobs_on",  "§aMob Damage: ON")
                     : m(player, "button_mobs_off", "§eMob Damage: OFF"),
                l(player, "mob_toggle_lore", List.of("§7Toggle hostile mob griefing/damage."))));

        // --- Pet Protection ---
        boolean pets = plugin.protection().isPetProtectionEnabled(player);
        inv.setItem(14, createItem(
                pets ? Material.BONE : Material.LEAD,
                pets ? m(player, "button_pets_on",  "§aPet Safety: ON")
                     : m(player, "button_pets_off", "§ePet Safety: OFF"),
                l(player, "pet_toggle_lore", List.of("§7Protect tamed animals from harm."))));

        // --- Entity Protection ---
        boolean entity = plugin.protection().isEntityProtectionEnabled(player);
        inv.setItem(15, createItem(
                entity ? Material.ARMOR_STAND : Material.ITEM_FRAME,
                entity ? m(player, "button_entity_on",  "§aEntities: ON")
                       : m(player, "button_entity_off", "§eEntities: OFF"),
                l(player, "entity_toggle_lore", List.of("§7Protect item frames, armor stands, etc."))));

        // --- Farm Protection ---
        boolean farm = plugin.protection().isFarmProtectionEnabled(player);
        inv.setItem(16, createItem(
                farm ? Material.WHEAT : Material.WHEAT_SEEDS,
                farm ? m(player, "button_farm_on",  "§aFarm: ON")
                     : m(player, "button_farm_off", "§eFarm: OFF"),
                l(player, "farm_toggle_lore", List.of("§7Prevent trampling/breaking crops."))));

        /* -----------------------------
         * Language Style Selection
         * ----------------------------- */
        String currentStyle = plugin.msg().getPlayerStyle(player); // expects: old_english | hybrid_english | modern_english
        Material langIcon = switch (currentStyle) {
            case "modern_english" -> Material.BOOK;
            case "hybrid_english" -> Material.ENCHANTED_BOOK;
            default -> Material.WRITABLE_BOOK; // old_english
        };

        inv.setItem(SLOT_LANG, createItem(
                langIcon,
                "§b🕮 Language Style: §f" + formatStyle(currentStyle),
                List.of(
                        "§7Click to switch between:",
                        "§dOld English §7(Immersive)",
                        "§eHybrid English §7(Blended)",
                        "§aModern English §7(Standard)"
                )
        ));

        // Navigation
        inv.setItem(SLOT_BACK, createItem(
                Material.ARROW,
                m(player, "button_back", "§7Back"),
                l(player, "back_lore", List.of("§7Return to main menu."))));

        inv.setItem(SLOT_EXIT, createItem(
                Material.BARRIER,
                m(player, "button_exit", "§cExit"),
                l(player, "exit_lore", List.of("§7Close this menu."))));

        player.openInventory(inv);
        playFlip(player);
    }

    /* -----------------------------
     * Handle Clicks
     * ----------------------------- */
    public void handleClick(Player player, InventoryClickEvent e) {
        e.setCancelled(true);
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;

        final int slot = e.getSlot();
        final Material type = clicked.getType();

        // Sounds tile (slot-specific to avoid BARRIER conflict)
        if (slot == SLOT_SOUND) {
            boolean globalEnabled = plugin.getConfig().getBoolean("sounds.global_enabled", true);
            if (!globalEnabled) {
                player.sendMessage(m(player, "sounds_disabled_global_msg", "§eSounds are disabled by the server."));
                playClose(player);
            } else {
                boolean currentlyEnabled = plugin.isSoundEnabled(player);
                plugin.getConfig().set("sounds.players." + player.getUniqueId(), !currentlyEnabled);
                plugin.saveConfig();
                playFlip(player);
            }
            open(player);
            return;
        }

        // Language style cycle (slot-specific)
        if (slot == SLOT_LANG && (type == Material.BOOK || type == Material.ENCHANTED_BOOK || type == Material.WRITABLE_BOOK)) {
            String current = plugin.msg().getPlayerStyle(player);
            String next = switch (current) {
                case "old_english" -> "hybrid_english";
                case "hybrid_english" -> "modern_english";
                default -> "old_english";
            };
            plugin.msg().setPlayerStyle(player, next);
            playFlip(player);
            open(player);
            return;
        }

        // Navigation
        if (slot == SLOT_BACK && type == Material.ARROW) {
            plugin.gui().openMain(player);
            playFlip(player);
            return;
        }
        if (slot == SLOT_EXIT && type == Material.BARRIER) {
            player.closeInventory();
            playClose(player);
            return;
        }

        // Protection toggles (type-based is fine here)
        switch (type) {
            case IRON_SWORD, WOODEN_SWORD -> plugin.protection().togglePvP(player);
            case CHEST, TRAPPED_CHEST ->     plugin.protection().toggleContainers(player);
            case ZOMBIE_HEAD, ROTTEN_FLESH ->plugin.protection().toggleMobProtection(player);
            case BONE, LEAD ->               plugin.protection().togglePetProtection(player);
            case ARMOR_STAND, ITEM_FRAME ->  plugin.protection().toggleEntityProtection(player);
            case WHEAT, WHEAT_SEEDS ->       plugin.protection().toggleFarmProtection(player);
            default -> { return; }
        }

        playFlip(player);
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
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
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

    private String m(Player p, String key, String fallback) {
        String v = plugin.msg().get(p, key);
        return (v == null || v.isEmpty()) ? fallback : v;
    }

    private List<String> l(Player p, String key, List<String> fallback) {
        List<String> v = plugin.msg().getList(p, key);
        return (v == null || v.isEmpty()) ? fallback : v;
    }

    /* Sounds (no external manager required) */
    private void playOpen(Player p) { if (plugin.isSoundEnabled(p)) p.playSound(p.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.7f, 1.0f); }
    private void playClose(Player p){ if (plugin.isSoundEnabled(p)) p.playSound(p.getLocation(), Sound.BLOCK_CHEST_CLOSE, 0.7f, 1.0f); }
    private void playFlip(Player p) { if (plugin.isSoundEnabled(p)) p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f); }
}
