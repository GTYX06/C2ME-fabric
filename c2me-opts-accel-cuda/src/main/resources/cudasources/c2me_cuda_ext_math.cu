/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2026 GTYX06
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

#pragma once

typedef signed char int8_t;
typedef unsigned char uint8_t;
typedef short int16_t;
typedef unsigned short uint16_t;
typedef int int32_t;
typedef unsigned int uint32_t;
typedef long long int64_t;
typedef unsigned long long uint64_t;
typedef unsigned long long size_t;

#ifndef NULL
#define NULL nullptr
#endif

#ifndef FUNC_NOINLINE
#define FUNC_NOINLINE __noinline__
#endif

#ifndef FUNC_NOINLINE_MIDDF
#define FUNC_NOINLINE_MIDDF __noinline__
#endif

#ifndef UINT32_MAX
#define UINT32_MAX 0xffffffffU
#endif
#ifndef INT32_MAX
#define INT32_MAX 0x7fffffff
#endif
#ifndef UINT64_MAX
#define UINT64_MAX 0xffffffffffffffffULL
#endif
#ifndef INT64_MAX
#define INT64_MAX 0x7fffffffffffffffLL
#endif

__constant__ static const double FLAT_SIMPLEX_GRAD[16][3] = {
        {1, 1, 0},
        {-1, 1, 0},
        {1, -1, 0},
        {-1, -1, 0},
        {1, 0, 1},
        {-1, 0, 1},
        {1, 0, -1},
        {-1, 0, -1},
        {0, 1, 1},
        {0, -1, 1},
        {0, 1, -1},
        {0, -1, -1},
        {1, 1, 0},
        {0, -1, 1},
        {-1, 1, 0},
        {0, -1, -1},
};

static const double SQRT_3 = 1.7320508075688772;
static const double SKEW_FACTOR_2D = 0.3660254037844386;
static const double UNSKEW_FACTOR_2D = 0.21132486540518713;

__device__ __forceinline__ static void *ptr_shift(const void * const ptr, const int32_t shift) {
    return (void *) (((uint8_t *) ptr) + shift);
}

__device__ __forceinline__ static const void *ptr_shift_const(const void * const ptr, const int32_t shift) {
    return (const void *) (((const uint8_t *) ptr) + shift);
}

__device__ __forceinline__ static double math_floor(const double v) {
    return floor(v);
}

__device__ __forceinline__ static uint64_t math_rotateLeftU64(uint64_t i, uint64_t distance) {
    return (i << distance) | (i >> (64 - distance));
}

__device__ __forceinline__ static int32_t math_floorDiv(const int32_t x, const int32_t y) {
    int r = x / y;
    if ((x ^ y) < 0 && (r * y != x)) {
        r--;
    }
    return r;
}

__device__ __forceinline__ static double math_octave_maintainPrecision(const double value) {
    return value - math_floor(value / 3.3554432E7 + 0.5) * 3.3554432E7;
}

__device__ __forceinline__ static double math_simplex_grad(const int32_t hash, const double x, const double y,
                                                          const double z, const double distance) {
    double d = distance - x * x - y * y - z * z;
    if (d < 0.0) {
        return 0.0;
    } else {
        double var0 = FLAT_SIMPLEX_GRAD[hash][0] * x;
        double var1 = FLAT_SIMPLEX_GRAD[hash][1] * y;
        double var2 = FLAT_SIMPLEX_GRAD[hash][2] * z;
        return d * d * d * d * (var0 + var1 + var2);
    }
}

__device__ __forceinline__ static double math_lerp(const double delta, const double start, const double end) {
    return start + delta * (end - start);
}

__device__ __forceinline__ static float math_lerpf(const float delta, const float start, const float end) {
    return start + delta * (end - start);
}

__device__ __forceinline__ static double math_clampedLerp(const double start, const double end, const double delta) {
    if (delta < 0.0) {
        return start;
    } else {
        return delta > 1.0 ? end : math_lerp(delta, start, end);
    }
}

__device__ __forceinline__ static double math_square(const double operand) {
    return operand * operand;
}

__device__ __forceinline__ static double math_lerp2(const double deltaX, const double deltaY, const double x0y0,
                                                   const double x1y0, const double x0y1, const double x1y1) {
    return math_lerp(deltaY, math_lerp(deltaX, x0y0, x1y0), math_lerp(deltaX, x0y1, x1y1));
}

__device__ __forceinline__ static double math_lerp3(
        const double deltaX,
        const double deltaY,
        const double deltaZ,
        const double x0y0z0,
        const double x1y0z0,
        const double x0y1z0,
        const double x1y1z0,
        const double x0y0z1,
        const double x1y0z1,
        const double x0y1z1,
        const double x1y1z1
) {
    return math_lerp(deltaZ, math_lerp2(deltaX, deltaY, x0y0z0, x1y0z0, x0y1z0, x1y1z0),
                     math_lerp2(deltaX, deltaY, x0y0z1, x1y0z1, x0y1z1, x1y1z1));
}

__device__ __forceinline__ static double math_getLerpProgress(const double value, const double start,
                                                             const double end) {
    return (value - start) / (end - start);
}

__device__ __forceinline__ static double
math_clampedLerpFromProgress(const double lerpValue, const double lerpStart, const double lerpEnd, const double start,
                             const double end) {
    return math_clampedLerp(start, end, math_getLerpProgress(lerpValue, lerpStart, lerpEnd));
}

__device__ __forceinline__ static double math_squeeze(const double x) {
    const double v = fmin(fmax(x, -1.0), 1.0);
    return v * (0.5 - (v * v) * (1.0 / 24.0));
}

__device__ __forceinline__ static float math_squeeze(const float x) {
    const float v = fminf(fmaxf(x, -1.0f), 1.0f);
    return v * (0.5f - (v * v) * (1.0f / 24.0f));
}

__device__ __forceinline__ static int32_t math_floorMod(const int32_t x, const int32_t y) {
    int32_t mod = x % y;
    if ((mod ^ y) < 0 && mod != 0) {
        mod += y;
    }
    return mod;
}

__device__ __forceinline__ static int32_t math_biome2block(const int32_t biomeCoord) {
    return biomeCoord << 2;
}

__device__ __forceinline__ static int32_t math_block2biome(const int32_t blockCoord) {
    return blockCoord >> 2;
}

__device__ __forceinline__ static uint32_t
__math_simplex_map(const uint32_t * __restrict__ permutations, const int32_t input) {
    return permutations[input & 0xFF];
}

