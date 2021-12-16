package com.github.nathakusuma.cubicicediscord;

import org.bukkit.ChatColor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MySQL {

    private static final CubicIceDiscord plugin = CubicIceDiscord.getPlugin(CubicIceDiscord.class);
    private static Connection connection;

    public static Connection getConnection() throws SQLException {
        if(connection == null || connection.isClosed()){
            String hostname = plugin.getConfig().getString("MySQL.Hostname");
            String port = plugin.getConfig().getString("MySQL.Port");
            String database = plugin.getConfig().getString("MySQL.Database");
            String username = plugin.getConfig().getString("MySQL.Username");
            String password = plugin.getConfig().getString("MySQL.Password");
            connection = DriverManager.getConnection("jdbc:mysql://"+hostname+":"+port+"/"+database, username, password);
            plugin.getLogger().info(ChatColor.GREEN + "Connected to MySQL Database.");
        }
        return connection;
    }


}
