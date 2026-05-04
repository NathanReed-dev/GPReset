package org.plutonix.gPReset.reset;

import java.io.File;
import java.util.Set;

public class RegionResetter {

    public int deleteUnprotectedRegions(File regionFolder, Set<Region> protectedRegions) {
        int deleted = 0;

        File[] files = regionFolder.listFiles((dir, name) -> name.endsWith(".mca"));

        if (files == null) return 0;

        for (File file : files) {
            Region region = parseRegion(file.getName());

            if (region == null) continue;

            if (!protectedRegions.contains(region)) {
                if (!file.delete()) {
                    deleted++;
                }
            }
        }
        return deleted;
    }

    private Region parseRegion(String name) {
        try {
            String[] parts = name.replace(".mca", "").split("\\.");

            int x = Integer.parseInt(parts[1]);
            int z = Integer.parseInt(parts[2]);
            return new Region(x , z);
        } catch (Exception e) {
            return null;
        }
    }

}