__device__ __forceinline__ static double math_simplex_dot(const int32_t hash, const double x, const double y,
                                                         const double z) {
    return FLAT_SIMPLEX_GRAD[hash][0] * x + FLAT_SIMPLEX_GRAD[hash][1] * y + FLAT_SIMPLEX_GRAD[hash][2] * z;
}

__device__ __forceinline__ static double __math_simplex_grad(const int32_t hash, const double x, const double y,
                                                             const double z, const double distance) {
    double d = distance - x * x - y * y - z * z;
    if (d < 0.0) {
        return 0.0;
    } else {
        d *= d;
        return d * d * math_simplex_dot(hash, x, y, z);
    }
}

__device__ static double
math_noise_simplex_sample2d(const uint32_t * __restrict__ permutations, const double x, const double y) {
    const double d = (x + y) * SKEW_FACTOR_2D;
    const double i = math_floor(x + d);
    const double j = math_floor(y + d);
    const double e = (i + j) * UNSKEW_FACTOR_2D;
    const double f = i - e;
    const double g = j - e;
    const double h = x - f;
    const double k = y - g;
    double l;
    int32_t li;
    double m;
    int32_t mi;
    if (h > k) {
        l = 1;
        li = 1;
        m = 0;
        mi = 0;
    } else {
        l = 0;
        li = 1;
        m = 1;
        mi = 1;
    }

    const double n = h - (double) l + UNSKEW_FACTOR_2D;
    const double o = k - (double) m + UNSKEW_FACTOR_2D;
    const double p = h - 1.0 + 2.0 * UNSKEW_FACTOR_2D;
    const double q = k - 1.0 + 2.0 * UNSKEW_FACTOR_2D;
    const int32_t r = (int32_t) i & 0xFF;
    const int32_t s = (int32_t) j & 0xFF;
    const int32_t t = __math_simplex_map(permutations, r + __math_simplex_map(permutations, s)) % 12;
    const int32_t u = __math_simplex_map(permutations, r + li + __math_simplex_map(permutations, s + mi)) % 12;
    const int32_t v = __math_simplex_map(permutations, r + 1 + __math_simplex_map(permutations, s + 1)) % 12;
    const double w = __math_simplex_grad(t, h, k, 0.0, 0.5);
    const double z = __math_simplex_grad(u, n, o, 0.0, 0.5);
    const double aa = __math_simplex_grad(v, p, q, 0.0, 0.5);
    return 70.0 * (w + z + aa);
}

__device__ __forceinline__ static double math_perlinFade(const double value) {
    return value * value * value * (value * (value * 6.0 - 15.0) + 10.0);
}

__device__ static double __math_perlin_grad(const uint8_t * __restrict__ permutations, const int32_t px,
                                            const int32_t py, const int32_t pz, const double fx,
                                            const double fy, const double fz) {
    const uint32_t map0 = (((uint32_t) permutations[((uint32_t) px) & 0xFF]) + ((uint32_t) py));
    const uint32_t map1 = (((uint32_t) permutations[map0 & 0xFF]) + ((uint32_t) pz));
    const uint32_t hash = permutations[map1 & 0xFF] & 0xF;
    return FLAT_SIMPLEX_GRAD[hash][0] * fx + FLAT_SIMPLEX_GRAD[hash][1] * fy + FLAT_SIMPLEX_GRAD[hash][2] * fz;
}

__device__ static double
math_noise_perlin_sampleScalar(const uint8_t * __restrict__ permutations,
                               const int32_t px0, const int32_t py0, const int32_t pz0,
                               const double fx0, const double fy0, const double fz0, const double fadeLocalY) {
    const int32_t px1 = px0 + 1;
    const int32_t py1 = py0 + 1;
    const int32_t pz1 = pz0 + 1;
    const double fx1 = fx0 - 1;
    const double fy1 = fy0 - 1;
    const double fz1 = fz0 - 1;

    double f000 = __math_perlin_grad(permutations, px0, py0, pz0, fx0, fy0, fz0);
    double f100 = __math_perlin_grad(permutations, px1, py0, pz0, fx1, fy0, fz0);
    double f010 = __math_perlin_grad(permutations, px0, py1, pz0, fx0, fy1, fz0);
    double f110 = __math_perlin_grad(permutations, px1, py1, pz0, fx1, fy1, fz0);
    double f001 = __math_perlin_grad(permutations, px0, py0, pz1, fx0, fy0, fz1);
    double f101 = __math_perlin_grad(permutations, px1, py0, pz1, fx1, fy0, fz1);
    double f011 = __math_perlin_grad(permutations, px0, py1, pz1, fx0, fy1, fz1);
    double f111 = __math_perlin_grad(permutations, px1, py1, pz1, fx1, fy1, fz1);

    double fadeX = math_perlinFade(fx0);
    double fadeY = fadeLocalY;
    double fadeZ = math_perlinFade(fz0);

    return math_lerp3(fadeX, fadeY, fadeZ, f000, f100, f010, f110, f001, f101, f011, f111);
}

typedef struct double_perlin_noise_sampler {
    double amplitude;
    double octave_samplers[1];
} double_perlin_noise_sampler_t;

typedef double_perlin_noise_sampler_t double_octave_sampler_data_t;

typedef struct perlin_noise_octave_sampler {
    double originX;
    double originY;
    double originZ;
    uint8_t permutations[256];
} perlin_noise_octave_sampler_t;

__device__ static double
math_noise_perlin_octave_sample(const perlin_noise_octave_sampler_t * const octave, const double x, const double y,
                                const double z, const double yScale, const double yMax) {
    double d = x + octave->originX;
    double e = y + octave->originY;
    double f = z + octave->originZ;
    double g = math_floor(d);
    double h = math_floor(e);
    double i = math_floor(f);
    double j = d - g;
    double k = e - h;
    double l = f - i;
    double o = 0.0;
    if (yScale != 0.0) {
        double m;
        if (yMax >= 0.0 && yMax < k) {
            m = yMax;
        } else {
            m = k;
        }

        o = math_floor(m / yScale + 1.0000001E-7) * yScale;
    }

    return math_noise_perlin_sampleScalar(octave->permutations, (int32_t) g, (int32_t) h, (int32_t) i, j, k - o, l,
                                          math_perlinFade(k));
}

