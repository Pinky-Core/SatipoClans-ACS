package me.lewisainsworth.satipoclans.CMDs;

import me.lewisainsworth.satipoclans.SatipoClan;
import me.lewisainsworth.satipoclans.Utils.Econo;
import me.lewisainsworth.satipoclans.Utils.FileHandler;
import me.lewisainsworth.satipoclans.Utils.MSG;
import static me.lewisainsworth.satipoclans.SatipoClan.prefix;

import java.util.*;
import java.util.stream.Collectors;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

public class CCMD implements CommandExecutor, TabCompleter {
    private final SatipoClan plugin;

    public CCMD(SatipoClan plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String s, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MSG.color(prefix + "&cConsole Commands: &f/cls reload."));
            return true;
        } else {
            if (!sender.hasPermission("sc.user")) {
                sender.sendMessage(MSG.color(prefix + "&cYou don't have permissions to use this command"));
                return true;
            }

            String playerName = player.getName();
            String playerClan = this.getPlayerClan(playerName);
            if (args.length < 1) {
                this.help(sender);
                return true;
            } else {
                if (args[0].equalsIgnoreCase("create")) {
                    if (playerClan != null && !playerClan.isEmpty()) {
                        sender.sendMessage(MSG.color(prefix + "&cYou are already in a clan."));
                        return true;
                    }

                    this.create(sender, args);
                } else if (args[0].equalsIgnoreCase("disband")) {
                    this.disband(sender, playerClan);
                } else if (args[0].equalsIgnoreCase("report")) {
                    if (args.length < 3) {
                        sender.sendMessage(MSG.color(prefix + "&cUSE: /cls report <clan> <reason>"));
                        return true;
                    }

                    String playerToInvite = args[1];
                    String reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                    this.report(sender, playerToInvite, reason);
                } else if (args[0].equalsIgnoreCase("list")) {
                    this.list(sender);
                } else if (args[0].equalsIgnoreCase("join")) {
                    if (args.length != 2) {
                        sender.sendMessage(MSG.color(prefix + "&cUSE: /cls join <clan>"));
                        return true;
                    }

                    String playerToInvite = args[1];
                    this.joinClan(sender, playerName, playerToInvite);
                } else if (args[0].equalsIgnoreCase("edit")) {
                    this.edit(player, playerClan, args);
                } else if (args[0].equalsIgnoreCase("kick")) {
                    this.kick(sender, args);
                } else if (args[0].equalsIgnoreCase("economy")) {
                    this.Economy(player, playerClan, args);
                } else if (args[0].equalsIgnoreCase("invite")) {
                    if (args.length != 2) {
                        sender.sendMessage(MSG.color(prefix + "&cUse: /cls invite <player>"));
                        return true;
                    }

                    String playerToInvite = args[1];
                    this.inviteToClan(sender, playerToInvite);
                } else if (args[0].equalsIgnoreCase("chat")) {
                    if (playerClan == null || playerClan.isEmpty()) {
                        sender.sendMessage(MSG.color(prefix + "&cYou are not in a clan."));
                        return true;
                    }

                    this.chat(playerClan, player, Arrays.copyOfRange(args, 1, args.length));
                } else if (args[0].equalsIgnoreCase("leave")) {
                    this.leave(sender, playerClan);
                } else if (args[0].equalsIgnoreCase("stats")) {
                    if (playerClan == null || playerClan.isEmpty()) {
                        sender.sendMessage(MSG.color(prefix + "&cYou are not in a clan."));
                        return true;
                    }

                    this.stats(sender, playerClan);
                } else if (args[0].equalsIgnoreCase("resign")) {
                    this.resign(sender, playerClan);
                } else if (args[0].equalsIgnoreCase("ff")) {
                    if (playerClan == null || playerClan.isEmpty()) {
                        sender.sendMessage(MSG.color(prefix + "&cYou are not in a clan."));
                        return true;
                    }
                    if (args.length != 2 || (!args[1].equalsIgnoreCase("on") && !args[1].equalsIgnoreCase("off"))) {
                        sender.sendMessage(MSG.color(prefix + "&cUse: /cls ff <on|off>"));
                        return true;
                    }
                    handleFriendlyFireCommand(sender, playerClan, args);
                } else if (args[0].equalsIgnoreCase("ally")) {
                    if (playerClan == null || playerClan.isEmpty()) {
                        sender.sendMessage(MSG.color(prefix + "&cYou are not in a clan."));
                        return true;
                    }
                    if (args.length != 2) {
                        sender.sendMessage(MSG.color(prefix + "&cUse: /cls ally <clanName>"));
                        return true;
                    }
                    handleAllyCommand(sender, playerName, playerClan, args);
                } else {
                    this.help(sender);
                }

                return false;
            }
        }
    }


    public void help(CommandSender sender) {
        sender.sendMessage(MSG.color("&6======= &lCLAN COMMANDS &6======="));
        sender.sendMessage(MSG.color("&3&lCREATE: &fCreate a new clan and start your adventure!"));
        sender.sendMessage(MSG.color("&3&lJOIN: &fJoin a public clan and make new friends!"));
        sender.sendMessage(MSG.color("&3&lINVITE: &fInvite a player to become part of your clan!"));
        sender.sendMessage(MSG.color("&3&lLEAVE: &fLeave your current clan respectfully."));
        sender.sendMessage(MSG.color("&3&lDISBAND: &fDisband your clan if necessary."));
        sender.sendMessage(MSG.color("&3&lKICK: &fRemove a player from your clan if needed."));
        sender.sendMessage(MSG.color("&3&lFF: &fToggle friendly fire for your clan members."));
        sender.sendMessage(MSG.color("&3&lALLY: &fForm an alliance with another clan."));
        sender.sendMessage(MSG.color("&3&lCHAT: &fTalk with your clan members easily."));
        sender.sendMessage(MSG.color("&3&lSTATS: &fView your clan's achievements and stats."));
        sender.sendMessage(MSG.color("&3&lLIST: &fSee all clans available on the server."));
        sender.sendMessage(MSG.color("&3&lREPORT: &fReport a clan for any issues."));
        sender.sendMessage(MSG.color("&3&lEDIT: &fEdit the clan properties."));
        sender.sendMessage(MSG.color("&3&lECONOMY: &fWithdraw/Deposit money from/for the clan."));
        sender.sendMessage(MSG.color("&3&lRESIGN: &fResign from your position as leader gracefully."));
//        sender.sendMessage(MSG.color(" "));
//        sender.sendMessage(MSG.color("&6======= &lWAR COMMANDS &6======="));
//        sender.sendMessage(MSG.color("&4&lDECLARE: &fDeclare war on another clan!"));
//        sender.sendMessage(MSG.color("&4&lPEACE: &fSend a peace request to an enemy clan."));
//        sender.sendMessage(MSG.color("&4&lACCEPT: &fAccept a peace or alliance request."));
//        sender.sendMessage(MSG.color("&4&lDENY: &fDeny a peace or alliance proposal."));
//        sender.sendMessage(MSG.color("&4&lALLIANCE: &fSend an alliance request to another clan."));
        sender.sendMessage(MSG.color("&6======= " + prefix + " &6======="));
    }

    public void kick(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(MSG.color(prefix + "&c&lUSE:&f /cls kick <player>"));
            return;
        }

        String target = args[1];
        Player player = (Player) sender;
        String clanName = getPlayerClan(player.getName());

        if (clanName == null) {
            sender.sendMessage(MSG.color(prefix + "&cYou are not in a clan."));
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
                sender.sendMessage(MSG.color(prefix + "&cOnly the clan leader can expel members."));
                return;
            }

            if (target.equalsIgnoreCase(player.getName())) {
                sender.sendMessage(MSG.color(prefix + "&cYou can't expel yourself. Use /cls leave."));
                return;
            }

            removeUser.setString(1, target);
            removeUser.setString(2, clanName);
            int removed = removeUser.executeUpdate();
            if (removed == 0) {
                sender.sendMessage(MSG.color(prefix + "&cPlayer is not a member of the clan."));
                return;
            }

            sender.sendMessage(MSG.color(prefix + "&2Player &e&l" + target + " &2has been expelled from clan &e&l" + clanName));

            countUsers.setString(1, clanName);
            ResultSet count = countUsers.executeQuery();
            if (count.next() && count.getInt("total") == 0) {
                deleteClan.setString(1, clanName);
                deleteClan.executeUpdate();
                sender.sendMessage(MSG.color(prefix + "&2The clan is empty. It has been eliminated."));
            }

        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage(MSG.color(prefix + "&cAn error occurred while kicking."));
        }
    }


    public void resign(CommandSender sender, String playerClan) {
        String playerName = sender.getName();

        if (playerClan == null) {
            sender.sendMessage(MSG.color(prefix + "&cYou are not in a clan."));
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
                sender.sendMessage(MSG.color(prefix + "&cYou are not the clan leader."));
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
                sender.sendMessage(MSG.color(prefix + "&cYou resign from leadership! New leader is " + newLeader));
            } else {
                deleteUsers.setString(1, playerClan);
                deleteUsers.executeUpdate();
                deleteClan.setString(1, playerClan);
                deleteClan.executeUpdate();
                sender.sendMessage(MSG.color(prefix + "&cClan deleted due to no members."));
            }

        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage(MSG.color(prefix + "&cError during resign."));
        }
    }


