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

import com.ishland.c2me.base.mixin.access.IDoublePerlinNoiseSampler;
import com.ishland.c2me.base.mixin.access.IOctavePerlinNoiseSampler;
import com.ishland.c2me.base.mixin.access.IPerlinNoiseSampler;
import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.VectorMask;
import net.minecraft.util.math.noise.DoublePerlinNoiseSampler;
import net.minecraft.util.math.noise.OctavePerlinNoiseSampler;
import net.minecraft.util.math.noise.PerlinNoiseSampler;

import static com.ishland.c2me.opts.math.common.VectorMathUtil.*;

public class VectorPerlinNoise {

    private static final ThreadLocal<double[]> BUF_D = ThreadLocal.withInitial(() -> new double[32]);
    private static final ThreadLocal<double[]> BUF_E = ThreadLocal.withInitial(() -> new double[32]);
    private static final ThreadLocal<double[]> BUF_F = ThreadLocal.withInitial(() -> new double[32]);
    private static final ThreadLocal<double[]> BUF_RES = ThreadLocal.withInitial(() -> new double[32]);

    public static double samplePerlin(PerlinNoiseSampler sampler, double x, double y, double z, double yScale, double yMax) {
        IPerlinNoiseSampler acc = (IPerlinNoiseSampler) (Object) sampler;
        byte[] perm = acc.getPermutation();
        double d = x + sampler.originX;
        double e = y + sampler.originY;
        double f = z + sampler.originZ;
        double i = Math.floor(d);
        double j = Math.floor(e);
        double k = Math.floor(f);
        double g = d - i;
        double h = e - j;
        double l = f - k;
        double o = 0.0;
        if (yScale != 0.0) {
            double m = (yMax >= 0.0 && yMax < h) ? yMax : h;
            o = Math.floor(m / yScale + 1.0E-7) * yScale;
        }
        return samplePerlinRaw(perm, (int) i, (int) j, (int) k, g, h - o, l, h);
    }

    public static double samplePerlinRaw(byte[] perm, int sectionX, int sectionY, int sectionZ,
                                         double localX, double localY, double localZ, double fadeLocalY) {
        int var0 = sectionX & 0xFF;
        int var1 = (sectionX + 1) & 0xFF;
        int var2 = perm[var0] & 0xFF;
        int var3 = perm[var1] & 0xFF;
        int var4 = (var2 + sectionY) & 0xFF;
        int var5 = (var3 + sectionY) & 0xFF;
        int var6 = (var2 + sectionY + 1) & 0xFF;
        int var7 = (var3 + sectionY + 1) & 0xFF;
        int var8 = perm[var4] & 0xFF;
        int var9 = perm[var5] & 0xFF;
        int var10 = perm[var6] & 0xFF;
        int var11 = perm[var7] & 0xFF;

        int var12 = (var8 + sectionZ) & 0xFF;
        int var13 = (var9 + sectionZ) & 0xFF;
        int var14 = (var10 + sectionZ) & 0xFF;
        int var15 = (var11 + sectionZ) & 0xFF;
        int var16 = (var8 + sectionZ + 1) & 0xFF;
        int var17 = (var9 + sectionZ + 1) & 0xFF;
        int var18 = (var10 + sectionZ + 1) & 0xFF;
        int var19 = (var11 + sectionZ + 1) & 0xFF;

        int var20 = (perm[var12] & 15) << 2;
        int var21 = (perm[var13] & 15) << 2;
        int var22 = (perm[var14] & 15) << 2;
        int var23 = (perm[var15] & 15) << 2;
        int var24 = (perm[var16] & 15) << 2;
        int var25 = (perm[var17] & 15) << 2;
        int var26 = (perm[var18] & 15) << 2;
        int var27 = (perm[var19] & 15) << 2;

        double var60 = localX - 1.0;
        double var61 = localY - 1.0;
        double var62 = localZ - 1.0;

        double var87 = FLAT_SIMPLEX_GRAD[var20] * localX + FLAT_SIMPLEX_GRAD[var20 | 1] * localY + FLAT_SIMPLEX_GRAD[var20 | 2] * localZ;
        double var88 = FLAT_SIMPLEX_GRAD[var21] * var60 + FLAT_SIMPLEX_GRAD[var21 | 1] * localY + FLAT_SIMPLEX_GRAD[var21 | 2] * localZ;
        double var89 = FLAT_SIMPLEX_GRAD[var22] * localX + FLAT_SIMPLEX_GRAD[var22 | 1] * var61 + FLAT_SIMPLEX_GRAD[var22 | 2] * localZ;
        double var90 = FLAT_SIMPLEX_GRAD[var23] * var60 + FLAT_SIMPLEX_GRAD[var23 | 1] * var61 + FLAT_SIMPLEX_GRAD[var23 | 2] * localZ;
        double var91 = FLAT_SIMPLEX_GRAD[var24] * localX + FLAT_SIMPLEX_GRAD[var24 | 1] * localY + FLAT_SIMPLEX_GRAD[var24 | 2] * var62;
        double var92 = FLAT_SIMPLEX_GRAD[var25] * var60 + FLAT_SIMPLEX_GRAD[var25 | 1] * localY + FLAT_SIMPLEX_GRAD[var25 | 2] * var62;
        double var93 = FLAT_SIMPLEX_GRAD[var26] * localX + FLAT_SIMPLEX_GRAD[var26 | 1] * var61 + FLAT_SIMPLEX_GRAD[var26 | 2] * var62;
        double var94 = FLAT_SIMPLEX_GRAD[var27] * var60 + FLAT_SIMPLEX_GRAD[var27 | 1] * var61 + FLAT_SIMPLEX_GRAD[var27 | 2] * var62;

        double fadeX = perlinFade(localX);
        double fadeY = perlinFade(fadeLocalY);
        double fadeZ = perlinFade(localZ);

        return lerp3(fadeX, fadeY, fadeZ, var87, var88, var89, var90, var91, var92, var93, var94);
    }

