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

import com.ishland.c2me.opts.dfc.common.ast.unary.AbsNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.CeilNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.CosNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.CubeNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.FloorNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.NegMulNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.SinNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.SqrtNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.SquareNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.SqueezeNode;
import com.ishland.c2me.opts.dfc.common.gen.cuda.CUDACEmitter;
import com.ishland.c2me.opts.dfc.common.gen.cuda.CUDACGenFunctionContext;

public class UnaryNodeCUDACCEmitters {

    public static class Abs implements CUDACEmitter<AbsNode> {
        @Override
        public String doCUDAGen(AbsNode node, CUDACGenFunctionContext context, String storeTo) {
            if (storeTo == null) storeTo = context.nextVarName();
            String op = context.getDelegateVar(context.newVarF64(node.operand));
            context.appendRaw(String.format("const double %s = fabs(%s);\n", storeTo, op));
            return storeTo;
        }
    }

    public static class Ceil implements CUDACEmitter<CeilNode> {
        @Override
        public String doCUDAGen(CeilNode node, CUDACGenFunctionContext context, String storeTo) {
            if (storeTo == null) storeTo = context.nextVarName();
            String op = context.getDelegateVar(context.newVarF64(node.operand));
            context.appendRaw(String.format("const double %s = ceil(%s);\n", storeTo, op));
            return storeTo;
        }
    }

    public static class Floor implements CUDACEmitter<FloorNode> {
        @Override
        public String doCUDAGen(FloorNode node, CUDACGenFunctionContext context, String storeTo) {
            if (storeTo == null) storeTo = context.nextVarName();
            String op = context.getDelegateVar(context.newVarF64(node.operand));
            context.appendRaw(String.format("const double %s = floor(%s);\n", storeTo, op));
            return storeTo;
        }
    }

    public static class Sin implements CUDACEmitter<SinNode> {
        @Override
        public String doCUDAGen(SinNode node, CUDACGenFunctionContext context, String storeTo) {
            if (storeTo == null) storeTo = context.nextVarName();
            String op = context.getDelegateVar(context.newVarF64(node.operand));
            context.appendRaw(String.format("const double %s = sin(%s);\n", storeTo, op));
            return storeTo;
        }
    }

    public static class Cos implements CUDACEmitter<CosNode> {
        @Override
        public String doCUDAGen(CosNode node, CUDACGenFunctionContext context, String storeTo) {
            if (storeTo == null) storeTo = context.nextVarName();
            String op = context.getDelegateVar(context.newVarF64(node.operand));
            context.appendRaw(String.format("const double %s = cos(%s);\n", storeTo, op));
            return storeTo;
        }
    }

    public static class Sqrt implements CUDACEmitter<SqrtNode> {
        @Override
        public String doCUDAGen(SqrtNode node, CUDACGenFunctionContext context, String storeTo) {
            if (storeTo == null) storeTo = context.nextVarName();
            String op = context.getDelegateVar(context.newVarF64(node.operand));
            context.appendRaw(String.format("const double %s = sqrt(%s);\n", storeTo, op));
            return storeTo;
        }
    }

    public static class Square implements CUDACEmitter<SquareNode> {
        @Override
        public String doCUDAGen(SquareNode node, CUDACGenFunctionContext context, String storeTo) {
            if (storeTo == null) storeTo = context.nextVarName();
            String op = context.getDelegateVar(context.newVarF64(node.operand));
            context.appendRaw(String.format("const double %s = %s * %s;\n", storeTo, op, op));
            return storeTo;
        }
    }

    public static class Cube implements CUDACEmitter<CubeNode> {
        @Override
        public String doCUDAGen(CubeNode node, CUDACGenFunctionContext context, String storeTo) {
            if (storeTo == null) storeTo = context.nextVarName();
            String op = context.getDelegateVar(context.newVarF64(node.operand));
            context.appendRaw(String.format("const double %s = %s * %s * %s;\n", storeTo, op, op, op));
            return storeTo;
        }
    }

    public static class Squeeze implements CUDACEmitter<SqueezeNode> {
        @Override
        public String doCUDAGen(SqueezeNode node, CUDACGenFunctionContext context, String storeTo) {
            if (storeTo == null) storeTo = context.nextVarName();
            String op = context.getDelegateVar(context.newVarF64(node.operand));
            context.appendRaw(String.format("const double %s = math_squeeze(%s);\n", storeTo, op));
            return storeTo;
        }
    }

    public static class NegMul implements CUDACEmitter<NegMulNode> {
        @Override
        public String doCUDAGen(NegMulNode node, CUDACGenFunctionContext context, String storeTo) {
            if (storeTo == null) storeTo = context.nextVarName();
            String op = context.getDelegateVar(context.newVarF64(node.operand));
            context.appendRaw(String.format("const double %s = %s <= 0.0 ? %s * %s : %s;\n", storeTo, op, op, Double.toString(node.negMul), op));
            return storeTo;
        }
    }
}
