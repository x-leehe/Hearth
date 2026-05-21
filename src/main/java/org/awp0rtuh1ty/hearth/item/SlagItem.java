package org.awp0rtuh1ty.hearth.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class SlagItem extends Item {
    private static final TagKey<Item> NUGGETS_TAG = TagKey.create(
            BuiltInRegistries.ITEM.key(),
            ResourceLocation.fromNamespaceAndPath("c", "nuggets")
    );

    public SlagItem(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        ItemStack heldStack = context.getItemInHand();
        Player player = context.getPlayer();

        // 只在服务端处理，但客户端也要返回成功
        if (!(state.getBlock() instanceof LayeredCauldronBlock)) {
            return InteractionResult.PASS;
        }

        // 检查是不是水炼药锅（不是满的粉末雪炼药锅）
        if (!state.is(Blocks.WATER_CAULDRON)) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide) {
            return InteractionResult.sidedSuccess(true);
        }

        // 获取炼药锅当前水位
        int waterLevel = state.getValue(LayeredCauldronBlock.LEVEL);
        if (waterLevel <= 0) {
            return InteractionResult.PASS;
        }

        // 消耗1个炉渣
        heldStack.shrink(1);

        // 降低炼药锅水位
        LayeredCauldronBlock.lowerFillLevel(state, level, pos);

        // 随机获取 nuggets 中的物品 1~9 个
        ServerLevel serverLevel = (ServerLevel) level;
        List<Item> nuggetItems = new ArrayList<>();
        BuiltInRegistries.ITEM.getTagOrEmpty(NUGGETS_TAG).forEach(holder -> {
            nuggetItems.add(holder.value());
        });

        if (!nuggetItems.isEmpty()) {
            int count = serverLevel.getRandom().nextInt(1, 10); // 1~9
            Item randomNugget = nuggetItems.get(serverLevel.getRandom().nextInt(nuggetItems.size()));
            ItemStack result = new ItemStack(randomNugget, count);

            // 直接给玩家，如果背包满了就掉在地上
            if (player != null && !player.getInventory().add(result)) {
                ItemEntity itemEntity = new ItemEntity(level,
                        pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                        result);
                level.addFreshEntity(itemEntity);
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
