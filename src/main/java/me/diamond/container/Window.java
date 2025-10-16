package me.diamond.container;

import org.geysermc.mcprotocollib.protocol.data.game.inventory.ClickItemAction;

import java.util.List;
import java.util.regex.Pattern;

public interface Window extends Container{
    void clickItem(int slot, ClickItemAction clickItemAction);
    List<Item> getItemsByName(Pattern namePattern);
}