__device__ static double
math_noise_perlin_octaves_sample(const double * const octaves_ptr, const double x, const double y, const double z) {
    double total = 0.0;
    double inputFactor = 1.0;
    const double *current = octaves_ptr;
    while (*current != 0.0) {
        const double factor = *current;
        const perlin_noise_octave_sampler_t *octave = (const perlin_noise_octave_sampler_t *) (current + 1);
        total += factor * math_noise_perlin_octave_sample(octave,
                                                          math_octave_maintainPrecision(x * inputFactor),
                                                          math_octave_maintainPrecision(y * inputFactor),
                                                          math_octave_maintainPrecision(z * inputFactor),
                                                          0.0, 0.0) / inputFactor;
        inputFactor *= 2.0;
        current = (const double *) ptr_shift_const(octave, sizeof(perlin_noise_octave_sampler_t));
    }
    return total;
}

__device__ static double
math_noise_perlin_double_sample(const double_perlin_noise_sampler_t * const sampler, const double x, const double y,
                                const double z) {
    const double first_res = math_noise_perlin_octaves_sample(sampler->octave_samplers, x, y, z);
    const double *second_octaves = sampler->octave_samplers;
    while (*second_octaves != 0.0) {
        second_octaves = (const double *) ptr_shift_const(second_octaves, sizeof(double) + sizeof(perlin_noise_octave_sampler_t));
    }
    second_octaves++;
    const double second_res = math_noise_perlin_octaves_sample(second_octaves, x * 1.0181268882175227, y * 1.0181268882175227, z * 1.0181268882175227);
    return (first_res + second_res) * sampler->amplitude;
}

typedef struct interpolated_noise_sampler {
    double scaledXzScale;
    double scaledYScale;
    double xzFactor;
    double yFactor;
    double smearScaleMultiplier;
    double octaves[1];
} interpolated_noise_sampler_t;

__device__ static double
math_noise_interpolated_sample(const interpolated_noise_sampler_t * const sampler, const double x, const double y,
                               const double z) {
    const double *lower_octaves = sampler->octaves;
    const double *upper_octaves = lower_octaves;
    while (*upper_octaves != 0.0) {
        upper_octaves = (const double *) ptr_shift_const(upper_octaves, sizeof(double) + sizeof(perlin_noise_octave_sampler_t));
    }
    upper_octaves++;
    const double *interpolation_octaves = upper_octaves;
    while (*interpolation_octaves != 0.0) {
        interpolation_octaves = (const double *) ptr_shift_const(interpolation_octaves, sizeof(double) + sizeof(perlin_noise_octave_sampler_t));
    }
    interpolation_octaves++;

    double d = 0.0;
    double e = 0.0;
    double f = 0.0;
    double g = 1.0;

    for (int32_t i = 0; i < 8; ++i) {
        const perlin_noise_octave_sampler_t *octave = (const perlin_noise_octave_sampler_t *) (interpolation_octaves + 1);
        if (octave) {
            f += math_noise_perlin_octave_sample(octave,
                                                 math_octave_maintainPrecision(x * sampler->xzFactor * g),
                                                 math_octave_maintainPrecision(y * sampler->yFactor * g),
                                                 math_octave_maintainPrecision(z * sampler->xzFactor * g),
                                                 sampler->yFactor * g,
                                                 y * sampler->yFactor * g) / g;
        }
        g /= 2.0;
        interpolation_octaves = (const double *) ptr_shift_const(octave, sizeof(perlin_noise_octave_sampler_t));
    }

    double h = (f / 10.0 + 1.0) / 2.0;
    bool bl = h >= 1.0;
    bool bl2 = h <= 0.0;
    g = 1.0;

    for (int32_t j = 0; j < 16; ++j) {
        double k = math_octave_maintainPrecision(x * sampler->scaledXzScale * g);
        double l = math_octave_maintainPrecision(y * sampler->scaledYScale * g);
        double m = math_octave_maintainPrecision(z * sampler->scaledXzScale * g);
        double n = sampler->scaledYScale * g;
        if (!bl) {
            const perlin_noise_octave_sampler_t *octave = (const perlin_noise_octave_sampler_t *) (lower_octaves + 1);
            if (octave) {
                d += math_noise_perlin_octave_sample(octave, k, l, m, n, y * n) / g;
            }
            lower_octaves = (const double *) ptr_shift_const(octave, sizeof(perlin_noise_octave_sampler_t));
        }

        if (!bl2) {
            const perlin_noise_octave_sampler_t *octave = (const perlin_noise_octave_sampler_t *) (upper_octaves + 1);
            if (octave) {
                e += math_noise_perlin_octave_sample(octave, k, l, m, n, y * n) / g;
            }
            upper_octaves = (const double *) ptr_shift_const(octave, sizeof(perlin_noise_octave_sampler_t));
        }

        g /= 2.0;
    }

    return math_clampedLerp(d / 512.0, e / 512.0, h) / 128.0;
}

__device__ static float math_noise_simplex_endIslands_sample(const uint32_t * __restrict__ permutations, const int32_t x,
                                                             const int32_t z) {
    const int32_t i = x / 2;
    const int32_t j = z / 2;
    const int32_t k = x % 2;
    const int32_t l = z % 2;
    float f = 100.0F - sqrtf((float) (x * x + z * z)) * 8.0F;
    f = fminf(fmaxf(f, -100.0F), 80.0F);

    for (int32_t m = -12; m <= 12; ++m) {
        for (int32_t n = -12; n <= 12; ++n) {
            const int64_t o = i + m;
            const int64_t p = j + n;
            if (o * o + p * p > 4096LL && math_noise_simplex_sample2d(permutations, (double) o, (double) p) < -0.8999999761581421) {
                const float g = (float) (((o < 0 ? -o : o) * 3439LL + (p < 0 ? -p : p) * 147LL) % 13LL + 9LL);
                const float h = (float) (k - m * 2);
                const float q = (float) (l - n * 2);
                float r = 100.0F - sqrtf(h * h + q * q) * g;
                r = fminf(fmaxf(r, -100.0F), 80.0F);
                f = fmaxf(f, r);
            }
        }
    }

    return f;
}

__device__ static double math_roundDownToMultiple(const double value, const int32_t multiple) {
    return (double) (math_floorDiv((int32_t) value, multiple) * multiple);
}

typedef struct {
    uint32_t length;
} spline_data_t;

__device__ static inline int math_spline_findRange(const float *locations, int length, float x) {
    int min = 0;
    int i = length;
    while (i > 0) {
        int j = i / 2;
        int k = min + j;
        if (x < locations[k]) {
            i = j;
        } else {
            min = k + 1;
            i -= j + 1;
        }
    }
    return min - 1;
}

__device__ static inline float math_spline_sampleOutsideRange(float point, const float *locations, float value, const float *derivatives, int i) {
    float f = derivatives[i];
    return f == 0.0f ? value : value + f * (point - locations[i]);
}

