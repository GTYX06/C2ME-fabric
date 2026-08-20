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

package com.ishland.c2me.opts.accel.cuda.common.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class CUDAStructs {

    public static final int SIZEOF_WORLDGEN_PARAMS = 96;
    public static final int SIZEOF_AQUIFER_FLUIDLEVEL = 8;
    public static final int SIZEOF_AQUIFER_DATA = 40;
    public static final int SIZEOF_RANDOM_STATE = 24;
    public static final int SIZEOF_BIOME_SEARCH_TREE_NODE = 32;

    public static final long RANDOM_Checked = 0;
    public static final long RANDOM_Xoroshiro128PlusPlus = 1;

    public static final int BLOCK_NULL = 0;
    public static final int BLOCK_AIR = 1;
    public static final int BLOCK_DEFAULT_BLOCK = 2;
    public static final int BLOCK_WATER = 3;
    public static final int BLOCK_LAVA = 4;
    public static final int BLOCK_COPPER_ORE = 5;
    public static final int BLOCK_RAW_COPPER_BLOCK = 6;
    public static final int BLOCK_GRANITE = 7;
    public static final int BLOCK_DEEPSLATE_IRON_ORE = 8;
    public static final int BLOCK_RAW_IRON_BLOCK = 9;
    public static final int BLOCK_TUFF = 10;

    public static void writeWorldgenParams(
            ByteBuffer buf,
            int startBiomeX, int startBiomeZ, int sizeBiomeX, int sizeBiomeZ,
            int startCellX, int startCellY, int startCellZ, int sizeCellX, int sizeCellY, int sizeCellZ,
            int estimateSurfaceHeight_startBiomeX, int estimateSurfaceHeight_startBiomeZ,
            int estimateSurfaceHeight_sizeBiomeX, int estimateSurfaceHeight_sizeBiomeZ,
            int cache2d_startX, int cache2d_startZ, int cache2d_sizeX, int cache2d_sizeZ,
            int offset_estimateSurfaceHeight, int genConfig_defaultBlock, int genConfig_defaultFluid,
            int offset_aquifer, int offset_fluidLevelSampler, int offset_oreVeinRandom
    ) {
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(startBiomeX);
        buf.putInt(startBiomeZ);
        buf.putInt(sizeBiomeX);
        buf.putInt(sizeBiomeZ);
        buf.putInt(startCellX);
        buf.putInt(startCellY);
        buf.putInt(startCellZ);
        buf.putInt(sizeCellX);
        buf.putInt(sizeCellY);
        buf.putInt(sizeCellZ);
        buf.putInt(estimateSurfaceHeight_startBiomeX);
        buf.putInt(estimateSurfaceHeight_startBiomeZ);
        buf.putInt(estimateSurfaceHeight_sizeBiomeX);
        buf.putInt(estimateSurfaceHeight_sizeBiomeZ);
        buf.putInt(cache2d_startX);
        buf.putInt(cache2d_startZ);
        buf.putInt(cache2d_sizeX);
        buf.putInt(cache2d_sizeZ);
        buf.putInt(offset_estimateSurfaceHeight);
        buf.putInt(genConfig_defaultBlock);
        buf.putInt(genConfig_defaultFluid);
        buf.putInt(offset_aquifer);
        buf.putInt(offset_fluidLevelSampler);
        buf.putInt(offset_oreVeinRandom);
    }
}
