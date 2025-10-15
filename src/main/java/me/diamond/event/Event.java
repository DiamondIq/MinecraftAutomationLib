package me.diamond.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.diamond.Bot;

@RequiredArgsConstructor
@Getter
public abstract class Event {
    private final Bot bot;
}
