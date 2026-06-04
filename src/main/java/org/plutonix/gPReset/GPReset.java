package org.plutonix.gPReset;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.plugin.java.JavaPlugin;
import org.plutonix.gPReset.commands.GPResetCommand;
import org.plutonix.gPReset.provider.ProtectionProvider;
import org.plutonix.gPReset.provider.GriefPreventionProvider;
import org.plutonix.gPReset.provider.WorldGuardProvider;
import org.plutonix.gPReset.reset.*;

import java.io.File;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;

public class GPReset extends JavaPlugin {

    private static final String FLAG_FILE = "reset.flag";
    private static final String WORLD_FILE = "reset.world";

    private final List<ProtectionProvider> protectionProviders = new ArrayList<>();

    @Override
    public void onEnable() {

        saveDefaultConfig();

        var command = new GPResetCommand(this);
        var cmd = getCommand("gpreset");

        protectionProviders.clear();
        registerProviders(new GriefPreventionProvider());
        registerProviders(new WorldGuardProvider(
                getConfig().getBoolean("world.ignore-global-region", true)
        ));

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

            // Backup
            backupWorld(world);

            // Protected Regions
            Set<Region> protectedRegions = getProtectedRegions(world);

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

    public void executeReset(String worldName) {
        try {
            Files.createDirectories(getDataFolder().toPath());

            Files.writeString(new File(getDataFolder(), WORLD_FILE).toPath(), worldName);
            Files.createFile(new File(getDataFolder(), FLAG_FILE).toPath());
        } catch (Exception e) {
            getLogger().severe("Failed to prepare reset: " + e.getMessage());
            return;
        }

        Bukkit.getOnlinePlayers().forEach(p ->
                p.kick(net.kyori.adventure.text.Component.text("Server restarting for world reset: " + worldName)));

        Bukkit.shutdown();
    }

    private void backupWorld(World world) throws Exception{
        File worldFolder = world.getWorldFolder();
        File backupDir = new File(getServer().getWorldContainer(), "backups");

        Files.createDirectories(backupDir.toPath());

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File backupTarget = new File(backupDir, world.getName() + "_" + timeStamp);

        copyFolder(worldFolder, backupTarget);

        int keep = getConfig().getInt("backup.limit", 5);
        pruneBackups(backupDir, world.getName(), keep);

        getLogger().info("Backup Created: " + backupTarget.getName());
    }

    private void pruneBackups(File backupDir, String worldName, int keep) {
        File[] backups = backupDir.listFiles((d, name) -> name.startsWith(worldName + "_"));

        if (backups == null || backups.length <= keep) return;

        Arrays.sort(backups, Comparator.comparingLong(File::lastModified).reversed());

        getLogger().info("Pruning backups. Found: " + backups.length + ", keeping: " + keep);

        for (int i = keep; i< backups.length; i++) {
            File backup = backups[i];

            deleteFolder(backup);
            getLogger().info("Deleted backup: " + backup.getName());
        }
    }

    private void deleteFolder(File file) {
        if (file.isDirectory()) {
            File[] contents = file.listFiles();
            if (contents != null) {
                for (File child : contents) {
                    deleteFolder(child);
                }
            }
        }

        if (!file.delete()) {
            getLogger().warning("Failed to delete: " + file.getAbsolutePath());
        }
    }

    private void copyFolder(File src, File dest) throws Exception {
        if (src.isDirectory()) {
            Files.createDirectories(dest.toPath());
            for (String file : Objects.requireNonNull(src.list())) {
                copyFolder(new File(src, file), new File(dest, file));
            }
        } else {
            Files.copy(src.toPath(), dest.toPath());
        }
    }

    public void registerProviders(ProtectionProvider provider) {
        if (protectionProviders.stream().noneMatch(p -> p.getName().equals(provider.getName()))) {
            protectionProviders.add(provider);
            getLogger().info("Registered providers: " + provider.getName());
        } else {
            getLogger().severe("Provider: " + provider.getName() + " already registered!");
        }
    }

    public List<ProtectionProvider> getProtectionProviders() {
        return protectionProviders;
    }

    public Set<Region> getProtectedRegions(World world) {
        Set<Region> protectedRegions = new HashSet<>();

        for  (ProtectionProvider provider : protectionProviders) {
            if (!provider.isEnabled()) {
                continue;
            }

            Set<Region> regions = provider.getProtectedRegions(world);

            protectedRegions.addAll(regions);
            getLogger().info(provider.getName() + " protected: " + regions.size());
        }

        return protectedRegions;
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
