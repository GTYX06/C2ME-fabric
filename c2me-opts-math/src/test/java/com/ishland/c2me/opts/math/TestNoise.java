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

package com.ishland.c2me.opts.math;

import com.ishland.c2me.opts.math.common.VectorBiomeAccess;
import com.ishland.c2me.opts.math.common.VectorInterpolatedNoise;
import com.ishland.c2me.opts.math.common.VectorPerlinNoise;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.noise.DoublePerlinNoiseSampler;
import net.minecraft.util.math.noise.InterpolatedNoiseSampler;
import net.minecraft.util.math.random.CheckedRandom;
import net.minecraft.util.math.random.ChunkRandom;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.SeedMixer;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestNoise {

    @BeforeAll
    public static void setup() {
        SharedConstants.createGameVersion();
        Bootstrap.initialize();
    }

    @Test
    public void testDoublePerlinNoise() {
        ChunkRandom random = new ChunkRandom(new CheckedRandom(12345L));
        DoublePerlinNoiseSampler sampler = DoublePerlinNoiseSampler.create(random, -2, 1.0, 2.0, 1.0);

        for (int i = 0; i < 100; i++) {
            double x = i * 10.5 - 500;
            double y = (i % 32) * 4.0;
            double z = i * -15.2 + 300;

            double vanillaVal = sampler.sample(x, y, z);
            double vectorVal = VectorPerlinNoise.sampleDoublePerlin(sampler, x, y, z);

            assertEquals(vanillaVal, vectorVal, 1e-6, "Mismatch at i=" + i);
        }
    }

    @Test
    public void testDoublePerlinNoiseBatch() {
        ChunkRandom random = new ChunkRandom(new CheckedRandom(12345L));
        DoublePerlinNoiseSampler sampler = DoublePerlinNoiseSampler.create(random, -2, 1.0, 2.0, 1.0);

        int count = 100;
        double[] x = new double[count];
        double[] y = new double[count];
        double[] z = new double[count];
        double[] densities = new double[count];

        for (int i = 0; i < count; i++) {
            x[i] = i * 10.5 - 500;
            y[i] = (i % 32) * 4.0;
            z[i] = i * -15.2 + 300;
        }

        VectorPerlinNoise.sampleDoublePerlinBatch(sampler, densities, x, y, z, count);

        for (int i = 0; i < count; i++) {
            double vanillaVal = sampler.sample(x[i], y[i], z[i]);
            assertEquals(vanillaVal, densities[i], 1e-6, "Batch mismatch at i=" + i);
        }
    }

    @Test
    public void testInterpolatedNoise() {
        InterpolatedNoiseSampler sampler = InterpolatedNoiseSampler.createBase3dNoiseFunction(0.25, 0.125, 80.0, 160.0, 8.0);

        for (int i = 0; i < 100; i++) {
            int bx = i * 16;
            int by = (i % 32) * 4;
            int bz = i * -16;
            DensityFunction.NoisePos pos = new DensityFunction.UnblendedNoisePos(bx, by, bz);

            double vanillaVal = sampler.sample(pos);
            double vectorVal = VectorInterpolatedNoise.sample(sampler, pos);

            assertEquals(vanillaVal, vectorVal, 1e-6, "InterpolatedNoise mismatch at i=" + i);
        }
    }

    public static int sampleVanilla(long theSeed, int x, int y , int z) {
        int i = x - 2;
        int j = y - 2;
        int k = z - 2;
        int l = i >> 2;
        int m = j >> 2;
        int n = k >> 2;
        double d = (double)(i & 3) / 4.0;
        double e = (double)(j & 3) / 4.0;
        double f = (double)(k & 3) / 4.0;
        int o = 0;
        double g = Double.POSITIVE_INFINITY;

        for(int p = 0; p < 8; ++p) {
            boolean bl = (p & 4) == 0;
            boolean bl2 = (p & 2) == 0;
            boolean bl3 = (p & 1) == 0;
            int q = bl ? l : l + 1;
            int r = bl2 ? m : m + 1;
            int s = bl3 ? n : n + 1;
            double h = bl ? d : d - 1.0;
            double t = bl2 ? e : e - 1.0;
            double u = bl3 ? f : f - 1.0;
            double v = method_38106(theSeed, q, r, s, h, t, u);
            if (g > v) {
                o = p;
                g = v;
            }
        }

        return o;
    }

    private static double method_38108(long l) {
        double d = (double)Math.floorMod(l >> 24, 1024) / 1024.0;
        return (d - 0.5) * 0.9;
    }

    private static double method_38106(long l, int i, int j, int k, double d, double e, double f) {
        long m = SeedMixer.mixSeed(l, (long)i);
        m = SeedMixer.mixSeed(m, (long)j);
        m = SeedMixer.mixSeed(m, (long)k);
        m = SeedMixer.mixSeed(m, (long)i);
        m = SeedMixer.mixSeed(m, (long)j);
        m = SeedMixer.mixSeed(m, (long)k);
        double g = method_38108(m);
        m = SeedMixer.mixSeed(m, l);
        double h = method_38108(m);
        m = SeedMixer.mixSeed(m, l);
        double n = method_38108(m);
        return (f + n) * (f + n) + (e + h) * (e + h) + (d + g) * (d + g);
    }

    @Test
    public void testBiomeAccessVsVanilla() {
        long seed = 123456789L;
        for (int x = -100; x <= 100; x += 13) {
            for (int y = -30; y <= 30; y += 7) {
                for (int z = -100; z <= 100; z += 17) {
                    int vanillaMask = sampleVanilla(seed, x, y, z);
                    int vectorMask = VectorBiomeAccess.sample(seed, x, y, z);

                    assertEquals(vanillaMask, vectorMask, "Mask mismatch at x=" + x + " y=" + y + " z=" + z);
                }
            }
        }
    }
}
