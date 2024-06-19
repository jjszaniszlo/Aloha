package org.jjstudio.chatplugin;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.intent.Intent;
import org.javacord.api.entity.message.WebhookMessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.interaction.SlashCommand;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.slf4j.Logger;

import java.util.List;


public class DiscordBot {
    private static DiscordApi discordApi;
    private static TextChannel channel;

    public static void init(Logger logger) {
        discordApi = new DiscordApiBuilder()
                .setToken(ChatPlugin.getConfig().discordBotPublicToken())
                .setIntents(Intent.MESSAGE_CONTENT, Intent.DIRECT_MESSAGES, Intent.DIRECT_MESSAGE_REACTIONS, Intent.DIRECT_MESSAGE_TYPING, Intent.GUILD_MESSAGES, Intent.GUILDS, Intent.GUILD_MEMBERS)
                .login()
                .join();

        discordApi.addMessageCreateListener(event -> {
            if (event.getChannel().getId() != channel.getId()) return;
            if (!event.getMessageAuthor().isBotUser() && !event.getMessageAuthor().isWebhook()) {
                ChatPlugin.sendMessageToAllPlayers(ChatPlugin.parseMessage(ChatPlugin.getConfig().discordChatFormat(), List.of(
                        new ChatPlugin.ChatTag("server", "<bold><color:#7785CC>DISCORD<reset>", true),
                        new ChatPlugin.ChatTag("player", event.getMessageAuthor().getDisplayName(), false),
                        new ChatPlugin.ChatTag("message", event.getMessage().getContent(), false)
                )));

                logger.info("[DISCORD] <{}>: {}", event.getMessageAuthor().getDisplayName(), event.getMessage().getContent());
            }
        });

        discordApi.getTextChannels().stream()
                .filter(c -> c.getIdAsString().compareTo(ChatPlugin.getConfig().discordChannelID()) == 0)
                .findAny()
                .ifPresentOrElse(
                    e -> channel = e,
                    () -> logger.info(String.format("Could not find channel with id: %s!", ChatPlugin.getConfig().discordChannelID())));

        SlashCommand statusCommand = SlashCommand.with("status", "Query the status of both the Creative and Survival MC Server")
                .setDefaultEnabledForEveryone()
                .createGlobal(discordApi)
                .join();

        discordApi.addSlashCommandCreateListener(event -> {
            final var interaction = event.getSlashCommandInteraction();
            if (interaction.getFullCommandName().equals("status")) {
                final var messageBuilder = interaction.createImmediateResponder();
                final var embed = new EmbedBuilder()
                        .setAuthor("ServerStatus");

                final var playerCounts = ChatPlugin.getAllServersPlayerCount();
                playerCounts.forEach((key, value) -> embed.addField(String.format("Server: %s", key), String.format("Players Connected: %s", value)));

                messageBuilder.addEmbed(embed).respond();
            }
        });
    }

    public static void relayChatMessage(String username, String message) {
        assert channel != null;
        new WebhookMessageBuilder().setContent(message).setDisplayName(username).send(discordApi, ChatPlugin.getConfig().discordWebHook());
    }
}