    public static double sampleOctave(OctavePerlinNoiseSampler octaveSampler, double x, double y, double z) {
        IOctavePerlinNoiseSampler acc = (IOctavePerlinNoiseSampler) octaveSampler;
        PerlinNoiseSampler[] octaveSamplers = acc.getOctaveSamplers();
        double d = 0.0;
        double e = acc.getLacunarity();
        double f = acc.getPersistence();

        for (int i = 0; i < octaveSamplers.length; i++) {
            PerlinNoiseSampler sampler = octaveSamplers[i];
            if (sampler != null) {
                double g = samplePerlin(sampler, maintainPrecision(x * e), maintainPrecision(y * e), maintainPrecision(z * e), 0.0, 0.0);
                d += acc.getAmplitudes().getDouble(i) * g * f;
            }
            e *= 2.0;
            f /= 2.0;
        }
        return d;
    }

    public static double sampleDoublePerlin(DoublePerlinNoiseSampler sampler, double x, double y, double z) {
        IDoublePerlinNoiseSampler acc = (IDoublePerlinNoiseSampler) sampler;
        OctavePerlinNoiseSampler first = acc.getFirstSampler();
        OctavePerlinNoiseSampler second = acc.getSecondSampler();
        double d = x * 1.0181268882175227;
        double e = y * 1.0181268882175227;
        double f = z * 1.0181268882175227;
        return (sampleOctave(first, x, y, z) + sampleOctave(second, d, e, f)) * acc.getAmplitude();
    }

    public static void sampleDoublePerlinBatch(DoublePerlinNoiseSampler sampler, double[] densities, double[] x, double[] y, double[] z, int length) {
        int speciesLen = D_SPECIES.length();
        int i = 0;
        for (; i <= length - speciesLen; i += speciesLen) {
            DoubleVector vx = DoubleVector.fromArray(D_SPECIES, x, i);
            DoubleVector vy = DoubleVector.fromArray(D_SPECIES, y, i);
            DoubleVector vz = DoubleVector.fromArray(D_SPECIES, z, i);

            DoubleVector vRes = sampleDoublePerlinVector(sampler, vx, vy, vz);
            vRes.intoArray(densities, i);
        }

        if (i < length) {
            VectorMask<Double> mask = D_SPECIES.indexInRange(i, length);
            DoubleVector vx = DoubleVector.fromArray(D_SPECIES, x, i, mask);
            DoubleVector vy = DoubleVector.fromArray(D_SPECIES, y, i, mask);
            DoubleVector vz = DoubleVector.fromArray(D_SPECIES, z, i, mask);

            DoubleVector vRes = sampleDoublePerlinVector(sampler, vx, vy, vz);
            vRes.intoArray(densities, i, mask);
        }
    }

