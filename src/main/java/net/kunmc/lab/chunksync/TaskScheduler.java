package net.kunmc.lab.chunksync;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

public class TaskScheduler {
    private final Deque<BukkitRunnable> deque = new ConcurrentLinkedDeque<>();
    public int numberOfExecutionsPerSec = 12500;

    public TaskScheduler(JavaPlugin plugin) {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (int i = 0; i < (numberOfExecutionsPerSec / 20) * (Bukkit.getTPS()[0] / 20); i++) {
                    BukkitRunnable runnable = deque.poll();
                    if (runnable != null) {
                        runnable.runTask(plugin);
                    }
                }
            }
        }.runTaskTimerAsynchronously(plugin, 0, 0);

        new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.getLogger().info(deque.size() + " ");
            }
        }.runTaskTimer(plugin, 0, 20);
    }

    public void offer(BukkitRunnable runnable) {
        deque.offer(runnable);
    }
}
