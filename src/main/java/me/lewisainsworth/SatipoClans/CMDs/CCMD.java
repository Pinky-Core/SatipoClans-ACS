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

import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;

public class CCMD implements CommandExecutor, TabCompleter {
    private final SatipoClan plugin;

    private final List<String> helpLines = Arrays.asList(
        "&e➤ &f&lᴄʀᴇᴀᴛᴇ &7» &fꜰᴏʀᴍᴀ ᴜɴ ᴄʟᴀɴ ɴᴜᴇᴠᴏ ʏ ᴄᴏᴍɪᴇɴᴢᴀ ᴛᴜ ᴀᴠᴇɴᴛᴜʀᴀ.",
        "&e➤ &f&lᴊᴏɪɴ &7» &fúɴᴇᴛᴇ ᴀ ᴜɴ ᴄʟᴀɴ ʏ ʜᴀᴢ ɴᴜᴇᴠᴏꜱ ᴀʟɪᴀᴅᴏꜱ.",
        "&e➤ &f&lɪɴᴠɪᴛᴇ &7» &fɪɴᴠɪᴛᴀ ᴊᴜɢᴀᴅᴏʀᴇꜱ ᴅɪɢɴᴏꜱ ᴀ ᴛᴜ ᴄʟᴀɴ.",
        "&e➤ &f&lʟᴇᴀᴠᴇ &7» &fᴅᴇᴊᴀ ᴛᴜ ᴄʟᴀɴ ᴄᴏɴ ʜᴏɴᴏʀ ʏ ʀᴇꜱᴘᴇᴛᴏ.",
        "&e➤ &f&lᴅɪꜱʙᴀɴᴅ &7» &fᴅɪꜱᴏʟᴠᴇ ᴛᴜ ᴄʟᴀɴ ᴄᴜᴀɴᴅᴏ ꜱᴇᴀ ɴᴇᴄᴇꜱᴀʀɪᴏ.",
        "&e➤ &f&lᴋɪᴄᴋ &7» &fᴇʟɪᴍɪɴᴀ ᴊᴜɢᴀᴅᴏʀᴇꜱ ᴘʀᴏʙʟᴇᴍᴀ́ᴛɪᴄᴏꜱ.",
        "&e➤ &f&lꜰꜰ &7» &fᴀᴄᴛɪᴠᴀ ᴏ ᴅᴇꜱᴀᴄᴛɪᴠᴀ ꜰᴜᴇɢᴏ ᴀᴍɪɢᴏ.",
        "&e➤ &f&lᴀʟʟʏ &7» &fꜰᴏʀᴍᴀ ᴀʟɪᴀɴᴢᴀꜱ ᴄᴏɴ ᴏᴛʀᴏꜱ ᴄʟᴀɴᴇꜱ.",
        "&e➤ &f&lᴄʜᴀᴛ &7» &fᴄᴏᴍᴜɴɪ́ᴄᴀᴛᴇ ᴇɴ ᴘʀɪᴠᴀᴅᴏ ᴄᴏɴ ᴛᴜ ᴄʟᴀɴ.",
        "&e➤ &f&lꜱᴛᴀᴛꜱ &7» &fᴄᴏɴꜱᴜʟᴛᴀ ᴇʟ ᴘʀᴏɢʀᴇꜱᴏ ʏ ʟᴏɢʀᴏꜱ ᴅᴇ ᴛᴜ ᴄʟᴀɴ.",
        "&e➤ &f&lʟɪꜱᴛ &7» &fᴇxᴘʟᴏʀᴀ ʟᴏꜱ ᴄʟᴀɴᴇꜱ ᴅᴇʟ ꜱᴇʀᴠɪᴅᴏʀ.",
        "&e➤ &f&lʀᴇᴘᴏʀᴛ &7» &fʀᴇᴘᴏʀᴛᴀ ᴄʟᴀɴᴇꜱ ǫᴜᴇ ᴄᴀᴜꜱᴇɴ ᴘʀᴏʙʟᴇᴍᴀꜱ.",
        "&e➤ &f&lᴇᴅɪᴛ &7» &fᴍᴏᴅɪꜰɪᴄᴀ ᴅᴇᴛᴀʟʟᴇꜱ ʏ ᴘʀɪᴠᴀᴄɪᴅᴀᴅ.",
        "&e➤ &f&lᴇᴄᴏɴᴏᴍʏ &7» &fᴀᴅᴍɪɴɪꜱᴛʀᴀ ʟᴏꜱ ꜰᴏɴᴅᴏꜱ ᴅᴇʟ ᴄʟᴀɴ.",
        "&e➤ &f&lʀᴇꜱɪɢɴ &7» &fʀᴇɴᴜɴᴄɪᴀ ᴀʟ ʟɪᴅᴇʀᴀᴢɢᴏ ᴄᴏɴ ᴅɪɢɴɪᴅᴀᴅ.",
        "&e➤ &f&lʜᴇʟᴘ &7» &fᴍᴜᴇꜱᴛʀᴀ ᴇꜱᴛᴇ ᴍᴇɴᴜ́ ᴅᴇ ᴀʏᴜᴅᴀ."
    );

