package me.diamond.container;

import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;

import java.util.List;

public interface Item {
    ItemStack getItemStack();
    int getId();
    int getAmount();
    boolean isUnbreakable();
    String getDisplayName();
    List<String> getLore();
}
