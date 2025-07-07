package me.lewisainsworth.satipoclans.CMDs;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import me.lewisainsworth.satipoclans.SatipoClan;
import me.lewisainsworth.satipoclans.Utils.Econo;
import me.lewisainsworth.satipoclans.Utils.FileHandler;
import me.lewisainsworth.satipoclans.Utils.MSG;
import me.lewisainsworth.satipoclans.Utils.LangManager;
import static me.lewisainsworth.satipoclans.SatipoClan.prefix;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.io.OutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.io.InputStream;
import java.util.regex.Pattern;


import com.zaxxer.hikari.HikariDataSource;

import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;




public class ACMD implements CommandExecutor, TabCompleter {

    private final SatipoClan plugin;

    private String lastMessageId = null;

    private final LangCMD langCMD;
    private final LangManager langManager;

    public ACMD(SatipoClan plugin) {
        this.plugin = plugin;
        this.langManager = plugin.getLangManager();
        this.langCMD = new LangCMD(plugin);
    }


    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String s, String[] args) {
        if (!(sender instanceof Player)) return handleConsole(sender, args);

        if (!sender.hasPermission("satipoclans.admin")) {
            sender.sendMessage(MSG.color(langManager.getMessage("msg.no_permission")));
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
            //case "economy" -> economy(sender, args);
            case "sqlstatus" -> {
                try {
                    var ds = plugin.getMariaDBManager().getDataSource();
                    if (ds == null) {
                        sender.sendMessage(MSG.color("&cDataSource is null."));
                        return true;
                    }

                    var pool = ds.getHikariPoolMXBean();
                    if (pool == null) {
                        sender.sendMessage(MSG.color("&cHikariPoolMXBean is null."));
                        return true;
                    }

                    int active = pool.getActiveConnections();
                    int idle = pool.getIdleConnections();
                    int total = pool.getTotalConnections();
                    int awaiting = pool.getThreadsAwaitingConnection();

                    sender.sendMessage(MSG.color("&7&m------&r &aSQL Pool Status &7&m------"));
                    sender.sendMessage(MSG.color("&eActive: &f" + active));
                    sender.sendMessage(MSG.color("&eIdle: &f" + idle));
                    sender.sendMessage(MSG.color("&eTotal: &f" + total));
                    sender.sendMessage(MSG.color("&eAwaiting: &f" + awaiting));
                    sender.sendMessage(MSG.color("&7&m----------------------------"));

                } catch (Exception e) {
                    sender.sendMessage(MSG.color("&cError getting SQL status."));
                    e.printStackTrace();
                }
            }
            case "lang" -> {
                if (!(sender instanceof Player p)) {
                    sender.sendMessage(MSG.color(langManager.getMessage("msg.only_in_game")));
                    return true;
                }

                if (!sender.hasPermission("satipoclans.admin")) {
                    sender.sendMessage(MSG.color(langManager.getMessage("msg.no_permission")));
                    return true;
                }

                if (args.length == 1) {
                    // Abrir menú interactivo
                    langCMD.showLanguageMenu(p);
                } else if (args.length == 3 && args[1].equalsIgnoreCase("select")) {
                    langCMD.setLanguageCommand(p, args[2].toLowerCase());
                } else {
                    sender.sendMessage(MSG.color(langManager.getMessage("msg.usage_lang")));
                }
            }
            case "forcejoin" -> {
                if (args.length < 3) {
                    sender.sendMessage(MSG.color(langManager.getMessage("msg.clan_forcejoin_usage")));
                    return true;
                }
                plugin.getMariaDBManager().forceJoin(args[1], args[2]);
                sender.sendMessage(MSG.color(langManager.getMessage("msg.clan_forced_joined")));
            }

            case "forceleave" -> {
                if (args.length < 2) {
                    sender.sendMessage(MSG.color(langManager.getMessage("msg.clan_forceleave_usage")));
                    return true;
                }
                plugin.getMariaDBManager().forceLeave(args[1]);
                sender.sendMessage(MSG.color(langManager.getMessage("msg.clan_forced_left")));
            }

            case "delete" -> {
                if (args.length < 2) {
                    sender.sendMessage(MSG.color(langManager.getMessage("msg.clan_force_delete")));
                    return true;
                }
                plugin.getMariaDBManager().deleteClan(args[1]);
                sender.sendMessage(MSG.color(langManager.getMessage("msg.clan_deleted")));
            }
            default -> help(sender);
        }

        return true;
    }


    private void sendSqlStatusToDiscord(int active, int idle, int total, int awaiting) {
        String webhookUrl = plugin.getConfig().getString("discord.sql_status_webhook");
        if (webhookUrl == null || webhookUrl.isEmpty()) return;

        String json = """
        {
        "embeds": [{
            "title": "SatipoClans - SQL Status",
            "color": 3066993,
            "fields": [
            { "name": "Active Connections", "value": "%d", "inline": true },
            { "name": "Idle Connections", "value": "%d", "inline": true },
            { "name": "Total Connections", "value": "%d", "inline": true },
            { "name": "Threads Awaiting Connection", "value": "%d", "inline": true }
            ]
        }]
        }
        """.formatted(active, idle, total, awaiting);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                if (lastMessageId != null) {
                    // Construir URL para borrar mensaje anterior
                    String deleteUrl = webhookUrl + "/messages/" + lastMessageId;
                    HttpURLConnection deleteConn = (HttpURLConnection) new URL(deleteUrl).openConnection();
                    deleteConn.setRequestMethod("DELETE");
                    int deleteResponseCode = deleteConn.getResponseCode();
                    if (deleteResponseCode != 204) {
                        plugin.getLogger().warning("No se pudo borrar el mensaje anterior, código: " + deleteResponseCode);
                    }
                }

                // Enviar mensaje nuevo
                HttpURLConnection connection = (HttpURLConnection) new URL(webhookUrl).openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json");

                try (OutputStream os = connection.getOutputStream()) {
                    os.write(json.getBytes(StandardCharsets.UTF_8));
                }

                // Leer respuesta JSON para obtener el ID del mensaje
                try (InputStream is = connection.getInputStream()) {
                    String response = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    // El JSON tiene un campo "id" con el ID del mensaje
                    // Usamos simple regex para extraerlo:
                    var matcher = Pattern.compile("\"id\":\"(\\d+)\"").matcher(response);
                    if (matcher.find()) {
                        lastMessageId = matcher.group(1);
                    }
                }

            } catch (Exception e) {
                plugin.getLogger().severe("No se pudo enviar estado SQL al webhook de Discord:");
                e.printStackTrace();
            }
        });
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }


    private boolean handleConsole(CommandSender sender, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            reload(sender);
        } else {
            sender.sendMessage(MSG.color(langManager.getMessage("msg.console_reload_only")));
        }
        return true;
    }

    private void help(CommandSender sender) {
        langManager.getMessageList("msg.help.commands").forEach(line -> sender.sendMessage(MSG.color(line)));
    }



    private void reload(CommandSender sender) {
        // Reproducir sonido al iniciar reload (solo si es Player)
        if (sender instanceof Player) {
            Player player = (Player) sender;
        }

        // Mensaje de recargando
        sender.sendMessage(MSG.color(langManager.getMessage("msg.reloading")));

        // Recargar archivos
        plugin.getFH().reloadConfig();
        plugin.getFH().reloadData();

        // Recargar economía
        // SatipoClan.getEcon().reload();

        // Recargar idioma
        plugin.getLangManager().reload();

        // Recargar comandos (si es necesario)
        plugin.getCommand("clan").setExecutor(new CCMD(plugin, plugin.getLangManager()));
        plugin.getCommand("clanadmin").setExecutor(new ACMD(plugin));

        // Obtener idioma actual y nombre legible
        String currentLang = plugin.getLangManager().getCurrentLang();
        String langDisplayName = plugin.getLangCMD().getLanguageDisplayName(currentLang);

        // Mensaje final
        sender.sendMessage(MSG.color("&7------------------------------------"));
        sender.sendMessage(MSG.color(langManager.getMessage("msg.plugin_reloaded")));
        sender.sendMessage(MSG.color(langManager.getMessage("lang.current_lang")
                .replace("{lang}", currentLang.toUpperCase())
                .replace("{lang_name}", langDisplayName)));
        sender.sendMessage(MSG.color("&7------------------------------------"));

        // Reproducir sonido al finalizar reload (solo si es Player)
        if (sender instanceof Player) {
            Player player = (Player) sender;
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        }
    }




    private final Set<CommandSender> confirmClear = new HashSet<>();

    private void clear(CommandSender sender) {
        if (!confirmClear.contains(sender)) {
            confirmClear.add(sender);
            sender.sendMessage(MSG.color(langManager.getMessage("msg.confirm_clear_1")));
            sender.sendMessage(MSG.color(langManager.getMessage("msg.confirm_clear_2")));
            return;
        }

        confirmClear.remove(sender);

        try (Connection con = plugin.getMariaDBManager().getConnection();
            Statement stmt = con.createStatement()) {

            stmt.executeUpdate("SET FOREIGN_KEY_CHECKS = 0");

            stmt.executeUpdate("TRUNCATE TABLE reports");
            stmt.executeUpdate("TRUNCATE TABLE banned_clans");
            stmt.executeUpdate("TRUNCATE TABLE clan_users");
            stmt.executeUpdate("TRUNCATE TABLE alliances");
            stmt.executeUpdate("TRUNCATE TABLE friendlyfire");
            stmt.executeUpdate("TRUNCATE TABLE friendlyfire_allies");
            stmt.executeUpdate("TRUNCATE TABLE clans");
            stmt.executeUpdate("TRUNCATE TABLE economy_players");
            stmt.executeUpdate("TRUNCATE TABLE player_clan_history");
            stmt.executeUpdate("TRUNCATE TABLE clan_invites");
            stmt.executeUpdate("TRUNCATE TABLE pending_alliances");

            stmt.executeUpdate("SET FOREIGN_KEY_CHECKS = 1");

            sender.sendMessage(MSG.color(langManager.getMessage("msg.data_cleared")));

        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage(MSG.color(langManager.getMessage("msg.error_clearing_data")));
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
                sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("msg.no_reports")));
                return;
            }

            sender.sendMessage(MSG.color(langManager.getMessage("msg.reports_title")));
            reported.forEach((clan, reasons) -> {
                // Reemplazo de placeholder {clan}
                sender.sendMessage(MSG.color(langManager.getMessage("msg.clan_colon").replace("{clan}", clan)));
                reasons.forEach(reason -> sender.sendMessage(MSG.color(langManager.getMessage("msg.report_reason").replace("{reason}", reason))));
            });

        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("msg.error_loading_reports")));
        }
    }


    private void ban(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("msg.usage_ban")));
            return;
        }

        String clan = args[1];
        String reason = args.length >= 3 ? args[2] : "Baneado por un administrador";

        try (Connection con = plugin.getMariaDBManager().getConnection();
            PreparedStatement check = con.prepareStatement("SELECT name FROM clans WHERE name = ?");
            PreparedStatement ban = con.prepareStatement("REPLACE INTO banned_clans (name, reason) VALUES (?, ?)");
            PreparedStatement members = con.prepareStatement("SELECT username FROM clan_users WHERE clan = ?");
            PreparedStatement deleteClan = con.prepareStatement("DELETE FROM clans WHERE name = ?");
            PreparedStatement deleteUsers = con.prepareStatement("DELETE FROM clan_users WHERE clan = ?");
            PreparedStatement deleteInvites = con.prepareStatement("DELETE FROM clan_invites WHERE clan = ?");
            PreparedStatement deleteAlliances = con.prepareStatement("DELETE FROM alliances WHERE clan1 = ? OR clan2 = ?");
            PreparedStatement deleteFF = con.prepareStatement("DELETE FROM friendlyfire WHERE clan = ?");
            PreparedStatement deletePendingAllies = con.prepareStatement("DELETE FROM pending_alliances WHERE requester = ? OR target = ?");
            PreparedStatement deleteReports = con.prepareStatement("DELETE FROM reports WHERE clan = ?")) {

            check.setString(1, clan);
            ResultSet rs = check.executeQuery();

            if (!rs.next()) {
                sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("msg.clan_not_exist").replace("{clan}", clan)));
                return;
            }

            // Agrega a la blacklist de nombres
            ban.setString(1, clan);
            ban.setString(2, reason);
            ban.executeUpdate();

            // Expulsa y limpia tags a los miembros conectados
            members.setString(1, clan);
            ResultSet mrs = members.executeQuery();
            while (mrs.next()) {
                String user = mrs.getString("username");
                Player player = Bukkit.getPlayer(user);
                if (player != null) {
                    // Limpiar scoreboard/tag si usás uno
                    player.kickPlayer(MSG.color(langManager.getMessage("msg.kicked_ban_message")
                        .replace("{clan}", clan)
                        .replace("{reason}", reason)));
                }
            }

            // Elimina todos los datos relacionados al clan
            deleteClan.setString(1, clan);
            deleteClan.executeUpdate();

            deleteUsers.setString(1, clan);
            deleteUsers.executeUpdate();

            deleteInvites.setString(1, clan);
            deleteInvites.executeUpdate();

            deleteAlliances.setString(1, clan);
            deleteAlliances.setString(2, clan);
            deleteAlliances.executeUpdate();

            deleteFF.setString(1, clan);
            deleteFF.executeUpdate();

            deletePendingAllies.setString(1, clan);
            deletePendingAllies.setString(2, clan);
            deletePendingAllies.executeUpdate();

            deleteReports.setString(1, clan);
            deleteReports.executeUpdate();

            sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("msg.clan_banned").replace("{clan}", clan)));

        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("msg.error_banning_clan")));
        }
    }




    /*
    private void economy(CommandSender sender, String[] args) {
        if (args.length < 4 || (args.length < 5 && !args[3].equalsIgnoreCase("reset"))) {
            sender.sendMessage(MSG.color(prefix + "&c Uso: /cla economy <player|clan> <nombre> <set|add|reset> <cantidad>"));
            return;
        }

        String type = args[1].toLowerCase(Locale.ROOT);
        String name = args[2];
        String action = args[3].toLowerCase(Locale.ROOT);
        String amountStr = args.length >= 5 ? args[4] : "0";

        if (!action.equals("set") && !action.equals("add") && !action.equals("reset")) {
            sender.sendMessage(MSG.color(prefix + "&c Acción inválida. Usa set, add o reset."));
            return;
        }

        double amount = 0;
        if (!action.equals("reset")) {
            try {
                amount = Double.parseDouble(amountStr);
                if (amount < 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                sender.sendMessage(MSG.color(prefix + "&c Cantidad inválida."));
                return;
            }
        }

        if (type.equals("player")) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(name);
            if (!(player.hasPlayedBefore() || player.isOnline())) {
                sender.sendMessage(MSG.color(prefix + "&c Jugador '" + name + "' no encontrado."));
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
                    sender.sendMessage(MSG.color(prefix + "&c El clan '" + name + "' no existe."));
                    return;
                }

                String message = prefix + "&a Economía del clan actualizada: &f" + name + " &7-> &f" + action;
                if (!action.equals("reset")) {
                    message += " &7= &f" + amount;
                } else {
                    message += " &7= &f0";
                }
                sender.sendMessage(MSG.color(message));
            } catch (SQLException e) {
                e.printStackTrace();
                sender.sendMessage(MSG.color(prefix + "&c Error al actualizar la economía del clan."));
            }
            return;
        }

        sender.sendMessage(MSG.color(prefix + "&c El primer argumento debe ser 'player' o 'clan'."));
    }


    private void modifyPlayerEcon(CommandSender sender, OfflinePlayer p, String action, double amount) {
        Econo econ = SatipoClan.getEcon();
        double current = econ.getBalance(p);

        switch (action) {
            case "set" -> {
                if (amount > current) econ.deposit(p, amount - current);
                else econ.withdraw(p, current - amount);
                sender.sendMessage(MSG.color("&a Saldo de &f" + p.getName() + "&a establecido en &f" + amount));
            }
            case "add" -> {
                econ.deposit(p, amount);
                sender.sendMessage(MSG.color("&a Añadido &f" + amount + "&a a &f" + p.getName()));
            }
            case "reset" -> {
                econ.withdraw(p, current);
                sender.sendMessage(MSG.color("&a Saldo de &f" + p.getName() + "&a reiniciado."));
            }
            default -> sender.sendMessage(MSG.color(prefix + "&c Acción inválida."));
        }
    } */


    private void unban(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("msg.usage_unban")));
            return;
        }

        String clan = args[1];

        try (Connection con = plugin.getMariaDBManager().getConnection();
            PreparedStatement check = con.prepareStatement("SELECT name FROM banned_clans WHERE name = ?");
            PreparedStatement unban = con.prepareStatement("DELETE FROM banned_clans WHERE name = ?")) {

            check.setString(1, clan);
            try (ResultSet rs = check.executeQuery()) {
                if (!rs.next()) {
                    sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("msg.clan_not_banned").replace("{clan}", clan)));
                    return;
                }
            }

            unban.setString(1, clan);
            unban.executeUpdate();

            sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("msg.clan_unbanned").replace("{clan}", clan)));

        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("msg.error_unbanning_clan")));
        }
    }


    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String s, String[] args) {
        if (!(sender instanceof Player p) || !p.hasPermission("satipoclans.admin")) {
            return args.length == 1 ? List.of("reload") : Collections.emptyList();
        }

        if (args.length == 1) {
            return Stream.of("reload", "ban", "unban", "clear", "reports", "lang", "forcejoin", "forceleave", "delete", "sqlstatus")
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
            return Stream.of("hacks", "toxicidad", "abusos", "spam")
                    .filter(reason -> reason.startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (arg0.equals("lang")) {
            if (args.length == 2) {
                return Stream.of("select")
                        .filter(opt -> opt.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }

            if (args.length == 3 && args[1].equalsIgnoreCase("select")) {
                return plugin.getLangManager().getAvailableLangs().stream()
                        .filter(lang -> lang.toLowerCase().startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            }

            return Collections.emptyList();
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