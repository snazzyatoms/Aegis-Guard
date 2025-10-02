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
 * - Polished with filler panes, admin-only buttons, and ghost-click prevention
 */
public class GUIManager {

    private final AegisGuard plugin;
    private final TrustedGUI trustedGUI;

    // Slot constants for clean layout
    private static final int SLOT_CLAIM = 11;
    private static final int SLOT_TRUSTED = 13;
    private static final int SLOT_SETTINGS = 15;
    private static final int SLOT_INFO = 22;
    private static final int SLOT_EXIT = 26;

    // Settings menu slots
    private static final int SLOT_SOUNDS = 10;
    private static final int SLOT_PVP = 11;
    private static final int SLOT_CONTAINERS = 12;
    private static final int SLOT_MOBS = 13;
    private static final int SLOT_PETS = 14;
    private static final int SLOT_ENTITY = 15;
    private static final int SLOT_FARM = 16;
    private static final int SLOT_BACK = 48;
    private static final int SLOT_EXIT_SETTINGS = 49;
    // Admin slots (reserved)
    private static final int SLOT_ADMIN1 = 31;
    private static final int SLOT_ADMIN2 = 32;

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

        fill(inv);

        inv.setItem(SLOT_CLAIM, createItem(
                Material.LIGHTNING_ROD,
                plugin.msg().get("button_claim_land"),
                plugin.msg().getList("claim_land_lore")
        ));

        inv.setItem(SLOT_TRUSTED, createItem(
                Material.PLAYER_HEAD,
                plugin.msg().get("button_trusted_players"),
                plugin.msg().getList("trusted_players_lore")
        ));

        inv.setItem(SLOT_SETTINGS, createItem(
                Material.REDSTONE_COMPARATOR,
                plugin.msg().get("button_settings"),
                plugin.msg().getList("settings_lore")
        ));

        inv.setItem(SLOT_INFO, createItem(
                Material.WRITABLE_BOOK,
                plugin.msg().get("button_info"),
                plugin.msg().getList("info_lore")
        ));

