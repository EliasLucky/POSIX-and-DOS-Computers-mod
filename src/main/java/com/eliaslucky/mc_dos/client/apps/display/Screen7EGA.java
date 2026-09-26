package com.eliaslucky.mc_dos.client.apps.display;

import net.minecraft.client.gui.GuiGraphics;

/**
 * SCREEN 7 — 320×200, 16 colors (EGA mode 0Dh).
 * Identical layout to Screen 13, but palette indices are masked to 4 bits.
 */
public final class Screen7EGA extends DisplayMode {
    public static final int W = 320;
    public static final int H = 200;

    private final byte[] pixels = new byte[W * H];

    public Screen7EGA() {
        super(7, W, H, 16, false);
        clear(0);
    }

    @Override public void clear(int color) {
        java.util.Arrays.fill(pixels, (byte)(color & 0xF));
    }

    @Override public void setPixel(int x, int y, int color) {
        if (x < 0 || x >= W || y < 0 || y >= H) return;
        pixels[y * W + x] = (byte)(color & 0xF);
    }

    @Override public int getPixel(int x, int y) {
        if (x < 0 || x >= W || y < 0 || y >= H) return 0;
        return pixels[y * W + x] & 0xF;
    }

    @Override
    public void render(GuiGraphics g, int screenX, int screenY, int maxW, int maxH) {
        int scale = Math.min(maxW / W, maxH / H);
        if (scale < 1) scale = 1;
        int dispW = W * scale, dispH = H * scale;
        int ox = screenX + (maxW - dispW) / 2;
        int oy = screenY + (maxH - dispH) / 2;

        for (int y = 0; y < H; y++) {
            int rowOff = y * W;
            int x = 0;
            while (x < W) {
                int idx = pixels[rowOff + x] & 0xF;
                int end = x;
                while (end < W && (pixels[rowOff + end] & 0xF) == idx) end++;
                int color = 0xFF000000 | DosPalette.EGA[idx];
                g.fill(ox + x * scale, oy + y * scale,
                       ox + end * scale, oy + (y + 1) * scale, color);
                x = end;
            }
        }
    }
}
