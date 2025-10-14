package me.diamond;

import me.diamond.credentials.OfflineCredentials;
import me.diamond.event.PlayerMoveEvent;

import java.net.InetSocketAddress;

public class Main {

    public static void main(String[] args) {
        //Example code
        MinecraftBot bot = BotFactory.createBot(new OfflineCredentials("Bot"), new InetSocketAddress("localhost", 25565));
        bot.setAutoReconnect(true, 1000);
        bot.getEventManager().addListener(PlayerMoveEvent.class, event -> {
            bot.lookAtPlayer(event.getPlayer());
        });
    }
}
