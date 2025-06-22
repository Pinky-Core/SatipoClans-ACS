package me.lewisainsworth.satipoclans.Database;
import me.lewisainsworth.satipoclans.SatipoClan;
import org.bukkit.Bukkit;


import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.lewisainsworth.satipoclans.Utils.FileHandler;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.*;
import java.util.*;

public class MariaDBManager {
    private final SatipoClan plugin;
    private final FileConfiguration config;
    private HikariDataSource dataSource;
    private final Map<String, String> playerClanCache = new HashMap<>();
    private final Set<String> clanNamesCache = new HashSet<>();
    private final Map<String, String> clanColoredNameCache = new HashMap<>();
    private long lastCacheUpdate = 0;

    public MariaDBManager(FileConfiguration config) {
        this.config = config;
        this.plugin = SatipoClan.getInstance();

        String host = config.getString("storage.mariadb.host");
        int port = config.getInt("storage.mariadb.port");
        String database = config.getString("storage.mariadb.database");
        String user = config.getString("storage.mariadb.username");
        String password = config.getString("storage.mariadb.password");

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:mariadb://" + host + ":" + port + "/" + database + "?useSSL=false&autoReconnect=true");
        hikariConfig.setUsername(user);
        hikariConfig.setPassword(password);

        // IMPORTANTE: usar driver sombreado si usás shading
        hikariConfig.setDriverClassName("me.lewisainsworth.shaded.mariadb.jdbc.Driver");

        hikariConfig.setMaximumPoolSize(10);
        hikariConfig.setMinimumIdle(2);
        hikariConfig.setConnectionTimeout(10000);
        hikariConfig.setIdleTimeout(600000);
        hikariConfig.setMaxLifetime(1800000);

        dataSource = new HikariDataSource(hikariConfig);
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    public void setupTables() throws SQLException {
        try (Connection con = getConnection(); Statement stmt = con.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS clans (
                    name VARCHAR(255) PRIMARY KEY,
                    name_colored TEXT,
                    founder VARCHAR(36),
                    leader VARCHAR(36),
                    money DOUBLE,
                    privacy VARCHAR(12)
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS clan_users (
                    clan VARCHAR(255),
                    username VARCHAR(36),
                    PRIMARY KEY (clan, username)
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS alliances (
                    clan1 VARCHAR(255),
                    clan2 VARCHAR(255),
                    friendly_fire BOOLEAN DEFAULT FALSE,
                    PRIMARY KEY (clan1, clan2)
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS friendlyfire (
                    clan VARCHAR(255) PRIMARY KEY,
                    enabled BOOLEAN
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS banned_clans (
                    name VARCHAR(255) PRIMARY KEY,
                    reason TEXT
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS reports (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    clan VARCHAR(255),
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
                    current_clan VARCHAR(255),
                    history TEXT,
                    PRIMARY KEY (uuid)
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS clan_invites (
                    clan VARCHAR(255),
                    username VARCHAR(36),
                    PRIMARY KEY (clan, username),
                    invite_time BIGINT NOT NULL DEFAULT 0
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS pending_alliances (
                    requester VARCHAR(255),
                    target VARCHAR(255),
                    PRIMARY KEY (requester, target)
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS friendlyfire_allies (
                    clan VARCHAR(255) PRIMARY KEY,
                    enabled BOOLEAN
                )
            """);
        }

        try (Connection con = getConnection();
            Statement stmt = con.createStatement()) {

            ResultSet rs = con.getMetaData().getColumns(null, null, "clans", "name_colored");
            if (!rs.next()) {
                stmt.executeUpdate("ALTER TABLE clans ADD COLUMN name_colored TEXT");
                Bukkit.getLogger().info("Columna 'name_colored' agregada a la tabla 'clans'.");
            }

        } catch (SQLException e) {
            Bukkit.getLogger().severe("Error al verificar o agregar la columna 'name_colored': " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void syncFromYaml(FileConfiguration data) throws SQLException {
        if (!data.contains("Clans")) return;

        try (
            Connection con = getConnection();
            PreparedStatement insertClan = con.prepareStatement(
                "REPLACE INTO clans (name, founder, leader, money, privacy) VALUES (?, ?, ?, ?, ?)");
            PreparedStatement insertUser = con.prepareStatement(
                "REPLACE INTO clan_users (clan, username) VALUES (?, ?)")
        ) {
            Set<String> clans = data.getConfigurationSection("Clans").getKeys(false);
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
    }

    public void clearYamlClans(FileConfiguration data, FileHandler fh) {
        data.set("Clans", null);
        fh.saveData();
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
            PreparedStatement stmt2 = con.prepareStatement("SELECT name, name_colored FROM clans");
            ResultSet rs2 = stmt2.executeQuery()) {

            while (rs2.next()) {
                String name = rs2.getString("name");
                String colored = rs2.getString("name_colored");

                clanNamesCache.add(name);
                clanColoredNameCache.put(name, colored != null ? colored : name); // fallback
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

    public Map<String, String> getPlayerClanCache() {
        ensureCacheFresh();
        return playerClanCache;
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

    public String getColoredClanName(String clan) {
        ensureCacheFresh();
        return clanColoredNameCache.getOrDefault(clan, clan);
    }

}