        inv.setItem(SLOT_EXIT, createItem(
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

        fill(inv);

        // --- Sounds ---
        boolean globalEnabled = plugin.getConfig().getBoolean("sounds.global_enabled", true);
        if (!globalEnabled) {
            inv.setItem(SLOT_SOUNDS, createItem(
                    Material.BARRIER,
                    plugin.msg().get("button_sounds_disabled_global"),
                    plugin.msg().getList("sounds_toggle_global_disabled_lore")
            ));
        } else {
            boolean soundsEnabled = plugin.isSoundEnabled(player);
            inv.setItem(SLOT_SOUNDS, createItem(
                    soundsEnabled ? Material.NOTE_BLOCK : Material.BARRIER,
                    soundsEnabled ? plugin.msg().get("button_sounds_on") : plugin.msg().get("button_sounds_off"),
                    plugin.msg().getList("sounds_toggle_lore")
            ));
        }

        // --- Protection Toggles ---
        inv.setItem(SLOT_PVP, createItem(
                plugin.protection().isPvPEnabled(player) ? Material.IRON_SWORD : Material.WOODEN_SWORD,
                plugin.protection().isPvPEnabled(player) ? plugin.msg().get("button_pvp_on") : plugin.msg().get("button_pvp_off"),
                plugin.msg().getList("pvp_toggle_lore")
        ));

        inv.setItem(SLOT_CONTAINERS, createItem(
                plugin.protection().isContainersEnabled(player) ? Material.CHEST : Material.TRAPPED_CHEST,
                plugin.protection().isContainersEnabled(player) ? plugin.msg().get("button_containers_on") : plugin.msg().get("button_containers_off"),
                plugin.msg().getList("container_toggle_lore")
        ));

        inv.setItem(SLOT_MOBS, createItem(
                plugin.protection().isMobProtectionEnabled(player) ? Material.ZOMBIE_HEAD : Material.ROTTEN_FLESH,
                plugin.protection().isMobProtectionEnabled(player) ? plugin.msg().get("button_mobs_on") : plugin.msg().get("button_mobs_off"),
                plugin.msg().getList("mob_toggle_lore")
        ));

        inv.setItem(SLOT_PETS, createItem(
                plugin.protection().isPetProtectionEnabled(player) ? Material.BONE : Material.LEAD,
                plugin.protection().isPetProtectionEnabled(player) ? plugin.msg().get("button_pets_on") : plugin.msg().get("button_pets_off"),
                plugin.msg().getList("pet_toggle_lore")
        ));

        inv.setItem(SLOT_ENTITY, createItem(
                plugin.protection().isEntityProtectionEnabled(player) ? Material.ARMOR_STAND : Material.ITEM_FRAME,
                plugin.protection().isEntityProtectionEnabled(player) ? plugin.msg().get("button_entity_on") : plugin.msg().get("button_entity_off"),
                plugin.msg().getList("entity_toggle_lore")
        ));

        inv.setItem(SLOT_FARM, createItem(
                plugin.protection().isFarmProtectionEnabled(player) ? Material.WHEAT : Material.WHEAT_SEEDS,
                plugin.protection().isFarmProtectionEnabled(player) ? plugin.msg().get("button_farm_on") : plugin.msg().get("button_farm_off"),
                plugin.msg().getList("farm_toggle_lore")
        ));

        // --- Admin Buttons ---
        if (player.hasPermission("aegisguard.admin")) {
            inv.setItem(SLOT_ADMIN1, createItem(
                    Material.COMMAND_BLOCK,
                    "&c[Admin] Auto-Remove Banned Players",
                    List.of("&7Toggles whether banned players' plots", "&7are auto-removed.")
            ));

            inv.setItem(SLOT_ADMIN2, createItem(
                    Material.BEDROCK,
                    "&c[Admin] Bypass Claim Limit",
                    List.of("&7Toggles whether admins bypass claim limits.")
            ));
        }

        // Navigation
        inv.setItem(SLOT_BACK, createItem(
                Material.ARROW,
                plugin.msg().get("button_back"),
                plugin.msg().getList("back_lore")
        ));

        inv.setItem(SLOT_EXIT_SETTINGS, createItem(
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

        e.setCancelled(true); // Always cancel
        Material type = e.getCurrentItem().getType();
        String title = e.getView().getTitle();

        // Main menu
        if (title.equals(plugin.msg().get("menu_title"))) {
            switch (type) {
                case LIGHTNING_ROD -> {
                    player.closeInventory();
                    plugin.selection().confirmClaim(player);
                    plugin.sounds().playMenuFlip(player);
                }
                case PLAYER_HEAD -> trustedGUI.open(player);
                case REDSTONE_COMPARATOR -> openSettings(player);
                case WRITABLE_BOOK -> player.sendMessage(plugin.msg().get("info_message"));
                case BARRIER -> {
                    player.closeInventory();
                    plugin.sounds().playMenuClose(player);
                }
            }
        }

        // Settings menu
        else if (title.equals(plugin.msg().get("settings_menu_title"))) {
            boolean globalEnabled = plugin.getConfig().getBoolean("sounds.global_enabled", true);

            switch (type) {
                case NOTE_BLOCK, BARRIER -> {
                    if (!globalEnabled) return;
                    boolean current = plugin.isSoundEnabled(player);
                    plugin.getConfig().set("sounds.players." + player.getUniqueId(), !current);
                    plugin.saveConfig();
                    openSettings(player);
                }
                case IRON_SWORD, WOODEN_SWORD -> plugin.protection().togglePvP(player);
                case CHEST, TRAPPED_CHEST -> plugin.protection().toggleContainers(player);
                case ZOMBIE_HEAD, ROTTEN_FLESH -> plugin.protection().toggleMobProtection(player);
                case BONE, LEAD -> plugin.protection().togglePetProtection(player);
                case ARMOR_STAND, ITEM_FRAME -> plugin.protection().toggleEntityProtection(player);
                case WHEAT, WHEAT_SEEDS -> plugin.protection().toggleFarmProtection(player);
                case ARROW -> openMain(player);
                case BARRIER -> {
                    player.closeInventory();
                    plugin.sounds().playMenuClose(player);
                }
                // Admin toggles
                case COMMAND_BLOCK -> {
                    if (player.hasPermission("aegisguard.admin")) {
                        boolean current = plugin.getConfig().getBoolean("admin.auto_remove_banned", false);
                        plugin.getConfig().set("admin.auto_remove_banned", !current);
                        plugin.saveConfig();
                        plugin.msg().send(player, "admin_removed_banned", "PLAYER", player.getName());
                        openSettings(player);
                    }
                }
                case BEDROCK -> {
                    if (player.hasPermission("aegisguard.admin")) {
                        boolean current = plugin.getConfig().getBoolean("admin.bypass_claim_limit", false);
                        plugin.getConfig().set("admin.bypass_claim_limit", !current);
                        plugin.saveConfig();
                        plugin.msg().send(player, "admin_bypass_limit");
                        openSettings(player);
                    }
                }
            }
        }
    }

    /* -----------------------------
     * Helpers
     * ----------------------------- */
    private void fill(Inventory inv) {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            filler.setItemMeta(meta);
        }
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) inv.setItem(i, filler);
        }
    }

    private ItemStack createItem(Material mat, String name, List<String> lore) {
        return icon(mat, name, lore);
    }

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
}
