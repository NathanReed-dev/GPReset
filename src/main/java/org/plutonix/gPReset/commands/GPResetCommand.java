package org.plutonix.gPReset.commands;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.plutonix.gPReset.GPReset;
import org.plutonix.gPReset.reset.Region;
import org.plutonix.gPReset.util.Msg;
import org.stringtemplate.v4.ST;

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

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            sender.sendMessage(Msg.info("Configuration reloaded."));
            return true;
        }

        if (args.length == 1 &&  args[0].equalsIgnoreCase("help")) {
            sender.sendMessage(Msg.info("Commands :- "));
            sender.sendMessage(Msg.info("/gpreset <world>"));
            sender.sendMessage(Msg.info("/gpreset preview <world>"));
            sender.sendMessage(Msg.info("/gpreset reload"));
            sender.sendMessage(Msg.info("/gpreset help"));
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("deny")) {
            pending.remove(player.getUniqueId());

            player.sendMessage(Msg.info("Reset cancelled."));
            return true;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("preview")) {
            World world = Bukkit.getWorld(args[1]);

            if (world == null) {
                player.sendMessage(Msg.error("World not found."));
                return true;
            }
            int total = plugin.countTotalRegions(world);
            Set<Region> protectedRegion = plugin.getProtectedRegions(world);

            int protectedCount = protectedRegion.size();
            int deleting = Math.max(0, total - protectedCount);

            player.sendMessage(Msg.warn("Preview for world: " + world.getName()));
            player.sendMessage(Msg.info("Total regions: " + total));
            player.sendMessage(Msg.info("Protected regions: " + protectedCount));
            player.sendMessage(Msg.info("Regions to delete: " + deleting));
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

            if (pending.containsKey(player.getUniqueId())) {
                player.sendMessage(Msg.hWarn("Previous pending reset replaced."));
            }

            Pending p = new Pending();
            p.world = args[0];
            p.timestamp = System.currentTimeMillis();

            pending.put(player.getUniqueId(), p);

            long timeout = plugin.getConfig().getLong("reset.confirmation-timeout-seconds");

            player.sendMessage(Msg.hWarn("Warning"));
            player.sendMessage(Msg.info("World: " + args[0]));
            player.sendMessage(Msg.info("Backup will be created."));
            player.sendMessage(Msg.error("Unclaimed regions will be reset."));
            player.sendMessage(Msg.hInfo("Players will be kicked and the Server will shut down."));
            player.sendMessage(Msg.raw(
                    "<green><underlined><bold><shadow:dark_green><click:run_command:'/gpreset "+ args[0] + " confirm'>CONFIRM</click></shadow></bold></underlined></green> " +
                            " <red><underlined><bold><shadow:dark_red><click:run_command:'/gpreset deny'>DENY</click></shadow></bold></underlined></red>"
            ));

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
            List<String> options = new ArrayList<>();

            Bukkit.getWorlds().forEach(w -> options.add(w.getName()));

            options.add("reload");
            options.add("preview");
            options.add("help");

            return options.stream().filter(s -> s.toLowerCase()
                    .startsWith(args[0].toLowerCase()))
                    .toList();
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("preview")) {
            List<String> options = new ArrayList<>();
            Bukkit.getWorlds().forEach(w -> options.add(w.getName()));

            return options.stream().filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase())).toList();
            }
        return Collections.emptyList();

    }

}