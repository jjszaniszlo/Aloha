package org.jjstudio.chatplugin;

import com.moandjiezana.toml.Toml;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public record PluginConfig(@NonNull String discordWebHook,
                           @NonNull String discordBotPublicToken,
                           @NonNull String discordChannelID,
                           @NonNull String globalChatFormat,
                           @NonNull String discordChatFormat,
                           @NonNull Map<String, String> serverNameAliases,
                           @NonNull Map<String, String> serverNameColors) {
    public static PluginConfig loadConfig(@DataDirectory Path dataDirectory) {
        final var dirFile = dataDirectory.toFile();
        if (!dirFile.exists()) {
            final var created = dirFile.mkdir();
            if (!created) {
                return null;
            }
        }

        final var configFile = new File(dirFile, "ChatPlugin.toml");
        if (!configFile.exists()) {
            try {
                InputStream fileStream = PluginConfig.class.getResourceAsStream("/config.toml");
                if (fileStream == null) {
                    throw new NullPointerException("Input stream is null!");
                }
                Files.copy(fileStream, configFile.toPath());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        final var toml = new Toml().read(configFile);

        final var discordTable = toml.getTable("discord");
        final var discordWebHook = discordTable.getString("webhook");
        final var discordBotPublicToken = discordTable.getString("public_token");
        final var discordChannelID = discordTable.getString("channel_id");

        final var chatTable = toml.getTable("chat");
        final var globalFormat = chatTable.getString("global_format");
        final var discordFormat = chatTable.getString("discord_format");

        final var serversTable = toml.getTable("servers");
        final var aliasesMap = serversTable.getTable("aliases").toMap();
        final var colorsMap = serversTable.getTable("colors").toMap();

        final var serverAliases = new HashMap<String, String>();
        final var serverColors = new HashMap<String, String>();

        for (String key : aliasesMap.keySet()) {
            serverAliases.put(key, String.valueOf(aliasesMap.get(key)));
        }

        for (String key : colorsMap.keySet()) {
            serverColors.put(key, String.valueOf(colorsMap.get(key)));
        }

        return new PluginConfig(discordWebHook, discordBotPublicToken, discordChannelID, globalFormat, discordFormat, serverAliases, serverColors);
    }
}
