package org.plutonix.gPReset.commands;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.plutonix.gPReset.GPReset;
import org.plutonix.gPReset.audit.AuditResult;
import org.plutonix.gPReset.provider.ProtectionProvider;
import org.plutonix.gPReset.util.Msg;

import java.util.*;

public class GPResetCommand implements CommandExecutor, TabCompleter {

    private final GPReset plugin;
    private final List<ProtectionProvider> protectionProviders = new ArrayList<>();

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
            sender.sendMessage(Msg.info("/gpreset reset <world>"));
            sender.sendMessage(Msg.info("/gpreset audit <world>"));
            sender.sendMessage(Msg.info("/gpreset reload"));
            sender.sendMessage(Msg.info("/gpreset help"));
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("deny")) {
            pending.remove(player.getUniqueId());

            player.sendMessage(Msg.info("Reset cancelled."));
            return true;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("audit")) {
            World world = Bukkit.getWorld(args[1]);
            if (world == null) {
                player.sendMessage(Msg.error("World Not Found."));
                return true;
            }
            AuditResult result = plugin.getAudit().audit(world);
            player.sendMessage(Msg.info(""));
            player.sendMessage(Msg.hInfo("==== GPReset Audit ==="));
            player.sendMessage(Msg.info(""));
            player.sendMessage(Msg.info("World: " + world.getName()));
            player.sendMessage(Msg.info("RegionFiles: " + result.regionFiles()));
            player.sendMessage(Msg.info(""));
            player.sendMessage(Msg.hInfo("Protection Providers:"));
            for (ProtectionProvider provider : plugin.getProtectionManager().getProviders()) {
                String status = provider.isEnabled() ? "Enabled" : "Disabled";

                player.sendMessage(Msg.raw("<gray>- " + provider.getName() + " : </gray>" + status));
            }
            player.sendMessage(Msg.hInfo(""));
            player.sendMessage(Msg.hInfo("Protected Regions:"));
            result.protectedRegions().forEach((name, count) -> player.sendMessage(Msg.info("- " + name + " : " + count)));
            player.sendMessage(Msg.info("- Total Protected Regions:" + result.allRegions()));

            player.sendMessage(Msg.hInfo(""));
            player.sendMessage(Msg.hInfo("Deletion Estimate: "));
            player.sendMessage(Msg.info("- Protected: " + result.allRegions()));
            player.sendMessage(Msg.info("- Deleting: " + result.deletingRegions()));

            player.sendMessage(Msg.hInfo(""));
            player.sendMessage(Msg.hInfo("Backup Settings: "));
            player.sendMessage(Msg.info("- Keep Limit: " + result.backupLimit()));
            player.sendMessage(Msg.info("- Backup Directory: " + result.backupDirectory().getAbsolutePath()));

            player.sendMessage(Msg.hInfo(""));
            player.sendMessage(Msg.hInfo("Global Regions: "));
            player.sendMessage(Msg.info("- Ignored: " + result.ignoreGlobalRegion()));

            player.sendMessage(Msg.hInfo(""));
            if (result.deletingRegions() > 0) {
                player.sendMessage(Msg.raw("<bold><gray>Result: </gray></bold><green>Reset Allowed</green>"));
            }else {
                player.sendMessage(Msg.raw("<bold><gray>Result: </gray></bold><red>Nothing to be Deleted</red>"));
            }

            return true;
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("reset")) {
            World world = Bukkit.getWorld(args[1]);

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
            p.world = args[1];
            p.timestamp = System.currentTimeMillis();

            pending.put(player.getUniqueId(), p);

            long timeout = plugin.getConfig().getLong("reset.confirmation-timeout-seconds");

            if (args.length == 3 && args[2].equalsIgnoreCase("confirm")) {

                Pending pen = pending.get(player.getUniqueId());

                if (pen == null || !pen.world.equals(args[1])) {
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

            player.sendMessage(Msg.hWarn("Warning"));
            player.sendMessage(Msg.info("World: " + args[1]));
            player.sendMessage(Msg.info("Backup will be created."));
            player.sendMessage(Msg.error("Unclaimed regions will be reset."));
            player.sendMessage(Msg.hInfo("Players will be kicked and the Server will shut down."));
            player.sendMessage(Msg.raw(
                    "<green><underlined><bold><shadow:dark_green><click:run_command:'/gpreset reset "+ args[1] + " confirm'>CONFIRM</click></shadow></bold></underlined></green> " +
                            " <red><underlined><bold><shadow:dark_red><click:run_command:'/gpreset deny'>DENY</click></shadow></bold></underlined></red>"
            ));

            return true;
        }

        player.sendMessage("Usage: /gpreset reset <world> [confirm]");
        return true;

    }

    private long getTimeout() {
        return plugin.getConfig().getLong("reset.confirmation-timeout-seconds", 30) * 1000;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender,@NotNull Command cmd,@NotNull String alias,@NotNull  String[] args) {

        if (args.length == 1) {
            List<String> options = new ArrayList<>();

            options.add("reload");
            options.add("reset");
            options.add("audit");
            options.add("help");

            return options.stream().filter(s -> s.toLowerCase()
                    .startsWith(args[0].toLowerCase()))
                    .toList();
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("audit")) {
            List<String> options = new ArrayList<>();
            Bukkit.getWorlds().forEach(w -> options.add(w.getName()));
            return options.stream().filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase())).toList();
            }

        if (args.length == 2 && args[0].equalsIgnoreCase("reset")) {
            List<String> options = new ArrayList<>();
            Bukkit.getWorlds().forEach(w -> options.add(w.getName()));

            return options.stream().filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase())).toList();
        }

        return Collections.emptyList();

    }

}