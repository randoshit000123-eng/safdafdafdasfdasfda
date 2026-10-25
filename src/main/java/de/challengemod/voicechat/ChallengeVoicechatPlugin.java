package de.challengemod.voicechat;

import de.challengemod.ChallengeMod;
import de.challengemod.ChallengeState;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EntitySoundPacketEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.LocationalSoundPacketEvent;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.StaticSoundPacketEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStoppedEvent;

/**
 * Simple-Voice-Chat-Plugin.
 *
 * Mute: Mikrofon-Pakete des Spielers werden verworfen (und zur Sicherheit auch alle Audio-Pakete,
 *       die von ihm als Sender kommen) -> niemand hoert ihn, er hoert aber weiter alle.
 * Deaf: Alle Audio-Pakete, die der Spieler empfangen wuerde, werden verworfen -> er hoert nichts,
 *       kann aber weiter sprechen.
 */
public class ChallengeVoicechatPlugin implements VoicechatPlugin {

    @Override
    public String getPluginId() {
        return ChallengeMod.MOD_ID;
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        // Wird aufgerufen, sobald Simple Voice Chat unser Plugin gefunden hat
        ChallengeState.markVoicechatPluginLoaded();
        ChallengeMod.LOGGER.info("Simple-Voice-Chat-Plugin der Challenge Mod wurde registriert.");

        registration.registerEvent(VoicechatServerStartedEvent.class, event -> {
            ChallengeState.setVoicechatServerRunning(true);
            ChallengeMod.LOGGER.info("Voicechat-Server gestartet - /mute und /deaf sind aktiv.");
        });
        registration.registerEvent(VoicechatServerStoppedEvent.class,
                event -> ChallengeState.setVoicechatServerRunning(false));

        // Hoehere Prioritaet (100), damit wir vor anderen Plugins entscheiden
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophone, 100);
        registration.registerEvent(EntitySoundPacketEvent.class, this::onEntitySound, 100);
        registration.registerEvent(LocationalSoundPacketEvent.class, this::onLocationalSound, 100);
        registration.registerEvent(StaticSoundPacketEvent.class, this::onStaticSound, 100);
    }

    // ------------------------------------------------------------ Mute (Sender)

    private void onMicrophone(MicrophonePacketEvent event) {
        if (isMuted(event.getSenderConnection())) {
            event.cancel();
            ChallengeState.countBlockedMic();
        }
    }

    // ------------------------------------------------------------ Deaf (Empfaenger) + Mute (Sicherheitsnetz)

    private void onEntitySound(EntitySoundPacketEvent event) {
        if (isMuted(event.getSenderConnection()) || isDeaf(event.getReceiverConnection())) {
            event.cancel();
            ChallengeState.countBlockedSound();
        }
    }

    private void onLocationalSound(LocationalSoundPacketEvent event) {
        if (isMuted(event.getSenderConnection()) || isDeaf(event.getReceiverConnection())) {
            event.cancel();
            ChallengeState.countBlockedSound();
        }
    }

    private void onStaticSound(StaticSoundPacketEvent event) {
        if (isMuted(event.getSenderConnection()) || isDeaf(event.getReceiverConnection())) {
            event.cancel();
            ChallengeState.countBlockedSound();
        }
    }

    // ------------------------------------------------------------ Hilfen

    private static boolean isMuted(VoicechatConnection connection) {
        return connection != null && ChallengeState.isMuted(connection.getPlayer().getUuid());
    }

    private static boolean isDeaf(VoicechatConnection connection) {
        return connection != null && ChallengeState.isDeaf(connection.getPlayer().getUuid());
    }
}
