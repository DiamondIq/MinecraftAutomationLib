package me.diamond;

import me.diamond.credentials.OfflineCredentials;
import me.diamond.event.LogInEvent;
import me.diamond.event.PlayerMoveEvent;
import me.diamond.internal.BotFactory;

import java.net.InetSocketAddress;

public class Main {

    public static void main(String[] args) {
        //Example code
        Bot bot = BotFactory.createBot(new OfflineCredentials("Bot"), new InetSocketAddress("localhost", 25565));
        bot.setAutoReconnect(true, 1000);
        bot.getEventManager().once(LogInEvent.class, event -> {
            bot.sendChatMessage("Hello World!");
        });
        bot.getEventManager().addListener(PlayerMoveEvent.class, event -> {
                bot.lookAtPlayer(event.getPlayer());
        });
    }
}
