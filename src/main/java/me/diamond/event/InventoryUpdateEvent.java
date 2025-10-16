package me.diamond.event;

import lombok.Getter;
import me.diamond.Bot;
import me.diamond.container.Inventory;

@Getter
public class InventoryUpdateEvent extends Event{
    private final Inventory inventory;

    public InventoryUpdateEvent(Bot bot, Inventory inventory) {
        super(bot);
        this.inventory = inventory;
    }
}
