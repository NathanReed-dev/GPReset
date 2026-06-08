package org.plutonix.gPReset.provider;

import org.plutonix.gPReset.GPReset;
import org.plutonix.gPReset.reset.Region;
import org.bukkit.World;

import java.util.*;

public class ProtectionManager {

    private final List<ProtectionProvider> providers = new ArrayList<>();
    public void register(ProtectionProvider provider) {
        boolean exists = providers.stream().anyMatch((p) -> p.getName().equals(provider.getName()));

        if (!exists) {
            providers.add(provider);
        }
    }

    public List<ProtectionProvider> getProviders() {
        return Collections.unmodifiableList(providers);
    }

    public Set<Region> getProtectedRegions(World world) {
        Set<Region> result = new HashSet<>();
        for (ProtectionProvider provider : providers) {

            if(!provider.isEnabled()) continue;

            result.addAll(provider.getProtectedRegions(world));
        }
        return result;
    }

}
