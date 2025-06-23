package me.lewisainsworth.satipoclans;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import me.lewisainsworth.satipoclans.CMDs.CCMD;
import me.lewisainsworth.satipoclans.CMDs.ACMD;
import me.lewisainsworth.satipoclans.CMDs.PECMD;
import me.lewisainsworth.satipoclans.CMDs.LangCMD;
import me.lewisainsworth.satipoclans.Events.Events;
import me.lewisainsworth.satipoclans.Utils.*;
import me.lewisainsworth.satipoclans.Database.MariaDBManager;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;

import java.io.File;
import java.util.Objects;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.HashMap;
import java.util.List;



public class SatipoClan extends JavaPlugin {
   public String version = getDescription().getVersion();
   public static String prefix;
   public static Econo econ;

   private Updater updater;
   private Metrics metrics;
   private FileHandler fh;
   private MariaDBManager mariaDBManager;
   private LangManager langManager;
   private LangCMD langCMD;

   private static SatipoClan instance;

   @Override
   public void onEnable() {
      instance = this;
      saveDefaultConfig();
      applyCommandAliases();
      prefix = getConfig().getString("prefix", "&7 [&a&lꜱᴀᴛɪᴘᴏ&6&lᴄʟᴀɴꜱ&7]&n");
      fh = new FileHandler(this);
      updater = new Updater(this, 114316);
      metrics = new Metrics(this, 20912);
      econ = new Econo(this);
      ClanUtils.init(this);
      copyLangFiles();
      langManager = new LangManager(this);
      getCommand("clans").setExecutor(new CCMD(this, langManager));
      LangCMD langCMD = new LangCMD(this);
      setLangCMD(langCMD);
      
      

      if (getConfig().getBoolean("economy.enabled", true)) {
         if (!econ.setupEconomy()) {
            getLogger().severe("Can´t load the economy system.");
            fh.getConfig().set("economy.enabled", false);
            fh.saveConfig();
            getLogger().severe("Economy system disabled.");
            return;
         }
      }

      fh.saveDefaults();

      try {
         mariaDBManager = new MariaDBManager(getConfig());
         mariaDBManager.setupTables();
         mariaDBManager.syncFromYaml(fh.getData());
         mariaDBManager.clearYamlClans(fh.getData(), fh);
         getLogger().info("Clans data successfully synced to MariaDB.");
      } catch (Exception e) {
         getLogger().severe("Failed to connect to MariaDB or sync data: " + e.getMessage());
         e.printStackTrace();
         getServer().getPluginManager().disablePlugin(this);
         return;
      }

      setupMetrics();
      registerCommands();
      registerEvents();
      searchUpdates();

      if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
         new PAPI(this).registerPlaceholders();
         getLogger().info("Placeholders de SatipoClans registrados correctamente.");
      }