    public static DoubleVector sampleDoublePerlinVector(DoublePerlinNoiseSampler sampler, DoubleVector vx, DoubleVector vy, DoubleVector vz) {
        IDoublePerlinNoiseSampler acc = (IDoublePerlinNoiseSampler) sampler;
        DoubleVector vShift = DoubleVector.broadcast(D_SPECIES, 1.0181268882175227);
        DoubleVector vxShift = vx.mul(vShift);
        DoubleVector vyShift = vy.mul(vShift);
        DoubleVector vzShift = vz.mul(vShift);

        DoubleVector v1 = sampleOctaveVector(acc.getFirstSampler(), vx, vy, vz);
        DoubleVector v2 = sampleOctaveVector(acc.getSecondSampler(), vxShift, vyShift, vzShift);

        return v1.add(v2).mul(acc.getAmplitude());
    }

    public static DoubleVector sampleOctaveVector(OctavePerlinNoiseSampler octaveSampler, DoubleVector vx, DoubleVector vy, DoubleVector vz) {
        IOctavePerlinNoiseSampler acc = (IOctavePerlinNoiseSampler) octaveSampler;
        PerlinNoiseSampler[] octaveSamplers = acc.getOctaveSamplers();
        DoubleVector vD = DoubleVector.zero(D_SPECIES);
        double e = acc.getLacunarity();
        double f = acc.getPersistence();

        for (int i = 0; i < octaveSamplers.length; i++) {
            PerlinNoiseSampler sampler = octaveSamplers[i];
            if (sampler != null) {
                DoubleVector ve = DoubleVector.broadcast(D_SPECIES, e);
                DoubleVector vXm = maintainPrecision(vx.mul(ve));
                DoubleVector vYm = maintainPrecision(vy.mul(ve));
                DoubleVector vZm = maintainPrecision(vz.mul(ve));

                DoubleVector vG = samplePerlinVector(sampler, vXm, vYm, vZm);
                double amp = acc.getAmplitudes().getDouble(i) * f;
                vD = vD.add(vG.mul(amp));
            }
            e *= 2.0;
            f /= 2.0;
        }
        return vD;
    }

    public static DoubleVector samplePerlinVector(PerlinNoiseSampler sampler, DoubleVector vx, DoubleVector vy, DoubleVector vz) {
        IPerlinNoiseSampler acc = (IPerlinNoiseSampler) (Object) sampler;
        byte[] perm = acc.getPermutation();

        DoubleVector vd = vx.add(sampler.originX);
        DoubleVector ve = vy.add(sampler.originY);
        DoubleVector vf = vz.add(sampler.originZ);

        int len = D_SPECIES.length();
        double[] arrD = BUF_D.get();
        if (arrD.length < len) { arrD = new double[len]; BUF_D.set(arrD); }
        double[] arrE = BUF_E.get();
        if (arrE.length < len) { arrE = new double[len]; BUF_E.set(arrE); }
        double[] arrF = BUF_F.get();
        if (arrF.length < len) { arrF = new double[len]; BUF_F.set(arrF); }
        double[] res = BUF_RES.get();
        if (res.length < len) { res = new double[len]; BUF_RES.set(res); }

        vd.intoArray(arrD, 0);
        ve.intoArray(arrE, 0);
        vf.intoArray(arrF, 0);

        for (int lane = 0; lane < len; lane++) {
            double d = arrD[lane];
            double e = arrE[lane];
            double f = arrF[lane];
            double i = Math.floor(d);
            double j = Math.floor(e);
            double k = Math.floor(f);
            double g = d - i;
            double h = e - j;
            double l = f - k;

            res[lane] = samplePerlinRaw(perm, (int) i, (int) j, (int) k, g, h, l, h);
        }

        return DoubleVector.fromArray(D_SPECIES, res, 0);
    }
}
