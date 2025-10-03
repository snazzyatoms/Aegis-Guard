package com.aegisguard.gui;

import com.aegisguard.AegisGuard;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class GUIListener implements Listener {

    private final AegisGuard plugin;

    public GUIListener(AegisGuard plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (e.getCurrentItem() == null || e.getClickedInventory() == null) return;

        String title = e.getView().getTitle();

        // Main Player GUI
        if (title.equals(plugin.msg().get("menu_title"))) {
            plugin.gui().handleClick(player, e);
        }

        // Trusted GUI + submenus (Add/Remove Trusted)
        else if (title.equals(plugin.msg().get("trusted_menu_title"))
              || title.equals(plugin.msg().get("add_trusted_title"))
              || title.equals(plugin.msg().get("remove_trusted_title"))) {
            plugin.gui().trusted().handleClick(player, e);
        }

        // Settings GUI
        else if (title.equals(plugin.msg().get("settings_menu_title"))) {
            plugin.gui().settings().handleClick(player, e);
        }

        // Admin GUI
        else if (title.equals(plugin.msg().get("admin_menu_title"))) {
            plugin.gui().admin().handleClick(player, e);
        }
    }
}
