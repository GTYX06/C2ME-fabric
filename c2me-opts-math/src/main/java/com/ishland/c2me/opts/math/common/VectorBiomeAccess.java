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
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.ishland.c2me.opts.math.common;

import jdk.incubator.vector.DoubleVector;

import static com.ishland.c2me.opts.math.common.VectorMathUtil.*;

public class VectorBiomeAccess {

    public static int sample(long theSeed, int x, int y, int z) {
        int var0 = x - 2;
        int var1 = y - 2;
        int var2 = z - 2;
        int var3 = var0 >> 2;
        int var4 = var1 >> 2;
        int var5 = var2 >> 2;
        double var6 = (double) (var0 & 3) / 4.0;
        double var7 = (double) (var1 & 3) / 4.0;
        double var8 = (double) (var2 & 3) / 4.0;

        double[] dists = new double[8];

        for (int var11 = 0; var11 < 8; var11++) {
            boolean var12 = (var11 & 4) != 0;
            boolean var13 = (var11 & 2) != 0;
            boolean var14 = (var11 & 1) != 0;

            long var15 = var12 ? var3 + 1 : var3;
            long var16 = var13 ? var4 + 1 : var4;
            long var17 = var14 ? var5 + 1 : var5;
            double var18 = var12 ? var6 - 1.0 : var6;
            double var19 = var13 ? var7 - 1.0 : var7;
            double var20 = var14 ? var8 - 1.0 : var8;

            long var21 = theSeed * (theSeed * 6364136223846793005L + 1442695040888963407L) + var15;
            var21 = var21 * (var21 * 6364136223846793005L + 1442695040888963407L) + var16;
            var21 = var21 * (var21 * 6364136223846793005L + 1442695040888963407L) + var17;
            var21 = var21 * (var21 * 6364136223846793005L + 1442695040888963407L) + var15;
            var21 = var21 * (var21 * 6364136223846793005L + 1442695040888963407L) + var16;
            var21 = var21 * (var21 * 6364136223846793005L + 1442695040888963407L) + var17;

            double var22 = (double) ((var21 >> 24) & 1023) / 1024.0;
            double var23 = (var22 - 0.5) * 0.9;
            var21 = var21 * (var21 * 6364136223846793005L + 1442695040888963407L) + theSeed;
            double var24 = (double) ((var21 >> 24) & 1023) / 1024.0;
            double var25 = (var24 - 0.5) * 0.9;
            var21 = var21 * (var21 * 6364136223846793005L + 1442695040888963407L) + theSeed;
            double var26 = (double) ((var21 >> 24) & 1023) / 1024.0;
            double var27 = (var26 - 0.5) * 0.9;

            dists[var11] = square(var20 + var27) + square(var19 + var25) + square(var18 + var23);
        }

        int minIdx = 0;
        double minDist = dists[0];

        if (D_SPECIES.length() >= 8) {
            DoubleVector vDists = DoubleVector.fromArray(D_SPECIES, dists, 0);
            minDist = vDists.reduceLanes(jdk.incubator.vector.VectorOperators.MIN);
            for (int i = 0; i < 8; i++) {
                if (dists[i] == minDist) {
                    return i;
                }
            }
        } else {
            for (int i = 1; i < 8; i++) {
                if (dists[i] < minDist) {
                    minDist = dists[i];
                    minIdx = i;
                }
            }
        }

        return minIdx;
    }
}
