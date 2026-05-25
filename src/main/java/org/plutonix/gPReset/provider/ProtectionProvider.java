package org.plutonix.gPReset.provider;

import org.bukkit.World;
import org.plutonix.gPReset.reset.Region;

import java.util.Set;

public interface ProtectionProvider {

    String getName();
    boolean isEnabled();
    Set<Region> getProtectedRegions(World world);

}
