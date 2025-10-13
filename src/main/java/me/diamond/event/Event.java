package me.diamond.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.diamond.MinecraftBot;

@RequiredArgsConstructor
@Getter
public abstract class Event {
    private final MinecraftBot bot;
}
