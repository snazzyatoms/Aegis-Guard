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

/**
 * GUIManager
 * - Guardian Codex main menu hub
 * - Access to claim tools, trusted players, and settings
 * - Fully synced with messages.yml for customization
 * - Player & claim-based protection toggles
 * - Admin controls visible only to admins
 */
public class GUIManager {

    private final AegisGuard plugin;
    private final TrustedGUI trustedGUI;

    public GUIManager(AegisGuard plugin) {
        this.plugin = plugin;
        this.trustedGUI = new TrustedGUI(plugin);
    }

    /* -----------------------------
     * Open Main Menu
     * ----------------------------- */
    public void openMain(Player player) {
        String title = plugin.msg().get("menu_title");
        Inventory inv = Bukkit.createInventory(null, 27, title);

        inv.setItem(11, createItem(
                Material.LIGHTNING_ROD,
                plugin.msg().get("button_claim_land"),
                plugin.msg().getList("claim_land_lore")
        ));

        inv.setItem(13, createItem(
                Material.PLAYER_HEAD,
                plugin.msg().get("button_trusted_players"),
                plugin.msg().getList("trusted_players_lore")
        ));

        inv.setItem(15, createItem(
                Material.REDSTONE_COMPARATOR,
                plugin.msg().get("button_settings"),
                plugin.msg().getList("settings_lore")
        ));

        inv.setItem(22, createItem(
                Material.WRITABLE_BOOK,
                plugin.msg().get("button_info"),
                plugin.msg().getList("info_lore")
        ));

        inv.setItem(26, createItem(
                Material.BARRIER,
                plugin.msg().get("button_exit"),
                plugin.msg().getList("exit_lore")
        ));

        player.openInventory(inv);
        plugin.sounds().playMenuOpen(player);
    }

    /* -----------------------------
     * Open Settings Menu
     * ----------------------------- */
    public void openSettings(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, plugin.msg().get("settings_menu_title"));

        // --- Sounds ---
        boolean globalEnabled = plugin.getConfig().getBoolean("sounds.global_enabled", true);
        if (!globalEnabled) {
            inv.setItem(10, createItem(
                    Material.BARRIER,
                    plugin.msg().get("button_sounds_disabled_global"),
                    plugin.msg().getList("sounds_toggle_global_disabled_lore")
            ));
        } else {
            boolean soundsEnabled = plugin.isSoundEnabled(player);
            inv.setItem(10, createItem(
                    soundsEnabled ? Material.NOTE_BLOCK : Material.BARRIER,
                    soundsEnabled ? plugin.msg().get("button_sounds_on") : plugin.msg().get("button_sounds_off"),
                    plugin.msg().getList("sounds_toggle_lore")
            ));
        }

        // --- PvP Protection ---
        boolean pvp = plugin.protection().isPvPEnabled(player);
        inv.setItem(11, createItem(
                pvp ? Material.IRON_SWORD : Material.WOODEN_SWORD,
                pvp ? plugin.msg().get("button_pvp_on") : plugin.msg().get("button_pvp_off"),
                plugin.msg().getList("pvp_toggle_lore")
        ));

        // --- Container Protection ---
        boolean containers = plugin.protection().isContainersEnabled(player);
        inv.setItem(12, createItem(
                containers ? Material.CHEST : Material.TRAPPED_CHEST,
                containers ? plugin.msg().get("button_containers_on") : plugin.msg().get("button_containers_off"),
                plugin.msg().getList("container_toggle_lore")
        ));

        // --- Mob Protection ---
        boolean mobs = plugin.protection().isMobProtectionEnabled(player);
        inv.setItem(13, createItem(
                mobs ? Material.ZOMBIE_HEAD : Material.ROTTEN_FLESH,
                mobs ? plugin.msg().get("button_mobs_on") : plugin.msg().get("button_mobs_off"),
                plugin.msg().getList("mob_toggle_lore")
        ));

        // --- Pet Protection ---
        boolean pets = plugin.protection().isPetProtectionEnabled(player);
        inv.setItem(14, createItem(
                pets ? Material.BONE : Material.LEAD,
                pets ? plugin.msg().get("button_pets_on") : plugin.msg().get("button_pets_off"),
                plugin.msg().getList("pet_toggle_lore")
        ));

        // --- Entity Damage Protection ---
        boolean entity = plugin.protection().isEntityProtectionEnabled(player);
        inv.setItem(15, createItem(
                entity ? Material.ARMOR_STAND : Material.ITEM_FRAME,
                entity ? plugin.msg().get("button_entity_on") : plugin.msg().get("button_entity_off"),
                plugin.msg().getList("entity_toggle_lore")
        ));

        // --- Farm Protection ---
        boolean farm = plugin.protection().isFarmProtectionEnabled(player);
        inv.setItem(16, createItem(
                farm ? Material.WHEAT : Material.WHEAT_SEEDS,
                farm ? plugin.msg().get("button_farm_on") : plugin.msg().get("button_farm_off"),
                plugin.msg().getList("farm_toggle_lore")
        ));

