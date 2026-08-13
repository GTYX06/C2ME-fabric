/*
 * All Rights Reserved
 *
 * Copyright (c) 2025-2026 ishland
 *
 * All rights reserved. Do not redistribute.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

#version 450
#extension GL_EXT_shader_explicit_arithmetic_types_int8 : enable
#extension GL_EXT_shader_explicit_arithmetic_types_int16 : enable
#extension GL_EXT_shader_explicit_arithmetic_types_int32 : enable
#extension GL_EXT_shader_explicit_arithmetic_types_int64 : enable
#extension GL_EXT_shader_explicit_arithmetic_types_float64 : enable
#extension GL_ARB_gpu_shader_int64 : enable
#extension GL_ARB_gpu_shader_fp64 : enable
#extension GL_EXT_scalar_block_layout : enable
#extension GL_EXT_buffer_reference : enable
#extension GL_EXT_buffer_reference2 : enable

#define int8_t int8_t
#define uint8_t uint8_t
#define int16_t int16_t
#define uint16_t uint16_t
#define int32_t int
#define uint32_t uint
#define int64_t int64_t
#define uint64_t uint64_t

#define UINT32_MAX 0xffffffffu
#define INT32_MAX 0x7fffffff
#define UINT64_MAX (~0u64)
#define INT64_MAX 0x7FFFFFFFFFFFFFFFL

#ifndef NULL
#define NULL 0u64
#endif

// Scalar Buffer References for fast 64-bit pointer dereferencing
layout(buffer_reference, scalar) readonly buffer ConstByteRef { uint8_t val; };
layout(buffer_reference, scalar) readonly buffer ConstInt16Ref { int16_t val; };
layout(buffer_reference, scalar) readonly buffer ConstUint16Ref { uint16_t val; };
layout(buffer_reference, scalar) readonly buffer ConstInt32Ref { int32_t val; };
layout(buffer_reference, scalar) readonly buffer ConstUint32Ref { uint32_t val; };
layout(buffer_reference, scalar) readonly buffer ConstInt64Ref { int64_t val; };
layout(buffer_reference, scalar) readonly buffer ConstUint64Ref { uint64_t val; };
layout(buffer_reference, scalar) readonly buffer ConstFloatRef { float val; };
layout(buffer_reference, scalar) readonly buffer ConstDoubleRef { double val; };

layout(buffer_reference, scalar) buffer RWInt32Ref { int32_t val; };
layout(buffer_reference, scalar) buffer RWUint32Ref { uint32_t val; };
layout(buffer_reference, scalar) buffer RWFloatRef { float val; };
layout(buffer_reference, scalar) buffer RWDoubleRef { double val; };
layout(buffer_reference, scalar) buffer RWUint64Ref { uint64_t val; };

#define kernel
#define convert_short_sat(x) int16_t(clamp(int64_t(x), -32768L, 32767L))
#define fabs(x) abs(x)
#define fmod(x, y) mod(x, y)
#define fmax(x, y) max(x, y)
#define fmin(x, y) min(x, y)
#define DBL_MAX 1.7976931348623157e+308
#define FLT_MAX 3.402823466e+38F
#define nan(x) (0.0 / 0.0)
#define __builtin_trap()
#define __builtin_unreachable()

const double FLAT_SIMPLEX_GRAD[16][3] = {
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

const double SQRT_3 = 1.7320508075688772;
// 0.5 * (SQRT_3 - 1.0)
const double SKEW_FACTOR_2D = 0.3660254037844386;
// (3.0 - SQRT_3) / 6.0
const double UNSKEW_FACTOR_2D = 0.21132486540518713;

// Used in intel fast compile
#ifdef BLOAT_APPARENT_FUNCTION_SIZES
void nop() {
}
#else
#define nop()
#endif

uint64_t ptr_shift(uint64_t ptr, const int32_t shift) {
    return ptr + uint64_t(shift);
}

uint64_t ptr_shift_const(uint64_t ptr, const int32_t shift) {
    return ptr + uint64_t(shift);
}

uint64_t ptr_shift_global(uint64_t ptr, const int32_t shift) {
    return ptr + uint64_t(shift);
}

double math_floor(const double v) {
    return floor(v);
}

uint64_t math_rotateLeftU64(uint64_t i, uint64_t distance) {
    return (i << distance) | (i >> -distance);
}

int32_t math_floorDiv(const int32_t x, const int32_t y) {
    int r = x / y;
    // if the signs are different and modulo not zero, round down
    if ((x ^ y) < 0 && (r * y != x)) {
        r--;
    }
    return r;
}

double math_octave_maintainPrecision(const double value) {
    return value - math_floor(value / 3.3554432E7 + 0.5) * 3.3554432E7;
}

double math_simplex_grad(const int32_t hash, const double x, const double y,
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

double math_lerp(const double delta, const double start, const double end) {
    return start + delta * (end - start);
}

float math_lerpf(const float delta, const float start, const float end) {
    return start + delta * (end - start);
}

double math_clampedLerp(const double start, const double end, const double delta) {
    if (delta < 0.0) {
        return start;
    } else {
        return delta > 1.0 ? end : math_lerp(delta, start, end);
    }
}

double math_square(const double operand) {
    return operand * operand;
}

double math_lerp2(const double deltaX, const double deltaY, const double x0y0,
                                                       const double x1y0, const double x0y1, const double x1y1) {
    return math_lerp(deltaY, math_lerp(deltaX, x0y0, x1y0), math_lerp(deltaX, x0y1, x1y1));
}

double math_lerp3(
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

double math_getLerpProgress(const double value, const double start,
                                                                 const double end) {
    return (value - start) / (end - start);
}

double
math_clampedLerpFromProgress(const double lerpValue, const double lerpStart, const double lerpEnd, const double start,
                             const double end) {
    return math_clampedLerp(start, end, math_getLerpProgress(lerpValue, lerpStart, lerpEnd));
}

int32_t math_floorMod(const int32_t x, const int32_t y) {
    int32_t mod = x % y;
    // if the signs are different and modulo not zero, adjust result
    if ((mod ^ y) < 0 && mod != 0) {
        mod += y;
    }
    return mod;
}

int32_t math_biome2block(const int32_t biomeCoord) {
    return biomeCoord << 2;
}

int32_t math_block2biome(const int32_t blockCoord) {
    return blockCoord >> 2;
}

int32_t
math_simplex_map_global_impl(uint64_t permutations, const int32_t in_val) {
    return int32_t(ConstUint32Ref(permutations + uint64_t((in_val & 0xFF) * 4)).val);
}

double math_simplex_dot(const int32_t hash, const double x, const double y,
                                                     const double z) {
    return FLAT_SIMPLEX_GRAD[hash][0] * x + FLAT_SIMPLEX_GRAD[hash][1] * y + FLAT_SIMPLEX_GRAD[hash][2] * z;
}

double math_simplex_grad_impl(const int32_t hash, const double x, const double y,
                                                         const double z, const double distance) {
    double d = distance - x * x - y * y - z * z;
    double e;
    if (d < 0.0) {
        e = 0.0;
    } else {
        d *= d;
        e = d * d * math_simplex_dot(hash, x, y, z);
    }
    return e;
    // double tmp = d * d; // speculative execution

    // return d < 0.0 ? 0.0 : tmp * tmp * math_simplex_dot(hash, x, y, z);
}

double 
math_noise_simplex_sample2d_global(uint64_t permutations, const double x, const double y) {
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

    const double n = h - double(l) + UNSKEW_FACTOR_2D;
    const double o = k - double(m) + UNSKEW_FACTOR_2D;
    const double p = h - 1.0 + 2.0 * UNSKEW_FACTOR_2D;
    const double q = k - 1.0 + 2.0 * UNSKEW_FACTOR_2D;
    const int32_t r = int32_t(i) & 0xFF;
    const int32_t s = int32_t(j) & 0xFF;
    const int32_t t = math_simplex_map_global_impl(permutations, r + math_simplex_map_global_impl(permutations, s)) % 12;
    const int32_t u = math_simplex_map_global_impl(permutations, r + li + math_simplex_map_global_impl(permutations, s + mi)) % 12;
    const int32_t v = math_simplex_map_global_impl(permutations, r + 1 + math_simplex_map_global_impl(permutations, s + 1)) % 12;
    const double w = math_simplex_grad_impl(t, h, k, 0.0, 0.5);
    const double z = math_simplex_grad_impl(u, n, o, 0.0, 0.5);
    const double aa = math_simplex_grad_impl(v, p, q, 0.0, 0.5);
    return 70.0 * (w + z + aa);
}

double math_perlinFade(const double value) {
    return value * value * value * (value * (value * 6.0 - 15.0) + 10.0);
}

// noinline to prevent broken optimizations on intel drivers
double math_perlin_grad_global_impl(uint64_t permutations, const int32_t px,
                                                                     const int32_t py, const int32_t pz, const double fx,
                                                                     const double fy, const double fz) {
    const uint32_t map0 = uint32_t(ConstByteRef(permutations + uint64_t(px & 0xFF)).val) + uint32_t(py);
    const uint32_t map1 = uint32_t(ConstByteRef(permutations + uint64_t(map0 & 0xFF)).val) + uint32_t(pz);
    const uint32_t hash = uint32_t(ConstByteRef(permutations + uint64_t(map1 & 0xFF)).val) & 0xFU;
    return FLAT_SIMPLEX_GRAD[hash][0] * fx + FLAT_SIMPLEX_GRAD[hash][1] * fy + FLAT_SIMPLEX_GRAD[hash][2] * fz;
}

double
math_noise_perlin_sampleScalar_global(uint64_t permutations,
                                      const int32_t px0, const int32_t py0, const int32_t pz0,
                                      const double fx0, const double fy0, const double fz0, const double fadeLocalY) {
    const int32_t px1 = px0 + 1;
    const int32_t py1 = py0 + 1;
    const int32_t pz1 = pz0 + 1;
    const double fx1 = fx0 - 1;
    const double fy1 = fy0 - 1;
    const double fz1 = fz0 - 1;

    double f000 = math_perlin_grad_global_impl(permutations, px0, py0, pz0, fx0, fy0, fz0);
    double f100 = math_perlin_grad_global_impl(permutations, px1, py0, pz0, fx1, fy0, fz0);
    double f010 = math_perlin_grad_global_impl(permutations, px0, py1, pz0, fx0, fy1, fz0);
    double f110 = math_perlin_grad_global_impl(permutations, px1, py1, pz0, fx1, fy1, fz0);
    double f001 = math_perlin_grad_global_impl(permutations, px0, py0, pz1, fx0, fy0, fz1);
    double f101 = math_perlin_grad_global_impl(permutations, px1, py0, pz1, fx1, fy0, fz1);
    double f011 = math_perlin_grad_global_impl(permutations, px0, py1, pz1, fx0, fy1, fz1);
    double f111 = math_perlin_grad_global_impl(permutations, px1, py1, pz1, fx1, fy1, fz1);

    const double dx = math_perlinFade(fx0);
    const double dy = math_perlinFade(fadeLocalY);
    const double dz = math_perlinFade(fz0);
    return math_lerp3(dx, dy, dz, f000, f100, f010, f110, f001, f101, f011, f111);
}

double
math_noise_perlin_sample_global(uint64_t permutations,
                                const double originX, const double originY, const double originZ,
                                const double x, const double y, const double z,
                                const double yScale, const double yMax) {
    const double d = x + originX;
    const double e = y + originY;
    const double f = z + originZ;
    const double i = math_floor(d);
    const double j = math_floor(e);
    const double k = math_floor(f);
    const double g = d - i;
    const double h = e - j;
    const double l = f - k;
    const double o = yScale != 0 ? math_floor(((yMax >= 0.0 && yMax < h) ? yMax : h) / yScale + 1.0E-7) * yScale : 0;

    return math_noise_perlin_sampleScalar_global(permutations, int32_t(i), int32_t(j), int32_t(k), g, h - o, l, h);
}


layout(buffer_reference, scalar) readonly buffer double_octave_sampler_data_t {
uint64_t len;
    double amplitude;
    int32_t need_shift;
    int32_t lacunarity_powd;
    int32_t persistence_powd;
    int32_t sampler_permutations;
    int32_t sampler_originX;
    int32_t sampler_originY;
    int32_t sampler_originZ;
    int32_t amplitudes;
};

double
math_noise_perlin_double_octave_sample_impl_global(double_octave_sampler_data_t data,
                                                   const double x, const double y, const double z,
                                                   const double yScale, const double yMax, const uint8_t useOrigin) {
    double d1 = 0.0;
    double d2 = 0.0;

    uint64_t need_shift = ptr_shift_global(uint64_t(data), data.need_shift);
    uint64_t lacunarity_powd = ptr_shift_global(uint64_t(data), data.lacunarity_powd);
    uint64_t persistence_powd = ptr_shift_global(uint64_t(data), data.persistence_powd);
    uint64_t sampler_permutations = ptr_shift_global(uint64_t(data), data.sampler_permutations);
    uint64_t sampler_originX = ptr_shift_global(uint64_t(data), data.sampler_originX);
    uint64_t sampler_originY = ptr_shift_global(uint64_t(data), data.sampler_originY);
    uint64_t sampler_originZ = ptr_shift_global(uint64_t(data), data.sampler_originZ);
    uint64_t amplitudes = ptr_shift_global(uint64_t(data), data.amplitudes);

    for (uint32_t i = 0; i < data.len; i++) {
        const double e = ConstDoubleRef(lacunarity_powd + uint64_t(i * 8)).val;
        const double f = ConstDoubleRef(persistence_powd + uint64_t(i * 8)).val;
        uint64_t permutations = sampler_permutations + 256 * i;
        const double sampleX = (ConstByteRef(need_shift + uint64_t(i)).val != 0) ? x * 1.0181268882175227 : x;
        const double sampleY = (ConstByteRef(need_shift + uint64_t(i)).val != 0) ? y * 1.0181268882175227 : y;
        const double sampleZ = (ConstByteRef(need_shift + uint64_t(i)).val != 0) ? z * 1.0181268882175227 : z;
        const double g = math_noise_perlin_sample_global(
                permutations,
                ConstDoubleRef(sampler_originX + uint64_t(i * 8)).val,
                ConstDoubleRef(sampler_originY + uint64_t(i * 8)).val,
                ConstDoubleRef(sampler_originZ + uint64_t(i * 8)).val,
                math_octave_maintainPrecision(sampleX * e),
                useOrigin ? -(ConstDoubleRef(sampler_originY + uint64_t(i * 8)).val) : math_octave_maintainPrecision(sampleY * e),
                math_octave_maintainPrecision(sampleZ * e),
                yScale * e,
                yMax * e);
        const double d = ConstDoubleRef(amplitudes + uint64_t(i * 8)).val * g * f;
        if (!(ConstByteRef(need_shift + uint64_t(i)).val != 0)) {
            d1 += d;
        } else {
            d2 += d;
        }
    }

    return (d1 + d2) * data.amplitude;
}

double
math_noise_perlin_double_octave_sample_global_noinline(double_octave_sampler_data_t data,
                                                       const double x, const double y, const double z) {
    return math_noise_perlin_double_octave_sample_impl_global(data, x, y, z, 0.0, 0.0, 0);
}

double
math_noise_perlin_double_octave_sample_global(double_octave_sampler_data_t data,
                                              const double x, const double y, const double z) {
    return math_noise_perlin_double_octave_sample_impl_global(data, x, y, z, 0.0, 0.0, 0);
}

layout(buffer_reference, scalar) readonly buffer interpolated_noise_sub_sampler_t {
uint32_t len;
    int32_t sampler_permutations;
    int32_t sampler_originX;
    int32_t sampler_originY;
    int32_t sampler_originZ;
    int32_t sampler_mulFactor;
};

layout(buffer_reference, scalar) readonly buffer interpolated_noise_sampler_t {
double scaledXzScale;
    double scaledYScale;
    double xzFactor;
    double yFactor;
    double smearScaleMultiplier;
    double xzScale;
    double yScale;

    interpolated_noise_sub_sampler_t lower;
    interpolated_noise_sub_sampler_t upper;
    interpolated_noise_sub_sampler_t normal;
};

double
math_noise_perlin_interpolated_sample_global(interpolated_noise_sampler_t data,
                                             const double x, const double y, const double z) {
    const double d = x * data.scaledXzScale;
    const double e = y * data.scaledYScale;
    const double f = z * data.scaledXzScale;
    const double g = d / data.xzFactor;
    const double h = e / data.yFactor;
    const double i = f / data.xzFactor;
    const double j = data.scaledYScale * data.smearScaleMultiplier;
    const double k = j / data.yFactor;
    double l = 0.0;
    double m = 0.0;
    double n = 0.0;

    for (uint32_t offset = 0; offset < data.normal.len; offset++) {
        uint64_t sampler_permutations = ptr_shift_global(uint64_t(data), data.normal.sampler_permutations);
        uint64_t sampler_originX = ptr_shift_global(uint64_t(data), data.normal.sampler_originX);
        uint64_t sampler_originY = ptr_shift_global(uint64_t(data), data.normal.sampler_originY);
        uint64_t sampler_originZ = ptr_shift_global(uint64_t(data), data.normal.sampler_originZ);
        uint64_t sampler_mulFactor = ptr_shift_global(uint64_t(data), data.normal.sampler_mulFactor);
        double mf = ConstDoubleRef(sampler_mulFactor + uint64_t(offset * 8)).val;
        n += math_noise_perlin_sample_global(
                sampler_permutations + 256 * offset,
                ConstDoubleRef(sampler_originX + uint64_t(offset * 8)).val,
                ConstDoubleRef(sampler_originY + uint64_t(offset * 8)).val,
                ConstDoubleRef(sampler_originZ + uint64_t(offset * 8)).val,
                math_octave_maintainPrecision(g * mf),
                math_octave_maintainPrecision(h * mf),
                math_octave_maintainPrecision(i * mf),
                k * mf,
                h * mf
        ) / mf;
    }

    const double q = (n / 10.0 + 1.0) / 2.0;
    const uint8_t bl2 = q >= 1.0;
    const uint8_t bl3 = q <= 0.0;

    if (!bl2) {
        for (uint32_t offset = 0; offset < data.lower.len; offset++) {
            uint64_t sampler_permutations = ptr_shift_global(uint64_t(data), data.lower.sampler_permutations);
            uint64_t sampler_originX = ptr_shift_global(uint64_t(data), data.lower.sampler_originX);
            uint64_t sampler_originY = ptr_shift_global(uint64_t(data), data.lower.sampler_originY);
            uint64_t sampler_originZ = ptr_shift_global(uint64_t(data), data.lower.sampler_originZ);
            uint64_t sampler_mulFactor = ptr_shift_global(uint64_t(data), data.lower.sampler_mulFactor);
            double mf = ConstDoubleRef(sampler_mulFactor + uint64_t(offset * 8)).val;
            l += math_noise_perlin_sample_global(
                    sampler_permutations + 256 * offset,
                    ConstDoubleRef(sampler_originX + uint64_t(offset * 8)).val,
                    ConstDoubleRef(sampler_originY + uint64_t(offset * 8)).val,
                    ConstDoubleRef(sampler_originZ + uint64_t(offset * 8)).val,
                    math_octave_maintainPrecision(d * mf),
                    math_octave_maintainPrecision(e * mf),
                    math_octave_maintainPrecision(f * mf),
                    j * mf,
                    e * mf
            ) / mf;
        }
    }

    if (!bl3) {
        for (uint32_t offset = 0; offset < data.upper.len; offset++) {
            uint64_t sampler_permutations = ptr_shift_global(uint64_t(data), data.upper.sampler_permutations);
            uint64_t sampler_originX = ptr_shift_global(uint64_t(data), data.upper.sampler_originX);
            uint64_t sampler_originY = ptr_shift_global(uint64_t(data), data.upper.sampler_originY);
            uint64_t sampler_originZ = ptr_shift_global(uint64_t(data), data.upper.sampler_originZ);
            uint64_t sampler_mulFactor = ptr_shift_global(uint64_t(data), data.upper.sampler_mulFactor);
            double mf = ConstDoubleRef(sampler_mulFactor + uint64_t(offset * 8)).val;
            m += math_noise_perlin_sample_global(
                    sampler_permutations + 256 * offset,
                    ConstDoubleRef(sampler_originX + uint64_t(offset * 8)).val,
                    ConstDoubleRef(sampler_originY + uint64_t(offset * 8)).val,
                    ConstDoubleRef(sampler_originZ + uint64_t(offset * 8)).val,
                    math_octave_maintainPrecision(d * mf),
                    math_octave_maintainPrecision(e * mf),
                    math_octave_maintainPrecision(f * mf),
                    j * mf,
                    e * mf
            ) / mf;
        }
    }

    return math_clampedLerp(l / 512.0, m / 512.0, q) / 128.0;
}

double
math_noise_perlin_interpolated_sample_global_noinline(interpolated_noise_sampler_t data,
                                                       const double x, const double y, const double z) {
    return math_noise_perlin_interpolated_sample_global(data, x, y, z);
}

float
math_end_islands_sample_global(uint64_t simplex_permutations, const int32_t x, const int32_t z) {
    const int32_t i = x / 2;
    const int32_t j = z / 2;
    const int32_t k = x % 2;
    const int32_t l = z % 2;
    volatile int32_t muld = x * x + z * z; // int32_t intentionally
    if (muld & 0x80000000L) {
        return nan(uint32_t(0));
    }
    float f = 100.0F - sqrt(float(muld & 0x7fffffffL)) * 8.0F;
    f = clamp(f, -100.0F, 80.0F);

    int32_t ms[625]; int32_t ns[625]; bool hit[625];
    const int64_t omin = abs(i) - 12L;
    const int64_t pmin = abs(j) - 12L;
    const int64_t omax = abs(i) + 12L;
    const int64_t pmax = abs(j) + 12L;

    {
        uint32_t idx = 0;
        for (int32_t m = -12; m < 13; m++) {
            for (int32_t n = -12; n < 13; n++) {
                ms[idx] = m;
                ns[idx] = n;
                idx++;
            }
        }
        if (idx != 25 * 25) {
            #ifdef DEBUG
            printf("trap: idx != 25 * 25\n idx=%u\n", idx);
            #endif
            __builtin_trap();
            __builtin_unreachable();
            return nan(uint64_t(0));
        }
    }

    if (omin * omin + pmin * pmin > 4096L) {
        for (uint32_t idx = 0; idx < 25 * 25; idx++) {
            const int64_t o = int64_t(i) + int64_t(ms[idx]);
            const int64_t p = int64_t(j) + int64_t(ns[idx]);
            hit[idx] = math_noise_simplex_sample2d_global(simplex_permutations, double(o), double(p)) < -0.9F;
        }
    } else {
        for (uint32_t idx = 0; idx < 25 * 25; idx++) {
            const int64_t o = int64_t(i) + int64_t(ms[idx]);
            const int64_t p = int64_t(j) + int64_t(ns[idx]);
            hit[idx] = (o * o + p * p > 4096L) && math_noise_simplex_sample2d_global(
                    simplex_permutations, double(o), double(p)) < -0.9F;
        }
    }

    for (uint32_t idx = 0; idx < 25 * 25; idx++) {
        if (hit[idx]) {
            const int32_t m = ms[idx];
            const int32_t n = ns[idx];
            const int64_t o = int64_t(i) + int64_t(m);
            const int64_t p = int64_t(j) + int64_t(n);
            const float g1 = fabs(float(o)) * 3439.0F;
            const float g2 = fabs(float(p)) * 147.0F;
            const float g = fmod((g1 + g2), 13.0F) + 9.0F;
            const float h = float(k - m * 2);
            const float q = float(l - n * 2);
            float r = 100.0F - sqrt(h * h + q * q) * g;
            r = clamp(r, -100.0F, 80.0F);
            f = fmax(f, r);
        }
    }

    return f;
}
 
uint32_t
math_biome_access_sample(const int64_t theSeed, const int32_t x, const int32_t y, const int32_t z) {
    const int32_t var0 = x - 2;
    const int32_t var1 = y - 2;
    const int32_t var2 = z - 2;
    const int32_t var3 = var0 >> 2;
    const int32_t var4 = var1 >> 2;
    const int32_t var5 = var2 >> 2;
    const double var6 = double(var0 & 3) / 4.0;
    const double var7 = double(var1 & 3) / 4.0;
    const double var8 = double(var2 & 3) / 4.0;
    uint32_t var9 = 0;
    double var10 = DBL_MAX;

    double var28s[8];

    for (uint32_t var11 = 0; var11 < 8; ++var11) {
        uint32_t var12 = var11 & 4;
        uint32_t var13 = var11 & 2;
        uint32_t var14 = var11 & 1;
        int64_t var15 = var12 ? var3 + 1 : var3;
        int64_t var16 = var13 ? var4 + 1 : var4;
        int64_t var17 = var14 ? var5 + 1 : var5;
        double var18 = var12 ? var6 - 1.0 : var6;
        double var19 = var13 ? var7 - 1.0 : var7;
        double var20 = var14 ? var8 - 1.0 : var8;
        int64_t var21 = theSeed * (theSeed * 6364136223846793005L + 1442695040888963407L) + var15;
        var21 = var21 * (var21 * 6364136223846793005L + 1442695040888963407L) + var16;
        var21 = var21 * (var21 * 6364136223846793005L + 1442695040888963407L) + var17;
        var21 = var21 * (var21 * 6364136223846793005L + 1442695040888963407L) + var15;
        var21 = var21 * (var21 * 6364136223846793005L + 1442695040888963407L) + var16;
        var21 = var21 * (var21 * 6364136223846793005L + 1442695040888963407L) + var17;
        double var22 = double((var21 >> 24) & 1023) / 1024.0;
        double var23 = (var22 - 0.5) * 0.9;
        var21 = var21 * (var21 * 6364136223846793005L + 1442695040888963407L) + theSeed;
        double var24 = double((var21 >> 24) & 1023) / 1024.0;
        double var25 = (var24 - 0.5) * 0.9;
        var21 = var21 * (var21 * 6364136223846793005L + 1442695040888963407L) + theSeed;
        double var26 = double((var21 >> 24) & 1023) / 1024.0;
        double var27 = (var26 - 0.5) * 0.9;
        double var28 = math_square(var20 + var27) + math_square(var19 + var25) + math_square(var18 + var23);
        var28s[var11] = var28;
    }

    for (int i = 0; i < 8; ++i) {
        if (var10 > var28s[i]) {
            var9 = i;
            var10 = var28s[i];
        }
    }

    return var9;
}

layout(buffer_reference, scalar) readonly buffer aquifer_data_t {
int32_t startX;
    int32_t startY;
    int32_t startZ;
    int32_t sizeX;
    int32_t sizeY;
    int32_t sizeZ;
    int32_t samplingYLowPassCutoff;

    int32_t randomDeriver;
    int32_t posIdx_len;
    int32_t waterLevels; // aquifer_fluidlevel_t[posIdx]
    int32_t packedBlockPositions; // short[posIdx]
};

uint32_t
math_aquifer_index_global(uint64_t aquiferData, const int32_t x, const int32_t y,
                          const int32_t z) {
    int i = x - aquiferData.startX;
    int j = y - aquiferData.startY;
    int k = z - aquiferData.startZ;
    if (i < 0 || j < 0 || k < 0 || i >= aquiferData.sizeX || j >= aquiferData.sizeY || k >= aquiferData.sizeZ) {
        #ifdef DEBUG
        printf("trap: i < 0 || j < 0 || k < 0 || i >= aquiferData.sizeX || j >= aquiferData.sizeY || k >= aquiferData.sizeZ\n i=%d j=%d k=%d aquiferData.sizeX=%d aquiferData.sizeY=%d aquiferData.sizeZ=%d\n", 
            i, j, k, aquiferData.sizeX, aquiferData.sizeY, aquiferData.sizeZ);
        #endif
        __builtin_trap();
        __builtin_unreachable();
        return 0;
    }
    return (j * aquiferData.sizeZ + k) * aquiferData.sizeX + i;
}

int32_t
math_aquifer_unpackPackedX(uint32_t packed) {
    return (packed >> 8) & 0b1111;
}

int32_t
math_aquifer_unpackPackedY(uint32_t packed) {
    return (packed >> 4) & 0b1111;
}

int32_t
math_aquifer_unpackPackedZ(uint32_t packed) {
    return packed & 0b1111;
}

int32_t
math_aquifer_unpackPackedDist(uint64_t packed) {
    return int32_t(packed >> 36u64);
}

int32_t
math_aquifer_unpackPackedPosIdx(uint64_t packed) {
    return int32_t(packed & 0xffffffffu64);
}

void
math_aquifer_refreshDistPosIdx_global(uint64_t packedBlockPositions, uint64_t res,
                                      uint64_t aquiferData,
                                      const int32_t x, const int32_t y, const int32_t z) {
    int32_t gx = (x - 5) >> 4;
    int32_t gy = math_floorDiv(y + 1, 12) - 1;
    int32_t gz = (z - 5) >> 4;
    uint64_t A = UINT64_MAX;
    uint64_t B = UINT64_MAX;
    uint64_t C = UINT64_MAX;
    uint64_t D = UINT64_MAX;

    uint64_t ps[12];

    uint64_t index = 12; // 12 max
    for (int32_t offY = 0; offY <= 2; ++offY) {
        int32_t gymul = gy * 12 + offY * 12;
        for (int32_t offZ = 0; offZ <= 1; ++offZ) {
            int32_t gzmul = (gz + offZ) << 4;

            uint64_t index0 = index - 1;
            uint32_t posIdx0 = math_aquifer_index_global(aquiferData, gx, gy + offY, gz + offZ);
            uint32_t position0 = uint32_t(ConstUint16Ref(packedBlockPositions + uint64_t(posIdx0 * 2)).val);
            int32_t dx0 = (gx << 4) + math_aquifer_unpackPackedX(position0) - x;
            int32_t dy0 = gymul + math_aquifer_unpackPackedY(position0) - y;
            int32_t dz0 = gzmul + math_aquifer_unpackPackedZ(position0) - z;
            uint64_t dist_0 = uint64_t(dx0 * dx0 + dy0 * dy0 + dz0 * dz0);

            uint64_t index1 = index - 2;
            uint32_t posIdx1 = posIdx0 + 1;
            uint32_t position1 = uint32_t(ConstUint16Ref(packedBlockPositions + uint64_t(posIdx1 * 2)).val);
            int32_t dx1 = ((gx + 1) << 4) + math_aquifer_unpackPackedX(position1) - x;
            int32_t dy1 = gymul + math_aquifer_unpackPackedY(position1) - y;
            int32_t dz1 = gzmul + math_aquifer_unpackPackedZ(position1) - z;
            uint64_t dist_1 = uint64_t(dx1 * dx1 + dy1 * dy1 + dz1 * dz1);

            ps[12 - index] = (dist_0 << 36u64) | (index0 << 32u64) | (uint64_t(posIdx0));
            ps[13 - index] = (dist_1 << 36u64) | (index1 << 32u64) | (uint64_t(posIdx1));

            index -= 2;
        }
    }

    A = ps[0];
    for (uint32_t i = 1; i < 12; i ++) {
        uint64_t p1 = ps[i];
        if (p1 <= C) {
            uint64_t n11 = max(A, p1);
            A = min(A, p1);

            uint64_t n12 = max(B, n11);
            B = min(B, n11);

            uint64_t n13 = max(C, n12);
            C = min(C, n12);

            D = min(D, n13);
        }
    }

    RWUint64Ref(res + uint64_t(0)).val = A;
    RWUint64Ref(res + uint64_t(8)).val = B;
    RWUint64Ref(res + uint64_t(16)).val = C;
    RWUint64Ref(res + uint64_t(24)).val = D;
}

const uint32_t MASK_enableFlatCache = 1 << 0;
const uint32_t MASK_enableAllCaches = (1 << 1) | MASK_enableFlatCache;

uint64_t df_data_offset_global(uint64_t root, const int32_t index) {
    int32_t offset = ConstInt32Ref(ptr_shift_global(root, 128) + uint64_t(index * 4)).val;
    return offset != 0 ? ptr_shift_global(root, offset) : NULL;
}

double math_clampedMap(const double value, const double oldStart, const double oldEnd, const double newStart, const double newEnd) {
    return math_clampedLerp(newStart, newEnd, math_getLerpProgress(value, oldStart, oldEnd));
}

int32_t math_roundDownToMultiple(const double a, const int32_t b) {
    return (int32_t(math_floor)(a / double(b))) * b;
}

extern const int32_t genShapeCfg_minimumY;
extern const int32_t genShapeCfg_height;
extern const uint32_t genShapeCfg_horizontalSize;
extern const uint32_t genShapeCfg_verticalSize;

uint32_t genShapeCfg_verticalCellBlockCount() {
    return math_biome2block(genShapeCfg_verticalSize);
}

uint32_t genShapeCfg_horizontalCellBlockCount() {
    return math_biome2block(genShapeCfg_horizontalSize);
}

layout(buffer_reference, scalar) buffer worldgen_params_t {
// cache size is (size + 1) to account for interpolation
    int32_t startBiomeX;
    int32_t startBiomeZ;
    int32_t sizeBiomeX;
    int32_t sizeBiomeZ;

    // cache size is (size + 1) to account for interpolation
    int32_t startCellX;
    int32_t startCellY;
    int32_t startCellZ;
    int32_t sizeCellX;
    int32_t sizeCellY;
    int32_t sizeCellZ;

    // cache size is actually size
    int32_t estimateSurfaceHeight_startBiomeX;
    int32_t estimateSurfaceHeight_startBiomeZ;
    int32_t estimateSurfaceHeight_sizeBiomeX;
    int32_t estimateSurfaceHeight_sizeBiomeZ;

    // cache size is actually size
    int32_t cache2d_startX;
    int32_t cache2d_startZ;
    int32_t cache2d_sizeX;
    int32_t cache2d_sizeZ;

    int32_t offset_estimateSurfaceHeight;
    int32_t genConfig_defaultBlock;
    int32_t genConfig_defaultFluid; // see aquifer code for blockstate defs
    int32_t offset_aquifer;
    int32_t offset_fluidLevelSampler; // aquifer_fluidlevel_t[] minimumY -> height
    int32_t offset_oreVeinRandom;
};

struct interpolation_pos_t {
int32_t cellRelX;
    int32_t cellRelY;
    int32_t cellRelZ;
    int32_t cellBlockX;
    int32_t cellBlockY;
    int32_t cellBlockZ;
};

interpolation_pos_t df_get_interpolation_pos(uint64_t params, const int32_t x, const int32_t y, const int32_t z) {
    int32_t cellRelX = math_floorDiv(x, genShapeCfg_horizontalCellBlockCount()) - params.startCellX;
    int32_t cellRelY = math_floorDiv(y, genShapeCfg_verticalCellBlockCount()) - params.startCellY;
    int32_t cellRelZ = math_floorDiv(z, genShapeCfg_horizontalCellBlockCount()) - params.startCellZ;
    int32_t cellBlockX = math_floorMod(x, genShapeCfg_horizontalCellBlockCount());
    int32_t cellBlockY = math_floorMod(y, genShapeCfg_verticalCellBlockCount());
    int32_t cellBlockZ = math_floorMod(z, genShapeCfg_horizontalCellBlockCount());
    if (cellRelX < 0 || cellRelY < 0 || cellRelZ < 0 || cellRelX >= params.sizeCellX || cellRelY >= params.sizeCellY || cellRelZ >= params.sizeCellY || cellBlockX < 0 || cellBlockY < 0 || cellBlockZ < 0) {
        #ifdef DEBUG
        printf("trap: cellRelX < 0 || cellRelY < 0 || cellRelZ < 0 || cellRelX >= params.sizeCellX || cellRelY >= params.sizeCellY || cellRelZ >= params.sizeCellY || cellBlockX < 0 || cellBlockY < 0 || cellBlockZ < 0\n x=%d y=%d z=%d params.sizeCellX=%d params.sizeCellY=%d params.sizeCellZ=%d genShapeCfg_horizontalCellBlockCount()=%d genShapeCfg_verticalCellBlockCount()=%d params.startCellX=%d params.startCellY=%d params.startCellZ=%d\n", x, y, z, params.sizeCellX, params.sizeCellY, params.sizeCellZ, genShapeCfg_horizontalCellBlockCount(), genShapeCfg_verticalCellBlockCount(), params.startCellX, params.startCellY, params.startCellZ);
        #endif
        __builtin_trap();
        __builtin_unreachable();
        return (interpolation_pos_t) {};
    }
    return (interpolation_pos_t) {
        .cellRelX = cellRelX,
        .cellRelY = cellRelY,
        .cellRelZ = cellRelZ,
        .cellBlockX = cellBlockX,
        .cellBlockY = cellBlockY,
        .cellBlockZ = cellBlockZ,
    };
}

uint32_t df_address_flatcache_buffer(uint64_t params, const uint32_t cacheIndex, const uint32_t offsetX, const uint32_t offsetZ) {
    if (offsetX > params.sizeBiomeX || offsetZ > params.sizeBiomeZ) {
        #ifdef DEBUG
        printf("trap: offsetX > params.sizeBiomeX || offsetZ > params.sizeBiomeZ\n offsetX=%d offsetZ=%d params.sizeBiomeX=%d params.sizeBiomeZ=%d\n", offsetX, offsetZ, params.sizeBiomeX, params.sizeBiomeZ);
        #endif
        __builtin_trap();
        __builtin_unreachable();
        return 0;
    }
    return ((cacheIndex) * (params.sizeBiomeX + 1) + offsetX) * (params.sizeBiomeZ + 1) + offsetZ;
}

uint32_t df_address_cache2d_buffer(uint64_t params, const uint32_t cacheIndex, const uint32_t offsetX, const uint32_t offsetZ) {
    if (offsetX >= params.cache2d_sizeX || offsetZ >= params.cache2d_sizeZ) {
        #ifdef DEBUG
        printf("trap: offsetX >= params.cache2d_sizeX || offsetZ >= params.cache2d_sizeZ\n offsetX=%d offsetZ=%d params.cache2d_sizeX=%d params.cache2d_sizeZ=%d\n", offsetX, offsetZ, params.cache2d_sizeX, params.cache2d_sizeZ);
        #endif
        __builtin_trap();
        __builtin_unreachable();
        return 0;
    }
    return ((cacheIndex) * (params.cache2d_sizeX) + offsetX) * (params.cache2d_sizeZ) + offsetZ;
}

uint32_t df_address_interpolator_buffer(uint64_t params, const uint32_t cacheIndex, const int32_t cellX, const int32_t cellY, const int32_t cellZ) {
    if (cellX < 0 || cellY < 0 || cellZ < 0 || cellX > params.sizeCellX || cellY > params.sizeCellY || cellZ > params.sizeCellZ) {
        #ifdef DEBUG
        printf("trap: cellX < 0 || cellY < 0 || cellZ < 0 || cellX > params.sizeCellX || cellY > params.sizeCellY || cellZ > params.sizeCellZ\n cellX=%d cellY=%d cellZ=%d params.sizeCellX=%d params.sizeCellY=%d params.sizeCellZ=%d\n", cellX, cellY, cellZ, params.sizeCellX, params.sizeCellY, params.sizeCellZ);
        #endif
        __builtin_trap();
        __builtin_unreachable();
        return 0;
    }
    return ((((cacheIndex) * (params.sizeCellX + 1) + cellX) * (params.sizeCellY + 1) + cellY) * (params.sizeCellZ + 1) + cellZ);
}

struct cache_result_t {
bool cached;
    double res;
};

cache_result_t df_cachelike_interpolator(uint64_t params, uint64_t interpolator_buffer, const uint32_t cacheIndex, const int32_t x, const int32_t y, const int32_t z, const uint32_t interpolationState) {
    if (!params || (interpolationState & MASK_enableAllCaches) != MASK_enableAllCaches) {
        return (cache_result_t) { .cached = false, .res = nan(uint64_t(0)) };
    }
    // if (!params_local.isSamplingForCaches) {
    //     *res = data.result;
    //     return true;
    // }
    const interpolation_pos_t pos = df_get_interpolation_pos(params, x, y, z);
    const double res = math_lerp3(
        double(pos).cellBlockX / double(genShapeCfg_horizontalCellBlockCount)(),
        double(pos).cellBlockY / double(genShapeCfg_verticalCellBlockCount)(),
        double(pos).cellBlockZ / double(genShapeCfg_horizontalCellBlockCount)(),
        // data.x0y0z0,
        // data.x1y0z0,
        // data.x0y1z0,
        // data.x1y1z0,
        // data.x0y0z1,
        // data.x1y0z1,
        // data.x0y1z1,
        // data.x1y1z1
        interpolator_buffer[df_address_interpolator_buffer(params, cacheIndex, pos.cellRelX, pos.cellRelY, pos.cellRelZ)],
        interpolator_buffer[df_address_interpolator_buffer(params, cacheIndex, pos.cellRelX + 1, pos.cellRelY, pos.cellRelZ)],
        interpolator_buffer[df_address_interpolator_buffer(params, cacheIndex, pos.cellRelX, pos.cellRelY + 1, pos.cellRelZ)],
        interpolator_buffer[df_address_interpolator_buffer(params, cacheIndex, pos.cellRelX + 1, pos.cellRelY + 1, pos.cellRelZ)],
        interpolator_buffer[df_address_interpolator_buffer(params, cacheIndex, pos.cellRelX, pos.cellRelY, pos.cellRelZ + 1)],
        interpolator_buffer[df_address_interpolator_buffer(params, cacheIndex, pos.cellRelX + 1, pos.cellRelY, pos.cellRelZ + 1)],
        interpolator_buffer[df_address_interpolator_buffer(params, cacheIndex, pos.cellRelX, pos.cellRelY + 1, pos.cellRelZ + 1)],
        interpolator_buffer[df_address_interpolator_buffer(params, cacheIndex, pos.cellRelX + 1, pos.cellRelY + 1, pos.cellRelZ + 1)]
    );
    return (cache_result_t) { .cached = true, .res = res };
}

cache_result_t df_cachelike_flatcache(uint64_t params, uint64_t data, const uint32_t cacheIndex, const int32_t x, const int32_t y, const int32_t z, const uint32_t interpolationState) {
    if (!params || (interpolationState & MASK_enableFlatCache) != MASK_enableFlatCache) {
        return (cache_result_t) { .cached = false, .res = nan(uint64_t(0)) };
    }
    const int32_t offsetX = math_block2biome(x) - params.startBiomeX;
    const int32_t offsetZ = math_block2biome(z) - params.startBiomeZ;
    if (offsetX >= 0 && offsetZ >= 0 && offsetX <= params.sizeBiomeX && offsetZ <= params.sizeBiomeZ) {
        const double res = data[df_address_flatcache_buffer(params, cacheIndex, offsetX, offsetZ)];
        return (cache_result_t) { .cached = true, .res = res };
    } else {
        return (cache_result_t) { .cached = false, .res = nan(uint64_t(0)) };
    }
}

cache_result_t df_cachelike_cache2d(uint64_t params, uint64_t data, const uint32_t cacheIndex, const int32_t x, const int32_t y, const int32_t z, const uint32_t interpolationState) {
    if (!params || (interpolationState & MASK_enableAllCaches) != MASK_enableAllCaches) {
        return (cache_result_t) { .cached = false, .res = nan(uint64_t(0)) };
    }
    const int32_t offsetX = x - params.cache2d_startX;
    const int32_t offsetZ = z - params.cache2d_startZ;
    if (offsetX >= 0 && offsetZ >= 0 && offsetX < params.cache2d_sizeX && offsetZ < params.cache2d_sizeZ) {
        const double res = data[df_address_cache2d_buffer(params, cacheIndex, offsetX, offsetZ)];
        return (cache_result_t) { .cached = true, .res = res };
    } else {
        return (cache_result_t) { .cached = false, .res = nan(uint64_t(0)) };
    }
}

double df_caveScaler_scaleCaves(const double value) {
    if (value < -0.75) {
        return 0.5;
    } else if (value < -0.5) {
        return 0.75;
    } else if (value < 0.5) {
        return 1.0;
    } else {
        return value < 0.75 ? 2.0 : 3.0;
    }
}

double df_caveScaler_scaleTunnels(const double value) {
    if (value < -0.5) {
        return 0.75;
    } else if (value < 0.0) {
        return 1.0;
    } else {
        return value < 0.5 ? 1.5 : 2.0;
    }
}

int32_t df_spline_findRangeForLocation(uint64_t locations, const uint32_t locations_len, const float x) {
    int32_t min = 0;
    int32_t i = locations_len;

    while (i > 0) {
        int32_t j = i / 2;
        int32_t k = min + j;
        if (x < locations[k]) {
            i = j;
        } else {
            min = k + 1;
            i -= j + 1;
        }
    }

    return min - 1;
}

int32_t df_spline_findRangeForLocation_const(uint64_t locations, const uint32_t locations_len, const float x) {
    int32_t min = 0;
    int32_t i = locations_len;

    while (i > 0) {
        int32_t j = i / 2;
        int32_t k = min + j;
        if (x < locations[k]) {
            i = j;
        } else {
            min = k + 1;
            i -= j + 1;
        }
    }

    return min - 1;
}

float df_spline_sampleOutsideRange(const float point, uint64_t locations, const float value, uint64_t derivatives, const int i) {
    float f = derivatives[i];
    return f == 0.0F ? value : value + f * (point - locations[i]);
}

float df_spline_sampleOutsideRange_const(const float point, uint64_t locations, const float value, uint64_t derivatives, const int i) {
    float f = derivatives[i];
    return f == 0.0F ? value : value + f * (point - locations[i]);
}

// StructureWeightSampler
const const int32_t SWSTA_NONE = 0;
const const int32_t SWSTA_BURY = 1;
const const int32_t SWSTA_BEARD_THIN = 2;
const const int32_t SWSTA_BEARD_BOX = 3;
const const int32_t SWSTA_ENCAPSULATE = 4;

layout(buffer_reference, scalar) buffer sws_index_t {
// chunk pos
    int32_t startX;
    int32_t startZ;
    uint32_t sizeX;
    uint32_t sizeZ;
};

layout(buffer_reference, scalar) buffer sws_data_t {
uint32_t pieceLength;
    int32_t boxStartX;
    int32_t boxStartY;
    int32_t boxStartZ;
    int32_t boxEndX;
    int32_t boxEndY;
    int32_t boxEndZ;
    int32_t groundLevelDelta;
    int32_t terrainAdjustment;

    uint32_t funcLength;
    int32_t sourceX;
    int32_t sourceGroundY;
    int32_t sourceZ;

    int32_t affectedBox_startX;
    int32_t affectedBox_startY;
    int32_t affectedBox_startZ;
    int32_t affectedBox_endX;
    int32_t affectedBox_endY;
    int32_t affectedBox_endZ;
};

double __df_structureWeightSampler_getMagnitudeWeight(const double x, const double y, const double z) {
    double d = sqrt(x * x + y * y + z * z);
    if (d > 6.0) {
        return 0.0;
    } else {
        return 1.0 - d / 6.0;
    }
}

double math_fastInverseSqrt(double a) {
    union {
        double d;
        uint64_t l;
    } x;
    x.d = a;
    double d = 0.5 * x.d;
    x.l = 6910469410427058090L - (x.l >> 1);
    return x.d * (1.5 - d * x.d * x.d);
}

double df_structureWeightSampler_getStructureWeight_impl(uint64_t structureWeightSamplerTable, const double x, const double y, const double z, const double yy) {
    int32_t i = x + 12;
    int32_t j = y + 12;
    int32_t k = z + 12;
    if (i >= 0 && i < 24 && j >= 0 && j < 24 && k >= 0 && k < 24) {
        double d = double(yy) + 0.5;
        // double e = MathHelper.squaredMagnitude(double(x), d, double(z));
        double e = (double(x) * double(x)) + (d * d) + (double(z) * double(z));
        double f = -d * math_fastInverseSqrt(e / 2.0) / 2.0;
        return f * double(structureWeightSamplerTable)[k * 24 * 24 + i * 24 + j];
    } else {
        return 0.0;
    }
}

double df_structureWeightSampler_sample(uint64_t structureWeightSamplerTable, uint64_t data_index, const int32_t x, const int32_t y, const int32_t z) {
    const int32_t chunkX = x >> 4;
    const int32_t chunkZ = z >> 4;
    const uint32_t dataRelX = uint32_t(clamp)(chunkX - data_index.startX, 0, int32_t(data_index)->sizeX - 1);
    const uint32_t dataRelZ = uint32_t(clamp)(chunkZ - data_index.startZ, 0, int32_t(data_index)->sizeZ - 1);
    uint64_t sws_data_offsets = ptr_shift_global(data_index, sizeof(sws_index_t));
    const uint32_t sws_current_offset = ConstUint32Ref(sws_data_offsets + uint64_t((dataRelX * data_index.sizeZ + dataRelZ) * 4)).val;

    if (!sws_current_offset) return 0.0;

    uint64_t data = ptr_shift_global(data_index, ConstUint32Ref(sws_data_offsets + uint64_t((dataRelX * data_index.sizeZ + dataRelZ) * 4)).val);

    uint64_t boxStartX = ptr_shift_global(data, data.boxStartX);
    uint64_t boxStartY = ptr_shift_global(data, data.boxStartY);
    uint64_t boxStartZ = ptr_shift_global(data, data.boxStartZ);
    uint64_t boxEndX = ptr_shift_global(data, data.boxEndX);
    uint64_t boxEndY = ptr_shift_global(data, data.boxEndY);
    uint64_t boxEndZ = ptr_shift_global(data, data.boxEndZ);
    uint64_t groundLevelDelta = ptr_shift_global(data, data.groundLevelDelta);
    uint64_t terrainAdjustment = ptr_shift_global(data, data.terrainAdjustment);

    uint64_t sourceX = ptr_shift_global(data, data.sourceX);
    uint64_t sourceGroundY = ptr_shift_global(data, data.sourceGroundY);
    uint64_t sourceZ = ptr_shift_global(data, data.sourceZ);

    if (x < data.affectedBox_startX || x > data.affectedBox_endX ||
        y < data.affectedBox_startY || y > data.affectedBox_endY ||
        z < data.affectedBox_startZ || z > data.affectedBox_endZ) {
        return 0.0;
    }

    double d = 0.0;

    for (uint32_t i = 0; i < data.pieceLength; i ++) {
        int32_t m = max(0, max(ConstInt32Ref(boxStartX + uint64_t(i * 4)).val - x, x - ConstInt32Ref(boxEndX + uint64_t(i * 4)).val));
        int32_t n = max(0, max(ConstInt32Ref(boxStartZ + uint64_t(i * 4)).val - z, z - ConstInt32Ref(boxEndZ + uint64_t(i * 4)).val));
        int32_t o = ConstInt32Ref(boxStartY + uint64_t(i * 4)).val + groundLevelDelta[i];
        int32_t p = y - o;

        switch (terrainAdjustment[i]) {
            case SWSTA_NONE:
                d += 0.0;
                break;
            case SWSTA_BURY:
                d += __df_structureWeightSampler_getMagnitudeWeight(m, double(p) / 2.0, n);
                break;
            case SWSTA_BEARD_THIN:
                d += df_structureWeightSampler_getStructureWeight_impl(structureWeightSamplerTable, m, p, n, p) * 0.8;
                break;
            case SWSTA_BEARD_BOX:
                d += df_structureWeightSampler_getStructureWeight_impl(structureWeightSamplerTable, m, max(0, max(o - y, y - ConstInt32Ref(boxEndY + uint64_t(i * 4)).val)), n, p) * 0.8;
                break;
            case SWSTA_ENCAPSULATE:
                d += __df_structureWeightSampler_getMagnitudeWeight(double(m) / 2.0, double(max)(0, max(ConstInt32Ref(boxStartY + uint64_t(i * 4)).val - y, y - ConstInt32Ref(boxEndY + uint64_t(i * 4)).val)) / 2.0, double(n) / 2.0) * 0.8;
                break;
            default:
                #ifdef DEBUG
                printf("trap: unexpected terrainAdjustment[i]=%d, i=%u\n", terrainAdjustment[i], i);
                #endif
                __builtin_trap();
                __builtin_unreachable();
                return nan(uint32_t(0));
        };
    }

    for (uint32_t i = 0; i < data.funcLength; i ++) {
        int r = x - sourceX[i];
        int l = y - sourceGroundY[i];
        int m = z - sourceZ[i];
        d += df_structureWeightSampler_getStructureWeight_impl(structureWeightSamplerTable, r, l, m, l) * 0.4;
    }

    return d;
}

struct sample_int32_ctx_t {
uint64_t const_data;
    uint64_t rw_data;
    int32_t x, y, z;
    uint32_t sample_flags;
};

sample_int32_ctx_t make_sample_int32_ctx(uint64_t const_data, uint64_t rw_data, const int32_t x, const int32_t y, const int32_t z, const uint32_t sample_flags) {
    return (sample_int32_ctx_t) {
        .const_data = const_data,
        .rw_data = rw_data,
        .x = x,
        .y = y,
        .z = z,
        .sample_flags = sample_flags
    };
}

double uninitializedF32() {
    union {
        float f;
        uint32_t l;
    } x;
    x.l = 0x7f8abcdeU;
    return x.f;
}

double uninitializedF64() {
    union {
        double d;
        uint64_t l;
    } x;
    x.l = 0x7ffddb972d486a4fu64;
    return x.d;
}

float assertNotUninitializedF32(float in) {
    union {
        float f;
        uint32_t l;
    } x;
    x.f = in;
    if (x.l == 0x7f8abcdeU) {
        __builtin_trap();
    }
    return in;
}

double assertNotUninitializedF64(double in) {
    union {
        double d;
        uint64_t l;
    } x;
    x.d = in;
    if (x.l == 0x7ffddb972d486a4fu64) {
        __builtin_trap();
    }
    return in;
}

#ifndef DEBUG
#define df_cachelike_trap_printf(desc, ctx)
#else
#define df_cachelike_trap_printf(desc, ctx) printf("trap: accessing cachelike \"%s\" beyond cache boundary\n ctx.xyz=(%d, %d, %d) ctx.sample_flags=%u\n", desc, ctx.x, ctx.y, ctx.z, ctx.sample_flags)
#endif

#define df_binding_def0(name, suffix) \
    double df_binding_##name##suffix(const sample_int32_ctx_t ctx);

#define df_binding_def(name) \
    df_binding_def0(name, _uncached) \
    df_binding_def0(name, _flatcache_only) \
    df_binding_def0(name, _fully_cached) \
    df_binding_def0(name, )

df_binding_def(barrier)
df_binding_def(fluid_level_floodedness)
df_binding_def(fluid_level_spread)
df_binding_def(lava)
df_binding_def(temperature)
df_binding_def(vegetation)
df_binding_def(continents)
df_binding_def(erosion)
df_binding_def(depth)
df_binding_def(ridges)
df_binding_def(preliminary_surface_level)
df_binding_def(final_density)
df_binding_def(vein_toggle)
df_binding_def(vein_ridged)
df_binding_def(vein_gap)
df_binding_def(final_final_density)

#undef df_binding_def

int32_t chunkNoiseSampler_estimateSurfaceHeight0(uint64_t const_data, uint64_t rw_data, const int32_t blockX, const int32_t blockZ) {
    return math_floor(df_binding_preliminary_surface_level(make_sample_int32_ctx(const_data, rw_data, blockX, 0, blockZ, 0)));
}

// const int32_t CACHE_CHUNK_RADIUS_estimateSurfaceHeight = 4;
// const int32_t CACHE_SIZE_estimateSurfaceHeight = (CACHE_CHUNK_RADIUS_estimateSurfaceHeight * 2 + 1) << 2;

int32_t chunkNoiseSampler_estimateSurfaceHeight(uint64_t const_data, uint64_t rw_data, const int32_t blockX, const int32_t blockZ) {
    uint64_t params = rw_data;
    uint64_t cache = ptr_shift_global(rw_data, params.offset_estimateSurfaceHeight);
    int32_t biomeX = math_block2biome(blockX);
    int32_t biomeZ = math_block2biome(blockZ);
    int32_t relX = biomeX - params.estimateSurfaceHeight_startBiomeX;
    int32_t relZ = biomeZ - params.estimateSurfaceHeight_startBiomeZ;
    if (!cache || relX < 0 || relZ < 0 || relX >= params.estimateSurfaceHeight_sizeBiomeX || relZ >= params.estimateSurfaceHeight_sizeBiomeZ) {
        // // SLOW PATH
        // printf("SLOW PATH\n");
        // return chunkNoiseSampler_estimateSurfaceHeight0(const_data, math_biome2block(biomeX), math_biome2block(biomeZ));
        #ifdef DEBUG
        printf("trap: accessing uncached region for estimateSurfaceHeight\n hasCache=%d blockPos=(%d, %d), cacheStartBiomeX=%d cacheStartBiomeZ=%d cacheSizeBiomeX=%d cacheSizeBiomeZ=%d\n", (cache ? 1 : 0), blockX, blockZ, params.estimateSurfaceHeight_startBiomeX, params.estimateSurfaceHeight_startBiomeZ, params.estimateSurfaceHeight_sizeBiomeX, params.estimateSurfaceHeight_sizeBiomeZ);
        #endif
        __builtin_trap();
        __builtin_unreachable();
        return nan(uint64_t(0L));
    } else {
        return cache[relX * params.estimateSurfaceHeight_sizeBiomeZ + relZ];
    }
}

#ifdef DF_COMPILE_ESTIMATE_SURFACE_HEIGHT
kernel ) void chunkNoiseSampler_estimateSurfaceHeight_prefill_indep(uint64_t const_data, uint64_t rw_data,
                                                                                                                 uint64_t cache,
                                                                                                                 const int32_t startChunkX, const int32_t startChunkZ, const uint32_t cacheWidth) {
    if (!const_data || !cache || !rw_data) {
        #ifdef DEBUG
        printf("trap: !const_data || !cache || !rw_data\n const_data=%p cache=%p rw_data=%p\n", const_data, cache, rw_data);
        #endif
        __builtin_trap();
        __builtin_unreachable();
        return;
    }

    int32_t relX = get_global_id(0);
    int32_t relZ = get_global_id(1);
    int32_t biomeX = (startChunkX << 2) + relX;
    int32_t biomeZ = (startChunkZ << 2) + relZ;
    int32_t blockX = math_biome2block(biomeX);
    int32_t blockZ = math_biome2block(biomeZ);
    cache[relX * cacheWidth + relZ] = chunkNoiseSampler_estimateSurfaceHeight0(const_data, rw_data, blockX, blockZ);
}
#endif

const const uint64_t RANDOM_Checked = 0;
const const uint64_t RANDOM_Xoroshiro128PlusPlus = 1;
 
layout(buffer_reference, scalar) buffer random_state_t {
uint64_t type; // see consts above
    uint64_t seedLo;
    uint64_t seedHi;
};

int64_t math_hashCode_int32x3(int32_t x, int32_t y, int32_t z) {
    int64_t l = int64_t(x * 3129871) ^ int64_t(z) * 116129781L ^ int64_t(y);
    l = l * l * 42317861L + l * 11L;
    return l >> 16;
}

uint64_t math_mixStafford13(uint64_t seed) {
    seed = (int64_t(seed ^ seed >> 30)) * -4658895280553007687L;
    seed = (int64_t(seed ^ seed >> 27)) * -7723592293110705685L;
    return seed ^ seed >> 31;
}

void random_state_set_seed(uint64_t state, int64_t seed) {
    if (state.type == RANDOM_Checked) {
        state.seedLo = (seed ^ 25214903917L) & 281474976710655L;
    } else if (state.type == RANDOM_Xoroshiro128PlusPlus) {
        state.seedLo = seed ^ 7640891576956012809L;
        state.seedHi = (int64_t(state)->seedLo) + -7046029254386353131L;
        state.seedLo = math_mixStafford13(state.seedLo);
        state.seedHi = math_mixStafford13(state.seedHi);
    } else {
        #ifdef DEBUG
        printf("trap: random_state_set_seed: unexpected random type %lu\n", state.type);
        #endif
        __builtin_trap();
        __builtin_unreachable();
    }
}

void random_state_split_coords(uint64_t state, int32_t x, int32_t y, int32_t z) {
    if (state.type == RANDOM_Checked) {
        int64_t l = math_hashCode_int32x3(x, y, z);
        state.seedLo ^= l;
        random_state_set_seed(state, state.seedLo);
    } else if (state.type == RANDOM_Xoroshiro128PlusPlus) {
        int64_t l = math_hashCode_int32x3(x, y, z);
        state.seedLo ^= l;
        if ((state.seedLo | state.seedHi) == 0L) {
            state.seedLo = -7046029254386353131L;
            state.seedHi = 7640891576956012809L;
        }
    } else {
        #ifdef DEBUG
        printf("trap: random_state_split_coords: unexpected random type %lu\n", state.type);
        #endif
        __builtin_trap();
        __builtin_unreachable();
    }
}

int32_t random_state_Checked_next(uint64_t state, int32_t bits) {
    if (state.type != RANDOM_Checked) {
        #ifdef DEBUG
        printf("trap: random_state_Checked_next: unexpected random type %lu\n", state.type);
        #endif
        __builtin_trap();
        __builtin_unreachable();
        return 0;
    }

    int32_t m = state.seedLo * 25214903917L + 11L & 281474976710655L;
    state.seedLo = m;
    return int32_t(m >> (48 - bits));
}

int64_t random_state_Xoroshiro128PlusPlus_next0(uint64_t state) {
    if (state.type != RANDOM_Xoroshiro128PlusPlus) {
        #ifdef DEBUG
        printf("trap: random_state_Xoroshiro128PlusPlus_next0: unexpected random type %lu\n", state.type);
        #endif
        __builtin_trap();
        __builtin_unreachable();
        return 0;
    }

    int64_t l = state.seedLo;
    int64_t m = state.seedHi;
    int64_t n = math_rotateLeftU64(uint64_t(l + m), 17) + l;
    m ^= l;
    state.seedLo = math_rotateLeftU64(uint64_t(l), 49) ^ m ^ m << 21;
    state.seedHi = math_rotateLeftU64(uint64_t(m), 28);
    return n;
}

int64_t random_state_Xoroshiro128PlusPlus_next(uint64_t state, int32_t bits) {
    return (uint64_t(random_state_Xoroshiro128PlusPlus_next0)(state)) >> (64 - bits);
}

float random_state_nextFloat(uint64_t state) {
    if (state.type == RANDOM_Checked) {
        return float(random_state_Checked_next)(state, 24) * 5.9604645E-8F;
    } else if (state.type == RANDOM_Xoroshiro128PlusPlus) {
        return float(random_state_Xoroshiro128PlusPlus_next)(state, 24) * 5.9604645E-8F;
    } else {
        #ifdef DEBUG
        printf("trap: random_state_nextFloat: unexpected random type %lu\n", state.type);
        #endif
        __builtin_trap();
        __builtin_unreachable();
        return nan(uint32_t(0));
    }
}

int32_t random_state_nextIntBounded(uint64_t state, int32_t bound) {
    if (state.type == RANDOM_Checked) {
        if (bound <= 0) {
            #ifdef DEBUG
            printf("trap: random_state_nextIntBounded RANDOM_Checked: bound <= 0\n bound=%d\n", bound);
            #endif
            __builtin_trap();
            __builtin_unreachable();
            return 0;
        } else if ((bound & bound - 1) == 0) {
            return int32_t(int64_t(bound) * int64_t(random_state_Checked_next)(state, 31) >> 31);
        } else {
            int32_t i;
            int32_t j;
            do {
                i = random_state_Checked_next(state, 31);
                j = i % bound;
            } while (i - j + (bound - 1) < 0);

            if (j >= bound) {
                #ifdef DEBUG
                printf("trap: random_state_nextIntBounded RANDOM_Checked ret >= bound\n ret=%d bound=%d\n", j, bound);
                #endif
                __builtin_trap();
                __builtin_unreachable();
                return 0;
            }
            return j;
        }
    } else if (state.type == RANDOM_Xoroshiro128PlusPlus) {
        if (bound <= 0) {
            #ifdef DEBUG
            printf("trap: random_state_nextIntBounded RANDOM_Xoroshiro128PlusPlus: bound <= 0\n bound=%d\n", bound);
            #endif
            __builtin_trap();
            __builtin_unreachable();
            return 0;
        } else {
            int64_t l = uint32_t(random_state_Xoroshiro128PlusPlus_next0)(state);
            int64_t m = l * int64_t(bound);
            int64_t n = m & 4294967295L;
            if (n < int64_t(bound)) {
                for (int32_t i = int32_t((uint32_t(~bound) + 1u) % uint32_t(bound)); n < int64_t(i); n = m & 4294967295L) {
                    l = uint32_t(random_state_Xoroshiro128PlusPlus_next0)(state);
                    m = l * int64_t(bound);
                }
            }

            int64_t o = m >> 32;
            int32_t ret = int32_t(o);
            if (ret >= bound) {
                #ifdef DEBUG
                printf("trap: random_state_nextIntBounded RANDOM_Checked ret >= bound\n ret=%d bound=%d\n", ret, bound);
                #endif
                __builtin_trap();
                __builtin_unreachable();
                return 0;
            }
            return int32_t(o);
        }
    } else {
        #ifdef DEBUG
        printf("trap: random_state_nextIntBounded: unexpected random type %lu\n", state.type);
        #endif
        __builtin_trap();
        __builtin_unreachable();
        return 0;
    }
}

const const int32_t BLOCK_NULL = 0;
const const int32_t BLOCK_AIR = 1;
const const int32_t BLOCK_DEFAULT_BLOCK = 2;
const const int32_t BLOCK_WATER = 3;
const const int32_t BLOCK_LAVA = 4;
const const int32_t BLOCK_COPPER_ORE = 5;
const const int32_t BLOCK_RAW_COPPER_BLOCK = 6;
const const int32_t BLOCK_GRANITE = 7;
const const int32_t BLOCK_DEEPSLATE_IRON_ORE = 8;
const const int32_t BLOCK_RAW_IRON_BLOCK = 9;
const const int32_t BLOCK_TUFF = 10;

layout(buffer_reference, scalar) buffer aquifer_fluidlevel_t {
int32_t y;
    int32_t blockState;
};

// void dbg_checkBlockState(int32_t blockState) {
//     if (blockState < 0 || blockState > 10) {
//         #ifdef DEBUG
//         printf("trap: dbg_checkBlockState: unexpected block state %d\n", blockState);
//         #endif
//         __builtin_trap();
//         __builtin_unreachable();
//     }
// }

int32_t aquifer_fluidlevel_getBlockState_ptr_global(uint64_t data, const int32_t y) {
    return y < data.y ? data.blockState : BLOCK_AIR;
}

int32_t aquifer_fluidlevel_equals_global(uint64_t data0, uint64_t data1) {
    return data0.y == data1.y && data0.blockState == data1.blockState;
}
const const int32_t __aquifer_chunkPosOffset[13][2] = {
    {0, 0}, {-2, -1}, {-1, -1}, {0, -1}, {1, -1}, {-3, 0}, {-2, 0}, {-1, 0}, {1, 0}, {-2, 1}, {-1, 1}, {0, 1}, {1, 1}
};

const uint64_t fluidLevelSampler_getFluidLevel_ptr(uint64_t rw_data, const int32_t y) {
    uint64_t params = rw_data;
    uint64_t fluidLevels = ptr_shift_global(rw_data, params.offset_fluidLevelSampler);
    const int32_t relY = y - genShapeCfg_minimumY;
    return &fluidLevels[clamp(relY, 0, genShapeCfg_height - 1)];
}

bool math_VanillaBiomeParameters_inDeepDarkParameters(const sample_int32_ctx_t ctx) {
    return df_binding_erosion(ctx) < -0.225F && df_binding_depth(ctx) > 0.9F;
}

int32_t __aquifer_getNoiseBasedFluidLevel(uint64_t const_data, int32_t blockX, int32_t blockY, int32_t blockZ, int32_t surfaceHeightEstimate) {
    int32_t i = 16;
    int32_t j = 40;
    int32_t k = blockX >> 4;
    int32_t l = math_floorDiv(blockY, 40);
    int32_t m = blockZ >> 4;
    int32_t n = l * 40 + 20;
    int32_t o = 10;
    double d = df_binding_fluid_level_spread(make_sample_int32_ctx(const_data, NULL, k, l, m, 0)) * 10.0;
    int32_t p = math_roundDownToMultiple(d, 3);
    int32_t q = n + p;
    return min(surfaceHeightEstimate, q);
}

const int32_t DimensionType_field_35479 = -32512; // copied from debugger

int32_t __aquifer_getFluidBlockY(uint64_t const_data, int32_t blockX, int32_t blockY, int32_t blockZ, uint64_t defaultFluidLevel, int32_t surfaceHeightEstimate, bool bl) {
    // DensityFunction.UnblendedNoisePos unblendedNoisePos = new DensityFunction.UnblendedNoisePos(blockX, blockY, blockZ);
    const sample_int32_ctx_t unblendedNoisePos = make_sample_int32_ctx(const_data, NULL, blockX, blockY, blockZ, 0);
    double d;
    double e;
    if (math_VanillaBiomeParameters_inDeepDarkParameters(unblendedNoisePos)) {
        d = -1.0;
        e = -1.0;
    } else {
        int i = surfaceHeightEstimate + 8 - blockY;
        double f = bl ? math_clampedLerp(1.0, 0.0, (double(i)) / 64.0) : 0.0; // inline
        double g = clamp(df_binding_fluid_level_floodedness(unblendedNoisePos), -1.0, 1.0);
        d = g + 0.8 + (f - 1.0) * 1.2; // inline
        e = g + 0.3 + (f - 1.0) * 1.1; // inline
    }

    int i;
    if (e > 0.0) {
        i = defaultFluidLevel.y;
    } else if (d > 0.0) {
        i = __aquifer_getNoiseBasedFluidLevel(const_data, blockX, blockY, blockZ, surfaceHeightEstimate);
    } else {
        i = DimensionType_field_35479;
    }

    return i;
}

int32_t __aquifer_getFluidBlockState(uint64_t const_data, int blockX, int blockY, int blockZ, uint64_t defaultFluidLevel, int fluidLevel) {
    int32_t blockState = defaultFluidLevel.blockState;
    if (fluidLevel <= -10 && fluidLevel != DimensionType_field_35479 && defaultFluidLevel.blockState != BLOCK_LAVA) {
        int i = 64;
        int j = 40;
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

#ifdef DF_COMPILE_AQUIFER_PREFILL
// launch with aquifer sizeX sizeY sizeZ
kernel void aquifer_data_prefill(uint64_t const_data, uint64_t rw_data) {
    if (!const_data || !rw_data) {
        #ifdef DEBUG
        printf("trap: !const_data || !rw_data\n const_data=%p rw_data=%p\n", const_data, rw_data);
        #endif
        __builtin_trap();
        __builtin_unreachable();
        return;
    }

    uint64_t params = rw_data;

    if (!params.offset_aquifer) {
        #ifdef DEBUG
        printf("trap: no aquifer configured\n");
        #endif
        __builtin_trap();
        __builtin_unreachable();
        return;
    }

    uint64_t data = ptr_shift_global(rw_data, params.offset_aquifer);
    uint64_t randomDeriver = ptr_shift_global(data, data.randomDeriver);
    
    uint64_t waterLevels = ptr_shift_global(data, data.waterLevels);
    uint64_t packedBlockPositions = ptr_shift_global(data, data.packedBlockPositions);

    const int32_t curX = data.startX + get_global_id(0);
    const int32_t curY = data.startY + get_global_id(2);
    const int32_t curZ = data.startZ + get_global_id(1);

    // fill packedBlockPositions
    random_state_t derived = *randomDeriver;
    random_state_split_coords(&derived, curX, curY, curZ);
    int32_t r0 = random_state_nextIntBounded(&derived, 10);
    int32_t r1 = random_state_nextIntBounded(&derived, 9);
    int32_t r2 = random_state_nextIntBounded(&derived, 10);
    int32_t blockX = curX * 16 + r0;
    int32_t blockY = curY * 12 + r1;
    int32_t blockZ = curZ * 16 + r2;
    int32_t index = math_aquifer_index_global(data, curX, curY, curZ);
    packedBlockPositions[index] = (uint16_t) ((r0 << 8) | (r1 << 4) | r2);

    int i = INT32_MAX;
    int j = blockY + 12;
    int k = blockY - 12;
    bool bl = false;

    // direct port
    uint64_t fluidLevel = fluidLevelSampler_getFluidLevel_ptr(rw_data, blockY);
    for (uint32_t __i = 0; __i < 13; __i ++) { // 13 comes from __aquifer_chunkPosOffset
        const int32_t offX = __aquifer_chunkPosOffset[__i][0];
        const int32_t offZ = __aquifer_chunkPosOffset[__i][1];
        int32_t l = blockX + (offX << 4);
        int32_t m = blockZ + (offZ << 4);
        int32_t n = chunkNoiseSampler_estimateSurfaceHeight(const_data, rw_data, l, m);
        int32_t o = n + 8;
        bool bl2 = offX == 0 && offZ == 0;
        if (bl2 && k > o) {
            waterLevels[index] = *fluidLevel;
            return;
        }

        bool bl3 = j > o;
        if (bl3 || bl2) {
            uint64_t fluidLevel2 = fluidLevelSampler_getFluidLevel_ptr(rw_data, o);
            if (aquifer_fluidlevel_getBlockState_ptr_global(fluidLevel2, o) != BLOCK_AIR) {
                if (bl2) {
                    bl = true;
                }

                if (bl3) {
                    waterLevels[index] = *fluidLevel2;
                    return;
                }
            }
        }

        i = min(i, n);
    }

    int32_t p = __aquifer_getFluidBlockY(const_data, blockX, blockY, blockZ, fluidLevel, i, bl);
    aquifer_fluidlevel_t res;
    res.y = p;
    res.blockState = __aquifer_getFluidBlockState(const_data, blockX, blockY, blockZ, fluidLevel, p);
    waterLevels[index] = res;
}
#endif

struct aquifer_result_t {
int32_t blockState;
    bool needsFluidTick;
};

double math_aquifer_maxDistance(int i, int a) {
    double d = 25.0;
    return 1.0 - double(abs)(a - i) / 25.0;
}

const const double aquifer_NEEDS_FLUID_TICK_DISTANCE_THRESHOLD = -0x1.851eb851eb852p-1; // = maxDistance(MathHelper.square(10), MathHelper.square(12)) = -0.76

double __aquifer_getQ(const double i, const double d, const double j) {
    double e = i + 0.5 - d;
    double f = j / 2.0;
    double o = f - fabs(e);
    double q;
    if (e > 0.0) {
        if (o > 0.0) {
            q = o / 1.5;
        } else {
            q = o / 2.5;
        }
    } else {
        double p = 3.0 + o;
        if (p > 0.0) {
            q = p / 3.0;
        } else {
            q = p / 10.0;
        }
    }
    return q;
}

double __aquifer_postCalculateDensityModified(const sample_int32_ctx_t ctx, const double q, uint64_t mutableDoubleThingy) {
    double r;
    if (!(q < -2.0) && !(q > 2.0)) {
        double s = *mutableDoubleThingy;
        if (isnan(s)) {
            double t = df_binding_barrier(ctx);
            *mutableDoubleThingy = t;
            r = t;
        } else {
            r = s;
        }
    } else {
        r = 0.0;
    }

    return 2.0 * (r + q);
}

double __aquifer_calculateDensityModified(const sample_int32_ctx_t ctx, uint64_t fluidLevel, uint64_t fluidLevel2, uint64_t mutableDoubleThingy) {
    int32_t i = ctx.y;
    int32_t blockState = aquifer_fluidlevel_getBlockState_ptr_global(fluidLevel, i);
    int32_t blockState2 = aquifer_fluidlevel_getBlockState_ptr_global(fluidLevel2, i);
    if ((blockState != BLOCK_LAVA || blockState2 != BLOCK_WATER) && (blockState != BLOCK_WATER || blockState2 != BLOCK_LAVA)) {
        int32_t j = abs(fluidLevel.y - fluidLevel2.y);
        if (j == 0) {
            return 0.0;
        } else {
            double d = 0.5 * double(fluidLevel.y + fluidLevel2.y);
            const double q = __aquifer_getQ(i, d, j);

            return __aquifer_postCalculateDensityModified(ctx, q, mutableDoubleThingy);
        }
    } else {
        return 2.0;
    }
}

bool __aquifer_extractedCheckFG(const sample_int32_ctx_t ctx,
                                       const double density, const double d, uint64_t fluidLevel2, const double f, uint64_t fluidLevel4, uint64_t mutableDoubleThingy) {
    if (f > 0.0) {
        double g = d * f * __aquifer_calculateDensityModified(ctx, fluidLevel2, fluidLevel4, mutableDoubleThingy);
        if (density + g > 0.0) {
            // this.needsFluidTick = false;
            return true;
        }
    }
    return false;
}

aquifer_result_t __aquifer_getFinalBlockState(const sample_int32_ctx_t ctx,
                                                     const double density, const double d, uint64_t fluidLevel2, uint64_t fluidLevel3, 
                                                     const int32_t blockState, uint64_t packedRes, uint64_t mutableDoubleThingy) {
    uint64_t params = ctx.rw_data;
    uint64_t data = ptr_shift_global(ctx.rw_data, params.offset_aquifer);
    uint64_t waterLevels = ptr_shift_global(data, data.waterLevels);

    uint64_t fluidLevel4 = &waterLevels[math_aquifer_unpackPackedPosIdx(packedRes[2])];
    // dbg_checkBlockState(fluidLevel4.blockState);
    int dist1 = math_aquifer_unpackPackedDist(packedRes[0]);
    int dist2 = math_aquifer_unpackPackedDist(packedRes[1]);
    int dist3 = math_aquifer_unpackPackedDist(packedRes[2]);
    int dist4 = math_aquifer_unpackPackedDist(packedRes[3]);
    double f = math_aquifer_maxDistance(dist1, dist3);

    aquifer_result_t nullResult;
    nullResult.blockState = BLOCK_NULL;
    nullResult.needsFluidTick = false;

    if (__aquifer_extractedCheckFG(ctx, density, d, fluidLevel2, f, fluidLevel4, mutableDoubleThingy)) return nullResult;

    double h = math_aquifer_maxDistance(dist2, dist3);
    if (__aquifer_extractedCheckFG(ctx, density, d, fluidLevel3, h, fluidLevel4, mutableDoubleThingy)) return nullResult;

    bool needsFluidTick = false;

    bool bl = !aquifer_fluidlevel_equals_global(fluidLevel2, fluidLevel3);
    bool bl2 = h >= aquifer_NEEDS_FLUID_TICK_DISTANCE_THRESHOLD && !aquifer_fluidlevel_equals_global(fluidLevel3, fluidLevel4);
    bool bl3 = f >= aquifer_NEEDS_FLUID_TICK_DISTANCE_THRESHOLD && !aquifer_fluidlevel_equals_global(fluidLevel2, fluidLevel4);
    if (!bl && !bl2 && !bl3) {
        needsFluidTick = f >= aquifer_NEEDS_FLUID_TICK_DISTANCE_THRESHOLD
            && math_aquifer_maxDistance(dist1, dist4) >= aquifer_NEEDS_FLUID_TICK_DISTANCE_THRESHOLD
            && !aquifer_fluidlevel_equals_global(fluidLevel2, &waterLevels[math_aquifer_unpackPackedPosIdx(packedRes[3])]);
    } else {
        needsFluidTick = true;
    }

    return (aquifer_result_t) {
        .blockState = blockState,
        .needsFluidTick = needsFluidTick,
    };
}

aquifer_result_t aquifer_applyPost_impl(const sample_int32_ctx_t ctx, const double density, const int32_t j, const int32_t i, const int32_t k, uint64_t packedRes) {
    uint64_t params = ctx.rw_data;
    uint64_t data = ptr_shift_global(ctx.rw_data, params.offset_aquifer);
    uint64_t waterLevels = ptr_shift_global(data, data.waterLevels);

    uint64_t fluidLevel2 = &waterLevels[math_aquifer_unpackPackedPosIdx(packedRes[0])];
    double d = math_aquifer_maxDistance(math_aquifer_unpackPackedDist(packedRes[0]), math_aquifer_unpackPackedDist(packedRes[1]));
    int32_t blockState = aquifer_fluidlevel_getBlockState_ptr_global(fluidLevel2, j);
    if (d <= 0.0) {
        bool needsFluidTick = false;
        if (d >= aquifer_NEEDS_FLUID_TICK_DISTANCE_THRESHOLD) {
            uint64_t fluidLevel3 = &waterLevels[math_aquifer_unpackPackedPosIdx(packedRes[1])];
            needsFluidTick = !aquifer_fluidlevel_equals_global(fluidLevel2, fluidLevel3);
        } else {
            needsFluidTick = false;
        }
        return (aquifer_result_t) {
            .blockState = blockState,
            .needsFluidTick = needsFluidTick,
        };
    } else if (blockState == BLOCK_WATER && aquifer_fluidlevel_getBlockState_ptr_global(fluidLevelSampler_getFluidLevel_ptr(ctx.rw_data, j - 1), j - 1) == BLOCK_LAVA) {
        return (aquifer_result_t) {
            .blockState = blockState,
            .needsFluidTick = true,
        };
    } else {
        double mutableDoubleThingy = nan(uint64_t(0));
        uint64_t fluidLevel3 = &waterLevels[math_aquifer_unpackPackedPosIdx(packedRes[1])];
        double e = d * __aquifer_calculateDensityModified(ctx, fluidLevel2, fluidLevel3, &mutableDoubleThingy);
        if (density + e > 0.0) {
            return (aquifer_result_t) {
                .blockState = BLOCK_NULL,
                .needsFluidTick = false,
            };
        } else {
            return __aquifer_getFinalBlockState(ctx, density, d, fluidLevel2, fluidLevel3, blockState, packedRes, &mutableDoubleThingy);
        }
    }
}

aquifer_result_t aquifer_sample(const sample_int32_ctx_t ctx, const double density) {
    uint64_t params = ctx.rw_data;

    if (!params.offset_aquifer) {
        if (density > 0.0) {
            return (aquifer_result_t) {
                .blockState = BLOCK_NULL,
                .needsFluidTick = false
            };
        } else {
            uint64_t fluidLevel = fluidLevelSampler_getFluidLevel_ptr(ctx.rw_data, ctx.y);
            return (aquifer_result_t) {
                .blockState = aquifer_fluidlevel_getBlockState_ptr_global(fluidLevel, ctx.y),
                .needsFluidTick = false,
            };
        }
    }

    uint64_t aquifer_data = ptr_shift_global(ctx.rw_data, params.offset_aquifer);

    int32_t i = ctx.x;
    int32_t j = ctx.y;
    int32_t k = ctx.z;

    if (density > 0.0) {
        return (aquifer_result_t) {
            .blockState = BLOCK_NULL,
            .needsFluidTick = false
        };
    } else {
        uint64_t fluidLevel = fluidLevelSampler_getFluidLevel_ptr(ctx.rw_data, j);
        if (j > aquifer_data.samplingYLowPassCutoff) {
            return (aquifer_result_t) {
                .blockState = aquifer_fluidlevel_getBlockState_ptr_global(fluidLevel, j),
                .needsFluidTick = false
            };
        }
        // if (fluidLevel.getBlockState(j).isOf(Blocks.LAVA)) {
        if (aquifer_fluidlevel_getBlockState_ptr_global(fluidLevel, j) == BLOCK_LAVA) {
            return (aquifer_result_t) {
                .blockState = BLOCK_LAVA,
                .needsFluidTick = false
            };
        } else {
            uint64_t packedBlockPositions = ptr_shift_global(aquifer_data, aquifer_data.packedBlockPositions);
            uint64_t packedRes[4];
            math_aquifer_refreshDistPosIdx_global(packedBlockPositions, packedRes, aquifer_data, i, j, k);
            return aquifer_applyPost_impl(ctx, density, j, i, k, packedRes);
        }
    }
}

struct vein_type_t {
int32_t ore;
    int32_t rawOreBlock;
    int32_t stone;
    int32_t minY;
    int32_t maxY;
};

const const vein_type_t VEIN_COPPER = {
    .ore = BLOCK_COPPER_ORE,
    .rawOreBlock = BLOCK_RAW_COPPER_BLOCK,
    .stone = BLOCK_GRANITE,
    .minY = 0,
    .maxY = 50
};

const const vein_type_t VEIN_IRON = {
    .ore = BLOCK_DEEPSLATE_IRON_ORE,
    .rawOreBlock = BLOCK_RAW_IRON_BLOCK,
    .stone = BLOCK_TUFF,
    .minY = -60,
    .maxY = -8
};

int32_t ore_vein_sample(const sample_int32_ctx_t ctx) {
    uint64_t params = ctx.rw_data;
    if (!params.offset_oreVeinRandom) return BLOCK_NULL;
    uint64_t veinRandom = ptr_shift_global(ctx.rw_data, params.offset_oreVeinRandom);

    double d = df_binding_vein_toggle(ctx);
    uint64_t veinType = d > 0.0 ? &VEIN_COPPER : &VEIN_IRON;
    double e = fabs(d);
    int32_t j = veinType.maxY - ctx.y;
    int32_t k = ctx.y - veinType.minY;
    if (k >= 0 && j >= 0) {
        int32_t l = min(j, k);
        double f = math_clampedMap(double(l), 0.0, 20.0, -0.2, 0.0);
        if (e + f < 0.4F) {
            return BLOCK_NULL;
        } else {
            random_state_t randomState = *veinRandom;
            random_state_split_coords(&randomState, ctx.x, ctx.y, ctx.z);
            if (random_state_nextFloat(&randomState) > 0.7F) {
                return BLOCK_NULL;
            } else if (df_binding_vein_ridged(ctx) >= 0.0) {
                return BLOCK_NULL;
            } else {
                double g = math_clampedMap(e, 0.4F, 0.6F, 0.1F, 0.3F);
                if (double(random_state_nextFloat)(&randomState) < g && df_binding_vein_gap(ctx) > -0.3F) {
                    return random_state_nextFloat(&randomState) < 0.02F ? veinType.rawOreBlock : veinType.ore;
                } else {
                    return veinType.stone;
                }
            }
        }
    } else {
        return BLOCK_NULL;
    }
}

#ifdef DF_COMPILE_NOISE_KERNEL
// res_blocks: [relY][relZ][relX], sign bit incdicate needsFluidTick
)
kernel void df_noise_kernel(uint64_t const_data, uint64_t rw_data, uint64_t res_blocks,
                            const int32_t chunkX, const int32_t chunkZ) {
    if (!const_data || !rw_data || !res_blocks) {
        #ifdef DEBUG
        printf("trap: !const_data || !rw_data || !res_blocks\n const_data=%p rw_data=%p res_blocks=%p\n", const_data, rw_data, res_blocks);
        #endif
        __builtin_trap();
        __builtin_unreachable();
        return;
    }

    uint64_t params = rw_data;
    const int32_t sizeX = get_global_size(0);
    const int32_t sizeY = get_global_size(2);
    const int32_t sizeZ = get_global_size(1);
    const int32_t relX = get_global_id(0);
    const int32_t relY = get_global_id(2);
    const int32_t relZ = get_global_id(1);

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
        blockState = params.genConfig_defaultBlock;
    }
    uint32_t idx = ((relY) * sizeX + relZ) * sizeZ + relX;
    res_blocks[idx] = (uint8_t(blockState)) | (aquifer_res.needsFluidTick ? (1U << 7) : 0);
}
#endif

// branch node: occupies two slots, first with node_minmacs, second with branch_children
// bit 31 set for both slots, bit 30 set for second slot
// leaf node: occupies one slot, with biome ID in state

layout(buffer_reference, scalar) readonly buffer branch_children {
// bit 31: set if branch node, clear if leaf node
    // bit 30: set if is branch node children offsets
    // bit 0-29: biome ID (only valid for leaf nodes)
    uint32_t state;
    union {
        struct {
            uint32_t children_offset[7]; // at most 7 children, 0 is reserved and means no child
};
        struct {
            int16_t maxs[7];
            int16_t mins[7];
        } node_minmaxs;
    };
} biome_search_tree_node_t;

bool 
math_biome_search_tree_is_branch_impl(uint64_t node) {
    return (node.state & (1U << 31)) != 0;
}

bool 
math_biome_search_tree_is_branch_children_impl(uint64_t node) {
    return (node.state & (1U << 30)) != 0;
}

void
math_biome_search_tree_validate_node_impl(uint64_t node) {
    if (!math_biome_search_tree_is_branch_impl(node) && math_biome_search_tree_is_branch_children_impl(node)) {
        // invalid state
        #ifdef DEBUG
        printf("trap: potential biome search tree corruption (math_biome_search_tree_validate_node_impl, 1)\n");
        #endif
        __builtin_trap();
        __builtin_unreachable();
    }
    if (math_biome_search_tree_is_branch_impl(node)) {
        if (!math_biome_search_tree_is_branch_impl(node + 1) || !math_biome_search_tree_is_branch_children_impl(node + 1)) {
            // branch node must have children offsets in the next slot
            #ifdef DEBUG
            printf("trap: potential biome search tree corruption (math_biome_search_tree_validate_node_impl, 2)\n");
            #endif
            __builtin_trap();
            __builtin_unreachable();
        }
        if (!math_biome_search_tree_is_branch_impl(node + 1) && math_biome_search_tree_is_branch_children_impl(node + 1)) {
            // branch node children offsets must be in a branch node
            #ifdef DEBUG
            printf("trap: potential biome search tree corruption (math_biome_search_tree_validate_node_impl, 3)\n");
            #endif
            __builtin_trap();
            __builtin_unreachable();
        }
    }
}

uint64_t 
math_biome_search_tree_distance_func_impl(uint64_t node,
                                       uint64_t target) {
    if (math_biome_search_tree_is_branch_children_impl(node)) {
        #ifdef DEBUG
        printf("trap: potential biome search tree corruption (math_biome_search_tree_distance_func_impl, 1)\n");
        #endif
        __builtin_trap();
        __builtin_unreachable();
    }

    uint64_t res = 0;

    for (uint32_t i = 0; i < 7; i ++) {
        int64_t l = int32_t(target)[i] - int32_t(node)->node_minmaxs.maxs[i];
        int64_t m = int32_t(node)->node_minmaxs.mins[i] - int32_t(target)[i];
        int64_t dist = l >= 0L ? l : max(m, 0L);
        res += dist * dist;
    }

    return res;
}

struct biome_search_stack_element_t {
uint32_t node;
    uint8_t iter_i;
};

uint32_t 
math_biome_search_tree_calc(uint64_t nodes,
                            uint64_t target,
                            const uint32_t nodes_c) {
    // no recursion allowed, because this needs to be eventually ported to GPU

    if (!math_biome_search_tree_is_branch_impl(nodes + 1)) {
        return nodes[1].state & 0x3FFFFFFF;
    }

    biome_search_stack_element_t working[BIOME_SEARCH_TREE_MAX_DEPTH];
    uint32_t top = 0;
    uint32_t current_optimal_node = 1;
    uint64_t current_optimal_dist = UINT64_MAX;

    working[top ++] = (biome_search_stack_element_t) { .node = 1, .iter_i = 0 };
    math_biome_search_tree_validate_node_impl(nodes + 1);

    loop_start:
    while (top) {
        uint32_t cur_node = working[top - 1].node;
        uint32_t iter_i = working[top - 1].iter_i;
        math_biome_search_tree_validate_node_impl(nodes + cur_node);

        uint32_t child_node;
        if (iter_i >= 7 || !(child_node = ConstUint32Ref(nodes + uint64_t((cur_node + 1) * 64 + 4 + iter_i * 4)).val)) {
            // no more children, pop the stack
            top --;
            continue;
        }

        // bump iter index for the current node
        working[top - 1].iter_i ++;

        math_biome_search_tree_validate_node_impl(nodes + child_node);

        uint64_t d = math_biome_search_tree_distance_func_impl(nodes + child_node, target);

        if (d >= current_optimal_dist) {
            // this child cannot be better than the current optimal, skip it
            continue;
        }

        if (math_biome_search_tree_is_branch_impl(nodes + child_node)) {
            // this is a branch node, push it to the stack
            working[top ++] = (biome_search_stack_element_t) { .node = child_node, .iter_i = 0 };
            if (top >= BIOME_SEARCH_TREE_MAX_DEPTH) {
                // stack overflow, this should never happen
                #ifdef DEBUG
                printf("trap: biome search stack overflow: top >= BIOME_SEARCH_TREE_MAX_DEPTH\n BIOME_SEARCH_TREE_MAX_DEPTH=%u\n", BIOME_SEARCH_TREE_MAX_DEPTH);
                #endif
                __builtin_trap();
                __builtin_unreachable();
            }
        } else {
            current_optimal_dist = d;
            current_optimal_node = child_node;
        }
    }

    return nodes[current_optimal_node].state & 0x3FFFFFFF;
}

extern const uint32_t biome_multinoise_tree_offset;
extern const uint32_t biome_multinoise_tree_nodes_c;

#ifdef DF_COMPILE_BIOME_MULTINOISE_KERNEL
// res_blocks: [relY][relZ][relX]
)
kernel void df_biome_multinoise_kernel(uint64_t const_data, uint64_t rw_data,
                                       uint64_t res_biomes,
                                       const int32_t startBiomeX, const int32_t startBiomeZ, const int32_t startBiomeY,
                                       const uint32_t sizeX, const uint32_t sizeZ, const uint32_t sizeY) {
    if (!biome_multinoise_tree_offset) {
        #ifdef DEBUG
        printf("trap: no multinoise configured\n");
        #endif
        __builtin_trap();
        __builtin_unreachable();
    }

    if (!const_data || !res_biomes || !rw_data) {
        #ifdef DEBUG
        printf("trap: !const_data || !res_biomes || !rw_data\n const_data=%p res_biomes=%p rw_data=%p\n", const_data, res_biomes, rw_data);
        #endif
        __builtin_trap();
        __builtin_unreachable();
        return;
    }

    const uint32_t relX = get_global_id(0);
    const uint32_t relZ = get_global_id(1);
    const uint32_t relY = get_global_id(2);

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

    const int16_t target[7] = int16_t[7](
        convert_short_sat(float(temperature) * 10000.0F),
        convert_short_sat(float(vegetation) * 10000.0F),
        convert_short_sat(float(continents) * 10000.0F),
        convert_short_sat(float(erosion) * 10000.0F),
        convert_short_sat(float(depth) * 10000.0F),
        convert_short_sat(float(ridges) * 10000.0F),
        int16_t(0)
    );

    uint64_t root_node = ptr_shift_global(const_data, biome_multinoise_tree_offset);

    const uint32_t result_biome = math_biome_search_tree_calc(root_node, target, biome_multinoise_tree_nodes_c);

    uint32_t idx = ((relY) * sizeX + relZ) * sizeZ + relX;
    res_biomes[idx] = result_biome;
}
#endif
