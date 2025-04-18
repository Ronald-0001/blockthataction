package net.mao.blockthataction;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.mao.blockthataction.config.ModConfig;
import net.mao.blockthataction.handler.ClickHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlockThatAction implements ModInitializer {
	public static final String MOD_ID = "blockthataction";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("Hello Fabric world!");

		ModConfig.isClient = FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
		ModConfig.isServer = FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER;

		LOGGER.info("we are client: " + ModConfig.isClient + " server: " + ModConfig.isServer);

		ModConfig.registerClientSync();
		ModConfig.load();
		ClickHandler.register();
	}
}