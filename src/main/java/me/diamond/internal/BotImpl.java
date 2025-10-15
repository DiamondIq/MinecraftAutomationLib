package me.diamond.internal;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import me.diamond.Bot;
import me.diamond.PlayerTracker;
import me.diamond.container.Inventory;
import me.diamond.container.Window;
import me.diamond.credentials.Credentials;
import me.diamond.event.EventManager;
import org.cloudburstmc.math.vector.Vector3d;
import org.geysermc.mcprotocollib.network.ClientSession;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatCommandPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerRotPacket;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.BitSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Slf4j
@Getter
@Setter
final class BotImpl implements Bot {
    private ClientSession session;
    private final Credentials credentials;
    private final InetSocketAddress serverAddress;
    private final String username;

    private final EventManager eventManager;
    private final Inventory inventory;
    private final PlayerTracker playerTracker;
    private ScheduledExecutorService executor;

    private Window openedWindow;
    private Vector3d location;
    private float yaw;
    private float pitch;
    private int entityId;

    private boolean autoReconnect;
    @Setter(AccessLevel.NONE)
    private long autoReconnectDelay;

    public BotImpl(ClientSession session, Credentials credentials, InetSocketAddress serverAddress, String username) {
        this.session = session;
        this.credentials = credentials;
        this.serverAddress = serverAddress;
        this.username = username;

        this.eventManager = new EventManagerImpl(username);
        this.inventory = new InventoryImpl(this);
        this.playerTracker = new PlayerTracker();

        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, username + "-bot-thread");
            t.setDaemon(true);
            return t;
        });

        this.autoReconnect = false;
        this.autoReconnectDelay = 0L;

        session.addListener(new PacketHandler(this));
    }

    @Override
    public void sendCommand(String command) {
        if (command.startsWith("/")) command = command.substring(1);
        session.send(new ServerboundChatCommandPacket(command));
    }

    @Override
    public void sendChatMessage(String message) {
        if (message.startsWith("/")) {
            log.warn("Message starts with '/'. Use sendCommand() for commands.");
        }
        session.send(new ServerboundChatPacket(message, Instant.now().toEpochMilli(), 0L, null, 0, new BitSet(), 0));
    }

    @Override
    public void lookAt(Vector3d location) {
        float[] angles = getYawPitchTo(this.location.add(0, 1.62, 0), location);
        this.yaw = angles[0];
        this.pitch = angles[1];
        session.send(new ServerboundMovePlayerRotPacket(true, true, yaw, pitch));
    }

    @Override
    public void lookAtPlayer(PlayerTracker.TrackedPlayer player) {
        lookAt(player.position.add(0, 1.62, 0));
    }

    @Override
    public void setAutoReconnect(boolean enabled, long delay) {
        this.autoReconnect = enabled;
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

    public void updateExecutor() {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, getUsername() + "-game-thread");
            t.setDaemon(true);
            return t;
        });
        this.executor = executor;
    }
}
