package me.lewisainsworth.satipoclans.Utils;

import me.lewisainsworth.satipoclans.SatipoClan;

import java.sql.*;

public class ClanUtils {

    private static SatipoClan plugin;

    public static void init(SatipoClan instance) {
        plugin = instance;
    }

    public static boolean isFriendlyFireEnabledAllies(String clan) {
        boolean enabled = false;
        String sql = "SELECT enabled FROM friendlyfire_allies WHERE clan = ?";
        try (Connection con = SatipoClan.getInstance().getMariaDBManager().getConnection();
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

    public static boolean areClansAllied(String clan1, String clan2) {
        if (clan1.equalsIgnoreCase(clan2)) return true;

        String sql = "SELECT 1 FROM alliances WHERE (clan1 = ? AND clan2 = ?) OR (clan1 = ? AND clan2 = ?)";
        try (Connection con = SatipoClan.getInstance().getMariaDBManager().getConnection();
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
}
