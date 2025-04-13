package net.mao.blockthataction;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.mao.blockthataction.config.ModConfig;
import net.minecraft.util.Identifier;

public class BlockThatActionClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(ModConfig.CONFIG_SYNC_PACKET_ID,
                (client, handler, buf, responseSender) -> {
                    String json = buf.readString();
                    JsonObject configJson = new Gson().fromJson(json, JsonObject.class);

                    client.execute(() -> {
                        ModConfig.loadServerConfig(configJson); // This will override the local config
                        ModConfig.usingServerConfig = true;
                    });
                }
        );

        ClientPlayNetworking.registerGlobalReceiver(ModConfig.RESTRICTION_SYNC_PACKET_ID,
                (client, handler, buf, responseSender) -> {
                    String blockId = buf.readString();
                    boolean restricted = buf.readBoolean();

                    client.execute(() -> {
                        if (restricted) {
                            ModConfig.addRestrictedBlock(new Identifier(blockId));
                        } else {
                            ModConfig.removeRestrictedBlock(new Identifier(blockId));
                        }
                    });
                }
        );
    }
}
