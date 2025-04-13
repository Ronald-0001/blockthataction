package net.mao.blockthataction.config;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.mao.blockthataction.BlockThatAction;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.Set;

public class ModConfig {

    public static boolean isClient = false;
    public static boolean isServer = false;

    public static final Identifier CONFIG_SYNC_PACKET_ID = new Identifier(BlockThatAction.MOD_ID, "config_sync");
    public static final Identifier RESTRICTION_SYNC_PACKET_ID = new Identifier(BlockThatAction.MOD_ID, "block_restriction_sync");
    public static boolean usingServerConfig = false;
    public static boolean isSingleplayer = true;

    private static final Set<Identifier> interactableTools = new HashSet<>();
    private static final Set<Identifier> restrictedBlocks = new HashSet<>();

    /*
    public static void addTool(Identifier itemId) {
        interactableTools.add(itemId);
        BlockThatAction.LOGGER.info("added interactable tool: " + itemId);
        save();
    }
    public static void removeTool(Identifier itemId) {
        interactableTools.remove(itemId);
        BlockThatAction.LOGGER.info("removed interactable tool: " + itemId);
        save();
    }
    public static void toggleTool(Identifier itemId) {
        if (interactableTools.contains(itemId)) {
            removeTool(itemId);
        } else {
            addTool(itemId);
        }
    }
    public static boolean isTool(Identifier itemId) {
        return interactableTools.contains(itemId);
    }
     */

    public static void addRestrictedBlock(Identifier blockId) {
        if (restrictedBlocks.add(blockId)) {
            BlockThatAction.LOGGER.info("added restricted block: " + blockId);
            save();
        }
    }
    public static void removeRestrictedBlock(Identifier blockId) {
        if (restrictedBlocks.remove(blockId)) {
            BlockThatAction.LOGGER.info("removed restricted block: " + blockId);
            save();
        }
    }
    public static boolean isRestricted(Identifier blockId) {
        return restrictedBlocks.contains(blockId);
    }

    // updates the isSingleplayer variable
    public static boolean CheckSingleplayerMode() {
        if (!isClient) return false; // if not client event it cant be singleplayer!

        isSingleplayer = MinecraftClient.getInstance().isConnectedToLocalServer();
        BlockThatAction.LOGGER.info("it's time again to play! ARE WE SINGLE-PLAYER!!!! : " + isSingleplayer);
        return isSingleplayer;
    }

    // construct the default config
    public static @NotNull JsonObject getDefault() {
        Gson gson = new Gson();
        // Create default config JSON
        JsonObject defaultConfig = new JsonObject();
        defaultConfig.add("interactable_tools", gson.toJsonTree(new String[] {
                "minecraft:stick"
                // You can change this default
        }));
        defaultConfig.add("restricted_blocks", gson.toJsonTree(new String[] {
                "conquest:ash_wood_beam_door_frame_lintel",
                "conquest:ash_wood_beam_lintel"
                // You can change this default too
        }));
        return defaultConfig;
    }
    // convert stored config into json obj
    public static @NotNull JsonObject toJson() {
        Gson gson = new Gson();
        JsonObject jsonConfig = new JsonObject();
        jsonConfig.add("interactable_tools", gson.toJsonTree(
                interactableTools.stream().map(Identifier::toString).toArray(String[]::new))
        );
        jsonConfig.add("restricted_blocks", gson.toJsonTree(
                restrictedBlocks.stream().map(Identifier::toString).toArray(String[]::new))
        );
        return jsonConfig;
    }

    // informs ALL client about a new block restriction rule
    public static void SyncBlockRestriction(ServerPlayerEntity serverPlayer, Identifier blockId, boolean restricted) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(blockId.toString());
        buf.writeBoolean(restricted);

