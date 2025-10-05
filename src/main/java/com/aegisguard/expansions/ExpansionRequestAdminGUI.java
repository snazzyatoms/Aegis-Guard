package com.aegisguard.expansions;

import com.aegisguard.AegisGuard;
import com.aegisguard.data.PlotStore;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;
import java.util.*;

/**
 * ExpansionRequestAdminGUI
 * ------------------------------------------------------------
 * Admin overview of all pending expansion requests.
 * - Each request displayed as a PAPER item
 * - EMERALD_BLOCK = Approve, REDSTONE_BLOCK = Deny
 * - BARRIER = Close, ARROW = Refresh
 *
 * Fully supports:
 *  - Multilingual tone system
 *  - Messages.yml color + lore integration
 *  - Sound & GUI refresh feedback
 */
public class ExpansionRequestAdminGUI {

    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.##");

    private final AegisGuard plugin;
    private final ExpansionRequestManager manager;

    public ExpansionRequestAdminGUI(AegisGuard plugin) {
        this.plugin = plugin;
        this.manager = plugin.getExpansionRequestManager();
    }

    /* -----------------------------
     * Open Admin GUI
     * ----------------------------- */
    public void open(Player admin) {
        if (!admin.hasPermission("aegisguard.admin")) {
            plugin.msg().send(admin, "no_perm");
            return;
        }

        String title = plugin.msg().has("expansion_admin_title")
                ? plugin.msg().get(admin, "expansion_admin_title")
                : "🛡 AegisGuard — Expansion Requests";

        Inventory inv = Bukkit.createInventory(null, 54, title);
        fill(inv, glass(Material.GRAY_STAINED_GLASS_PANE, " "));

        List<ExpansionRequest> pending = new ArrayList<>(manager.getActiveRequests());
        int index = 0;
        for (ExpansionRequest req : pending) {
            if (index >= 45) break;
            inv.setItem(index++, requestItem(req));
        }

        inv.setItem(48, icon(Material.ARROW, plugin.msg().color("&eRefresh"),
                List.of(plugin.msg().color("&7Reload pending requests."))));
        inv.setItem(49, icon(Material.REDSTONE_BLOCK, plugin.msg().color("&cDeny Selected"),
                List.of(plugin.msg().color("&7Click a PAPER first, then this to deny."))));
        inv.setItem(50, icon(Material.EMERALD_BLOCK, plugin.msg().color("&aApprove Selected"),
                List.of(plugin.msg().color("&7Click a PAPER first, then this to approve."))));
        inv.setItem(53, icon(Material.BARRIER, plugin.msg().get(admin, "button_exit"),
                plugin.msg().getList(admin, "exit_lore")));

        admin.openInventory(inv);
        plugin.sounds().playMenuOpen(admin);
    }

    /* -----------------------------
     * Handle Click Events
     * ----------------------------- */
    public void handleClick(Player admin, InventoryClickEvent e) {
        e.setCancelled(true);
        if (!admin.hasPermission("aegisguard.admin")) return;
        if (e.getCurrentItem() == null) return;

        String title = e.getView().getTitle();
        String expected = plugin.msg().has("expansion_admin_title")
                ? plugin.msg().get(admin, "expansion_admin_title")
                : "🛡 AegisGuard — Expansion Requests";
        if (!title.equals(expected)) return;

        Material type = e.getCurrentItem().getType();

        switch (type) {
            case PAPER -> plugin.sounds().playMenuFlip(admin);

            case EMERALD_BLOCK -> {
                ExpansionRequest req = findSelectedRequest(e);
                if (req == null) {
                    admin.sendMessage(plugin.msg().color("&c❌ Select a PAPER first."));
                    plugin.sounds().playMenuClose(admin);
                    return;
                }

                boolean ok = manager.approveRequest(req);
                if (ok) {
                    Map<String, String> placeholders = Map.of("PLAYER", playerName(req.getRequester()));
                    plugin.msg().send(admin, "expansion_approved", placeholders);
                    Player target = Bukkit.getPlayer(req.getRequester());
                    if (target != null) plugin.msg().send(target, "expansion_approved", placeholders);
                    plugin.sounds().playMenuFlip(admin);
                } else {
                    plugin.msg().send(admin, "expansion_invalid");
                    plugin.sounds().playMenuClose(admin);
                }
                open(admin);
            }

            case REDSTONE_BLOCK -> {
                ExpansionRequest req = findSelectedRequest(e);
                if (req == null) {
                    admin.sendMessage(plugin.msg().color("&c❌ Select a PAPER first."));
                    plugin.sounds().playMenuClose(admin);
                    return;
                }

                manager.denyRequest(req);
                Map<String, String> placeholders = Map.of("PLAYER", playerName(req.getRequester()));
                plugin.msg().send(admin, "expansion_denied", placeholders);
                Player target = Bukkit.getPlayer(req.getRequester());
                if (target != null) plugin.msg().send(target, "expansion_denied", placeholders);

                plugin.sounds().playMenuClose(admin);
                open(admin);
            }

            case ARROW -> open(admin);
            case BARRIER -> {
                admin.closeInventory();
                plugin.sounds().playMenuClose(admin);
            }
            default -> { /* ignore */ }
        }
    }

    /* -----------------------------
     * Helpers
     * ----------------------------- */
    private ItemStack requestItem(ExpansionRequest req) {
        String owner = playerName(req.getRequester());
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(plugin.msg().color("&e" + owner));
            List<String> lore = new ArrayList<>();
            lore.add(plugin.msg().color("&7World: &f" + req.getWorldName()));
            lore.add(plugin.msg().color("&7Status: &f" + req.getStatus().name()));
            lore.add(plugin.msg().color("&7Current Radius: &e" + req.getCurrentRadius()));
            lore.add(plugin.msg().color("&7Requested Radius: &a" + req.getRequestedRadius()));
            lore.add(plugin.msg().color("&7Cost: &6" + MONEY.format(req.getCost())));
            lore.add(plugin.msg().color("&8Requested: &7" + req.getCreatedAt()));
            lore.add(plugin.msg().color("&0ID:" + req.getRequestId()));
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private String playerName(UUID id) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(id);
        return op.getName() != null ? op.getName() : id.toString().substring(0, 8);
    }

    private ExpansionRequest findSelectedRequest(InventoryClickEvent e) {
        ItemStack cursor = e.getCursor();
        if (cursor != null && cursor.getType() == Material.PAPER) {
            ExpansionRequest req = fromItem(cursor);
            if (req != null) return req;
        }

        for (ItemStack it : e.getView().getTopInventory().getContents()) {
            if (it != null && it.getType() == Material.PAPER) {
                ExpansionRequest r = fromItem(it);
                if (r != null) return r;
            }
        }
        return null;
    }

    private ExpansionRequest fromItem(ItemStack item) {
        if (item == null || item.getType() != Material.PAPER || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || meta.getLore() == null) return null;
        for (String line : meta.getLore()) {
            if (line.startsWith("§0ID:")) {
                try {
                    UUID id = UUID.fromString(line.substring("§0ID:".length()));
                    return manager.getRequest(id);
                } catch (Exception ignored) { }
            }
        }
        return null;
    }

    private void fill(Inventory inv, ItemStack with) {
        for (int i = 0; i < inv.getSize(); i++)
            if (inv.getItem(i) == null) inv.setItem(i, with);
    }

    private ItemStack glass(Material m, String name) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            it.setItemMeta(meta);
        }
        return it;
    }

    private ItemStack icon(Material m, String name, List<String> lore) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            it.setItemMeta(meta);
        }
        return it;
    }
}
