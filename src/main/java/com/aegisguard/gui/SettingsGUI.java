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

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, plugin.msg().get("settings_menu_title"));

        // Example PvP toggle
        boolean pvp = plugin.protection().isPvPEnabled(player);
        inv.setItem(11, createItem(
                pvp ? Material.IRON_SWORD : Material.WOODEN_SWORD,
                pvp ? plugin.msg().get("button_pvp_on") : plugin.msg().get("button_pvp_off"),
                plugin.msg().getList("pvp_toggle_lore")
        ));

        // ... (repeat for containers, mobs, pets, entities, farms, etc.)

        inv.setItem(48, createItem(Material.ARROW,
                plugin.msg().get("button_back"),
                plugin.msg().getList("back_lore")));

        inv.setItem(49, createItem(Material.BARRIER,
                plugin.msg().get("button_exit"),
                plugin.msg().getList("exit_lore")));

        player.openInventory(inv);
        plugin.sounds().playMenuFlip(player);
    }

    public void handleClick(Player player, InventoryClickEvent e) {
        Material type = e.getCurrentItem().getType();
        switch (type) {
            case IRON_SWORD, WOODEN_SWORD -> plugin.protection().togglePvP(player);
            case CHEST, TRAPPED_CHEST -> plugin.protection().toggleContainers(player);
            case ZOMBIE_HEAD, ROTTEN_FLESH -> plugin.protection().toggleMobProtection(player);
            case BONE, LEAD -> plugin.protection().togglePetProtection(player);
            case ARMOR_STAND, ITEM_FRAME -> plugin.protection().toggleEntityProtection(player);
            case WHEAT, WHEAT_SEEDS -> plugin.protection().toggleFarmProtection(player);

            case ARROW -> plugin.gui().player().open(player);
            case BARRIER -> {
                player.closeInventory();
                plugin.sounds().playMenuClose(player);
            }
        }
        open(player); // refresh after toggle
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
