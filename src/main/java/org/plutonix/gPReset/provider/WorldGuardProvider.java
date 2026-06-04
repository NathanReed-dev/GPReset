package org.plutonix.gPReset.provider;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.plutonix.gPReset.reset.Region;

import java.util.HashSet;
import java.util.Set;

public class WorldGuardProvider implements ProtectionProvider {

    private final boolean ignoreGlobal;
    public WorldGuardProvider(boolean ignoreGlobal) {
        this.ignoreGlobal = ignoreGlobal;
    }

    @Override
    public String getName() {
        return "WorldGuard";
    }
    @Override
    public boolean isEnabled() {
        return Bukkit.getPluginManager().getPlugin("WorldGuard") != null;
    }
    @Override
    public Set<Region> getProtectedRegions(World world) {
        Set<Region> regions = new HashSet<>();

        RegionManager manager = WorldGuard.getInstance()
                .getPlatform()
                .getRegionContainer()
                .get(BukkitAdapter.adapt(world));
        if (manager == null) {
            return  regions;
        }

        for (ProtectedRegion pr : manager.getRegions().values()) {
            if (ignoreGlobal && pr.getId().equalsIgnoreCase("__global__")) {
                continue;
            }

            BlockVector3 min = pr.getMinimumPoint();
            BlockVector3 max = pr.getMaximumPoint();

            int  minChunkX = min.getBlockX() >> 4;
            int minChunkZ = min.getBlockZ() >> 4;

            int maxChunkX = max.getBlockX() >> 4;
            int maxChunkZ = max.getBlockZ() >> 4;

            int minRegionX = Math.floorDiv(minChunkX, 32);
            int minRegionZ = Math.floorDiv(minChunkZ, 32);

            int maxRegionX = Math.floorDiv(maxChunkX, 32);
            int maxRegionZ = Math.floorDiv(maxChunkZ, 32);

            for (int rx = minRegionX; rx <= maxRegionX; rx++) {
                for (int rz = minRegionZ; rz <= maxRegionZ; rz++) {
                    regions.add(new Region(rx, rz));
                }
            }

        }
        return regions;
    }


}
