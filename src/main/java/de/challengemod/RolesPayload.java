package de.challengemod;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Server -> Client: Welche Rollen hat der Spieler selbst? (fuer die HUD-Anzeige) */
public record RolesPayload(boolean blind, boolean mute, boolean deaf) implements CustomPayload {

    public static final CustomPayload.Id<RolesPayload> ID =
            new CustomPayload.Id<>(Identifier.of(ChallengeMod.MOD_ID, "roles"));

    public static final PacketCodec<RegistryByteBuf, RolesPayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.BOOLEAN, RolesPayload::blind,
                    PacketCodecs.BOOLEAN, RolesPayload::mute,
                    PacketCodecs.BOOLEAN, RolesPayload::deaf,
                    RolesPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
