package com.aegisguard.gui;

import com.aegisguard.AegisGuard;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
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

    public void open(Player player) {
        String world = player.getWorld().getName();
        Inventory inv = Bukkit.createInventory(null, 54, plugin.msg().get("settings_menu_title"));

        // --- Sounds ---
        boolean globalEnabled = plugin.getConfig().getBoolean("sounds.global_enabled", true);
        if (!globalEnabled) {
            inv.setItem(10, createItem(Material.BARRIER,
                    plugin.msg().get("button_sounds_disabled_global"),
                    plugin.msg().getList("sounds_toggle_global_disabled_lore")));
        } else {
            boolean soundsEnabled = plugin.isSoundEnabled(player);
            inv.setItem(10, createItem(
                    soundsEnabled ? Material.NOTE_BLOCK : Material.BARRIER,
                    soundsEnabled ? plugin.msg().get("button_sounds_on") : plugin.msg().get("button_sounds_off"),
                    plugin.msg().getList("sounds_toggle_lore")
            ));
        }

        // --- Per-world protections ---
        java.util.function.BiPredicate<String, String> worldEnabled = (worldName, key) -> {
            if (plugin.getConfig().isSet("claims.per_world." + worldName + ".protections." + key)) {
                return plugin.getConfig().getBoolean("claims.per_world." + worldName + ".protections." + key);
            }
            return plugin.getConfig().getBoolean("protections." + key, true);
        };

        if (worldEnabled.test(world, "pvp_protection")) {
            inv.setItem(11, toggleIcon(player, plugin.protection().isPvPEnabled(player),
                    Material.IRON_SWORD, Material.WOODEN_SWORD,
                    "button_pvp_on", "button_pvp_off", "pvp_toggle_lore"));
        }

        if (worldEnabled.test(world, "container_protection")) {
            inv.setItem(12, toggleIcon(player, plugin.protection().isContainersEnabled(player),
                    Material.CHEST, Material.TRAPPED_CHEST,
                    "button_containers_on", "button_containers_off", "container_toggle_lore"));
        }

        if (worldEnabled.test(world, "mobs_protection")) {
            inv.setItem(13, toggleIcon(player, plugin.protection().isMobProtectionEnabled(player),
                    Material.ZOMBIE_HEAD, Material.ROTTEN_FLESH,
                    "button_mobs_on", "button_mobs_off", "mob_toggle_lore"));
        }

        if (worldEnabled.test(world, "pets_protection")) {
            inv.setItem(14, toggleIcon(player, plugin.protection().isPetProtectionEnabled(player),
                    Material.BONE, Material.LEAD,
                    "button_pets_on", "button_pets_off", "pet_toggle_lore"));
        }

        if (worldEnabled.test(world, "entities_protection")) {
            inv.setItem(15, toggleIcon(player, plugin.protection().isEntityProtectionEnabled(player),
                    Material.ARMOR_STAND, Material.ITEM_FRAME,
                    "button_entity_on", "button_entity_off", "entity_toggle_lore"));
        }

        if (worldEnabled.test(world, "farm_protection")) {
            inv.setItem(16, toggleIcon(player, plugin.protection().isFarmProtectionEnabled(player),
                    Material.WHEAT, Material.WHEAT_SEEDS,
                    "button_farm_on", "button_farm_off", "farm_toggle_lore"));
        }

        // Navigation
        inv.setItem(48, createItem(Material.ARROW, plugin.msg().get("button_back"), plugin.msg().getList("back_lore")));
        inv.setItem(49, createItem(Material.BARRIER, plugin.msg().get("button_exit"), plugin.msg().getList("exit_lore")));

        player.openInventory(inv);
        plugin.sounds().playMenuFlip(player);
    }

    private ItemStack toggleIcon(Player player, boolean state, Material onMat, Material offMat,
                                 String onKey, String offKey, String loreKey) {
        return createItem(state ? onMat : offMat,
                state ? plugin.msg().get(onKey) : plugin.msg().get(offKey),
                plugin.msg().getList(loreKey));
    }

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
}
