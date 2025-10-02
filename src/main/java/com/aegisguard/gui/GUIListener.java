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
 */
public class GUIListener implements Listener {

    private final GUIManager gui;
    private final TrustedGUI trustedGUI;

    public GUIListener(AegisGuard plugin) {
        this.gui = plugin.gui();
        this.trustedGUI = new TrustedGUI(plugin);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;

        String title = e.getView().getTitle();
        if (title.contains("Aegis Menu")) {
            gui.handleClick(e);
        } else if (title.contains("Trusted Players")) {
            trustedGUI.handleClick(player, e);
        }
    }
}
