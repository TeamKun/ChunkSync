package net.kunmc.lab.chunksync.command;

import net.kunmc.lab.chunksync.ChunkSync;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

        switch (args[0]) {
            case "addPlayer":
                addPlayer(sender, args);
                break;
            case "removePlayer":
                removePlayer(sender, args);
                break;
            case "checkEnabledPlayers":
                checkEnabledPlayers(sender, args);
                break;
            case "numberOfProcessedBlocksPerSec":
                numberOfProcessedBlocksPerSec(sender, args);
                break;
            case "syncLiquid":
                syncLiquid(sender, args);
                break;
            case "applicableChunkDistance":
                applicableChunkDistance(sender, args);
                break;
            case "clearQueue":
                clearQueue(sender);
                break;
        }

        return true;
    }

    private void clearQueue(CommandSender sender) {
        sender.sendMessage(ChunkSync.scheduler.amount() + "の処理をキャンセルしました.");
        ChunkSync.scheduler.clear();
    }

    private void addPlayer(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "usage: /chunksync addPlayer <target>");
            return;
        }

        List<Entity> playerList = Bukkit.selectEntities(sender, args[1]).stream()
                .filter(x -> x instanceof Player)
                .collect(Collectors.toList());

        int count = 0;
        for (Entity p : playerList) {
            if (ChunkSync.enabledPlayerUUIDList.add(p.getUniqueId())) {
                count++;
            }
        }

        sender.sendMessage(ChatColor.GREEN + "" + count + "人のプレイヤーを追加しました.");
    }

    private void removePlayer(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "usage: /chunksync removePlayer <target>");
            return;
        }

        List<Entity> playerList = Bukkit.selectEntities(sender, args[1]).stream()
                .filter(x -> x instanceof Player)
                .collect(Collectors.toList());

        int count = 0;
        for (Entity p : playerList) {
            if (ChunkSync.enabledPlayerUUIDList.remove(p.getUniqueId())) {
                count++;
            }
        }

        sender.sendMessage(ChatColor.GREEN + "" + count + "人のプレイヤーを削除しました.");
    }

    private void checkEnabledPlayers(CommandSender sender, String[] args) {
        sender.sendMessage(ChatColor.YELLOW + "有効化されているプレイヤー一覧");

        StringBuilder message = new StringBuilder(ChatColor.GREEN.toString());
        for (UUID uuid : ChunkSync.enabledPlayerUUIDList) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                message.append(p.getName()).append(", ");
            }
        }
        sender.sendMessage(message.toString());
    }

    private void numberOfProcessedBlocksPerSec(CommandSender sender, String[] args) {
        if (args.length == 1) {
            sender.sendMessage(ChatColor.GREEN + "numberOfProcessedBlocksPerSecの値は" + plugin.scheduler.numberOfExecutionsPerSec + "です.");
        } else {
            try {
                plugin.scheduler.numberOfExecutionsPerSec = Integer.parseInt(args[1]);
                sender.sendMessage(ChatColor.GREEN + "numberOfProcessedBlocksPerSecの値を" + plugin.scheduler.numberOfExecutionsPerSec + "に設定しました.");
            } catch (Exception e) {
                sender.sendMessage(ChatColor.RED + "valueの値が不正です.");
            }
        }
    }

    private void syncLiquid(CommandSender sender, String[] args) {
        if (args.length == 1) {
            sender.sendMessage(ChatColor.GREEN + "syncLiquidの値は" + ChunkSync.shouldSyncLiquid + "です.");
        } else {
            try {
                ChunkSync.shouldSyncLiquid = Boolean.parseBoolean(args[1]);
                sender.sendMessage(ChatColor.GREEN + "syncLiquidの値を" + ChunkSync.shouldSyncLiquid + "に設定しました.");
            } catch (Exception e) {
                sender.sendMessage(ChatColor.RED + "valueの値が不正です.");
            }
        }
    }

    private void applicableChunkDistance(CommandSender sender, String[] args) {
        if (args.length == 1) {
            sender.sendMessage(ChatColor.GREEN + "applicableChunkDistanceの値は" + ChunkSync.applicableChunkDistance + "です.");
        } else {
            try {
                ChunkSync.applicableChunkDistance = Integer.parseInt(args[1]);
                sender.sendMessage(ChatColor.GREEN + "applicableChunkDistanceの値を" + ChunkSync.applicableChunkDistance + "に設定しました.");
            } catch (Exception e) {
                sender.sendMessage(ChatColor.RED + "valueの値が不正です.");
            }
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completion = new ArrayList<>();
        if (args.length == 1) {
            completion.add("numberOfProcessedBlocksPerSec");
            completion.add("syncLiquid");
            completion.add("applicableChunkDistance");
            completion.add("addPlayer");
            completion.add("removePlayer");
            completion.add("checkEnabledPlayers");
            completion.add("clearQueue");
        }

        if (args.length == 2) {
            switch (args[0]) {
                case "addPlayer":
                    completion.addAll(Stream.concat(Bukkit.getOnlinePlayers().stream().map(Player::getName), Stream.of("@a", "@r")).collect(Collectors.toList()));
                    break;
                case "removePlayer":
                    completion.addAll(Stream.concat(ChunkSync.enabledPlayerUUIDList.stream()
                            .map(Bukkit::getPlayer)
                            .filter(Objects::nonNull)
                            .map(Player::getName), Stream.of("@a", "@r"))
                            .collect(Collectors.toList()));
                    break;
                case "checkEnabledPlayers":
                case "clearQueue":
                    break;
                case "numberOfProcessedBlocksPerSec":
                case "syncLiquid":
                case "applicableChunkDistance":
                    completion.add("[value]");
                    break;
            }
        }

        return completion.stream().filter(x -> x.startsWith(args[args.length - 1])).collect(Collectors.toList());
    }
}
