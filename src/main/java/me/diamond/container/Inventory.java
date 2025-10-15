package me.diamond.container;

import org.jetbrains.annotations.Range;

public interface Inventory extends Container {
    /**
     * Uses the selected item (Right clicks)
     */
    void useItemInMainHand();
    void selectHotbarSlot(@Range(from = 0, to = 8) int slot);
}
