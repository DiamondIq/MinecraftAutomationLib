package me.diamond.event;

import lombok.Getter;
import me.diamond.Bot;
import me.diamond.container.Window;

@Getter
public class WindowOpenEvent extends Event {
    private final Window window;

    public WindowOpenEvent(Bot bot, Window window) {
        super(bot);
        this.window = window;
    }
}
