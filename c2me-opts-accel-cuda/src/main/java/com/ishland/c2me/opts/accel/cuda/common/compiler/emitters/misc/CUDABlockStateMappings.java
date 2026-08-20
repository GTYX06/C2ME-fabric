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

import com.ishland.c2me.opts.accel.cuda.common.util.CUDAStructs;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;

public class CUDABlockStateMappings {

    public static BlockState[] getDefaultBlockStates(BlockState defaultBlock, BlockState defaultFluid) {
        BlockState[] states = new BlockState[16];
        states[CUDAStructs.BLOCK_NULL] = Blocks.AIR.getDefaultState();
        states[CUDAStructs.BLOCK_AIR] = Blocks.AIR.getDefaultState();
        states[CUDAStructs.BLOCK_DEFAULT_BLOCK] = defaultBlock != null ? defaultBlock : Blocks.STONE.getDefaultState();
        states[CUDAStructs.BLOCK_WATER] = Blocks.WATER.getDefaultState();
        states[CUDAStructs.BLOCK_LAVA] = Blocks.LAVA.getDefaultState();
        states[CUDAStructs.BLOCK_COPPER_ORE] = Blocks.COPPER_ORE.getDefaultState();
        states[CUDAStructs.BLOCK_RAW_COPPER_BLOCK] = Blocks.RAW_COPPER_BLOCK.getDefaultState();
        states[CUDAStructs.BLOCK_GRANITE] = Blocks.GRANITE.getDefaultState();
        states[CUDAStructs.BLOCK_DEEPSLATE_IRON_ORE] = Blocks.DEEPSLATE_IRON_ORE.getDefaultState();
        states[CUDAStructs.BLOCK_RAW_IRON_BLOCK] = Blocks.RAW_IRON_BLOCK.getDefaultState();
        states[CUDAStructs.BLOCK_TUFF] = Blocks.TUFF.getDefaultState();
        return states;
    }
}
