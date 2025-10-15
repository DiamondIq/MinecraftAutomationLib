package me.diamond.event;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

public class EventManager {

    private final Map<Class<? extends Event>, List<Listener<? extends Event>>> listeners = new HashMap<>();
    private final Map<Class<? extends Event>, List<Consumer<? extends Event>>> oneTimeListeners = new HashMap<>();
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

    /**
     * Registers a one-time listener for a specific event type.
     * <p>
     * The provided {@link Consumer} will be executed the next time an event of the given type
     * is fired. After it runs once, it is automatically removed
     * and will not be invoked again for future events of the same type.
     * </p>
     *
     * <p>
     * This method is useful for temporary or single-use callbacks, such as waiting for a specific
     * login event, entity spawn or menu open.
     * </p>
     *
     * <h3>Example usage:</h3>
     * <pre>{@code
     * bot.getEventManager().once(WindowOpenEvent.class, windowOpenEvent -> {
     *     System.out.printf("Opened window: %s\n", windowOpenEvent.getWindow().getTitle());
     * }
     * }</pre>
     *
     * @param <T>         the type of event to listen for
     * @param eventClass  the class object representing the event type
     * @param action      the consumer to execute once when the event is fired
     *
     * @see #addListener(Class, Listener)
     */
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
