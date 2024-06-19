package org.jjstudio.chatplugin;

import com.google.inject.Inject;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Plugin(
        id = "alohachatplugin",
        name = "AlohaChatPlugin",
        version = "0.0.1"
)
public class ChatPlugin {
    private static PluginConfig config;
    private static ProxyServer proxyServer;
    private static Path dataDirectory;
    private static Logger logger;

    private static Map<String, Integer> serverCapacities;
    private static Map<String, RegisteredServer> servers;

    @Inject
    public ChatPlugin(ProxyServer server, @DataDirectory Path dataDirectory, Logger logger) {
        ChatPlugin.proxyServer = server;
        ChatPlugin.dataDirectory = dataDirectory;
        ChatPlugin.logger = logger;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent proxyInitializeEvent) {
        ChatPlugin.init();
    }

    public static PluginConfig getConfig() {
        return config;
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onPlayerChat(PlayerChatEvent event) {
        final var server = event.getPlayer().getCurrentServer();
        if (server.isEmpty()) return;

        String serverName = server.get().getServerInfo().getName();
        String serverAlias = config.serverNameAliases().get(serverName);
        String serverColor = config.serverNameColors().get(serverName);

        assert serverAlias != null;
        assert serverColor != null;

        DiscordBot.relayChatMessage(String.format("[%s] %s", serverName.toUpperCase(), event.getPlayer().getUsername()), event.getMessage());

        final var msg = parseMessage(config.globalChatFormat(), List.of(
                new ChatTag("server", serverAlias, true),
                new ChatTag("player", event.getPlayer().getUsername(), false),
                new ChatTag("message", event.getMessage(), false)
        ));

        sendMessageExceptServer(msg, server.get().getServer());
        logger.info("[GLOBAL] <{}>: {}", event.getPlayer().getUsername(), event.getMessage());
    }

    private static void init() {
        config = PluginConfig.loadConfig(dataDirectory);

        servers = new HashMap<>();
        serverCapacities = new HashMap<>();
        for (RegisteredServer s : proxyServer.getAllServers()) {
            servers.put(s.getServerInfo().getName(), s);
            s.ping().thenAccept(ping -> {
                ping.getPlayers().ifPresent(players -> {
                    serverCapacities.put(s.getServerInfo().getName(), players.getMax());
                });
            });
        }


        DiscordBot.init(logger);
    }

    public static Component parseMessage(String chat_format, List<ChatTag> tags) {
        List<TagResolver.Single> list =new ArrayList<>();
        for (ChatTag tag : tags) {
            if (tag.parseValue) {
                list.add(Placeholder.parsed(tag.name, tag.value));
            } else {
                list.add(Placeholder.parsed(tag.name, Component.text(tag.value).content()));
            }
        }
        return MiniMessage.miniMessage().deserialize(chat_format, list.toArray(TagResolver[]::new));
    }

    public static void sendMessageToAllPlayers(Component msg) {
        for (Player player : proxyServer.getAllPlayers()) {
            player.sendMessage(msg);
        }
    }

    public static void sendMessageExceptServer(Component msg, RegisteredServer srv) {
        for (RegisteredServer s : servers.values()) {
            if (srv != s) {
                s.sendMessage(msg);
            }
        }
    }

    public static Map<String, String> getAllServersPlayerCount() {
        Map<String, String> playerCounts = new HashMap<>();
        for (RegisteredServer s : servers.values()) {
            int currentServerPlayerCount = s.getPlayersConnected().size();
            playerCounts.put(s.getServerInfo().getName(), String.format("%d/%d", currentServerPlayerCount, serverCapacities.get(s.getServerInfo().getName())));
        }
        return playerCounts;
    }

    public record ChatTag(@NonNull String name, @NonNull String value, boolean parseValue) {}
}
