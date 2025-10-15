package me.diamond.event;

import lombok.Getter;
import me.diamond.MinecraftBot;
import me.diamond.container.Window;

@Getter
public class WindowOpenEvent extends Event {
    private final Window window;

    public WindowOpenEvent(MinecraftBot bot, Window window) {
        super(bot);
        this.window = window;
    }
}
