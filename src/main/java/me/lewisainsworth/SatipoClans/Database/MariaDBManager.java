package me.lewisainsworth.satipoclans.Database;

import me.lewisainsworth.satipoclans.Utils.FileHandler;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.*;
import java.util.*;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;


public class MariaDBManager {
    private final String host, database, user, password;
    private final int port;
    private Connection connection;

    private final Map<String, String> playerClanCache = new HashMap<>();
    private final Set<String> clanNamesCache = new HashSet<>();
    private long lastCacheUpdate = 0;

    public MariaDBManager(FileConfiguration config) {
        this.host = config.getString("storage.mariadb.host");
        this.port = config.getInt("storage.mariadb.port");
        this.database = config.getString("storage.mariadb.database");
        this.user = config.getString("storage.mariadb.username");
        this.password = config.getString("storage.mariadb.password");
    }

    public void connect() throws SQLException {
        if (connection != null && !connection.isClosed()) return;
        try {
            Class.forName("me.lewisainsworth.shaded.mariadb.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        String url = "jdbc:mariadb://" + host + ":" + port + "/" + database + "?useSSL=false";
        connection = DriverManager.getConnection(url, user, password);
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setupTables() throws SQLException {
        Statement stmt = connection.createStatement();

        stmt.executeUpdate("""
            CREATE TABLE IF NOT EXISTS clans (
                name VARCHAR(36) PRIMARY KEY,
                founder VARCHAR(36),
                leader VARCHAR(36),
                money DOUBLE,
                privacy VARCHAR(12)
            )
        """);

        stmt.executeUpdate("""
            CREATE TABLE IF NOT EXISTS clan_users (
                clan VARCHAR(36),
                username VARCHAR(36),
                PRIMARY KEY (clan, username)
            )
        """);

        stmt.executeUpdate("""
            CREATE TABLE IF NOT EXISTS alliances (
                clan1 VARCHAR(36),
                clan2 VARCHAR(36),
                PRIMARY KEY (clan1, clan2)
            )
        """);

        stmt.executeUpdate("""
            CREATE TABLE IF NOT EXISTS friendlyfire (
                clan VARCHAR(36) PRIMARY KEY,
                enabled BOOLEAN
            )
        """);

        stmt.executeUpdate("""
            CREATE TABLE IF NOT EXISTS banned_clans (
                name VARCHAR(36) PRIMARY KEY,
                reason TEXT
            )
        """);

        stmt.executeUpdate("""
            CREATE TABLE IF NOT EXISTS reports (
                id INT AUTO_INCREMENT PRIMARY KEY,
                clan VARCHAR(36),
                reason TEXT
            )
        """);

        stmt.executeUpdate("""
            CREATE TABLE IF NOT EXISTS economy_players (
                player VARCHAR(36) PRIMARY KEY,
                balance DOUBLE
            )
        """);

        stmt.executeUpdate("""
            CREATE TABLE IF NOT EXISTS player_clan_history (
                uuid VARCHAR(36) NOT NULL,
                name VARCHAR(16),
                current_clan VARCHAR(32),
                history TEXT,
                PRIMARY KEY (uuid)
            )
        """);
    }

    public void syncFromYaml(FileConfiguration data) throws SQLException {
        if (!data.contains("Clans")) return;
        Set<String> clans = data.getConfigurationSection("Clans").getKeys(false);
        PreparedStatement insertClan = connection.prepareStatement(
            "REPLACE INTO clans (name, founder, leader, money, privacy) VALUES (?, ?, ?, ?, ?)"
        );
        PreparedStatement insertUser = connection.prepareStatement(
            "REPLACE INTO clan_users (clan, username) VALUES (?, ?)"
        );

        for (String clan : clans) {
            String path = "Clans." + clan;
            insertClan.setString(1, clan);
            insertClan.setString(2, data.getString(path + ".Founder"));
            insertClan.setString(3, data.getString(path + ".Leader"));
            insertClan.setDouble(4, data.getDouble(path + ".Money"));
            insertClan.setString(5, data.getString(path + ".Privacy"));
            insertClan.executeUpdate();

            List<String> users = data.getStringList(path + ".Users");
            for (String user : users) {
                insertUser.setString(1, clan);
                insertUser.setString(2, user);
                insertUser.executeUpdate();
            }
        }
    }

    public void clearYamlClans(FileConfiguration data, FileHandler fh) {
        data.set("Clans", null);
        fh.saveData();
    }

    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connect();
        }
        return connection;
    }

    public void reloadCache() {
        playerClanCache.clear();
        clanNamesCache.clear();

        try (Connection con = getConnection();
             PreparedStatement stmt1 = con.prepareStatement("SELECT username, clan FROM clan_users");
             ResultSet rs1 = stmt1.executeQuery()) {

            while (rs1.next()) {
                playerClanCache.put(rs1.getString("username").toLowerCase(), rs1.getString("clan"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        try (Connection con = getConnection();
             PreparedStatement stmt2 = con.prepareStatement("SELECT name FROM clans");
             ResultSet rs2 = stmt2.executeQuery()) {

            while (rs2.next()) {
                clanNamesCache.add(rs2.getString("name"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        lastCacheUpdate = System.currentTimeMillis();
    }

    public String getCachedPlayerClan(String playerName) {
        ensureCacheFresh();
        return playerClanCache.getOrDefault(playerName.toLowerCase(), null);
    }

    public List<String> getCachedClanNames() {
        ensureCacheFresh();
        return new ArrayList<>(clanNamesCache);
    }

    private void ensureCacheFresh() {
        if (System.currentTimeMillis() - lastCacheUpdate > 5 * 60 * 1000) {
            reloadCache();
        }
    }
}
