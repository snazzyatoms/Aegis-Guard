package com.aegisguard.gui;

import com.aegisguard.AegisGuard;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * GUIListener
 * - Captures click events in GUIs
 * - Delegates to GUIManager + TrustedGUI
 * - Uses SoundManager for immersive feedback
 */
public class GUIListener implements Listener {

    private final AegisGuard plugin;
    private final GUIManager gui;
    private final TrustedGUI trustedGUI;

    public GUIListener(AegisGuard plugin) {
        this.plugin = plugin;
        this.gui = plugin.gui();
        this.trustedGUI = new TrustedGUI(plugin);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (e.getClickedInventory() == null) return;
        if (e.getCurrentItem() == null) return;

        String title = e.getView().getTitle();

        // Guardian Codex (Main Menu)
        if (title.equals(plugin.msg().get("menu_title"))) {
            gui.handleClick(e);
            plugin.sounds().playMenuFlip(player);
            return;
        }

        // Trusted Players Menus (Trusted, Add Trusted, Remove Trusted)
        if (title.equals(plugin.msg().get("trusted_menu_title")) ||
            title.equals(plugin.msg().get("add_trusted_title")) ||
            title.equals(plugin.msg().get("remove_trusted_title"))) {

            trustedGUI.handleClick(player, e);

            // Safely check item name
            String itemName = e.getCurrentItem().getItemMeta() != null
                    ? e.getCurrentItem().getItemMeta().getDisplayName()
                    : "";

            if (itemName.equals(plugin.msg().get("button_exit"))) {
                plugin.sounds().playMenuClose(player);
            } else if (itemName.equals(plugin.msg().get("button_back"))) {
                plugin.sounds().playMenuFlip(player);
            } else {
                plugin.sounds().playMenuFlip(player);
            }
        }

        // Settings Menu
        if (title.equals(plugin.msg().get("settings_menu_title"))) {
            gui.handleClick(e); // settings clicks handled in GUIManager
            plugin.sounds().playMenuFlip(player);
        }
    }
}
