package me.diamond.event;

import lombok.Getter;
import me.diamond.Bot;
import me.diamond.container.Window;


@Getter
public class WindowUpdateContentEvent extends Event{
    private final Window window;

    public WindowUpdateContentEvent(Bot bot, Window window) {
        super(bot);
        this.window = window;
    }
}
