package com.github.nathakusuma.cubicicediscord;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;

public class DataYML {

    private static final CubicIceDiscord plugin = CubicIceDiscord.getPlugin(CubicIceDiscord.class);
    private static FileConfiguration dataConfig = null;
    private static File configFile = null;

    public static void reloadConfig() {
        if (configFile == null) configFile = new File(plugin.getDataFolder(), "data.yml");
        dataConfig = YamlConfiguration.loadConfiguration(configFile);
        InputStream defaultStream = plugin.getResource("data.yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream));
            dataConfig.setDefaults(defaultConfig);
        }
    }

    public static FileConfiguration getData() {
        if (dataConfig == null) reloadConfig();
        return dataConfig;
    }

    public static void saveData() {
        if (dataConfig == null || configFile == null) return;
        try {
            getData().save(configFile);
        } catch (IOException ioException) {
            plugin.getLogger().log(Level.SEVERE, "Could not save data to " + configFile, ioException);
        }
    }

    public static void saveDefaultData() {
        if (configFile == null) configFile = new File(plugin.getDataFolder(), "data.yml");
        if (!configFile.exists()) plugin.saveResource("data.yml", false);
    }

}
