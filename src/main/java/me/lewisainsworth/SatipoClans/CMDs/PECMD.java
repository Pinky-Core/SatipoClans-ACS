package me.lewisainsworth.satipoclans.CMDs;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import me.lewisainsworth.satipoclans.SatipoClan;
import me.lewisainsworth.satipoclans.Utils.Econo;
import me.lewisainsworth.satipoclans.Utils.MSG;
import static me.lewisainsworth.satipoclans.SatipoClan.prefix;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PECMD implements CommandExecutor, TabCompleter {

    private final SatipoClan plugin;
    private static File playerDataFile;
    private static FileConfiguration playerDataCfg;
    private final Econo econ;

    public PECMD(SatipoClan plugin) {
        this.plugin = plugin;
        this.econ = SatipoClan.getEcon();
        loadPlayerData();
    }

    private void loadPlayerData() {
        playerDataFile = new File(plugin.getDataFolder(), "playerData.yml");
        if (!playerDataFile.exists()) {
            playerDataFile.getParentFile().mkdirs();
            try {
                playerDataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        playerDataCfg = YamlConfiguration.loadConfiguration(playerDataFile);
    }

    private static void savePlayerData() {
        try {
            playerDataCfg.save(playerDataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void addClanToHistory(OfflinePlayer player, String newClan) {
        String path = player.getUniqueId() + ".clanHistory";
        List<String> history = playerDataCfg.getStringList(path);
        if (!history.contains(newClan)) {
            history.add(newClan);
            playerDataCfg.set(path, history);
            savePlayerData();
        }
        playerDataCfg.set(player.getUniqueId() + ".currentClan", newClan);
        savePlayerData();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(MSG.color(prefix + "&c&lUSE: &f/scstats <player>"));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        double money = econ.getBalance(target);
        String currentClan = playerDataCfg.getString(target.getUniqueId() + ".currentClan", "No clan");
        List<String> history = playerDataCfg.getStringList(target.getUniqueId() + ".clanHistory");

        sender.sendMessage(MSG.color("&6======= &aStatistics of &e" + target.getName() + " &6======="));
        sender.sendMessage(MSG.color("&eMoney: &a" + money));
        sender.sendMessage(MSG.color("&eCurrent Clan: &a" + currentClan));
        sender.sendMessage(MSG.color("&eClan History:"));
        if (history.isEmpty()) {
            sender.sendMessage(MSG.color("&7No history found."));
        } else {
            for (String clan : history) {
                sender.sendMessage(MSG.color("&7- " + clan));
            }
        }
        sender.sendMessage(MSG.color("&6=============================="));

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            List<String> players = new ArrayList<>();
            Bukkit.getOnlinePlayers().forEach(p -> {
                if (p.getName().toLowerCase().startsWith(partial)) {
                    players.add(p.getName());
                }
            });
            return players;
        }
        return Collections.emptyList();
    }
}