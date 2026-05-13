package org.plutonix.gPReset.adapter;

import me.ryanhamshire.GriefPrevention.*;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.HashSet;
import java.util.Set;


public class ClaimAdapter {
    public Set<Long> getProtectedChunks(World world) {
        Set<Long> protectedChunks = new HashSet<>();

        GriefPrevention gp = GriefPrevention.instance;

        if (gp == null) return protectedChunks;

        for (Claim claim : gp.dataStore.getClaims()) {
            Location lesser = claim.getLesserBoundaryCorner();
            Location greater = claim.getGreaterBoundaryCorner();

            if (!lesser.getWorld().equals(world)) continue;

            int minX = lesser.getBlockX() >> 4;
            int minZ = lesser.getBlockZ() >> 4;
            int maxX = greater.getBlockX() >> 4;
            int maxZ = greater.getBlockZ() >> 4;

            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <=maxZ; z++) {
                    protectedChunks.add(chunkKey(x,z));
                }
            }
        }
        return protectedChunks;
    }

    private long chunkKey(int x, int z) {
        return (((long) x) << 32) | (z & 0xffffffffL);
    }
}
