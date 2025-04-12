package net.mao.blockthataction.handler;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.mao.blockthataction.BlockThatAction;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.mao.blockthataction.config.ModConfig;

public class ClickHandler {

    public static void register() {
        BlockThatAction.LOGGER.info("adding UseBlockCallback.EVENT in ClickHandler");

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            // if (world.isClient) return ActionResult.PASS; // Only server side

            BlockPos pos = hitResult.getBlockPos();
            Block block = world.getBlockState(pos).getBlock();
            Identifier blockId = net.minecraft.registry.Registries.BLOCK.getId(block);

            if (blockId == null) return ActionResult.PASS;

            ItemStack stack = player.getStackInHand(hand);
            Item item = stack.getItem();
            boolean isHoldingTool = stack.isOf(net.minecraft.item.Items.STICK);
            boolean isHoldingBlock = (item instanceof BlockItem);
            boolean isSneaking = player.isSneaking();

            // toggle restrictions ...
            if (isHoldingTool && isSneaking) {
                BlockThatAction.LOGGER.info("player is sneaking with tool in hand — blocking interaction & toggling config.");

                // toggle block as restricted + update config!!!
                ModConfig.toggleRestrictedBlock(blockId);

                if (!world.isClient) { // Only server side
                    if (player instanceof ServerPlayerEntity serverPlayer) {
                        if (ModConfig.isRestricted(blockId)) {
                            serverPlayer.sendMessage(Text.literal("restricted block: " + blockId), true);
                        } else {
                            serverPlayer.sendMessage(Text.literal("block: " + blockId + " is no longer restricted"), true);
                        }
                    }
                }

                return ActionResult.FAIL; // block action
            }

            if (ModConfig.isRestricted(blockId)) {
                BlockThatAction.LOGGER.info("player right-clicked lockable block: " + blockId);

                // Check if player is sneaking (holding shift)
                if (isSneaking && isHoldingBlock) {
                    BlockThatAction.LOGGER.info("player is sneaking with block in hand — allowing interaction.");
                    return ActionResult.PASS; // Allow action
                }

                // Check if player is holding a stick
                if (isHoldingTool && !isSneaking) {
                    BlockThatAction.LOGGER.info("player is holding a stick — allowing interaction.");
                    return ActionResult.PASS; // Allow action
                }

                return ActionResult.FAIL; // Block the action

            } else {
                BlockThatAction.LOGGER.info("player right-clicked ordinary block: " + blockId);
            }

            return ActionResult.PASS;
        });
    }
}