      Bukkit.getConsoleSender().sendMessage(MSG.color("&av" + getDescription().getVersion() + " &2Enabled!"));
   }

   @Override
   public void onDisable() {
      if (mariaDBManager != null) {
         mariaDBManager.close();
      }
      Bukkit.getConsoleSender().sendMessage(MSG.color("&av" + getDescription().getVersion() + " &cDisabled"));
   }

   public static SatipoClan getInstance() {
      return instance;
   }

   private void applyCommandAliases() {
      FileConfiguration config = getConfig();
      Map<String, List<String>> aliasMap = new HashMap<>();

      // Cargar aliases del config
      ConfigurationSection section = config.getConfigurationSection("command-aliases");
      if (section != null) {
         for (String cmd : section.getKeys(false)) {
               List<String> aliases = section.getStringList(cmd);
               aliasMap.put(cmd, aliases);
         }
      }

      // Aplicar aliases a cada comando registrado
      aliasMap.forEach((cmdName, aliases) -> {
         PluginCommand command = getCommand(cmdName);
         if (command != null) {
               command.setAliases(aliases);
         } else {
               getLogger().warning("No se encontró el comando '" + cmdName + "' para aplicar aliases.");
         }
      });
   }

   private void copyLangFiles() {
      File langFolder = new File(getDataFolder(), "lang");
      if (!langFolder.exists()) {
         langFolder.mkdirs();
      }

      String[] languages = {"es.yml", "en.yml"};  // pon aquí todos los idiomas que tengas

      for (String langFile : languages) {
         File file = new File(langFolder, langFile);
         if (!file.exists()) {
               saveResource("lang/" + langFile, false);
         }
      }
   }


   private void setupMetrics() {
      int max = getConfig().getInt("max-clans", -1);
      String maxClans = (max <= 0) ? "Unlimited" : String.valueOf(max);

      metrics.addCustomChart(new Metrics.SimplePie("economy_enabled",
              () -> String.valueOf(getConfig().getBoolean("economy.enabled", true))));
      metrics.addCustomChart(new Metrics.SimplePie("economy_system",
              () -> getConfig().getString("economy.system", "Unknown")));
      metrics.addCustomChart(new Metrics.SimplePie("max_clans",
              () -> maxClans));
   }

   private void registerCommands() {
      Objects.requireNonNull(getCommand("clansadmin")).setExecutor(new ACMD(this));
      Objects.requireNonNull(getCommand("clans")).setExecutor(new CCMD(this, langManager));
      Objects.requireNonNull(getCommand("scstats")).setExecutor(new PECMD(this));
   }

   private void registerEvents() {
      getServer().getPluginManager().registerEvents(new Events(this), this);
   }

   public void searchUpdates() {
      String downloadUrl = "https://www.spigotmc.org/resources/satipoclans.126207/";
      TextComponent link = new TextComponent(MSG.color("&6&lClick here to download the update!"));
      link.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, downloadUrl));

      boolean updateAvailable = false;
      String latestVersion = "unknown";

      try {
         updater = new Updater(this, 126207);
         updateAvailable = updater.isUpdateAvailable();
         latestVersion = updater.getLatestVersion();
      } catch (Exception e) {
         Bukkit.getConsoleSender().sendMessage(MSG.color("&cError checking for updates: " + e.getMessage()));
      }

      if (updateAvailable) {
          Bukkit.getConsoleSender().sendMessage(MSG.color("&2&l============================================================"));
          Bukkit.getConsoleSender().sendMessage(MSG.color("&6&l         ＮＥＷ  ＶＥＲＳＩＯＮ  ＡＶＡＩＬＡＢＬＥ!"));
          Bukkit.getConsoleSender().sendMessage(MSG.color("&7"));
          Bukkit.getConsoleSender().sendMessage(MSG.color("&e&lPlugin: &fSatipoClans"));
          Bukkit.getConsoleSender().sendMessage(MSG.color("&e&lCurrent Version: &f" + version));
          Bukkit.getConsoleSender().sendMessage(MSG.color("&e&lLatest Version: &f" + latestVersion));
          Bukkit.getConsoleSender().sendMessage(MSG.color("&e&lDownload: &b" + downloadUrl));
          Bukkit.getConsoleSender().sendMessage(MSG.color("&7"));
          Bukkit.getConsoleSender().sendMessage(MSG.color("&a&lChangelog &7(see plugin page for details)"));
          Bukkit.getConsoleSender().sendMessage(MSG.color("&7- Bug fixes and improvements"));
          Bukkit.getConsoleSender().sendMessage(MSG.color("&7- New features may be available!"));
          Bukkit.getConsoleSender().sendMessage(MSG.color("&7"));
          Bukkit.getConsoleSender().sendMessage(MSG.color("&c&lPlease update to enjoy the latest features and fixes!"));
          Bukkit.getConsoleSender().sendMessage(MSG.color("&2&l============================================================"));

         for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("satipoclans.admin")) {
                  player.sendMessage(MSG.color(prefix + "&e A new plugin update is available!"));
               player.spigot().sendMessage(link);
            }
         }
      }
   }

   public boolean isClanBanned(String clanName) {
      try (Connection con = getMariaDBManager().getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT name FROM banned_clans WHERE name = ?")) {
         ps.setString(1, clanName);
         ResultSet rs = ps.executeQuery();
         return rs.next();
      } catch (SQLException e) {
         e.printStackTrace();
         return false; // Por seguridad, podrías devolver true si quieres evitar riesgos
      }
   }


   public static Econo getEcon() {
      return econ;
   }

   public FileHandler getFH() {
      return fh;
   }

   public MariaDBManager getMariaDBManager() {
      return mariaDBManager;
   }

   public LangManager getLangManager() {
      return langManager;
   }

   public LangCMD getLangCMD() {
      return langCMD;
   }

   public void setLangCMD(LangCMD langCMD) {
      this.langCMD = langCMD;
   }
}
