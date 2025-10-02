package com.aegisguard.gui;

import com.aegisguard.AegisGuard;
import com.aegisguard.data.Plot;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;
import java.util.UUID;

public class TrustedGUI {

    private final AegisGuard plugin;

    public TrustedGUI(AegisGuard plugin) {
        this.plugin = plugin;
    }

    /* -----------------------------
     * Open Trusted Menu
     * ----------------------------- */
    public void open(Player owner) {
        Plot plot = plugin.store().getPlot(owner.getUniqueId());
        if (plot == null) {
            plugin.msg().send(owner, "no_plot_here");
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 54, plugin.msg().get("trusted_menu_title"));

        // Trusted players list
        int slot = 0;
        for (UUID trustedId : plot.getTrusted()) {
            if (slot >= 45) break;

            OfflinePlayer trusted = Bukkit.getOfflinePlayer(trustedId);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();

            if (meta != null) {
                meta.setOwningPlayer(trusted);
                meta.setDisplayName("§a" + trusted.getName());

                List<String> lore = plugin.msg().formatList("trusted_menu_lore",
                        "PLAYER", trusted.getName());
                meta.setLore(lore);

                head.setItemMeta(meta);
            }
            inv.setItem(slot++, head);
        }

        // Control buttons
        inv.setItem(45, GUIManager.icon(Material.EMERALD, plugin.msg().get("button_add_trusted")));
        inv.setItem(46, GUIManager.icon(Material.BARRIER, plugin.msg().get("button_remove_trusted")));
        inv.setItem(52, GUIManager.icon(Material.ARROW, plugin.msg().get("button_back")));
        inv.setItem(53, GUIManager.icon(Material.BOOK, plugin.msg().get("button_exit")));

        owner.openInventory(inv);
    }

    /* -----------------------------
     * Handle Clicks
     * ----------------------------- */
    public void handleClick(Player player, InventoryClickEvent e) {
        e.setCancelled(true); // prevent item pickup/movement

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        Plot plot = plugin.store().getPlot(player.getUniqueId());
        if (plot == null) {
            player.closeInventory();
            plugin.msg().send(player, "no_plot_here");
            return;
        }

        // Handle buttons
        switch (clicked.getType()) {
            case EMERALD -> {
                player.closeInventory();
                player.sendMessage("§a➡ Type the name of the player you want to trust.");
                // TODO: Hook chat input to trust a player
            }
            case BARRIER -> {
                player.closeInventory();
                player.sendMessage("§c➡ Type the name of the player you want to untrust.");
                // TODO: Hook chat input to untrust a player
            }
            case ARROW -> {
                player.closeInventory();
                plugin.gui().openMain(player);
            }
            case BOOK -> {
                player.closeInventory();
            }
            case PLAYER_HEAD -> {
                if (clicked.getItemMeta() instanceof SkullMeta skull) {
                    String trustedName = skull.getOwningPlayer() != null ? skull.getOwningPlayer().getName() : skull.getDisplayName();
                    player.sendMessage("§e✔ Trusted player: " + trustedName);
                }
            }
            default -> {}
        }
    }
}
