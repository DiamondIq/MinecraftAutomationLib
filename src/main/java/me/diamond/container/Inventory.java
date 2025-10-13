package me.diamond.container;

import me.diamond.MinecraftBot;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3i;
import org.geysermc.mcprotocollib.protocol.data.game.entity.object.Direction;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundSetCarriedItemPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundSwingPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundUseItemOnPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundUseItemPacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

public class Inventory extends Container {
    public Inventory(MinecraftBot owner) {
        super(owner, null, 0);
    }

    /**
     * Uses the selected item (Right clicks)
     */
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

    public void selectHotbarSlot(@Range(from = 0, to = 8) int slot) {
        getOwner().getSession().send(new ServerboundSetCarriedItemPacket(slot));
    }
}