__device__ static inline float math_spline_sample(const spline_data_t *spline, float point) {
    const uint32_t *header = (const uint32_t *) spline;
    uint32_t len = header[0];
    const float *locations = (const float *) (header + 1);
    const float *values = locations + len;
    const float *derivatives = values + len;
    if (len == 1) {
        return math_spline_sampleOutsideRange(point, locations, values[0], derivatives, 0);
    }
    int range = math_spline_findRange(locations, len, point);
    if (range < 0) {
        return math_spline_sampleOutsideRange(point, locations, values[0], derivatives, 0);
    }
    if (range >= len - 1) {
        return math_spline_sampleOutsideRange(point, locations, values[len - 1], derivatives, len - 1);
    }
    float loc0 = locations[range];
    float loc1 = locations[range + 1];
    float locDist = loc1 - loc0;
    float k = (point - loc0) / locDist;
    float n = values[range];
    float o = values[range + 1];
    float onDist = o - n;
    float p = derivatives[range] * locDist - onDist;
    float q = -derivatives[range + 1] * locDist + onDist;
    return (n + k * (o - n)) + k * (1.0f - k) * (p + k * (q - p));
}

typedef struct worldgen_params {
    int32_t startBiomeX;
    int32_t startBiomeZ;
    int32_t sizeBiomeX;
    int32_t sizeBiomeZ;

    int32_t startCellX;
    int32_t startCellY;
    int32_t startCellZ;
    int32_t sizeCellX;
    int32_t sizeCellY;
    int32_t sizeCellZ;

    int32_t estimateSurfaceHeight_startBiomeX;
    int32_t estimateSurfaceHeight_startBiomeZ;
    int32_t estimateSurfaceHeight_sizeBiomeX;
    int32_t estimateSurfaceHeight_sizeBiomeZ;

    int32_t cache2d_startX;
    int32_t cache2d_startZ;
    int32_t cache2d_sizeX;
    int32_t cache2d_sizeZ;

    int32_t offset_estimateSurfaceHeight;
    int32_t genConfig_defaultBlock;
    int32_t genConfig_defaultFluid;
    int32_t offset_aquifer;
    int32_t offset_fluidLevelSampler;
    int32_t offset_oreVeinRandom;
} worldgen_params_t;

typedef struct sample_int32_ctx {
    const void * const_data;
    void *rw_data;
    int32_t x;
    int32_t y;
    int32_t z;
    uint32_t sample_flags;
} sample_int32_ctx_t;

static const uint32_t MASK_enableFlatCache = 1U << 0;
static const uint32_t MASK_enableAllCaches = 1U << 1;

__device__ __forceinline__ static sample_int32_ctx_t make_sample_int32_ctx(const void *const_data, void *rw_data, int32_t x, int32_t y, int32_t z, uint32_t sample_flags) {
    sample_int32_ctx_t ctx;
    ctx.const_data = const_data;
    ctx.rw_data = rw_data;
    ctx.x = x;
    ctx.y = y;
    ctx.z = z;
    ctx.sample_flags = sample_flags;
    return ctx;
}

__device__ __forceinline__ static uint32_t df_address_flatcache_buffer(const worldgen_params_t *params, int32_t id, int32_t relBiomeX, int32_t relBiomeZ) {
    uint32_t buffer_size = params->sizeBiomeX * params->sizeBiomeZ;
    return (uint32_t) id * buffer_size + (uint32_t) (relBiomeX * params->sizeBiomeZ + relBiomeZ);
}

__device__ __forceinline__ static uint32_t df_address_cache2d_buffer(const worldgen_params_t *params, int32_t id, int32_t relBlockX, int32_t relBlockZ) {
    uint32_t buffer_size = params->cache2d_sizeX * params->cache2d_sizeZ;
    return (uint32_t) id * buffer_size + (uint32_t) (relBlockX * params->cache2d_sizeZ + relBlockZ);
}

__device__ __forceinline__ static uint32_t df_address_interpolator_buffer(const worldgen_params_t *params, int32_t id, int32_t cellRelX, int32_t cellRelY, int32_t cellRelZ) {
    uint32_t buffer_size = (params->sizeCellX + 1) * (params->sizeCellY + 1) * (params->sizeCellZ + 1);
    return (uint32_t) id * buffer_size + (uint32_t) ((cellRelX * (params->sizeCellY + 1) + cellRelY) * (params->sizeCellZ + 1) + cellRelZ);
}

__device__ __forceinline__ static double *df_data_offset(void *rw_data, int32_t offset) {
    const uint32_t *table = (const uint32_t *) ptr_shift(rw_data, 128);
    return (double *) ptr_shift(rw_data, table[offset]);
}

extern __device__ double df_binding_barrier(const sample_int32_ctx_t ctx);
extern __device__ double df_binding_fluid_level_floodedness(const sample_int32_ctx_t ctx);
extern __device__ double df_binding_fluid_level_spread(const sample_int32_ctx_t ctx);
extern __device__ double df_binding_lava(const sample_int32_ctx_t ctx);
extern __device__ double df_binding_temperature(const sample_int32_ctx_t ctx);
extern __device__ double df_binding_vegetation(const sample_int32_ctx_t ctx);
extern __device__ double df_binding_continents(const sample_int32_ctx_t ctx);
extern __device__ double df_binding_erosion(const sample_int32_ctx_t ctx);
extern __device__ double df_binding_depth(const sample_int32_ctx_t ctx);
extern __device__ double df_binding_ridges(const sample_int32_ctx_t ctx);
extern __device__ double df_binding_preliminary_surface_level(const sample_int32_ctx_t ctx);
extern __device__ double df_binding_final_density(const sample_int32_ctx_t ctx);
extern __device__ double df_binding_vein_toggle(const sample_int32_ctx_t ctx);
extern __device__ double df_binding_vein_ridged(const sample_int32_ctx_t ctx);
extern __device__ double df_binding_vein_gap(const sample_int32_ctx_t ctx);
extern __device__ double df_binding_final_final_density(const sample_int32_ctx_t ctx);

extern __constant__ const int32_t genShapeCfg_minimumY;
extern __constant__ const int32_t genShapeCfg_height;
extern __constant__ const uint32_t genShapeCfg_horizontalSize;
extern __constant__ const uint32_t genShapeCfg_verticalSize;

__device__ __forceinline__ static uint32_t genShapeCfg_horizontalCellBlockCount() {
    return genShapeCfg_horizontalSize << 2;
}

__device__ __forceinline__ static uint32_t genShapeCfg_verticalCellBlockCount() {
    return genShapeCfg_verticalSize << 2;
}