    public CCMD(SatipoClan plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String s, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MSG.color(prefix + "&c Comandos de consola: &f/cls reload."));
            return true;
        }

        if (!sender.hasPermission("satipoclans.user")) {
            sender.sendMessage(MSG.color(prefix + "&c No tienes permisos para usar este comando"));
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
                    sender.sendMessage(MSG.color(prefix + "&c Número de página inválido."));
                    return true;
                }
            }

            if (sender instanceof Player p) {
                this.help(p, page);  // Llama al método help paginado
                return true;
            } else {
                sender.sendMessage(MSG.color(prefix + "&c Solo los jugadores pueden ver la ayuda paginada."));
                return true;
            }
        }

        // Resto de comandos
        if (args[0].equalsIgnoreCase("create")) {
            if (playerClan != null && !playerClan.isEmpty()) {
                sender.sendMessage(MSG.color(prefix + "&c Ya perteneces a un clan."));
                return true;
            }
            this.create(sender, args);
        } else if (args[0].equalsIgnoreCase("disband")) {
            this.disband(sender, playerClan);
        } else if (args[0].equalsIgnoreCase("report")) {
            if (args.length < 3) {
                sender.sendMessage(MSG.color(prefix + "&c USO: /cls report <clan> <razón>"));
                return true;
            }
            String playerToInvite = args[1];
            String reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
            this.report(sender, playerToInvite, reason);
        } else if (args[0].equalsIgnoreCase("list")) {
            this.list(sender);
        } else if (args[0].equalsIgnoreCase("join")) {
            if (args.length != 2) {
                sender.sendMessage(MSG.color(prefix + "&c USO: /cls join <clan>"));
                return true;
            }
            String playerToInvite = args[1];
            this.joinClan(sender, playerName, playerToInvite);
        } else if (args[0].equalsIgnoreCase("edit")) {
            this.edit(player, playerClan, args);
        } else if (args[0].equalsIgnoreCase("kick")) {
            this.kick(sender, args);
        /*} else if (args[0].equalsIgnoreCase("economy")) {
            this.Economy(player, playerClan, args); */
        } else if (args[0].equalsIgnoreCase("invite")) {
            if (args.length != 2) {
                sender.sendMessage(MSG.color(prefix + "&c USO: /cls invite <jugador>"));
                return true;
            }
            String playerToInvite = args[1];
            this.inviteToClan(sender, playerToInvite);
        } else if (args[0].equalsIgnoreCase("chat")) {
            if (playerClan == null || playerClan.isEmpty()) {
                sender.sendMessage(MSG.color(prefix + "&c No perteneces a ningún clan."));
                return true;
            }
            this.chat(playerClan, player, Arrays.copyOfRange(args, 1, args.length));
        } else if (args[0].equalsIgnoreCase("leave")) {
            this.leave(sender, playerClan);
        } else if (args[0].equalsIgnoreCase("stats")) {
            if (playerClan == null || playerClan.isEmpty()) {
                sender.sendMessage(MSG.color(prefix + "&c No perteneces a ningún clan."));
                return true;
            }
            this.stats(sender, playerClan);
        } else if (args[0].equalsIgnoreCase("resign")) {
            this.resign(sender, playerClan);
        } else if (args[0].equalsIgnoreCase("ff")) {
            if (playerClan == null || playerClan.isEmpty()) {
                sender.sendMessage(MSG.color(prefix + "&c No perteneces a ningún clan."));
                return true;
            }
            if (args.length != 2 || (!args[1].equalsIgnoreCase("on") && !args[1].equalsIgnoreCase("off"))) {
                sender.sendMessage(MSG.color(prefix + "&c USO: /cls ff <on|off>"));
                return true;
            }
            handleFriendlyFireCommand(sender, playerClan, args);
        } else if (args[0].equalsIgnoreCase("ally")) {
            if (playerClan == null || playerClan.isEmpty()) {
                sender.sendMessage(MSG.color(prefix + "&c No perteneces a ningún clan."));
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(MSG.color(prefix + "&c USO: /cls ally <request|accept|decline|remove> <nombreClan>"));
                return true;
            }
            String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
            handleAllyCommand(sender, playerName, playerClan, subArgs);
        } else {
            this.help(player, 1);  // Mostrar página 1 de ayuda por defecto
        }

        return true;
    }



    public void help(Player player, int page) {
        int linesPerPage = 5;
        int totalPages = (int) Math.ceil((double) helpLines.size() / linesPerPage);

        if (page < 1 || page > totalPages) {
            player.sendMessage(MSG.color(prefix + "&c Página inválida. Usa /cls help <1-" + totalPages + ">"));
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
            prev.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/cls help " + (page - 1)));
            prev.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Haz clic para ir a la página " + (page - 1))));
            nav.addExtra(prev);
        }

        if (page < totalPages) {
            TextComponent next = new TextComponent(MSG.color("&e Página siguiente »"));
            next.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/cls help " + (page + 1)));
            next.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Haz clic para ir a la página " + (page + 1))));
            nav.addExtra(next);
        }

        player.spigot().sendMessage(nav);
        player.sendMessage(MSG.color("&6&m====================================="));
    }





    public void kick(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(MSG.color(prefix + "&c &lUSO:&f /cls kick <jugador>"));
            return;
        }

        String target = args[1];
        Player player = (Player) sender;
        String clanName = getPlayerClan(player.getName());

        if (clanName == null) {
            sender.sendMessage(MSG.color(prefix + "&c No perteneces a ningún clan."));
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
                sender.sendMessage(MSG.color(prefix + "&c Solo el líder del clan puede expulsar miembros."));
                return;
            }

            if (target.equalsIgnoreCase(player.getName())) {
                sender.sendMessage(MSG.color(prefix + "&c No puedes expulsarte a ti mismo. Usa /cls leave."));
                return;
            }

            removeUser.setString(1, target);
            removeUser.setString(2, clanName);
            int removed = removeUser.executeUpdate();
            if (removed == 0) {
                sender.sendMessage(MSG.color(prefix + "&c El jugador no es miembro del clan."));
                return;
            }

            sender.sendMessage(MSG.color(prefix + "&2 El jugador &e&l" + target + " &2ha sido expulsado del clan &e&l" + clanName));

            countUsers.setString(1, clanName);
            ResultSet count = countUsers.executeQuery();
            if (count.next() && count.getInt("total") == 0) {
                deleteClan.setString(1, clanName);
                deleteClan.executeUpdate();
                sender.sendMessage(MSG.color(prefix + "&2 El clan está vacío. Ha sido eliminado."));
            }

        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage(MSG.color(prefix + "&c Ocurrió un error al expulsar."));
        }
    }


    public void resign(CommandSender sender, String playerClan) {
        String playerName = sender.getName();

        if (playerClan == null) {
            sender.sendMessage(MSG.color(prefix + "&c No perteneces a ningún clan."));
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
                sender.sendMessage(MSG.color(prefix + "&c No eres el líder del clan."));
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
                sender.sendMessage(MSG.color(prefix + "&c ¡Has renunciado al liderazgo! El nuevo líder es " + newLeader));
            } else {
                deleteUsers.setString(1, playerClan);
                deleteUsers.executeUpdate();
                deleteClan.setString(1, playerClan);
                deleteClan.executeUpdate();
                sender.sendMessage(MSG.color(prefix + "&c Clan eliminado por no tener miembros."));
            }

        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage(MSG.color(prefix + "&c Error al renunciar."));
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
            PreparedStatement clanStmt = con.prepareStatement("SELECT founder, leader, privacy, money FROM clans WHERE name=?");
            PreparedStatement membersStmt = con.prepareStatement("SELECT username FROM clan_users WHERE clan=?")) {

            clanStmt.setString(1, clanName);
            ResultSet clanRs = clanStmt.executeQuery();

            if (!clanRs.next()) {
                sender.sendMessage(MSG.color(prefix + "&c No se encontró un clan con ese nombre."));
                return;
            }

            String founder = clanRs.getString("founder");
            String leader = clanRs.getString("leader");
            String privacy = clanRs.getString("privacy");
            double money = clanRs.getDouble("money");

            sender.sendMessage(MSG.color("&8"));
            sender.sendMessage(MSG.color("&8&m===================================================="));
            sender.sendMessage(MSG.color("&8&l» &e&lEstadísticas: &b" + clanName + "&8&l «"));
            sender.sendMessage(MSG.color("&8&m===================================================="));
            sender.sendMessage(MSG.color("&7Fundador: &f" + founder));
            sender.sendMessage(MSG.color("&7Líder: &f" + leader));
            sender.sendMessage(MSG.color("&7Privacidad: &f" + privacy));
            sender.sendMessage(MSG.color("&7Dinero del clan: &a$" + money));

            membersStmt.setString(1, clanName);
            ResultSet members = membersStmt.executeQuery();
            sender.sendMessage(MSG.color("&2Miembros:"));
            while (members.next()) {
                sender.sendMessage(MSG.color("&f- &l" + members.getString("username")));
            }

            sender.sendMessage(MSG.color("&2================== " + prefix + "&2=================="));

        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage(MSG.color(prefix + "&c Error al cargar las estadísticas del clan."));
        }
    }


    /* private void Economy(Player player, String clan, String[] args) {
        if (args.length != 3) {
            player.sendMessage(MSG.color(prefix + "&c USO: /cls economy <depositar|retirar> <cantidad>"));
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
            sender.sendMessage(MSG.color(prefix + "&c Solo los jugadores pueden enviar invitaciones."));
            return;
        }

        String inviterName = inviter.getName();
        String inviterClan = plugin.getMariaDBManager().getCachedPlayerClan(inviterName);

        if (inviterClan == null) {
            sender.sendMessage(MSG.color(prefix + "&c No estás en un clan."));
            return;
        }

        if (playerToInvite.equalsIgnoreCase(inviterName)) {
            sender.sendMessage(MSG.color(prefix + "&c ¡No puedes invitarte a ti mismo!"));
            return;
        }

        Player invitedPlayer = Bukkit.getPlayerExact(playerToInvite);
        if (invitedPlayer == null || !invitedPlayer.isOnline()) {
            sender.sendMessage(MSG.color(prefix + "&c El jugador no está en línea."));
            return;
        }

        String invitedPlayerClan = plugin.getMariaDBManager().getCachedPlayerClan(playerToInvite);
        if (invitedPlayerClan != null) {
            sender.sendMessage(MSG.color(prefix + "&c Ese jugador ya pertenece a otro clan."));
            return;
        }

        try (Connection con = plugin.getMariaDBManager().getConnection()) {
            // Chequear invitación vigente (menos de 5 minutos)
            String checkSql = "SELECT invite_time FROM clan_invites WHERE clan=? AND username=?";
            try (PreparedStatement checkStmt = con.prepareStatement(checkSql)) {
                checkStmt.setString(1, inviterClan);
                checkStmt.setString(2, playerToInvite);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        long inviteTime = rs.getLong("invite_time");
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - inviteTime < 5 * 60 * 1000) {
                            sender.sendMessage(MSG.color(prefix + "&c Este jugador ya tiene una invitación pendiente."));
                            return;
                        } else {
                            // Invitación expirada, eliminarla para reemplazarla
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

            // Insertar invitación nueva con timestamp actual
            String insertSql = "INSERT INTO clan_invites (clan, username, invite_time) VALUES (?, ?, ?)";
            try (PreparedStatement insertStmt = con.prepareStatement(insertSql)) {
                insertStmt.setString(1, inviterClan);
                insertStmt.setString(2, playerToInvite);
                insertStmt.setLong(3, System.currentTimeMillis());
                insertStmt.executeUpdate();
            }

            sender.sendMessage(MSG.color(prefix + "&a Invitación enviada a &f" + playerToInvite));
            invitedPlayer.sendMessage(MSG.color(prefix + "&e Has recibido una invitación para unirte al clan &f" + inviterClan));
            invitedPlayer.sendMessage(MSG.color("&8 USO: /cls join " + inviterClan + " &7- para unirte al clan"));

        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage(MSG.color(prefix + "&c Error al enviar la invitación."));
        }
    }


    public void chat(String clanName, Player player, String[] message) {
        String playerClan = getPlayerClan(player.getName());
        if (playerClan == null || playerClan.isEmpty()) {
            player.sendMessage(MSG.color(prefix + "&c No perteneces a ningún clan."));
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
            player.sendMessage(MSG.color(prefix + "&c Error al enviar el mensaje al chat del clan."));
        }
    }


    private void leave(CommandSender sender, String playerClan) {
        Player player = (Player) sender;
        String playerName = player.getName();

        if (playerClan == null) {
            sender.sendMessage(MSG.color(prefix + "&c No perteneces a ningún clan."));
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
                sender.sendMessage(MSG.color(prefix + "&c Clan eliminado por no tener miembros."));
                return;
            }

            if (isLeader) {
                String newLeader = remaining.get(new Random().nextInt(remaining.size()));
                updateLeader.setString(1, newLeader);
                updateLeader.setString(2, playerClan);
                updateLeader.executeUpdate();
                sender.sendMessage(MSG.color(prefix + "&c Has salido. El nuevo líder es " + newLeader));
            } else {
                sender.sendMessage(MSG.color(prefix + "&2 Has salido del clan."));
            }

            plugin.getMariaDBManager().reloadCache();

        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage(MSG.color(prefix + "&c Error al salir del clan."));
        }
    }


    private static final long INVITE_EXPIRATION_MS = 5 * 60 * 1000; // 5 minutos en ms

    private void joinClan(CommandSender sender, String playerName, String clanToJoin) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MSG.color(prefix + "&c Solo los jugadores pueden unirse a clanes."));
            return;
        }

        String currentClan = getPlayerClan(playerName);
        if (currentClan != null) {
            sender.sendMessage(MSG.color(prefix + "&c Ya perteneces a un clan."));
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
                sender.sendMessage(MSG.color(prefix + "&c El clan no existe."));
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
                            // Invitación expiró
                            sender.sendMessage(MSG.color(prefix + "&c Tu invitación para unirte al clan ha expirado."));
                            // Opcional: borrar invitación expirada
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
                sender.sendMessage(MSG.color(prefix + "&c Este clan es &lPrivado&c."));
                return;
            }

            addUser.setString(1, playerName);
            addUser.setString(2, clanToJoin);
            addUser.executeUpdate();

            // Borrar invitación si existía
            deleteInvite.setString(1, playerName);
            deleteInvite.setString(2, clanToJoin);
            deleteInvite.executeUpdate();

            // Registrar en historial (si mantenés esto)
            PECMD.addClanToHistory(player, clanToJoin);

            sender.sendMessage(MSG.color(prefix + "&2 Te has unido al clan: &e" + clanToJoin));

        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage(MSG.color(prefix + "&c Error al unirse al clan."));
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
                "SELECT name FROM clans WHERE name NOT IN (SELECT name FROM banned_clans)"
            );
            ResultSet rs = ps.executeQuery()) {

            if (!rs.isBeforeFirst()) {
                sender.sendMessage(MSG.color(prefix + "&c No hay clanes en el servidor."));
                return;
            }

            StringBuilder clansList = new StringBuilder();
            clansList.append(MSG.color(prefix + "&2&l Clanes:\n"));

            while (rs.next()) {
                String rawName = rs.getString("name");
                clansList.append(MSG.color("&7- " + MSG.color(rawName))).append("\n");
            }

            clansList.append(MSG.color("&2&m=================="));
            sender.sendMessage(clansList.toString());

        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage(MSG.color(prefix + "&c Error al obtener la lista de clanes."));
        }
    }



    private void report(CommandSender sender, String reportedClan, String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            sender.sendMessage(MSG.color(prefix + "&c Por favor proporciona una razón válida para el reporte."));
            return;
        }

        try (Connection con = plugin.getMariaDBManager().getConnection();
            PreparedStatement check = con.prepareStatement("SELECT name FROM clans WHERE name=?");
            PreparedStatement checkDup = con.prepareStatement("SELECT * FROM reports WHERE clan=? AND reason=?");
            PreparedStatement insert = con.prepareStatement("INSERT INTO reports (clan, reason) VALUES (?, ?)")) {

            check.setString(1, reportedClan);
            ResultSet clanRs = check.executeQuery();
            if (!clanRs.next()) {
                sender.sendMessage(MSG.color(prefix + "&c El clan reportado no existe."));
                return;
            }

            checkDup.setString(1, reportedClan);
            checkDup.setString(2, reason);
            ResultSet dupRs = checkDup.executeQuery();
            if (dupRs.next()) {
                sender.sendMessage(MSG.color(prefix + "&c Este reporte ya ha sido enviado."));
                return;
            }

            insert.setString(1, reportedClan);
            insert.setString(2, reason);
            insert.executeUpdate();

            sender.sendMessage(MSG.color(prefix + "&2 Clan reportado: &e" + reportedClan + "&2. Razón: " + reason));

        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage(MSG.color(prefix + "&c Error al enviar el reporte."));
        }
    }


    private void edit(Player player, String clanName, String[] args) {
        if (!isLeader(player, clanName)) {
            player.sendMessage(MSG.color(prefix + "&c ¡Solo el líder puede modificar el clan!"));
            return;
        }

        if (args.length != 3) {
            player.sendMessage(MSG.color(prefix + "&c USO: /cls edit <name|privacy> <valor>"));
            return;
        }

        String type = args[1];
        String value = args[2];

        try (Connection con = plugin.getMariaDBManager().getConnection()) {
            if (type.equalsIgnoreCase("name")) {
                con.setAutoCommit(false);
                try {
                    // Actualizar nombre en todas las tablas necesarias
                    try (PreparedStatement ps = con.prepareStatement("UPDATE clans SET name=? WHERE name=?")) {
                        ps.setString(1, value);
                        ps.setString(2, clanName);
                        ps.executeUpdate();
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
                    player.sendMessage(MSG.color(prefix + "&3 Nombre del clan cambiado a: &f" + value));
                } catch (SQLException e) {
                    con.rollback();
                    e.printStackTrace();
                    player.sendMessage(MSG.color(prefix + "&c Error al cambiar el nombre del clan."));
                } finally {
                    con.setAutoCommit(true);
                }
            } else if (type.equalsIgnoreCase("privacy")) {
                try (PreparedStatement ps = con.prepareStatement("UPDATE clans SET privacy=? WHERE name=?")) {
                    ps.setString(1, value);
                    ps.setString(2, clanName);
                    ps.executeUpdate();
                    player.sendMessage(MSG.color(prefix + "&3 Privacidad del clan cambiada a: &f" + value));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            player.sendMessage(MSG.color(prefix + "&c Error al editar el clan."));
        }
    }




    public void disband(CommandSender sender, String playerClan) {
        if (playerClan == null || playerClan.isEmpty()) {
            sender.sendMessage(MSG.color(prefix + "&c No perteneces a ningún clan."));
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
                        sender.sendMessage(MSG.color(prefix + "&c No eres el líder de este clan."))
                    );
                    return;
                }

                // Iniciar eliminación de todo lo relacionado
                deleteEntireClanData(con, playerClan);

                boolean econEnabled = plugin.getFH().getConfig().getBoolean("economy.enabled");
                int deleteGain = plugin.getFH().getConfig().getInt("economy.earn.delete-clan", 0);
                if (econEnabled) econ.deposit(player, deleteGain);

                // Actualizar caché
                plugin.getMariaDBManager().reloadCache();

                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (econEnabled) {
                        sender.sendMessage(MSG.color(prefix + "&2 El clan fue eliminado. Ganaste: &e$" + deleteGain));
                    } else {
                        sender.sendMessage(MSG.color(prefix + "&2 El clan fue eliminado."));
                    }
                });

            } catch (SQLException e) {
                e.printStackTrace();
                Bukkit.getScheduler().runTask(plugin, () ->
                    sender.sendMessage(MSG.color(prefix + "&c Error al eliminar el clan."))
                );
            }
        });
    }




    public void create(CommandSender sender, String[] args) {
        if (args.length < 2 || !(sender instanceof Player player)) {
            sender.sendMessage(MSG.color(prefix + "&c&l USO:&f /cls create <nombre>"));
            return;
        }

        String clanName = args[1];
        String playerName = player.getName();
        FileConfiguration config = plugin.getFH().getConfig();
        Econo econ = SatipoClan.getEcon();

        // Nombre bloqueado
        if (config.getStringList("names-blocked.blocked").contains(clanName)) {
            sender.sendMessage(MSG.color(prefix + "&c Este nombre está bloqueado."));
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection con = plugin.getMariaDBManager().getConnection();
                PreparedStatement check = con.prepareStatement("SELECT name FROM clans WHERE name=?")) {

                check.setString(1, clanName);
                ResultSet rs = check.executeQuery();
                if (rs.next()) {
                    Bukkit.getScheduler().runTask(plugin, () ->
                        sender.sendMessage(MSG.color(prefix + "&c El clan ya existe."))
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
                                sender.sendMessage(MSG.color(prefix + "&c Se alcanzó el límite de clanes (" + maxClans + ")."))
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
                            sender.sendMessage(MSG.color("&c No tienes suficiente dinero. Necesitas: &2&l$" + cost))
                        );
                        return;
                    }
                    econ.withdraw(player, cost);
                }

                try (PreparedStatement insertClan = con.prepareStatement("INSERT INTO clans (name, founder, leader, money, privacy) VALUES (?, ?, ?, 0, 'Private')");
                    PreparedStatement insertUser = con.prepareStatement("INSERT INTO clan_users (username, clan) VALUES (?, ?)")) {

                    insertClan.setString(1, clanName);
                    insertClan.setString(2, playerName);
                    insertClan.setString(3, playerName);
                    insertClan.executeUpdate();

                    insertUser.setString(1, playerName);
                    insertUser.setString(2, clanName);
                    insertUser.executeUpdate();
                }

                Bukkit.getScheduler().runTask(plugin, () -> {
                    PECMD.addClanToHistory(player, clanName); // si seguís usando historial
                    player.sendMessage(MSG.color(prefix + "&2 Tu clan &e" + clanName + " &2ha sido creado."));
                });

            } catch (SQLException e) {
                e.printStackTrace();
                Bukkit.getScheduler().runTask(plugin, () ->
                    sender.sendMessage(MSG.color(prefix + "&c Error al crear el clan."))
                );
            }
        });
    }


    private void handleFriendlyFireCommand(CommandSender sender, String playerClan, String[] args) {
        if (playerClan == null || playerClan.isEmpty()) {
            sender.sendMessage(MSG.color(prefix + "&c No perteneces a ningún clan."));
            return;
        }

        if (args.length != 2 || (!args[1].equalsIgnoreCase("on") && !args[1].equalsIgnoreCase("off"))) {
            sender.sendMessage(MSG.color(prefix + "&c USO: /cls ff <on|off>"));
            return;
        }

        boolean enabled = args[1].equalsIgnoreCase("on");

        try (Connection con = plugin.getMariaDBManager().getConnection();
            PreparedStatement stmt = con.prepareStatement("REPLACE INTO friendlyfire (clan, enabled) VALUES (?, ?)")) {

            stmt.setString(1, playerClan);
            stmt.setBoolean(2, enabled);
            stmt.executeUpdate();

            sender.sendMessage(MSG.color(prefix + "&a El fuego amigo ahora está: &e" + (enabled ? "ACTIVADO" : "DESACTIVADO")));

        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage(MSG.color(prefix + "&c Error al actualizar la configuración de fuego amigo."));
        }
    }

    private void handleAllyFriendlyFireCommand(CommandSender sender, String playerClan, String[] args) {
        if (playerClan == null || playerClan.isEmpty()) {
            sender.sendMessage(MSG.color(prefix + "&c No perteneces a ningún clan."));
            return;
        }

        if (args.length != 2 || (!args[1].equalsIgnoreCase("on") && !args[1].equalsIgnoreCase("off"))) {
            sender.sendMessage(MSG.color(prefix + "&c USO: /clan allyff <on|off>"));
            return;
        }

        boolean enabled = args[1].equalsIgnoreCase("on");

        try (Connection con = plugin.getMariaDBManager().getConnection();
            PreparedStatement stmt = con.prepareStatement("REPLACE INTO friendlyfire_allies (clan, enabled) VALUES (?, ?)")) {

            stmt.setString(1, playerClan);
            stmt.setBoolean(2, enabled);
            stmt.executeUpdate();

            sender.sendMessage(MSG.color(prefix + "&a El fuego amigo entre aliados ahora está: &e" + (enabled ? "ACTIVADO" : "DESACTIVADO")));

        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage(MSG.color(prefix + "&c Error al actualizar la configuración de fuego amigo entre aliados."));
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
        if (playerClan == null || playerClan.isEmpty()) {
            sender.sendMessage(MSG.color(prefix + "&c No perteneces a ningún clan."));
            return;
        }

        if (args.length < 1) {
            sender.sendMessage(MSG.color(prefix + "&c USO: /cls ally <request|accept|decline|remove|ff> [<clan>|<on|off>]"));
            return;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "request":
                if (args.length != 2) {
                    sender.sendMessage(MSG.color(prefix + "&c Uso correcto: /cls ally request <clan>"));
                    return;
                }
                sendAllyRequest(sender, playerClan, args[1]);
                break;

            case "accept":
                if (args.length != 2) {
                    sender.sendMessage(MSG.color(prefix + "&c Uso correcto: /cls ally accept <clan>"));
                    return;
                }
                acceptAlly(sender, playerClan, args[1]);
                break;

            case "decline":
                if (args.length != 2) {
                    sender.sendMessage(MSG.color(prefix + "&c Uso correcto: /cls ally decline <clan>"));
                    return;
                }
                declineAlly(sender, playerClan, args[1]);
                break;

            case "remove":
                if (args.length != 2) {
                    sender.sendMessage(MSG.color(prefix + "&c Uso correcto: /cls ally remove <clan>"));
                    return;
                }
                removeAlly(sender, playerClan, args[1]);
                break;

            case "ff":
                if (args.length != 2) {
                    sender.sendMessage(MSG.color(prefix + "&c Uso correcto: /cls ally ff <on|off>"));
                    return;
                }
                handleAllyFriendlyFireCommand(sender, playerClan, args[1]);
                break;

            default:
                sender.sendMessage(MSG.color(prefix + "&c Subcomando inválido. Usa request, accept, decline, remove o ff."));
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
            sender.sendMessage(MSG.color(prefix + "&c No puedes aliarte con tu propio clan."));
            return;
        }

        try (Connection con = plugin.getMariaDBManager().getConnection();
            PreparedStatement check = con.prepareStatement("SELECT name FROM clans WHERE name = ?");
            PreparedStatement checkPending = con.prepareStatement("SELECT 1 FROM pending_alliances WHERE requester = ? AND target = ?");
            PreparedStatement insert = con.prepareStatement("INSERT INTO pending_alliances (requester, target) VALUES (?, ?)")) {

            check.setString(1, targetClan);
            ResultSet rs = check.executeQuery();
            if (!rs.next()) {
                sender.sendMessage(MSG.color(prefix + "&c El clan &e" + targetClan + " &cno existe."));
                return;
            }

            checkPending.setString(1, playerClan);
            checkPending.setString(2, targetClan);
            ResultSet rsPending = checkPending.executeQuery();
            if (rsPending.next()) {
                sender.sendMessage(MSG.color(prefix + "&c Ya enviaste una solicitud a ese clan."));
                return;
            }

            insert.setString(1, playerClan);
            insert.setString(2, targetClan);
            insert.executeUpdate();

            sender.sendMessage(MSG.color(prefix + "&a Solicitud de alianza enviada a &e" + targetClan));

            // Notificar a jugadores online del clan target
            for (Player p : Bukkit.getOnlinePlayers()) {
                String pClan = this.getPlayerClan(p.getName());
                if (pClan != null && pClan.equalsIgnoreCase(targetClan)) {
                    p.sendMessage(MSG.color(prefix + "&eTienes una nueva solicitud de alianza de &a" + playerClan));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage(MSG.color(prefix + "&c Error al enviar la solicitud de alianza."));
        }
    }

    private void acceptAlly(CommandSender sender, String playerClan, String requesterClan) {
        if (requesterClan.equalsIgnoreCase(playerClan)) {
            sender.sendMessage(MSG.color(prefix + "&c No puedes aceptar alianza con tu propio clan."));
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
                sender.sendMessage(MSG.color(prefix + "&c No tienes solicitudes pendientes de ese clan."));
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
            con.setAutoCommit(true);

            sender.sendMessage(MSG.color(prefix + "&a Has aceptado la alianza con &e" + requesterClan));

            // Notificar a jugadores online del clan solicitante
            for (Player p : Bukkit.getOnlinePlayers()) {
                String pClan = this.getPlayerClan(p.getName());
                if (pClan != null && pClan.equalsIgnoreCase(requesterClan)) {
                    p.sendMessage(MSG.color(prefix + "&a El clan &e" + playerClan + " &aha aceptado la alianza."));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage(MSG.color(prefix + "&c Error al aceptar la alianza."));
            try {
                plugin.getMariaDBManager().getConnection().setAutoCommit(true);
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void declineAlly(CommandSender sender, String playerClan, String requesterClan) {
        if (requesterClan.equalsIgnoreCase(playerClan)) {
            sender.sendMessage(MSG.color(prefix + "&c No puedes rechazar alianza con tu propio clan."));
            return;
        }

        try (Connection con = plugin.getMariaDBManager().getConnection();
            PreparedStatement checkPending = con.prepareStatement("SELECT 1 FROM pending_alliances WHERE requester = ? AND target = ?");
            PreparedStatement deletePending = con.prepareStatement("DELETE FROM pending_alliances WHERE requester = ? AND target = ?")) {

            checkPending.setString(1, requesterClan);
            checkPending.setString(2, playerClan);
            ResultSet rs = checkPending.executeQuery();

            if (!rs.next()) {
                sender.sendMessage(MSG.color(prefix + "&c No tienes solicitudes pendientes de ese clan."));
                return;
            }

            deletePending.setString(1, requesterClan);
            deletePending.setString(2, playerClan);
            deletePending.executeUpdate();

            sender.sendMessage(MSG.color(prefix + "&a Has rechazado la alianza con &e" + requesterClan));

            // Notificar a jugadores online del clan solicitante
            for (Player p : Bukkit.getOnlinePlayers()) {
                String pClan = this.getPlayerClan(p.getName());
                if (pClan != null && pClan.equalsIgnoreCase(requesterClan)) {
                    p.sendMessage(MSG.color(prefix + "&c El clan &e" + playerClan + " &ch a rechazado la alianza."));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage(MSG.color(prefix + "&c Error al rechazar la alianza."));
        }
    }

    private void removeAlly(CommandSender sender, String playerClan, String targetClan) {
        if (targetClan.equalsIgnoreCase(playerClan)) {
            sender.sendMessage(MSG.color(prefix + "&c No puedes eliminar alianza con tu propio clan."));
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
                sender.sendMessage(MSG.color(prefix + "&a Has eliminado la alianza con &e" + targetClan));
            } else {
                sender.sendMessage(MSG.color(prefix + "&c No tienes alianza con ese clan."));
            }

        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage(MSG.color(prefix + "&c Error al eliminar la alianza."));
        }
    }

    private void handleAllyFriendlyFireCommand(CommandSender sender, String playerClan, String value) {
        if (!value.equalsIgnoreCase("on") && !value.equalsIgnoreCase("off")) {
            sender.sendMessage(MSG.color(prefix + "&c Uso correcto: /cls ally ff <on|off>"));
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

            sender.sendMessage(MSG.color(prefix + "&a El fuego amigo entre aliados ahora está: &e" + (ffEnabled ? "ACTIVADO" : "DESACTIVADO")));

        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage(MSG.color(prefix + "&c Error al modificar la configuración de fuego amigo entre aliados."));
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
                    "ff", "ally", "help"
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