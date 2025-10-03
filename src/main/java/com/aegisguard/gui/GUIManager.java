package com.aegisguard.gui;

import com.aegisguard.AegisGuard;
import org.bukkit.entity.Player;

public class GUIManager {

    private final AegisGuard plugin;
    private final PlayerGUI playerGUI;
    private final TrustedGUI trustedGUI;
    private final SettingsGUI settingsGUI;
    private final AdminGUI adminGUI;

    public GUIManager(AegisGuard plugin) {
        this.plugin = plugin;
        this.playerGUI = new PlayerGUI(plugin);
        this.trustedGUI = new TrustedGUI(plugin);
        this.settingsGUI = new SettingsGUI(plugin);
        this.adminGUI = new AdminGUI(plugin);
    }

    public void openPlayerMenu(Player player) {
        playerGUI.open(player);
    }

    public void openTrustedMenu(Player player) {
        trustedGUI.open(player);
    }

    public void openSettingsMenu(Player player) {
        settingsGUI.open(player);
    }

    public void openAdminMenu(Player player) {
        if (player.hasPermission("aegisguard.admin")) {
            adminGUI.open(player);
        } else {
            plugin.msg().send(player, "no_permission");
        }
    }
}
