package me.diamond;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import me.diamond.container.Inventory;
import me.diamond.container.Window;
import me.diamond.credentials.Credentials;
import me.diamond.event.EventManager;
import me.diamond.event.PacketHandler;
import org.cloudburstmc.math.vector.Vector3d;
import org.geysermc.mcprotocollib.network.ClientSession;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatCommandPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosRotPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerRotPacket;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.BitSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Slf4j
@Getter
@Setter(AccessLevel.PRIVATE)
public class MinecraftBot {
    private ClientSession session;
    private final Credentials credentials;
    private final InetSocketAddress serverAddress;
    private ScheduledExecutorService executor;
    private boolean autoReconnect;
    @Setter(AccessLevel.NONE) //No setter
    private long autoReconnectDelay;
    private final Inventory inventory;
    private final EventManager eventManager;
    private final PlayerTracker playerTracker;
    private final String username;
    private Window openedWindow;
    private Vector3d location;
    private float yaw;
    private float pitch;
    private int entityId;

    public MinecraftBot(ClientSession session, Credentials credentials, InetSocketAddress serverAddress, String username) {
        this.session = session;
        this.credentials = credentials;
        this.serverAddress = serverAddress;
        this.username = username;
        this.autoReconnect = false;
        this.eventManager = new EventManager(username);
        this.inventory = new Inventory(this);
        this.playerTracker = new PlayerTracker();
        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, getUsername() + "-game-thread");
            t.setDaemon(true);
            return t;
        });
        session.addListener(new PacketHandler(this));
    }

    public void sendCommand(String command) {
        if (command.startsWith("/")) command = command.substring(1);
        session.send(new ServerboundChatCommandPacket(command));
    }

    public void sendChatMessage(String message) {
        if (message.startsWith("/")) {
            log.warn("Sending message starting with '/'. To send chat commands use 'sendCommand(String command)' instead");
        }
        session.send(new ServerboundChatPacket(message, Instant.now().toEpochMilli(), 0L, null, 0, new BitSet(), 0));
    }

    public void lookAt(Vector3d location) {
        float[] angles = getYawPitchTo(this.location.add(0, 1.62, 0), location); //Add 1.62 on the Y to make it eye level
        this.yaw = angles[0];
        this.pitch = angles[1];
        session.send(new ServerboundMovePlayerRotPacket(true, true, yaw, pitch));
    }

    public void lookAtPlayer(PlayerTracker.TrackedPlayer player) {
        lookAt(player.position.add(0, 1.62, 0));  //Add 1.62 on the Y to make it eye level
    }

    /**
     * @param autoReconnect Should the bot automatically reconnect if it disconnects from the server
     * @param delay         The delay in millis for the bot to rejoin the server after getting disconnected
     */
    public void setAutoReconnect(boolean autoReconnect, long delay) {
        this.autoReconnect = autoReconnect;
        this.autoReconnectDelay = delay;
    }


    private float[] getYawPitchTo(Vector3d from, Vector3d to) {
        double dx = to.getX() - from.getX();
        double dy = to.getY() - from.getY();
        double dz = to.getZ() - from.getZ();

        double distanceXZ = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) Math.toDegrees(-Math.atan2(dy, distanceXZ));

        return new float[]{yaw, pitch};
    }


    public static final class Accessor {
        private Accessor() {
        }
        public static void setSession(MinecraftBot bot, ClientSession session) {
            bot.setSession(session);
        }
        public static void setOpenedWindow(MinecraftBot bot, Window window) {
            bot.setOpenedWindow(window);
        }
        public static void setLocation(MinecraftBot bot, Vector3d location) {
            bot.setLocation(location);
        }
        public static void setYaw(MinecraftBot bot, float yaw) {
            bot.setYaw(yaw);
        }
        public static void setPitch(MinecraftBot bot, float pitch) {
            bot.setPitch(pitch);
        }
        public static void setEntityId(MinecraftBot bot, int entityId) {
            bot.setEntityId(entityId);
        }
        public static void setExecutor(MinecraftBot bot, ScheduledExecutorService executor) {
            bot.setExecutor(executor);
        }
    }
}
