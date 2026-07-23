/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2021-2026 ishland
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 */

package com.ishland.c2me.opts.math.common;

import com.ishland.c2me.base.mixin.access.ISimplexNoiseSampler;
import net.minecraft.util.math.noise.SimplexNoiseSampler;

import static com.ishland.c2me.opts.math.common.VectorMathUtil.*;

public class VectorEndIslands {

    public static float sample(SimplexNoiseSampler sampler, int x, int z) {
        int[] perm = ((ISimplexNoiseSampler) sampler).getPermutation();
        return sample(perm, x, z);
    }

    public static float sample(int[] perm, int x, int z) {
        int i = x / 2;
        int j = z / 2;
        int k = x % 2;
        int l = z % 2;
        long muld = (long) x * x + (long) z * z;
        float f = 100.0F - (float) Math.sqrt((double) muld) * 8.0F;
        f = Math.max(-100.0F, Math.min(80.0F, f));

        long omin = Math.abs((long) i) - 12L;
        long pmin = Math.abs((long) j) - 12L;
        boolean checkCircle = (omin * omin + pmin * pmin) <= 4096L;

        for (int m = -12; m <= 12; m++) {
            for (int n = -12; n <= 12; n++) {
                long o = (long) i + m;
                long p = (long) j + n;
                if (checkCircle && (o * o + p * p) <= 4096L) {
                    continue;
                }
                if (sampleSimplex2D(perm, (double) o, (double) p) < -0.9F) {
                    float g1 = Math.abs((float) o) * 3439.0F;
                    float g2 = Math.abs((float) p) * 147.0F;
                    float g = ((g1 + g2) % 13.0F) + 9.0F;
                    float hVal = (float) (k - m * 2);
                    float qVal = (float) (l - n * 2);
                    float r = 100.0F - (float) Math.sqrt(hVal * hVal + qVal * qVal) * g;
                    r = Math.max(-100.0F, Math.min(80.0F, r));
                    f = Math.max(f, r);
                }
            }
        }
        return f;
    }

    public static double sampleSimplex2D(int[] perm, double x, double y) {
        double d = (x + y) * SKEW_FACTOR_2D;
        double i = Math.floor(x + d);
        double j = Math.floor(y + d);
        double e = (i + j) * UNSKEW_FACTOR_2D;
        double f = i - e;
        double g = j - e;
        double h = x - f;
        double k = y - g;
        int lVal, mVal;
        if (h > k) {
            lVal = 1; mVal = 0;
        } else {
            lVal = 0; mVal = 1;
        }

        double n = h - (double) lVal + UNSKEW_FACTOR_2D;
        double o = k - (double) mVal + UNSKEW_FACTOR_2D;
        double p = h - 1.0 + 2.0 * UNSKEW_FACTOR_2D;
        double q = k - 1.0 + 2.0 * UNSKEW_FACTOR_2D;

        int r = (int) i & 0xFF;
        int s = (int) j & 0xFF;

        int t = Math.floorMod(perm[r + (perm[s] & 0xFF) & 0xFF], 12);
        int u = Math.floorMod(perm[r + lVal + (perm[s + mVal & 0xFF] & 0xFF) & 0xFF], 12);
        int v = Math.floorMod(perm[r + 1 + (perm[s + 1 & 0xFF] & 0xFF) & 0xFF], 12);

        double w = grad2D(t, h, k);
        double zVal = grad2D(u, n, o);
        double aa = grad2D(v, p, q);

        return 70.0 * (w + zVal + aa);
    }

    private static double grad2D(int hash, double dx, double dy) {
        double dist = 0.5 - dx * dx - dy * dy;
        if (dist < 0.0) {
            return 0.0;
        }
        dist *= dist;
        int loc = hash << 2;
        double dot = FLAT_SIMPLEX_GRAD[loc] * dx + FLAT_SIMPLEX_GRAD[loc | 1] * dy;
        return dist * dist * dot;
    }
}
