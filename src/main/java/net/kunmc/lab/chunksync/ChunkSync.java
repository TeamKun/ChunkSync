package net.kunmc.lab.chunksync;

import net.kunmc.lab.chunksync.util.Utils;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
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

import java.util.Arrays;

public final class ChunkSync extends JavaPlugin implements Listener {
    private final ChunkSyncData data = new ChunkSyncData();
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
        int count = 0;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 256; y++) {
                    Block block = chunk.getBlock(x, y, z);
                    BlockData chunkBlockData = block.getBlockData();
                    BlockData globalBlockData = data.getBlockData(x, y, z, block.getWorld().getEnvironment());
                    if (globalBlockData != null && !chunkBlockData.equals(globalBlockData)) {
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                Utils.setTypeAndData(block, globalBlockData, true);
                            }
                        }.runTaskLater(this, count % setBlockPerTick);
                        count++;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        BlockData blockData = e.getBlock().getBlockData();
        Location location = e.getBlock().getLocation();

        data.setBlockData(blockData, location);
        applyChangeToOtherChunks(blockData, location);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        Location location = e.getBlock().getLocation();

        data.setBlockData(Material.AIR.createBlockData(), location);
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

        int count = 0;
        Location chunkOffset = Utils.toChunkOffset(location.clone());
        for (Chunk chunk : chunks) {
            if (chunk.equals(location.getChunk())) {
                continue;
            }

            new BukkitRunnable() {
                @Override
                public void run() {
                    Utils.setTypeAndData(chunk.getBlock(chunkOffset.getBlockX(), chunkOffset.getBlockY(), chunkOffset.getBlockZ()), blockData, true);
                }
            }.runTaskLater(this, count % setBlockPerTick);
            count++;
        }
    }
}
