package me.diamond.container;

import me.diamond.MinecraftBot;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ClickItemAction;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerActionType;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClickPacket;

import java.util.List;
import java.util.regex.Pattern;

public class Window extends Container{
    public Window(MinecraftBot owner, String title, int containerId) {
        super(owner, title, containerId);
    }

    public void clickItem(int slot, ClickItemAction clickItemAction) {
        ServerboundContainerClickPacket click = new ServerboundContainerClickPacket(
                getContainerId(),
                windowStateId,
                slot,
                ContainerActionType.CLICK_ITEM,
                clickItemAction,
                cursorItem != null ? cursorItem.toHashedStack() : null,
                changedSlots
        );
        getOwner().getSession().send(click);
    }

    public List<Item> getItemsByName(Pattern namePattern) {
        return getItems().stream().filter(item -> namePattern.matcher(item.getDisplayName()).matches()).toList();
    }
}
