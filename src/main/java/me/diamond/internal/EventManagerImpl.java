package me.diamond.internal;

import me.diamond.event.Event;
import me.diamond.event.EventManager;
import me.diamond.event.Listener;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

final class EventManagerImpl implements EventManager {

    private final Map<Class<? extends Event>, List<Listener<? extends Event>>> listeners = new HashMap<>();
    private final Map<Class<? extends Event>, List<Consumer<? extends Event>>> oneTimeListeners = new HashMap<>();
    private final ScheduledExecutorService executor; ;

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
        // Handle persistent listeners
        List<Listener<? extends Event>> list = listeners.get(event.getClass());
        if (list != null) {
            for (Listener<? extends Event> listener : list) {
                executor.execute(() -> ((Listener<T>) listener).onEvent(event));
            }
        }

        // Handle one-time listeners
        List<Consumer<? extends Event>> list2 = oneTimeListeners.get(event.getClass());
        if (list2 != null && !list2.isEmpty()) {
            Iterator<Consumer<? extends Event>> iterator = list2.iterator();
            while (iterator.hasNext()) {
                Consumer<? extends Event> action = iterator.next();
                executor.execute(() -> ((Consumer<T>) action).accept(event));
                iterator.remove();
            }

            // Clean up map entry if list is empty
            if (list2.isEmpty()) {
                oneTimeListeners.remove(event.getClass());
            }
        }
    }
}
