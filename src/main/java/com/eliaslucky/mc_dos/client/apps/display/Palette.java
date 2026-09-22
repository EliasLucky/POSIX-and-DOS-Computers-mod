package com.eliaslucky.mc_dos.client.apps.display;

public final class Palette {
    private Palette() {}

    private static final int[] VGA256 = buildVga256();

    public static int color256(int i) { return VGA256[i & 0xFF]; }

    private static int[] buildVga256() {
        int[] p = new int[256];
        // 0..15: standard EGA colors
        System.arraycopy(DosPalette.EGA, 0, p, 0, 16);
        // 16..31: grayscale ramp
        for (int i = 0; i < 16; i++) {
            int v = i * 17;
            p[16 + i] = (v << 16) | (v << 8) | v;
        }
        // 32..247: 6×6×6 RGB cube
        int idx = 32;
        for (int r = 0; r < 6; r++)
            for (int g = 0; g < 6; g++)
                for (int b = 0; b < 6; b++)
                    p[idx++] = ((r * 51) << 16) | ((g * 51) << 8) | (b * 51);
        // 248..255: black
        for (int i = 248; i < 256; i++) p[i] = 0;
        return p;
    }
}