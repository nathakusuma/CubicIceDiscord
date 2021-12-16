package com.github.nathakusuma.cubicicediscord;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.Button;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

public class Ticket extends ListenerAdapter {

    private final CubicIceDiscord plugin = CubicIceDiscord.getPlugin(CubicIceDiscord.class);

    MessageEmbed wrongChannel = new EmbedBuilder()
            .setColor(Color.RED)
            .setTitle("Channel Salah")
            .setDescription("Kamu hanya bisa menggunakan command itu di dalam channel tiketmu.")
            .build();

    public static void upsertCommand() {
        Guild guild = CubicIceDiscord.bot.getGuildById(437845027150888960L);
        assert guild != null;
        guild.upsertCommand(
                new CommandData("ticket", " ").addSubcommands(
                        new SubcommandData("close", "Menutup Tiket"),
                        new SubcommandData("addguest", "Menambahkan user lain sebagai tamu di dalam tiketmu")
                                .addOption(OptionType.USER, "guest", "User yang ingin ditambahkan", true),
                        new SubcommandData("removeguest", "Mengeluarkan tamu dari tiketmu")
                                .addOption(OptionType.USER, "guest", "User yang ingin dikeluarkan", true)
                )
        ).queue();
    }

    private boolean isCreator(long userID, long channelID) {
        try {
            PreparedStatement ps = MySQL.getConnection().prepareStatement("SELECT * FROM cubicdiscord_ticket WHERE channel_id=?");
            ps.setLong(1, channelID);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getLong("creator_id") == userID;
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return false;
    }

    private boolean isStaff(Member member, TextChannel channel){
        if(channel.getParentCategoryIdLong() != plugin.getConfig().getLong("TicketCategoryID")) return false;
        return member.getRoles().contains(CubicIceDiscord.staffRole);
    }

    private boolean isCreatorOrStaff(Member member, TextChannel channel){
        return (isCreator(member.getIdLong(), channel.getIdLong()) || isStaff(member, channel));
    }

    private void closeTicket(TextChannel ticketChannel) {
        try {
            PreparedStatement ps = MySQL.getConnection().prepareStatement("DELETE FROM cubicdiscord_ticket WHERE channel_id=?");
            ps.setLong(1, ticketChannel.getIdLong());
            ps.executeUpdate();
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        ticketChannel.delete().queue();
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if(event.getAuthor().isBot()) return;
        if(!Objects.requireNonNull(event.getMember()).getRoles().contains(CubicIceDiscord.staffRole)) return;
        if (!event.getMessage().getContentRaw().startsWith(".")) return;
        String command = event.getMessage().getContentRaw().replaceFirst(".", "");
        if (command.startsWith("ticket init")) {
            TextChannel textChannel = event.getJDA().getTextChannelById(DataYML.getData().getLong("TicketButton.ChannelID"));
            if (textChannel != null) {
                Message message = textChannel.retrieveMessageById(DataYML.getData().getLong("TicketButton.MessageID")).complete();
                if (message != null) message.delete().queue();
            }
            event.getChannel().sendMessageEmbeds(new EmbedBuilder().
                    setColor(CubicIceDiscord.themeColor)
                    .setTitle("Tiket")
                    .setThumbnail("https://i.imgur.com/EJbCMeL.png")
                    .setDescription("Tiket adalah media untuk berkomunikasi langsung dengan para " + CubicIceDiscord.staffRole.getAsMention() + ". Fitur ini biasanya digunakan untuk bertanya tentang donasi, melaporkan bug, melaporkan player, meminta refund, dan lain-lain.\n" +
                            "\u200B\n" +
                            "Dilarang menyalahgunakan tiket. Akan ada sanksi tegas untuk pengguna yang melakukannya.\n" +
                            "\u200B\n" +
                            "**Untuk membuat tiket, tekan tombol :ticket: di bawah dan ikuti petunjuk berikutnya.**\n" +
                            "\u200B\n" +
                            "Command Tiket:\n" +
                            "**/ticket close** - Menutup tiketmu\n" +
                            "**/ticket addguest @USER** - Menambahkan user lain sebagai tamu di dalam tiketmu\n" +
                            "**/ticket removeguest @USER** - Mengeluarkan tamu dari tiketmu")
                    .build()).setActionRow(Button.primary("ticket_create", Emoji.fromUnicode("U+1F3AB"))).queue(message -> {
                DataYML.getData().set("TicketButton.ChannelID", message.getChannel().getIdLong());
                DataYML.getData().set("TicketButton.MessageID", message.getIdLong());
                DataYML.saveData();
            });
            event.getMessage().delete().queue();
        } else if (command.startsWith("ticket resetuser")) {
            List<User> mentionedUsers = event.getMessage().getMentionedUsers();
            if(mentionedUsers.isEmpty()){
                event.getMessage().reply("```.ticket resetuser @USER```").mentionRepliedUser(false).queue();
            } else {
                User user = mentionedUsers.get(0);
                try{
                    PreparedStatement ps = MySQL.getConnection().prepareStatement("DELETE FROM cubicdiscord_ticket WHERE creator_id=?");
                    ps.setLong(1, user.getIdLong());
                    ps.executeUpdate();
                    event.getMessage().replyEmbeds(new EmbedBuilder()
                            .setColor(Color.GREEN)
                            .setTitle("Done")
                            .setDescription("Ticket ownership data of "+user.getAsMention()+" has been reset.")
                            .build()).queue();
                }catch (SQLException exception){
                    exception.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onButtonClick(@NotNull ButtonClickEvent event) {
        if (!event.getComponentId().equals("ticket_create")) return;
        boolean exists = true;
        long previousTicketID = 0;
        try {
            PreparedStatement ps = MySQL.getConnection().prepareStatement("SELECT * FROM cubicdiscord_ticket WHERE creator_id=?");
            ps.setLong(1, event.getUser().getIdLong());
            ResultSet rs = ps.executeQuery();
            exists = rs.next();
            if (exists) previousTicketID = rs.getLong("channel_id");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (exists) {
            event.replyEmbeds(new EmbedBuilder()
                    .setColor(Color.RED)
                    .setTitle("Kamu masih memiliki tiket terbuka")
                    .setDescription("Silahkan gunakan tiketmu sebelumnya (<#" + previousTicketID + ">).")
                    .build()
            ).setEphemeral(true).queue();
        } else {
            Category category = event.getJDA().getCategoryById(plugin.getConfig().getLong("TicketCategoryID"));
            assert category != null;
            category.createTextChannel("ticket-" + Objects.requireNonNull(event.getMember()).getEffectiveName())
                    .setTopic(event.getUser().getAsMention())
                    .addMemberPermissionOverride(event.getUser().getIdLong(), EnumSet.of(Permission.VIEW_CHANNEL), null)
                    .queue(ticketChannel -> {
                        try {
                            PreparedStatement ps = MySQL.getConnection().prepareStatement("INSERT INTO cubicdiscord_ticket (channel_id, creator_id) VALUES (?,?)");
                            ps.setLong(1, ticketChannel.getIdLong());
                            ps.setLong(2, event.getUser().getIdLong());
                            ps.executeUpdate();
                        } catch (SQLException exception) {
                            exception.printStackTrace();
                        }
                        event.replyEmbeds(new EmbedBuilder()
                                .setColor(Color.GREEN)
                                .setTitle("Tiket Dibuat")
                                .setDescription("Tiketmu berada di "+ticketChannel.getAsMention())
                                .build()).setEphemeral(true).queue();
                        ticketChannel.sendMessage(event.getUser().getAsMention()).setEmbeds(new EmbedBuilder()
                                .setColor(CubicIceDiscord.themeColor)
                                .setTitle("Hai!")
                                .setDescription("Silakan ketik pesan yang kamu ingin sampaikan ke "+CubicIceDiscord.staffRole.getAsMention()+" di sini.\n" +
                                        "Mohon sabar dan jangan spam.\n" +
                                        "Jika kamu tidak bermaksud untuk membuat tiket ini, silakan tutup tiketnya segera.\n" +
                                        "Kirim command **/ticket close** jika ingin menutup tiket")
                                .build()).queue(welcomeMessage -> Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                    if(ticketChannel.getLatestMessageIdLong() == welcomeMessage.getIdLong()){
                                        ticketChannel.sendMessageEmbeds(new EmbedBuilder()
                                                .setColor(Color.RED)
                                                .setTitle("Tiket Kosong")
                                                .setDescription("<a:loading:818738342426443787> Tiket akan ditutup dalam 10 detik")
                                                .build()).queue();
                                        Bukkit.getScheduler().runTaskLater(plugin, () -> closeTicket(ticketChannel), 200);
                                    }
                                }, 6000));
                    });
        }
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!event.getName().equals("ticket")) return;
        Member member = event.getMember();
        assert member != null;
        if (Objects.equals(event.getSubcommandName(), "close")) {
            if (isCreatorOrStaff(member, event.getTextChannel())) {
                event.replyEmbeds(new EmbedBuilder()
                        .setColor(Color.GREEN)
                        .setTitle("Menutup Tiket")
                        .setDescription("<a:loading:818738342426443787> Tiket akan ditutup dalam 10 detik")
                        .build()).queue();
                Bukkit.getScheduler().runTaskLater(plugin, () -> closeTicket(event.getTextChannel()), 200);
            } else {
                event.replyEmbeds(wrongChannel).queue();
            }
        } else if (Objects.equals(event.getSubcommandName(), "addguest")) {
            if(!isCreatorOrStaff(member, event.getTextChannel())){
                event.replyEmbeds(wrongChannel).queue();
            } else {
                Member target = Objects.requireNonNull(event.getOption("guest")).getAsMember();
                assert target != null;
                if(target.hasPermission(event.getTextChannel(), Permission.VIEW_CHANNEL)) {
                    event.replyEmbeds(new EmbedBuilder()
                            .setColor(Color.RED)
                            .setTitle("Gagal")
                            .setDescription(target.getAsMention()+" sudah berada di tiket ini.")
                            .build()).queue();
                } else {
                    event.getTextChannel().getManager().putMemberPermissionOverride(target.getIdLong(),
                            EnumSet.of(Permission.VIEW_CHANNEL), null).queue();
                    event.replyEmbeds(new EmbedBuilder()
                            .setColor(Color.GREEN)
                            .setTitle("Berhasil")
                            .setDescription(target.getAsMention()+" telah ditambahkan sebagai tamu di tiket ini.")
                            .build()).queue();
                }
            }
        } else if (Objects.equals(event.getSubcommandName(), "removeguest")) {
            if(!isCreatorOrStaff(member, event.getTextChannel()))
                event.replyEmbeds(wrongChannel).queue();
            else {
                Member target = Objects.requireNonNull(event.getOption("guest")).getAsMember();
                assert target != null;
                if(!target.hasPermission(event.getTextChannel(), Permission.VIEW_CHANNEL)) {
                    event.replyEmbeds(new EmbedBuilder()
                            .setColor(Color.RED)
                            .setTitle("Gagal")
                            .setDescription(target.getAsMention()+" bukan tamu di tiket ini.")
                            .build()).queue();
                } else {
                    event.getTextChannel().getManager().removePermissionOverride(target.getIdLong()).queue();
                    event.replyEmbeds(new EmbedBuilder()
                            .setColor(Color.GREEN)
                            .setTitle("Berhasil")
                            .setDescription(target.getAsMention()+" telah dikeluarkan dari tiket ini.")
                            .build()).queue();
                }
            }
        }
    }

}
