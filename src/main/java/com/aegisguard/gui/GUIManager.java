package com.aegisguard.gui;

import com.aegisguard.AegisGuard;

public class GUIManager {

    private final AegisGuard plugin;
    private final PlayerGUI playerGUI;
    private final SettingsGUI settingsGUI;
    private final AdminGUI adminGUI;
    private final TrustedGUI trustedGUI;

    public GUIManager(AegisGuard plugin) {
        this.plugin = plugin;
        this.playerGUI = new PlayerGUI(plugin);
        this.settingsGUI = new SettingsGUI(plugin);
        this.adminGUI = new AdminGUI(plugin);
        this.trustedGUI = new TrustedGUI(plugin);
    }

    public PlayerGUI player() { return playerGUI; }
    public SettingsGUI settings() { return settingsGUI; }
    public AdminGUI admin() { return adminGUI; }
    public TrustedGUI trusted() { return trustedGUI; }
}
