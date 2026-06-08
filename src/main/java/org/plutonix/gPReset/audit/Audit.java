package org.plutonix.gPReset.audit;

import org.bukkit.World;
import org.plutonix.gPReset.GPReset;
import org.plutonix.gPReset.backup.BackupManager;
import org.plutonix.gPReset.provider.ProtectionManager;
import org.plutonix.gPReset.provider.ProtectionProvider;
import org.plutonix.gPReset.reset.Region;
import org.plutonix.gPReset.reset.WorldUtils;

import java.io.File;
import java.util.*;

public class Audit {
    private final ProtectionManager protectionManager;
    private final BackupManager backupManager;

    public Audit(ProtectionManager protectionManager, BackupManager backupManager) {
        this.protectionManager = protectionManager;
        this.backupManager = backupManager;
    }

    public AuditResult audit(World world) {
        Map<String, Boolean> providerStatus = new LinkedHashMap<>();
        Map<String, Integer> protectedRegions = new LinkedHashMap<>();

        Set<Region> allRegions = new HashSet<>();

        for (ProtectionProvider provider : protectionManager.getProviders()) {
            boolean enabled = provider.isEnabled();

            providerStatus.put(provider.getName(), enabled);

            if (!enabled) {
                providerStatus.put(provider.getName(), false);
                continue;
            }

            Set<Region> regions = provider.getProtectedRegions(world);

            protectedRegions.put(provider.getName(), regions.size());

            allRegions.addAll(regions);
        }
        File regionFolder = WorldUtils.getRegionFolder(world.getName());
        int totalRegions = countRegions(regionFolder);
        int allRegionsCount = allRegions.size();
        int deleting = Math.max(0, totalRegions - allRegionsCount);

        int backupLimit = backupManager.getBackupLimit();
        File backupDir = backupManager.getBackupDir();

        boolean ignoreGlobal = backupManager.isGlobalIgnored();

        boolean resetAllowed = deleting > 0;

        return new AuditResult(
                world.getName(),
                totalRegions,
                providerStatus,
                protectedRegions,
                allRegionsCount,
                deleting,
                backupLimit,
                backupDir,
                ignoreGlobal,
                resetAllowed
        );
    }

    private int countRegions(File folder) {
        if (!folder.exists()) {
            return 0;
        }
        File[] files = folder.listFiles((d, n) -> n.endsWith(".mca"));

        return files == null ? 0 : files.length;
    }
}
