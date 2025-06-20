package me.lewisainsworth.satipoclans.Utils;

import me.lewisainsworth.satipoclans.SatipoClan;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class LangManager {

    private final SatipoClan plugin;
    private final Map<UUID, String> playerLanguages = new HashMap<>();
    private final Map<String, YamlConfiguration> loadedLangs = new HashMap<>();
    private final String defaultLang = "es";

    public LangManager(SatipoClan plugin) {
        this.plugin = plugin;
        loadLangs();
    }

    private void loadLangs() {
        File langFolder = new File(plugin.getDataFolder(), "lang");
        if (!langFolder.exists()) langFolder.mkdirs();

        for (File file : Objects.requireNonNull(langFolder.listFiles())) {
            if (file.getName().endsWith(".yml")) {
                String langName = file.getName().replace(".yml", "");
                loadedLangs.put(langName, YamlConfiguration.loadConfiguration(file));
            }
        }

        if (loadedLangs.isEmpty()) {
            plugin.getLogger().warning("No se encontraron archivos de idioma en /lang/");
        }
    }

    public String getMessage(UUID uuid, String path) {
        String lang = playerLanguages.getOrDefault(uuid, defaultLang);
        YamlConfiguration config = loadedLangs.getOrDefault(lang, loadedLangs.get(defaultLang));
        String prefix = plugin.getConfig().getString("prefix", "&7 [&a&lꜱᴀᴛɪᴘᴏ&6&lᴄʟᴀɴꜱ&7]&n");
        String msg = config.getString(path);

        if (msg == null) return prefix + " &c¡Mensaje no encontrado! [" + path + "]";
        return prefix + " " + msg;
    }

    public List<String> getMessageList(UUID uuid, String path) {
        String lang = playerLanguages.getOrDefault(uuid, defaultLang);
        YamlConfiguration config = loadedLangs.getOrDefault(lang, loadedLangs.get(defaultLang));
        List<String> list = config.getStringList(path);
        String prefix = plugin.getConfig().getString("prefix", "&7 [&a&lꜱᴀᴛɪᴘᴏ&6&lᴄʟᴀɴꜱ&7]&n");

        if (list.isEmpty()) {
            return List.of(prefix + " &c¡Mensaje de lista no encontrado! [" + path + "]");
        }

        return list.stream().map(line -> prefix + " " + line).toList();
    }

    public void setPlayerLang(UUID uuid, String lang) {
        playerLanguages.put(uuid, lang);
    }

    public String getPlayerLang(UUID uuid) {
        return playerLanguages.getOrDefault(uuid, defaultLang);
    }

    public Set<String> getAvailableLanguages() {
        return loadedLangs.keySet();
    }

    public boolean isValidLanguage(String lang) {
        return loadedLangs.containsKey(lang);
    }
}
