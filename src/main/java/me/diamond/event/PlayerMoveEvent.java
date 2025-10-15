package me.diamond.event;

import lombok.Getter;
import me.diamond.Bot;
import me.diamond.PlayerTracker;

@Getter
public class PlayerMoveEvent extends Event{
    private final PlayerTracker.TrackedPlayer player;

    public PlayerMoveEvent(Bot bot, PlayerTracker.TrackedPlayer player) {
        super(bot);
        this.player = player;
    }
}
