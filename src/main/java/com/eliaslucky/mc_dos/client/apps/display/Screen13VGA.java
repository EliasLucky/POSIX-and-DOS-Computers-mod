package com.eliaslucky.mc_dos.client.apps.display;

import net.minecraft.client.gui.GuiGraphics;

/**
 * SCREEN 13 — 320×200, 256 colors (VGA mode 13h).
 * Each pixel is one byte, an index into the 256-entry VGA palette.
 */
public final class Screen13VGA extends DisplayMode {

    public static final int W = 320;
    public static final int H = 200;

    private final byte[] pixels = new byte[W * H];

    public Screen13VGA() {
        super(13, W, H, 256, false);
        clear(0);
    }

    @Override
    public void clear(int color) {
        java.util.Arrays.fill(pixels, (byte) (color & 0xFF));
    }

    @Override
    public void setPixel(int x, int y, int color) {
        if (x < 0 || x >= W || y < 0 || y >= H) return;
        pixels[y * W + x] = (byte) (color & 0xFF);
    }

    @Override
    public int getPixel(int x, int y) {
        if (x < 0 || x >= W || y < 0 || y >= H) return 0;
        return pixels[y * W + x] & 0xFF;
    }

    @Override
    public void render(GuiGraphics g, int screenX, int screenY, int maxW, int maxH) {
        // Integer scale that fits the whole framebuffer inside the rectangle.
        int scale = Math.min(maxW / W, maxH / H);
        if (scale < 1) scale = 1;

        int dispW = W * scale;
        int dispH = H * scale;
        int ox = screenX + (maxW - dispW) / 2;
        int oy = screenY + (maxH - dispH) / 2;

        // Batch horizontal runs of the same palette index.
        for (int y = 0; y < H; y++) {
            int rowOff = y * W;
            int x = 0;
            while (x < W) {
                int idx = pixels[rowOff + x] & 0xFF;
                int end = x;
                while (end < W && (pixels[rowOff + end] & 0xFF) == idx) end++;

                int color = 0xFF000000 | Palette.color256(idx);
                g.fill(
                        ox + x   * scale,
                        oy + y   * scale,
                        ox + end * scale,
                        oy + (y + 1) * scale,
                        color);

                x = end;
            }
        }
    }
}