__device__ static int32_t chunkNoiseSampler_estimateSurfaceHeight0(const void *const_data, void *rw_data, const int32_t blockX, const int32_t blockZ) {
    const int32_t cellBlockCount = genShapeCfg_verticalCellBlockCount();
    const int32_t minCellY = math_floorDiv(genShapeCfg_minimumY, cellBlockCount);
    const int32_t cellCountY = math_floorDiv(genShapeCfg_height, cellBlockCount);
    for (int32_t cellY = cellCountY - 1; cellY >= 0; --cellY) {
        const int32_t blockY = (minCellY + cellY) * cellBlockCount;
        const double d = df_binding_preliminary_surface_level(make_sample_int32_ctx(const_data, rw_data, blockX, blockY, blockZ, MASK_enableFlatCache));
        if (d > 0.390625) {
            return blockY;
        }
    }
    return INT32_MAX;
}

__device__ static double chunkNoiseSampler_estimateSurfaceHeight(const sample_int32_ctx_t ctx) {
    const worldgen_params_t *params = (const worldgen_params_t *) ctx.rw_data;
    const int32_t *cache = (const int32_t *) ptr_shift_const(ctx.rw_data, params->offset_estimateSurfaceHeight);
    int32_t biomeX = math_block2biome(ctx.x);
    int32_t biomeZ = math_block2biome(ctx.z);
    int32_t relX = biomeX - params->estimateSurfaceHeight_startBiomeX;
    int32_t relZ = biomeZ - params->estimateSurfaceHeight_startBiomeZ;
    if (relX < 0 || relZ < 0 || relX >= params->estimateSurfaceHeight_sizeBiomeX || relZ >= params->estimateSurfaceHeight_sizeBiomeZ) {
        return 0.0;
    } else {
        return (double) cache[relX * params->estimateSurfaceHeight_sizeBiomeZ + relZ];
    }
}

#ifdef DF_COMPILE_ESTIMATE_SURFACE_HEIGHT
extern "C" __global__ void chunkNoiseSampler_estimateSurfaceHeight_prefill_indep(
        const void * __restrict__ const_data, void * __restrict__ rw_data,
        int32_t * __restrict__ const cache,
        const int32_t startChunkX, const int32_t startChunkZ, const uint32_t cacheWidth) {
    int32_t relX = blockIdx.x * blockDim.x + threadIdx.x;
    int32_t relZ = blockIdx.y * blockDim.y + threadIdx.y;
    if (relX >= cacheWidth || relZ >= cacheWidth) return;

    int32_t biomeX = (startChunkX << 2) + relX;
    int32_t biomeZ = (startChunkZ << 2) + relZ;
    int32_t blockX = math_biome2block(biomeX);
    int32_t blockZ = math_biome2block(biomeZ);
    cache[relX * cacheWidth + relZ] = chunkNoiseSampler_estimateSurfaceHeight0(const_data, rw_data, blockX, blockZ);
}
#endif

static const uint64_t RANDOM_Checked = 0;
static const uint64_t RANDOM_Xoroshiro128PlusPlus = 1;

typedef struct random_state {
    uint64_t type;
    uint64_t seedLo;
    uint64_t seedHi;
} random_state_t;

__device__ __forceinline__ static int64_t math_hashCode_int32x3(int32_t x, int32_t y, int32_t z) {
    int64_t l = (int64_t)(x * 3129871) ^ (int64_t)z * 116129781LL ^ (int64_t)y;
    l = l * l * 42317861LL + l * 11LL;
    return l >> 16;
}

__device__ __forceinline__ static uint64_t math_mixStafford13(uint64_t seed) {
    seed = ((int64_t) (seed ^ seed >> 30)) * -4658895280553007687LL;
    seed = ((int64_t) (seed ^ seed >> 27)) * -7723592293110705685LL;
    return seed ^ seed >> 31;
}

__device__ static void random_state_set_seed(random_state_t *state, int64_t seed) {
    if (state->type == RANDOM_Checked) {
        state->seedLo = (seed ^ 25214903917LL) & 281474976710655LL;
    } else if (state->type == RANDOM_Xoroshiro128PlusPlus) {
        state->seedLo = seed ^ 7640891576956012809LL;
        state->seedHi = ((int64_t) state->seedLo) + -7046029254386353131LL;
        state->seedLo = math_mixStafford13(state->seedLo);
        state->seedHi = math_mixStafford13(state->seedHi);
    }
}

__device__ static void random_state_split_coords(random_state_t *state, int32_t x, int32_t y, int32_t z) {
    if (state->type == RANDOM_Checked) {
        int64_t l = math_hashCode_int32x3(x, y, z);
        state->seedLo ^= l;
        random_state_set_seed(state, state->seedLo);
    } else if (state->type == RANDOM_Xoroshiro128PlusPlus) {
        int64_t l = math_hashCode_int32x3(x, y, z);
        state->seedLo ^= l;
        if ((state->seedLo | state->seedHi) == 0ULL) {
            state->seedLo = 0x9e3779b97f4a7c15ULL;
            state->seedHi = 0x6a09e667f3bcc909ULL;
        }
    }
}

__device__ static int32_t random_state_Checked_next(random_state_t *state, int32_t bits) {
    int64_t m = (state->seedLo * 25214903917LL + 11LL) & 281474976710655LL;
    state->seedLo = m;
    return (int32_t) (m >> (48 - bits));
}

__device__ static int64_t random_state_Xoroshiro128PlusPlus_next0(random_state_t *state) {
    int64_t l = state->seedLo;
    int64_t m = state->seedHi;
    int64_t n = math_rotateLeftU64((uint64_t) (l + m), 17) + l;
    m ^= l;
    state->seedLo = math_rotateLeftU64((uint64_t) l, 49) ^ m ^ (m << 21);
    state->seedHi = math_rotateLeftU64((uint64_t) m, 28);
    return n;
}

__device__ static float random_state_nextFloat(random_state_t *state) {
    if (state->type == RANDOM_Checked) {
        return (float) random_state_Checked_next(state, 24) * 5.9604645E-8F;
    } else {
        return (float) (((uint64_t) random_state_Xoroshiro128PlusPlus_next0(state)) >> 40) * 5.9604645E-8F;
    }
}

