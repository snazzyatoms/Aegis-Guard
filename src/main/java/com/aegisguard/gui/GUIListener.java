package com.aegisguard.gui;

import com.aegisguard.AegisGuard;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * GUIListener
 * - Captures click events in GUIs
 * - Delegates to GUIManager + TrustedGUI
 * - Adds immersive sound feedback
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
            playPageSound(player);
            return;
        }

        // Trusted Players Menus (Trusted, Add Trusted, Remove Trusted)
        if (title.equals(plugin.msg().get("trusted_menu_title")) ||
            title.equals(plugin.msg().get("add_trusted_title")) ||
            title.equals(plugin.msg().get("remove_trusted_title"))) {

            trustedGUI.handleClick(player, e);

            // Special handling for Exit
            String itemName = e.getCurrentItem().getItemMeta() != null ?
                    e.getCurrentItem().getItemMeta().getDisplayName() : "";

            if (itemName.equals(plugin.msg().get("button_exit"))) {
                playCloseSound(player);
            } else if (itemName.equals(plugin.msg().get("button_back"))) {
                playPageSound(player);
            } else {
                playPageSound(player);
            }
        }
    }

    /* -----------------------------
     * Sound Helpers
     * ----------------------------- */
    private void playPageSound(Player player) {
        if (plugin.cfg().getBoolean("sounds.enabled", true)) {
            player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
        }
    }

    private void playCloseSound(Player player) {
        if (plugin.cfg().getBoolean("sounds.enabled", true)) {
            player.playSound(player.getLocation(), Sound.ITEM_BOOK_PUT, 1f, 0.9f);
        }
    }
}
