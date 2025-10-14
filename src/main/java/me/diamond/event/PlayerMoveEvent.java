package me.diamond.event;

import lombok.Getter;
import me.diamond.MinecraftBot;
import me.diamond.PlayerTracker;

@Getter
public class PlayerMoveEvent extends Event{
    private final PlayerTracker.TrackedPlayer player;

    public PlayerMoveEvent(MinecraftBot bot, PlayerTracker.TrackedPlayer player) {
        super(bot);
        this.player = player;
    }
}
