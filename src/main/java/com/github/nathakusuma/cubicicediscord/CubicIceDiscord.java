package com.github.nathakusuma.cubicicediscord;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Role;
import org.bukkit.plugin.java.JavaPlugin;

import javax.security.auth.login.LoginException;
import java.awt.*;
import java.util.Objects;

public final class CubicIceDiscord extends JavaPlugin {

    public static final Color themeColor = new Color(0x8cd7f8);
    public static JDA bot = null;
    public static Role staffRole = null;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        DataYML.saveDefaultData();
        startBot();
        registerCommands();
        registerEvents();
        upsertCommands();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    private void startBot() {
        JDABuilder jdaBuilder = JDABuilder.createDefault(getConfig().getString("BotToken"));
        jdaBuilder.addEventListeners(new StaffChat());
        jdaBuilder.addEventListeners(new Ticket());
        try {
            bot = jdaBuilder.build();
            bot.awaitReady();
        } catch (LoginException | InterruptedException e) {
            e.printStackTrace();
        }
        staffRole = bot.getRoleById(Objects.requireNonNull(getConfig().getString("StaffRoleID")));
    }

    private void registerCommands(){
        Objects.requireNonNull(this.getServer().getPluginCommand("s")).setExecutor(new StaffChat());
    }

    private void registerEvents(){
        this.getServer().getPluginManager().registerEvents(new StaffChat(), this);
    }

    private void upsertCommands(){
        Ticket.upsertCommand();
    }

}
