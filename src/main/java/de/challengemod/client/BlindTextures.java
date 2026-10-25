package de.challengemod.client;

import net.minecraft.client.texture.NativeImage;

/** Faerbt eine Block-Textur schwarz und zeichnet nur die Konturen weiss. */
public final class BlindTextures {

    private BlindTextures() {
    }

    /**
     * @param image  Textur (bei Animationen: der komplette Streifen aller Frames)
     * @param frameW Breite eines Frames
     * @param frameH Hoehe eines Frames
     */
    public static void apply(NativeImage image, int frameW, int frameH) {
        int w = image.getWidth();
        int h = image.getHeight();
        if (frameW <= 0 || frameH <= 0 || w <= 0 || h <= 0) {
            return;
        }

        int border = Math.max(1, frameW / 16); // Kontur-Staerke skaliert mit der Aufloesung

        int[] alpha = new int[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                alpha[y * w + x] = (image.getColorArgb(x, y) >>> 24) & 0xFF;
            }
        }

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int a = alpha[y * w + x];
                if (a == 0) {
                    continue; // durchsichtige Pixel bleiben durchsichtig
                }
                int fx = x % frameW;
                int fy = y % frameH;

                boolean edge = fx < border || fx >= frameW - border
                        || fy < border || fy >= frameH - border
                        || (fx > 0 && alpha[y * w + x - 1] == 0)
                        || (fx < frameW - 1 && alpha[y * w + x + 1] == 0)
                        || (fy > 0 && alpha[(y - 1) * w + x] == 0)
                        || (fy < frameH - 1 && alpha[(y + 1) * w + x] == 0);

                // Farbformat ist ARGB: Alpha bleibt erhalten, RGB = weiss (Kontur) oder schwarz
                image.setColorArgb(x, y, (a << 24) | (edge ? 0x00FFFFFF : 0x00000000));
            }
        }
    }
}
