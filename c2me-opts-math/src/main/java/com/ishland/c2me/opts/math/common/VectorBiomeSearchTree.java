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

import jdk.incubator.vector.ShortVector;
import jdk.incubator.vector.VectorOperators;

import static com.ishland.c2me.opts.math.common.VectorMathUtil.S_SPECIES;

public class VectorBiomeSearchTree {

    public static long distanceSq(short[] target, short[] mins, short[] maxs) {
        if (S_SPECIES.length() >= 8) {
            ShortVector vTarget = ShortVector.fromArray(S_SPECIES, target, 0);
            ShortVector vMins = ShortVector.fromArray(S_SPECIES, mins, 0);
            ShortVector vMaxs = ShortVector.fromArray(S_SPECIES, maxs, 0);

            ShortVector vL = vTarget.sub(vMaxs);
            ShortVector vM = vMins.sub(vTarget);

            ShortVector vZero = ShortVector.zero(S_SPECIES);
            ShortVector vMClamped = vM.lanewise(VectorOperators.MAX, vZero);

            // Select l >= 0 ? l : max(m, 0)
            ShortVector vDist = vL.lanewise(VectorOperators.MAX, vMClamped).lanewise(VectorOperators.MAX, vZero);

            short[] arrDist = vDist.toArray();
            long res = 0;
            for (int i = 0; i < 7; i++) {
                long d = arrDist[i];
                res += d * d;
            }
            return res;
        } else {
            long res = 0;
            for (int i = 0; i < 7; i++) {
                long l = (long) target[i] - (long) maxs[i];
                long m = (long) mins[i] - (long) target[i];
                long dist = l >= 0 ? l : Math.max(m, 0);
                res += dist * dist;
            }
            return res;
        }
    }
}
