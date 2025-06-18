package me.lewisainsworth.satipoclans.Database;

import org.bukkit.configuration.file.FileConfiguration;
import me.lewisainsworth.satipoclans.Utils.FileHandler;

import java.sql.*;
import java.util.List;
import java.util.Set;

public class MariaDBManager {
    private final String host, database, user, password;
    private final int port;
    private Connection connection;

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
            Class.forName("org.mariadb.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        System.out.println("⛓️ Conectando a MariaDB con:");
        System.out.println("Host: " + host);
        System.out.println("Port: " + port);
        System.out.println("Database: " + database);
        System.out.println("User: " + user);

        String url = "jdbc:mariadb://" + host + ":" + port + "/" + database + "?useSSL=false";

        connection = DriverManager.getConnection(url, user, password);
        setupTables();
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void setupTables() throws SQLException {
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
            connect(); // abre o reabre la conexión
        }
        return connection;
    }

} 
