package me.diamond;

import org.cloudburstmc.math.vector.Vector3d;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerTracker {

    public static class TrackedPlayer {
        public final int id;
        public final String uuid;
        public Vector3d position;

        public TrackedPlayer(int id, String uuid, Vector3d position) {
            this.id = id;
            this.uuid = uuid;
            this.position = position;
        }
    }

    private final Map<Integer, TrackedPlayer> players = new ConcurrentHashMap<>();

    public void addPlayer(int id, String uuid, Vector3d position, float yaw, float pitch) {
        players.put(id, new TrackedPlayer(id, uuid, position));
    }

    public void removePlayer(int id) {
        players.remove(id);
    }

    public TrackedPlayer getPlayer(int id) {
        return players.get(id);
    }

    public Map<Integer, TrackedPlayer> getAllPlayers() {
        return players;
    }
}
