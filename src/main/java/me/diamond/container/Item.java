package me.diamond.container;

import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;

import java.util.List;

public interface Item {
    ItemStack getItemStack();
    int getId();
    int getAmount();
    /**
     * @return The slot of a window that the item is in
     */
    int getWindowSlot();
    boolean isUnbreakable();
    String getDisplayName();
    List<String> getLore();
}
