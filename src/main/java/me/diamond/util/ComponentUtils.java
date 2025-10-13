package me.diamond.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

public class ComponentUtils {
    // Converts Adventure Component to plain text
    public static String toPlainText(Component component) {
        if (component == null) return "";
        StringBuilder builder = new StringBuilder();
        appendComponentText(component, builder);
        return builder.toString();
    }

    private static void appendComponentText(Component component, StringBuilder builder) {
        if (component instanceof TextComponent textComponent) {
            builder.append(textComponent.content());
        }
        for (Component child : component.children()) {
            appendComponentText(child, builder);
        }
    }
}
