package org.plutonix.gPReset.reset;


import java.util.HashSet;
import java.util.Set;

public class RegionCalculator {
    public Set<Region> getProtectedRegions(Set<Long> protectedChunks) {
        Set<Region> regions = new HashSet<>();

        for (long key : protectedChunks) {
            int chunkX = (int) (key >> 32);
            int chunkZ = (int) key;

            regions.add(Region.fromChunk(chunkX, chunkZ));
        }
        return regions;
    }
}
