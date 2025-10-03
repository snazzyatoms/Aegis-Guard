package com.aegisguard.gui;

import com.aegisguard.AegisGuard;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class RolesGUI {

    private final AegisGuard plugin;

    public RolesGUI(AegisGuard plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        String title = plugin.msg().get("roles_menu_title");
        Inventory inv = Bukkit.createInventory(null, 27, title);

        // Placeholder “Coming Soon” icon
        ItemStack placeholder = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = placeholder.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(plugin.msg().get("button_roles")); // from messages.yml
            meta.setLore(Arrays.asList(
                    plugin.msg().color("&7Feature not yet available"),
                    plugin.msg().color("&e📌 Coming in a future update")
            ));
            placeholder.setItemMeta(meta);
        }

        inv.setItem(13, placeholder);

        // Exit
        inv.setItem(26, GUIManager.icon(
                Material.BARRIER,
                plugin.msg().get("button_exit"),
                plugin.msg().getList("exit_lore")
        ));

        player.openInventory(inv);
        plugin.sounds().playMenuOpen(player);
    }

    public void handleClick(Player player, InventoryClickEvent e) {
        e.setCancelled(true);

        if (e.getCurrentItem() == null) return;
        Material type = e.getCurrentItem().getType();

        if (type == Material.BARRIER) {
            player.closeInventory();
            plugin.sounds().playMenuClose(player);
        }
        else if (type == Material.NAME_TAG) {
            // Subtle thud effect — placeholder only
            plugin.sounds().playMenuClose(player);
        }
    }
}
