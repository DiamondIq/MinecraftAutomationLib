package me.diamond.internal;

import me.diamond.MinecraftBot;
import me.diamond.container.Window;
import org.cloudburstmc.math.vector.Vector3d;
import org.geysermc.mcprotocollib.network.ClientSession;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class BotUpdater {
    public static void updateSession(MinecraftBot bot, ClientSession session) {
        MinecraftBot.Accessor.setSession(bot, session);
    }
    public static void setLocation(MinecraftBot bot, Vector3d newLoc) {
        MinecraftBot.Accessor.setLocation(bot, newLoc);
    }
    public static void setOpenedWindow(MinecraftBot bot, Window window) {
        MinecraftBot.Accessor.setOpenedWindow(bot, window);
    }
    public static void setYaw(MinecraftBot bot, float yaw) {
        MinecraftBot.Accessor.setYaw(bot, yaw);
    }
    public static void setPitch(MinecraftBot bot, float pitch) {
        MinecraftBot.Accessor.setPitch(bot, pitch);
    }
    public static void setEntityId(MinecraftBot bot, int entityId) {
        MinecraftBot.Accessor.setEntityId(bot, entityId);
    }

    public static void updateExecutor(MinecraftBot bot) {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, bot.getUsername() + "-game-thread");
            t.setDaemon(true);
            return t;
        });
        MinecraftBot.Accessor.setExecutor(bot, executor);
    }
}
