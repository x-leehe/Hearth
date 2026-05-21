package org.awp0rtuh1ty.hearth.block;

import com.mojang.serialization.DataResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.awp0rtuh1ty.hearth.PotionCauldron;

public class PotionCauldronBlockEntity extends BlockEntity {
    private static final String TAG_POTION = "potion_contents";
    private static final String TAG_LEVEL = "level";
    private static final String TAG_ITEM = "potion_item";

    private PotionContents potionContents = PotionContents.EMPTY;
    private int level = 1;
    private Item potionItem = Items.POTION;

    public PotionCauldronBlockEntity(BlockPos pos, BlockState state) {
        super(PotionCauldron.POTION_CAULDRON_BLOCK_ENTITY, pos, state);
    }

    public PotionContents getPotionContents() {
        return potionContents;
    }

    public void setPotionContents(PotionContents contents) {
        this.potionContents = contents;
        setChanged();
    }

    public int getFillLevel() {
        return level;
    }

    public void setFillLevel(int level) {
        this.level = level;
        setChanged();
    }

    public Item getPotionItem() {
        return potionItem;
    }

    public void setPotionItem(Item item) {
        this.potionItem = item;
        setChanged();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(TAG_POTION)) {
            DataResult<PotionContents> result = PotionContents.CODEC.parse(
                    registries.createSerializationContext(NbtOps.INSTANCE), tag.get(TAG_POTION));
            this.potionContents = result.result().orElse(PotionContents.EMPTY);
        }
        this.level = tag.getInt(TAG_LEVEL);
        if (this.level < 1) this.level = 1;
        if (tag.contains(TAG_ITEM)) {
            this.potionItem = BuiltInRegistries.ITEM.get(ResourceLocation.parse(tag.getString(TAG_ITEM)));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!potionContents.equals(PotionContents.EMPTY)) {
            DataResult<Tag> result = PotionContents.CODEC.encodeStart(
                    registries.createSerializationContext(NbtOps.INSTANCE), potionContents);
            result.result().ifPresent(encoded -> tag.put(TAG_POTION, encoded));
        }
        tag.putInt(TAG_LEVEL, level);
        tag.putString(TAG_ITEM, BuiltInRegistries.ITEM.getKey(potionItem).toString());
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }
}
