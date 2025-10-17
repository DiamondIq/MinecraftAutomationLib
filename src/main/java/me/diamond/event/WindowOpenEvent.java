package me.diamond.event;

import me.diamond.Bot;
import me.diamond.container.Window;

public class WindowOpenEvent extends Event {
    public WindowOpenEvent(Bot bot) {
        super(bot);
    }
    public Window getWindow() {
        return getBot().getOpenedWindow();
    }
}
