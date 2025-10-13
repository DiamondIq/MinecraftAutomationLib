package me.diamond.event;

import lombok.Getter;
import me.diamond.MinecraftBot;
import me.diamond.container.Window;

@Getter
public class OpenWindowEvent extends Event {
    private final Window window;

    public OpenWindowEvent(MinecraftBot bot, Window window) {
        super(bot);
        this.window = window;
    }
}
