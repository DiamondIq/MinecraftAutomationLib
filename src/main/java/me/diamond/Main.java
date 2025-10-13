package me.diamond;

import me.diamond.credentials.OfflineCredentials;
import me.diamond.event.LogInEvent;

import java.net.InetSocketAddress;

public class Main {

    public static void main(String[] args) {
        MinecraftBot bot = BotFactory.createBot(new OfflineCredentials("Bot"), new InetSocketAddress("localhost", 25565));
        bot.setAutoReconnect(true, 1000);
        bot.getEventManager().addListener(LogInEvent.class,event -> {
            bot.sendChatMessage("Hello World!");
        });
    }
}
