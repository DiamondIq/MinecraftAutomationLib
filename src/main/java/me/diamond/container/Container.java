package me.diamond.container;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.Data;
import lombok.Setter;
import me.diamond.MinecraftBot;
import org.geysermc.mcprotocollib.protocol.data.game.item.HashedStack;

import java.util.ArrayList;
import java.util.List;

@Data
public abstract class Container {

    private List<Item> items;
    private final MinecraftBot owner;
    private final String title;
    private final int containerId;
    public int windowStateId = 0;
    public Item cursorItem = null;
    public final Int2ObjectOpenHashMap<HashedStack> changedSlots = new Int2ObjectOpenHashMap<>();

    public Container(MinecraftBot owner, String title, int containerId) {
        this.owner = owner;
        this.title = title;
        this.containerId = containerId;
        this.items = new ArrayList<>();
    }

    public void addItem(Item item) {
        items.add(item);
    }

    public void setItem(int slot, Item item) {
        items.set(slot, item);
    }

    public void setItems(List<Item> items) {
        this.items = new ArrayList<>(items);
    }
}
