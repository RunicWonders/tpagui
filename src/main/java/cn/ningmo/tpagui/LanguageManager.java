package cn.ningmo.tpagui;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class LanguageManager {
    private static LanguageManager instance;
    private final TpaGui plugin;
    private final Map<String, FileConfiguration> languageFiles;
    private String currentLanguage;

    public LanguageManager(TpaGui plugin) {
        this.plugin = plugin;
        this.languageFiles = new HashMap<>();
        this.currentLanguage = plugin.getConfig().getString("language", "zh_CN");
        loadLanguages();
    }

    public static LanguageManager getInstance() {
        return instance;
    }

    private void loadLanguages() {
        instance = this;
        
        String[] languages = {"zh_CN", "zh_TW", "en_US"};
        for (String lang : languages) {
            saveDefaultLanguageFile(lang);
            loadLanguageFile(lang);
        }
    }

    private void saveDefaultLanguageFile(String language) {
        File langFolder = new File(plugin.getDataFolder(), "lang");
        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }

        File langFile = new File(langFolder, language + ".yml");
        if (!langFile.exists()) {
            plugin.saveResource("lang/" + language + ".yml", false);
        }
    }

    private void loadLanguageFile(String language) {
        File langFolder = new File(plugin.getDataFolder(), "lang");
        File langFile = new File(langFolder, language + ".yml");

        if (langFile.exists()) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(langFile);
            
            InputStream defaultStream = plugin.getResource("lang/" + language + ".yml");
            if (defaultStream != null) {
                YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8)
                );
                config.setDefaults(defaultConfig);
            }
            
            languageFiles.put(language, config);
        }
    }

    public void setLanguage(String language) {
        if (languageFiles.containsKey(language)) {
            this.currentLanguage = language;
            plugin.getConfig().set("language", language);
            plugin.saveConfig();
        }
    }

    public String getLanguage() {
        return currentLanguage;
    }

    public String getMessage(String path) {
        return getMessage(path, currentLanguage);
    }

    public String getMessage(String path, String language) {
        FileConfiguration langConfig = languageFiles.get(language);
        if (langConfig == null) {
            langConfig = languageFiles.get("zh_CN");
        }
        
        String message = langConfig.getString(path);
        if (message == null) {
            return path;
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public String getMessage(String path, String... placeholders) {
        String message = getMessage(path);
        if (message.equals(path)) return path;
        return replacePlaceholders(message, placeholders);
    }

    public String getLogMessage(String path, String... placeholders) {
        FileConfiguration langConfig = languageFiles.get(currentLanguage);
        if (langConfig == null) {
            langConfig = languageFiles.get("zh_CN");
        }
        
        String message = langConfig.getString("log." + path);
        if (message == null) {
            message = langConfig.getString(path);
        }
        if (message == null) {
            return path;
        }
        return replacePlaceholders(message, placeholders);
    }

    private String replacePlaceholders(String message, String... placeholders) {
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                message = message.replace(placeholders[i], placeholders[i + 1]);
            }
        }
        return message;
    }

    public FileConfiguration getLanguageConfig(String language) {
        return languageFiles.get(language);
    }

    public void reload() {
        languageFiles.clear();
        loadLanguages();
    }
}