__device__ static int32_t random_state_nextIntBounded(random_state_t *state, int32_t bound) {
    if (bound <= 0) return 0;
    if (state->type == RANDOM_Checked) {
        if ((bound & (bound - 1)) == 0) {
            return (int32_t)((int64_t)bound * (int64_t)random_state_Checked_next(state, 31) >> 31);
        } else {
            int32_t i, j;
            do {
                i = random_state_Checked_next(state, 31);
                j = i % bound;
            } while (i - j + (bound - 1) < 0);
            return j;
        }
    } else {
        uint32_t l = (uint32_t) random_state_Xoroshiro128PlusPlus_next0(state);
        uint64_t m = (uint64_t) l * (uint64_t) bound;
        uint64_t n = m & 0xFFFFFFFFULL;
        if (n < (uint64_t) bound) {
            for (uint32_t i = (~((uint32_t) bound) + 1) % ((uint32_t) bound); n < (uint64_t) i; n = m & 0xFFFFFFFFULL) {
                l = (uint32_t) random_state_Xoroshiro128PlusPlus_next0(state);
                m = (uint64_t) l * (uint64_t) bound;
            }
        }
        return (int32_t) (m >> 32);
    }
}

static const int32_t BLOCK_NULL = 0;
static const int32_t BLOCK_AIR = 1;
static const int32_t BLOCK_DEFAULT_BLOCK = 2;
static const int32_t BLOCK_WATER = 3;
static const int32_t BLOCK_LAVA = 4;
static const int32_t BLOCK_COPPER_ORE = 5;
static const int32_t BLOCK_RAW_COPPER_BLOCK = 6;
static const int32_t BLOCK_GRANITE = 7;
static const int32_t BLOCK_DEEPSLATE_IRON_ORE = 8;
static const int32_t BLOCK_RAW_IRON_BLOCK = 9;
static const int32_t BLOCK_TUFF = 10;

typedef struct aquifer_fluidlevel {
    int32_t y;
    int32_t blockState;
} aquifer_fluidlevel_t;

__device__ __forceinline__ static int32_t aquifer_fluidlevel_getBlockState(const aquifer_fluidlevel_t *data, const int32_t y) {
    return y < data->y ? data->blockState : BLOCK_AIR;
}

static const int32_t __aquifer_chunkPosOffset[13][2] = {
    {0, 0}, {-2, -1}, {-1, -1}, {0, -1}, {1, -1}, {-3, 0}, {-2, 0}, {-1, 0}, {1, 0}, {-2, 1}, {-1, 1}, {0, 1}, {1, 1}
};

__device__ static const aquifer_fluidlevel_t *fluidLevelSampler_getFluidLevel_ptr(const void *rw_data, const int32_t y) {
    const worldgen_params_t *params = (const worldgen_params_t *) rw_data;
    const aquifer_fluidlevel_t *fluidLevels = (const aquifer_fluidlevel_t *) ptr_shift_const(rw_data, params->offset_fluidLevelSampler);
    const int32_t relY = y - genShapeCfg_minimumY;
    int32_t clamped = relY < 0 ? 0 : (relY >= genShapeCfg_height ? genShapeCfg_height - 1 : relY);
    return &fluidLevels[clamped];
}

__device__ static bool math_VanillaBiomeParameters_inDeepDarkParameters(const sample_int32_ctx_t ctx) {
    return df_binding_erosion(ctx) < -0.225F && df_binding_depth(ctx) > 0.9F;
}

__device__ static int32_t __aquifer_getNoiseBasedFluidLevel(const void *const_data, int32_t blockX, int32_t blockY, int32_t blockZ, int32_t surfaceHeightEstimate) {
    int32_t k = blockX >> 4;
    int32_t l = math_floorDiv(blockY, 40);
    int32_t m = blockZ >> 4;
    int32_t n = l * 40 + 20;
    double d = df_binding_fluid_level_spread(make_sample_int32_ctx(const_data, NULL, k, l, m, 0)) * 10.0;
    int32_t p = (int32_t) math_roundDownToMultiple(d, 3);
    int32_t q = n + p;
    return surfaceHeightEstimate < q ? surfaceHeightEstimate : q;
}

static const int32_t DimensionType_field_35479 = -32512;

__device__ static int32_t __aquifer_getFluidBlockY(const void *const_data, int32_t blockX, int32_t blockY, int32_t blockZ, const aquifer_fluidlevel_t *defaultFluidLevel, int32_t surfaceHeightEstimate, bool bl) {
    const sample_int32_ctx_t unblendedNoisePos = make_sample_int32_ctx(const_data, NULL, blockX, blockY, blockZ, 0);
    double d, e;
    if (math_VanillaBiomeParameters_inDeepDarkParameters(unblendedNoisePos)) {
        d = -1.0;
        e = -1.0;
    } else {
        int i = surfaceHeightEstimate + 8 - blockY;
        double f = bl ? math_clampedLerp(1.0, 0.0, ((double) i) / 64.0) : 0.0;
        double g = df_binding_fluid_level_floodedness(unblendedNoisePos);
        g = g < -1.0 ? -1.0 : (g > 1.0 ? 1.0 : g);
        d = g + 0.8 + (f - 1.0) * 1.2;
        e = g + 0.3 + (f - 1.0) * 1.1;
    }

    if (e > 0.0) {
        return defaultFluidLevel->y;
    } else if (d > 0.0) {
        return __aquifer_getNoiseBasedFluidLevel(const_data, blockX, blockY, blockZ, surfaceHeightEstimate);
    } else {
        return DimensionType_field_35479;
    }
}

__device__ static int32_t __aquifer_getFluidBlockState(const void *const_data, int blockX, int blockY, int blockZ, const aquifer_fluidlevel_t *defaultFluidLevel, int fluidLevel) {
    int32_t blockState = defaultFluidLevel->blockState;
    if (fluidLevel <= -10 && fluidLevel != DimensionType_field_35479 && defaultFluidLevel->blockState != BLOCK_LAVA) {
        int k = blockX >> 6;
        int l = math_floorDiv(blockY, 40);
        int m = blockZ >> 6;
        double d = df_binding_lava(make_sample_int32_ctx(const_data, NULL, k, l, m, 0));
        if (fabs(d) > 0.3) {
            blockState = BLOCK_LAVA;
        }
    }
    return blockState;
}

typedef struct aquifer_data {
    int32_t startX;
    int32_t startY;
    int32_t startZ;
    int32_t sizeX;
    int32_t sizeY;
    int32_t sizeZ;
    int32_t randomDeriver;
    int32_t waterLevels;
    int32_t packedBlockPositions;
    int32_t globalFluidLevelStatus;
} aquifer_data_t;

typedef struct aquifer_result {
    int32_t blockState;
    bool needsFluidTick;
} aquifer_result_t;

__device__ static uint32_t aquifer_data_index(const aquifer_data_t *data, int32_t x, int32_t y, int32_t z) {
    return (uint32_t) ((x * data->sizeZ + z) * data->sizeY + y);
}

