package me.diamond.internal;

import lombok.Getter;
import me.diamond.container.Item;
import org.geysermc.mcprotocollib.protocol.data.game.item.HashedStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponent;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

import java.util.*;

@Getter
final class ItemImpl implements Item {
    private final ItemStack itemStack;
    private final int id;
    private final int amount;
    private final boolean unbreakable;
    private final String displayName;
    private final List<String> lore;

    public ItemImpl(ItemStack stack) {
        itemStack = stack;
        this.id = stack.getId();
        this.amount = stack.getAmount();

        DataComponents data = stack.getDataComponentsPatch();
        if (data != null) {
            Map<DataComponentType<?>, DataComponent<?, ?>> components = data.getDataComponents();

            this.unbreakable = hasComponent(components, "unbreakable");
            this.displayName = extractComponentText(components, "custom_name");
            this.lore = extractComponentTextList(components, "lore");
        } else {
            this.unbreakable = false;
            this.displayName = null;
            this.lore = Collections.emptyList();
        }
    }

    // Check if boolean component exists
    private boolean hasComponent(Map<DataComponentType<?>, DataComponent<?, ?>> components, String keyPart) {
        for (DataComponent<?, ?> comp : components.values()) {
            if (comp.getType().getKey().asString().contains(keyPart)) {
                return true;
            }
        }
        return false;
    }

    // Extract a single Component as plain text
    private String extractComponentText(Map<DataComponentType<?>, DataComponent<?, ?>> components, String keyPart) {
        for (DataComponent<?, ?> comp : components.values()) {
            if (comp.getType().getKey().asString().contains(keyPart)) {
                Object value = comp.getValue();
                if (value instanceof Component component) {
                    return toPlainText(component);
                } else if (value != null) {
                    return value.toString();
                }
            }
        }
        return null;
    }

    // Extract a list of Components as plain text
    private List<String> extractComponentTextList(Map<DataComponentType<?>, DataComponent<?, ?>> components, String keyPart) {
        for (DataComponent<?, ?> comp : components.values()) {
            if (comp.getType().getKey().asString().contains(keyPart)) {
                Object value = comp.getValue();
                if (value instanceof List<?> list) {
                    List<String> result = new ArrayList<>();
                    for (Object obj : list) {
                        if (obj instanceof Component component) {
                            result.add(toPlainText(component));
                        } else if (obj != null) {
                            result.add(obj.toString());
                        }
                    }
                    return result;
                }
            }
        }
        return Collections.emptyList();
    }

    // Recursively convert a Component to plain text
    private String toPlainText(Component component) {
        if (component == null) return "";
        StringBuilder builder = new StringBuilder();
        appendComponentText(component, builder);
        return builder.toString();
    }

    private void appendComponentText(Component component, StringBuilder builder) {
        if (component instanceof TextComponent text) {
            builder.append(text.content());
        }
        for (Component child : component.children()) {
            appendComponentText(child, builder);
        }
    }

    public HashedStack toHashedStack() {
        if (itemStack == null) return null;

        Map<DataComponentType<?>, Integer> addedComponents = new HashMap<>();
        Set<DataComponentType<?>> removedComponents = new HashSet<>();

        DataComponents data = itemStack.getDataComponentsPatch();
        if (data != null) {
            for (Map.Entry<DataComponentType<?>, DataComponent<?, ?>> entry : data.getDataComponents().entrySet()) {
                DataComponentType<?> type = entry.getKey();
                DataComponent<?, ?> comp = entry.getValue();

                Object value = comp.getValue();
                if (value instanceof Number number) {
                    addedComponents.put(type, number.intValue());
                }
            }
        }

        // If unbreakable is false, remove that component from the hash (only if data exists)
        if (!unbreakable && data != null) {
            data.getDataComponents().keySet().forEach(removedComponents::add);
        }

        return new HashedStack(id, amount, addedComponents, removedComponents);
    }

    @Override
    public String toString() {
        return "Item{" +
                "id=" + id +
                ", amount=" + amount +
                ", unbreakable=" + unbreakable +
                ", displayName='" + displayName + '\'' +
                ", lore=" + lore +
                '}';
    }
}
