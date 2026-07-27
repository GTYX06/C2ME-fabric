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

import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.VectorOperators;

import static com.ishland.c2me.opts.math.common.VectorMathUtil.I_SPECIES;

public class VectorBiomeSearchTree {

    private static final ThreadLocal<int[]> BUF_TARGET = ThreadLocal.withInitial(() -> new int[16]);
    private static final ThreadLocal<int[]> BUF_MINS = ThreadLocal.withInitial(() -> new int[16]);
    private static final ThreadLocal<int[]> BUF_MAXS = ThreadLocal.withInitial(() -> new int[16]);

    public static long distanceSq(short[] target, short[] mins, short[] maxs) {
        if (I_SPECIES.length() >= 8) {
            int[] tInt = BUF_TARGET.get();
            int[] minInt = BUF_MINS.get();
            int[] maxInt = BUF_MAXS.get();

            for (int i = 0; i < 7; i++) {
                tInt[i] = target[i];
                minInt[i] = mins[i];
                maxInt[i] = maxs[i];
            }

            IntVector vTarget = IntVector.fromArray(I_SPECIES, tInt, 0);
            IntVector vMins = IntVector.fromArray(I_SPECIES, minInt, 0);
            IntVector vMaxs = IntVector.fromArray(I_SPECIES, maxInt, 0);

            IntVector vL = vTarget.sub(vMaxs);
            IntVector vM = vMins.sub(vTarget);

            IntVector vZero = IntVector.zero(I_SPECIES);
            IntVector vMClamped = vM.lanewise(VectorOperators.MAX, vZero);
            IntVector vDist = vL.lanewise(VectorOperators.MAX, vMClamped).lanewise(VectorOperators.MAX, vZero);

            IntVector vSq = vDist.mul(vDist);
            return vSq.reduceLanes(VectorOperators.ADD);
        } else {
            long res = 0;
            for (int i = 0; i < 7; i++) {
                long l = (long) target[i] - (long) maxs[i];
                long m = (long) mins[i] - (long) target[i];
                long dist = l >= 0 ? l : Math.max(m, 0L);
                res += dist * dist;
            }
            return res;
        }
    }
}
