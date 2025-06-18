package me.lewisainsworth.satipoclans;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import me.lewisainsworth.satipoclans.CMDs.CCMD;
import me.lewisainsworth.satipoclans.CMDs.ACMD;
import me.lewisainsworth.satipoclans.CMDs.PECMD;
import me.lewisainsworth.satipoclans.Events.Events;
import me.lewisainsworth.satipoclans.Utils.*;
import me.lewisainsworth.satipoclans.Database.MariaDBManager;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.Objects;

public class SatipoClan extends JavaPlugin {
   public String version = getDescription().getVersion();
   public static String prefix;
   public static Econo econ;

   private Updater updater;
   private Metrics metrics;
   private FileHandler fh;
   private MariaDBManager mariaDBManager;

   @Override
   public void onEnable() {
      saveDefaultConfig();
      prefix = getConfig().getString("prefix", "&7[&a&lSatipo&6&lClans&7] ");
      fh = new FileHandler(this);
      updater = new Updater(this, 114316);
      metrics = new Metrics(this, 20912);
      econ = new Econo(this);

      // Configuración economía
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

      // Conexión y sincronización a MariaDB
      try {
         mariaDBManager = new MariaDBManager(getConfig());
         mariaDBManager.connect();
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
      Objects.requireNonNull(getCommand("clans")).setExecutor(new CCMD(this));
      Objects.requireNonNull(getCommand("scstats")).setExecutor(new PECMD(this));
   }

   private void registerEvents() {
      getServer().getPluginManager().registerEvents(new Events(this), this);
   }

   public void searchUpdates() {
      String downloadUrl = "https://www.spigotmc.org/resources/clansx-the-best-clan-system-1-8-1-21.114316/";
      TextComponent link = new TextComponent(MSG.color("&e&lClick here to download the update!"));
      link.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, downloadUrl));

      boolean updateAvailable = false;
      String latestVersion = "unknown";

      try {
         updater = new Updater(this, 114316);
         updateAvailable = updater.isUpdateAvailable();
         latestVersion = updater.getLatestVersion();
      } catch (Exception e) {
         Bukkit.getConsoleSender().sendMessage(MSG.color("&cError checking for updates: " + e.getMessage()));
      }

      if (updateAvailable) {
         Bukkit.getConsoleSender().sendMessage(MSG.color("&2&l============= " + prefix + "&2&l============="));
         Bukkit.getConsoleSender().sendMessage(MSG.color("&6&lNEW VERSION AVAILABLE!"));
         Bukkit.getConsoleSender().sendMessage(MSG.color("&e&lCurrent Version: &f" + version));
         Bukkit.getConsoleSender().sendMessage(MSG.color("&e&lLatest Version: &f" + latestVersion));
         Bukkit.getConsoleSender().sendMessage(MSG.color("&e&lDownload it here: &f" + downloadUrl));
         Bukkit.getConsoleSender().sendMessage(MSG.color("&2&l============= " + prefix + "&2&l============="));

         for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("sc.admin")) {
               player.sendMessage(MSG.color(prefix + "&e&lA new plugin update is available!"));
               player.spigot().sendMessage(link);
            }
         }
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
}
