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

import com.ishland.c2me.opts.dfc.common.ast.noise.GenericShiftedNoiseNode;
import com.ishland.c2me.opts.dfc.common.gen.cuda.CUDACEmitter;
import com.ishland.c2me.opts.dfc.common.gen.cuda.CUDACGenFunctionContext;

public class GenericShiftedNoiseNodeCUDACCEmitter implements CUDACEmitter<GenericShiftedNoiseNode> {

    @Override
    public String doCUDAGen(GenericShiftedNoiseNode node, CUDACGenFunctionContext context, String storeTo) {
        if (storeTo == null) storeTo = context.nextVarName();
        String shiftX = context.getDelegateVar(context.newVarF64(node.inputX));
        String shiftY = context.getDelegateVar(context.newVarF64(node.inputY));
        String shiftZ = context.getDelegateVar(context.newVarF64(node.inputZ));

        int structOffset = context.getGlobalContext().allocGlobalConstDataObject(node.noise);
        context.appendRaw(String.format("const double %s = math_noise_perlin_double_sample((const double_octave_sampler_data_t *)(cdata + %d), %s, %s, %s);\n",
                storeTo, structOffset, shiftX, shiftY, shiftZ));
        return storeTo;
    }
}
