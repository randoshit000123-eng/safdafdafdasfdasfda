package de.challengemod;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** Zentraler Zustand: Rollen (Blind/Mute/Deaf), Rollen-Anzeige ueber dem Kopf und Challenge-Timer. */
public final class ChallengeState {

    public enum Role {
        BLIND("Blind"),
        MUTE("Mute"),
        DEAF("Deaf");

        public final String label;

        Role(String label) {
            this.label = label;
        }
    }

    private static final String TEAM_PREFIX = "cc_";

    /** Standard-Sichtradius bei Blind in Bloecken (Vanilla-Blindness: 5). */
    public static final float DEFAULT_BLIND_RADIUS = 2.5f;

    // Voicechat-Events laufen in einem anderen Thread -> thread-sichere Sets
    private static final Set<UUID> BLIND = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> MUTED = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> DEAF = ConcurrentHashMap.newKeySet();

    /** Merkt sich das urspruengliche Team eines Spielers (nach Spielername), um es wiederherzustellen. */
    private static final Map<String, String> PREVIOUS_TEAM = new ConcurrentHashMap<>();

    private static volatile float blindRadius = DEFAULT_BLIND_RADIUS;

    // Diagnose Simple Voice Chat
    private static volatile boolean voicechatPluginLoaded = false;
    private static volatile boolean voicechatServerRunning = false;
    private static final AtomicLong BLOCKED_MIC_PACKETS = new AtomicLong();
    private static final AtomicLong BLOCKED_SOUND_PACKETS = new AtomicLong();

    private static boolean timerRunning = false;
    private static long timerTicks = 0;

    /** Aktueller Server (wird beim Serverstart gesetzt). */
    private static volatile MinecraftServer currentServer;

    private ChallengeState() {
    }

    // ---------------------------------------------------------------- Abfragen

    public static boolean isMuted(UUID id) {
        return MUTED.contains(id);
    }

    public static boolean isDeaf(UUID id) {
        return DEAF.contains(id);
    }

    public static boolean isBlind(UUID id) {
        return BLIND.contains(id);
    }

    private static Set<UUID> setFor(Role role) {
        return switch (role) {
            case BLIND -> BLIND;
            case MUTE -> MUTED;
            case DEAF -> DEAF;
        };
    }

    // ---------------------------------------------------------------- Voicechat-Diagnose

    public static void markVoicechatPluginLoaded() {
        voicechatPluginLoaded = true;
    }

    public static boolean isVoicechatPluginLoaded() {
        return voicechatPluginLoaded;
    }

    public static void setVoicechatServerRunning(boolean running) {
        voicechatServerRunning = running;
    }

    public static void countBlockedMic() {
        BLOCKED_MIC_PACKETS.incrementAndGet();
    }

    public static void countBlockedSound() {
        BLOCKED_SOUND_PACKETS.incrementAndGet();
    }

    public static List<String> describe(MinecraftServer server) {
        List<String> lines = new ArrayList<>();
        lines.add("Simple-Voice-Chat-Plugin geladen: " + (voicechatPluginLoaded ? "ja" : "NEIN (Simple Voice Chat auf dem Server installiert?)"));
        lines.add("Voicechat-Server laeuft: " + (voicechatServerRunning ? "ja" : "nein/unbekannt"));
        lines.add("Blind: " + names(server, BLIND) + " (Radius " + blindRadius + " Bloecke)");
        lines.add("Mute: " + names(server, MUTED));
        lines.add("Deaf: " + names(server, DEAF));
        lines.add("Blockierte Mikrofon-Pakete: " + BLOCKED_MIC_PACKETS.get()
                + ", blockierte Empfangs-Pakete: " + BLOCKED_SOUND_PACKETS.get());
        lines.add("Timer: " + (timerRunning ? "laeuft (" + format(timerTicks / 20) + ")" : "gestoppt"));
        return lines;
    }

