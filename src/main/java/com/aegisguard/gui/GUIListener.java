package com.aegisguard.gui;

import com.aegisguard.AegisGuard;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * GUIListener
 * - Captures click events in GUIs
 * - Delegates handling to GUIManager
 */
public class GUIListener implements Listener {

    private final GUIManager gui;

    public GUIListener(AegisGuard plugin) {
        this.gui = plugin.gui(); // use manager from main
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        gui.handleClick(e);
    }
}
