package org.plutonix.gPReset;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.plugin.java.JavaPlugin;
import org.plutonix.gPReset.audit.Audit;
import org.plutonix.gPReset.backup.BackupManager;
import org.plutonix.gPReset.commands.GPResetCommand;
import org.plutonix.gPReset.provider.ProtectionManager;
import org.plutonix.gPReset.provider.GriefPreventionProvider;
import org.plutonix.gPReset.provider.WorldGuardProvider;
import org.plutonix.gPReset.reset.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class GPReset extends JavaPlugin {

    private static final String FLAG_FILE = "reset.flag";
    private static final String WORLD_FILE = "reset.world";

    private ProtectionManager protectionManager;
    private BackupManager backupManager;
    private Audit audit;

    @Override
    public void onEnable() {

        saveDefaultConfig();

        protectionManager = new ProtectionManager();
        protectionManager.register(new GriefPreventionProvider());
        protectionManager.register(new WorldGuardProvider(getConfig().getBoolean("world.ignore-global-region", true)));

        backupManager = new BackupManager(this);

        audit = new Audit(protectionManager, backupManager);

        var command = new GPResetCommand(this);
        var cmd = getCommand("gpreset");

        if (cmd == null) {
            getLogger().severe("Command gpreset not defined in plugin.yml");
            return;
        }

        cmd.setExecutor(command);
        cmd.setTabCompleter(command);

        handleStartupReset();

    }

    private void handleStartupReset() {
        File flag = new File(getDataFolder(), FLAG_FILE);
        File worldFile = new File(getDataFolder(), WORLD_FILE);

        if (!flag.exists() || !worldFile.exists()) return;

        try {
            String worldName = Files.readString(worldFile.toPath()).trim();
            getLogger().info("Resetting World: " + worldName);

            World world = Bukkit.getWorld(worldName);
            if(world == null) {
                world = Bukkit.createWorld(new WorldCreator(worldName));
            }

            if (world == null) {
                getLogger().severe("Failed to load world: " + worldName);
                return;
            }

            Set<Region> protectedRegions = protectionManager.getProtectedRegions(world);

            // Backup
            backupManager.backupWorld(world);

            // Unload World
            Bukkit.unloadWorld(world, true);

            // Delete Unprotected Regions
            File regionFolder = WorldUtils.getRegionFolder(worldName);
            RegionResetter resetter = new RegionResetter();
            int deleted = resetter.deleteUnprotectedRegions(regionFolder, protectedRegions);

            getLogger().info("Deleted regions: " + deleted);

            // Cleanup Flags
            if (!flag.delete()) {
                getLogger().warning("Could not delete reset.flag");
            }

            if (!worldFile.delete()) {
                getLogger().warning("Could not delete reset.world");
            }

            // Reload World
            Bukkit.createWorld(new WorldCreator(worldName));

            getLogger().info("World reset complete.");
        } catch (Exception e) {
            getLogger().severe("Reset Failed : " + e.getMessage());
        }

    }

    public BackupManager getBackupManager() {return backupManager;}
    public ProtectionManager getProtectionManager() {return protectionManager;}
    public Audit getAudit() {return audit;}

    public void executeReset(String worldName) {
        try {
            Files.createDirectories(getDataFolder().toPath());

            File worldFile =  new File(getDataFolder(), WORLD_FILE);
            File flag = new File(getDataFolder(), FLAG_FILE);

            Files.writeString(worldFile.toPath(), worldName, StandardOpenOption.CREATE,  StandardOpenOption.TRUNCATE_EXISTING);

            if (!flag.exists()) {
                Files.createFile(flag.toPath());
            }
        } catch (Exception e) {
            getLogger().severe("Failed to prepare reset: " + e.getMessage());
            return;
        }

        Bukkit.getOnlinePlayers().forEach(p ->
                p.kick(net.kyori.adventure.text.Component.text("Server restarting for world reset: " + worldName)));
        Bukkit.shutdown();
    }

    public  int countTotalRegions(World world) {
        File regionDir = WorldUtils.getRegionFolder(world.getName());

        if (!regionDir.exists()) {
            return 0;
        }
        File[] files = regionDir.listFiles((d, name) ->
                name.endsWith(".mca"));

        return files == null ? 0 : files.length;
    }
}
