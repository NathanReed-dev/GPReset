package org.plutonix.gPReset.reset;

import org.bukkit.World;

import java.io.File;

public class WorldUtils {

    public static File getRegionFolder(String worldname) {
        return new File(worldname + "/region");
    }
}
