package me.diamond.container;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ClickItemAction;
import org.geysermc.mcprotocollib.protocol.data.game.item.HashedStack;

import java.util.List;
import java.util.regex.Pattern;

public interface Window extends Container{
    String getTitle();
    int getWindowStateId();
    void setWindowStateId(int windowStateId);
    Item getCursorItem();
    void setCursorItem(Item cursorItem);
    Int2ObjectOpenHashMap<HashedStack> getChangedSlots();
    void clickItem(int slot, ClickItemAction clickItemAction);
    List<Item> getItemsByName(Pattern namePattern);
}
