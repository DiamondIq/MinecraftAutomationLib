package me.diamond.event;

import lombok.Getter;
import me.diamond.MinecraftBot;
import me.diamond.container.Container;

@Getter
public class ContainerUpdateContentEvent extends Event{
    private final Container container;
    private final boolean isPlayersInventory;

    public ContainerUpdateContentEvent(MinecraftBot bot, Container container, boolean isPlayersInventory) {
        super(bot);
        this.container = container;
        this.isPlayersInventory = isPlayersInventory;
    }
}
