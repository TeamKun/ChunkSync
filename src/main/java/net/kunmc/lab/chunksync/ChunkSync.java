package net.kunmc.lab.chunksync;

import net.kunmc.lab.chunksync.util.Utils;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public final class ChunkSync extends JavaPlugin implements Listener {
    private final BlockData[] blocksForOverworld = new BlockData[65536];
    private final BlockData[] blocksForNether = new BlockData[65536];
    private final BlockData[] blocksForEnd = new BlockData[65536];
    private int setBlockPerTick = 8;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent e) {
        Chunk chunk = e.getChunk();
        int delay = 0;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 256; y++) {
                    Block block = chunk.getBlock(x, y, z);
                    BlockData globalBlockData = getBlockData(x, y, z, block.getWorld().getEnvironment());
                    if (globalBlockData != null && !block.getBlockData().equals(globalBlockData)) {
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                Utils.setTypeAndData(block, globalBlockData, true);
                            }
                        }.runTaskLater(this, delay % setBlockPerTick);
                        delay++;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        BlockData blockData = e.getBlock().getBlockData();
        Location location = e.getBlock().getLocation();

        setBlockData(blockData, location);
        applyChangeToOtherChunks(blockData, location);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        Location location = e.getBlock().getLocation();

        setBlockData(Material.AIR.createBlockData(), location);
        applyChangeToOtherChunks(Material.AIR.createBlockData(), location);
    }

    public void applyChangeToOtherChunks(BlockData blockData, Location location) {
        Chunk[] chunks = location.getWorld().getLoadedChunks();
        Arrays.parallelSort(chunks, (x, y) -> {
            Vector vec = new Vector(location.getChunk().getX(), 0, location.getChunk().getZ());
            double a = new Vector(x.getX(), 0, x.getZ()).distance(vec);
            double b = new Vector(y.getX(), 0, y.getZ()).distance(vec);

            if (a == b) {
                return 0;
            } else {
                return (a - b) < 0 ? -1 : 1;
            }
        });

        int delay = 0;
        Location chunkOffset = toChunkOffset(location.clone());
        for (Chunk chunk : chunks) {
            if (chunk.equals(location.getChunk())) {
                continue;
            }

            new BukkitRunnable() {
                @Override
                public void run() {
                    Utils.setTypeAndData(chunk.getBlock(chunkOffset.getBlockX(), chunkOffset.getBlockY(), chunkOffset.getBlockZ()), blockData, true);
                }
            }.runTaskLater(this, delay % setBlockPerTick);
            delay++;
        }
    }

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
        Location chunkOffset = toChunkOffset(location.clone());
        setBlockData(blockData, chunkOffset.getBlockX(), chunkOffset.getBlockY(), chunkOffset.getBlockZ(), location.getWorld().getEnvironment());
    }

    public void setBlockData(BlockData blockData, int x, int y, int z, World.Environment environment) {
        switch (environment) {
            case NORMAL:
                blocksForOverworld[convertCoordinate(x, y, z)] = blockData;
            case NETHER:
                blocksForNether[convertCoordinate(x, y, z)] = blockData;
            default:
                blocksForEnd[convertCoordinate(x, y, z)] = blockData;
        }
    }

    public int convertCoordinate(int x, int y, int z) {
        return (x + z * 16) + y * 256;
    }

    public Location toChunkOffset(Location location) {
        Chunk chunk = location.getChunk();
        return location.subtract(chunk.getX() * 16, 0, chunk.getZ() * 16);
    }
}
