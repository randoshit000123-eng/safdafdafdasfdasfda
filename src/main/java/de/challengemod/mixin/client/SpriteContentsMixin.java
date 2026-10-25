package de.challengemod.mixin.client;

import de.challengemod.client.BlindClient;
import de.challengemod.client.BlindTextures;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.SpriteContents;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Faerbt beim (Neu-)Laden alle Block-Texturen um, solange der Blind-Modus aktiv ist.
 * HUD, GUI, Partikel, Entities usw. liegen in anderen Atlanten bzw. Pfaden und bleiben unveraendert.
 */
@Mixin(SpriteContents.class)
public abstract class SpriteContentsMixin {

    @Shadow
    @Final
    private NativeImage image;

    @Shadow
    public abstract Identifier getId();

    @Shadow
    public abstract int getWidth();

    @Shadow
    public abstract int getHeight();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void challengemod$blindify(CallbackInfo ci) {
        if (!BlindClient.active) {
            return;
        }
        String path = getId().getPath();
        if (!path.startsWith("block/") || path.contains("destroy_stage")) {
            return;
        }
        BlindTextures.apply(this.image, getWidth(), getHeight());
    }
}
