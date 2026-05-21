package org.awp0rtuh1ty.hearth.mixin;

import org.awp0rtuh1ty.hearth.AshStorage;
import org.awp0rtuh1ty.hearth.Chimney;
import org.awp0rtuh1ty.hearth.HearthConfig;
import org.awp0rtuh1ty.hearth.HearthLogConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin implements AshStorage {
    @Unique
    private static final Logger HEARTH_LOGGER = LoggerFactory.getLogger("Hearth");

    @Unique
    private static final int HEARTH_MAX_ASH_COUNT = 128;

    @Unique
    private ItemStack hearth$slot3 = ItemStack.EMPTY;

    @Unique
    private ItemStack hearth$slot4 = ItemStack.EMPTY;

    @Unique
    private boolean hearth$recipeJustCompleted;

    @Unique
    private boolean hearth$penaltyApplied;

    @Shadow
    int cookingTotalTime;

    @Shadow
    int cookingProgress;

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void hearth$loadAshCount(CompoundTag compoundTag, HolderLookup.Provider provider, CallbackInfo ci) {
        if (compoundTag.contains("hearth_ashSlot3", 10)) {
            this.hearth$slot3 = ItemStack.parseOptional(provider, compoundTag.getCompound("hearth_ashSlot3"));
        }
        if (compoundTag.contains("hearth_ashSlot4", 10)) {
            this.hearth$slot4 = ItemStack.parseOptional(provider, compoundTag.getCompound("hearth_ashSlot4"));
        }
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void hearth$saveAshCount(CompoundTag compoundTag, HolderLookup.Provider provider, CallbackInfo ci) {
        boolean log = HearthLogConfig.isLoggingEnabled();
        if (this.hearth$slot3 != null && !this.hearth$slot3.isEmpty()) {
            CompoundTag tag = (CompoundTag) this.hearth$slot3.save(provider);
            compoundTag.put("hearth_ashSlot3", tag);
            if (log) {
                HEARTH_LOGGER.info("[Hearth] Saving slot3: {}", tag);
            }
        }
        if (this.hearth$slot4 != null && !this.hearth$slot4.isEmpty()) {
            CompoundTag tag = (CompoundTag) this.hearth$slot4.save(provider);
            compoundTag.put("hearth_ashSlot4", tag);
            if (log) {
                HEARTH_LOGGER.info("[Hearth] Saving slot4: {}", tag);
            }
        }
    }

    @Inject(
            method = "serverTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;setRecipeUsed(Lnet/minecraft/world/item/crafting/RecipeHolder;)V",
                    shift = At.Shift.AFTER
            )
    )
    private static void hearth$markRecipeCompleted(Level level, BlockPos blockPos, BlockState blockState, AbstractFurnaceBlockEntity furnace, CallbackInfo ci) {
        AbstractFurnaceBlockEntityMixin mixin = (AbstractFurnaceBlockEntityMixin) (Object) furnace;
        mixin.hearth$recipeJustCompleted = true;
        boolean log = HearthLogConfig.isLoggingEnabled();

        int ashToAdd = mixin.hearth$getAshPerRecipe(furnace);
        Item byproductItem = mixin.hearth$getByproductItem(furnace);

        if (hearth$hasAdjacentChimney(level, blockPos)) {
            if (log) {
                HEARTH_LOGGER.info("[Hearth] Adjacent chimney detected, skipping byproduct generation");
            }
            return;
        }

        if (log) {
            HEARTH_LOGGER.info("[Hearth] Recipe completed! Furnace type: {}, byproduct: {}, amount: {}",
                    furnace.getClass().getSimpleName(), byproductItem, ashToAdd);
        }
        if (ashToAdd > 0 && byproductItem != null && !mixin.hearth$isExtraSlotsFull(byproductItem)) {
            int remaining = ashToAdd;
            remaining = mixin.hearth$insertByproductIntoSlot(remaining, 3, byproductItem);
            remaining = mixin.hearth$insertByproductIntoSlot(remaining, 4, byproductItem);
            if (log) {
                HEARTH_LOGGER.info("[Hearth] After insertion - slot3: {}, slot4: {}, remaining: {}", mixin.hearth$slot3, mixin.hearth$slot4, remaining);
            }
            if (remaining != ashToAdd) {
                furnace.setChanged();
            }
        }
    }

    @Inject(method = "serverTick", at = @At("TAIL"))
    private static void hearth$handleAsh(Level level, BlockPos blockPos, BlockState blockState, AbstractFurnaceBlockEntity furnace, CallbackInfo ci) {
        AbstractFurnaceBlockEntityMixin mixin = (AbstractFurnaceBlockEntityMixin) (Object) furnace;
        Item byproductItem = mixin.hearth$getByproductItem(furnace);
        if (byproductItem != null && mixin.hearth$isExtraSlotsFull(byproductItem)) {
            if (!mixin.hearth$penaltyApplied && mixin.cookingTotalTime > 0 && mixin.cookingProgress == 0) {
                mixin.cookingTotalTime += 20;
                mixin.hearth$penaltyApplied = true;
            }
        } else {
            mixin.hearth$penaltyApplied = false;
        }

        if (mixin.hearth$recipeJustCompleted) {
            mixin.hearth$recipeJustCompleted = false;
            mixin.hearth$penaltyApplied = false;
        }
    }

    @Unique
    private int hearth$getAshPerRecipe(AbstractFurnaceBlockEntity furnace) {
        return HearthConfig.getByproductCount();
    }

    @Unique
    private Item hearth$getByproductItem(AbstractFurnaceBlockEntity furnace) {
        ResourceLocation id = HearthConfig.getByproductItem();
        if (id != null) {
            return BuiltInRegistries.ITEM.get(id);
        }
        return null;
    }

    @Unique
    private static boolean hearth$hasAdjacentChimney(Level level, BlockPos blockPos) {
        for (Direction dir : Direction.values()) {
            if (level.getBlockState(blockPos.relative(dir)).is(Chimney.CHIMNEY_BLOCK)) {
                return true;
            }
        }
        return false;
    }

    @Unique
    private boolean hearth$isExtraSlotsFull(Item byproductItem) {
        int cap = HEARTH_MAX_ASH_COUNT / 2;
        boolean s3Full = this.hearth$slot3 != null && !this.hearth$slot3.isEmpty()
                && this.hearth$slot3.is(byproductItem) && this.hearth$slot3.getCount() >= cap;
        boolean s4Full = this.hearth$slot4 != null && !this.hearth$slot4.isEmpty()
                && this.hearth$slot4.is(byproductItem) && this.hearth$slot4.getCount() >= cap;
        return s3Full && s4Full;
    }

    @Unique
    private int hearth$insertByproductIntoSlot(int amount, int slotIndex, Item byproductItem) {
        int perStackCap = HEARTH_MAX_ASH_COUNT / 2;
        if (amount <= 0) {
            return 0;
        }

        ItemStack slotStack = slotIndex == 3 ? this.hearth$slot3 : this.hearth$slot4;
        if (slotStack == null || slotStack.isEmpty()) {
            int toPut = Math.min(amount, perStackCap);
            ItemStack newStack = new ItemStack(byproductItem, toPut);
            if (slotIndex == 3) {
                this.hearth$slot3 = newStack;
            } else {
                this.hearth$slot4 = newStack;
            }
            return amount - toPut;
        }

        if (!slotStack.is(byproductItem)) {
            return amount;
        }

        int canAdd = perStackCap - slotStack.getCount();
        if (canAdd <= 0) {
            return amount;
        }

        int toAdd = Math.min(canAdd, amount);
        slotStack.grow(toAdd);
        if (slotIndex == 3) {
            this.hearth$slot3 = slotStack;
        } else {
            this.hearth$slot4 = slotStack;
        }
        return amount - toAdd;
    }

    @Override
    public ItemStack hearth$getExtraSlot3() {
        return this.hearth$slot3 == null ? ItemStack.EMPTY : this.hearth$slot3.copy();
    }

    @Override
    public ItemStack hearth$getExtraSlot4() {
        return this.hearth$slot4 == null ? ItemStack.EMPTY : this.hearth$slot4.copy();
    }

    @Override
    public ItemStack hearth$takeExtraSlot3() {
        ItemStack out = this.hearth$slot3 == null ? ItemStack.EMPTY : this.hearth$slot3.copy();
        this.hearth$slot3 = ItemStack.EMPTY;
        return out;
    }

    @Override
    public ItemStack hearth$takeExtraSlot4() {
        ItemStack out = this.hearth$slot4 == null ? ItemStack.EMPTY : this.hearth$slot4.copy();
        this.hearth$slot4 = ItemStack.EMPTY;
        return out;
    }

    @Override
    public void hearth$shrinkExtraSlot3(int amount) {
        if (this.hearth$slot3 != null && !this.hearth$slot3.isEmpty()) {
            this.hearth$slot3.shrink(amount);
            if (this.hearth$slot3.isEmpty()) {
                this.hearth$slot3 = ItemStack.EMPTY;
            }
        }
    }

    @Override
    public void hearth$shrinkExtraSlot4(int amount) {
        if (this.hearth$slot4 != null && !this.hearth$slot4.isEmpty()) {
            this.hearth$slot4.shrink(amount);
            if (this.hearth$slot4.isEmpty()) {
                this.hearth$slot4 = ItemStack.EMPTY;
            }
        }
    }
}
