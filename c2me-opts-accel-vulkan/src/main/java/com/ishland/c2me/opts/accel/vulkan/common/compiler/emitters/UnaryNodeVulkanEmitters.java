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

package com.ishland.c2me.opts.accel.vulkan.common.compiler.emitters;

import com.ishland.c2me.opts.accel.vulkan.common.compiler.VulkanGen;
import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.AbsNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.AbstractUnaryNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.CubeNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.NegMulNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.SquareNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.SqueezeNode;
import com.ishland.c2me.opts.dfc.common.gen.CodeGenRegistry;
import com.ishland.c2me.opts.dfc.common.gen.meta.ValuesMethodDefF64;
import com.ishland.c2me.opts.dfc.common.gen.vulkan.VulkanCEmitter;
import com.ishland.c2me.opts.dfc.common.gen.vulkan.VulkanGenFunctionContext;

public class UnaryNodeVulkanEmitters {

    public static abstract class AbstractGenericUnaryNodeVulkanEmitter<T extends AbstractUnaryNode> implements VulkanCEmitter<T> {

        @Override
        public String doGen(T node, VulkanGenFunctionContext context, String storeTo) {
            StringBuilder sb = new StringBuilder();
            ValuesMethodDefF64 operand = context.newVarF64(node.operand);
            genBody(node, context, storeTo, sb, operand);
            return sb.toString();
        }

        protected abstract void genBody(T node, VulkanGenFunctionContext context, String storeTo, StringBuilder sb, ValuesMethodDefF64 operand);

    }

    public static class AbsNodeEmitter extends AbstractGenericUnaryNodeVulkanEmitter<AbsNode> {
        public static final AbsNodeEmitter INSTANCE = new AbsNodeEmitter();

        private AbsNodeEmitter() {
        }

        @Override
        protected void genBody(AbsNode node, VulkanGenFunctionContext context, String storeTo, StringBuilder sb, ValuesMethodDefF64 operand) {
            sb.append(storeTo).append(" = abs(").append(context.getDelegateVar(operand)).append(");\n");
        }
    }

    public static class CubeNodeEmitter extends AbstractGenericUnaryNodeVulkanEmitter<CubeNode> {
        public static final CubeNodeEmitter INSTANCE = new CubeNodeEmitter();

        private CubeNodeEmitter() {
        }

        @Override
        protected void genBody(CubeNode node, VulkanGenFunctionContext context, String storeTo, StringBuilder sb, ValuesMethodDefF64 operand) {
            sb
                    .append("double v = ").append(context.getDelegateVar(operand)).append(";\n")
                    .append(storeTo).append(" = v * v * v;\n");
        }
    }

    public static class NegMulNodeEmitter extends AbstractGenericUnaryNodeVulkanEmitter<NegMulNode> {
        public static final NegMulNodeEmitter INSTANCE = new NegMulNodeEmitter();

        private NegMulNodeEmitter() {
        }

        @Override
        protected void genBody(NegMulNode node, VulkanGenFunctionContext context, String storeTo, StringBuilder sb, ValuesMethodDefF64 operand) {
            sb
                    .append("double v = ").append(context.getDelegateVar(operand)).append(";\n")
                    .append(storeTo).append(" = v > 0.0 ? v : v * ").append(VulkanGen.literal(node.negMul)).append(";\n");
        }
    }

    public static class SquareNodeEmitter extends AbstractGenericUnaryNodeVulkanEmitter<SquareNode> {
        public static final SquareNodeEmitter INSTANCE = new SquareNodeEmitter();

        private SquareNodeEmitter() {
        }

        @Override
        protected void genBody(SquareNode node, VulkanGenFunctionContext context, String storeTo, StringBuilder sb, ValuesMethodDefF64 operand) {
            sb
                    .append("double v = ").append(context.getDelegateVar(operand)).append(";\n")
                    .append(storeTo).append(" = v * v;\n");
        }
    }

    public static class SqueezeNodeEmitter extends AbstractGenericUnaryNodeVulkanEmitter<SqueezeNode> {
        public static final SqueezeNodeEmitter INSTANCE = new SqueezeNodeEmitter();

        private SqueezeNodeEmitter() {
        }

        @Override
        protected void genBody(SqueezeNode node, VulkanGenFunctionContext context, String storeTo, StringBuilder sb, ValuesMethodDefF64 operand) {
            sb
                    .append("double v = clamp(").append(context.getDelegateVar(operand)).append(", -1.0, 1.0);\n")
                    .append(storeTo).append(" = v / 2.0 - v * v * v / 24.0;\n");
        }
    }

    public static void register(CodeGenRegistry<VulkanCEmitter<? extends AstNode>> registry) {
        registry.registerExactMatch(AbsNode.class, AbsNodeEmitter.INSTANCE);
        registry.registerExactMatch(CubeNode.class, CubeNodeEmitter.INSTANCE);
        registry.registerExactMatch(NegMulNode.class, NegMulNodeEmitter.INSTANCE);
        registry.registerExactMatch(SquareNode.class, SquareNodeEmitter.INSTANCE);
        registry.registerExactMatch(SqueezeNode.class, SqueezeNodeEmitter.INSTANCE);
    }

}
