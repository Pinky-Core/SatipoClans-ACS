package me.lewisainsworth.satipoclans.CMDs;

import me.lewisainsworth.satipoclans.SatipoClan;
import me.lewisainsworth.satipoclans.Utils.Econo;
import me.lewisainsworth.satipoclans.Utils.FileHandler;
import me.lewisainsworth.satipoclans.Utils.MSG;
import static me.lewisainsworth.satipoclans.SatipoClan.prefix;
import me.lewisainsworth.satipoclans.Utils.LangManager;
import me.lewisainsworth.satipoclans.Utils.ClanNameHandler;


import java.util.*;
import java.util.stream.Collectors;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.DriverManager;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;


import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;

public class CCMD implements CommandExecutor, TabCompleter, Listener {
    private final SatipoClan plugin;
    private final LangManager langManager;
    private final List<String> helpLines;
    public Set<UUID> teleportingPlayers = new HashSet<>();
    private final Map<UUID, Long> homeCooldowns = new HashMap<>();
    
    

    public CCMD(SatipoClan plugin, LangManager langManager) {
        this.plugin = plugin;
        this.langManager = langManager;
        this.helpLines = langManager.getMessageList("user.help_lines");
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String s, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MSG.color(langManager.getMessage("user.console_command_only")));
            return true;
        }

        if (plugin.isWorldBlocked(player.getWorld())) {
            player.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.command_blocked_world")));
            return true;
        }

        String playerName = player.getName();
        String playerClan = this.getPlayerClan(playerName);

        // Comando de ayuda paginada
        if (args.length < 1 || args[0].equalsIgnoreCase("help")) {
            int page = 1;
            if (args.length > 1) {
                try {
                    page = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.invalid_page_number")));
                    return true;
                }
            }
            
            // Solo llamamos a tu método help que ya tiene la paginación y botones
            help(player, page);
            return true;
        }

        // Resto de comandos con permisos individuales
        switch (args[0].toLowerCase()) {
            case "create":
                if (!player.hasPermission("satipoclans.user.create")) {
                    sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.no_permission")));
                    return true;
                }
                if (playerClan != null && !playerClan.isEmpty()) {
                    sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.already_in_clan")));
                    return true;
                }
                this.create(sender, args);
                break;

            case "disband":
                if (!player.hasPermission("satipoclans.user.disband")) {
                    sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.no_permission")));
                    return true;
                }
                this.disband(sender, playerClan);
                break;

            case "sethome":
                if (!player.hasPermission("satipoclans.user.sethome")) {
                    player.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.no_permission")));
                    return true;
                }
                if (playerClan == null || playerClan.isEmpty()) {
                    player.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.no_clan")));
                    return true;
                }
                plugin.getMariaDBManager().setClanHome(playerClan, player.getLocation());
                player.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.home_set")));
                break;

            case "delhome":
                if (!player.hasPermission("satipoclans.user.delhome")) {
                    player.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.no_permission")));
                    return true;
                }
                if (playerClan == null || playerClan.isEmpty()) {
                    player.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.no_clan")));
                    return true;
                }
                plugin.getMariaDBManager().deleteClanHome(playerClan);
                player.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.home_deleted")));
                break;

            case "home":
                if (!player.hasPermission("satipoclans.user.home")) {
                    player.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.no_permission")));
                    return true;
                }
                if (playerClan == null || playerClan.isEmpty()) {
                    player.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.no_clan")));
                    return true;
                }
                teleportToClanHome(player, playerClan);
                break;

            case "report":
                if (!player.hasPermission("satipoclans.user.report")) {
                    sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.no_permission")));
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.usage_report")));
                    return true;
                }
                this.report(sender, args[1], String.join(" ", Arrays.copyOfRange(args, 2, args.length)));
                break;

            case "list":
                if (!player.hasPermission("satipoclans.user.list")) {
                    sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.no_permission")));
                    return true;
                }
                this.list(sender);
                break;

            case "join":
                if (!player.hasPermission("satipoclans.user.join")) {
                    sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.no_permission")));
                    return true;
                }
                if (args.length != 2) {
                    sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.usage_join")));
                    return true;
                }
                this.joinClan(sender, playerName, args[1]);
                break;

            case "leave":
                if (!player.hasPermission("satipoclans.user.leave")) {
                    sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.no_permission")));
                    return true;
                }
                this.leave(sender, playerClan);
                break;

            case "kick":
                if (!player.hasPermission("satipoclans.user.kick")) {
                    sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.no_permission")));
                    return true;
                }
                this.kick(sender, args);
                break;

            case "invite":
                if (!player.hasPermission("satipoclans.user.invite")) {
                    sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.no_permission")));
                    return true;
                }
                if (args.length != 2) {
                    sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.usage_invite")));
                    return true;
                }
                this.inviteToClan(sender, args[1]);
                break;

            case "chat":
                if (!player.hasPermission("satipoclans.user.chat")) {
                    sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.no_permission")));
                    return true;
                }

                if (playerClan == null || playerClan.isEmpty()) {
                    sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.no_clan")));
                    return true;
                }

                if (args.length >= 2) {
                    // Modo clásico: mensaje directo al clan
                    this.chat(playerClan, player, Arrays.copyOfRange(args, 1, args.length));
                } else {
                    // Modo toggle: activás o desactivás el modo chat clan
                    plugin.toggleClanChat(player);
                    boolean toggled = plugin.isClanChatToggled(player);
                    sender.sendMessage(MSG.color(langManager.getMessageWithPrefix(
                        toggled ? "user.chat_enabled" : "user.chat_disabled")));
                }
                break;

            case "stats":
                if (!player.hasPermission("satipoclans.user.stats")) {
                    sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.no_permission")));
                    return true;
                }

                if (args.length >= 2) {
                    String targetClan = args[1];
                    this.stats(sender, targetClan);
                } else {
                    if (playerClan == null || playerClan.isEmpty()) {
                        sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.no_clan")));
                        return true;
                    }
                    this.stats(sender, playerClan);
                }
                break;

            case "resign":
                if (!player.hasPermission("satipoclans.user.resign")) {
                    sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.no_permission")));
                    return true;
                }
                this.resign(sender, playerClan);
                break;

            case "ff":
                if (!player.hasPermission("satipoclans.user.ff")) {
                    sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.no_permission")));
                    return true;
                }
                if (playerClan == null || playerClan.isEmpty()) {
                    sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.no_clan")));
                    return true;
                }
                if (args.length != 2 || (!args[1].equalsIgnoreCase("on") && !args[1].equalsIgnoreCase("off"))) {
                    sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.usage_ff")));
                    return true;
                }
                handleFriendlyFireCommand(sender, playerClan, args);
                break;

            case "ally":
                if (!player.hasPermission("satipoclans.user.ally")) {
                    sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.no_permission")));
                    return true;
                }
                if (playerClan == null || playerClan.isEmpty()) {
                    sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.no_clan")));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.usage_ally")));
                    return true;
                }
                handleAllyCommand(sender, playerName, playerClan, Arrays.copyOfRange(args, 1, args.length));
                break;

            case "edit":
                if (!player.hasPermission("satipoclans.user.edit")) {
                    sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.no_permission")));
                    return true;
                }
                this.edit(player, playerClan, args);
                break;

            default:
                this.help(player, 1);
                break;
        }

        return true;
    }





    public void help(Player player, int page) {
        int linesPerPage = 5;
        int totalPages = (int) Math.ceil((double) helpLines.size() / linesPerPage);

        if (page < 1 || page > totalPages) {
            player.sendMessage(MSG.color(prefix + "&c Página inválida. Usa /clan help <1-" + totalPages + ">"));
            return;
        }

        player.sendMessage(MSG.color("&6&m====================================="));
        player.sendMessage(MSG.color("&6&l» &a&lꜱᴀᴛɪᴘᴏ&6&lᴄʟᴀɴꜱ &e&lᴄᴏᴍᴀɴᴅᴏꜱ &7(Página " + page + "/" + totalPages + ")"));
        player.sendMessage(MSG.color("&6&m====================================="));

        int start = (page - 1) * linesPerPage;
        int end = Math.min(start + linesPerPage, helpLines.size());

        for (int i = start; i < end; i++) {
            player.sendMessage(MSG.color(helpLines.get(i)));
        }

        // Flechas de navegación
        TextComponent nav = new TextComponent();

        if (page > 1) {
            TextComponent prev = new TextComponent(MSG.color("&e« Página anterior "));
            prev.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/clan help " + (page - 1)));
            prev.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Haz clic para ir a la página " + (page - 1))));
            nav.addExtra(prev);
        }

        if (page < totalPages) {
            TextComponent next = new TextComponent(MSG.color("&e Página siguiente »"));
            next.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/clan help " + (page + 1)));
            next.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Haz clic para ir a la página " + (page + 1))));
            nav.addExtra(next);
        }

        player.spigot().sendMessage(nav);
        player.sendMessage(MSG.color("&6&m====================================="));
    }





    public void kick(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.usage_kick")));
            return;
        }

        String target = args[1];
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.console_command_only")));
            return;
        }

        String clanName = getPlayerClan(player.getName());

        if (clanName == null) {
            sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.no_clan")));
            return;
        }

        try (Connection con = plugin.getMariaDBManager().getConnection();
            PreparedStatement checkLeader = con.prepareStatement("SELECT leader FROM clans WHERE name=?");
            PreparedStatement removeUser = con.prepareStatement("DELETE FROM clan_users WHERE username=? AND clan=?");
            PreparedStatement countUsers = con.prepareStatement("SELECT COUNT(*) as total FROM clan_users WHERE clan=?");
            PreparedStatement deleteClan = con.prepareStatement("DELETE FROM clans WHERE name=?")) {

            checkLeader.setString(1, clanName);
            ResultSet rs = checkLeader.executeQuery();
            if (rs.next() && !rs.getString("leader").equalsIgnoreCase(player.getName())) {
                sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.kick_only_leader")));
                return;
            }

            if (target.equalsIgnoreCase(player.getName())) {
                sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.kick_cant_kick_self")));
                return;
            }

            removeUser.setString(1, target);
            removeUser.setString(2, clanName);
            int removed = removeUser.executeUpdate();
            if (removed == 0) {
                sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.kick_player_not_member")));
                return;
            }

            sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.kick_success")
                .replace("{player}", target)
                .replace("{clan}", clanName)));

            countUsers.setString(1, clanName);
            ResultSet count = countUsers.executeQuery();
            if (count.next() && count.getInt("total") == 0) {
                deleteClan.setString(1, clanName);
                deleteClan.executeUpdate();
                sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.kick_clan_deleted")));
            }

        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.kick_error")));
        }
    }



    public void resign(CommandSender sender, String playerClan) {
        String playerName = sender.getName();

        if (playerClan == null) {
            sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.no_clan")));
            return;
        }

        try (Connection con = plugin.getMariaDBManager().getConnection();
            PreparedStatement checkLeader = con.prepareStatement("SELECT leader FROM clans WHERE name=?");
            PreparedStatement selectNext = con.prepareStatement("SELECT username FROM clan_users WHERE clan=? AND username<>? LIMIT 1");
            PreparedStatement updateLeader = con.prepareStatement("UPDATE clans SET leader=? WHERE name=?");
            PreparedStatement deleteClan = con.prepareStatement("DELETE FROM clans WHERE name=?");
            PreparedStatement deleteUsers = con.prepareStatement("DELETE FROM clan_users WHERE clan=?")) {

            checkLeader.setString(1, playerClan);
            ResultSet rs = checkLeader.executeQuery();
            if (!rs.next() || !rs.getString("leader").equalsIgnoreCase(playerName)) {
                sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.resign_not_leader")));
                return;
            }

            selectNext.setString(1, playerClan);
            selectNext.setString(2, playerName);
            ResultSet next = selectNext.executeQuery();

            if (next.next()) {
                String newLeader = next.getString("username");
                updateLeader.setString(1, newLeader);
                updateLeader.setString(2, playerClan);
                updateLeader.executeUpdate();
                sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.resign_success")
                    .replace("{newLeader}", newLeader)));
            } else {
                deleteUsers.setString(1, playerClan);
                deleteUsers.executeUpdate();
                deleteClan.setString(1, playerClan);
                deleteClan.executeUpdate();
                sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.resign_clan_deleted")));
            }

        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.resign_error")));
        }
    }



