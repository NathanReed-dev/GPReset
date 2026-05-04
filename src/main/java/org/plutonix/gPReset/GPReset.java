package org.plutonix.gPReset;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.plutonix.gPReset.commands.ResetCommand;
import org.plutonix.gPReset.grief.ClaimAdapter;
import org.plutonix.gPReset.reset.*;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

public class GPReset extends JavaPlugin {

    private static final String FLAG_FILE = "reset.flag";

    @Override
    public void onEnable() {
        ClaimAdapter claimAdapter = new ClaimAdapter();

        // Register command safely
        var cmd = getCommand("resetworld");
        if (cmd == null) {
            getLogger().severe("Command 'resetworld' not defined in plugin.yml!");
            return;
        }
        cmd.setExecutor(new ResetCommand(this));

        // If flag exists, perform reset BEFORE world is actively used
        File flag = new File(getDataFolder(), FLAG_FILE);
        if (flag.exists()) {
            getLogger().info("Reset flag detected. Performing world reset...");

            // pick target world (simple: first world)
            World world = Bukkit.getWorlds().getFirst();
            String worldName = world.getName();

            // Compute protected regions
            var protectedChunks = claimAdapter.getProtectedChunks(world);
            var regionCalc = new RegionCalculator();
            Set<Region> protectedRegions = regionCalc.getProtectedRegions(protectedChunks);

            // Backup
            backupWorld(world);

            // Unload world to release file handles
            Bukkit.unloadWorld(world, true);

            // Delete unprotected regions
            File regionFolder = WorldUtils.getRegionFolder(worldName);
            RegionResetter resetter = new RegionResetter();
            int deleted = resetter.deleteUnprotectedRegions(regionFolder, protectedRegions);

            getLogger().info("Deleted regions: " + deleted);

            // Remove flag
            if (!flag.delete()) {
                getLogger().warning("Failed to delete reset flag file.");
            }

            // Recreate world
            Bukkit.createWorld(new org.bukkit.WorldCreator(worldName));

            getLogger().info("World reset complete.");
        }
    }

    public void markResetFlagAndShutdown() {
        try {
            getDataFolder().mkdirs();
            File flag = new File(getDataFolder(), FLAG_FILE);
            if (!flag.exists()) {
                flag.createNewFile();
            }
        } catch (Exception e) {
            getLogger().severe("Failed to create reset flag: " + e.getMessage());
            return;
        }

        // Kick players with message
        Bukkit.getOnlinePlayers().forEach(p ->
                p.kick(Component.text("Server is restarting for world reset"))
        );

        // Shutdown server
        Bukkit.shutdown();
    }

    private void backupWorld(World world) {
        try {
            File worldFolder = world.getWorldFolder();

            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            File backupDir = new File(getServer().getWorldContainer(), "backups");
            backupDir.mkdirs();

            File backupTarget = new File(backupDir, world.getName() + "_" + timestamp);

            getLogger().info("Creating world backup...");

            copyFolder(worldFolder, backupTarget);

            getLogger().info("Backup created at: " + backupTarget.getAbsolutePath());

        } catch (Exception e) {
            getLogger().severe("Backup failed: " + e.getMessage());
        }
    }

    private void copyFolder(File src, File dest) throws Exception {
        if (src.isDirectory()) {
            if (!dest.exists()) dest.mkdirs();

            for (String file : src.list()) {
                copyFolder(new File(src, file), new File(dest, file));
            }
        } else {
            java.nio.file.Files.copy(src.toPath(), dest.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