#ifdef DF_COMPILE_AQUIFER_PREFILL
extern "C" __global__ void aquifer_data_prefill(const void * __restrict__ const_data, void * __restrict__ rw_data,
                                                const int32_t countX, const int32_t countZ, const int32_t countY) {
    int32_t relX = blockIdx.x * blockDim.x + threadIdx.x;
    int32_t relZ = blockIdx.y * blockDim.y + threadIdx.y;
    int32_t relY = blockIdx.z * blockDim.z + threadIdx.z;
    if (relX >= countX || relZ >= countZ || relY >= countY) return;

    const worldgen_params_t *params = (const worldgen_params_t *) rw_data;
    if (!params->offset_aquifer) return;

    const aquifer_data_t *data = (const aquifer_data_t *) ptr_shift_const(rw_data, params->offset_aquifer);
    const random_state_t *randomDeriver = (const random_state_t *) ptr_shift_const(data, data->randomDeriver);
    aquifer_fluidlevel_t *waterLevels = (aquifer_fluidlevel_t *) ptr_shift(rw_data, params->offset_aquifer + data->waterLevels);
    uint16_t *packedBlockPositions = (uint16_t *) ptr_shift(rw_data, params->offset_aquifer + data->packedBlockPositions);

    const int32_t curX = data->startX + relX;
    const int32_t curY = data->startY + relY;
    const int32_t curZ = data->startZ + relZ;

    const int32_t blockX = curX << 4;
    const int32_t blockY = curY * 12;
    const int32_t blockZ = curZ << 4;

    random_state_t random = *randomDeriver;
    random_state_split_coords(&random, curX, curY, curZ);

    const aquifer_fluidlevel_t *defaultFluidLevel = fluidLevelSampler_getFluidLevel_ptr(rw_data, blockY);

    int32_t surfaceHeightEstimate = (int32_t) chunkNoiseSampler_estimateSurfaceHeight(make_sample_int32_ctx(const_data, rw_data, blockX, blockY, blockZ, 0));

    aquifer_fluidlevel_t fluidLevel;
    if (blockY <= surfaceHeightEstimate + 8) {
        int32_t fluidBlockY = __aquifer_getFluidBlockY(const_data, blockX, blockY, blockZ, defaultFluidLevel, surfaceHeightEstimate, false);
        fluidLevel.y = fluidBlockY;
        fluidLevel.blockState = __aquifer_getFluidBlockState(const_data, blockX, blockY, blockZ, defaultFluidLevel, fluidBlockY);
    } else {
        fluidLevel = *defaultFluidLevel;
    }

    uint32_t index = aquifer_data_index(data, relX, relY, relZ);
    waterLevels[index] = fluidLevel;

    int32_t posX = random_state_nextIntBounded(&random, 10);
    int32_t posY = random_state_nextIntBounded(&random, 9);
    int32_t posZ = random_state_nextIntBounded(&random, 10);
    packedBlockPositions[index] = (uint16_t) ((posX & 0xF) | ((posY & 0xF) << 4) | ((posZ & 0xF) << 8));
}
#endif

__device__ static aquifer_result_t aquifer_sample(const sample_int32_ctx_t ctx, const double finalDensity) {
    aquifer_result_t res;
    res.blockState = BLOCK_NULL;
    res.needsFluidTick = false;
    if (finalDensity > 0.0) return res;

    const worldgen_params_t *params = (const worldgen_params_t *) ctx.rw_data;
    if (!params->offset_aquifer) {
        const aquifer_fluidlevel_t *fl = fluidLevelSampler_getFluidLevel_ptr(ctx.rw_data, ctx.y);
        res.blockState = aquifer_fluidlevel_getBlockState(fl, ctx.y);
        return res;
    }

    const aquifer_data_t *data = (const aquifer_data_t *) ptr_shift_const(ctx.rw_data, params->offset_aquifer);
    const aquifer_fluidlevel_t *waterLevels = (const aquifer_fluidlevel_t *) ptr_shift_const(data, data->waterLevels);
    const uint16_t *packedBlockPositions = (const uint16_t *) ptr_shift_const(data, data->packedBlockPositions);

    int32_t cellX = (ctx.x >> 4) - data->startX;
    int32_t cellY = math_floorDiv(ctx.y, 12) - data->startY;
    int32_t cellZ = (ctx.z >> 4) - data->startZ;

    if (cellX < 0 || cellZ < 0 || cellY < 0 || cellX >= data->sizeX || cellZ >= data->sizeZ || cellY >= data->sizeY) {
        const aquifer_fluidlevel_t *fl = fluidLevelSampler_getFluidLevel_ptr(ctx.rw_data, ctx.y);
        res.blockState = aquifer_fluidlevel_getBlockState(fl, ctx.y);
        return res;
    }

    uint32_t idx = aquifer_data_index(data, cellX, cellY, cellZ);
    res.blockState = aquifer_fluidlevel_getBlockState(&waterLevels[idx], ctx.y);
    return res;
}

__device__ static int32_t ore_vein_sample(const sample_int32_ctx_t ctx) {
    const worldgen_params_t *params = (const worldgen_params_t *) ctx.rw_data;
    if (!params->offset_oreVeinRandom) return BLOCK_NULL;

    double veinToggle = df_binding_vein_toggle(ctx);
    if (veinToggle <= 0.0) return BLOCK_NULL;

    double veinRidged = df_binding_vein_ridged(ctx);
    double veinGap = df_binding_vein_gap(ctx);
    double d = fabs(veinRidged) - 0.08;
    if (d >= 0.0 || veinGap <= -0.3) return BLOCK_NULL;

    return veinToggle > 0.4 ? BLOCK_COPPER_ORE : BLOCK_RAW_IRON_BLOCK;
}

