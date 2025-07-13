package me.lewisainsworth.satipoclans.Utils;

import me.lewisainsworth.satipoclans.SatipoClan;
import me.lewisainsworth.satipoclans.Utils.LangManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.bukkit.ChatColor;


import java.util.List;
import java.util.regex.Pattern;

import me.lewisainsworth.satipoclans.Utils.MSG;


public class ClanNameHandler {

    private static final int MAX_VISIBLE_LENGTH = 16;
    private static final Pattern HEX_PATTERN = Pattern.compile("&#[a-fA-F0-9]{6}");
    private static final Pattern FORMAT_CODES = Pattern.compile("&[0-9a-fl-or]");

    public static String getVisibleName(String raw) {
        if (raw == null) return "";
        String noHex = HEX_PATTERN.matcher(raw).replaceAll("");
        return FORMAT_CODES.matcher(noHex).replaceAll("");
    }

    public static boolean isValid(String raw) {
        return getVisibleName(raw).length() <= MAX_VISIBLE_LENGTH;
    }

    public static void insertClan(SatipoClan plugin, String rawName, String founder, String leader) throws SQLException {
        LangManager lang = plugin.getLangManager();

        String visibleName = getVisibleName(rawName);

        if (!isValid(rawName)) {
            throw new IllegalArgumentException(
                MSG.color(lang.getMessageWithPrefix("user.create_name_too_long")
                    .replace("{max}", String.valueOf(MAX_VISIBLE_LENGTH)))
            );
        }

        List<String> blocked = plugin.getFH().getConfig().getStringList("names-blocked.blocked");
        if (blocked.stream().anyMatch(b -> visibleName.equalsIgnoreCase(b))) {
            throw new IllegalArgumentException(
                MSG.color(lang.getMessageWithPrefix("user.create_name_blocked"))
            );
        }

        String colored = MSG.color(rawName);

        try (Connection con = plugin.getMariaDBManager().getConnection();
            PreparedStatement stmt = con.prepareStatement(
                "INSERT INTO clans (name, name_colored, founder, leader, money, privacy) VALUES (?, ?, ?, ?, 0, 'private')"
            )) {
            stmt.setString(1, visibleName);  // ❌ NO usar colored
            stmt.setString(2, colored);
            stmt.setString(3, founder);
            stmt.setString(4, leader);
            stmt.executeUpdate();
        }
    }

    public static void updateClanName(SatipoClan plugin, String oldName, String newRawName) throws SQLException {
        LangManager lang = plugin.getLangManager();

        String newVisible = ChatColor.stripColor(MSG.color(newRawName)); // quitar color
        String newColored = MSG.color(newRawName); // aplicar color

        // Validar longitud visible
        if (!isValid(newVisible)) {
            throw new IllegalArgumentException(
                MSG.color(lang.getMessageWithPrefix("user.edit_name_too_long")
                    .replace("{max}", String.valueOf(MAX_VISIBLE_LENGTH)))
            );
        }

        // Validar nombres bloqueados
        List<String> blocked = plugin.getFH().getConfig().getStringList("names-blocked.blocked");
        if (blocked.stream().anyMatch(b -> newVisible.equalsIgnoreCase(b))) {
            throw new IllegalArgumentException(
                MSG.color(lang.getMessageWithPrefix("user.edit_name_blocked"))
            );
        }

        // Verificar que no exista un clan con ese nombre
        if (plugin.getMariaDBManager().clanExists(newVisible)) {
            throw new IllegalArgumentException(
                MSG.color(lang.getMessageWithPrefix("user.edit_name_exists"))
            );
        }

        try (Connection con = plugin.getMariaDBManager().getConnection()) {
            con.setAutoCommit(false);

            try (
                PreparedStatement updateClans = con.prepareStatement(
                    "UPDATE clans SET name=?, name_colored=? WHERE name=?");
                PreparedStatement updateUsers = con.prepareStatement(
                    "UPDATE clan_users SET clan=? WHERE clan=?");
                PreparedStatement updateFF = con.prepareStatement(
                    "UPDATE friendlyfire SET clan=? WHERE clan=?");
                PreparedStatement updateInvites = con.prepareStatement(
                    "UPDATE clan_invites SET clan=? WHERE clan=?");
                PreparedStatement updateReports = con.prepareStatement(
                    "UPDATE reports SET clan=? WHERE clan=?");
                PreparedStatement updateAlliances1 = con.prepareStatement(
                    "UPDATE alliances SET clan1=? WHERE clan1=?");
                PreparedStatement updateAlliances2 = con.prepareStatement(
                    "UPDATE alliances SET clan2=? WHERE clan2=?");
                PreparedStatement updatePending1 = con.prepareStatement(
                    "UPDATE pending_alliances SET requester=? WHERE requester=?");
                PreparedStatement updatePending2 = con.prepareStatement(
                    "UPDATE pending_alliances SET target=? WHERE target=?");
                PreparedStatement updateHistory = con.prepareStatement(
                    "UPDATE player_clan_history SET current_clan=? WHERE current_clan=?")
            ) {

                updateClans.setString(1, newVisible);
                updateClans.setString(2, newColored);
                updateClans.setString(3, oldName);
                updateClans.executeUpdate();

                PreparedStatement[] updates = {
                    updateUsers, updateFF, updateInvites, updateReports,
                    updateAlliances1, updateAlliances2, updatePending1, updatePending2, updateHistory
                };

                for (PreparedStatement ps : updates) {
                    ps.setString(1, newVisible);
                    ps.setString(2, oldName);
                    ps.executeUpdate();
                }

                con.commit();
            } catch (SQLException e) {
                con.rollback();
                e.printStackTrace();
                throw new IllegalArgumentException(MSG.color("&cError al actualizar el nombre del clan."));
            } finally {
                con.setAutoCommit(true);
            }
        }
    }
}
