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
            sender.sendMessage(MSG.color(prefix + "&c Comandos de consola: &f/cls reload."));
            return true;
        } else {
            if (!sender.hasPermission("sc.user")) {
                sender.sendMessage(MSG.color(prefix + "&c No tienes permisos para usar este comando"));
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
                } else if (args[0].equalsIgnoreCase("economy")) {
                    this.Economy(player, playerClan, args);
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
                    if (args.length != 2) {
                        sender.sendMessage(MSG.color(prefix + "&c USO: /cls ally <nombreClan>"));
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
        sender.sendMessage(MSG.color("&6&m====================================="));
        sender.sendMessage(MSG.color("&6&l» &a&lSatipo&6&lClans &e&lComandos &6«"));
        sender.sendMessage(MSG.color("&6&m====================================="));
        sender.sendMessage(MSG.color(""));

        sender.sendMessage(MSG.color("&e\u27A4 &e&lCREAR &7» &fForma un clan nuevo y comienza tu aventura."));
        sender.sendMessage(MSG.color("&e\u27A4 &e&lUNIRSE &7» &fÚnete a un clan y haz nuevos aliados."));
        sender.sendMessage(MSG.color("&e\u27A4 &e&lINVITAR &7» &fInvita jugadores dignos a tu clan."));
        sender.sendMessage(MSG.color("&e\u27A4 &e&lSALIR &7» &fDeja tu clan con honor y respeto."));
        sender.sendMessage(MSG.color("&e\u27A4 &e&lDISOLVER &7» &fDisuelve tu clan cuando sea necesario."));
        sender.sendMessage(MSG.color("&e\u27A4 &e&lEXPULSAR &7» &fElimina jugadores problemáticos de tu clan."));
        sender.sendMessage(MSG.color("&e\u27A4 &e&lFF &7» &fActiva o desactiva fuego amigo para tu clan."));
        sender.sendMessage(MSG.color("&e\u27A4 &e&lALIANZA &7» &fForma alianzas poderosas con otros clanes."));
        sender.sendMessage(MSG.color("&e\u27A4 &e&lCHAT &7» &fComunícate en privado con tu clan."));
        sender.sendMessage(MSG.color("&e\u27A4 &e&lESTADÍSTICAS &7» &fConsulta el progreso y logros de tu clan."));
        sender.sendMessage(MSG.color("&e\u27A4 &e&lLISTAR &7» &fExplora todos los clanes del servidor."));
        sender.sendMessage(MSG.color("&e\u27A4 &e&lREPORTAR &7» &fReporta clanes que causen problemas."));
        sender.sendMessage(MSG.color("&e\u27A4 &e&lEDITAR &7» &fModifica detalles y configuraciones del clan."));
        sender.sendMessage(MSG.color("&e\u27A4 &e&lECONOMÍA &7» &fAdministra los fondos del clan."));
        sender.sendMessage(MSG.color("&e\u27A4 &e&lRENUNCIAR &7» &fRenuncia a tu liderazgo con dignidad."));

        sender.sendMessage(MSG.color(""));
        sender.sendMessage(MSG.color("&6&m====================================="));
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

            sender.sendMessage(MSG.color(prefix + "&2El jugador &e&l" + target + " &2ha sido expulsado del clan &e&l" + clanName));

            countUsers.setString(1, clanName);
            ResultSet count = countUsers.executeQuery();
            if (count.next() && count.getInt("total") == 0) {
                deleteClan.setString(1, clanName);
                deleteClan.executeUpdate();
                sender.sendMessage(MSG.color(prefix + "&2El clan está vacío. Ha sido eliminado."));
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
            sender.sendMessage(MSG.color("&8&l» &e&lEstadísticas: &b" + clanName + "&8&l«"));
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


    private void Economy(Player player, String clan, String[] args) {
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
                        player.sendMessage(MSG.color(prefix + "&2Depositaste &a$" + amount + " &2al clan."));
                    } else {
                        econ.deposit(player, amount);
                        player.sendMessage(MSG.color(prefix + "&2Retiraste &a$" + amount + " &2del clan."));
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
            sender.sendMessage(MSG.color(prefix + "&2Invitación enviada a: &e" + playerToInvite));
            assert p != null;
            p.sendMessage(MSG.color(prefix + "&2Has sido invitado al clan: &e" + playerClan));
            p.sendMessage(MSG.color("&8USO: /cls join " + playerClan + " - para unirte al clan"));
        } else {
            sender.sendMessage(MSG.color(prefix + "&c Este jugador no está en línea."));
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
                sender.sendMessage(MSG.color(prefix + "&2Has salido del clan."));
            }

        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage(MSG.color(prefix + "&c Error al salir del clan."));
        }
    }


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
            PreparedStatement inviteCheck = con.prepareStatement("SELECT * FROM clan_invites WHERE username=? AND clan=?");
            PreparedStatement addUser = con.prepareStatement("INSERT INTO clan_users (username, clan) VALUES (?, ?)");
            PreparedStatement deleteInvite = con.prepareStatement("DELETE FROM clan_invites WHERE username=? AND clan=?")) {

            clanCheck.setString(1, clanToJoin);
            ResultSet clanRs = clanCheck.executeQuery();

            if (!clanRs.next()) {
                sender.sendMessage(MSG.color(prefix + "&c El clan no existe."));
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
                sender.sendMessage(MSG.color(prefix + "&c Este clan es &lPrivado&c."));
                return;
            }

            addUser.setString(1, playerName);
            addUser.setString(2, clanToJoin);
            addUser.executeUpdate();

            // Opcional: limpiar invitación si existía
            deleteInvite.setString(1, playerName);
            deleteInvite.setString(2, clanToJoin);
            deleteInvite.executeUpdate();

            // Registrar en historial (si mantenés esto)
            PECMD.addClanToHistory(player, clanToJoin);

            sender.sendMessage(MSG.color(prefix + "&2Te has unido al clan: &e" + clanToJoin));

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
            PreparedStatement ps = con.prepareStatement("SELECT name FROM clans");
            ResultSet rs = ps.executeQuery()) {

            if (!rs.isBeforeFirst()) {
                sender.sendMessage(MSG.color(prefix + "&c No hay clanes en el servidor."));
                return;
            }

            StringBuilder clansList = new StringBuilder();
            clansList.append(MSG.color(prefix + "&2&lClanes:\n"));
            while (rs.next()) {
                clansList.append(MSG.color("&c- ")).append(rs.getString("name")).append("\n");
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

            sender.sendMessage(MSG.color(prefix + "&2Clan reportado: &e" + reportedClan + "&2. Razón: " + reason));

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
                try (PreparedStatement ps = con.prepareStatement("UPDATE clans SET name=? WHERE name=?")) {
                    ps.setString(1, value);
                    ps.setString(2, clanName);
                    ps.executeUpdate();
                    player.sendMessage(MSG.color(prefix + "&3Nombre del clan cambiado a: &f" + value));
                }
            } else if (type.equalsIgnoreCase("privacy")) {
                try (PreparedStatement ps = con.prepareStatement("UPDATE clans SET privacy=? WHERE name=?")) {
                    ps.setString(1, value);
                    ps.setString(2, clanName);
                    ps.executeUpdate();
                    player.sendMessage(MSG.color(prefix + "&3Privacidad del clan cambiada a: &f" + value));
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

                try (PreparedStatement deleteUsers = con.prepareStatement("DELETE FROM clan_users WHERE clan=?");
                    PreparedStatement deleteClan = con.prepareStatement("DELETE FROM clans WHERE name=?")) {

                    deleteUsers.setString(1, playerClan);
                    deleteUsers.executeUpdate();

                    deleteClan.setString(1, playerClan);
                    deleteClan.executeUpdate();
                }

                boolean econEnabled = plugin.getFH().getConfig().getBoolean("economy.enabled");
                int deleteGain = plugin.getFH().getConfig().getInt("economy.earn.delete-clan", 0);
                if (econEnabled) econ.deposit(player, deleteGain);

                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (econEnabled) {
                        sender.sendMessage(MSG.color(prefix + "&2El clan fue eliminado. Ganaste: &e$" + deleteGain));
                    } else {
                        sender.sendMessage(MSG.color(prefix + "&2El clan fue eliminado."));
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
            sender.sendMessage(MSG.color(prefix + "&c &lUSO:&f /cls create <nombre>"));
            return;
        }

        String clanName = args[1].toLowerCase();
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
                            sender.sendMessage(MSG.color("&cNo tienes suficiente dinero. Necesitas: &2&l$" + cost))
                        );
                        return;
                    }
                    econ.withdraw(player, cost);
                }

                try (PreparedStatement insertClan = con.prepareStatement("INSERT INTO clans (name, founder, leader, money, privacy) VALUES (?, ?, ?, 0, 'Public')");
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
                    player.sendMessage(MSG.color(prefix + "&2Tu clan &e" + clanName + " &2ha sido creado."));
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

            sender.sendMessage(MSG.color(prefix + "&aEl fuego amigo ahora está: &e" + (enabled ? "ACTIVADO" : "DESACTIVADO")));

        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage(MSG.color(prefix + "&c Error al actualizar la configuración de fuego amigo."));
        }
    }

    private void handleAllyCommand(CommandSender sender, String playerName, String playerClan, String[] args) {
        if (playerClan == null || playerClan.isEmpty()) {
            sender.sendMessage(MSG.color(prefix + "&c No perteneces a ningún clan."));
            return;
        }

        if (args.length != 2) {
            sender.sendMessage(MSG.color(prefix + "&c USO: /cls ally <nombreClan>"));
            return;
        }

        String targetClan = args[1];

        if (targetClan.equalsIgnoreCase(playerClan)) {
            sender.sendMessage(MSG.color(prefix + "&c No puedes aliarte con tu propio clan."));
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
                sender.sendMessage(MSG.color(prefix + "&c El clan &e" + targetClan + " &cno existe."));
                return;
            }

            insert.setString(1, playerClan);
            insert.setString(2, targetClan);
            insert.setString(3, targetClan);
            insert.setString(4, playerClan);
            insert.executeUpdate();

            sender.sendMessage(MSG.color(prefix + "&aAlianza formada con &e" + targetClan));

        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage(MSG.color(prefix + "&c Error al crear la alianza."));
        }
    }




    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            return args.length == 1 ? List.of("reload") : new ArrayList<>();
        }

        // USAR CACHE EN VEZ DE getPlayerClan (que hace SQL)
        String playerClan = SatipoClan.getInstance().getMariaDBManager().getCachedPlayerClan(player.getName());
        List<String> completions = new ArrayList<>();

        switch (args.length) {
            case 1 -> completions.addAll(List.of(
                    "create", "disband", "report", "list", "join",
                    "kick", "invite", "chat", "leave", "stats", "resign", "edit", "economy", "ally", "ff"
            ));

            case 2 -> {
                String arg = args[0].toLowerCase();
                switch (arg) {
                    case "join" -> {
                        if (isNotInClan(playerClan)) completions.addAll(SatipoClan.getInstance().getMariaDBManager().getCachedClanNames());
                    }
                    case "invite", "kick" -> {
                        if (isInClan(playerClan) && isLeader(player, playerClan)) completions.addAll(getOnlinePlayerNames());
                    }
                    case "economy" -> completions.addAll(List.of("depositar", "retirar"));
                    case "report" -> completions.addAll(SatipoClan.getInstance().getMariaDBManager().getCachedClanNames());
                    case "edit" -> {
                        if (isInClan(playerClan) && isLeader(player, playerClan)) {
                            completions.addAll(List.of("name", "privacy"));
                        }
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