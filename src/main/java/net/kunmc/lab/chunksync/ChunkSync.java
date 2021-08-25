package net.kunmc.lab.chunksync;

import io.papermc.paper.event.packet.PlayerChunkLoadEvent;
import net.kunmc.lab.chunksync.command.CommandHandler;
import net.kunmc.lab.chunksync.util.Utils;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.TabExecutor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockMultiPlaceEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public final class ChunkSync extends JavaPlugin implements Listener {
    private final ChunkSyncData data = new ChunkSyncData();
    public static TaskScheduler scheduler;
    public static boolean shouldSyncLiquid = false;
    public static int applicableChunkDistance = 10;
    public static Set<UUID> enabledPlayerUUIDList = new HashSet<>();

    @Override

    public void onEnable() {
        TabExecutor tabExecutor = new CommandHandler(this);
        getServer().getPluginCommand("chunksync").setExecutor(tabExecutor);
        getServer().getPluginCommand("chunksync").setTabCompleter(tabExecutor);

        getServer().getPluginManager().registerEvents(this, this);

        scheduler = new TaskScheduler(this);
    }

    @Override
    public void onDisable() {
    }

    @EventHandler
    public void onChunkLoad(PlayerChunkLoadEvent e) {
        Chunk chunk = e.getChunk();
        World.Environment environment = chunk.getWorld().getEnvironment();

        for (int y = 0; y < 256; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    Block block = chunk.getBlock(x, y, z);
                    BlockData chunkBlockData = block.getBlockData();
                    BlockData globalBlockData = data.getBlockData(x, y, z, environment);
                    if (globalBlockData != null && !chunkBlockData.equals(globalBlockData)) {
                        scheduler.offer(new BukkitRunnable() {
                            @Override
                            public void run() {
                                Utils.setTypeAndData(block, globalBlockData, true);
                            }
                        });
                    }
                }
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        if (!enabledPlayerUUIDList.contains(e.getPlayer().getUniqueId())) {
            return;
        }

        BlockData blockData = e.getBlock().getBlockData();
        Location location = e.getBlock().getLocation();

        if (blockData.getMaterial().equals(Material.FIRE)) {
            return;
        }

        data.setBlockData(blockData, location);
        applyChangeToOtherChunks(blockData, location);
    }

    @EventHandler
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent e) {
        if (!enabledPlayerUUIDList.contains(e.getPlayer().getUniqueId())) {
            return;
        }

        if (shouldSyncLiquid) {
            BlockData blockData;
            if (e.getBucket().equals(Material.LAVA_BUCKET)) {
                blockData = Material.LAVA.createBlockData();
            } else {
                blockData = Material.WATER.createBlockData();
            }

            Location location = e.getBlock().getLocation();

            data.setBlockData(blockData, location);
            applyChangeToOtherChunks(blockData, location);
        }
    }

    @EventHandler
    public void onPlayerBucketFill(PlayerBucketFillEvent e) {
        if (!enabledPlayerUUIDList.contains(e.getPlayer().getUniqueId())) {
            return;
        }

        if (shouldSyncLiquid) {
            Location location = e.getBlock().getLocation();

            data.setBlockData(Material.AIR.createBlockData(), location);
            applyChangeToOtherChunks(Material.AIR.createBlockData(), location);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        if (!enabledPlayerUUIDList.contains(e.getPlayer().getUniqueId())) {
            return;
        }

        Location location = e.getBlock().getLocation();

        data.setBlockData(Material.AIR.createBlockData(), location);
        applyChangeToOtherChunks(Material.AIR.createBlockData(), location);
    }

    @EventHandler
    public void onEntityPlace(EntityPlaceEvent e) {
        if (!enabledPlayerUUIDList.contains(e.getPlayer().getUniqueId())) {
            return;
        }

        BlockData blockData = e.getBlock().getBlockData();
        Location location = e.getBlock().getLocation();

        data.setBlockData(blockData, location);
        applyChangeToOtherChunks(blockData, location);
    }

    @EventHandler
    public void onBlockMultiPlace(BlockMultiPlaceEvent e) {
        if (!enabledPlayerUUIDList.contains(e.getPlayer().getUniqueId())) {
            return;
        }

        e.getReplacedBlockStates().forEach(blockState -> {
            Location location = blockState.getLocation();
            BlockData blockData = location.getBlock().getBlockData();

            data.setBlockData(blockData, location);
            applyChangeToOtherChunks(blockData, location);
        });
    }

    public void applyChangeToOtherChunks(BlockData blockData, Location location) {
        List<Chunk> chunkList = new ArrayList<>(Arrays.asList(location.getWorld().getLoadedChunks()));
        chunkList.sort((x, y) -> {
            Vector vec = new Vector(location.getChunk().getX(), 0, location.getChunk().getZ());
            double a = new Vector(x.getX(), 0, x.getZ()).distance(vec);
            double b = new Vector(y.getX(), 0, y.getZ()).distance(vec);

            if (a == b) {
                return 0;
            } else {
                return (a - b) < 0 ? -1 : 1;
            }
        });

        chunkList.removeIf(x -> {
            Chunk origin = location.getChunk();
            return new Vector(x.getX(), 0, x.getZ()).distance(new Vector(origin.getX(), 0, origin.getZ())) > applicableChunkDistance;
        });

        Location chunkOffset = Utils.toChunkOffset(location.clone());
        for (Chunk chunk : chunkList) {
            if (chunk.equals(location.getChunk())) {
                continue;
            }

            scheduler.offer(new BukkitRunnable() {
                @Override
                public void run() {
                    Block block = chunk.getBlock(chunkOffset.getBlockX(), chunkOffset.getBlockY(), chunkOffset.getBlockZ());
                    Utils.setTypeAndData(block, blockData, true);
                }
            });
        }
    }
}
