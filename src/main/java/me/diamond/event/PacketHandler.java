package me.diamond.event;

import lombok.extern.slf4j.Slf4j;
import me.diamond.BotFactory;
import me.diamond.MinecraftBot;
import me.diamond.container.Item;
import me.diamond.container.Window;
import me.diamond.internal.BotUpdater;
import net.kyori.adventure.key.Key;
import org.geysermc.mcprotocollib.network.ClientSession;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;
import org.geysermc.mcprotocollib.network.event.session.SessionAdapter;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.HandPreference;
import org.geysermc.mcprotocollib.protocol.data.game.setting.ChatVisibility;
import org.geysermc.mcprotocollib.protocol.data.game.setting.ParticleStatus;
import org.geysermc.mcprotocollib.protocol.data.game.setting.SkinPart;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundKeepAlivePacket;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundClientInformationPacket;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundCustomPayloadPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundPlayerChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerPositionPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.*;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundChunkBatchFinishedPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundClientTickEndPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundPlayerLoadedPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundAcceptTeleportationPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundChunkBatchReceivedPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosRotPacket;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static me.diamond.util.ComponentUtils.toPlainText;

@Slf4j
public class PacketHandler extends SessionAdapter {

    private final MinecraftBot bot;
    private final EventManager eventManager;

    private volatile boolean loggedIn = false;
    private volatile boolean chunksLoaded = false;
    private volatile boolean tickTaskStarted = false;

    private final AtomicInteger remainingChunkBatches = new AtomicInteger(0);

    private final ScheduledExecutorService botExecutor;

    public PacketHandler(MinecraftBot bot) {
        this.bot = bot;
        this.eventManager = bot.getEventManager();
        this.botExecutor = bot.getExecutor();
    }

    @Override
    public void packetReceived(Session session, Packet packet) {
        botExecutor.execute(() -> {
            try {
                // --- LOGIN SEQUENCE ---
                if (packet instanceof ClientboundLoginPacket loginPacket) {
                    loggedIn = true;
                    BotUpdater.setEntityId(bot, loginPacket.getEntityId());
                    session.send(new ServerboundClientInformationPacket(
                            "en_us",
                            10,
                            ChatVisibility.FULL,
                            true,
                            Arrays.asList(SkinPart.VALUES),
                            HandPreference.RIGHT_HAND,
                            false,
                            true,
                            ParticleStatus.ALL
                    ));
                    session.send(new ServerboundCustomPayloadPacket(
                            Key.key("minecraft:brand"),
                            "vanilla".getBytes(StandardCharsets.UTF_8)
                    ));
                    eventManager.fireEvent(new LogInEvent(bot));
                } else if (packet instanceof ClientboundKeepAlivePacket keepAlive) {
//                session.send(new ServerboundKeepAlivePacket(keepAlive.getPingId()));
                } else if (packet instanceof ClientboundPlayerPositionPacket pos) {
                    // Accept teleport
                    session.send(new ServerboundAcceptTeleportationPacket(pos.getId()));
                    botExecutor.schedule(() -> {
                        if (session.isConnected()) {
                            session.send(new ServerboundMovePlayerPosRotPacket(
                                    true, true,
                                    pos.getPosition().getX(),
                                    pos.getPosition().getY(),
                                    pos.getPosition().getZ(),
                                    pos.getYRot(),
                                    pos.getXRot()
                            ));
                        }
                    }, 50 + ThreadLocalRandom.current().nextInt(0, 30), TimeUnit.MILLISECONDS);

                    BotUpdater.setLocation(bot, pos.getPosition());
                    BotUpdater.setYaw(bot, pos.getYRot());
                    BotUpdater.setPitch(bot, pos.getXRot());

                } else if (packet instanceof ClientboundChunkBatchFinishedPacket) {
                    remainingChunkBatches.decrementAndGet();
                    session.send(new ServerboundChunkBatchReceivedPacket(1.0f));

                    if (remainingChunkBatches.get() <= 0 && !chunksLoaded) {
                        chunksLoaded = true;
                        botExecutor.schedule(() -> {
                            if (session.isConnected()) {
                                session.send(ServerboundPlayerLoadedPacket.INSTANCE);
                                startTickLoop(session);
                            }
                        }, ThreadLocalRandom.current().nextInt(500, 1200), TimeUnit.MILLISECONDS);
                    }

                } else if (packet instanceof ClientboundPlayerChatPacket msg) {
                    String content = msg.getContent() != null ? msg.getContent() : "";
                    eventManager.fireEvent(new ChatMessageEvent(bot, content, Optional.of(toPlainText(msg.getName()))));

                } else if (packet instanceof ClientboundSystemChatPacket msg) {
                    String content = msg.getContent() != null ? toPlainText(msg.getContent()) : "";
                    eventManager.fireEvent(new ChatMessageEvent(bot, content, Optional.empty()));

                }
                // --- INVENTORY PACKETS ---
                if (packet instanceof ClientboundSetPlayerInventoryPacket inventoryPacket) {
                    handleSetPlayerInventory(inventoryPacket);
                } else if (packet instanceof ClientboundContainerSetContentPacket contentPacket) {
                    handleContainerSetContent(contentPacket);
                } else if (packet instanceof ClientboundContainerSetSlotPacket slotPacket) {
                    handleContainerSetSlot(slotPacket);
                } else if (packet instanceof ClientboundOpenScreenPacket openScreenPacket) {
                    if (openScreenPacket.getContainerId() != 0) {
                        Window window = new Window(bot, toPlainText(openScreenPacket.getTitle()), openScreenPacket.getContainerId());
                        BotUpdater.setOpenedWindow(bot, window);
                        eventManager.fireEvent(new OpenWindowEvent(bot, window));
                    }
                } else if (packet instanceof ClientboundContainerClosePacket containerClosePacket) {
                    if (containerClosePacket.getContainerId() != 0) {
                        BotUpdater.setOpenedWindow(bot, null);
                    }
                }
            } catch (Exception e) {
                log.warn("Error handling packet {}: {}", packet.getClass().getSimpleName(), e.getMessage(), e);
            }
        });
    }

