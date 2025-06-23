package me.lewisainsworth.satipoclans.Events;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import me.lewisainsworth.satipoclans.SatipoClan;
import me.lewisainsworth.satipoclans.Utils.Econo;
import me.lewisainsworth.satipoclans.Utils.FileHandler;
import me.lewisainsworth.satipoclans.Utils.MSG;
import me.lewisainsworth.satipoclans.Utils.ClanUtils;
import me.lewisainsworth.satipoclans.Utils.ClanUtils;

import static me.lewisainsworth.satipoclans.SatipoClan.prefix;

import java.sql.*;
import java.util.*;

public class Events implements Listener {
    private final SatipoClan plugin;

    public Events(SatipoClan plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        FileHandler fh = plugin.getFH();
        FileConfiguration config = fh.getConfig();

        if (!config.getBoolean("welcome-message.enabled")) return;

        String clan = getPlayerClan(player.getName());

        if (clan == null) {
            config.getStringList("welcome-message.no-clan").forEach(
                msg -> player.sendMessage(MSG.color(msg))
            );
        } else {
            List<String> clanUsers = getClanUsers(clan);

            // Mensajes para el jugador que se unió
            for (String line : config.getStringList("welcome-message.self-clan")) {
                player.sendMessage(MSG.color(player, line));
            }

            // Mensajes para los demás miembros del clan
            for (String u : clanUsers) {
                if (!u.equalsIgnoreCase(player.getName())) {
                    Player target = Bukkit.getPlayerExact(u);
                    if (target != null && target.isOnline()) {
                        for (String line : config.getStringList("welcome-message.to-clan")) {
                            target.sendMessage(MSG.color(player, line));
                        }
                    }
                }
            }
        }

        List<String> invites = getInvites(player.getName());
        if (!invites.isEmpty()) {
            player.sendMessage(MSG.color(prefix + " &eFuiste invitado a un clan:"));
            invites.forEach(c -> player.sendMessage(MSG.color("&7- &a" + c + " &7(/clan join " + c + ")")));
        }
    }

    @EventHandler
    public void onKill(PlayerDeathEvent event) {
        FileHandler fh = plugin.getFH();
        if (!fh.getConfig().getBoolean("economy.enabled")) return;

        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer != null) {
            int killReward = fh.getConfig().getInt("economy.earn.kill-enemy");
            SatipoClan.econ.deposit(killer, killReward);
            killer.sendMessage(MSG.color(prefix + "&2 Ganaste: &e&l" + killReward));
        }
    }

    @EventHandler
    public void onFriendlyFire(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player damager)) return;

        String clanVictim = getPlayerClan(victim.getName());
        String clanDamager = getPlayerClan(damager.getName());

        if (clanVictim == null || clanDamager == null) return;

        if (clanVictim.equals(clanDamager)) {
            // Fuego amigo dentro del mismo clan
            if (!isFriendlyFireEnabled(clanVictim)) {
                event.setCancelled(true);
            }
        } else if (areClansAllied(clanVictim, clanDamager)) {
            // Fuego amigo entre clanes aliados
            if (!ClanUtils.isFriendlyFireEnabledAllies(clanVictim) || !ClanUtils.isFriendlyFireEnabledAllies(clanDamager)) {
                event.setCancelled(true);
            }
        }
    }



    private boolean areClansAllied(String clan1, String clan2) {
        String sql = "SELECT 1 FROM alliances WHERE (clan1 = ? AND clan2 = ?) OR (clan1 = ? AND clan2 = ?)";
        try (Connection con = plugin.getMariaDBManager().getConnection();
            PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, clan1);
            ps.setString(2, clan2);
            ps.setString(3, clan2);
            ps.setString(4, clan1);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }


    private String getPlayerClan(String playerName) {
        String clan = null;
        String sql = "SELECT clan FROM clan_users WHERE username = ? LIMIT 1";
        try (Connection con = plugin.getMariaDBManager().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, playerName);
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

    private List<String> getClanUsers(String clan) {
        List<String> users = new ArrayList<>();
        String sql = "SELECT username FROM clan_users WHERE clan = ?";
        try (Connection con = plugin.getMariaDBManager().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, clan);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    users.add(rs.getString("username"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    private List<String> getInvites(String playerName) {
        List<String> invites = new ArrayList<>();
        String sql = "SELECT clan FROM clan_invites WHERE username = ?";
        try (Connection con = plugin.getMariaDBManager().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, playerName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    invites.add(rs.getString("clan"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return invites;
    }

    private boolean isFriendlyFireEnabled(String clan) {
        boolean enabled = false; // Por defecto off
        String sql = "SELECT enabled FROM friendlyfire WHERE clan = ?";
        try (Connection con = plugin.getMariaDBManager().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, clan);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    enabled = rs.getBoolean("enabled");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return enabled;
    }
}
