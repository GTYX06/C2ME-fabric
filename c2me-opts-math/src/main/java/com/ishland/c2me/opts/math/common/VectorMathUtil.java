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
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.LongVector;
import jdk.incubator.vector.ShortVector;
import jdk.incubator.vector.VectorSpecies;

public class VectorMathUtil {

    public static final VectorSpecies<Double> D_SPECIES;
    public static final VectorSpecies<Float> F_SPECIES;
    public static final VectorSpecies<Integer> I_SPECIES;
    public static final VectorSpecies<Short> S_SPECIES;
    public static final VectorSpecies<Long> L_SPECIES;

    static {
        switch (Config.vectorMode) {
            case AVX512 -> {
                if (DoubleVector.SPECIES_PREFERRED.vectorBitSize() >= 512) {
                    D_SPECIES = DoubleVector.SPECIES_512;
                    F_SPECIES = FloatVector.SPECIES_512;
                    I_SPECIES = IntVector.SPECIES_512;
                    S_SPECIES = ShortVector.SPECIES_512;
                    L_SPECIES = LongVector.SPECIES_512;
                } else {
                    D_SPECIES = DoubleVector.SPECIES_PREFERRED;
                    F_SPECIES = FloatVector.SPECIES_PREFERRED;
                    I_SPECIES = IntVector.SPECIES_PREFERRED;
                    S_SPECIES = ShortVector.SPECIES_PREFERRED;
                    L_SPECIES = LongVector.SPECIES_PREFERRED;
                }
            }
            case AVX2 -> {
                if (DoubleVector.SPECIES_PREFERRED.vectorBitSize() >= 256) {
                    D_SPECIES = DoubleVector.SPECIES_256;
                    F_SPECIES = FloatVector.SPECIES_256;
                    I_SPECIES = IntVector.SPECIES_256;
                    S_SPECIES = ShortVector.SPECIES_256;
                    L_SPECIES = LongVector.SPECIES_256;
                } else {
                    D_SPECIES = DoubleVector.SPECIES_PREFERRED;
                    F_SPECIES = FloatVector.SPECIES_PREFERRED;
                    I_SPECIES = IntVector.SPECIES_PREFERRED;
                    S_SPECIES = ShortVector.SPECIES_PREFERRED;
                    L_SPECIES = LongVector.SPECIES_PREFERRED;
                }
            }
            default -> {
                D_SPECIES = DoubleVector.SPECIES_PREFERRED;
                F_SPECIES = FloatVector.SPECIES_PREFERRED;
                I_SPECIES = IntVector.SPECIES_PREFERRED;
                S_SPECIES = ShortVector.SPECIES_PREFERRED;
                L_SPECIES = LongVector.SPECIES_PREFERRED;
            }
        }
    }

    public static final double[] FLAT_SIMPLEX_GRAD = new double[]{
            1, 1, 0, 0,
            -1, 1, 0, 0,
            1, -1, 0, 0,
            -1, -1, 0, 0,
            1, 0, 1, 0,
            -1, 0, 1, 0,
            1, 0, -1, 0,
            -1, 0, -1, 0,
            0, 1, 1, 0,
            0, -1, 1, 0,
            0, 1, -1, 0,
            0, -1, -1, 0,
            1, 1, 0, 0,
            0, -1, 1, 0,
            -1, 1, 0, 0,
            0, -1, -1, 0,
    };

    public static final double SQRT_3 = 1.7320508075688772;
    public static final double SKEW_FACTOR_2D = 0.3660254037844386; // 0.5 * (SQRT_3 - 1.0)
    public static final double UNSKEW_FACTOR_2D = 0.21132486540518713; // (3.0 - SQRT_3) / 6.0

    private static final ThreadLocal<double[]> TEMP_BUF = ThreadLocal.withInitial(() -> new double[32]);

    public static double maintainPrecision(double value) {
        return value - Math.floor(value / 3.3554432E7 + 0.5) * 3.3554432E7;
    }

    public static DoubleVector maintainPrecision(DoubleVector v) {
        double[] arr = TEMP_BUF.get();
        int len = D_SPECIES.length();
        if (arr.length < len) {
            arr = new double[len];
            TEMP_BUF.set(arr);
        }
        v.intoArray(arr, 0);
        for (int k = 0; k < len; k++) {
            double val = arr[k];
            arr[k] = val - Math.floor(val / 3.3554432E7 + 0.5) * 3.3554432E7;
        }
        return DoubleVector.fromArray(D_SPECIES, arr, 0);
    }

    public static double perlinFade(double value) {
        return value * value * value * (value * (value * 6.0 - 15.0) + 10.0);
    }

    public static DoubleVector perlinFade(DoubleVector v) {
        DoubleVector poly = v.mul(6.0).sub(15.0).fma(v, DoubleVector.broadcast(D_SPECIES, 10.0));
        DoubleVector v3 = v.mul(v).mul(v);
        return v3.mul(poly);
    }

    public static double lerp(double delta, double start, double end) {
        return start + delta * (end - start);
    }

    public static DoubleVector lerp(DoubleVector delta, DoubleVector start, DoubleVector end) {
        return end.sub(start).fma(delta, start);
    }

    public static double lerp2(double deltaX, double deltaY, double x0y0, double x1y0, double x0y1, double x1y1) {
        return lerp(deltaY, lerp(deltaX, x0y0, x1y0), lerp(deltaX, x0y1, x1y1));
    }

    public static DoubleVector lerp2(DoubleVector deltaX, DoubleVector deltaY, DoubleVector x0y0, DoubleVector x1y0, DoubleVector x0y1, DoubleVector x1y1) {
        return lerp(deltaY, lerp(deltaX, x0y0, x1y0), lerp(deltaX, x0y1, x1y1));
    }

    public static double lerp3(double deltaX, double deltaY, double deltaZ,
                               double x0y0z0, double x1y0z0, double x0y1z0, double x1y1z0,
                               double x0y0z1, double x1y0z1, double x0y1z1, double x1y1z1) {
        return lerp(deltaZ, lerp2(deltaX, deltaY, x0y0z0, x1y0z0, x0y1z0, x1y1z0),
                lerp2(deltaX, deltaY, x0y0z1, x1y0z1, x0y1z1, x1y1z1));
    }

    public static DoubleVector lerp3(DoubleVector deltaX, DoubleVector deltaY, DoubleVector deltaZ,
                                     DoubleVector x0y0z0, DoubleVector x1y0z0, DoubleVector x0y1z0, DoubleVector x1y1z0,
                                     DoubleVector x0y0z1, DoubleVector x1y0z1, DoubleVector x0y1z1, DoubleVector x1y1z1) {
        return lerp(deltaZ, lerp2(deltaX, deltaY, x0y0z0, x1y0z0, x0y1z0, x1y1z0),
                lerp2(deltaX, deltaY, x0y0z1, x1y0z1, x0y1z1, x1y1z1));
    }

    public static double clampedLerp(double start, double end, double delta) {
        if (delta < 0.0) {
            return start;
        } else if (delta > 1.0) {
            return end;
        } else {
            return lerp(delta, start, end);
        }
    }

    public static double square(double operand) {
        return operand * operand;
    }
}
