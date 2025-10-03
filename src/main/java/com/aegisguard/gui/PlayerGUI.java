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

public class PlayerGUI {

    private final AegisGuard plugin;

    public PlayerGUI(AegisGuard plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        String title = plugin.msg().get("menu_title");
        Inventory inv = Bukkit.createInventory(null, 27, title);

        inv.setItem(11, createItem(Material.LIGHTNING_ROD,
                plugin.msg().get("button_claim_land"),
                plugin.msg().getList("claim_land_lore")));

        inv.setItem(13, createItem(Material.PLAYER_HEAD,
                plugin.msg().get("button_trusted_players"),
                plugin.msg().getList("trusted_players_lore")));

        inv.setItem(15, createItem(Material.REDSTONE_COMPARATOR,
                plugin.msg().get("button_settings"),
                plugin.msg().getList("settings_lore")));

        inv.setItem(22, createItem(Material.WRITABLE_BOOK,
                plugin.msg().get("button_info"),
                plugin.msg().getList("info_lore")));

        inv.setItem(26, createItem(Material.BARRIER,
                plugin.msg().get("button_exit"),
                plugin.msg().getList("exit_lore")));

        player.openInventory(inv);
        plugin.sounds().playMenuOpen(player);
    }

    public void handleClick(Player player, InventoryClickEvent e) {
        Material type = e.getCurrentItem().getType();
        switch (type) {
            case LIGHTNING_ROD -> {
                player.closeInventory();
                plugin.selection().confirmClaim(player);
                plugin.sounds().playMenuFlip(player);
            }
            case PLAYER_HEAD -> {
                plugin.gui().trusted().open(player);
                plugin.sounds().playMenuFlip(player);
            }
            case REDSTONE_COMPARATOR -> plugin.gui().settings().open(player);
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
