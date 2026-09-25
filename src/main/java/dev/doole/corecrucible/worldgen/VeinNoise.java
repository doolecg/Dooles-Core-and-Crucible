package dev.doole.corecrucible.worldgen;

/**
 * Seeded 3D Perlin noise ("improved noise"), values roughly -1..1. Plain Java rather than vanilla's noise classes,
 * whose API changes between versions, and so unit tests can run it without Minecraft.
 */
public final class VeinNoise {

    private final int[] perm = new int[512];
    private final double offsetX;
    private final double offsetY;
    private final double offsetZ;

    // Shuffles the numbers 0-255 using the seed: the standard Perlin permutation table.
    public VeinNoise(long seed) {
        long state = seed;
        int[] p = new int[256];
        for (int i = 0; i < 256; i++) p[i] = i;
        for (int i = 255; i > 0; i--) {
            state = mix(state);
            int j = (int) Long.remainderUnsigned(state, i + 1);
            int t = p[i];
            p[i] = p[j];
            p[j] = t;
        }
        for (int i = 0; i < 512; i++) perm[i] = p[i & 255];
        // A random offset so the lattice points (where the noise is always 0) don't line up across seeds.
        offsetX = unit(state = mix(state)) * 256;
        offsetY = unit(state = mix(state)) * 256;
        offsetZ = unit(mix(state)) * 256;
    }

    // Classic Perlin noise: find the unit cube the point is in, blend the gradients at its 8 corners with a smooth curve.
    public double sample(double x, double y, double z) {
        x += offsetX;
        y += offsetY;
        z += offsetZ;
        int xi = (int) Math.floor(x);
        int yi = (int) Math.floor(y);
        int zi = (int) Math.floor(z);
        double xf = x - xi;
        double yf = y - yi;
        double zf = z - zi;
        xi &= 255;
        yi &= 255;
        zi &= 255;
        double u = fade(xf);
        double v = fade(yf);
        double w = fade(zf);
        int a = perm[xi] + yi;
        int aa = perm[a] + zi;
        int ab = perm[a + 1] + zi;
        int b = perm[xi + 1] + yi;
        int ba = perm[b] + zi;
        int bb = perm[b + 1] + zi;
        return lerp(w,
                lerp(v, lerp(u, grad(perm[aa], xf, yf, zf), grad(perm[ba], xf - 1, yf, zf)),
                        lerp(u, grad(perm[ab], xf, yf - 1, zf), grad(perm[bb], xf - 1, yf - 1, zf))),
                lerp(v, lerp(u, grad(perm[aa + 1], xf, yf, zf - 1), grad(perm[ba + 1], xf - 1, yf, zf - 1)),
                        lerp(u, grad(perm[ab + 1], xf, yf - 1, zf - 1), grad(perm[bb + 1], xf - 1, yf - 1, zf - 1))));
    }

    /** SplitMix64 step: turns any long into a well-scrambled one. Also used for per-block random rolls. */
    public static long mix(long z) {
        z += 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /** The top 53 bits of a long as a double in 0..1. */
    public static double unit(long bits) {
        return (bits >>> 11) * 0x1.0p-53;
    }

    private static double fade(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    private static double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }

    private static double grad(int hash, double x, double y, double z) {
        int h = hash & 15;
        double u = h < 8 ? x : y;
        double v = h < 4 ? y : h == 12 || h == 14 ? x : z;
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }
}
