package me.diamond.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.diamond.MinecraftBot;

import java.util.Optional;

@Getter
public class ChatMessageEvent extends Event {
    private final String message;
    private final Optional<String> senderName;

    public ChatMessageEvent(MinecraftBot bot, String message, Optional<String> senderName) {
        super(bot);
        this.message = message;
        this.senderName = senderName;
    }
}
