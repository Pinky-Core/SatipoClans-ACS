package me.lewisainsworth.satipoclans.Database;
import me.lewisainsworth.satipoclans.SatipoClan;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import me.lewisainsworth.satipoclans.Utils.MSG;


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

        hikariConfig.setMaximumPoolSize(75);
        hikariConfig.setMinimumIdle(20);          
        hikariConfig.setConnectionTimeout(10000);  
        hikariConfig.setIdleTimeout(300000);       
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

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS clan_homes (
                    clan VARCHAR(255) PRIMARY KEY,
                    world VARCHAR(64),
                    x DOUBLE,
                    y DOUBLE,
                    z DOUBLE
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS player_stats (
                    username VARCHAR(16) PRIMARY KEY,
                    kills INT DEFAULT 0,
                    deaths INT DEFAULT 0
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

    public void deleteClanHome(String clan) {
        try (Connection con = getConnection();
            PreparedStatement ps = con.prepareStatement("DELETE FROM clan_homes WHERE clan = ?")) {
            ps.setString(1, clan);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public void forceJoin(String playerName, String clanName) {
        try (Connection con = getConnection()) {
            // Verificar si el clan existe
            try (PreparedStatement checkClan = con.prepareStatement("SELECT name FROM clans WHERE name = ?")) {
                checkClan.setString(1, clanName);
                try (ResultSet rs = checkClan.executeQuery()) {
                    if (!rs.next()) {
                        System.out.println("[SatipoClans] Clan '" + clanName + "' no existe.");
                        return;
                    }
                }
            }

            // Eliminar usuario de cualquier clan anterior
            try (PreparedStatement removeOld = con.prepareStatement("DELETE FROM clan_users WHERE username = ?")) {
                removeOld.setString(1, playerName);
                removeOld.executeUpdate();
            }

            // Insertar al nuevo clan
            try (PreparedStatement insert = con.prepareStatement("INSERT INTO clan_users (clan, username) VALUES (?, ?)")) {
                insert.setString(1, clanName);
                insert.setString(2, playerName);
                insert.executeUpdate();
            }

            System.out.println("[SatipoClans] Jugador " + playerName + " forzado a unirse al clan " + clanName + ".");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void forceLeave(String playerName) {
        try (Connection con = getConnection();
            PreparedStatement stmt = con.prepareStatement("DELETE FROM clan_users WHERE username = ?")) {

            stmt.setString(1, playerName);
            int affected = stmt.executeUpdate();

            if (affected > 0) {
                System.out.println("[SatipoClans] Jugador " + playerName + " fue forzado a salir del clan.");
            } else {
                System.out.println("[SatipoClans] El jugador " + playerName + " no estaba en ningún clan.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteClan(String clanName) {
        try (Connection con = getConnection()) {
            // Eliminar aliados
            try (PreparedStatement stmt = con.prepareStatement("DELETE FROM alliances WHERE clan1 = ? OR clan2 = ?")) {
                stmt.setString(1, clanName);
                stmt.setString(2, clanName);
                stmt.executeUpdate();
            }

            // Eliminar usuarios del clan
            try (PreparedStatement stmt = con.prepareStatement("DELETE FROM clan_users WHERE clan = ?")) {
                stmt.setString(1, clanName);
                stmt.executeUpdate();
            }

            // Eliminar solicitudes de alianza pendientes
            try (PreparedStatement stmt = con.prepareStatement("DELETE FROM pending_alliances WHERE requester = ? OR target = ?")) {
                stmt.setString(1, clanName);
                stmt.setString(2, clanName);
                stmt.executeUpdate();
            }

            // Eliminar home
            try (PreparedStatement stmt = con.prepareStatement("DELETE FROM clan_homes WHERE clan = ?")) {
                stmt.setString(1, clanName);
                stmt.executeUpdate();
            }

            // Eliminar configuraciones de FF
            try (PreparedStatement stmt = con.prepareStatement("DELETE FROM friendlyfire WHERE clan = ?")) {
                stmt.setString(1, clanName);
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = con.prepareStatement("DELETE FROM friendlyfire_allies WHERE clan = ?")) {
                stmt.setString(1, clanName);
                stmt.executeUpdate();
            }

            // Eliminar invitaciones
            try (PreparedStatement stmt = con.prepareStatement("DELETE FROM clan_invites WHERE clan = ?")) {
                stmt.setString(1, clanName);
                stmt.executeUpdate();
            }

            // Eliminar reportes
            try (PreparedStatement stmt = con.prepareStatement("DELETE FROM reports WHERE clan = ?")) {
                stmt.setString(1, clanName);
                stmt.executeUpdate();
            }

            // Finalmente eliminar el clan
            try (PreparedStatement stmt = con.prepareStatement("DELETE FROM clans WHERE name = ?")) {
                stmt.setString(1, clanName);
                int affected = stmt.executeUpdate();

                if (affected > 0) {
                    System.out.println("[SatipoClans] Clan '" + clanName + "' eliminado correctamente.");
                } else {
                    System.out.println("[SatipoClans] El clan '" + clanName + "' no existe.");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public HikariDataSource getDataSource() {
        return this.dataSource;
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

    public void setClanHome(String clan, Location location) {
        try (Connection con = getConnection();
            PreparedStatement ps = con.prepareStatement("""
                REPLACE INTO clan_homes (clan, world, x, y, z) VALUES (?, ?, ?, ?, ?)
            """)) {
            ps.setString(1, clan);
            ps.setString(2, location.getWorld().getName());
            ps.setDouble(3, location.getX());
            ps.setDouble(4, location.getY());
            ps.setDouble(5, location.getZ());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Location getClanHome(String clan) {
        try (Connection con = getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM clan_homes WHERE clan = ?")) {
            ps.setString(1, clan);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String world = rs.getString("world");
                    double x = rs.getDouble("x");
                    double y = rs.getDouble("y");
                    double z = rs.getDouble("z");
                    World bukkitWorld = Bukkit.getWorld(world);
                    if (bukkitWorld == null) return null;
                    return new Location(bukkitWorld, x, y, z);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
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


    public double getKillDeathRatio(String playerName) {
        try (Connection con = plugin.getMariaDBManager().getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT kills, deaths FROM player_stats WHERE username = ?")) {
            ps.setString(1, playerName);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int kills = rs.getInt("kills");
                int deaths = rs.getInt("deaths");
                if (deaths == 0) return kills;
                return (double) kills / deaths;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    public void fixClanColorsAsync(CommandSender sender) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            final int[] fixed = {0}; // Usamos un array de 1 elemento para contador mutable
            try (Connection con = getConnection();
                PreparedStatement select = con.prepareStatement("SELECT name_colored, name FROM clans WHERE name_colored != name");
                PreparedStatement update = con.prepareStatement("UPDATE clans SET name_colored=? WHERE name=?")) {

                ResultSet rs = select.executeQuery();
                while (rs.next()) {
                    String raw = rs.getString("name");

                    update.setString(1, raw);
                    update.setString(2, raw);
                    update.addBatch();
                    fixed[0]++;
                }
                update.executeBatch();

                Bukkit.getScheduler().runTask(plugin, () -> {
                    plugin.getMariaDBManager().reloadCache();
                    sender.sendMessage(MSG.color("&aSe han corregido &f" + fixed[0] + " &aclanes, eliminando colores del nombre y reparando la detección de clan."));
                });

            } catch (SQLException e) {
                e.printStackTrace();
                Bukkit.getScheduler().runTask(plugin, () ->
                    sender.sendMessage(MSG.color("&cOcurrió un error al intentar reparar los colores de los clanes. Revisa la consola."))
                );
            }
        });
    }



    /**
     * Obtiene el nombre coloreado de un clan, o devuelve el nombre plano si no existe.
     */
    public String getColoredName(String clanName) {
        if (clanName == null) return null;
        try (Connection con = getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT name_colored FROM clans WHERE name=?")) {
            ps.setString(1, clanName);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String colored = rs.getString("name_colored");
                return colored != null ? colored : clanName;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return clanName;
    }

    /**
     * Obtiene el nombre plano (raw) de un clan dado un nombre coloreado.
     */
    public String getRawNameFromColored(String coloredName) {
        if (coloredName == null) return null;
        try (Connection con = getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT name FROM clans WHERE name_colored=?")) {
            ps.setString(1, coloredName);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("name");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Verifica si existe un clan por nombre plano.
     */
    public boolean clanExists(String clanName) {
        try (Connection con = getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT 1 FROM clans WHERE name=?")) {
            ps.setString(1, clanName);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
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
