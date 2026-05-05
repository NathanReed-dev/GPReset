package org.plutonix.gPReset.commands;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.plutonix.gPReset.GPReset;
import org.plutonix.gPReset.util.Msg;

import java.util.*;

public class GPResetCommand implements CommandExecutor, TabCompleter {

    private final GPReset plugin;

    private static class Pending {
        String world;
        long timestamp;
    }
    private final Map<UUID, Pending> pending = new HashMap<>();

    public GPResetCommand(GPReset plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,@NotNull Command cmd,@NotNull String label,@NotNull String[] args) {


        if(!(sender instanceof Player player)) {
            sender.sendMessage("Run in-game.");
            return true;
        }

        if(!player.hasPermission("gpreset.admin")) {
            player.sendMessage(Msg.error("No Permission."));
            return true;
        }

        if (args.length == 1) {
            World world = Bukkit.getWorld(args[0]);

            List<String> blacklist = plugin.getConfig().getStringList("worlds.blacklist");


            if (world == null) {
                player.sendMessage(Msg.error("World not found."));
                return true;
            }

            if (blacklist.contains(world.getName())) {
                player.sendMessage(Msg.error("This world cannot be reset"));
                return true;
            }

            Pending p = new Pending();
            p.world = args[0];
            p.timestamp = System.currentTimeMillis();

            pending.put(player.getUniqueId(), p);

            long timeout = plugin.getConfig().getLong("reset.confirmation-timeout-seconds");

            player.sendMessage(Msg.warn("Warning"));
            player.sendMessage(Msg.info("World: " + args[0]));
            player.sendMessage(Msg.info("Backup will be created."));
            player.sendMessage(Msg.error("Unclaimed regions will be reset."));
            player.sendMessage(Msg.info( "Run /gpreset "+ args[0] + " confirm"));
            player.sendMessage(Msg.warn("Confirmation will be timed out by: " + timeout + " Seconds"));

            return true;
        }

        if (args.length == 2 && args[1].equalsIgnoreCase("confirm")) {

            Pending p = pending.get(player.getUniqueId());

            if (p == null || !p.world.equals(args[0])) {
                player.sendMessage(Msg.error("Invalid Confirmation."));
                return true;
            }

            if (System.currentTimeMillis() - p.timestamp > getTimeout()) {
                pending.remove(player.getUniqueId());
                player.sendMessage(Msg.error("Confirmation expired."));
                return true;
            }

            plugin.executeReset(p.world);
            return true;

        }

        player.sendMessage("Usage: /gpreset <world> [confirm]");
        return true;

    }

    private long getTimeout() {
        return plugin.getConfig().getLong("reset.confirmation-timeout-seconds", 30) * 1000;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender,@NotNull Command cmd,@NotNull String alias,@NotNull  String[] args) {

        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            List<String> worlds = new ArrayList<>();

            Bukkit.getWorlds().forEach(w -> {
                if (w.getName().toLowerCase().startsWith(prefix)) {
                    worlds.add(w.getName());
                }
            });
            return worlds;
        }

        if (args.length == 2) {
            return Collections.singletonList("confirm");
        }
        return Collections.emptyList();

    }

}