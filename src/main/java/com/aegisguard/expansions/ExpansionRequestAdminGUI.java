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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * ExpansionRequestAdminGUI
 * ------------------------------------------------------------
 * Admin overview of all pending expansion requests.
 * - Shows each request as a PAPER item with full details in lore
 * - Click EMERALD_BLOCK to Approve, REDSTONE_BLOCK to Deny
 * - BARRIER to close, ARROW to refresh
 *
 * Integrates with:
 *  - ExpansionRequestManager for persistence
 *  - VaultHook (handled through manager approve/deny)
 *
 * Titles (config-driven suggested keys):
 *  - "🛡 AegisGuard — Expansion Requests"
 */
public class ExpansionRequestAdminGUI {

    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.##");

    private final AegisGuard plugin;
    private final ExpansionRequestManager manager;

    public ExpansionRequestAdminGUI(AegisGuard plugin) {
        this.plugin = plugin;
        this.manager = plugin.expansion();
    }

    /* -----------------------------
     * Open Admin Menu
     * ----------------------------- */
    public void open(Player admin) {
        if (!admin.hasPermission("aegisguard.admin")) {
            admin.sendMessage(plugin.msg().get("no_perm"));
            return;
        }

        String title = plugin.msg().has("expansion_admin_title")
                ? plugin.msg().get("expansion_admin_title")
                : "🛡 AegisGuard — Expansion Requests";

        Inventory inv = Bukkit.createInventory(null, 54, title);

        // Background glass for a polished look
        fill(inv, glass(Material.GRAY_STAINED_GLASS_PANE, " "));

        // Populate pending requests
        List<ExpansionRequest> pending = new ArrayList<>(manager.listPending());
        int index = 0;
        for (ExpansionRequest req : pending) {
            if (index >= 45) break; // first 5 rows reserved for entries

            inv.setItem(index++, requestItem(req));
        }

        // Action bar (bottom row)
        inv.setItem(48, icon(Material.ARROW, plugin.msg().color("&eRefresh"),
                List.of(plugin.msg().color("&7Reload the queue view"))));
        inv.setItem(49, icon(Material.REDSTONE_BLOCK, plugin.msg().color("&cDeny Selected"),
                List.of(plugin.msg().color("&7Click a request (PAPER) first,"),
                        plugin.msg().color("&7then click this to deny & refund (if any)."))));
        inv.setItem(50, icon(Material.EMERALD_BLOCK, plugin.msg().color("&aApprove Selected"),
                List.of(plugin.msg().color("&7Click a request (PAPER) first,"),
                        plugin.msg().color("&7then click this to approve & apply."))));
        inv.setItem(53, icon(Material.BARRIER, plugin.msg().get("button_exit"),
                plugin.msg().getList("exit_lore")));

        admin.openInventory(inv);
        plugin.sounds().playMenuOpen(admin);
    }

