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

package com.ishland.c2me.opts.accel.cuda.common.compiler.emitters.misc;

import com.ishland.c2me.opts.dfc.common.ast.misc.CacheLikeNode;
import com.ishland.c2me.opts.dfc.common.gen.cuda.CUDACEmitter;
import com.ishland.c2me.opts.dfc.common.gen.cuda.CUDACGenFunctionContext;
import net.minecraft.world.gen.densityfunction.DensityFunctionTypes;

public class CacheLikeNodeCUDACCEmitter implements CUDACEmitter<CacheLikeNode> {

    @Override
    public String doCUDAGen(CacheLikeNode node, CUDACGenFunctionContext context, String storeTo) {
        if (storeTo == null) storeTo = context.nextVarName();

        if (!((Object) node.getCacheLike() instanceof DensityFunctionTypes.Wrapping wrapping)) {
            String delegate = context.getDelegateVar(context.newVarF64(node.getDelegate()));
            context.appendRaw(String.format("const double %s = %s;\n", storeTo, delegate));
            return storeTo;
        }

        switch (wrapping.type()) {
            case FLAT_CACHE -> {
                int id = context.getGlobalContext().registerFlatCache(node);
                if (context.getVariant().enableFlatCache) {
                    String fallback = context.getDelegateVar(context.newVarF64(node.getDelegate()));
                    context.appendRaw(String.format("""
                            double %s;
                            if (ctx.sample_flags & MASK_enableFlatCache) {
                                const worldgen_params_t *params = (const worldgen_params_t *) ctx.rw_data;
                                int32_t relBiomeX = math_block2biome(ctx.x) - params->startBiomeX;
                                int32_t relBiomeZ = math_block2biome(ctx.z) - params->startBiomeZ;
                                uint32_t addr = df_address_flatcache_buffer(params, %d, relBiomeX, relBiomeZ);
                                %s = df_data_offset(ctx.rw_data, 0)[addr];
                            } else {
                                %s = %s;
                            }
                            """, storeTo, id, storeTo, storeTo, fallback));
                } else {
                    String delegate = context.getDelegateVar(context.newVarF64(node.getDelegate()));
                    context.appendRaw(String.format("const double %s = %s;\n", storeTo, delegate));
                }
            }
            case CACHE2D -> {
                int id = context.getGlobalContext().registerCache2d(node);
                if (context.getVariant().useCache2D()) {
                    String fallback = context.getDelegateVar(context.newVarF64(node.getDelegate()));
                    context.appendRaw(String.format("""
                            double %s;
                            if (ctx.sample_flags & MASK_enableAllCaches) {
                                const worldgen_params_t *params = (const worldgen_params_t *) ctx.rw_data;
                                int32_t relBlockX = ctx.x - params->cache2d_startX;
                                int32_t relBlockZ = ctx.z - params->cache2d_startZ;
                                uint32_t addr = df_address_cache2d_buffer(params, %d, relBlockX, relBlockZ);
                                %s = df_data_offset(ctx.rw_data, 1)[addr];
                            } else {
                                %s = %s;
                            }
                            """, storeTo, id, storeTo, storeTo, fallback));
                } else {
                    String delegate = context.getDelegateVar(context.newVarF64(node.getDelegate()));
                    context.appendRaw(String.format("const double %s = %s;\n", storeTo, delegate));
                }
            }
            case INTERPOLATED -> {
                int id = context.getGlobalContext().registerInterpolator(node);
                if (context.getVariant().enableAllCache) {
                    String fallback = context.getDelegateVar(context.newVarF64(node.getDelegate()));
                    context.appendRaw(String.format("""
                            double %s;
                            if (ctx.sample_flags & MASK_enableAllCaches) {
                                const worldgen_params_t *params = (const worldgen_params_t *) ctx.rw_data;
                                int32_t hCellBlockCount = genShapeCfg_horizontalCellBlockCount();
                                int32_t vCellBlockCount = genShapeCfg_verticalCellBlockCount();
                                int32_t cellX = math_floorDiv(ctx.x, hCellBlockCount) - params->startCellX;
                                int32_t cellY = math_floorDiv(ctx.y, vCellBlockCount) - params->startCellY;
                                int32_t cellZ = math_floorDiv(ctx.z, hCellBlockCount) - params->startCellZ;

                                double deltaX = ((double) math_floorMod(ctx.x, hCellBlockCount)) / (double) hCellBlockCount;
                                double deltaY = ((double) math_floorMod(ctx.y, vCellBlockCount)) / (double) vCellBlockCount;
                                double deltaZ = ((double) math_floorMod(ctx.z, hCellBlockCount)) / (double) hCellBlockCount;

                                const double *buf = df_data_offset(ctx.rw_data, 2);
                                double x0y0z0 = buf[df_address_interpolator_buffer(params, %d, cellX, cellY, cellZ)];
                                double x1y0z0 = buf[df_address_interpolator_buffer(params, %d, cellX + 1, cellY, cellZ)];
                                double x0y1z0 = buf[df_address_interpolator_buffer(params, %d, cellX, cellY + 1, cellZ)];
                                double x1y1z0 = buf[df_address_interpolator_buffer(params, %d, cellX + 1, cellY + 1, cellZ)];
                                double x0y0z1 = buf[df_address_interpolator_buffer(params, %d, cellX, cellY, cellZ + 1)];
                                double x1y0z1 = buf[df_address_interpolator_buffer(params, %d, cellX + 1, cellY, cellZ + 1)];
                                double x0y1z1 = buf[df_address_interpolator_buffer(params, %d, cellX, cellY + 1, cellZ + 1)];
                                double x1y1z1 = buf[df_address_interpolator_buffer(params, %d, cellX + 1, cellY + 1, cellZ + 1)];

                                %s = math_lerp3(deltaX, deltaY, deltaZ, x0y0z0, x1y0z0, x0y1z0, x1y1z0, x0y0z1, x1y0z1, x0y1z1, x1y1z1);
                            } else {
                                %s = %s;
                            }
                            """, storeTo, id, id, id, id, id, id, id, id, storeTo, storeTo, fallback));
                } else {
                    String delegate = context.getDelegateVar(context.newVarF64(node.getDelegate()));
                    context.appendRaw(String.format("const double %s = %s;\n", storeTo, delegate));
                }
            }
            default -> {
                String delegate = context.getDelegateVar(context.newVarF64(node.getDelegate()));
                context.appendRaw(String.format("const double %s = %s;\n", storeTo, delegate));
            }
        }
        return storeTo;
    }
}
