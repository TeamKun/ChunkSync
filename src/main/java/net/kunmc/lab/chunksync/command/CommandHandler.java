package net.kunmc.lab.chunksync.command;

import net.kunmc.lab.chunksync.ChunkSync;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CommandHandler implements TabExecutor {
    private final ChunkSync plugin;

    public CommandHandler(ChunkSync plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length < 1) {
            return false;
        }

        if (args.length == 1) {
            sender.sendMessage(ChatColor.GREEN + "numberOfProcessedBlocksPerSecの値は" + plugin.scheduler.numberOfExecutionsPerSec + "です.");
        } else {
            try {
                plugin.scheduler.numberOfExecutionsPerSec = Integer.parseInt(args[1]);
                sender.sendMessage(ChatColor.GREEN + "numberOfProcessedBlocksPerSecの値を" + plugin.scheduler.numberOfExecutionsPerSec + "に設定しました.");
            } catch (Exception e) {
                sender.sendMessage(ChatColor.RED + "IntegerValueの値が不正です.");
            }
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completion = new ArrayList<>();
        if (args.length == 1) {
            completion.add("numberOfProcessedBlocksPerSec");
        }

        if (args.length == 2) {
            completion.add("[IntegerValue]");
        }

        return completion.stream().filter(x -> x.startsWith(args[args.length - 1])).collect(Collectors.toList());
    }
}
