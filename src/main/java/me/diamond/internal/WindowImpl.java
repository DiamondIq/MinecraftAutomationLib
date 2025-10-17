package me.diamond.internal;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.Getter;
import lombok.Setter;
import me.diamond.Bot;
import me.diamond.container.Item;
import me.diamond.container.Window;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ClickItemAction;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerActionType;
import org.geysermc.mcprotocollib.protocol.data.game.item.HashedStack;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClickPacket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

@Getter
final class WindowImpl implements Window {
    private List<Item> items;
    private final Bot owner;
    private final String title;
    private final int containerId;
    @Setter
    private int windowStateId = 0;
    @Setter
    private Item cursorItem = null;
    private final Int2ObjectOpenHashMap<HashedStack> changedSlots = new Int2ObjectOpenHashMap<>();

    public WindowImpl(Bot owner, String title, int containerId, int windowSize) {
        this.owner = owner;
        this.title = title;
        this.containerId = containerId;
        items = new ArrayList<>(Collections.nCopies(windowSize, null));
    }

    @Override
    public void clickItem(int slot, ClickItemAction clickItemAction) {
        windowStateId++;

        Int2ObjectOpenHashMap<HashedStack> slots = new Int2ObjectOpenHashMap<>();
        slots.put(slot, null);

        ServerboundContainerClickPacket click = new ServerboundContainerClickPacket(
                getContainerId(),
                windowStateId,
                slot,
                ContainerActionType.CLICK_ITEM,
                clickItemAction,
                null,
                slots
        );

        getOwner().getSession().send(click);
    }

    @Override
    public List<Item> getItemsByName(Pattern namePattern) {
        return getItems().stream()
                .filter(item -> item != null && item.getDisplayName() != null && namePattern.matcher(item.getDisplayName()).matches())
                .toList();
    }

    public void addItem(Item item) {
        items.add(item);
    }

    public void setItem(int slot, Item item) {
        items.set(slot, item);
    }
}
