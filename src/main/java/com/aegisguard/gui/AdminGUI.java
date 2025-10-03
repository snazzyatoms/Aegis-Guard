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

public class AdminGUI {

    private final AegisGuard plugin;

    public AdminGUI(AegisGuard plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, plugin.msg().get("admin_menu_title"));

        boolean autoRemove = plugin.getConfig().getBoolean("admin.auto_remove_banned", false);
        inv.setItem(10, createItem(
                autoRemove ? Material.TNT : Material.GUNPOWDER,
                plugin.msg().get("button_admin_auto_remove"),
                plugin.msg().getList("admin_auto_remove_lore")
        ));

        boolean bypassLimit = plugin.getConfig().getBoolean("admin.bypass_claim_limit", false);
        inv.setItem(12, createItem(
                bypassLimit ? Material.NETHER_STAR : Material.REDSTONE,
                plugin.msg().get("button_admin_bypass_limit"),
                plugin.msg().getList("admin_bypass_limit_lore")
        ));

        boolean broadcast = plugin.getConfig().getBoolean("admin.broadcast_admin_actions", false);
        inv.setItem(14, createItem(
                broadcast ? Material.BEACON : Material.ENDER_EYE,
                plugin.msg().get("button_admin_broadcast"),
                plugin.msg().getList("admin_broadcast_lore")
        ));

        // Navigation
        inv.setItem(22, createItem(Material.ARROW, plugin.msg().get("button_back"), plugin.msg().getList("back_lore")));

        player.openInventory(inv);
        plugin.sounds().playMenuOpen(player);
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
