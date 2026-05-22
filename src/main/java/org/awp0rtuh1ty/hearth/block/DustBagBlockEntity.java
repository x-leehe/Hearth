package org.awp0rtuh1ty.hearth.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.awp0rtuh1ty.hearth.AshStorage;
import org.awp0rtuh1ty.hearth.DustBag;
import org.awp0rtuh1ty.hearth.Slag;
import org.awp0rtuh1ty.hearth.WoodAsh;
import org.awp0rtuh1ty.hearth.screen.DustBagScreenHandler;
import org.jetbrains.annotations.Nullable;

import java.util.stream.IntStream;

public class DustBagBlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer {
    private final SimpleContainer inventory = new SimpleContainer(27) {
        @Override
        public void setChanged() {
            super.setChanged();
            DustBagBlockEntity.this.setChanged();
        }

        @Override
        public boolean stillValid(Player player) {
            return Container.stillValidBlockEntity(DustBagBlockEntity.this, player);
        }
    };

    public DustBagBlockEntity(BlockPos pos, BlockState state) {
        super(DustBag.DUST_BAG_BLOCK_ENTITY, pos, state);
    }

    public Container getInventory() {
        return this.inventory;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, DustBagBlockEntity be) {
        if (level.getGameTime() % 20 != 0) {
            return;
        }
        for (Direction dir : Direction.values()) {
            BlockEntity targetBe = level.getBlockEntity(pos.relative(dir));
            if (targetBe instanceof AshStorage ashStorage) {
                be.pullAsh(ashStorage);
            }
        }
    }

    private void pullAsh(AshStorage ashStorage) {
        pullSlot(ashStorage, true);
        pullSlot(ashStorage, false);
    }

    private void pullSlot(AshStorage ashStorage, boolean isSlot3) {
        ItemStack stack = isSlot3 ? ashStorage.hearth$getExtraSlot3() : ashStorage.hearth$getExtraSlot4();
        if (stack.isEmpty()) {
            return;
        }
        ItemStack remaining = insertAsh(stack.copy());
        int taken = stack.getCount() - remaining.getCount();
        if (taken <= 0) {
            return;
        }
        if (taken >= stack.getCount()) {
            if (isSlot3) {
                ashStorage.hearth$takeExtraSlot3();
            } else {
                ashStorage.hearth$takeExtraSlot4();
            }
        } else {
            if (isSlot3) {
                ashStorage.hearth$shrinkExtraSlot3(taken);
            } else {
                ashStorage.hearth$shrinkExtraSlot4(taken);
            }
        }
    }

    private ItemStack insertAsh(ItemStack stack) {
        if (!stack.is(WoodAsh.WOOD_ASH) && !stack.is(Slag.SLAG)) {
            return stack;
        }
        for (int i = 0; i < this.inventory.getContainerSize(); i++) {
            ItemStack slot = this.inventory.getItem(i);
            if (slot.isEmpty()) {
                this.inventory.setItem(i, stack);
                return ItemStack.EMPTY;
            }
            if (ItemStack.isSameItemSameComponents(slot, stack) && slot.getCount() < slot.getMaxStackSize()) {
                int canAdd = Math.min(stack.getCount(), slot.getMaxStackSize() - slot.getCount());
                slot.grow(canAdd);
                stack.shrink(canAdd);
                this.inventory.setChanged();
                if (stack.isEmpty()) {
                    return ItemStack.EMPTY;
                }
            }
        }
        return stack;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.hearth.dust_bag");
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new DustBagScreenHandler(syncId, playerInventory, this.inventory);
    }

    // --- saveToItem: 潜影盒式保留NBT ---

    public void saveToItem(ItemStack stack, HolderLookup.Provider registries) {
        CompoundTag blockEntityTag = new CompoundTag();
        NonNullList<ItemStack> items = NonNullList.withSize(this.inventory.getContainerSize(), ItemStack.EMPTY);
        for (int i = 0; i < this.inventory.getContainerSize(); i++) {
            items.set(i, this.inventory.getItem(i).copy());
        }
        ContainerHelper.saveAllItems(blockEntityTag, items, registries);
        if (!blockEntityTag.isEmpty()) {
            BlockItem.setBlockEntityData(stack, DustBag.DUST_BAG_BLOCK_ENTITY, blockEntityTag);
        }
    }

    // --- NBT save/load ---

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Items", 9)) {
            NonNullList<ItemStack> items = NonNullList.withSize(this.inventory.getContainerSize(), ItemStack.EMPTY);
            ContainerHelper.loadAllItems(tag, items, registries);
            for (int i = 0; i < items.size(); i++) {
                this.inventory.setItem(i, items.get(i));
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        NonNullList<ItemStack> items = NonNullList.withSize(this.inventory.getContainerSize(), ItemStack.EMPTY);
        for (int i = 0; i < this.inventory.getContainerSize(); i++) {
            items.set(i, this.inventory.getItem(i));
        }
        ContainerHelper.saveAllItems(tag, items, registries);
    }

    // --- WorldlyContainer (漏斗交互) ---

    @Override
    public int getContainerSize() {
        return this.inventory.getContainerSize();
    }

    @Override
    public boolean isEmpty() {
        return this.inventory.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.inventory.getItem(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return this.inventory.removeItem(slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return this.inventory.removeItemNoUpdate(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.inventory.setItem(slot, stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return this.inventory.stillValid(player);
    }

    @Override
    public void clearContent() {
        this.inventory.clearContent();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return IntStream.range(0, this.inventory.getContainerSize()).toArray();
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        return true;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return true;
    }
}
