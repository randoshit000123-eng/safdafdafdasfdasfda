package de.challengemod;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Server -> Client: Blind-Modus ein/aus und Sichtradius (in Bloecken).
 */
public record BlindPayload(boolean blind, float radius) implements CustomPayload {

    public static final CustomPayload.Id<BlindPayload> ID =
            new CustomPayload.Id<>(Identifier.of(ChallengeMod.MOD_ID, "blind"));

    public static final PacketCodec<RegistryByteBuf, BlindPayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.BOOLEAN, BlindPayload::blind,
                    PacketCodecs.FLOAT, BlindPayload::radius,
                    BlindPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
