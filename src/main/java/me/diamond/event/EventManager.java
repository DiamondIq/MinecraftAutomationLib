package me.diamond.event;

import java.util.function.Consumer;

public interface EventManager {
    <T extends Event> void addListener(Class<T> eventClass, Listener<T> listener);

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
    <T extends Event> void once(Class<T> eventClass, Consumer<T> action);
}
