package me.lewisainsworth.satipoclans.listeners;

import me.lewisainsworth.satipoclans.SatipoClan;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PlayerStatsListener implements Listener {

    private final SatipoClan plugin;

    public PlayerStatsListener(SatipoClan plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player deceased = event.getEntity();
        Player killer = deceased.getKiller();

        incrementDeaths(deceased.getName());

        if (killer != null && !killer.getName().equalsIgnoreCase(deceased.getName())) {
            incrementKills(killer.getName());
        }
    }

    private void incrementKills(String playerName) {
        try (Connection con = plugin.getMariaDBManager().getConnection()) {
            if (playerExists(con, playerName)) {
                try (PreparedStatement ps = con.prepareStatement("UPDATE player_stats SET kills = kills + 1 WHERE username = ?")) {
                    ps.setString(1, playerName);
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = con.prepareStatement("INSERT INTO player_stats (username, kills, deaths) VALUES (?, 1, 0)")) {
                    ps.setString(1, playerName);
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void incrementDeaths(String playerName) {
        try (Connection con = plugin.getMariaDBManager().getConnection()) {
            if (playerExists(con, playerName)) {
                try (PreparedStatement ps = con.prepareStatement("UPDATE player_stats SET deaths = deaths + 1 WHERE username = ?")) {
                    ps.setString(1, playerName);
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = con.prepareStatement("INSERT INTO player_stats (username, kills, deaths) VALUES (?, 0, 1)")) {
                    ps.setString(1, playerName);
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private boolean playerExists(Connection con, String playerName) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT username FROM player_stats WHERE username = ?")) {
            ps.setString(1, playerName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
