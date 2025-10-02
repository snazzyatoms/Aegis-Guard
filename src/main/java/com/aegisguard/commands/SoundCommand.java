package com.aegisguard.commands;

import com.aegisguard.AegisGuard;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SoundCommand implements CommandExecutor {

    private final AegisGuard plugin;

    public SoundCommand(AegisGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("aegisguard.admin")) {
            plugin.msg().send(sender, "no_perm");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§eUsage:");
            sender.sendMessage("§7/aegis sound global <on|off>");
            sender.sendMessage("§7/aegis sound player <name> <on|off>");
            return true;
        }

        String mode = args[0].toLowerCase();
        switch (mode) {
            case "global" -> {
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /aegis sound global <on|off>");
                    return true;
                }
                boolean enable = args[1].equalsIgnoreCase("on");
                plugin.getConfig().set("enable_sounds.global", enable);
                plugin.saveConfig();
                sender.sendMessage("§a✔ Global sounds " + (enable ? "enabled" : "disabled"));
            }
            case "player" -> {
                if (args.length < 3) {
                    sender.sendMessage("§cUsage: /aegis sound player <name> <on|off>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage("§cPlayer not found: " + args[1]);
                    return true;
                }
                boolean enable = args[2].equalsIgnoreCase("on");
                plugin.getConfig().set("enable_sounds.players." + target.getUniqueId(), enable);
                plugin.saveConfig();
                sender.sendMessage("§a✔ Sounds for " + target.getName() + " " + (enable ? "enabled" : "disabled"));
            }
            default -> {
                sender.sendMessage("§cInvalid mode. Use §7global §cor §7player");
            }
        }

        return true;
    }
}
