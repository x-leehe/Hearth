package org.awp0rtuh1ty.hearth.screen;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.awp0rtuh1ty.hearth.Repellent;

public class RepellentScreenHandler extends AbstractContainerMenu {
    private final Container inventory;

    public RepellentScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(9));
    }

    public RepellentScreenHandler(int syncId, Inventory playerInventory, Container inventory) {
        super(Repellent.REPELLENT_SCREEN_HANDLER, syncId);
        this.inventory = inventory;
        inventory.startOpen(playerInventory.player);

        // 3x3 inventory slots (dispenser-style)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                this.addSlot(new Slot(inventory, col + row * 3, 62 + col * 18, 17 + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return isPotion(stack);
                    }
                });
            }
        }

        // Player inventory (3 rows of 9)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Hotbar
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    private static boolean isPotion(ItemStack stack) {
        return stack.getItem() == Items.POTION || stack.getItem() == Items.SPLASH_POTION
                || stack.getItem() == Items.LINGERING_POTION;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack copy = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack item = slot.getItem();
            copy = item.copy();
            if (index < 9) {
                // From block inventory → player inventory
                if (!this.moveItemStackTo(item, 9, 45, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (isPotion(item)) {
                // From player inventory → block inventory (only potions)
                if (!this.moveItemStackTo(item, 0, 9, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= 9 && index < 36) {
                // From main inventory → hotbar
                if (!this.moveItemStackTo(item, 36, 45, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= 36 && index < 45) {
                // From hotbar → main inventory
                if (!this.moveItemStackTo(item, 9, 36, false)) {
                    return ItemStack.EMPTY;
                }
            }
            if (item.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (item.getCount() == copy.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, item);
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.inventory.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.inventory.stopOpen(player);
    }
}
