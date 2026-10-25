package de.challengemod.client;

import net.minecraft.client.MinecraftClient;

/** Client-Zustand fuer den Blind-Modus. Aenderungen laden die Ressourcen neu (Texturen werden dabei umgefaerbt). */
public final class BlindClient {

    /** Wird vom Mixin beim Laden der Block-Texturen gelesen. */
    public static volatile boolean active = false;

    /** Sichtradius in Bloecken (Vanilla-Blindness: 5). Wird vom Nebel-Mixin gelesen. */
    public static volatile float radius = 2.5f;

    private BlindClient() {
    }

    public static void apply(boolean on, float newRadius) {
        radius = newRadius; // der Nebel liest den Wert jedes Frame - kein Neuladen noetig
        if (active == on) {
            return;
        }
        active = on;
        reload();
    }

    public static void reset() {
        if (active) {
            active = false;
            reload();
        }
    }

    private static void reload() {
        MinecraftClient client = MinecraftClient.getInstance();
        client.execute(client::reloadResources);
    }
}
