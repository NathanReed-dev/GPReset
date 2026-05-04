package org.plutonix.gPReset.commands;

import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.plutonix.gPReset.GPReset;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ResetCommand implements CommandExecutor {

    private final GPReset plugin;
    private final Set<UUID> confirmations = new HashSet<>();

    public ResetCommand(GPReset plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Run this in-game.");
            return true;
        }

        if (!player.hasPermission("gpreset.admin")) {
            player.sendMessage("No permission.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§cWARNING:");
            player.sendMessage("§7A backup will be created.");
            player.sendMessage("§7All unclaimed builds will be LOST.");
            player.sendMessage("§eRun /resetworld confirm to continue.");

            confirmations.add(player.getUniqueId());
            return true;
        }

        if (args[0].equalsIgnoreCase("confirm")) {

            if (!confirmations.contains(player.getUniqueId())) {
                player.sendMessage("§cYou must run /resetworld first.");
                return true;
            }

            player.sendMessage("§cFINAL WARNING:");
            player.sendMessage("§7Server will restart.");
            player.sendMessage("§7All players will be kicked.");
            player.sendMessage("§7Reset will begin immediately.");

            plugin.markResetFlagAndShutdown();
            return true;
        }

        return true;
    }
}