package me.diamond.container;

import me.diamond.Bot;


import java.util.List;

public interface Container {
    Bot getOwner();
    int getContainerId();

    void addItem(Item item);
    void setItem(int slot, Item item);
    void setItems(List<Item> items);
    List<Item> getItems();
}
