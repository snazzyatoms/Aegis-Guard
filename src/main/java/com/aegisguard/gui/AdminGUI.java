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

public class AdminGUI {

    private final AegisGuard plugin;

    public AdminGUI(AegisGuard plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        if (!player.hasPermission("aegisguard.admin")) return;

        Inventory inv = Bukkit.createInventory(null, 27, plugin.msg().get("admin_menu_title"));

        inv.setItem(10, createItem(Material.TNT,
                plugin.msg().get("button_admin_auto_remove"),
                plugin.msg().getList("admin_auto_remove_lore")));

        inv.setItem(12, createItem(Material.NETHER_STAR,
                plugin.msg().get("button_admin_bypass_limit"),
                plugin.msg().getList("admin_bypass_limit_lore")));

        inv.setItem(14, createItem(Material.BEACON,
                plugin.msg().get("button_admin_broadcast"),
                plugin.msg().getList("admin_broadcast_lore")));

        inv.setItem(22, createItem(Material.BARRIER,
                plugin.msg().get("button_exit"),
                plugin.msg().getList("exit_lore")));

        player.openInventory(inv);
        plugin.sounds().playMenuOpen(player);
    }

    public void handleClick(Player player, InventoryClickEvent e) {
        Material type = e.getCurrentItem().getType();
        switch (type) {
            case TNT -> {
                boolean autoRemove = plugin.getConfig().getBoolean("admin.auto_remove_banned", false);
                plugin.getConfig().set("admin.auto_remove_banned", !autoRemove);
                plugin.saveConfig();
                player.sendMessage(plugin.msg().get(!autoRemove ? "admin_auto_remove_enabled" : "admin_auto_remove_disabled"));
            }
            case NETHER_STAR -> {
                boolean bypass = plugin.getConfig().getBoolean("admin.bypass_claim_limit", false);
                plugin.getConfig().set("admin.bypass_claim_limit", !bypass);
                plugin.saveConfig();
                player.sendMessage(plugin.msg().get(!bypass ? "admin_bypass_enabled" : "admin_bypass_disabled"));
            }
            case BEACON -> {
                boolean broadcast = plugin.getConfig().getBoolean("admin.broadcast_admin_actions", false);
                plugin.getConfig().set("admin.broadcast_admin_actions", !broadcast);
                plugin.saveConfig();
                player.sendMessage(plugin.msg().get(!broadcast ? "admin_broadcast_enabled" : "admin_broadcast_disabled"));
            }
            case BARRIER -> {
                player.closeInventory();
                plugin.sounds().playMenuClose(player);
            }
        }
        open(player); // refresh
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
