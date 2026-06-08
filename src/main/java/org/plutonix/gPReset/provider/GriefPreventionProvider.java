package org.plutonix.gPReset.provider;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.plutonix.gPReset.reset.Region;

import java.util.HashSet;
import java.util.Set;

public class GriefPreventionProvider implements ProtectionProvider {

    @Override
    public String getName() {
        return "GriefPrevention";
    }

    @Override
    public boolean isEnabled() {
        return Bukkit.getPluginManager().getPlugin("GriefPrevention") != null;
    }

    @Override
    public Set<Region> getProtectedRegions(World world) {
        Set<Region> protectedRegions = new HashSet<>();

        GriefPrevention gp = GriefPrevention.instance;

        if (gp == null) return protectedRegions;

        for (Claim claim : gp.dataStore.getClaims()) {
            Location lesser = claim.getLesserBoundaryCorner();
            Location greater = claim.getGreaterBoundaryCorner();
            String owner = claim.ownerID.toString();

            if (!lesser.getWorld().equals(world)) continue;

            int minChunkX = lesser.getBlockX() >> 4;
            int minChunkZ = lesser.getBlockZ() >> 4;

            int maxChunkX = greater.getBlockX() >> 4;
            int maxChunkZ = greater.getBlockZ() >> 4;

            int minRegionX = Math.floorDiv(minChunkX, 32);
            int minRegionZ = Math.floorDiv(minChunkZ, 32);

            int maxRegionX = Math.floorDiv(maxChunkX, 32);
            int maxRegionZ = Math.floorDiv(maxChunkZ, 32);

            for (int rx = minRegionX; rx <= maxRegionX; rx++) {
                for (int rz = minRegionZ; rz <= maxRegionZ; rz++) {
                    protectedRegions.add(new Region(rx, rz));
                }
            }
        }
        return protectedRegions;
    }
}


