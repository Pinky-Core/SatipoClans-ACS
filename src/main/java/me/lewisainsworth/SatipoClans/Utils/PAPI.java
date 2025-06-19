package me.lewisainsworth.satipoclans.Utils;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import me.lewisainsworth.satipoclans.SatipoClan;

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
        return "sc";
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
        if (player == null) return "no player";

        Econo econ = plugin.getEcon();
        String clanName = getPlayerClan(player.getName());
        if (clanName == null) return "no clan";

        switch (identifier.toLowerCase()) {
            case "prefix": {
                return SatipoClan.prefix;
            }
            case "player_money": {
                return String.valueOf(econ.getBalance(player));
            }
            case "clan_leader": {
                return data.getString("Clans." + clanName + ".Leader", "N/A");
            }
            case "clan_founder": {
                return data.getString("Clans." + clanName + ".Founder", "N/A");
            }
            case "clan_name": {
                return clanName;
            }
            case "clan_tag": {
                return data.getString("Clans." + clanName + ".Tag", "N/A");
            }
            case "clan_money": {
                return data.getString("Clans." + clanName + ".Money", "0");
            }
            case "clan_membercount": {
                List<String> users = data.getStringList("Clans." + clanName + ".Users");
                return Integer.toString(users.size());
            }
            case "clan_membercount_online": {
                List<String> users = data.getStringList("Clans." + clanName + ".Users");
                long online = Bukkit.getOnlinePlayers().stream().filter(p -> users.contains(p.getName())).count();
                return Long.toString(online);
            }
            case "clan_membercount_offline": {
                List<String> users = data.getStringList("Clans." + clanName + ".Users");
                long online = Bukkit.getOnlinePlayers().stream().filter(p -> users.contains(p.getName())).count();
                return Long.toString(users.size() - online);
            }
            default:
                return "&a&lSatipo&6&lClans";
        }
    }

    private String getPlayerClan(String playerName) {
        if (playerName == null) return null;
        playerName = playerName.trim().toLowerCase();

        ConfigurationSection clans = data.getConfigurationSection("Clans");
        if (clans == null) return null;

        for (String clan : clans.getKeys(false)) {
            List<String> users = data.getStringList("Clans." + clan + ".Users");
            for (String user : users) {
                if (user != null && user.trim().toLowerCase().equals(playerName)) {
                    return clan;
                }
            }
        }
        return null;
    }

    public void registerPlaceholders() {
        if (!register()) plugin.getLogger().warning("Failed to register SatipoClans placeholders.");
    }
}
