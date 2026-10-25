package de.challengemod.client;

import de.challengemod.BlindPayload;
import de.challengemod.RolesPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class ChallengeModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(BlindPayload.ID,
                (payload, context) -> BlindClient.apply(payload.blind(), payload.radius()));

        ClientPlayNetworking.registerGlobalReceiver(RolesPayload.ID,
                (payload, context) -> RoleHud.set(payload.blind(), payload.mute(), payload.deaf()));

        RoleHud.register();

        // Beim Verlassen des Servers wieder alles zuruecksetzen
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            BlindClient.reset();
            RoleHud.reset();
        });
    }
}