    // --- INVENTORY HANDLERS ---
    private void handleSetPlayerInventory(ClientboundSetPlayerInventoryPacket packet) {
        try {
            Item item = packet.getContents() != null ? new Item(packet.getContents()) : null;
            bot.getInventory().setItem(packet.getSlot(), item);
            eventManager.fireEvent(new ContainerUpdateContentEvent(bot, bot.getInventory(), true));
        } catch (Exception e) {
            log.warn("Error handling SetPlayerInventoryPacket: ", e);
        }
    }

    private void handleContainerSetContent(ClientboundContainerSetContentPacket packet) {
        try {
            int containerId = packet.getContainerId();
            List<Item> items = Arrays.stream(packet.getItems())
                    .map(stack -> stack == null ? null : new Item(stack))
                    .toList();

            if (containerId == 0) {
                bot.getInventory().setItems(items);
                bot.getInventory().windowStateId++;
                eventManager.fireEvent(new ContainerUpdateContentEvent(bot, bot.getInventory(), true));
            } else {
                Window window = bot.getOpenedWindow();
                if (window != null) {
                    window.setItems(items);
                    window.windowStateId++;
                    eventManager.fireEvent(new ContainerUpdateContentEvent(bot, window, false));
                }
            }
        } catch (Exception e) {
            log.warn("Error handling ContainerSetContentPacket: ", e);
        }
    }

    private void handleContainerSetSlot(ClientboundContainerSetSlotPacket packet) {
        try {
            int containerId = packet.getContainerId();
            Item item = packet.getItem() != null ? new Item(packet.getItem()) : null;


            if (containerId == 0) {
                bot.getInventory().setItem(packet.getSlot(), item);
                bot.getInventory().windowStateId++;
                bot.getInventory().changedSlots.put(packet.getSlot(), item != null ? item.toHashedStack() : null);
                eventManager.fireEvent(new ContainerUpdateContentEvent(bot, bot.getInventory(), true));
            } else {
                Window window = bot.getOpenedWindow();
                if (window != null) {
                    window.setItem(packet.getSlot(), item);
                    window.windowStateId++;
                    window.changedSlots.put(packet.getSlot(), item != null ? item.toHashedStack() : null);
                    eventManager.fireEvent(new ContainerUpdateContentEvent(bot, window, false));
                }
            }
        } catch (Exception e) {
            log.warn("Error handling ContainerSetSlotPacket: ", e);
        }
    }

    private void startTickLoop(Session session) {
        if (tickTaskStarted) return;
        tickTaskStarted = true;

        botExecutor.scheduleAtFixedRate(() -> {
            if (!session.isConnected()) return;

            // Send client tick packet
            session.send(ServerboundClientTickEndPacket.INSTANCE);

            // Send movement update (tell the server where the bot is)
            session.send(new ServerboundMovePlayerPosRotPacket(
                    true, true,
                    bot.getLocation().getX(),
                    bot.getLocation().getY(),
                    bot.getLocation().getZ(),
                    bot.getYaw(),
                    bot.getPitch()
            ));

        }, 0, 50, TimeUnit.MILLISECONDS);
    }


    @Override
    public void disconnected(DisconnectedEvent event) {
        botExecutor.execute(() -> {
            log.warn("{} has disconnected: {}", bot.getUsername(), toPlainText(event.getReason()));

            eventManager.fireEvent(new DisconnectEvent(bot));

            if (bot.isAutoReconnect()) {
                long delay = bot.getAutoReconnectDelay();
                log.info("Reconnecting {} in {}ms...", bot.getUsername(), delay);

                botExecutor.schedule(() -> {
                    try {
                        ClientSession newSession = BotFactory.createSession(bot.getCredentials(), bot.getServerAddress());
                        BotUpdater.updateExecutor(bot);
                        newSession.addListener(new PacketHandler(bot));
                        BotUpdater.updateSession(bot, newSession);

                        log.info("{} has reconnected to the server", bot.getUsername());
                    } catch (Exception e) {
                        log.error("Failed to reconnect {}: {}", bot.getUsername(), e.getMessage(), e);
                    } finally {
                        botExecutor.shutdown();
                    }
                }, delay, TimeUnit.MILLISECONDS);
            }
        });
    }
}