        // Iterate through all connected players and send the packet
        for (ServerPlayerEntity player : serverPlayer.server.getPlayerManager().getPlayerList()) {
            // if (!player.getUuid().equals(serverPlayer.getUuid())) {
                MutableText message = Text.literal("").append(serverPlayer.getDisplayName()); // Properly clickable & hoverable name
                if (restricted) {
                    message.append(Text.literal(" has restricted block: ").formatted(Formatting.RED));
                } else {
                    message.append(Text.literal(" has removed restrictions on block: ").formatted(Formatting.WHITE));
                }
                message.append(Text.literal(blockId.toString()).formatted(Formatting.WHITE));

                player.sendMessage(message, false);
            // }
            ServerPlayNetworking.send(player, RESTRICTION_SYNC_PACKET_ID, buf);  // Send the packet to each client
        }
    }
    // creates server side handlers
    public static void registerClientSync() {
        // sends current config to new clients
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {

            BlockThatAction.LOGGER.info("sending config to client: " + handler.player.getName().getString());

            JsonObject configJson = toJson(); // Already implemented
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeString(configJson.toString());

            ServerPlayNetworking.send(handler.player, CONFIG_SYNC_PACKET_ID, buf);
        });

        // handel's new restriction rule changes for clients that use the mod
        ServerPlayNetworking.registerGlobalReceiver(RESTRICTION_SYNC_PACKET_ID,
            (server, player, handler, buf, sender) -> {
                String blockId = buf.readString();
                boolean restricted = buf.readBoolean();

                server.execute(() -> {
                    restrictBlock(player, new Identifier(blockId), restricted);
                });
            });
    }
    // global restricts function handel's code based of it's the server or client
    public static void restrictBlock(PlayerEntity player, Identifier blockId, boolean restricted) {
        if (isClient) {
            if (!isSingleplayer && usingServerConfig) {
                BlockThatAction.LOGGER.info("client to server block restriction toggle");
                // tell the server?
                PacketByteBuf buf = PacketByteBufs.create();
                buf.writeString(blockId.toString());
                buf.writeBoolean(restricted);

                ClientPlayNetworking.send(RESTRICTION_SYNC_PACKET_ID, buf);
            }
            else {
                BlockThatAction.LOGGER.info("client only block restriction toggle");
                if (restricted) {
                    addRestrictedBlock(blockId);
                } else {
                    removeRestrictedBlock(blockId);
                }
            }
            return;
        }

        BlockThatAction.LOGGER.info("server block restriction toggle");
        if (player instanceof ServerPlayerEntity serverPlayer) {
            BlockThatAction.LOGGER.info("of instance");
            if (serverPlayer.hasPermissionLevel(4)) {
                BlockThatAction.LOGGER.info("has perms");
                // Level	Description
                // 0	All players (default)
                // 1	Can use limited commands
                // 2	Can use more admin commands /gamemode, etc.
                // 3	Can kick/ban, manage others
                // 4	Full access (server owner level)

                if (restricted) {
                    addRestrictedBlock(blockId);
                } else {
                    removeRestrictedBlock(blockId);
                }

                ModConfig.SyncBlockRestriction(serverPlayer, blockId, restricted);
            }
        }
    }
    // loads the config given by the server
    public static void loadServerConfig(JsonObject jsonConfig) {
        usingServerConfig = true;
        CheckSingleplayerMode(); // update if singleplayer

        BlockThatAction.LOGGER.info("loading server config...");

        loadFromJson(jsonConfig);
    }
    // loads a json into config
    public static void loadFromJson(@NotNull JsonObject jsonConfig) {

        interactableTools.clear();
        restrictedBlocks.clear();

        jsonConfig.getAsJsonArray("interactable_tools").forEach(element -> {
            String id = element.getAsString();
            interactableTools.add(new Identifier(id));
        });

        jsonConfig.getAsJsonArray("restricted_blocks").forEach(element -> {
            String id = element.getAsString();
            restrictedBlocks.add(new Identifier(id));
        });

        BlockThatAction.LOGGER.info("Successfully loaded config.");
    }
    // loads the config file and creates default if it doesn't exist
    public static void load() {
        BlockThatAction.LOGGER.info("Loading config for " + BlockThatAction.MOD_ID);
        CheckSingleplayerMode(); // update if singleplayer

        Gson gson = new Gson();
        File configFile = new File("config/blockthataction.json");

        if (!configFile.exists()) {
            BlockThatAction.LOGGER.info("Config file not found — creating default config.");

            // Create default config JSON
            JsonObject defaultConfig = getDefault();
            try (FileWriter writer = new FileWriter(configFile)) {
                gson.toJson(defaultConfig, writer);
            } catch (Exception e) {
                BlockThatAction.LOGGER.error("Failed to write default config: " + e.getMessage());
                return;
            }
        }

        // Load existing config
        try (FileReader reader = new FileReader(configFile)) {
            JsonObject obj = gson.fromJson(reader, JsonObject.class);
            loadFromJson(obj);
        } catch (Exception e) {
            BlockThatAction.LOGGER.error("Failed to load config: " + e.getMessage());
        }
    }
    // saves config into file
    public static void save() {

        BlockThatAction.LOGGER.info("Validating before save! \nisClient: " + isClient + " isServer: " + isServer + " usingServerConfig: " + usingServerConfig);
        BlockThatAction.LOGGER.info("are we playing single-player: " + isSingleplayer);
        if (!isSingleplayer && usingServerConfig) { return; } // if where on a multiplayer that is sync config don't save to client
        BlockThatAction.LOGGER.info("Saving config to config/blockthataction.json");

        Gson gson = new Gson();
        File configFile = new File("config/blockthataction.json");

        JsonObject jsonConfig = toJson();
        try (FileWriter writer = new FileWriter(configFile)) {
            gson.toJson(jsonConfig, writer);
            BlockThatAction.LOGGER.info("Config saved successfully.");
        } catch (Exception e) {
            BlockThatAction.LOGGER.error("Failed to save config: " + e.getMessage());
        }
    }
}