        // --- Admin Toggles (visible only to admins) ---
        if (player.hasPermission("aegisguard.admin")) {
            boolean autoRemove = plugin.getConfig().getBoolean("admin.auto_remove_banned", false);
            inv.setItem(20, createItem(
                    Material.COMMAND_BLOCK,
                    autoRemove ? "§aAuto-Remove Banned: ON" : "§cAuto-Remove Banned: OFF",
                    List.of("§7Automatically removes plots of banned players", autoRemove ? "§aCurrently Enabled" : "§cCurrently Disabled")
            ));

            boolean bypass = plugin.getConfig().getBoolean("admin.bypass_claim_limit", false);
            inv.setItem(21, createItem(
                    Material.BEDROCK,
                    bypass ? "§aBypass Claim Limit: ON" : "§cBypass Claim Limit: OFF",
                    List.of("§7Admins bypass claim limits", bypass ? "§aCurrently Enabled" : "§cCurrently Disabled")
            ));
        }

        // Navigation
        inv.setItem(48, createItem(
                Material.ARROW,
                plugin.msg().get("button_back"),
                plugin.msg().getList("back_lore")
        ));

        inv.setItem(49, createItem(
                Material.BARRIER,
                plugin.msg().get("button_exit"),
                plugin.msg().getList("exit_lore")
        ));

        player.openInventory(inv);
        plugin.sounds().playMenuFlip(player);
    }

    /* -----------------------------
     * Handle Menu Clicks
     * ----------------------------- */
    public void handleClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (e.getClickedInventory() == null || e.getCurrentItem() == null) return;

        String title = e.getView().getTitle();
        Material type = e.getCurrentItem().getType();

        // Main menu
        if (title.equals(plugin.msg().get("menu_title"))) {
            e.setCancelled(true);
            switch (type) {
                case LIGHTNING_ROD -> {
                    player.closeInventory();
                    plugin.selection().confirmClaim(player);
                    plugin.sounds().playMenuFlip(player);
                }
                case PLAYER_HEAD -> {
                    trustedGUI.open(player);
                    plugin.sounds().playMenuFlip(player);
                }
                case REDSTONE_COMPARATOR -> openSettings(player);
                case WRITABLE_BOOK -> {
                    player.sendMessage(plugin.msg().get("info_message"));
                    plugin.sounds().playMenuFlip(player);
                }
                case BARRIER -> {
                    player.closeInventory();
                    plugin.sounds().playMenuClose(player);
                }
            }
        }

        // Settings menu
        else if (title.equals(plugin.msg().get("settings_menu_title"))) {
            e.setCancelled(true);

            boolean globalEnabled = plugin.getConfig().getBoolean("sounds.global_enabled", true);

            // Block sound button if globally disabled
            if (!globalEnabled && type == Material.BARRIER) return;

            switch (type) {
                case NOTE_BLOCK, BARRIER -> {
                    boolean currentlyEnabled = plugin.isSoundEnabled(player);
                    plugin.getConfig().set("sounds.players." + player.getUniqueId(), !currentlyEnabled);
                    plugin.saveConfig();
                    openSettings(player);
                }
                case IRON_SWORD, WOODEN_SWORD -> {
                    plugin.protection().togglePvP(player);
                    openSettings(player);
                }
                case CHEST, TRAPPED_CHEST -> {
                    plugin.protection().toggleContainers(player);
                    openSettings(player);
                }
                case ZOMBIE_HEAD, ROTTEN_FLESH -> {
                    plugin.protection().toggleMobProtection(player);
                    openSettings(player);
                }
                case BONE, LEAD -> {
                    plugin.protection().togglePetProtection(player);
                    openSettings(player);
                }
                case ARMOR_STAND, ITEM_FRAME -> {
                    plugin.protection().toggleEntityProtection(player);
                    openSettings(player);
                }
                case WHEAT, WHEAT_SEEDS -> {
                    plugin.protection().toggleFarmProtection(player);
                    openSettings(player);
                }
                case COMMAND_BLOCK -> {
                    if (player.hasPermission("aegisguard.admin")) {
                        boolean current = plugin.getConfig().getBoolean("admin.auto_remove_banned", false);
                        plugin.getConfig().set("admin.auto_remove_banned", !current);
                        plugin.saveConfig();
                        plugin.msg().send(player, !current ? "admin_auto_remove_enabled" : "admin_auto_remove_disabled");
                        if (!current) {
                            plugin.sounds().playMenuFlip(player);
                        } else {
                            plugin.sounds().playMenuClose(player);
                        }
                        openSettings(player);
                    }
                }
                case BEDROCK -> {
                    if (player.hasPermission("aegisguard.admin")) {
                        boolean current = plugin.getConfig().getBoolean("admin.bypass_claim_limit", false);
                        plugin.getConfig().set("admin.bypass_claim_limit", !current);
                        plugin.saveConfig();
                        plugin.msg().send(player, !current ? "admin_bypass_enabled" : "admin_bypass_disabled");
                        if (!current) {
                            plugin.sounds().playMenuFlip(player);
                        } else {
                            plugin.sounds().playMenuClose(player);
                        }
                        openSettings(player);
                    }
                }
                case ARROW -> openMain(player);
                case BARRIER -> {
                    player.closeInventory();
                    plugin.sounds().playMenuClose(player);
                }
            }
        }
    }

    /* -----------------------------
     * Helper: Build Icon
     * ----------------------------- */
    public static ItemStack icon(Material mat, String name, List<String> lore) {
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

    private ItemStack createItem(Material mat, String name, List<String> lore) {
        return icon(mat, name, lore);
    }
}
