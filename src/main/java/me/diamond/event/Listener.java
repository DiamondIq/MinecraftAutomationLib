package me.diamond.event;

public interface Listener<T extends Event> {
    void onEvent(T event);
}