//    public void wars(CommandSender sender, String[] args, String playerClan) {
//        if (args.length < 2) {
//            sender.sendMessage(MSG.color(prefix + "&c&lUSE:&f /clans wars <declare/peace/alliance/accept/deny> <clan>"));
//            return;
//        }
//
//        if (playerClan == null || playerClan.isEmpty()) {
//            sender.sendMessage(MSG.color(prefix + "&cYou are not in a clan."));
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
//            sender.sendMessage(MSG.color(prefix + "&cYou are not the leader of this clan."));
//            return;
//        }
//
//        String action = args[1].toLowerCase();
//        String otherClan;
//
//        switch (action) {
//            case "peace":
//                if (args.length < 3) {
//                    sender.sendMessage(MSG.color(prefix + "&cYou need to specify a clan name to offer peace."));
//                    return;
//                }
//                otherClan = args[2];
//                handlePeaceOffer(sender, playerClan, otherClan, data, fh);
//                break;
//
//            case "declare":
//                if (args.length < 3) {
//                    sender.sendMessage(MSG.color(prefix + "&cYou need to specify a clan name to declare war."));
//                    return;
//                }
//                otherClan = args[2];
//                handleWarDeclaration(sender, playerClan, otherClan, data, fh);
//                break;
//
//            case "alliance":
//                if (args.length < 3) {
//                    sender.sendMessage(MSG.color(prefix + "&cYou need to specify a clan name to declare an alliance."));
//                    return;
//                }
//                otherClan = args[2];
//                handleAllianceRequest(sender, playerClan, otherClan, data, fh);
//                break;
//
//            case "accept":
//                if (args.length < 3) {
//                    sender.sendMessage(MSG.color(prefix + "&cYou need to specify a clan name to accept the alliance."));
//                    return;
//                }
//                otherClan = args[2];
//                handleAllianceAcceptance(sender, playerClan, otherClan, data, fh);
//                break;
//
//            case "deny":
//                if (args.length < 3) {
//                    sender.sendMessage(MSG.color(prefix + "&cYou need to specify a clan name to deny the alliance."));
//                    return;
//                }
//                otherClan = args[2];
//                handleAllianceDenial(sender, playerClan, otherClan, data, fh);
//                break;
//
//            default:
//                sender.sendMessage(MSG.color(prefix + "&cInvalid action. Use: <declare/peace/alliance/accept/deny>"));
//                break;
//        }
//    }
//
//    private void handlePeaceOffer(CommandSender sender, String playerClan, String otherClan, FileConfiguration data, FileHandler fh) {
//        if (!data.contains("Clans." + otherClan)) {
//            sender.sendMessage(MSG.color(prefix + "&cThe specified clan does not exist."));
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
//            sender.sendMessage(MSG.color(prefix + "&cThe specified clan does not exist."));
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
//            sender.sendMessage(MSG.color(prefix + "&cNo clan found with that name."));
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
//            sender.sendMessage(MSG.color(prefix + "&cThe specified clan does not exist."));
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
//            sender.sendMessage(MSG.color(prefix + "&cNo clan found with that name."));
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
            PreparedStatement clanStmt = con.prepareStatement("SELECT founder, leader, privacy, money FROM clans WHERE name=?");
            PreparedStatement membersStmt = con.prepareStatement("SELECT username FROM clan_users WHERE clan=?")) {

            clanStmt.setString(1, clanName);
            ResultSet clanRs = clanStmt.executeQuery();

            if (!clanRs.next()) {
                sender.sendMessage(MSG.color(prefix + "&cNo clan found with that name."));
                return;
            }

            String founder = clanRs.getString("founder");
            String leader = clanRs.getString("leader");
            String privacy = clanRs.getString("privacy");
            double money = clanRs.getDouble("money");

            sender.sendMessage(MSG.color("&2--------&f&lSTATS&2--------"));
            sender.sendMessage(MSG.color("&2Name: &e&l" + clanName));
            sender.sendMessage(MSG.color("&2Founder: &e&l" + founder));
            sender.sendMessage(MSG.color("&2Leader: &e&l" + leader));
            sender.sendMessage(MSG.color("&2Privacy: &e&l" + privacy));
            sender.sendMessage(MSG.color("&2Money: &e&l$" + money));

            membersStmt.setString(1, clanName);
            ResultSet members = membersStmt.executeQuery();
            sender.sendMessage(MSG.color("&2Members:"));
            while (members.next()) {
                sender.sendMessage(MSG.color("&f- &l" + members.getString("username")));
            }

            sender.sendMessage(MSG.color("&2-------- " + prefix + "&2--------"));

        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage(MSG.color(prefix + "&cError loading clan stats."));
        }
    }


    private void Economy(Player player, String clan, String[] args) {
        if (args.length != 3) {
            player.sendMessage(MSG.color(prefix + "&cUsage: /cls eco <deposit|withdraw> <amount>"));
            return;
        }

        String playerClan = getPlayerClan(player.getName());
        if (playerClan == null) {
            player.sendMessage(MSG.color(prefix + "&cYou are not in a clan."));
            return;
        }

        String type = args[1];
        int amount;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            player.sendMessage(MSG.color(prefix + "&cAmount must be a number."));
            return;
        }

        if (amount <= 0) {
            player.sendMessage(MSG.color(prefix + "&cAmount must be greater than 0."));
            return;
        }

        Econo econ = SatipoClan.getEcon();
        double playerBalance = econ.getBalance(player);

        try (Connection con = plugin.getMariaDBManager().getConnection();
            PreparedStatement getMoney = con.prepareStatement("SELECT money FROM clans WHERE name=?");
            PreparedStatement updateMoney = con.prepareStatement("UPDATE clans SET money=? WHERE name=?")) {

            getMoney.setString(1, playerClan);
            ResultSet rs = getMoney.executeQuery();

            if (!rs.next()) {
                player.sendMessage(MSG.color(prefix + "&cClan not found."));
                return;
            }

            double clanMoney = rs.getDouble("money");

            if (type.equalsIgnoreCase("deposit")) {
                if (playerBalance >= amount) {
                    econ.withdraw(player, amount);
                    updateMoney.setDouble(1, clanMoney + amount);
                    updateMoney.setString(2, playerClan);
                    updateMoney.executeUpdate();
                    player.sendMessage(MSG.color(prefix + "&2Deposited &a$" + amount + " &2to clan."));
                } else {
                    player.sendMessage(MSG.color(prefix + "&cNot enough money to deposit."));
                }
            } else if (type.equalsIgnoreCase("withdraw")) {
                if (clanMoney >= amount) {
                    econ.deposit(player, amount);
                    updateMoney.setDouble(1, clanMoney - amount);
                    updateMoney.setString(2, playerClan);
                    updateMoney.executeUpdate();
                    player.sendMessage(MSG.color(prefix + "&2Withdrew &a$" + amount + " &2from clan."));
                } else {
                    player.sendMessage(MSG.color(prefix + "&cClan doesn't have enough money."));
                }
            } else {
                player.sendMessage(MSG.color(prefix + "&cInvalid operation. Use deposit or withdraw."));
            }

        } catch (SQLException e) {
            e.printStackTrace();
            player.sendMessage(MSG.color(prefix + "&cAn error occurred while processing economy action."));
        }
    }


    private void inviteToClan(CommandSender sender, String playerToInvite) {
        Player invitedPlayer = this.plugin.getServer().getPlayer(playerToInvite);
        FileHandler fh = plugin.getFH();
        FileConfiguration data = fh.getData();

        String playerClan = getPlayerClan(sender.getName());
        List<String> invitations = data.getStringList("Clans." + playerClan + ".Invitations");

        Player p = Bukkit.getPlayerExact(playerToInvite);

        if (invitedPlayer != null && invitedPlayer.isOnline()) {
            invitations.add(sender.getName());
            data.set("Clans." + playerClan + ".invitations", invitations);
            fh.saveData();
            sender.sendMessage(MSG.color(prefix + "&2Invitation sent to: &e" + playerToInvite));
            assert p != null;
            p.sendMessage(MSG.color(prefix + "&2You have been invited to the clan: &e" + playerClan));
            p.sendMessage(MSG.color("&8USE: /cls join " + playerClan + " - to join the clan"));
        } else {
            sender.sendMessage(MSG.color(prefix + "&cThis player is not online."));
        }
    }

    public void chat(String clanName, Player player, String[] message) {
        String playerClan = getPlayerClan(player.getName());
        if (playerClan == null || playerClan.isEmpty()) {
            player.sendMessage(MSG.color(prefix + "&cYou are not in a clan."));
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
                    recipient.sendMessage(MSG.color("&e" + playerClan + " &f" + player.getName() + "&f: &7" + formattedMessage));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            player.sendMessage(MSG.color(prefix + "&cError sending clan chat message."));
        }
    }


    private void leave(CommandSender sender, String playerClan) {
        Player player = (Player) sender;
        String playerName = player.getName();

        if (playerClan == null) {
            sender.sendMessage(MSG.color(prefix + "&cYou are not in a clan."));
            return;
        }

        try (Connection con = plugin.getMariaDBManager().getConnection();
            PreparedStatement checkLeader = con.prepareStatement("SELECT leader FROM clans WHERE name=?");
            PreparedStatement removeUser = con.prepareStatement("DELETE FROM clan_users WHERE username=? AND clan=?");
            PreparedStatement countUsers = con.prepareStatement("SELECT username FROM clan_users WHERE clan=?");
            PreparedStatement updateLeader = con.prepareStatement("UPDATE clans SET leader=? WHERE name=?");
            PreparedStatement deleteClan = con.prepareStatement("DELETE FROM clans WHERE name=?")) {

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
                deleteClan.setString(1, playerClan);
                deleteClan.executeUpdate();
                sender.sendMessage(MSG.color(prefix + "&cClan deleted due to no members."));
                return;
            }

            if (isLeader) {
                String newLeader = remaining.get(new Random().nextInt(remaining.size()));
                updateLeader.setString(1, newLeader);
                updateLeader.setString(2, playerClan);
                updateLeader.executeUpdate();
                sender.sendMessage(MSG.color(prefix + "&cYou have left. New leader is " + newLeader));
            } else {
                sender.sendMessage(MSG.color(prefix + "&2You have left the clan."));
            }

        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage(MSG.color(prefix + "&cError leaving clan."));
        }
    }


    private void joinClan(CommandSender sender, String playerName, String clanToJoin) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MSG.color(prefix + "&cOnly players can join clans."));
            return;
        }

        String currentClan = getPlayerClan(playerName);
        if (currentClan != null) {
            sender.sendMessage(MSG.color(prefix + "&cYou are already in a clan."));
            return;
        }

        try (Connection con = plugin.getMariaDBManager().getConnection();
            PreparedStatement clanCheck = con.prepareStatement("SELECT privacy FROM clans WHERE name=?");
            PreparedStatement inviteCheck = con.prepareStatement("SELECT * FROM clan_invites WHERE username=? AND clan=?");
            PreparedStatement addUser = con.prepareStatement("INSERT INTO clan_users (username, clan) VALUES (?, ?)");
            PreparedStatement deleteInvite = con.prepareStatement("DELETE FROM clan_invites WHERE username=? AND clan=?")) {

            clanCheck.setString(1, clanToJoin);
            ResultSet clanRs = clanCheck.executeQuery();

            if (!clanRs.next()) {
                sender.sendMessage(MSG.color(prefix + "&cClan does not exist."));
                return;
            }

            String privacy = clanRs.getString("privacy");

            boolean canJoin = privacy.equalsIgnoreCase("Public");

            if (!canJoin) {
                inviteCheck.setString(1, playerName);
                inviteCheck.setString(2, clanToJoin);
                ResultSet inviteRs = inviteCheck.executeQuery();
                if (inviteRs.next()) {
                    canJoin = true;
                }
            }

            if (!canJoin) {
                sender.sendMessage(MSG.color(prefix + "&cThis clan is &lPrivate&c."));
                return;
            }

            addUser.setString(1, playerName);
            addUser.setString(2, clanToJoin);
            addUser.executeUpdate();

            // Optional: clean up invitation if it existed
            deleteInvite.setString(1, playerName);
            deleteInvite.setString(2, clanToJoin);
            deleteInvite.executeUpdate();

            // Registrar en historial (si mantenés esto)
            PECMD.addClanToHistory(player, clanToJoin);

            sender.sendMessage(MSG.color(prefix + "&2You have joined the clan: &e" + clanToJoin));

        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage(MSG.color(prefix + "&cError joining the clan."));
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
            PreparedStatement ps = con.prepareStatement("SELECT name FROM clans");
            ResultSet rs = ps.executeQuery()) {

            if (!rs.isBeforeFirst()) {
                sender.sendMessage(MSG.color(prefix + "&cThere are no clans on the server."));
                return;
            }

            StringBuilder clansList = new StringBuilder();
            clansList.append(MSG.color(prefix + "&2&lClans:\n"));
            while (rs.next()) {
                clansList.append(MSG.color("&c- ")).append(rs.getString("name")).append("\n");
            }
            clansList.append(MSG.color(prefix + "&c--- end >_< ---"));
            sender.sendMessage(clansList.toString());

        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage(MSG.color(prefix + "&cError fetching clan list."));
        }
    }


    private void report(CommandSender sender, String reportedClan, String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            sender.sendMessage(MSG.color(prefix + "&cPlease provide a valid reason for the report."));
            return;
        }

        try (Connection con = plugin.getMariaDBManager().getConnection();
            PreparedStatement check = con.prepareStatement("SELECT name FROM clans WHERE name=?");
            PreparedStatement checkDup = con.prepareStatement("SELECT * FROM reports WHERE clan=? AND reason=?");
            PreparedStatement insert = con.prepareStatement("INSERT INTO reports (clan, reason) VALUES (?, ?)")) {

            check.setString(1, reportedClan);
            ResultSet clanRs = check.executeQuery();
            if (!clanRs.next()) {
                sender.sendMessage(MSG.color(prefix + "&cThe reported clan does not exist."));
                return;
            }

            checkDup.setString(1, reportedClan);
            checkDup.setString(2, reason);
            ResultSet dupRs = checkDup.executeQuery();
            if (dupRs.next()) {
                sender.sendMessage(MSG.color(prefix + "&cThis report has already been submitted."));
                return;
            }

            insert.setString(1, reportedClan);
            insert.setString(2, reason);
            insert.executeUpdate();

            sender.sendMessage(MSG.color(prefix + "&2Clan reported: &e" + reportedClan + "&2. Reason: " + reason));

        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage(MSG.color(prefix + "&cError submitting report."));
        }
    }


    private void edit(Player player, String clanName, String[] args) {
        if (!isLeader(player, clanName)) {
            player.sendMessage(MSG.color(prefix + "&cOnly the leader can modify the clan!"));
            return;
        }

        if (args.length != 3) {
            player.sendMessage(MSG.color(prefix + "&cUsage: /cls edit <name|privacy> <value>"));
            return;
        }

        String type = args[1];
        String value = args[2];

        try (Connection con = plugin.getMariaDBManager().getConnection()) {
            if (type.equalsIgnoreCase("name")) {
                try (PreparedStatement ps = con.prepareStatement("UPDATE clans SET name=? WHERE name=?")) {
                    ps.setString(1, value);
                    ps.setString(2, clanName);
                    ps.executeUpdate();
                    player.sendMessage(MSG.color(prefix + "&3Clan Name changed to: &f" + value));
                }
            } else if (type.equalsIgnoreCase("privacy")) {
                try (PreparedStatement ps = con.prepareStatement("UPDATE clans SET privacy=? WHERE name=?")) {
                    ps.setString(1, value);
                    ps.setString(2, clanName);
                    ps.executeUpdate();
                    player.sendMessage(MSG.color(prefix + "&3Clan Privacy changed to: &f" + value));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            player.sendMessage(MSG.color(prefix + "&cError editing clan."));
        }
    }


    public void disband(CommandSender sender, String playerClan) {
        if (playerClan == null || playerClan.isEmpty()) {
            sender.sendMessage(MSG.color(prefix + "&cYou are not in a clan."));
            return;
        }

        Player player = (Player) sender;
        Econo econ = SatipoClan.getEcon();

        try (Connection con = plugin.getMariaDBManager().getConnection();
            PreparedStatement checkLeader = con.prepareStatement("SELECT leader FROM clans WHERE name=?");
            PreparedStatement deleteClan = con.prepareStatement("DELETE FROM clans WHERE name=?");
            PreparedStatement deleteUsers = con.prepareStatement("DELETE FROM clan_users WHERE clan=?")) {

            checkLeader.setString(1, playerClan);
            ResultSet rs = checkLeader.executeQuery();
            if (!rs.next() || !rs.getString("leader").equalsIgnoreCase(player.getName())) {
                sender.sendMessage(MSG.color(prefix + "&cYou are not the leader of this clan."));
                return;
            }

            // Economía (si está activa)
            if (plugin.getFH().getConfig().getBoolean("economy.enabled")) {
                int deleteGain = plugin.getFH().getConfig().getInt("economy.earn.delete-clan", 0);
                econ.deposit(player, deleteGain);
                sender.sendMessage(MSG.color(prefix + "&2The clan was eliminated. You won: &e$" + deleteGain));
            } else {
                sender.sendMessage(MSG.color(prefix + "&2The clan was eliminated."));
            }

            deleteUsers.setString(1, playerClan);
            deleteUsers.executeUpdate();

            deleteClan.setString(1, playerClan);
            deleteClan.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage(MSG.color(prefix + "&cError disbanding clan."));
        }
    }


    public void create(CommandSender sender, String[] args) {
        if (args.length < 2 || !(sender instanceof Player player)) {
            sender.sendMessage(MSG.color(prefix + "&c&lUSE:&f /cls create <name>"));
            return;
        }

        String clanName = args[1].toLowerCase();
        String playerName = player.getName();

        FileConfiguration config = plugin.getFH().getConfig();
        Econo econ = SatipoClan.getEcon();

        // Nombre bloqueado
        if (config.getStringList("names-blocked.blocked").contains(clanName)) {
            sender.sendMessage(MSG.color(prefix + "&cThis name is blocked."));
            return;
        }

        try (Connection con = plugin.getMariaDBManager().getConnection();
            PreparedStatement check = con.prepareStatement("SELECT name FROM clans WHERE name=?");
            PreparedStatement insertClan = con.prepareStatement("INSERT INTO clans (name, founder, leader, money, privacy) VALUES (?, ?, ?, 0, 'Public')");
            PreparedStatement insertUser = con.prepareStatement("INSERT INTO clan_users (username, clan) VALUES (?, ?)")) {

            check.setString(1, clanName);
            ResultSet rs = check.executeQuery();
            if (rs.next()) {
                sender.sendMessage(MSG.color(prefix + "&cClan already exists."));
                return;
            }

            // Limite de clanes
            int maxClans = config.getInt("max-clans", 0);
            if (maxClans > 0) {
                try (PreparedStatement countStmt = con.prepareStatement("SELECT COUNT(*) AS total FROM clans")) {
                    ResultSet countRs = countStmt.executeQuery();
                    if (countRs.next() && countRs.getInt("total") >= maxClans) {
                        sender.sendMessage(MSG.color(prefix + "&cClan limit reached (" + maxClans + ")."));
                        return;
                    }
                }
            }

            // Economía
            if (config.getBoolean("economy.enabled")) {
                int cost = config.getInt("economy.cost.create-clan");
                if (econ.getBalance(player) < cost) {
                    sender.sendMessage(MSG.color("&cYou don’t have enough money. You need: &2&l$" + cost));
                    return;
                }
                econ.withdraw(player, cost);
            }

            insertClan.setString(1, clanName);
            insertClan.setString(2, playerName);
            insertClan.setString(3, playerName);
            insertClan.executeUpdate();

            insertUser.setString(1, playerName);
            insertUser.setString(2, clanName);
            insertUser.executeUpdate();

            PECMD.addClanToHistory(player, clanName); // si seguís usando historial
            player.sendMessage(MSG.color(prefix + "&2Your clan &e" + clanName + " &2has been created."));

        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage(MSG.color(prefix + "&cError creating clan."));
        }
    }

    private void handleFriendlyFireCommand(CommandSender sender, String playerClan, String[] args) {
        if (playerClan == null || playerClan.isEmpty()) {
            sender.sendMessage(MSG.color(prefix + "&cYou are not in a clan."));
            return;
        }

        if (args.length != 2 || (!args[1].equalsIgnoreCase("on") && !args[1].equalsIgnoreCase("off"))) {
            sender.sendMessage(MSG.color(prefix + "&cUse: /cls ff <on|off>"));
            return;
        }

        boolean enabled = args[1].equalsIgnoreCase("on");

        try (Connection con = plugin.getMariaDBManager().getConnection();
            PreparedStatement stmt = con.prepareStatement("REPLACE INTO friendlyfire (clan, enabled) VALUES (?, ?)")) {

            stmt.setString(1, playerClan);
            stmt.setBoolean(2, enabled);
            stmt.executeUpdate();

            sender.sendMessage(MSG.color(prefix + "&aFriendly fire is now: &e" + (enabled ? "ON" : "OFF")));

        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage(MSG.color(prefix + "&cError updating friendly fire setting."));
        }
    }

    private void handleAllyCommand(CommandSender sender, String playerName, String playerClan, String[] args) {
        if (playerClan == null || playerClan.isEmpty()) {
            sender.sendMessage(MSG.color(prefix + "&cYou are not in a clan."));
            return;
        }

        if (args.length != 2) {
            sender.sendMessage(MSG.color(prefix + "&cUse: /cls ally <clanName>"));
            return;
        }

        String targetClan = args[1];

        if (targetClan.equalsIgnoreCase(playerClan)) {
            sender.sendMessage(MSG.color(prefix + "&cYou can't ally with your own clan."));
            return;
        }

        try (Connection con = plugin.getMariaDBManager().getConnection();
            PreparedStatement check = con.prepareStatement("SELECT name FROM clans WHERE name=?");
            PreparedStatement insert = con.prepareStatement("""
                INSERT IGNORE INTO alliances (clan1, clan2) VALUES (?, ?), (?, ?)
            """)) {

            check.setString(1, targetClan);
            ResultSet rs = check.executeQuery();
            if (!rs.next()) {
                sender.sendMessage(MSG.color(prefix + "&cThe clan &e" + targetClan + " &cdoes not exist."));
                return;
            }

            insert.setString(1, playerClan);
            insert.setString(2, targetClan);
            insert.setString(3, targetClan);
            insert.setString(4, playerClan);
            insert.executeUpdate();

            sender.sendMessage(MSG.color(prefix + "&aAlliance formed with &e" + targetClan));

        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage(MSG.color(prefix + "&cError creating alliance."));
        }
    }




    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            return args.length == 1 ? List.of("reload") : new ArrayList<>();
        }

        String playerClan = getPlayerClan(player.getName());
        List<String> completions = new ArrayList<>();

        switch (args.length) {
            case 1:
                completions.addAll(List.of(
                        "create", "disband", "report", "list", "join", //"war",
                        "kick", "invite", "chat", "leave", "stats", "resign", "edit", "economy",
                        "ally", "ff"
                ));
                break;

            case 2:
                switch (args[0].toLowerCase()) {
                    case "join":
                        if (isNotInClan(playerClan)) {
                            completions.addAll(getClanNames());
                        }
                        break;
                    case "invite":
                    case "kick":
                        if (isInClan(playerClan) && isLeader(player, playerClan)) {
                            completions.addAll(getOnlinePlayerNames());
                        }
                        break;
//                    case "war":
//                        completions.addAll(List.of("declare", "peace", "alliance", "accept", "deny"));
//                        break;
                    case "economy":
                        completions.addAll(List.of("deposit", "withdraw"));
                        break;
                    case "report":
                        completions.addAll(getClanNames());
                        break;
                    case "edit":
                        if (isInClan(playerClan) && isLeader(player, playerClan)) {
                            completions.addAll(List.of("name", "privacy"));
                        }
                        break;
                }
                break;

            case 3:
                // TODO: Enable when war system is implemented
            /*
            if (args[0].equalsIgnoreCase("war")) {
                switch (args[1].toLowerCase()) {
                    case "declare":
                    case "peace":
                    case "alliance":
                    case "accept":
                    case "deny":
                        if (isInClan(playerClan) && isLeader(player, playerClan)) {
                            completions.addAll(getClanNames());
                        }
                        break;
                }
            }
            */
                break;
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