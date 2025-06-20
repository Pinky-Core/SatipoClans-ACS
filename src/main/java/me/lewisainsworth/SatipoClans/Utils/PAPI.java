package me.lewisainsworth.satipoclans.Utils;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import me.lewisainsworth.satipoclans.SatipoClan;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;


import java.util.List;

public class PAPI extends PlaceholderExpansion {

    private final SatipoClan plugin;
    private final FileConfiguration data;

    public PAPI(SatipoClan plugin) {
        this.plugin = plugin;
        this.data = plugin.getFH().getData();
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "satipoclans";
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (player == null) return "N/A";

        Econo econ = plugin.getEcon();
        String clanName = getPlayerClan(player.getName());
        if (clanName == null) return "N/A";

        try (Connection con = plugin.getMariaDBManager().getConnection()) {
            switch (identifier.toLowerCase()) {
                case "prefix":
                    return SatipoClan.prefix;

                case "player_money":
                    return String.valueOf(econ.getBalance(player));

                case "clan_leader":
                    return querySingleString(con, "SELECT leader FROM clans WHERE name = ?", clanName, "N/A");

                case "clan_founder":
                    return querySingleString(con, "SELECT founder FROM clans WHERE name = ?", clanName, "N/A");

                case "clan_name":
                    return clanName;

                case "clan_tag":
                    return querySingleString(con, "SELECT tag FROM clans WHERE name = ?", clanName, "N/A");

                case "clan_money":
                    return querySingleString(con, "SELECT money FROM clans WHERE name = ?", clanName, "0");

                case "clan_membercount":
                    return String.valueOf(queryCount(con, "SELECT COUNT(*) FROM clan_users WHERE clan = ?", clanName));

                case "clan_membercount_online": {
                    List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
                    int onlineCount = 0;

                    for (Player p : onlinePlayers) {
                        String pClan = getPlayerClan(p.getName());
                        if (clanName.equalsIgnoreCase(pClan)) onlineCount++;
                    }
                    return String.valueOf(onlineCount);
                }

                case "clan_membercount_offline": {
                    int total = queryCount(con, "SELECT COUNT(*) FROM clan_users WHERE clan = ?", clanName);
                    int onlineCount = 0;
                    List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
                    for (Player p : onlinePlayers) {
                        String pClan = getPlayerClan(p.getName());
                        if (clanName.equalsIgnoreCase(pClan)) onlineCount++;
                    }
                    return String.valueOf(total - onlineCount);
                }

                default:
                    return "&a&lSatipo&6&lClans";
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "error";
        }
    }

    private String querySingleString(Connection con, String sql, String param, String def) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String val = rs.getString(1);
                    return val != null ? val : def;
                }
            }
        }
        return def;
    }

    private int queryCount(Connection con, String sql, String param) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }


    private String getPlayerClan(String playerName) {
        if (playerName == null) return null;

        String clan = null;
        String sql = "SELECT clan FROM clan_users WHERE LOWER(username) = ? LIMIT 1";

        try (Connection con = plugin.getMariaDBManager().getConnection();
            PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, playerName.trim().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    clan = rs.getString("clan");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return clan;
    }


    public void registerPlaceholders() {
        if (!register()) {
            plugin.getLogger().warning("Failed to register SatipoClans placeholders.");
        } else {
            plugin.getLogger().info("SatipoClans placeholders registered!");
        }
    }
}

