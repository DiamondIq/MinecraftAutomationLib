package me.diamond.internal;

import lombok.extern.slf4j.Slf4j;
import me.diamond.event.Event;
import me.diamond.event.EventManager;
import me.diamond.event.Listener;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

@Slf4j
final class EventManagerImpl implements EventManager {

    private final Map<Class<? extends Event>, List<Listener<? extends Event>>> listeners = new HashMap<>();
    private final Map<Class<? extends Event>, List<Consumer<? extends Event>>> oneTimeListeners = new HashMap<>();
    private final ScheduledExecutorService executor;

    public EventManagerImpl(String username) {
        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, username + "-event-thread");
            t.setDaemon(true);
            return t;
        });
    }

    @Override
    public <T extends Event> void addListener(Class<T> eventClass, Listener<T> listener) {
        listeners.computeIfAbsent(eventClass, k -> new ArrayList<>()).add(listener);
    }

    @Override
    public <T extends Event> void once(Class<T> eventClass, Consumer<T> action) {
        oneTimeListeners.computeIfAbsent(eventClass, k -> new ArrayList<>()).add(action);
    }

    @SuppressWarnings("unchecked")
    public <T extends Event> void fireEvent(T event) {
        // Persistent listeners
        List<Listener<? extends Event>> list = listeners.get(event.getClass());
        if (list != null) {
            for (Listener<? extends Event> listener : list) {
                executor.execute(() -> {
                    try {
                        ((Listener<T>) listener).onEvent(event);
                    } catch (Exception e) {
                        log.error("Error while executing {} listener: {}",
                                event.getClass().getSimpleName(), e.getMessage(), e);
                    }
                });
            }
        }

        // One-time listeners
        List<Consumer<? extends Event>> list2 = oneTimeListeners.get(event.getClass());
        if (list2 != null && !list2.isEmpty()) {
            Iterator<Consumer<? extends Event>> iterator = list2.iterator();
            while (iterator.hasNext()) {
                Consumer<? extends Event> action = iterator.next();
                executor.execute(() -> {
                    try {
                        ((Consumer<T>) action).accept(event);
                    } catch (Exception e) {
                        log.error("Error while executing one-time {} listener: {}",
                                event.getClass().getSimpleName(), e.getMessage(), e);
                    }
                });
                iterator.remove();
            }

            if (list2.isEmpty()) {
                oneTimeListeners.remove(event.getClass());
            }
        }
    }
}
