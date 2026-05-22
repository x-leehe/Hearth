package org.awp0rtuh1ty.hearth.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.awp0rtuh1ty.hearth.DamageTracker;
import org.awp0rtuh1ty.hearth.HearthConfig;
import org.awp0rtuh1ty.hearth.HearthSounds;
import org.awp0rtuh1ty.hearth.Repellent;
import org.awp0rtuh1ty.hearth.screen.RepellentScreenHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

public class RepellentBlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer {
    private static final int REPEL_INTERVAL = 10;
    private static final int DAMAGE_MEMORY_TICKS = 100;
    private static final int LINE_OF_SIGHT_RANGE = 20;
    private static final int INVENTORY_SIZE = 9;
    private static final Set<String> CLEANSING_POTION_IDS = Set.of(
            "hearth:cleansing", "hearth:long_cleansing", "hearth:strong_cleansing");

    int remainingTicks;
    int potionCount;
    String potionVariant; // "normal", "long", "strong"
    boolean wasPowered;

    private final SimpleContainer inventory = new SimpleContainer(INVENTORY_SIZE) {
        @Override
        public void setChanged() {
            super.setChanged();
            RepellentBlockEntity.this.setChanged();
        }
    };

    public RepellentBlockEntity(BlockPos pos, BlockState state) {
        super(Repellent.REPELLENT_BLOCK_ENTITY, pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, RepellentBlockEntity be) {
        // Detect redstone state change and play enable/disable sounds (like beacon)
        boolean isPowered = level.hasNeighborSignal(pos);
        if (isPowered != be.wasPowered) {
            if (isPowered) {
                level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0F, 1.0F);
            } else {
                level.playSound(null, pos, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            be.wasPowered = isPowered;
        }

        // Try to consume fuel from inventory if empty
        if (be.remainingTicks <= 0) {
            be.tryConsumePotion();
        }

        // Periodically refresh potion count for syncing
        if (level.getGameTime() % 20 == 0) {
            int count = 0;
            for (int i = 0; i < INVENTORY_SIZE; i++) {
                if (isCleansingPotionItem(be.inventory.getItem(i))) {
                    count++;
                }
            }
            be.potionCount = count;
        }

        if (!isPowered) return;
        if (be.remainingTicks <= 0) return;

        be.remainingTicks--;

        // Sync remaining ticks to clients every second (20 ticks)
        if (be.remainingTicks % 20 == 0) {
            ClientboundBlockEntityDataPacket packet = ClientboundBlockEntityDataPacket.create(be);
            for (Player player : level.players()) {
                if (player instanceof ServerPlayer sp) {
                    sp.connection.send(packet);
                }
            }
        }

        // Periodic cleanup of stale damage records
        if (be.remainingTicks % 200 == 0) {
            DamageTracker.cleanup(level.getGameTime() - DAMAGE_MEMORY_TICKS * 2);
        }

        if (be.remainingTicks <= 0) {
            if (!HearthConfig.isDestroyEmptyBottles()) {
                be.returnEmptyBottle(level, pos);
            }
            be.potionVariant = null;
            be.setChanged();
            return;
        }

        if (level.getGameTime() % REPEL_INTERVAL != 0) return;

        int range = "strong".equals(be.potionVariant) ? 15 : 10;

        AABB box = new AABB(
                pos.getX() - range, pos.getY() - range, pos.getZ() - range,
                pos.getX() + range + 1, pos.getY() + range + 1, pos.getZ() + range + 1);

        List<? extends LivingEntity> monsters = level.getEntitiesOfClass(LivingEntity.class, box, e ->
                e instanceof Monster && HearthConfig.isEntityAffected(e.getType()));

        double centerX = pos.getX() + 0.5;
        double centerZ = pos.getZ() + 0.5;

        for (LivingEntity monster : monsters) {
            if (shouldSkip(level, monster)) continue;

            double dx = monster.getX() - centerX;
            double dz = monster.getZ() - centerZ;
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > 0.01) {
                monster.push(dx / dist * 0.5, 0.2, dz / dist * 0.5);
            }
        }
    }

    private void tryConsumePotion() {
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) continue;
            if (!isCleansingPotionItem(stack)) continue;

            PotionContents contents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
            String variant = contents.potion().map(holder ->
                    holder.unwrapKey().map(key -> {
                        String p = key.location().getPath();
                        if (p.contains("long")) return "long";
                        if (p.contains("strong")) return "strong";
                        return "normal";
                    }).orElse("normal")
            ).orElse("normal");

            int ticks = switch (variant) {
                case "long" -> 6000;
                case "strong" -> 1800;
                default -> 3600;
            };

            remainingTicks = ticks;
            potionVariant = variant;

            stack.shrink(1);
            if (stack.isEmpty()) {
                inventory.setItem(i, ItemStack.EMPTY);
            }
            setChanged();

            // Immediately sync new remainingTicks to clients
            ClientboundBlockEntityDataPacket packet = ClientboundBlockEntityDataPacket.create(this);
            for (Player player : level.players()) {
                if (player instanceof ServerPlayer sp) {
                    sp.connection.send(packet);
                }
            }
            return;
        }
    }

    private void returnEmptyBottle(Level level, BlockPos pos) {
        ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
        if (HearthConfig.getPotionStackSize() > 1) {
            Block.popResource(level, pos.above(), bottle);
            return;
        }
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            ItemStack slot = inventory.getItem(i);
            if (slot.is(Items.GLASS_BOTTLE) && slot.getCount() < slot.getMaxStackSize()) {
                slot.grow(1);
                return;
            }
        }
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            if (inventory.getItem(i).isEmpty()) {
                inventory.setItem(i, bottle);
                return;
            }
        }
        Block.popResource(level, pos.above(), bottle);
    }

    private static boolean isCleansingPotionItem(ItemStack stack) {
        if (stack.getItem() != Items.POTION && stack.getItem() != Items.SPLASH_POTION
                && stack.getItem() != Items.LINGERING_POTION) {
            return false;
        }
        PotionContents contents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        return contents.potion().map(holder ->
                holder.unwrapKey().map(key -> CLEANSING_POTION_IDS.contains(key.location().toString())).orElse(false)
        ).orElse(false);
    }

    private static boolean shouldSkip(Level level, LivingEntity monster) {
        AABB playerBox = new AABB(monster.blockPosition()).inflate(LINE_OF_SIGHT_RANGE);
        List<Player> nearbyPlayers = level.getEntitiesOfClass(Player.class, playerBox);
        for (Player player : nearbyPlayers) {
            if (monster.hasLineOfSight(player)) return true;
            if (DamageTracker.wasRecentlyDamagedBy(player, monster, DAMAGE_MEMORY_TICKS)) return true;
        }
        return false;
    }

    public boolean hasFuel() {
        return remainingTicks > 0;
    }

    public int getRemainingTicks() {
        return remainingTicks;
    }

    public int getPotionCount() {
        return potionCount;
    }

    public Container getInventory() {
        return inventory;
    }

    // --- MenuProvider ---

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.hearth.repellent");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new RepellentScreenHandler(syncId, playerInventory, inventory);
    }

    // --- NBT ---

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        remainingTicks = tag.getInt("remainingTicks");
        potionCount = tag.getInt("potionCount");
        if (tag.contains("potionVariant")) {
            potionVariant = tag.getString("potionVariant");
        }
        wasPowered = tag.getBoolean("wasPowered");
        NonNullList<ItemStack> items = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        for (int i = 0; i < items.size(); i++) {
            inventory.setItem(i, items.get(i));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("remainingTicks", remainingTicks);
        tag.putInt("potionCount", potionCount);
        if (potionVariant != null) {
            tag.putString("potionVariant", potionVariant);
        }
        tag.putBoolean("wasPowered", wasPowered);
        NonNullList<ItemStack> items = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            items.set(i, inventory.getItem(i));
        }
        ContainerHelper.saveAllItems(tag, items, registries);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("remainingTicks", remainingTicks);
        tag.putInt("potionCount", potionCount);
        if (potionVariant != null) {
            tag.putString("potionVariant", potionVariant);
        }
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // --- WorldlyContainer (hopper interaction) ---

    @Override
    public int getContainerSize() {
        return INVENTORY_SIZE;
    }

    @Override
    public boolean isEmpty() {
        return inventory.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return inventory.getItem(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return inventory.removeItem(slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return inventory.removeItemNoUpdate(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        inventory.setItem(slot, stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        inventory.clearContent();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return IntStream.range(0, INVENTORY_SIZE).toArray();
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        return isCleansingPotionItem(stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return !isCleansingPotionItem(stack);
    }
}
