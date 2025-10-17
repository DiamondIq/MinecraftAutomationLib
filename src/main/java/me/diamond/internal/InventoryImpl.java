package me.diamond.internal;

import lombok.Getter;
import lombok.Setter;
import me.diamond.Bot;
import me.diamond.container.Inventory;
import me.diamond.container.Item;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundSetCarriedItemPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundSwingPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundUseItemPacket;
import org.jetbrains.annotations.Range;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
final class InventoryImpl implements Inventory {

    private List<Item> items;
    private final Bot owner;
    private final int containerId;

    InventoryImpl(Bot owner) {
        this.owner = owner;
        this.containerId = 0;
        this.items = new ArrayList<>(Collections.nCopies(46, null)); // default player inv size
    }

    @Override
    public void useItemInMainHand() {
        float yaw = getOwner().getYaw();
        float pitch = getOwner().getPitch();

        // Create and send the packet
        ServerboundUseItemPacket usePacket = new ServerboundUseItemPacket(
                Hand.MAIN_HAND,
                0, // sequence, 0 is fine for most servers
                yaw,
                pitch
        );

        getOwner().getSession().send(usePacket);
        getOwner().getSession().send(new ServerboundSwingPacket(Hand.MAIN_HAND));
    }

    @Override
    public void selectHotbarSlot(@Range(from = 0, to = 8) int slot) {
        getOwner().getSession().send(new ServerboundSetCarriedItemPacket(slot));
    }
    public void addItem(Item item) {
        items.add(item);
    }

    public void setItem(int slot, Item item) {
        items.set(slot, item);
    }
}
