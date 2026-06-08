package org.plutonix.gPReset.backup;

import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;

public class BackupManager {

    private final JavaPlugin plugin;

    public BackupManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void backupWorld(World world) throws Exception{
        File worldFolder = world.getWorldFolder();
        File backupDir = new File(plugin.getServer().getWorldContainer(), "backups");

        Files.createDirectories(backupDir.toPath());

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File backupTarget = new File(backupDir, world.getName() + "_" + timeStamp);

        copyFolder(worldFolder, backupTarget);

        int keep = plugin.getConfig().getInt("backup.limit", 5);
        pruneBackups(backupDir, world.getName(), keep);

        plugin.getLogger().info("Backup Created: " + backupTarget.getName());
    }

    public List<BackupInfo> listBackups(String worldName) {
        File backupDir = new File(plugin.getServer().getWorldContainer(), "backups");
        File[] backupFiles = backupDir.listFiles((dir, name) -> name.startsWith(worldName + "_"));

        if (backupFiles == null || backupFiles.length == 0) {
            return Collections.emptyList();
        }

        List<BackupInfo> result = new ArrayList<>();

        for (File file : backupFiles) {

            result.add( new BackupInfo(file.getName(), folderSize(file), file.lastModified(), file));
        }
        result.sort(Comparator.comparingLong(BackupInfo::lastModified).reversed());
        return result;
    }

    public void pruneBackups(File backupDir, String worldName, int keep) {
        File[] backups = backupDir.listFiles((d, name) -> name.startsWith(worldName + "_"));

        if (backups == null || backups.length <= keep) return;

        Arrays.sort(backups, Comparator.comparingLong(File::lastModified).reversed());

        plugin.getLogger().info("Pruning backups. Found: " + backups.length + ", keeping: " + keep);

        for (int i = keep; i< backups.length; i++) {
            File backup = backups[i];

            deleteFolder(backup);
            plugin.getLogger().info("Deleted backup: " + backup.getName());
        }
    }

    private long folderSize(File file) {

        if (file.isFile()) {return file.length();}


        long size = 0;
        File[] files = file.listFiles();
        if (files != null) {
            for (File child : files) {
                size += folderSize(child);
            }
        }
        return size;
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
            plugin.getLogger().warning("Failed to delete: " + file.getAbsolutePath());
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

    public int getBackupLimit() {
        return plugin.getConfig().getInt("backup.limit", 5);
    }

    public File getBackupDir() {
        return new File(plugin.getServer().getWorldContainer(), "backups");
    }

    public boolean isGlobalIgnored() {
        return plugin.getConfig().getBoolean("world.ignore-global-region", true);
    }
}
