package net.kunmc.lab.chunksync;

import net.kunmc.lab.chunksync.util.Utils;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.Nullable;

public class ChunkSyncData {
    private final BlockData[] blocksForOverworld = new BlockData[65536];
    private final BlockData[] blocksForNether = new BlockData[65536];
    private final BlockData[] blocksForEnd = new BlockData[65536];

    public @Nullable BlockData getBlockData(int x, int y, int z, World.Environment environment) {
        switch (environment) {
            case NORMAL:
                return blocksForOverworld[convertCoordinate(x, y, z)];
            case NETHER:
                return blocksForNether[convertCoordinate(x, y, z)];
            default:
                return blocksForEnd[convertCoordinate(x, y, z)];
        }
    }

    public void setBlockData(BlockData blockData, Location location) {
        Location chunkOffset = Utils.toChunkOffset(location.clone());
        setBlockData(blockData, chunkOffset.getBlockX(), chunkOffset.getBlockY(), chunkOffset.getBlockZ(), location.getWorld().getEnvironment());
    }

    public void setBlockData(BlockData blockData, int x, int y, int z, World.Environment environment) {
        switch (environment) {
            case NORMAL:
                blocksForOverworld[convertCoordinate(x, y, z)] = blockData;
                break;
            case NETHER:
                blocksForNether[convertCoordinate(x, y, z)] = blockData;
                break;
            default:
                blocksForEnd[convertCoordinate(x, y, z)] = blockData;
        }
    }

    private int convertCoordinate(int x, int y, int z) {
        return (x + z * 16) + y * 256;
    }
}
