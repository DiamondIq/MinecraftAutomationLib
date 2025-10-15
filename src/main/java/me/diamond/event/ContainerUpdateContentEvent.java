package me.diamond.event;

import lombok.Getter;
import me.diamond.Bot;
import me.diamond.container.Container;


@Getter
public class ContainerUpdateContentEvent extends Event{
    private final Container container;
    private final boolean isPlayersInventory;

    public ContainerUpdateContentEvent(Bot bot, Container container, boolean isPlayersInventory) {
        super(bot);
        this.container = container;
        this.isPlayersInventory = isPlayersInventory;
    }
}
