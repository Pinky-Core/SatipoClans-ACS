package me.lewisainsworth.satipoclans.CMDs;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import me.lewisainsworth.satipoclans.SatipoClan;
import me.lewisainsworth.satipoclans.Utils.Econo;
import me.lewisainsworth.satipoclans.Utils.FileHandler;
import me.lewisainsworth.satipoclans.Utils.MSG;
import static me.lewisainsworth.satipoclans.SatipoClan.prefix;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;



public class ACMD implements CommandExecutor, TabCompleter {

    private final SatipoClan plugin;

    public ACMD(SatipoClan plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String s, String[] args) {
        if (!(sender instanceof Player)) return handleConsole(sender, args);

        if (!sender.hasPermission("sc.admin")) {
            sender.sendMessage(MSG.color(prefix + "&c You don't have permission to use this command."));
            return true;
        }

        if (args.length == 0) {
            help(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> reload(sender);
            case "ban" -> ban(sender, args);
            case "unban" -> unban(sender, args);
            case "clear" -> clear(sender);
            case "reports" -> reports(sender);
            case "economy" -> economy(sender, args);
            default -> help(sender);
        }

        return true;
    }

    private boolean handleConsole(CommandSender sender, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            reload(sender);
        } else {
            sender.sendMessage(MSG.color(prefix + "&c Console can only use: &f/cla reload"));
        }
        return true;
    }

    private void help(CommandSender sender) {
        sender.sendMessage(MSG.color("""
                &8&m=====================================
                &8&l» &a&lSatipo&6&lClans &c&lAdmin &8&l«
                &8&m=====================================
                &e/cla reports &7» &fMuestra todos los clanes con reportes activos.
                &e/cla reload &7» &fRecarga la configuración y datos del plugin.
                &e/cla ban <clan> [razón] &7» &fProhíbe un clan permanentemente (permiso por defecto).
                &e/cla unban <clan> &7» &fLevanta la prohibición de un clan.
                &e/cla clear &7» &c⚠ Borra toda la base de datos MariaDB (¡Usar con extrema precaución!).
                &e/cla economy <player|clan> <nombre> <set|add|reset> <cantidad> &7» &fGestiona la economía de un clan o jugador.
                &8&m=====================================
                """));
    }


    private void reload(CommandSender sender) {
        FileHandler fh = plugin.getFH();
        Econo econ = SatipoClan.getEcon();
        fh.reloadConfig();
        fh.reloadData();
        econ.reload();
        sender.sendMessage(MSG.color(prefix + "&a Plugin and all files reloaded."));
    }

    private void clear(CommandSender sender) {
        try (Connection con = plugin.getMariaDBManager().getConnection();
            Statement stmt = con.createStatement()) {

            stmt.executeUpdate("DELETE FROM reports");
            stmt.executeUpdate("DELETE FROM banned_clans");
            stmt.executeUpdate("DELETE FROM clan_users");
            stmt.executeUpdate("DELETE FROM alliances");
            stmt.executeUpdate("DELETE FROM friendlyfire");
            stmt.executeUpdate("DELETE FROM clans");
            stmt.executeUpdate("DELETE FROM economy_players");

            sender.sendMessage(MSG.color(prefix + "&c All clan-related data wiped from MariaDB."));
        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage(MSG.color(prefix + "&c Failed to wipe MariaDB."));
        }
    }


    private void reports(CommandSender sender) {
        try (Connection con = plugin.getMariaDBManager().getConnection();
            PreparedStatement stmt = con.prepareStatement("SELECT clan, reason FROM reports");
            ResultSet rs = stmt.executeQuery()) {

            Map<String, List<String>> reported = new HashMap<>();

            while (rs.next()) {
                String clan = rs.getString("clan");
                String reason = rs.getString("reason");
                reported.computeIfAbsent(clan, k -> new ArrayList<>()).add(reason);
            }

            if (reported.isEmpty()) {
                sender.sendMessage(MSG.color(prefix + "&a No clans with reports."));
                return;
            }

            sender.sendMessage(MSG.color("&e--- &6Clan Reports &e---"));
            reported.forEach((clan, reasons) -> {
                sender.sendMessage(MSG.color("&6" + clan + ":"));
                reasons.forEach(reason -> sender.sendMessage(MSG.color("  &7- &f" + reason)));
            });

        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage(MSG.color(prefix + "&c Failed to load reports."));
        }
    }