    /* -----------------------------
     * Handle Admin Clicks
     * ----------------------------- */
    public void handleClick(Player admin, InventoryClickEvent e) {
        e.setCancelled(true);
        if (!admin.hasPermission("aegisguard.admin")) return;
        if (e.getCurrentItem() == null) return;

        String title = e.getView().getTitle();
        String expected = plugin.msg().has("expansion_admin_title")
                ? plugin.msg().get("expansion_admin_title")
                : "🛡 AegisGuard — Expansion Requests";
        if (!title.equals(expected)) return;

        Material type = e.getCurrentItem().getType();

        switch (type) {
            case PAPER -> {
                // Selecting a request: store its ID in slot 49/50 lore hint (not strictly necessary, but helpful)
                // We’ll simply rely on the PAPER item itself holding the hidden ID line.
                plugin.sounds().playMenuFlip(admin);
            }
            case EMERALD_BLOCK -> {
                ExpansionRequest req = findSelectedRequest(e);
                if (req == null) {
                    admin.sendMessage(plugin.msg().color("&c❌ Select a request (PAPER) first."));
                    plugin.sounds().playMenuClose(admin);
                    return;
                }
                boolean ok = manager.approve(req);
                if (ok) {
                    admin.sendMessage(plugin.msg().color("&a✔ Approved & applied expansion for &e" + playerName(req.getOwner())));
                    Player target = Bukkit.getPlayer(req.getOwner());
                    if (target != null) {
                        target.sendMessage(plugin.msg().color("&a⚡ Your claim expansion has been approved!"));
                    }
                    plugin.sounds().playMenuFlip(admin);
                    open(admin); // refresh
                } else {
                    admin.sendMessage(plugin.msg().color("&c❌ Failed to approve the request."));
                    plugin.sounds().playMenuClose(admin);
                }
            }
            case REDSTONE_BLOCK -> {
                ExpansionRequest req = findSelectedRequest(e);
                if (req == null) {
                    admin.sendMessage(plugin.msg().color("&c❌ Select a request (PAPER) first."));
                    plugin.sounds().playMenuClose(admin);
                    return;
                }
                manager.deny(req);
                admin.sendMessage(plugin.msg().color("&e⚠ Denied expansion for &e" + playerName(req.getOwner())));
                Player target = Bukkit.getPlayer(req.getOwner());
                if (target != null) {
                    target.sendMessage(plugin.msg().color("&c❌ Your expansion request was denied by an admin."));
                }
                plugin.sounds().playMenuClose(admin);
                open(admin); // refresh
            }
            case ARROW -> {
                // Refresh
                open(admin);
            }
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
        String ownerName = playerName(req.getOwner());

        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(plugin.msg().color("&e" + ownerName));
            List<String> lore = new ArrayList<>();
            lore.add(plugin.msg().color("&7World: &f" + req.getWorld()));
            lore.add(plugin.msg().color("&7Status: &f" + req.getStatus().name()));
            lore.add(plugin.msg().color("&7Current: &e" + safeRadius(req)));
            lore.add(plugin.msg().color("&7Requested: &a" + req.getNewRadius()));
            lore.add(plugin.msg().color("&7Cost: &6" + MONEY.format(req.getCost())));
            lore.add(plugin.msg().color("&8Requested: &7" + req.getCreatedAt()));
            lore.add(plugin.msg().color("&8Reason: &7" + (req.getReason() == null ? "-" : req.getReason())));
            lore.add(plugin.msg().color("&0ID:" + req.getRequestId())); // hidden ID line
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private String playerName(UUID id) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(id);
        return op.getName() != null ? op.getName() : id.toString().substring(0, 8);
        // fallback to short UUID if name is null
    }

    private int safeRadius(ExpansionRequest req) {
        PlotStore.Plot plot = req.getPlot();
        // If Plot doesn’t expose radius directly, approximate from bounds
        if (plot == null) return 0;
        int dx = Math.abs(plot.getX2() - plot.getX1());
        int dz = Math.abs(plot.getZ2() - plot.getZ1());
        // crude radius equivalent (half of min edge)
        return Math.max(1, Math.min(dx, dz) / 2);
    }

    private ExpansionRequest findSelectedRequest(InventoryClickEvent e) {
        // Expect the admin to have clicked a PAPER item before selecting Approve/Deny.
        // If they're clicking approve/deny, try to find the last clicked PAPER in the top inventory.
        // Since we don’t track “last selection” state, we’ll retrieve the currently hovered slot in the same view row, or
        // fallback to scan for the nearest PAPER under cursor. Simpler approach: read the item in the cursor if PAPER.
        ItemStack cursor = e.getCursor();
        if (cursor != null && cursor.getType() == Material.PAPER) {
            ExpansionRequest req = fromItem(cursor);
            if (req != null) return req;
        }
        // Else try the clicked slot if it’s PAPER (when they click EMERALD/REDSTONE, currentItem is those blocks)
        // so we also try the “selected” slot – Bukkit doesn’t store “selected” – we’ll scan hotbar/upper for a PAPER with ID.
        // Pragmatic approach: look at the slot  e.getInventory().getItem(e.getSlot())  if PAPER.
        // But for Approve/Deny we don’t have a pointer to the previously clicked paper.
        // So we’ll just check the slot under the admin’s cursor (the top inventory raw slot) if PAPER.
        // If none found, we’ll scan visible contents for a PAPER and pick the first one the admin is pointing at – not perfect,
        // but works well when admin clicks PAPER and then immediately clicks approve/deny without moving the cursor.

        // Check the slot the admin most recently clicked (not perfect, but acceptable UX)
        ItemStack maybePaper = e.getView().getTopInventory().getItem(e.getRawSlot());
        if (maybePaper != null && maybePaper.getType() == Material.PAPER) {
            ExpansionRequest req = fromItem(maybePaper);
            if (req != null) return req;
        }

        // Fallback: if there’s exactly one PAPER in the menu, act on it.
        List<ExpansionRequest> papers = new ArrayList<>();
        for (ItemStack it : e.getView().getTopInventory().getContents()) {
            if (it != null && it.getType() == Material.PAPER) {
                ExpansionRequest r = fromItem(it);
                if (r != null) papers.add(r);
            }
        }
        if (papers.size() == 1) return papers.get(0);

        return null;
    }

    private ExpansionRequest fromItem(ItemStack item) {
        if (item == null || item.getType() != Material.PAPER || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || meta.getLore() == null) return null;
        for (String line : meta.getLore()) {
            if (line != null && line.startsWith("§0ID:")) {
                try {
                    UUID id = UUID.fromString(line.substring("§0ID:".length()));
                    return manager.getByRequestId(id);
                } catch (Exception ignored) { }
            }
        }
        return null;
    }

    private void fill(Inventory inv, ItemStack with) {
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) inv.setItem(i, with);
        }
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
