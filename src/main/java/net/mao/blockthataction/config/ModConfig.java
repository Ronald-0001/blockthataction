package net.mao.blockthataction.config;

import net.mao.blockthataction.BlockThatAction;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.util.Identifier;

import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class ModConfig {

    private static final Set<Identifier> interactableTools = new HashSet<>();
    private static final Set<Identifier> restrictedBlocks = new HashSet<>();

    public static void addTool(Identifier itemId) {
        interactableTools.add(itemId);
        BlockThatAction.LOGGER.info("added interactable tool: " + itemId);
    }
    public static void removeTool(Identifier itemId) {
        interactableTools.remove(itemId);
        BlockThatAction.LOGGER.info("removed interactable tool: " + itemId);
    }
    public static void toggleTool(Identifier itemId) {
        if (interactableTools.contains(itemId)) {
            removeTool(itemId);
        } else {
            addTool(itemId);
        }
    }

    public static void addRestrictedBlock(Identifier blockId) {
        restrictedBlocks.add(blockId);
        BlockThatAction.LOGGER.info("added restricted block: " + blockId);
    }
    public static void removeRestrictedBlock(Identifier blockId) {
        restrictedBlocks.remove(blockId);
        BlockThatAction.LOGGER.info("removed restricted block: " + blockId);
    }
    public static void toggleRestrictedBlock(Identifier blockId) {
        if (restrictedBlocks.contains(blockId)) {
            removeRestrictedBlock(blockId);
        } else {
            addRestrictedBlock(blockId);
        }
    }

    public static void load() {
        BlockThatAction.LOGGER.info("loading mod config for " + BlockThatAction.MOD_ID);
        try (InputStreamReader reader = new InputStreamReader(
                Objects.requireNonNull(ModConfig.class.getClassLoader().getResourceAsStream("config/blockthataction.json")))) {

            Gson gson = new Gson();
            JsonObject obj = gson.fromJson(reader, JsonObject.class);

            obj.getAsJsonArray("interactable_tools").forEach(element -> {
                String id = element.getAsString();
                addTool(new Identifier(id));
            });

            obj.getAsJsonArray("restricted_blocks").forEach(element -> {
                String id = element.getAsString();
                addRestrictedBlock(new Identifier(id));
            });

            BlockThatAction.LOGGER.info("loaded config");

        } catch (Exception e) {
            BlockThatAction.LOGGER.info("failed to load config: " + e.getMessage());
        }
    }

    public static boolean isTool(Identifier itemId) {
        return interactableTools.contains(itemId);
    }
    public static boolean isRestricted(Identifier blockId) {
        return restrictedBlocks.contains(blockId);
    }
}