#ifdef DF_COMPILE_NOISE_KERNEL
extern "C" __global__ void df_noise_kernel(
        const void * __restrict__ const_data, void * __restrict__ rw_data,
        uint8_t * __restrict__ res_blocks,
        const int32_t chunkX, const int32_t chunkZ,
        const int32_t sizeX, const int32_t sizeZ, const int32_t sizeY) {
    const int32_t relX = blockIdx.x * blockDim.x + threadIdx.x;
    const int32_t relZ = blockIdx.y * blockDim.y + threadIdx.y;
    const int32_t relY = blockIdx.z * blockDim.z + threadIdx.z;

    if (relX >= sizeX || relZ >= sizeZ || relY >= sizeY) return;

    const worldgen_params_t *params = (const worldgen_params_t *) rw_data;
    const int32_t blockX = (chunkX << 4) + relX;
    const int32_t blockY = genShapeCfg_minimumY + relY;
    const int32_t blockZ = (chunkZ << 4) + relZ;

    sample_int32_ctx_t ctx = make_sample_int32_ctx(const_data, rw_data, blockX, blockY, blockZ, MASK_enableAllCaches);

    int32_t blockState = BLOCK_NULL;
    aquifer_result_t aquifer_res = aquifer_sample(ctx, df_binding_final_final_density(ctx));
    blockState = aquifer_res.blockState;
    if (blockState == BLOCK_NULL) {
        blockState = ore_vein_sample(ctx);
    }
    if (blockState == BLOCK_NULL) {
        blockState = params->genConfig_defaultBlock;
    }
    uint32_t idx = ((relY) * sizeX + relZ) * sizeZ + relX;
    res_blocks[idx] = ((uint8_t) blockState) | (aquifer_res.needsFluidTick ? (1U << 7) : 0);
}
#endif

typedef struct biome_search_tree_node {
    uint32_t state;
    union {
        struct {
            uint32_t children_offset[7];
        } branch_children;
        struct {
            int16_t maxs[7];
            int16_t mins[7];
        } node_minmaxs;
    };
} biome_search_tree_node_t;

__device__ __forceinline__ static bool __math_biome_search_tree_is_branch(const biome_search_tree_node_t * const node) {
    return (node->state & (1U << 31)) != 0;
}

__device__ __forceinline__ static uint64_t __math_biome_search_tree_distance_func(const biome_search_tree_node_t * const node, const int16_t * const target) {
    uint64_t res = 0;
    for (uint32_t i = 0; i < 7; i++) {
        int64_t l = (int32_t) target[i] - (int32_t) node->node_minmaxs.maxs[i];
        int64_t m = (int32_t) node->node_minmaxs.mins[i] - (int32_t) target[i];
        int64_t dist = l >= 0LL ? l : (m > 0LL ? m : 0LL);
        res += dist * dist;
    }
    return res;
}

typedef struct __biome_search_stack_element {
    uint32_t node;
    uint8_t iter_i;
} __biome_search_stack_element_t;

#ifndef BIOME_SEARCH_TREE_MAX_DEPTH
#define BIOME_SEARCH_TREE_MAX_DEPTH 16
#endif

__device__ static uint32_t math_biome_search_tree_calc(const biome_search_tree_node_t * const nodes, const int16_t * const target, const uint32_t nodes_c) {
    if (!__math_biome_search_tree_is_branch(nodes + 1)) {
        return nodes[1].state & 0x3FFFFFFF;
    }

    __biome_search_stack_element_t working[BIOME_SEARCH_TREE_MAX_DEPTH];
    uint32_t top = 0;
    uint32_t current_optimal_node = 1;
    uint64_t current_optimal_dist = UINT64_MAX;

    working[top++] = { 1, 0 };

    while (top > 0) {
        uint32_t cur_node = working[top - 1].node;
        uint32_t iter_i = working[top - 1].iter_i;

        uint32_t child_node = 0;
        if (iter_i < 7) {
            child_node = nodes[cur_node + 1].branch_children.children_offset[iter_i];
        }

        if (iter_i >= 7 || child_node == 0) {
            top--;
            continue;
        }

        working[top - 1].iter_i++;

        uint64_t d = __math_biome_search_tree_distance_func(nodes + child_node, target);
        if (d >= current_optimal_dist) {
            continue;
        }

        if (__math_biome_search_tree_is_branch(nodes + child_node)) {
            if (top < BIOME_SEARCH_TREE_MAX_DEPTH) {
                working[top++] = { child_node, 0 };
            }
        } else {
            current_optimal_dist = d;
            current_optimal_node = child_node;
        }
    }

    return nodes[current_optimal_node].state & 0x3FFFFFFF;
}

extern __constant__ const uint32_t biome_multinoise_tree_offset;
extern __constant__ const uint32_t biome_multinoise_tree_nodes_c;

__device__ __forceinline__ static int16_t clamp_to_short(int32_t v) {
    return v < -32768 ? -32768 : (v > 32767 ? 32767 : (int16_t) v);
}

#ifdef DF_COMPILE_BIOME_MULTINOISE_KERNEL
extern "C" __global__ void df_biome_multinoise_kernel(
        const void * __restrict__ const_data, void * __restrict__ rw_data,
        uint32_t * __restrict__ const res_biomes,
        const int32_t startBiomeX, const int32_t startBiomeZ, const int32_t startBiomeY,
        const uint32_t sizeX, const uint32_t sizeZ, const uint32_t sizeY) {
    const uint32_t relX = blockIdx.x * blockDim.x + threadIdx.x;
    const uint32_t relZ = blockIdx.y * blockDim.y + threadIdx.y;
    const uint32_t relY = blockIdx.z * blockDim.z + threadIdx.z;

    if (relX >= sizeX || relZ >= sizeZ || relY >= sizeY) return;

    const uint32_t blockX = math_biome2block(startBiomeX + relX);
    const uint32_t blockY = math_biome2block(startBiomeY + relY);
    const uint32_t blockZ = math_biome2block(startBiomeZ + relZ);

    sample_int32_ctx_t ctx = make_sample_int32_ctx(const_data, rw_data, blockX, blockY, blockZ, MASK_enableFlatCache);

    const double temperature = df_binding_temperature(ctx);
    const double vegetation = df_binding_vegetation(ctx);
    const double continents = df_binding_continents(ctx);
    const double erosion = df_binding_erosion(ctx);
    const double depth = df_binding_depth(ctx);
    const double ridges = df_binding_ridges(ctx);

    const int16_t target[7] = {
        clamp_to_short((int32_t) (((float) temperature) * 10000.0F)),
        clamp_to_short((int32_t) (((float) vegetation) * 10000.0F)),
        clamp_to_short((int32_t) (((float) continents) * 10000.0F)),
        clamp_to_short((int32_t) (((float) erosion) * 10000.0F)),
        clamp_to_short((int32_t) (((float) depth) * 10000.0F)),
        clamp_to_short((int32_t) (((float) ridges) * 10000.0F)),
        0,
    };

    const biome_search_tree_node_t *root_node = (const biome_search_tree_node_t *) ptr_shift_const(const_data, biome_multinoise_tree_offset);
    const uint32_t result_biome = math_biome_search_tree_calc(root_node, target, biome_multinoise_tree_nodes_c);

    uint32_t idx = ((relY) * sizeX + relZ) * sizeZ + relX;
    res_biomes[idx] = result_biome;
}
#endif
