package me.diamond;

import me.diamond.container.Inventory;
import me.diamond.container.Window;
import me.diamond.credentials.Credentials;
import me.diamond.event.EventManager;
import org.cloudburstmc.math.vector.Vector3d;
import org.geysermc.mcprotocollib.network.ClientSession;

import java.net.InetSocketAddress;
import java.util.concurrent.ScheduledExecutorService;

public interface Bot {

    String getUsername();
    int getEntityId();

    ClientSession getSession();
    InetSocketAddress getServerAddress();
    Credentials getCredentials();

    EventManager getEventManager();
    Inventory getInventory();
    PlayerTracker getPlayerTracker();

    Vector3d getLocation();
    float getYaw();
    float getPitch();

    Window getOpenedWindow();

    boolean isAutoReconnect();
    long getAutoReconnectDelay();
    void setAutoReconnect(boolean enabled, long delay);

    ScheduledExecutorService getExecutor();

    void sendChatMessage(String message);
    void sendCommand(String command);
    void lookAt(Vector3d location);
    void lookAtPlayer(PlayerTracker.TrackedPlayer player);

    void setOpenedWindow(Window window);
    void setLocation(Vector3d location);
    void setYaw(float yaw);
    void setPitch(float pitch);
    void setEntityId(int entityId);
}
