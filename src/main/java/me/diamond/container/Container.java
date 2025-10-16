package me.diamond.container;

import me.diamond.Bot;


import java.util.List;

public interface Container {
    Bot getOwner();
    int getContainerId();

    List<Item> getItems();
}
