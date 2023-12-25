package com.github.nathakusuma.cubicicediscord;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.bukkit.plugin.java.JavaPlugin;

public final class CubicIceDiscord extends JavaPlugin {
    private static JDA bot = null;

    public static JDA getBot() {
        return bot;
    }
    private void startBot() {
        JDABuilder jdaBuilder = JDABuilder.createDefault(getConfig().getString("BotToken"));
        jdaBuilder.enableIntents(
                GatewayIntent.MESSAGE_CONTENT,
                GatewayIntent.GUILD_MEMBERS,
                GatewayIntent.GUILD_PRESENCES
        );
        bot = jdaBuilder.build();
        try {
            bot.awaitReady();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    @Override
    public void onEnable() {
        saveDefaultConfig();
        startBot();
    }

    @Override
    public void onDisable() {
        bot.shutdownNow();
    }
}
