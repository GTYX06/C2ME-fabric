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

import com.ishland.c2me.opts.dfc.common.ast.misc.IntervalSelectNode;
import com.ishland.c2me.opts.dfc.common.gen.cuda.CUDACEmitter;
import com.ishland.c2me.opts.dfc.common.gen.cuda.CUDACGenFunctionContext;

public class IntervalSelectNodeCUDACCEmitter implements CUDACEmitter<IntervalSelectNode> {
    @Override
    public String doCUDAGen(IntervalSelectNode node, CUDACGenFunctionContext context, String storeTo) {
        if (storeTo == null) storeTo = context.nextVarName();
        String input = context.getDelegateVar(context.newVarF64(node.input));

        String[] funcVars = new String[node.functions.length];
        for (int i = 0; i < node.functions.length; i++) {
            funcVars[i] = context.getDelegateVar(context.newVarF64(node.functions[i]));
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("double %s;\n", storeTo));
        for (int i = 0; i < node.thresholds.length; i++) {
            if (i == 0) {
                sb.append(String.format("if (%s < %s) {\n    %s = %s;\n}", input, Double.toString(node.thresholds[i]), storeTo, funcVars[i]));
            } else {
                sb.append(String.format(" else if (%s < %s) {\n    %s = %s;\n}", input, Double.toString(node.thresholds[i]), storeTo, funcVars[i]));
            }
        }
        if (node.thresholds.length > 0) {
            sb.append(String.format(" else {\n    %s = %s;\n}\n", storeTo, funcVars[node.functions.length - 1]));
        } else if (node.functions.length > 0) {
            sb.append(String.format("%s = %s;\n", storeTo, funcVars[0]));
        }

        context.appendRaw(sb.toString());
        return storeTo;
    }
}
