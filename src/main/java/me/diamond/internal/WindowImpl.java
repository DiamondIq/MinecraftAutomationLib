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
import java.util.List;
import java.util.regex.Pattern;

@Getter
@Setter
final class WindowImpl implements Window {
    private List<Item> items;
    private final Bot owner;
    private final String title;
    private final int containerId;
    private int windowStateId = 0;
    private Item cursorItem = null;
    private final Int2ObjectOpenHashMap<HashedStack> changedSlots = new Int2ObjectOpenHashMap<>();

    public WindowImpl(Bot owner, String title, int containerId) {
        this.owner = owner;
        this.title = title;
        this.containerId = containerId;
        items = new ArrayList<>();
    }

    @Override
    public void clickItem(int slot, ClickItemAction clickItemAction) {
        ServerboundContainerClickPacket click = new ServerboundContainerClickPacket(
                getContainerId(),
                windowStateId,
                slot,
                ContainerActionType.CLICK_ITEM,
                clickItemAction,
                (cursorItem != null ? ((ItemImpl) cursorItem).toHashedStack() : null),
                changedSlots
        );
        getOwner().getSession().send(click);
    }

    @Override
    public List<Item> getItemsByName(Pattern namePattern) {
        return getItems().stream().filter(item -> namePattern.matcher(item.getDisplayName()).matches()).toList();
    }

    @Override
    public void addItem(Item item) {
        items.add(item);
    }

    @Override
    public void setItem(int slot, Item item) {
        items.set(slot, item);
    }
}
