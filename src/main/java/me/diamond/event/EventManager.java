package me.diamond.event;

import me.diamond.MinecraftBot;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class EventManager {

    private final Map<Class<? extends Event>, List<Listener<? extends Event>>> listeners = new HashMap<>();
    private final ScheduledExecutorService executor; ;

    public EventManager(String username) {
        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, username + "-event-thread");
            t.setDaemon(true);
            return t;
        });
    }

    public <T extends Event> void addListener(Class<T> eventClass, Listener<T> listener) {
        listeners.computeIfAbsent(eventClass, k -> new ArrayList<>()).add(listener);
    }

    @SuppressWarnings("unchecked")
    public <T extends Event> void fireEvent(T event) {
        List<Listener<? extends Event>> list = listeners.get(event.getClass());
        if (list != null) {
            for (Listener<? extends Event> listener : list) {
                executor.execute(() -> ((Listener<T>) listener).onEvent(event));
            }
        }
    }
}
