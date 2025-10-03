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
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (e.getClickedInventory() == null || e.getCurrentItem() == null) return;

        String title = e.getView().getTitle();
        e.setCancelled(true); // Prevent default item moving

        if (title.equals(plugin.msg().get("menu_title"))) {
            plugin.gui().player().handleClick(player, e);
        } else if (title.equals(plugin.msg().get("settings_menu_title"))) {
            plugin.gui().settings().handleClick(player, e);
        } else if (title.equals(plugin.msg().get("admin_menu_title"))) {
            plugin.gui().admin().handleClick(player, e);
        } else if (title.equals(plugin.msg().get("trusted_menu_title"))) {
            plugin.gui().trusted().handleClick(player, e);
        }
    }
}
