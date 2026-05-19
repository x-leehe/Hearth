package org.awp0rtuh1ty.hearth.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import org.awp0rtuh1ty.hearth.Hearth;
import org.awp0rtuh1ty.hearth.potion.CleansingPotions;

import java.util.Map;
import java.util.Set;

public class CleansingCauldronBlock extends LayeredCauldronBlock {

    private static final Set<ResourceLocation> CLEANSING_POTION_IDS = Set.of(
            ResourceLocation.fromNamespaceAndPath(Hearth.MOD_ID, "cleansing"),
            ResourceLocation.fromNamespaceAndPath(Hearth.MOD_ID, "long_cleansing"),
            ResourceLocation.fromNamespaceAndPath(Hearth.MOD_ID, "strong_cleansing")
    );

    public static final CauldronInteraction.InteractionMap CLEANSING_INTERACTIONS =
            CauldronInteraction.newInteractionMap("cleansing");

    static {
        Map<Item, CauldronInteraction> map = CLEANSING_INTERACTIONS.map();

        // 玻璃瓶 → 装取荡涤药水
        map.put(Items.GLASS_BOTTLE, (state, level, pos, player, hand, stack) -> {
            if (!level.isClientSide) {
                ItemStack potionStack = PotionContents.createItemStack(
                        Items.POTION, CleansingPotions.CLEANSING);
                stack.shrink(1);
                if (stack.isEmpty()) {
                    player.setItemInHand(hand, potionStack);
                } else if (!player.getInventory().add(potionStack)) {
                    player.drop(potionStack, false);
                }
                LayeredCauldronBlock.lowerFillLevel(state, level, pos);
                level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                level.gameEvent(null, GameEvent.FLUID_PICKUP, pos);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        });

        // 荡涤药水（普通/喷溅/滞留）→ 填充炼药锅
        CauldronInteraction fillCauldron = (state, level, pos, player, hand, stack) -> {
            PotionContents contents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
            boolean isCleansing = contents.potion()
                    .map(h -> CLEANSING_POTION_IDS.contains(h.unwrapKey().map(k -> k.location()).orElse(null)))
                    .orElse(false);
            if (!isCleansing) {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
            if (!level.isClientSide) {
                int cur = state.getValue(LEVEL);
                if (cur < MAX_FILL_LEVEL) {
                    level.setBlock(pos, state.setValue(LEVEL, cur + 1), 3);
                    stack.shrink(1);
                    ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
                    if (stack.isEmpty()) {
                        player.setItemInHand(hand, bottle);
                    } else if (!player.getInventory().add(bottle)) {
                        player.drop(bottle, false);
                    }
                    level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
                    level.gameEvent(null, GameEvent.FLUID_PLACE, pos);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        };
        map.put(Items.POTION, fillCauldron);
        map.put(Items.SPLASH_POTION, fillCauldron);
        map.put(Items.LINGERING_POTION, fillCauldron);

        // 腐肉 → 皮革
        map.put(Items.ROTTEN_FLESH, (state, level, pos, player, hand, stack) -> {
            if (!level.isClientSide) {
                stack.shrink(1);
                ItemStack leather = new ItemStack(Items.LEATHER);
                if (!player.getInventory().add(leather)) {
                    player.drop(leather, false);
                }
                LayeredCauldronBlock.lowerFillLevel(state, level, pos);
                level.playSound(null, pos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 1.0F, 1.0F);
                level.gameEvent(null, GameEvent.FLUID_PICKUP, pos);
                ((ServerLevel) level).sendParticles(ParticleTypes.WAX_ON,
                        pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5,
                        10, 0.3, 0.2, 0.3, 0.05);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        });
    }

    public CleansingCauldronBlock(Properties properties) {
        super(Biome.Precipitation.NONE, CLEANSING_INTERACTIONS, properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        ItemInteractionResult result = super.useItemOn(stack, state, level, pos, player, hand, hitResult);
        if (result != ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) {
            return result;
        }
        Item cleaned = getCleanedItem(stack.getItem());
        if (cleaned == null || cleaned == stack.getItem()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            stack.shrink(1);
            ItemStack cleanedStack = new ItemStack(cleaned, 1);
            if (!player.getInventory().add(cleanedStack)) {
                player.drop(cleanedStack, false);
            }
            LayeredCauldronBlock.lowerFillLevel(state, level, pos);
            level.playSound(null, pos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.gameEvent(null, GameEvent.FLUID_PICKUP, pos);
            ((ServerLevel) level).sendParticles(ParticleTypes.WAX_ON,
                    pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5,
                    10, 0.3, 0.2, 0.3, 0.05);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    private static Item getCleanedItem(Item item) {
        String path = BuiltInRegistries.ITEM.getKey(item).getPath();
        if (path.endsWith("_wool") && !path.equals("white_wool")) return Items.WHITE_WOOL;
        if (path.endsWith("_carpet") && !path.equals("white_carpet")) return Items.WHITE_CARPET;
        if (path.endsWith("_stained_glass")) return Items.GLASS;
        if (path.endsWith("_stained_glass_pane")) return Items.GLASS_PANE;
        if (path.endsWith("_terracotta") && !path.equals("terracotta") && !path.contains("glazed")) return Items.TERRACOTTA;
        if (path.endsWith("_glazed_terracotta") && !path.equals("white_glazed_terracotta")) return Items.WHITE_GLAZED_TERRACOTTA;
        if (path.endsWith("_concrete") && !path.equals("white_concrete")) return Items.WHITE_CONCRETE;
        if (path.endsWith("_concrete_powder") && !path.equals("white_concrete_powder")) return Items.WHITE_CONCRETE_POWDER;
        if (path.endsWith("_shulker_box") && !path.equals("shulker_box")) return Items.SHULKER_BOX;
        if (path.endsWith("_candle") && !path.contains("cake") && !path.equals("candle")) return Items.CANDLE;
        if (path.endsWith("_banner") && !path.equals("white_banner")) return Items.WHITE_BANNER;
        return null;
    }
}
