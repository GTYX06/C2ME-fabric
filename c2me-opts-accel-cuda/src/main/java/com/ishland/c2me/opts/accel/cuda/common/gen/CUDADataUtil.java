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

package com.ishland.c2me.opts.accel.cuda.common.gen;

import com.ishland.c2me.opts.accel.cuda.common.util.CUDAStructs;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.random.CheckedRandom;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.math.random.Xoroshiro128PlusPlusRandom;
import net.minecraft.world.gen.chunk.AquiferSampler;
import net.minecraft.world.gen.chunk.GenerationShapeConfig;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class CUDADataUtil {

    public static void serializeFluidLevelSampler(ByteBuffer buf, AquiferSampler.FluidLevelSampler fluidLevelSampler, GenerationShapeConfig shape) {
        buf.order(ByteOrder.LITTLE_ENDIAN);
        int minY = shape.minimumY();
        int height = shape.height();
        for (int y = 0; y < height; y++) {
            AquiferSampler.FluidLevel fl = fluidLevelSampler.getFluidLevel(0, minY + y, 0);
            int blockState = getBlockStateId(fl.state());
            buf.putInt(fl.y());
            buf.putInt(blockState);
        }
    }

    public static void serializeRandom(ByteBuffer buf, Random random) {
        buf.order(ByteOrder.LITTLE_ENDIAN);
        if (random instanceof CheckedRandom checked) {
            buf.putLong(CUDAStructs.RANDOM_Checked);
            buf.putLong(0L);
            buf.putLong(0L);
        } else if (random instanceof Xoroshiro128PlusPlusRandom xor) {
            buf.putLong(CUDAStructs.RANDOM_Xoroshiro128PlusPlus);
            buf.putLong(0L);
            buf.putLong(0L);
        } else {
            buf.putLong(CUDAStructs.RANDOM_Checked);
            buf.putLong(0L);
            buf.putLong(0L);
        }
    }

    public static int getBlockStateId(BlockState state) {
        if (state == null || state.isAir()) return CUDAStructs.BLOCK_AIR;
        if (state.getBlock() == Blocks.WATER) return CUDAStructs.BLOCK_WATER;
        if (state.getBlock() == Blocks.LAVA) return CUDAStructs.BLOCK_LAVA;
        if (state.getBlock() == Blocks.COPPER_ORE) return CUDAStructs.BLOCK_COPPER_ORE;
        if (state.getBlock() == Blocks.RAW_COPPER_BLOCK) return CUDAStructs.BLOCK_RAW_COPPER_BLOCK;
        if (state.getBlock() == Blocks.GRANITE) return CUDAStructs.BLOCK_GRANITE;
        if (state.getBlock() == Blocks.DEEPSLATE_IRON_ORE) return CUDAStructs.BLOCK_DEEPSLATE_IRON_ORE;
        if (state.getBlock() == Blocks.RAW_IRON_BLOCK) return CUDAStructs.BLOCK_RAW_IRON_BLOCK;
        if (state.getBlock() == Blocks.TUFF) return CUDAStructs.BLOCK_TUFF;
        return CUDAStructs.BLOCK_DEFAULT_BLOCK;
    }
}