    private void ban(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MSG.color(prefix + "&c Usage: /cla ban <clan> [reason]"));
            return;
        }

        String clan = args[1];
        String reason = args.length >= 3 ? args[2] : "Banned by admin";

        try (Connection con = plugin.getMariaDBManager().getConnection();
            PreparedStatement check = con.prepareStatement("SELECT name FROM clans WHERE name = ?");
            PreparedStatement ban = con.prepareStatement("REPLACE INTO banned_clans (name, reason) VALUES (?, ?)");
            PreparedStatement members = con.prepareStatement("SELECT username FROM clan_users WHERE clan = ?")) {

            check.setString(1, clan);
            ResultSet rs = check.executeQuery();

            if (!rs.next()) {
                sender.sendMessage(MSG.color(prefix + "&c Clan '" + clan + "' doesn't exist."));
                return;
            }

            ban.setString(1, clan);
            ban.setString(2, reason);
            ban.executeUpdate();

            members.setString(1, clan);
            ResultSet mrs = members.executeQuery();
            while (mrs.next()) {
                String user = mrs.getString("username");
                Player player = Bukkit.getPlayer(user);
                if (player != null) {
                    player.kickPlayer(MSG.color("&cYou have been banned from your clan."));
                    player.ban(reason, (Date) null, "SatipoClans", true);
                }
            }

            sender.sendMessage(MSG.color(prefix + "&c Clan '" + clan + "' has been banned."));

        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage(MSG.color(prefix + "&c Failed to ban the clan."));
        }
    }


    private void economy(CommandSender sender, String[] args) {
        if (args.length < 4 || (args.length < 5 && !args[3].equalsIgnoreCase("reset"))) {
            sender.sendMessage(MSG.color(prefix + "&c Usage: /cla economy <player|clan> <name> <set|add|reset> <amount>"));
            return;
        }

        String type = args[1].toLowerCase(Locale.ROOT);
        String name = args[2];
        String action = args[3].toLowerCase(Locale.ROOT);
        String amountStr = args.length >= 5 ? args[4] : "0";

        if (!action.equals("set") && !action.equals("add") && !action.equals("reset")) {
            sender.sendMessage(MSG.color(prefix + "&c Invalid action. Use set, add or reset."));
            return;
        }

        double amount = 0;
        if (!action.equals("reset")) {
            try {
                amount = Double.parseDouble(amountStr);
                if (amount < 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                sender.sendMessage(MSG.color(prefix + "&c Invalid amount."));
                return;
            }
        }

        if (type.equals("player")) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(name);
            if (!(player.hasPlayedBefore() || player.isOnline())) {
                sender.sendMessage(MSG.color(prefix + "&c Player '" + name + "' not found."));
                return;
            }
            modifyPlayerEcon(sender, player, action, amount);
            return;
        }

        if (type.equals("clan")) {
            String sql = """
                UPDATE clans
                SET money = CASE
                    WHEN ? = 'set' THEN ?
                    WHEN ? = 'add' THEN money + ?
                    WHEN ? = 'reset' THEN 0
                    ELSE money
                END
                WHERE name = ?
            """;

            try (Connection con = plugin.getMariaDBManager().getConnection();
                PreparedStatement stmt = con.prepareStatement(sql)) {

                stmt.setString(1, action);
                stmt.setDouble(2, amount);
                stmt.setString(3, action);
                stmt.setDouble(4, amount);
                stmt.setString(5, action);
                stmt.setString(6, name);

                int rowsUpdated = stmt.executeUpdate();

                if (rowsUpdated == 0) {
                    sender.sendMessage(MSG.color(prefix + "&c Clan '" + name + "' doesn't exist."));
                    return;
                }

                String message = prefix + "&a Clan economy updated: &f" + name + " &7-> &f" + action;
                if (!action.equals("reset")) {
                    message += " &7= &f" + amount;
                } else {
                    message += " &7= &f0";
                }
                sender.sendMessage(MSG.color(message));
            } catch (SQLException e) {
                e.printStackTrace();
                sender.sendMessage(MSG.color(prefix + "&c Failed to update clan economy."));
            }
            return;
        }

        sender.sendMessage(MSG.color(prefix + "&c First argument must be 'player' or 'clan'."));
    }


    private void modifyPlayerEcon(CommandSender sender, OfflinePlayer p, String action, double amount) {
        Econo econ = SatipoClan.getEcon();
        double current = econ.getBalance(p);

        switch (action) {
            case "set" -> {
                if (amount > current) econ.deposit(p, amount - current);
                else econ.withdraw(p, current - amount);
                sender.sendMessage(MSG.color("&aSet &f" + p.getName() + "&a's balance to &f" + amount));
            }
            case "add" -> {
                econ.deposit(p, amount);
                sender.sendMessage(MSG.color("&aAdded &f" + amount + "&a to &f" + p.getName()));
            }
            case "reset" -> {
                econ.withdraw(p, current);
                sender.sendMessage(MSG.color("&aReset &f" + p.getName() + "&a's balance."));
            }
            default -> sender.sendMessage(MSG.color(prefix + "&c Invalid action."));
        }
    }


    private void unban(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MSG.color(prefix + "&c Usage: /cla unban <clan>"));
            return;
        }

        String clan = args[1];

        try (Connection con = plugin.getMariaDBManager().getConnection();
            PreparedStatement check = con.prepareStatement("SELECT name FROM clans WHERE name = ?");
            PreparedStatement unban = con.prepareStatement("DELETE FROM banned_clans WHERE name = ?");
            PreparedStatement members = con.prepareStatement("SELECT username FROM clan_users WHERE clan = ?")) {

            check.setString(1, clan);
            ResultSet rs = check.executeQuery();

            if (!rs.next()) {
                sender.sendMessage(MSG.color(prefix + "&c Clan '" + clan + "' doesn't exist."));
                return;
            }

            unban.setString(1, clan);
            unban.executeUpdate();

            members.setString(1, clan);
            ResultSet mrs = members.executeQuery();
            while (mrs.next()) {
                Bukkit.getBanList(org.bukkit.BanList.Type.NAME).pardon(mrs.getString("username"));
            }

            sender.sendMessage(MSG.color(prefix + "&a Clan '" + clan + "' has been unbanned."));

        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage(MSG.color(prefix + "&c Failed to unban the clan."));
        }
    }


    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String s, String[] args) {
        if (!(sender instanceof Player p) || !p.hasPermission("sc.admin")) {
            return args.length == 1 ? List.of("reload") : Collections.emptyList();
        }

        if (args.length == 1) {
            return Stream.of("reload", "ban", "unban", "clear", "reports", "economy")
                    .filter(sub -> sub.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        String arg0 = args[0].toLowerCase();

        if (arg0.equals("economy")) {
            if (args.length == 2) {
                return Stream.of("player", "clan")
                        .filter(opt -> opt.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }

            if (args.length == 3) {
                if (args[1].equalsIgnoreCase("player")) {
                    return Arrays.stream(Bukkit.getOfflinePlayers())
                            .map(OfflinePlayer::getName)
                            .filter(Objects::nonNull)
                            .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                            .collect(Collectors.toList());
                }
                if (args[1].equalsIgnoreCase("clan")) {
                    return getClanNames().stream()
                            .filter(clan -> clan.toLowerCase().startsWith(args[2].toLowerCase()))
                            .collect(Collectors.toList());
                }
                return Collections.emptyList();
            }

            if (args.length == 4) {
                return Stream.of("set", "add", "reset")
                        .filter(opt -> opt.startsWith(args[3].toLowerCase()))
                        .collect(Collectors.toList());
            }

            if (args.length == 5 && !args[3].equalsIgnoreCase("reset")) {
                return Stream.of("100", "500", "1000", "10000")
                        .filter(a -> a.startsWith(args[4]))
                        .collect(Collectors.toList());
            }

            return Collections.emptyList();
        }

        if ((arg0.equals("ban") || arg0.equals("unban")) && args.length == 2) {
            return getClanNames().stream()
                    .filter(c -> c.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (arg0.equals("ban") && args.length == 3) {
            return Stream.of("cheating", "toxicity", "abuse")
                    .filter(reason -> reason.startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    private List<String> getClanNames() {
        FileConfiguration data = plugin.getFH().getData();
        if (data.contains("Clans")) {
            return new ArrayList<>(Objects.requireNonNull(data.getConfigurationSection("Clans")).getKeys(false));
        }
        return Collections.emptyList();
    }
}