//    public void wars(CommandSender sender, String[] args, String playerClan) {
//        if (args.length < 2) {
//            sender.sendMessage(MSG.color(prefix + "&c &lUSE:&f /clans wars <declare/peace/alliance/accept/deny> <clan>"));
//            return;
//        }
//
//        if (playerClan == null || playerClan.isEmpty()) {
//            sender.sendMessage(MSG.color(prefix + "&c You are not in a clan."));
//            return;
//        }
//
//        Player player = (Player) sender;
//        String playerName = player.getName();
//        FileHandler fh = plugin.getFH();
//        FileConfiguration data = fh.getData();
//        String leader = data.getString("Clans." + playerClan + ".Leader");
//
//        if (!playerName.equalsIgnoreCase(leader)) {
//            sender.sendMessage(MSG.color(prefix + "&c You are not the leader of this clan."));
//            return;
//        }
//
//        String action = args[1].toLowerCase();
//        String otherClan;
//
//        switch (action) {
//            case "peace":
//                if (args.length < 3) {
//                    sender.sendMessage(MSG.color(prefix + "&c You need to specify a clan name to offer peace."));
//                    return;
//                }
//                otherClan = args[2];
//                handlePeaceOffer(sender, playerClan, otherClan, data, fh);
//                break;
//
//            case "declare":
//                if (args.length < 3) {
//                    sender.sendMessage(MSG.color(prefix + "&c You need to specify a clan name to declare war."));
//                    return;
//                }
//                otherClan = args[2];
//                handleWarDeclaration(sender, playerClan, otherClan, data, fh);
//                break;
//
//            case "alliance":
//                if (args.length < 3) {
//                    sender.sendMessage(MSG.color(prefix + "&c You need to specify a clan name to declare an alliance."));
//                    return;
//                }
//                otherClan = args[2];
//                handleAllianceRequest(sender, playerClan, otherClan, data, fh);
//                break;
//
//            case "accept":
//                if (args.length < 3) {
//                    sender.sendMessage(MSG.color(prefix + "&c You need to specify a clan name to accept the alliance."));
//                    return;
//                }
//                otherClan = args[2];
//                handleAllianceAcceptance(sender, playerClan, otherClan, data, fh);
//                break;
//
//            case "deny":
//                if (args.length < 3) {
//                    sender.sendMessage(MSG.color(prefix + "&c You need to specify a clan name to deny the alliance."));
//                    return;
//                }
//                otherClan = args[2];
//                handleAllianceDenial(sender, playerClan, otherClan, data, fh);
//                break;
//
//            default:
//                sender.sendMessage(MSG.color(prefix + "&c Invalid action. Use: <declare/peace/alliance/accept/deny>"));
//                break;
//        }
//    }
//
//    private void handlePeaceOffer(CommandSender sender, String playerClan, String otherClan, FileConfiguration data, FileHandler fh) {
//        if (!data.contains("Clans." + otherClan)) {
//            sender.sendMessage(MSG.color(prefix + "&c The specified clan does not exist."));
//            return;
//        }
//
//        List<String> pending = data.getStringList("Wars." + otherClan + ".Enemy");
//        pending.remove(playerClan);
//        data.set("Wars." + otherClan + ".Enemy", pending);
//        fh.saveData();
//        sender.sendMessage(MSG.color(prefix + "&2You have offered peace to: &e" + otherClan));
//    }
//
//    private void handleWarDeclaration(CommandSender sender, String playerClan, String otherClan, FileConfiguration data, FileHandler fh) {
//        if (!data.contains("Clans." + otherClan)) {
//            sender.sendMessage(MSG.color(prefix + "&c The specified clan does not exist."));
//            return;
//        }
//
//        List<String> pending = data.getStringList("Wars." + otherClan + ".Enemy");
//        pending.add(playerClan);
//        data.set("Wars." + otherClan + ".Enemy", pending);
//
//        removePendingAlliance(playerClan, otherClan, data);
//
//        List<String> playerEnemy = data.getStringList("Wars." + playerClan + ".Enemy");
//        playerEnemy.add(otherClan);
//        data.set("Wars." + playerClan + ".Enemy", playerEnemy);
//
//        fh.saveData();
//        sender.sendMessage(MSG.color(prefix + "&2Your clan has started a war with: &e" + otherClan));
//    }
//
//    private void handleAllianceRequest(CommandSender sender, String playerClan, String otherClan, FileConfiguration data, FileHandler fh) {
//        if (!data.contains("Clans." + otherClan)) {
//            sender.sendMessage(MSG.color(prefix + "&c No clan found with that name."));
//            return;
//        }
//
//        List<String> pending = data.getStringList("Wars." + otherClan + ".Ally.Pending");
//        pending.add(playerClan);
//        data.set("Wars." + otherClan + ".Ally.Pending", pending);
//
//        fh.saveData();
//        sender.sendMessage(MSG.color(prefix + "&2Alliance request sent to: &e" + otherClan));
//    }
//
//    private void handleAllianceAcceptance(CommandSender sender, String playerClan, String otherClan, FileConfiguration data, FileHandler fh) {
//        if (!data.contains("Clans." + otherClan)) {
//            sender.sendMessage(MSG.color(prefix + "&c The specified clan does not exist."));
//            return;
//        }
//
//        List<String> pending = data.getStringList("Wars." + otherClan + ".Ally.Alliance");
//        pending.add(playerClan);
//        data.set("Wars." + otherClan + ".Ally.Alliance", pending);
//
//        pending = data.getStringList("Wars." + otherClan + ".Ally.Pending");
//        pending.remove(playerClan);
//        data.set("Wars." + otherClan + ".Ally.Pending", pending);
//
//        fh.saveData();
//        sender.sendMessage(MSG.color(prefix + "&2Now you are allied with: &e" + otherClan));
//    }
//
//    private void handleAllianceDenial(CommandSender sender, String playerClan, String otherClan, FileConfiguration data, FileHandler fh) {
//        if (!data.contains("Clans." + otherClan)) {
//            sender.sendMessage(MSG.color(prefix + "&c No clan found with that name."));
//            return;
//        }
//
//        List<String> pending = data.getStringList("Wars." + otherClan + ".Ally.Pending");
//        pending.remove(playerClan);
//        data.set("Wars." + otherClan + ".Ally.Pending", pending);
//
//        fh.saveData();
//        sender.sendMessage(MSG.color(prefix + "&2You rejected the alliance request of: &e" + otherClan));
//    }
//
//    private void removePendingAlliance(String playerClan, String otherClan, FileConfiguration data) {
//        List<String> pending = data.getStringList("Wars." + otherClan + ".Ally.Pending");
//        pending.remove(playerClan);
//        data.set("Wars." + otherClan + ".Ally.Pending", pending);
//
//        List<String> allianceOtherClan = data.getStringList("Wars." + otherClan + ".Ally.Alliance");
//        allianceOtherClan.remove(playerClan);
//        data.set("Wars." + otherClan + ".Ally.Alliance", allianceOtherClan);
//    }

    public void stats(CommandSender sender, String clanName) {
        try (Connection con = plugin.getMariaDBManager().getConnection();
            PreparedStatement clanStmt = con.prepareStatement(
                    "SELECT name_colored, founder, leader, privacy FROM clans WHERE name = ?"
            );
            PreparedStatement membersStmt = con.prepareStatement(
                    "SELECT username FROM clan_users WHERE clan = ?"
            )) {

            clanStmt.setString(1, clanName);
            ResultSet clanRs = clanStmt.executeQuery();

            if (!clanRs.next()) {
                sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.stats_not_found")));
                return;
            }

            String coloredName = clanRs.getString("name_colored");
            String founder = clanRs.getString("founder");
            String leader = clanRs.getString("leader");
            String privacy = clanRs.getString("privacy");

            sender.sendMessage(MSG.color(""));
            sender.sendMessage(MSG.color(langManager.getMessage("user.stats_border")));
            sender.sendMessage(MSG.color(langManager.getMessage("user.stats_title").replace("{clan}", coloredName)));
            sender.sendMessage(MSG.color(langManager.getMessage("user.stats_border")));
            sender.sendMessage(MSG.color(langManager.getMessage("user.stats_founder").replace("{founder}", founder)));
            sender.sendMessage(MSG.color(langManager.getMessage("user.stats_leader").replace("{leader}", leader)));
            sender.sendMessage(MSG.color(langManager.getMessage("user.stats_privacy").replace("{privacy}", privacy)));

            membersStmt.setString(1, clanName);
            ResultSet members = membersStmt.executeQuery();

            sender.sendMessage(MSG.color(langManager.getMessage("user.stats_members_title")));

            double totalKD = 0.0;
            int count = 0;

            while (members.next()) {
                String username = members.getString("username");
                double kd = plugin.getMariaDBManager().getKillDeathRatio(username);
                totalKD += kd;
                count++;

                sender.sendMessage(MSG.color(langManager.getMessage("user.stats_member_line").replace("{member}", username)
                        .replace("{kd}", String.format("%.2f", kd))));
            }

            double avgKD = count > 0 ? totalKD / count : 0.0;

            sender.sendMessage(MSG.color(langManager.getMessage("user.stats_avg_kd").replace("{avgKD}", String.format("%.2f", avgKD))));

            sender.sendMessage(MSG.color(langManager.getMessage("user.stats_footer")));

        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.stats_error")));
        }
    }





    /* private void Economy(Player player, String clan, String[] args) {
        if (args.length != 3) {
            player.sendMessage(MSG.color(prefix + "&c USO: /clan economy <depositar|retirar> <cantidad>"));
            return;
        }

        String playerClan = getPlayerClan(player.getName());
        if (playerClan == null) {
            player.sendMessage(MSG.color(prefix + "&c No perteneces a ningún clan."));
            return;
        }

        String type = args[1].toLowerCase(Locale.ROOT);
        int amount;

        try {
            amount = Integer.parseInt(args[2]);
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            player.sendMessage(MSG.color(prefix + "&c La cantidad debe ser un número positivo."));
            return;
        }

        if (!type.equals("deposit") && !type.equals("withdraw")) {
            player.sendMessage(MSG.color(prefix + "&c Operación inválida. Usa deposit o withdraw."));
            return;
        }

        Econo econ = SatipoClan.getEcon();
        double playerBalance = econ.getBalance(player);

        if (type.equals("deposit") && playerBalance < amount) {
            player.sendMessage(MSG.color(prefix + "&c No tienes suficiente dinero para depositar."));
            return;
        }

        try (Connection con = plugin.getMariaDBManager().getConnection()) {
            con.setAutoCommit(false);

            try (PreparedStatement stmt = con.prepareStatement("SELECT money FROM clans WHERE name = ? FOR UPDATE")) {
                stmt.setString(1, playerClan);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        player.sendMessage(MSG.color(prefix + "&c No se encontró el clan."));
                        con.rollback();
                        return;
                    }

                    double clanMoney = rs.getDouble("money");

                    if (type.equals("withdraw")) {
                        if (clanMoney < amount) {
                            player.sendMessage(MSG.color(prefix + "&c El clan no tiene suficiente dinero."));
                            con.rollback();
                            return;
                        }
                    }

                    String sqlUpdate = type.equals("deposit")
                        ? "UPDATE clans SET money = money + ? WHERE name = ?"
                        : "UPDATE clans SET money = money - ? WHERE name = ?";

                    try (PreparedStatement updateStmt = con.prepareStatement(sqlUpdate)) {
                        updateStmt.setInt(1, amount);
                        updateStmt.setString(2, playerClan);
                        int rows = updateStmt.executeUpdate();
                        if (rows == 0) {
                            player.sendMessage(MSG.color(prefix + "&c No se encontró el clan."));
                            con.rollback();
                            return;
                        }
                    }

                    if (type.equals("deposit")) {
                        econ.withdraw(player, amount);
                        player.sendMessage(MSG.color(prefix + "&2 Depositaste &a$" + amount + " &2al clan."));
                    } else {
                        econ.deposit(player, amount);
                        player.sendMessage(MSG.color(prefix + "&2 Retiraste &a$" + amount + " &2del clan."));
                    }

                    con.commit();
                    plugin.getMariaDBManager().reloadCache();
                }
            } catch (SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            player.sendMessage(MSG.color(prefix + "&c Ocurrió un error al procesar la acción de economía."));
        }
    } */

    private void inviteToClan(CommandSender sender, String playerToInvite) {
        String prefix = SatipoClan.prefix;

        if (!(sender instanceof Player inviter)) {
            sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.only_players_invite")));
            return;
        }

        String inviterName = inviter.getName();
        String inviterClan = plugin.getMariaDBManager().getCachedPlayerClan(inviterName);

        if (inviterClan == null) {
            sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.no_clan")));
            return;
        }

        if (playerToInvite.equalsIgnoreCase(inviterName)) {
            sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.cant_invite_self")));
            return;
        }

        Player invitedPlayer = Bukkit.getPlayerExact(playerToInvite);
        if (invitedPlayer == null || !invitedPlayer.isOnline()) {
            sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.player_not_online")));
            return;
        }

        String invitedPlayerClan = plugin.getMariaDBManager().getCachedPlayerClan(playerToInvite);
        if (invitedPlayerClan != null) {
            sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.player_in_other_clan")));
            return;
        }

        try (Connection con = plugin.getMariaDBManager().getConnection()) {
            String checkSql = "SELECT invite_time FROM clan_invites WHERE clan=? AND username=?";
            try (PreparedStatement checkStmt = con.prepareStatement(checkSql)) {
                checkStmt.setString(1, inviterClan);
                checkStmt.setString(2, playerToInvite);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        long inviteTime = rs.getLong("invite_time");
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - inviteTime < 5 * 60 * 1000) {
                            sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.invite_pending")));
                            return;
                        } else {
                            String deleteSql = "DELETE FROM clan_invites WHERE clan=? AND username=?";
                            try (PreparedStatement delStmt = con.prepareStatement(deleteSql)) {
                                delStmt.setString(1, inviterClan);
                                delStmt.setString(2, playerToInvite);
                                delStmt.executeUpdate();
                            }
                        }
                    }
                }
            }

            String insertSql = "INSERT INTO clan_invites (clan, username, invite_time) VALUES (?, ?, ?)";
            try (PreparedStatement insertStmt = con.prepareStatement(insertSql)) {
                insertStmt.setString(1, inviterClan);
                insertStmt.setString(2, playerToInvite);
                insertStmt.setLong(3, System.currentTimeMillis());
                insertStmt.executeUpdate();
            }

            sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.invite_sent").replace("{player}", playerToInvite)));
            invitedPlayer.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.invite_received")
                .replace("{clan}", inviterClan)));
            invitedPlayer.sendMessage(MSG.color(langManager.getMessage("user.invite_usage")));

        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.invite_error")));
        }
    }



    public void chat(String clanName, Player player, String[] message) {
        String playerClan = getPlayerClan(player.getName());
        if (playerClan == null || playerClan.isEmpty()) {
            player.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.no_clan")));
            return;
        }

        String formattedMessage = String.join(" ", message);

        try (Connection con = plugin.getMariaDBManager().getConnection();
            PreparedStatement stmt = con.prepareStatement("SELECT username FROM clan_users WHERE clan=?")) {
            stmt.setString(1, playerClan);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String userName = rs.getString("username");
                Player recipient = Bukkit.getPlayerExact(userName);
                if (recipient != null) {
                    recipient.sendMessage(MSG.color(langManager.getMessage("user.chat_format")
                        .replace("{clan}", plugin.getMariaDBManager().getColoredClanName(playerClan))
                        .replace("{player}", player.getName())
                        .replace("{message}", formattedMessage)));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            player.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.chat_error")));
        }
    }



    private void leave(CommandSender sender, String playerClan) {
        Player player = (Player) sender;
        String playerName = player.getName();

        if (playerClan == null) {
            sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.no_clan")));
            return;
        }

        try (Connection con = plugin.getMariaDBManager().getConnection();
            PreparedStatement checkLeader = con.prepareStatement("SELECT leader FROM clans WHERE name=?");
            PreparedStatement removeUser = con.prepareStatement("DELETE FROM clan_users WHERE username=? AND clan=?");
            PreparedStatement countUsers = con.prepareStatement("SELECT username FROM clan_users WHERE clan=?");
            PreparedStatement updateLeader = con.prepareStatement("UPDATE clans SET leader=? WHERE name=?")) {

            checkLeader.setString(1, playerClan);
            ResultSet leaderRs = checkLeader.executeQuery();
            boolean isLeader = false;
            if (leaderRs.next()) {
                isLeader = leaderRs.getString("leader").equalsIgnoreCase(playerName);
            }

            removeUser.setString(1, playerName);
            removeUser.setString(2, playerClan);
            removeUser.executeUpdate();

            countUsers.setString(1, playerClan);
            ResultSet countRs = countUsers.executeQuery();

            List<String> remaining = new ArrayList<>();
            while (countRs.next()) {
                remaining.add(countRs.getString("username"));
            }

            if (remaining.isEmpty()) {
                deleteEntireClanData(con, playerClan);
                plugin.getMariaDBManager().reloadCache();
                sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.clan_deleted_empty")));
                return;
            }

            if (isLeader) {
                String newLeader = remaining.get(new Random().nextInt(remaining.size()));
                updateLeader.setString(1, newLeader);
                updateLeader.setString(2, playerClan);
                updateLeader.executeUpdate();
                sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.leader_left").replace("{newLeader}", newLeader)));
            } else {
                sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.left_clan")));
            }

            plugin.getMariaDBManager().reloadCache();

        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.leave_error")));
        }
    }



    private static final long INVITE_EXPIRATION_MS = 5 * 60 * 1000; // 5 minutos en ms

    private void joinClan(CommandSender sender, String playerName, String clanToJoin) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.only_players_join")));
            return;
        }

        String currentClan = getPlayerClan(playerName);
        if (currentClan != null) {
            sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.already_in_clan")));
            return;
        }

        try (Connection con = plugin.getMariaDBManager().getConnection();
            PreparedStatement clanCheck = con.prepareStatement("SELECT privacy FROM clans WHERE name=?");
            PreparedStatement inviteCheck = con.prepareStatement("SELECT invite_time FROM clan_invites WHERE username=? AND clan=?");
            PreparedStatement addUser = con.prepareStatement("INSERT INTO clan_users (username, clan) VALUES (?, ?)");
            PreparedStatement deleteInvite = con.prepareStatement("DELETE FROM clan_invites WHERE username=? AND clan=?")) {

            clanCheck.setString(1, clanToJoin);
            ResultSet clanRs = clanCheck.executeQuery();

            if (!clanRs.next()) {
                sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.clan_not_exist")));
                return;
            }

            String privacy = clanRs.getString("privacy");
            boolean canJoin = "Public".equalsIgnoreCase(privacy);

            if (!canJoin) {
                inviteCheck.setString(1, playerName);
                inviteCheck.setString(2, clanToJoin);
                try (ResultSet inviteRs = inviteCheck.executeQuery()) {
                    if (inviteRs.next()) {
                        long inviteTime = inviteRs.getLong("invite_time");
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - inviteTime <= INVITE_EXPIRATION_MS) {
                            canJoin = true;
                        } else {
                            sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.invite_expired")));
                            try (PreparedStatement delExpired = con.prepareStatement("DELETE FROM clan_invites WHERE username=? AND clan=?")) {
                                delExpired.setString(1, playerName);
                                delExpired.setString(2, clanToJoin);
                                delExpired.executeUpdate();
                            }
                            return;
                        }
                    }
                }
            }

            if (!canJoin) {
                sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.clan_private")));
                return;
            }

            addUser.setString(1, playerName);
            addUser.setString(2, clanToJoin);
            addUser.executeUpdate();

            deleteInvite.setString(1, playerName);
            deleteInvite.setString(2, clanToJoin);
            deleteInvite.executeUpdate();

            PECMD.addClanToHistory(player, clanToJoin);

            sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.joined_clan").replace("{clan}", clanToJoin)));

        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.join_error")));
        }
    }



    private String getPlayerClan(String playerName) {
        try (Connection con = plugin.getMariaDBManager().getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT clan FROM clan_users WHERE username=?")) {
            ps.setString(1, playerName);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("clan");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }


    public void list(CommandSender sender) {
        try (Connection con = plugin.getMariaDBManager().getConnection();
            PreparedStatement ps = con.prepareStatement(
                "SELECT name_colored FROM clans WHERE name NOT IN (SELECT name FROM banned_clans)"
            );
            ResultSet rs = ps.executeQuery()) {

            if (!rs.isBeforeFirst()) {
                sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.no_clans")));
                return;
            }

            StringBuilder clansList = new StringBuilder();
            clansList.append(MSG.color(langManager.getMessageWithPrefix("user.clans_header") + "\n"));

            while (rs.next()) {
                String coloredName = rs.getString("name_colored");
                clansList.append(MSG.color("&7- " + coloredName)).append("\n");
            }

            clansList.append(MSG.color(langManager.getMessage("user.clans_footer")));
            sender.sendMessage(clansList.toString());

        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.clans_error")));
        }
    }





    private void report(CommandSender sender, String reportedClan, String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.report_no_reason")));
            return;
        }

        try (Connection con = plugin.getMariaDBManager().getConnection();
            PreparedStatement check = con.prepareStatement("SELECT name FROM clans WHERE name=?");
            PreparedStatement checkDup = con.prepareStatement("SELECT * FROM reports WHERE clan=? AND reason=?");
            PreparedStatement insert = con.prepareStatement("INSERT INTO reports (clan, reason) VALUES (?, ?)")) {

            check.setString(1, reportedClan);
            ResultSet clanRs = check.executeQuery();
            if (!clanRs.next()) {
                sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.report_clan_not_exist")));
                return;
            }

            checkDup.setString(1, reportedClan);
            checkDup.setString(2, reason);
            ResultSet dupRs = checkDup.executeQuery();
            if (dupRs.next()) {
                sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.report_already_sent")));
                return;
            }

            insert.setString(1, reportedClan);
            insert.setString(2, reason);
            insert.executeUpdate();

            sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.report_success")
                .replace("{clan}", reportedClan)
                .replace("{reason}", reason)));

        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.report_error")));
        }
    }



    private void edit(Player player, String clanName, String[] args) {
        if (!isLeader(player, clanName)) {
            player.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.edit_no_leader")));
            return;
        }

        if (args.length != 3) {
            player.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.edit_usage")));
            return;
        }

        String type = args[1];
        String value = args[2];

        try (Connection con = plugin.getMariaDBManager().getConnection()) {
           if (type.equalsIgnoreCase("name")) {
                if (plugin.isClanBanned(value)) {
                    player.sendMessage(MSG.color(langManager.getMessageWithPrefix("msg.clan_name_banned").replace("{clan}", value)));
                    return;
                }

                con.setAutoCommit(false);
                try {
                    try {
                        ClanNameHandler.updateClanName(plugin, clanName, value);
                    } catch (IllegalArgumentException e) {
                        player.sendMessage(e.getMessage());
                        return;
                    }

                    try (PreparedStatement ps = con.prepareStatement("UPDATE clan_users SET clan=? WHERE clan=?")) {
                        ps.setString(1, value);
                        ps.setString(2, clanName);
                        ps.executeUpdate();
                    }

                    try (PreparedStatement ps = con.prepareStatement("UPDATE friendlyfire SET clan=? WHERE clan=?")) {
                        ps.setString(1, value);
                        ps.setString(2, clanName);
                        ps.executeUpdate();
                    }

                    try (PreparedStatement ps = con.prepareStatement("UPDATE clan_invites SET clan=? WHERE clan=?")) {
                        ps.setString(1, value);
                        ps.setString(2, clanName);
                        ps.executeUpdate();
                    }

                    try (PreparedStatement ps = con.prepareStatement("UPDATE reports SET clan=? WHERE clan=?")) {
                        ps.setString(1, value);
                        ps.setString(2, clanName);
                        ps.executeUpdate();
                    }

                    try (PreparedStatement ps = con.prepareStatement("UPDATE alliances SET clan1=? WHERE clan1=?")) {
                        ps.setString(1, value);
                        ps.setString(2, clanName);
                        ps.executeUpdate();
                    }

                    try (PreparedStatement ps = con.prepareStatement("UPDATE alliances SET clan2=? WHERE clan2=?")) {
                        ps.setString(1, value);
                        ps.setString(2, clanName);
                        ps.executeUpdate();
                    }

                    try (PreparedStatement ps = con.prepareStatement("UPDATE pending_alliances SET requester=? WHERE requester=?")) {
                        ps.setString(1, value);
                        ps.setString(2, clanName);
                        ps.executeUpdate();
                    }

                    try (PreparedStatement ps = con.prepareStatement("UPDATE pending_alliances SET target=? WHERE target=?")) {
                        ps.setString(1, value);
                        ps.setString(2, clanName);
                        ps.executeUpdate();
                    }

                    try (PreparedStatement ps = con.prepareStatement("UPDATE player_clan_history SET current_clan=? WHERE current_clan=?")) {
                        ps.setString(1, value);
                        ps.setString(2, clanName);
                        ps.executeUpdate();
                    }

                    con.commit();
                    plugin.getMariaDBManager().reloadCache();
                    player.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.edit_name_success").replace("{name}", value)));
                } catch (SQLException e) {
                    con.rollback();
                    e.printStackTrace();
                    player.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.edit_name_error")));
                } finally {
                    con.setAutoCommit(true);
                }
            } else if (type.equalsIgnoreCase("privacy")) {
                try (PreparedStatement ps = con.prepareStatement("UPDATE clans SET privacy=? WHERE name=?")) {
                    ps.setString(1, value);
                    ps.setString(2, clanName);
                    ps.executeUpdate();
                    player.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.edit_privacy_success").replace("{privacy}", value)));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            player.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.edit_error")));
        }
    }




    public void disband(CommandSender sender, String playerClan) {
        if (playerClan == null || playerClan.isEmpty()) {
            sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.no_clan")));
            return;
        }

        Player player = (Player) sender;
        Econo econ = SatipoClan.getEcon();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection con = plugin.getMariaDBManager().getConnection();
                PreparedStatement checkLeader = con.prepareStatement("SELECT leader FROM clans WHERE name=?")) {

                checkLeader.setString(1, playerClan);
                ResultSet rs = checkLeader.executeQuery();

                if (!rs.next() || !rs.getString("leader").equalsIgnoreCase(player.getName())) {
                    Bukkit.getScheduler().runTask(plugin, () ->
                        sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.disband_not_leader")))
                    );
                    return;
                }

                // Eliminar datos
                deleteEntireClanData(con, playerClan);

                boolean econEnabled = plugin.getFH().getConfig().getBoolean("economy.enabled");
                int deleteGain = plugin.getFH().getConfig().getInt("economy.earn.delete-clan", 0);
                if (econEnabled) econ.deposit(player, deleteGain);

                plugin.getMariaDBManager().reloadCache();

                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (econEnabled) {
                        sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.disband_success_earn")
                            .replace("{money}", String.valueOf(deleteGain))));
                    } else {
                        sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.disband_success")));
                    }
                });

            } catch (SQLException e) {
                e.printStackTrace();
                Bukkit.getScheduler().runTask(plugin, () ->
                    sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.disband_error")))
                );
            }
        });
    }


    public void create(CommandSender sender, String[] args) {
        if (args.length < 2 || !(sender instanceof Player player)) {
            sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.create_usage")));
            return;
        }

        String rawClanName = args[1];
        String plainClanName = ClanNameHandler.getVisibleName(rawClanName);
        String playerName = player.getName();
        FileConfiguration config = plugin.getFH().getConfig();
        Econo econ = SatipoClan.getEcon();

        // Validar nombres bloqueados
        if (config.getStringList("names-blocked.blocked").stream().anyMatch(b -> b.equalsIgnoreCase(plainClanName))) {
            sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.create_name_blocked")));
            return;
        }

        // ❌ Verificar si está baneado
        if (plugin.isClanBanned(plainClanName)) {
            sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("msg.clan_name_banned").replace("{clan}", plainClanName)));
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection con = plugin.getMariaDBManager().getConnection();
                PreparedStatement check = con.prepareStatement("SELECT name FROM clans WHERE name=?")) {

                check.setString(1, plainClanName);
                ResultSet rs = check.executeQuery();
                if (rs.next()) {
                    Bukkit.getScheduler().runTask(plugin, () ->
                            sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.create_exists")))
                    );
                    return;
                }

                // Límite de clanes
                int maxClans = config.getInt("max-clans", 0);
                if (maxClans > 0) {
                    try (PreparedStatement countStmt = con.prepareStatement("SELECT COUNT(*) AS total FROM clans")) {
                        ResultSet countRs = countStmt.executeQuery();
                        if (countRs.next() && countRs.getInt("total") >= maxClans) {
                            Bukkit.getScheduler().runTask(plugin, () ->
                                    sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.create_limit")
                                            .replace("{max}", String.valueOf(maxClans))))
                            );
                            return;
                        }
                    }
                }

                // Economía
                if (config.getBoolean("economy.enabled")) {
                    int cost = config.getInt("economy.cost.create-clan");
                    if (econ.getBalance(player) < cost) {
                        Bukkit.getScheduler().runTask(plugin, () ->
                                sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.create_no_money")
                                        .replace("{cost}", String.valueOf(cost))))
                        );
                        return;
                    }
                    econ.withdraw(player, cost);
                }

                // Insertar clan
                try {
                    ClanNameHandler.insertClan(plugin, rawClanName, playerName, playerName);

                    try (PreparedStatement insertUser = con.prepareStatement("INSERT INTO clan_users (username, clan) VALUES (?, ?)")) {
                        insertUser.setString(1, playerName);
                        insertUser.setString(2, plainClanName);
                        insertUser.executeUpdate();
                    }

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        PECMD.addClanToHistory(player, plainClanName);
                        player.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.create_success")
                                .replace("{clan}", MSG.color(rawClanName))));
                    });

                } catch (IllegalArgumentException e) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        player.sendMessage(e.getMessage());
                    });
                }

            } catch (SQLException e) {
                e.printStackTrace();
                Bukkit.getScheduler().runTask(plugin, () ->
                        sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.create_error")))
                );
            }
        });
    }





    private void handleFriendlyFireCommand(CommandSender sender, String playerClan, String[] args) {
        Player player = (Player) sender;

        if (!isLeader(player, playerClan)) {
            player.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.not_leader_command")));
            return;
        }

        if (playerClan == null || playerClan.isEmpty()) {
            sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.no_clan")));
            return;
        }

        if (args.length != 2 || (!args[1].equalsIgnoreCase("on") && !args[1].equalsIgnoreCase("off"))) {
            sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("command.ff_usage")));
            return;
        }

        boolean enabled = args[1].equalsIgnoreCase("on");

        try (Connection con = plugin.getMariaDBManager().getConnection();
            PreparedStatement stmt = con.prepareStatement("REPLACE INTO friendlyfire (clan, enabled) VALUES (?, ?)")) {

            stmt.setString(1, playerClan);
            stmt.setBoolean(2, enabled);
            stmt.executeUpdate();

            sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("command.ff_status")
                .replace("{status}", enabled ? langManager.getMessage("status.enabled") : langManager.getMessage("status.disabled"))));

        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("command.ff_error")));
        }
    }

    private void handleAllyFriendlyFireCommand(CommandSender sender, String playerClan, String[] args) {
         Player player = (Player) sender;

        if (!isLeader(player, playerClan)) {
            player.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.not_leader_command")));
            return;
        }

        if (playerClan == null || playerClan.isEmpty()) {
            sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.no_clan")));
            return;
        }

        if (args.length != 2 || (!args[1].equalsIgnoreCase("on") && !args[1].equalsIgnoreCase("off"))) {
            sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("command.allyff_usage")));
            return;
        }

        boolean enabled = args[1].equalsIgnoreCase("on");

        try (Connection con = plugin.getMariaDBManager().getConnection();
            PreparedStatement stmt = con.prepareStatement("REPLACE INTO friendlyfire_allies (clan, enabled) VALUES (?, ?)")) {

            stmt.setString(1, playerClan);
            stmt.setBoolean(2, enabled);
            stmt.executeUpdate();

            sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("command.allyff_status")
                .replace("{status}", enabled ? langManager.getMessage("status.enabled") : langManager.getMessage("status.disabled"))));

        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("command.allyff_error")));
        }
    }


    private boolean areClansAllied(String clan1, String clan2) {
        if (clan1.equalsIgnoreCase(clan2)) return true;

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

    private void handleAllyCommand(CommandSender sender, String playerName, String playerClan, String[] args) {
         Player player = (Player) sender;

        if (!isLeader(player, playerClan)) {
            player.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.not_leader_command")));
            return;
        }
        
        if (playerClan == null || playerClan.isEmpty()) {
            sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.no_clan")));
            return;
        }

        if (args.length < 1) {
            sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("command.ally_usage")));
            return;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "request":
                if (args.length != 2) {
                    sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("command.ally_request_usage")));
                    return;
                }
                sendAllyRequest(sender, playerClan, args[1]);
                break;

            case "accept":
                if (args.length != 2) {
                    sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("command.ally_accept_usage")));
                    return;
                }
                acceptAlly(sender, playerClan, args[1]);
                break;

            case "decline":
                if (args.length != 2) {
                    sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("command.ally_decline_usage")));
                    return;
                }
                declineAlly(sender, playerClan, args[1]);
                break;

            case "remove":
                if (args.length != 2) {
                    sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("command.ally_remove_usage")));
                    return;
                }
                removeAlly(sender, playerClan, args[1]);
                break;

            case "ff":
                if (args.length != 2) {
                    sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("command.ally_ff_usage")));
                    return;
                }
                handleAllyFriendlyFireCommand(sender, playerClan, args);
                break;

            default:
                sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("command.ally_invalid_subcommand")));
        }
    }

    private void setHome(Player player, String clan) {
        if (!isLeader(player, clan)) {
            player.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.not_leader_home")));
            return;
        }

        plugin.getMariaDBManager().setClanHome(clan, player.getLocation());
        player.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.home_set")));
    }

    private void teleportToClanHome(Player player, String clan) {
        UUID uuid = player.getUniqueId();

        if (plugin.teleportingPlayers.contains(uuid)) {
            player.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.on_teport")));
            return;
        }

        boolean bypassCooldown = player.hasPermission("satipoclans.bypass.homecooldown");
        boolean bypassDelay = player.hasPermission("satipoclans.bypass.homedelay");

        if (!bypassCooldown) {
            long lastUsed = plugin.homeCooldowns.getOrDefault(uuid, 0L);
            long timeLeft = ((lastUsed + plugin.clanHomeCooldown * 1000L) - System.currentTimeMillis()) / 1000;

            if (timeLeft > 0) {
                player.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.home_cooldown")
                        .replace("{seconds}", String.valueOf(timeLeft))));
                return;
            }

            plugin.homeCooldowns.put(uuid, System.currentTimeMillis());
        }

        if (bypassDelay) {
            // Teletransporta inmediato sin delay
            Location home = plugin.getMariaDBManager().getClanHome(clan);
            if (home != null) {
                player.teleport(home);
                player.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.home_teleported")));
            } else {
                player.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.home_not_set")));
            }
        } else {
            // Teletransporta con delay y cancelación por movimiento
            plugin.teleportingPlayers.add(uuid);
            player.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.teleporting_home")
                    .replace("{seconds}", String.valueOf(plugin.clanHomeDelay))));

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!plugin.teleportingPlayers.contains(uuid)) {
                    // Cancelado por movimiento
                    return;
                }

                Location home = plugin.getMariaDBManager().getClanHome(clan);
                if (home != null) {
                    player.teleport(home);
                    player.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.home_teleported")));
                } else {
                    player.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.home_not_set")));
                }

                plugin.teleportingPlayers.remove(uuid);
            }, plugin.clanHomeDelay * 20L);
        }
    }





    // ------------------------------------
    // Métodos auxiliares:


    private void deleteEntireClanData(Connection con, String clan) throws SQLException {
        try (PreparedStatement ps1 = con.prepareStatement("DELETE FROM clan_users WHERE clan=?");
            PreparedStatement ps2 = con.prepareStatement("DELETE FROM clans WHERE name=?");
            PreparedStatement ps3 = con.prepareStatement("DELETE FROM reports WHERE clan=?");
            PreparedStatement ps4 = con.prepareStatement("DELETE FROM alliances WHERE clan1=? OR clan2=?");
            PreparedStatement ps5 = con.prepareStatement("DELETE FROM friendlyfire WHERE clan=?");
            PreparedStatement ps6 = con.prepareStatement("DELETE FROM clan_invites WHERE clan=?");
            PreparedStatement ps7 = con.prepareStatement("DELETE FROM pending_alliances WHERE requester=? OR target=?");
            PreparedStatement ps8 = con.prepareStatement("DELETE FROM friendlyfire_allies WHERE clan=?");
            PreparedStatement ps9 = con.prepareStatement("UPDATE player_clan_history SET current_clan = NULL WHERE current_clan = ?")) {

            ps1.setString(1, clan); ps1.executeUpdate();
            ps2.setString(1, clan); ps2.executeUpdate();
            ps3.setString(1, clan); ps3.executeUpdate();
            ps4.setString(1, clan); ps4.setString(2, clan); ps4.executeUpdate();
            ps5.setString(1, clan); ps5.executeUpdate();
            ps6.setString(1, clan); ps6.executeUpdate();
            ps7.setString(1, clan); ps7.setString(2, clan); ps7.executeUpdate();
            ps8.setString(1, clan); ps8.executeUpdate();
            ps9.setString(1, clan); ps9.executeUpdate();
        }
    }


    private void sendAllyRequest(CommandSender sender, String playerClan, String targetClan) {
        if (targetClan.equalsIgnoreCase(playerClan)) {
            sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("command.ally_same_clan")));
            return;
        }

        try (Connection con = plugin.getMariaDBManager().getConnection();
            PreparedStatement check = con.prepareStatement("SELECT name FROM clans WHERE name = ?");
            PreparedStatement checkPending = con.prepareStatement("SELECT 1 FROM pending_alliances WHERE requester = ? AND target = ?");
            PreparedStatement insert = con.prepareStatement("INSERT INTO pending_alliances (requester, target) VALUES (?, ?)")) {

            check.setString(1, targetClan);
            ResultSet rs = check.executeQuery();
            if (!rs.next()) {
                sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("command.ally_target_not_exist").replace("{target}", targetClan)));
                return;
            }

            checkPending.setString(1, playerClan);
            checkPending.setString(2, targetClan);
            ResultSet rsPending = checkPending.executeQuery();
            if (rsPending.next()) {
                sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("command.ally_already_requested")));
                return;
            }

            insert.setString(1, playerClan);
            insert.setString(2, targetClan);
            insert.executeUpdate();

            sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("command.ally_request_sent").replace("{target}", targetClan)));

            for (Player p : Bukkit.getOnlinePlayers()) {
                String pClan = this.getPlayerClan(p.getName());
                if (pClan != null && pClan.equalsIgnoreCase(targetClan)) {
                    p.sendMessage(MSG.color(langManager.getMessageWithPrefix("command.ally_request_received").replace("{clan}", playerClan)));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("command.ally_request_error")));
        }
    }


    private void acceptAlly(CommandSender sender, String playerClan, String requesterClan) {
        if (requesterClan.equalsIgnoreCase(playerClan)) {
            sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("command.ally_accept_same_clan")));
            return;
        }

        try (Connection con = plugin.getMariaDBManager().getConnection();
            PreparedStatement checkPending = con.prepareStatement("SELECT 1 FROM pending_alliances WHERE requester = ? AND target = ?");
            PreparedStatement deletePending = con.prepareStatement("DELETE FROM pending_alliances WHERE requester = ? AND target = ?");
            PreparedStatement insertAlliance = con.prepareStatement("INSERT IGNORE INTO alliances (clan1, clan2) VALUES (?, ?), (?, ?)")) {

            checkPending.setString(1, requesterClan);
            checkPending.setString(2, playerClan);
            ResultSet rs = checkPending.executeQuery();

            if (!rs.next()) {
                sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("command.ally_accept_no_pending")));
                return;
            }

            con.setAutoCommit(false);

            insertAlliance.setString(1, requesterClan);
            insertAlliance.setString(2, playerClan);
            insertAlliance.setString(3, playerClan);
            insertAlliance.setString(4, requesterClan);
            insertAlliance.executeUpdate();

            deletePending.setString(1, requesterClan);
            deletePending.setString(2, playerClan);
            deletePending.executeUpdate();

            con.commit();
            plugin.getMariaDBManager().reloadCache();
            con.setAutoCommit(true);

            sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("command.ally_accept_success").replace("{requester}", requesterClan)));

            for (Player p : Bukkit.getOnlinePlayers()) {
                String pClan = this.getPlayerClan(p.getName());
                if (pClan != null && pClan.equalsIgnoreCase(requesterClan)) {
                    p.sendMessage(MSG.color(langManager.getMessageWithPrefix("command.ally_accepted_notify").replace("{clan}", playerClan)));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("command.ally_accept_error")));
            try {
                plugin.getMariaDBManager().getConnection().setAutoCommit(true);
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }


    private void declineAlly(CommandSender sender, String playerClan, String requesterClan) {
        if (requesterClan.equalsIgnoreCase(playerClan)) {
            sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("command.ally_decline_same_clan")));
            return;
        }

        try (Connection con = plugin.getMariaDBManager().getConnection();
            PreparedStatement checkPending = con.prepareStatement("SELECT 1 FROM pending_alliances WHERE requester = ? AND target = ?");
            PreparedStatement deletePending = con.prepareStatement("DELETE FROM pending_alliances WHERE requester = ? AND target = ?")) {

            checkPending.setString(1, requesterClan);
            checkPending.setString(2, playerClan);
            ResultSet rs = checkPending.executeQuery();

            if (!rs.next()) {
                sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("command.ally_decline_no_pending")));
                return;
            }

            deletePending.setString(1, requesterClan);
            deletePending.setString(2, playerClan);
            deletePending.executeUpdate();

            sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("command.ally_decline_success").replace("{requester}", requesterClan)));

            for (Player p : Bukkit.getOnlinePlayers()) {
                String pClan = this.getPlayerClan(p.getName());
                if (pClan != null && pClan.equalsIgnoreCase(requesterClan)) {
                    p.sendMessage(MSG.color(langManager.getMessageWithPrefix("command.ally_declined_notify").replace("{clan}", playerClan)));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("command.ally_decline_error")));
        }
    }


    private void removeAlly(CommandSender sender, String playerClan, String targetClan) {
        if (targetClan.equalsIgnoreCase(playerClan)) {
            sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("command.ally_remove_same_clan")));
            return;
        }

        try (Connection con = plugin.getMariaDBManager().getConnection();
            PreparedStatement deleteAlliance = con.prepareStatement("DELETE FROM alliances WHERE (clan1 = ? AND clan2 = ?) OR (clan1 = ? AND clan2 = ?)")) {

            deleteAlliance.setString(1, playerClan);
            deleteAlliance.setString(2, targetClan);
            deleteAlliance.setString(3, targetClan);
            deleteAlliance.setString(4, playerClan);
            int affected = deleteAlliance.executeUpdate();

            if (affected > 0) {
                sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("command.ally_remove_success").replace("{target}", targetClan)));
            } else {
                sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("command.ally_remove_none")));
            }

        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("command.ally_remove_error")));
        }
    }


    private void handleAllyFriendlyFireCommand(CommandSender sender, String playerClan, String value) {
         Player player = (Player) sender;

        if (!isLeader(player, playerClan)) {
            player.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.not_leader_command")));
            return;
        }

        if (!value.equalsIgnoreCase("on") && !value.equalsIgnoreCase("off")) {
            sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("command.allyff_usage")));
            return;
        }

        boolean ffEnabled = value.equalsIgnoreCase("on");

        try (Connection con = plugin.getMariaDBManager().getConnection();
            PreparedStatement stmt = con.prepareStatement(
                "REPLACE INTO friendlyfire_allies (clan, enabled) VALUES (?, ?)"
            )) {

            stmt.setString(1, playerClan);
            stmt.setBoolean(2, ffEnabled);
            stmt.executeUpdate();

            String status = langManager.getMessageWithPrefix(ffEnabled ? "status.enabled" : "status.disabled");
            sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("command.allyff_status").replace("{status}", status)));

        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("command.allyff_error")));
        }
    }



    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            return args.length == 1 ? List.of("reload") : new ArrayList<>();
        }

        String playerClan = SatipoClan.getInstance().getMariaDBManager().getCachedPlayerClan(player.getName());
        List<String> completions = new ArrayList<>();

        switch (args.length) {
            case 1 -> completions.addAll(List.of(
                    "create", "disband", "report", "list", "join",
                    "kick", "invite", "chat", "leave", "stats", "resign", "edit",
                    "ff", "ally", "help", "home", "sethome", "delhome"
            ));

            case 2 -> {
                String arg0 = args[0].toLowerCase();
                switch (arg0) {
                    case "join" -> {
                        if (isNotInClan(playerClan)) completions.addAll(SatipoClan.getInstance().getMariaDBManager().getCachedClanNames());
                    }
                    case "invite", "kick" -> {
                        if (isInClan(playerClan) && isLeader(player, playerClan)) completions.addAll(getOnlinePlayerNames());
                    }
                    //case "economy" -> completions.addAll(List.of("deposit", "withdraw"));
                    case "report", "allyremove" -> completions.addAll(SatipoClan.getInstance().getMariaDBManager().getCachedClanNames());
                    case "edit" -> {
                        if (isInClan(playerClan) && isLeader(player, playerClan)) {
                            completions.addAll(List.of("name", "privacy"));
                        }
                    }
                    case "ff" -> {
                        completions.addAll(List.of("on", "off"));
                    }
                    case "ally" -> {
                        completions.addAll(List.of("request", "accept", "decline", "remove", "ff"));
                    }
                }
            }

            case 3 -> {
                String arg0 = args[0].toLowerCase();
                String arg1 = args[1].toLowerCase();

                if (arg0.equals("ally")) {
                    if (List.of("request", "accept", "decline", "remove").contains(arg1)) {
                        completions.addAll(SatipoClan.getInstance().getMariaDBManager().getCachedClanNames());
                    } else if (arg1.equals("ff")) {
                        completions.addAll(List.of("on", "off"));
                    }
                }
            }
        }

        return completions.stream()
                .filter(c -> c.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }

    private boolean isInClan(String clan) {
        return clan != null && !clan.isEmpty();
    }

    private boolean isNotInClan(String clan) {
        return !isInClan(clan);
    }

    private boolean isLeader(Player player, String clanName) {
        try (Connection con = plugin.getMariaDBManager().getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT leader FROM clans WHERE name=?")) {
            ps.setString(1, clanName);
            ResultSet rs = ps.executeQuery();
            return rs.next() && player.getName().equalsIgnoreCase(rs.getString("leader"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private List<String> getClanNames() {
        List<String> names = new ArrayList<>();
        try (Connection con = plugin.getMariaDBManager().getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT name FROM clans");
            ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                names.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return names;
    }

    private List<String> getOnlinePlayerNames() {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toList());
    }
}