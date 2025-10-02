package com.aegisguard.admin;

import com.aegisguard.AegisGuard;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * AdminCommand
 * - Handles all /aegis admin commands
 * - Cleanup banned player plots
 * - Future: show claims, transfer, force-unclaim, etc.
 */
public class AdminCommand implements CommandExecutor {

    private final AegisGuard plugin;

    public AdminCommand(AegisGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("aegisguard.admin")) {
            sender.sendMessage(plugin.msg().get("no_perm"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(plugin.msg().prefix() + " &7Admin commands:");
            sender.sendMessage("&e/aegis admin cleanup &7- Remove all plots owned by banned players");
            // Later we can add: /aegis admin showclaims, /aegis admin transfer, etc.
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "cleanup" -> {
                plugin.store().removeBannedPlots();
                sender.sendMessage(plugin.msg().prefix() + plugin.msg().get("admin_cleanup_done"));
                return true;
            }

            default -> {
                sender.sendMessage(plugin.msg().prefix() + "&cUnknown admin subcommand.");
                return true;
            }
        }
    }
}
