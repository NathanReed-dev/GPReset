package org.plutonix.gPReset.audit;

import java.io.File;
import java.util.Map;

public record AuditResult (
        String worldName,
        int regionFiles,
        Map<String, Boolean> providers,
        Map<String, Integer> protectedRegions,
        int allRegions,
        int deletingRegions,
        int backupLimit,
        File backupDirectory,
        boolean ignoreGlobalRegion,
        boolean resetAllowed
) {}
