package me.lewisainsworth.satipoclans.CMDs;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import me.lewisainsworth.satipoclans.SatipoClan;
import me.lewisainsworth.satipoclans.Utils.Econo;
import me.lewisainsworth.satipoclans.Utils.MSG;
import static me.lewisainsworth.satipoclans.SatipoClan.prefix;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import me.lewisainsworth.satipoclans.Database.MariaDBManager;
import me.lewisainsworth.satipoclans.Utils.LangManager;



public class PECMD implements CommandExecutor, TabCompleter {

    private final SatipoClan plugin;
    private final MariaDBManager db;
    private final Econo econ;
    private final LangManager langManager; // Añadido

    public PECMD(SatipoClan plugin) {
        this.plugin = plugin;
        this.db = plugin.getMariaDBManager();
        this.econ = SatipoClan.getEcon();
        this.langManager = plugin.getLangManager(); // Inicializa
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (args.length != 1) {
            // Uso correcto sin prefijo
            sender.sendMessage(MSG.color(langManager.getMessage("stats.usage_stats")));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);

        String currentClan = langManager.getMessage("stats.no_clan"); // "Sin clan"
        List<String> history = new ArrayList<>();

        try (Connection con = db.getConnection();
             PreparedStatement stmt = con.prepareStatement("SELECT current_clan, history FROM player_clan_history WHERE uuid = ?")) {

            stmt.setString(1, target.getUniqueId().toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    currentClan = rs.getString("current_clan");
                    String rawHistory = rs.getString("history");
                    if (rawHistory != null && !rawHistory.isEmpty()) {
                        history = Arrays.asList(rawHistory.split(","));
                    }
                }
            }

        } catch (SQLException e) {
            sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("stats.error_database")));
            e.printStackTrace();
            return true;
        }

        double money = econ.getBalance(target);

        sender.sendMessage(MSG.color(langManager.getMessage("stats.stats_header_line")));
        sender.sendMessage(MSG.color(langManager.getMessage("stats.stats_title").replace("%player%", target.getName())));
        sender.sendMessage(MSG.color(langManager.getMessage("stats.stats_money").replace("%money%", String.format("%.2f", money))));
        sender.sendMessage(MSG.color(langManager.getMessage("stats.stats_current_clan").replace("%clan%", currentClan)));
        sender.sendMessage(MSG.color(langManager.getMessage("stats.stats_history")));

        if (history.isEmpty()) {
            sender.sendMessage(MSG.color(langManager.getMessage("stats.stats_no_history")));
        } else {
            for (String clan : history) {
                sender.sendMessage(MSG.color(langManager.getMessage("stats.stats_history_entry").replace("%clan%", clan)));
            }
        }

        sender.sendMessage(MSG.color(langManager.getMessage("stats.stats_footer_line")));
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


    public static void addClanToHistory(OfflinePlayer player, String newClan) {
        try (Connection con = SatipoClan.getInstance().getMariaDBManager().getConnection()) {
            PreparedStatement select = con.prepareStatement("SELECT history FROM player_clan_history WHERE uuid = ?");
            select.setString(1, player.getUniqueId().toString());

            String updatedHistory = newClan;

            try (ResultSet rs = select.executeQuery()) {
                if (rs.next()) {
                    String currentHistory = rs.getString("history");
                    if (currentHistory != null && !Arrays.asList(currentHistory.split(",")).contains(newClan)) {
                        updatedHistory = currentHistory + "," + newClan;
                    } else {
                        updatedHistory = currentHistory;
                    }
                }
            }

            PreparedStatement upsert = con.prepareStatement("""
                REPLACE INTO player_clan_history (uuid, name, current_clan, history)
                VALUES (?, ?, ?, ?)
            """);

            upsert.setString(1, player.getUniqueId().toString());
            upsert.setString(2, player.getName());
            upsert.setString(3, newClan);
            upsert.setString(4, updatedHistory);
            upsert.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}