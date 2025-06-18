package me.lewisainsworth.satipoclans.Events;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import me.lewisainsworth.satipoclans.SatipoClan;
import me.lewisainsworth.satipoclans.Utils.Econo;
import me.lewisainsworth.satipoclans.Utils.FileHandler;
import me.lewisainsworth.satipoclans.Utils.MSG;
import static me.lewisainsworth.satipoclans.SatipoClan.prefix;

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
        FileConfiguration data = fh.getData();

        if (!config.getBoolean("welcome-message.enabled")) return;

        String clan = getPlayerClan(player.getName());

        if (clan == null) {
            config.getStringList("welcome-message.no-clan").forEach(
                    msg -> player.sendMessage(MSG.color(msg))
            );
        } else {
            List<String> users = data.getStringList("Clans." + clan + ".Users");
            for (String u : users) {
                Player p = Bukkit.getPlayerExact(u);
                if (p != null) {
                    p.isOnline();
                }
            }

            for (String line : config.getStringList("welcome-message.self-clan")) {
                player.sendMessage(MSG.color(player, line));
            }

            for (String u : users) {
                Player target = Bukkit.getPlayerExact(u);
                if (target != null && target.isOnline() && !target.getName().equalsIgnoreCase(player.getName())) {
                    for (String line : config.getStringList("welcome-message.to-clan")) {
                        target.sendMessage(MSG.color(player, line));
                    }
                }
            }
        }

        List<String> invites = new ArrayList<>();
        if (data.isConfigurationSection("Clans")) {
            for (String id : Objects.requireNonNull(data.getConfigurationSection("Clans")).getKeys(false)) {
                List<String> list = data.getStringList("Clans." + id + ".Invitations");
                if (list.stream().anyMatch(inv -> inv.equalsIgnoreCase(player.getName()))) invites.add(id);
            }
        }

        if (!invites.isEmpty()) {
            player.sendMessage(MSG.color(prefix + "&eYou are invited to these clans:"));
            invites.forEach(c -> player.sendMessage(MSG.color("&7- &a" + c + " &7(/cl join " + c + ")")));
        }
    }

    @EventHandler
    public void onKill(PlayerDeathEvent event) {
        Econo econ = SatipoClan.getEcon();
        FileHandler fh = plugin.getFH();
        int killReward = fh.getConfig().getInt("economy.earn.kill-enemy");

        if (fh.getConfig().getBoolean("economy.enabled")) {
            Player victim = event.getEntity();
            Player killer = victim.getKiller();

            if (killer != null) {
                SatipoClan.econ.deposit(killer, killReward);
                killer.sendMessage(MSG.color(prefix + "&2You Won: &e&l" + killReward));
            }
        }
    }

    private String getPlayerClan(String playerName) {
        FileHandler fh = plugin.getFH();
        FileConfiguration data = fh.getData();
        ConfigurationSection clans = data.getConfigurationSection("Clans");
        if (clans != null) {
            for (String clan : clans.getKeys(false)) {
                List<String> users = data.getStringList("Clans." + clan + ".Users");
                if (users.contains(playerName)) return clan;
            }
        }
        return null;
    }
}