    private static String names(MinecraftServer server, Set<UUID> ids) {
        if (ids.isEmpty()) {
            return "-";
        }
        List<String> out = new ArrayList<>();
        for (UUID id : ids) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(id);
            out.add(player != null ? player.getName().getString() : id.toString().substring(0, 8) + " (offline)");
        }
        return String.join(", ", out);
    }

    // ---------------------------------------------------------------- Rollen

    public static void setRole(ServerPlayerEntity player, Role role, boolean enabled) {
        UUID id = player.getUuid();
        if (enabled) {
            setFor(role).add(id);
        } else {
            setFor(role).remove(id);
        }

        if (role == Role.BLIND) {
            if (enabled) {
                giveBlindEffects(player);
            } else {
                player.removeStatusEffect(StatusEffects.BLINDNESS);
                player.removeStatusEffect(StatusEffects.DARKNESS);
            }
            sendBlindState(player, enabled);
        }

        updateRoleTag(player);
        sendRoles(player);
    }

    /** Schickt dem Spieler seine eigenen Rollen fuer die HUD-Anzeige. */
    private static void sendRoles(ServerPlayerEntity player) {
        if (ServerPlayNetworking.canSend(player, RolesPayload.ID)) {
            UUID id = player.getUuid();
            ServerPlayNetworking.send(player,
                    new RolesPayload(BLIND.contains(id), MUTED.contains(id), DEAF.contains(id)));
        }
    }

    private static void giveBlindEffects(ServerPlayerEntity player) {
        player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.BLINDNESS, StatusEffectInstance.INFINITE, 0, false, false, false));
        player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.DARKNESS, StatusEffectInstance.INFINITE, 0, false, false, false));
    }

    private static void sendBlindState(ServerPlayerEntity player, boolean blind) {
        // Nur senden, wenn der Client die Mod hat - sonst wuerde er die Verbindung trennen.
        if (ServerPlayNetworking.canSend(player, BlindPayload.ID)) {
            ServerPlayNetworking.send(player, new BlindPayload(blind, blindRadius));
        }
    }

    // ---------------------------------------------------------------- Blind-Radius

    public static float getBlindRadius() {
        return blindRadius;
    }

    /** Setzt den Sichtradius (in Bloecken) und schickt ihn allen blinden Spielern. */
    public static void setBlindRadius(MinecraftServer server, float radius) {
        blindRadius = radius;
        for (UUID id : BLIND) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(id);
            if (player != null) {
                sendBlindState(player, true);
            }
        }
    }

    public static void onJoin(ServerPlayerEntity player) {
        UUID id = player.getUuid();
        if (BLIND.contains(id)) {
            giveBlindEffects(player);
            sendBlindState(player, true);
        }
        if (BLIND.contains(id) || MUTED.contains(id) || DEAF.contains(id)) {
            updateRoleTag(player);
            sendRoles(player);
        }
    }

    // ---------------------------------------------------------------- Rollen-Anzeige ueber dem Kopf
    // Umgesetzt ueber Scoreboard-Teams: Der Suffix eines Teams wird hinter dem Namen angezeigt
    // (ueber dem Kopf, in der Tab-Liste und im Chat).

    private static void updateRoleTag(ServerPlayerEntity player) {
        UUID id = player.getUuid();
        boolean blind = BLIND.contains(id);
        boolean mute = MUTED.contains(id);
        boolean deaf = DEAF.contains(id);

        MinecraftServer server = currentServer;
        if (server == null) {
            return;
        }
        Scoreboard scoreboard = server.getScoreboard();
        String name = player.getNameForScoreboard();
        Team current = scoreboard.getScoreHolderTeam(name);

        if (!blind && !mute && !deaf) {
            if (current != null && current.getName().startsWith(TEAM_PREFIX)) {
                scoreboard.removeScoreHolderFromTeam(name, current);
            }
            String previous = PREVIOUS_TEAM.remove(name);
            if (previous != null) {
                Team previousTeam = scoreboard.getTeam(previous);
                if (previousTeam != null) {
                    scoreboard.addScoreHolderToTeam(name, previousTeam);
                }
            }
            return;
        }

        List<String> labels = new ArrayList<>();
        if (blind) labels.add(Role.BLIND.label);
        if (mute) labels.add(Role.MUTE.label);
        if (deaf) labels.add(Role.DEAF.label);

        String teamName = TEAM_PREFIX + (blind ? "b" : "") + (mute ? "m" : "") + (deaf ? "d" : "");
        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.addTeam(teamName);
            team.setSuffix(Text.literal(" (" + String.join(", ", labels) + ")").formatted(Formatting.RED));
        }

        if (current != null && !current.getName().startsWith(TEAM_PREFIX)) {
            PREVIOUS_TEAM.put(name, current.getName());
        }
        if (current != team) {
            scoreboard.addScoreHolderToTeam(name, team);
        }
    }

    // ---------------------------------------------------------------- Server-Lebenszyklus

    public static void setServer(MinecraftServer server) {
        currentServer = server;
    }

    public static void reset() {
        BLIND.clear();
        MUTED.clear();
        DEAF.clear();
        PREVIOUS_TEAM.clear();
        BLOCKED_MIC_PACKETS.set(0);
        BLOCKED_SOUND_PACKETS.set(0);
        timerRunning = false;
        timerTicks = 0;
    }

    /** Entfernt Rollen-Teams, die von einer frueheren Sitzung im Weltspeicher uebrig sind. */
    public static void cleanupStaleTeams(MinecraftServer server) {
        Scoreboard scoreboard = server.getScoreboard();
        for (Team team : new ArrayList<>(scoreboard.getTeams())) {
            if (team.getName().startsWith(TEAM_PREFIX)) {
                scoreboard.removeTeam(team);
            }
        }
    }

    // ---------------------------------------------------------------- Tick: Effekte + Timer

    public static void tick(MinecraftServer server) {
        // Blind-Effekte alle 2 Sekunden pruefen (z.B. nach Tod oder Milch)
        if (server.getTicks() % 40 == 0 && !BLIND.isEmpty()) {
            for (UUID id : BLIND) {
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(id);
                if (player != null && player.isAlive()
                        && !(player.hasStatusEffect(StatusEffects.BLINDNESS)
                        && player.hasStatusEffect(StatusEffects.DARKNESS))) {
                    giveBlindEffects(player);
                }
            }
        }

        if (timerRunning) {
            timerTicks++;
            if (timerTicks % 20 == 0) {
                showTimer(server);
            }
        }
    }

    // ---------------------------------------------------------------- Timer

    public static boolean startTimer(MinecraftServer server) {
        if (timerRunning) {
            return false;
        }
        timerRunning = true;
        timerTicks = 0;
        showTimer(server);
        return true;
    }

    public static boolean stopTimer(MinecraftServer server) {
        if (!timerRunning) {
            return false;
        }
        timerRunning = false;
        server.getPlayerManager().broadcast(
                Text.literal("Challenge beendet! Zeit: " + format(timerTicks / 20)).formatted(Formatting.GOLD), false);
        return true;
    }

    private static void showTimer(MinecraftServer server) {
        Text text = Text.literal(format(timerTicks / 20)).formatted(Formatting.YELLOW, Formatting.BOLD);
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.sendMessage(text, true); // true = Action-Bar (direkt ueber der Hotbar)
        }
    }

    private static String format(long totalSeconds) {
        long h = totalSeconds / 3600;
        long m = (totalSeconds % 3600) / 60;
        long s = totalSeconds % 60;
        return h > 0 ? String.format("%d:%02d:%02d", h, m, s) : String.format("%02d:%02d", m, s);
    }
}
