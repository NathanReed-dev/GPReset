package org.plutonix.gPReset.reset;

public record Region(int x, int z) {
    public static Region fromChunk(int chunkX, int chunkZ) {
        return new Region(chunkX >> 5, chunkZ >> 5);
    }

    public String filename() {
        return "r." + x + "." + z + ".mca";
    }
}
