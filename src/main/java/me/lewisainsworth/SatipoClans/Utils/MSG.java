package me.lewisainsworth.satipoclans.Utils;

import net.md_5.bungee.api.ChatColor;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MSG {

    // Regex para encontrar códigos hex (#AABBCC)
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    public static String color(String msg) {
        if (isPlaceholderAPIAvailable()) {
            msg = PlaceholderAPI.setPlaceholders(null, msg);
        }

        msg = translateHexColors(msg);

        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    public static String color(Player player, String msg) {
        if (isPlaceholderAPIAvailable()) {
            msg = PlaceholderAPI.setPlaceholders(player, msg);
        }

        msg = translateHexColors(msg);

        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    public static List<String> colorList(List<String> messages) {
        return messages.stream()
                .map(MSG::color)
                .collect(Collectors.toList());
    }

    private static boolean isPlaceholderAPIAvailable() {
        try {
            Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    // Método para reemplazar los hex color codes por códigos ChatColor
    private static String translateHexColors(String message) {
      Matcher matcher = HEX_PATTERN.matcher(message);
      StringBuffer buffer = new StringBuffer();

      while (matcher.find()) {
         String hexCode = matcher.group(1);
         String replacement = ChatColor.of("#" + hexCode).toString();
         matcher.appendReplacement(buffer, replacement);
      }
      matcher.appendTail(buffer);
      return buffer.toString();
   }
}
