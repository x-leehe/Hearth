package org.awp0rtuh1ty.hearth.block;

import com.mojang.serialization.DataResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.awp0rtuh1ty.hearth.PotionCauldron;

public class PotionCauldronBlockEntity extends BlockEntity {
    private static final String TAG_POTION = "potion_contents";
    private static final String TAG_LEVEL = "level";

    private PotionContents potionContents = PotionContents.EMPTY;
    private int level = 1;

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
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }
}
