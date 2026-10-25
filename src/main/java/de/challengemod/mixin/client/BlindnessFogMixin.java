package de.challengemod.mixin.client;

import de.challengemod.client.BlindClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.fog.BlindnessEffectFogModifier;
import net.minecraft.client.render.fog.FogData;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Verkleinert den Sichtradius des Blindness-Nebels.
 *
 * Vanilla setzt bei Blindness (unendliche Dauer) den Nebel auf Start 1.25 / Ende 5 Bloecke
 * (Himmel/Wolken: Ende 4). Wir skalieren diese Werte nach dem Vanilla-Aufruf mit radius / 5.
 *
 * Signatur in 1.21.11: applyStartEndModifier(FogData, Camera, ClientWorld, float, RenderTickCounter)
 */
@Mixin(BlindnessEffectFogModifier.class)
public abstract class BlindnessFogMixin {

    @Inject(method = "applyStartEndModifier", at = @At("TAIL"))
    private void challengemod$shrinkBlindFog(FogData data, Camera camera, ClientWorld world,
                                             float viewDistance, RenderTickCounter tickCounter,
                                             CallbackInfo ci) {
        if (!BlindClient.active) {
            return;
        }
        float scale = BlindClient.radius / 5.0f;
        data.environmentalStart *= scale;
        data.environmentalEnd *= scale;
        data.skyEnd *= scale;
        data.cloudEnd *= scale;
    }
}
