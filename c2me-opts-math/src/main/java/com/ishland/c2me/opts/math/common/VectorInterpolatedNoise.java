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

import com.ishland.c2me.base.mixin.access.IInterpolatedNoiseSampler;
import net.minecraft.util.math.noise.InterpolatedNoiseSampler;
import net.minecraft.util.math.noise.OctavePerlinNoiseSampler;
import net.minecraft.util.math.noise.PerlinNoiseSampler;
import net.minecraft.world.gen.densityfunction.DensityFunction;

import static com.ishland.c2me.opts.math.common.VectorMathUtil.*;

public class VectorInterpolatedNoise {

    public static double sample(InterpolatedNoiseSampler interpolated, DensityFunction.NoisePos pos) {
        return sample(interpolated, pos.blockX(), pos.blockY(), pos.blockZ());
    }

    public static double sample(InterpolatedNoiseSampler interpolated, int blockX, int blockY, int blockZ) {
        IInterpolatedNoiseSampler acc = (IInterpolatedNoiseSampler) interpolated;

        double d = blockX * acc.getScaledXzScale();
        double e = blockY * acc.getScaledYScale();
        double f = blockZ * acc.getScaledXzScale();
        double g = d / acc.getXzFactor();
        double h = e / acc.getYFactor();
        double i = f / acc.getXzFactor();
        double j = acc.getScaledYScale() * acc.getSmearScaleMultiplier();
        double k = j / acc.getYFactor();

        double n = 0.0;
        OctavePerlinNoiseSampler normalSampler = acc.getInterpolationNoise();
        for (int offset = 0; offset < 8; offset++) {
            PerlinNoiseSampler sampler = normalSampler.getOctave(offset);
            if (sampler != null) {
                double mulFactor = Math.pow(2.0, -offset);
                n += VectorPerlinNoise.samplePerlin(sampler,
                        maintainPrecision(g * mulFactor),
                        maintainPrecision(h * mulFactor),
                        maintainPrecision(i * mulFactor),
                        k * mulFactor,
                        h * mulFactor) / mulFactor;
            }
        }

        double q = (n / 10.0 + 1.0) / 2.0;

        double l = 0.0;
        if (q < 1.0) {
            OctavePerlinNoiseSampler lowerSampler = acc.getLowerInterpolatedNoise();
            for (int offset = 0; offset < 16; offset++) {
                PerlinNoiseSampler sampler = lowerSampler.getOctave(offset);
                if (sampler != null) {
                    double mulFactor = Math.pow(2.0, -offset);
                    l += VectorPerlinNoise.samplePerlin(sampler,
                            maintainPrecision(d * mulFactor),
                            maintainPrecision(e * mulFactor),
                            maintainPrecision(f * mulFactor),
                            j * mulFactor,
                            e * mulFactor) / mulFactor;
                }
            }
        }

        double m = 0.0;
        if (q > 0.0) {
            OctavePerlinNoiseSampler upperSampler = acc.getUpperInterpolatedNoise();
            for (int offset = 0; offset < 16; offset++) {
                PerlinNoiseSampler sampler = upperSampler.getOctave(offset);
                if (sampler != null) {
                    double mulFactor = Math.pow(2.0, -offset);
                    m += VectorPerlinNoise.samplePerlin(sampler,
                            maintainPrecision(d * mulFactor),
                            maintainPrecision(e * mulFactor),
                            maintainPrecision(f * mulFactor),
                            j * mulFactor,
                            e * mulFactor) / mulFactor;
                }
            }
        }

        return clampedLerp(l / 512.0, m / 512.0, q) / 128.0;
    }
}
