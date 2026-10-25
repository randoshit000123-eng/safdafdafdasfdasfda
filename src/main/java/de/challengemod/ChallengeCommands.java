package de.challengemod;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.DefaultPermissions;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class ChallengeCommands {

    private ChallengeCommands() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        role(dispatcher, "blind", ChallengeState.Role.BLIND, true,
                "ist jetzt blind.", "Du bist jetzt blind!");
        role(dispatcher, "unblind", ChallengeState.Role.BLIND, false,
                "ist nicht mehr blind.", "Du kannst wieder sehen.");

        role(dispatcher, "mute", ChallengeState.Role.MUTE, true,
                "ist jetzt stumm.", "Du wurdest im Voicechat stummgeschaltet!");
        role(dispatcher, "unmute", ChallengeState.Role.MUTE, false,
                "ist nicht mehr stumm.", "Du kannst wieder sprechen.");

        role(dispatcher, "deaf", ChallengeState.Role.DEAF, true,
                "ist jetzt taub.", "Du kannst im Voicechat nichts mehr hoeren!");
        role(dispatcher, "undeaf", ChallengeState.Role.DEAF, false,
                "ist nicht mehr taub.", "Du kannst wieder hoeren.");

        dispatcher.register(blindRadius());

        // "challange" (wie gewuenscht) und "challenge" (korrekte Schreibweise)
        dispatcher.register(challenge("challange"));
        dispatcher.register(challenge("challenge"));
    }

    private static void role(CommandDispatcher<ServerCommandSource> dispatcher, String name,
                             ChallengeState.Role role, boolean enable,
                             String feedbackSuffix, String targetMessage) {
        dispatcher.register(CommandManager.literal(name)
                .requires(source -> source.getPermissions().hasPermission(DefaultPermissions.GAMEMASTERS))
                .then(CommandManager.argument("player", EntityArgumentType.player())
                        .executes(context -> {
                            ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
                            ChallengeState.setRole(target, role, enable);

                            context.getSource().sendFeedback(
                                    () -> Text.literal(target.getName().getString() + " " + feedbackSuffix),
                                    true);
                            target.sendMessage(Text.literal(targetMessage)
                                    .formatted(enable ? Formatting.RED : Formatting.GREEN), false);

                            // Mute/Deaf funktionieren nur ueber das Simple-Voice-Chat-Plugin auf dem Server
                            boolean voiceRole = role == ChallengeState.Role.MUTE || role == ChallengeState.Role.DEAF;
                            if (enable && voiceRole && !ChallengeState.isVoicechatPluginLoaded()) {
                                context.getSource().sendError(Text.literal(
                                        "Achtung: Das Simple-Voice-Chat-Plugin ist auf dem Server nicht geladen - "
                                                + "Mute/Deaf haben so keine Wirkung. Ist Simple Voice Chat "
                                                + "auch auf dem SERVER installiert (gleiche Version wie bei den Spielern)?"));
                            }
                            return 1;
                        })));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> blindRadius() {
        return CommandManager.literal("blindradius")
                .requires(source -> source.getPermissions().hasPermission(DefaultPermissions.GAMEMASTERS))
                .executes(context -> {
                    context.getSource().sendFeedback(() -> Text.literal(
                            "Aktueller Blind-Radius: " + ChallengeState.getBlindRadius()
                                    + " Bloecke (Vanilla-Blindness: 5). Aendern mit /blindradius <1-20>"), false);
                    return 1;
                })
                .then(CommandManager.argument("radius", FloatArgumentType.floatArg(1.0f, 20.0f))
                        .executes(context -> {
                            float radius = FloatArgumentType.getFloat(context, "radius");
                            ChallengeState.setBlindRadius(context.getSource().getServer(), radius);
                            context.getSource().sendFeedback(
                                    () -> Text.literal("Blind-Radius auf " + radius + " Bloecke gesetzt."), true);
                            return 1;
                        }));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> challenge(String name) {
        return CommandManager.literal(name)
                .requires(source -> source.getPermissions().hasPermission(DefaultPermissions.GAMEMASTERS))
                .then(CommandManager.literal("start").executes(context -> {
                    if (ChallengeState.startTimer(context.getSource().getServer())) {
                        context.getSource().sendFeedback(
                                () -> Text.literal("Challenge gestartet! Der Timer laeuft."), true);
                        return 1;
                    }
                    context.getSource().sendError(Text.literal("Die Challenge laeuft bereits."));
                    return 0;
                }))
                .then(CommandManager.literal("stop").executes(context -> {
                    if (ChallengeState.stopTimer(context.getSource().getServer())) {
                        return 1;
                    }
                    context.getSource().sendError(Text.literal("Es laeuft keine Challenge."));
                    return 0;
                }))
                .then(CommandManager.literal("status").executes(context -> {
                    for (String line : ChallengeState.describe(context.getSource().getServer())) {
                        context.getSource().sendFeedback(() -> Text.literal(line), false);
                    }
                    return 1;
                }));
    }
}
