package org.awp0rtuh1ty.hearth.mixin;

import org.awp0rtuh1ty.hearth.AshStorage;
import org.awp0rtuh1ty.hearth.HearthLogConfig;
import org.awp0rtuh1ty.hearth.HearthSounds;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.sounds.SoundSource;

@Mixin(AbstractFurnaceBlock.class)
public abstract class AbstractFurnaceBlockMixin {
    private static final Logger HEARTH_LOGGER = LoggerFactory.getLogger("Hearth");

    private static boolean inventoryHasSpaceFor(Player player, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        int remaining = stack.getCount();
        Inventory inventory = player.getInventory();
        for (ItemStack slot : inventory.items) {
            if (slot.isEmpty()) {
                remaining -= stack.getMaxStackSize();
            } else if (ItemStack.isSameItemSameComponents(slot, stack) && slot.isStackable()) {
                remaining -= stack.getMaxStackSize() - slot.getCount();
            }
            if (remaining <= 0) {
                return true;
            }
        }
        return remaining <= 0;
    }

    @Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
    private void hearth$takeAshWithShovel(BlockState blockState, Level level, BlockPos blockPos, Player player, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        // 根据 config/hearth.json 判断是否开启日志
        boolean log = HearthLogConfig.isLoggingEnabled();
        ItemStack heldStack = player.getMainHandItem();
        if (log) {
            // 记录玩家是否手持铲子进入了 useWithoutItem 方法
            HEARTH_LOGGER.info("[Hearth] useWithoutItem called on furnace - holding shovel: {}", heldStack.is(ItemTags.SHOVELS));
        }

        // 如果玩家没有手持铲子，则不拦截这个交互
        if (!heldStack.is(ItemTags.SHOVELS)) {
            return;
        }

        // 获取方块实体并检查是否为炉子实体且实现了 AshStorage 接口
        BlockEntity blockEntity = level.getBlockEntity(blockPos);
        if (log) {
            HEARTH_LOGGER.info("[Hearth] Block entity: {}, is AbstractFurnaceBlockEntity: {}, is AshStorage: {}", 
                blockEntity != null ? blockEntity.getClass().getSimpleName() : "null",
                blockEntity instanceof AbstractFurnaceBlockEntity,
                blockEntity instanceof AshStorage);
        }
        
        // 只有当方块实体既是 AbstractFurnaceBlockEntity 又实现 AshStorage 时才继续处理
        if (!(blockEntity instanceof AbstractFurnaceBlockEntity furnace) || !(furnace instanceof AshStorage ashStorage)) {
            return;
        }

        // 读取炉子额外的两个灰烬槽位内容
        net.minecraft.world.item.ItemStack slot3 = ashStorage.hearth$getExtraSlot3();
        net.minecraft.world.item.ItemStack slot4 = ashStorage.hearth$getExtraSlot4();
        if (log) {
            HEARTH_LOGGER.info("[Hearth] Retrieved slots - slot3: {}, slot4: {}", slot3, slot4);
        }
        
        // 如果两个槽都为空，则无需进一步处理
        if ((slot3 == null || slot3.isEmpty()) && (slot4 == null || slot4.isEmpty())) {
            if (log) {
                HEARTH_LOGGER.info("[Hearth] Both slots empty, returning");
            }
            return;
        }

        // 客户端根据背包是否有空间决定是否拦截交互
        if (level.isClientSide) {
            boolean canTake = (slot3 != null && !slot3.isEmpty() && inventoryHasSpaceFor(player, slot3))
                    || (slot4 != null && !slot4.isEmpty() && inventoryHasSpaceFor(player, slot4));
            if (log) {
                HEARTH_LOGGER.info("[Hearth] Client side, canTake: {}", canTake);
            }
            if (canTake) {
                cir.setReturnValue(InteractionResult.SUCCESS);
            }
            return;
        }

        // 服务器端继续执行，将灰烬槽物品转移给玩家
        if (log) {
            HEARTH_LOGGER.info("[Hearth] Server side - transferring items to player");
        }
        boolean anyTransferred = false;
        if (slot3 != null && !slot3.isEmpty()) {
            ItemStack copy = slot3.copy();
            if (inventoryHasSpaceFor(player, copy)) {
                player.getInventory().add(copy);
                ashStorage.hearth$takeExtraSlot3();
                anyTransferred = true;
                level.playSound(null, blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5,
                    HearthSounds.WOOD_ASH_SHOVEL,
                    SoundSource.PLAYERS, 1.0F, 1.0F);
                if (log) {
                    HEARTH_LOGGER.info("[Hearth] Transferred slot3: {}", slot3);
                }
            } else if (log) {
                HEARTH_LOGGER.info("[Hearth] No inventory space for slot3, skipping");
            }
        }
        if (slot4 != null && !slot4.isEmpty()) {
            ItemStack copy = slot4.copy();
            if (inventoryHasSpaceFor(player, copy)) {
                player.getInventory().add(copy);
                ashStorage.hearth$takeExtraSlot4();
                anyTransferred = true;
                level.playSound(null, blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5,
                    HearthSounds.WOOD_ASH_SHOVEL,
                    SoundSource.PLAYERS, 1.0F, 1.0F);
                if (log) {
                    HEARTH_LOGGER.info("[Hearth] Transferred slot4: {}", slot4);
                }
            } else if (log) {
                HEARTH_LOGGER.info("[Hearth] No inventory space for slot4, skipping");
            }
        }

        if (anyTransferred) {
            furnace.setChanged();
            cir.setReturnValue(InteractionResult.CONSUME);
            if (log) {
                HEARTH_LOGGER.info("[Hearth] Interaction complete, returning CONSUME");
            }
        }
    }
}
