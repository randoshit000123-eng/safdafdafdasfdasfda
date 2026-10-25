package de.challengemod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChallengeMod implements ModInitializer {

    public static final String MOD_ID = "challengemod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playS2C().register(BlindPayload.ID, BlindPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(RolesPayload.ID, RolesPayload.CODEC);

        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> ChallengeCommands.register(dispatcher));

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            ChallengeState.reset();
            ChallengeState.setServer(server);
        });
        ServerLifecycleEvents.SERVER_STARTED.register(ChallengeState::cleanupStaleTeams);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            ChallengeState.reset();
            ChallengeState.setServer(null);
        });

        ServerTickEvents.END_SERVER_TICK.register(ChallengeState::tick);

        ServerPlayConnectionEvents.JOIN.register(
                (handler, sender, server) -> ChallengeState.onJoin(handler.getPlayer()));

        LOGGER.info("Challenge Mod geladen.");
    }
}
