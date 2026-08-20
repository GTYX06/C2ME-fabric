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

package com.ishland.c2me.opts.accel.cuda.common.compiler.emitters;

import com.ishland.c2me.opts.dfc.common.ast.binary.AddNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.MaxNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.MinNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.MulNode;
import com.ishland.c2me.opts.dfc.common.gen.cuda.CUDACEmitter;
import com.ishland.c2me.opts.dfc.common.gen.cuda.CUDACGenFunctionContext;

public class BinaryNodeCUDACCEmitters {

    public static class Add implements CUDACEmitter<AddNode> {
        @Override
        public String doCUDAGen(AddNode node, CUDACGenFunctionContext context, String storeTo) {
            if (storeTo == null) storeTo = context.nextVarName();
            String left = context.getDelegateVar(context.newVarF64(node.left));
            String right = context.getDelegateVar(context.newVarF64(node.right));
            context.appendRaw(String.format("const double %s = %s + %s;\n", storeTo, left, right));
            return storeTo;
        }
    }

    public static class Mul implements CUDACEmitter<MulNode> {
        @Override
        public String doCUDAGen(MulNode node, CUDACGenFunctionContext context, String storeTo) {
            if (storeTo == null) storeTo = context.nextVarName();
            String left = context.getDelegateVar(context.newVarF64(node.left));
            String right = context.getDelegateVar(context.newVarF64(node.right));
            context.appendRaw(String.format("const double %s = %s * %s;\n", storeTo, left, right));
            return storeTo;
        }
    }

    public static class Min implements CUDACEmitter<MinNode> {
        @Override
        public String doCUDAGen(MinNode node, CUDACGenFunctionContext context, String storeTo) {
            if (storeTo == null) storeTo = context.nextVarName();
            String left = context.getDelegateVar(context.newVarF64(node.left));
            String right = context.getDelegateVar(context.newVarF64(node.right));
            context.appendRaw(String.format("const double %s = fmin(%s, %s);\n", storeTo, left, right));
            return storeTo;
        }
    }

    public static class Max implements CUDACEmitter<MaxNode> {
        @Override
        public String doCUDAGen(MaxNode node, CUDACGenFunctionContext context, String storeTo) {
            if (storeTo == null) storeTo = context.nextVarName();
            String left = context.getDelegateVar(context.newVarF64(node.left));
            String right = context.getDelegateVar(context.newVarF64(node.right));
            context.appendRaw(String.format("const double %s = fmax(%s, %s);\n", storeTo, left, right));
            return storeTo;
        }
    }

    public static class MinShort implements CUDACEmitter<com.ishland.c2me.opts.dfc.common.ast.binary.MinShortNode> {
        @Override
        public String doCUDAGen(com.ishland.c2me.opts.dfc.common.ast.binary.MinShortNode node, CUDACGenFunctionContext context, String storeTo) {
            if (storeTo == null) storeTo = context.nextVarName();
            String left = context.getDelegateVar(context.newVarF64(node.left));
            String right = context.getDelegateVar(context.newVarF64(node.right));
            context.appendRaw(String.format("const double %s = fmin(%s, %s);\n", storeTo, left, right));
            return storeTo;
        }
    }

    public static class MaxShort implements CUDACEmitter<com.ishland.c2me.opts.dfc.common.ast.binary.MaxShortNode> {
        @Override
        public String doCUDAGen(com.ishland.c2me.opts.dfc.common.ast.binary.MaxShortNode node, CUDACGenFunctionContext context, String storeTo) {
            if (storeTo == null) storeTo = context.nextVarName();
            String left = context.getDelegateVar(context.newVarF64(node.left));
            String right = context.getDelegateVar(context.newVarF64(node.right));
            context.appendRaw(String.format("const double %s = fmax(%s, %s);\n", storeTo, left, right));
            return storeTo;
        }
    }

    public static class Div implements CUDACEmitter<com.ishland.c2me.opts.dfc.common.ast.binary.DivNode> {
        @Override
        public String doCUDAGen(com.ishland.c2me.opts.dfc.common.ast.binary.DivNode node, CUDACGenFunctionContext context, String storeTo) {
            if (storeTo == null) storeTo = context.nextVarName();
            String left = context.getDelegateVar(context.newVarF64(node.left));
            String right = context.getDelegateVar(context.newVarF64(node.right));
            context.appendRaw(String.format("const double %s = %s / %s;\n", storeTo, left, right));
            return storeTo;
        }
    }
}
