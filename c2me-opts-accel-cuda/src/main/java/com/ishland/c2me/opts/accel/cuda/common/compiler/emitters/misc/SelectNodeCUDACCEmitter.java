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

import com.ishland.c2me.opts.dfc.common.ast.integration.lithostitched.misc.SelectNode;
import com.ishland.c2me.opts.dfc.common.gen.cuda.CUDACEmitter;
import com.ishland.c2me.opts.dfc.common.gen.cuda.CUDACGenFunctionContext;

public class SelectNodeCUDACCEmitter implements CUDACEmitter<SelectNode> {

    @Override
    public String doCUDAGen(SelectNode node, CUDACGenFunctionContext context, String storeTo) {
        if (storeTo == null) storeTo = context.nextVarName();
        String input = context.getDelegateVar(context.newVarF64(node.input));
        String fallback = context.getDelegateVar(context.newVarF64(node.fallback));

        context.appendRaw(String.format("double %s = %s;\n", storeTo, fallback));
        for (int i = 0; i < node.functions.length; i++) {
            double min = node.mins[i];
            double max = node.maxs[i];
            String fnVar = context.getDelegateVar(context.newVarF64(node.functions[i]));
            context.appendRaw(String.format("if (%s >= %f && %s <= %f) { %s = %s; }\n", input, min, input, max, storeTo, fnVar));
        }
        return storeTo;
    }
}
