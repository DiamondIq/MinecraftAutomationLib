package me.diamond.event;

import lombok.extern.slf4j.Slf4j;
import me.diamond.BotFactory;
import me.diamond.MinecraftBot;
import me.diamond.container.Item;
import me.diamond.container.Window;
import me.diamond.internal.BotUpdater;
import org.cloudburstmc.math.vector.Vector3d;
import org.geysermc.mcprotocollib.network.ClientSession;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;
import org.geysermc.mcprotocollib.network.event.session.SessionAdapter;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.HandPreference;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;
import org.geysermc.mcprotocollib.protocol.data.game.setting.ChatVisibility;
import org.geysermc.mcprotocollib.protocol.data.game.setting.ParticleStatus;
import org.geysermc.mcprotocollib.protocol.data.game.setting.SkinPart;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundClientInformationPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundPlayerChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.*;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerPositionPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.*;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundChunkBatchFinishedPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundClientTickEndPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundPlayerLoadedPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundAcceptTeleportationPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundChunkBatchReceivedPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosPacket;

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
                if (packet instanceof ClientboundLoginPacket loginPacket) {
                    log.info("Connected as {} to {}:{}", bot.getUsername(), bot.getServerAddress().getHostName(), bot.getServerAddress().getPort());
                    BotUpdater.setEntityId(bot, loginPacket.getEntityId());
                    session.send(new ServerboundClientInformationPacket("en_us", 2, ChatVisibility.FULL, true, Arrays.asList(SkinPart.VALUES), HandPreference.RIGHT_HAND, false, true, ParticleStatus.ALL));
//                    session.send(new ServerboundCustomPayloadPacket(Key.key("minecraft:brand"), "vanilla".getBytes(StandardCharsets.UTF_8)));
                    botExecutor.schedule(() -> {
                        if (session.isConnected()) {
                            session.send(ServerboundPlayerLoadedPacket.INSTANCE);
                            startTickLoop(session);
                            startSendingPosition(session);
                        }
                    }, ThreadLocalRandom.current().nextInt(500, 1200), TimeUnit.MILLISECONDS);
                    eventManager.fireEvent(new LogInEvent(bot));
                } else if (packet instanceof ClientboundPlayerPositionPacket pos) {
                    // Accept teleport
                    session.send(new ServerboundAcceptTeleportationPacket(pos.getId()));
                    BotUpdater.setLocation(bot, pos.getPosition());
                    BotUpdater.setYaw(bot, pos.getYRot());
                    BotUpdater.setPitch(bot, pos.getXRot());

                } else if (packet instanceof ClientboundChunkBatchFinishedPacket) {
                    session.send(new ServerboundChunkBatchReceivedPacket(1.0f));
                } else if (packet instanceof ClientboundPlayerChatPacket msg) {
                    String content = msg.getContent() != null ? msg.getContent() : "";
                    eventManager.fireEvent(new ChatMessageEvent(bot, content, Optional.of(toPlainText(msg.getName()))));

                } else if (packet instanceof ClientboundSystemChatPacket msg) {
                    String content = msg.getContent() != null ? toPlainText(msg.getContent()) : "";
                    eventManager.fireEvent(new ChatMessageEvent(bot, content, Optional.empty()));

                }
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
                } else if (packet instanceof ClientboundAddEntityPacket addEntity) {
                    if (addEntity.getType() == EntityType.PLAYER) {
                        bot.getPlayerTracker().addPlayer(addEntity.getEntityId(), addEntity.getUuid().toString(), Vector3d.from(addEntity.getX(), addEntity.getY(), addEntity.getZ()), addEntity.getYaw(), addEntity.getPitch());
                    }
                } else if (packet instanceof ClientboundTeleportEntityPacket teleport) {
                    handleTeleportEntity(teleport);
                } else if (packet instanceof ClientboundMoveEntityPosRotPacket move) {
                    handleMoveEntityPosRot(move);
                } else if (packet instanceof ClientboundMoveEntityPosPacket move) {
                    var tracked = bot.getPlayerTracker().getPlayer(move.getEntityId());
                    if (tracked != null) {
                        var newPos = tracked.position.add(move.getMoveX(), move.getMoveY(), move.getMoveZ());
                        if (tracked.position.compareTo(newPos) != 0) {
                            tracked.position = newPos;
                            bot.getEventManager().fireEvent(new PlayerMoveEvent(bot, tracked));
                        }
                    }
                } else if (packet instanceof ClientboundEntityPositionSyncPacket sync) {
                    var tracked = bot.getPlayerTracker().getPlayer(sync.getId());
                    if (tracked != null) {
                        if (tracked.position.compareTo(sync.getPosition()) != 0) {
                            tracked.position = sync.getPosition();
                        }
                    }
                    bot.getEventManager().fireEvent(new PlayerMoveEvent(bot, tracked));
                } else if (packet instanceof ClientboundRemoveEntitiesPacket remove) {
                    for (int id : remove.getEntityIds()) {
                        bot.getPlayerTracker().removePlayer(id);
                    }
                }
            } catch (Exception e) {
                log.warn("Error handling packet {}: {}", packet.getClass().getSimpleName(), e.getMessage(), e);
            }
        });
    }

    private void handleTeleportEntity(ClientboundTeleportEntityPacket packet) {
        if (packet.getId() == bot.getEntityId()) {
            BotUpdater.setLocation(bot, packet.getPosition());
            BotUpdater.setYaw(bot, packet.getYRot());
            BotUpdater.setPitch(bot, packet.getXRot());
        } else {
            var tracked = bot.getPlayerTracker().getPlayer(packet.getId());
            if (tracked != null) {
                if (tracked.position.compareTo(packet.getPosition()) != 0) {
                    tracked.position = packet.getPosition();
                    bot.getEventManager().fireEvent(new PlayerMoveEvent(bot, tracked));
                }
            }
        }
    }

    private void handleMoveEntityPosRot(ClientboundMoveEntityPosRotPacket packet) {
        var tracked = bot.getPlayerTracker().getPlayer(packet.getEntityId());
        if (tracked != null) {
            var newPos = tracked.position.add(packet.getMoveX(), packet.getMoveY(), packet.getMoveZ());
            if (tracked.position.compareTo(newPos) != 0) {
                tracked.position = newPos;
                bot.getEventManager().fireEvent(new PlayerMoveEvent(bot, tracked));
            }
        }
    }

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
            List<Item> items = Arrays.stream(packet.getItems()).map(stack -> stack == null ? null : new Item(stack)).toList();

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

            session.send(ServerboundClientTickEndPacket.INSTANCE);

        }, 0, 50, TimeUnit.MILLISECONDS);
    }


    private void startSendingPosition(Session session) {
        botExecutor.scheduleAtFixedRate(() -> {
            session.send(new ServerboundMovePlayerPosPacket(true, true, bot.getLocation().getX(), bot.getLocation().getY(), bot.getLocation().getZ()));
        }, 0, 1, TimeUnit.SECONDS);
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
