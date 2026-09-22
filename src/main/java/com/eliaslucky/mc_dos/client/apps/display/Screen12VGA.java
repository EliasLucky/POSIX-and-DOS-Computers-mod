package com.eliaslucky.mc_dos.client.apps.display;

import net.minecraft.client.gui.GuiGraphics;

// TODO: IMPROVE LATER
/** SCREEN 12 — 640×480, 16 colors (VGA mode 12h). */
public final class Screen12VGA extends DisplayMode {

    public static final int W = 640;
    public static final int H = 480;

    private final byte[] pixels = new byte[W * H];   // one palette index per byte (simplified)

    public Screen12VGA() {
        super(12, W, H, 16, false);
        clear(0);
    }

    @Override public void clear(int color) {
        java.util.Arrays.fill(pixels, (byte) (color & 0xF));
    }

    @Override public void setPixel(int x, int y, int color) {
        if (x < 0 || x >= W || y < 0 || y >= H) return;
        pixels[y * W + x] = (byte) (color & 0xF);
    }

    @Override public int getPixel(int x, int y) {
        if (x < 0 || x >= W || y < 0 || y >= H) return 0;
        return pixels[y * W + x] & 0xF;
    }

    @Override
    public void render(GuiGraphics g, int screenX, int screenY, int maxW, int maxH) {
        int scale = Math.min(maxW / W, maxH / H);
        if (scale < 1) scale = 1;
        int dispW = W * scale;
        int dispH = H * scale;
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
