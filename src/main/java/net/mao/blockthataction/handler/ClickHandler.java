package net.mao.blockthataction.handler;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.mao.blockthataction.BlockThatAction;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
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

            BlockPos pos = hitResult.getBlockPos();
            Block block = world.getBlockState(pos).getBlock();
            Identifier blockId = net.minecraft.registry.Registries.BLOCK.getId(block);

            if (blockId == null) {
                // BlockThatAction.LOGGER.info("blockId was null action result it's a pass");
                return ActionResult.PASS;
            }

            ItemStack stack = player.getStackInHand(hand);
            Item item = stack.getItem();
            boolean isHoldingTool = stack.isOf(net.minecraft.item.Items.STICK); // TODO: use config tool
            boolean isHoldingBlock = (item instanceof BlockItem);
            boolean isSneaking = player.isSneaking();

            // toggle restrictions ...
            if (isHoldingTool && isSneaking) {
                BlockThatAction.LOGGER.info("player is sneaking with tool in hand — blocking interaction & toggling config.");
                boolean restricted = !ModConfig.isRestricted(blockId);
                ModConfig.restrictBlock(player, blockId, restricted);

                if (world.isClient) { // display message client side!
                    BlockThatAction.LOGGER.info("world is client");

                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client.player != null) {
                        if (restricted) {
                            client.player.sendMessage(Text.literal("restricted block: " + blockId).formatted(Formatting.RED), true);
                        } else {
                            client.player.sendMessage(Text.literal("block: " + blockId + " is no longer restricted").formatted(Formatting.DARK_GREEN), true);
                        }
                    }

                } else { // display for client in-case they don't use mod!
                    BlockThatAction.LOGGER.info("world is not client");

                    if (player instanceof ServerPlayerEntity serverPlayer) {
                        BlockThatAction.LOGGER.info("player is instance of server player entity");
                        if (restricted) {
                            serverPlayer.sendMessage(Text.literal("restricted block: " + blockId).formatted(Formatting.RED), true);
                        } else {
                            serverPlayer.sendMessage(Text.literal("block: " + blockId + " is no longer restricted").formatted(Formatting.DARK_GREEN), true);
                        }

                    }

                }

                return ActionResult.FAIL; // block action both client and server
            }

            if (ModConfig.isRestricted(blockId)) {
                BlockThatAction.LOGGER.info("player right-clicked restricted block: " + blockId);

                // Check if player is sneaking (holding shift)
                if (isSneaking && isHoldingBlock) { // check if they want to place of the restricted block!
                    BlockThatAction.LOGGER.info("player is sneaking with block in hand — allowing interaction.");
                    return ActionResult.PASS; // Allow action
                }

                // Check if player is holding a stick
                if (isHoldingTool && !isSneaking) { // check if they want to interact with the block
                    BlockThatAction.LOGGER.info("player is holding a stick — allowing interaction.");
                    return ActionResult.PASS; // Allow action
                }

                return ActionResult.FAIL; // Block the action both client and server!

            } else {
                // do nothing it's not a block we care about
                // BlockThatAction.LOGGER.info("player right-clicked ordinary block: " + blockId);

                return ActionResult.PASS;
            }

        });
    }
}