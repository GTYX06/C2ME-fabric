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

import com.ishland.c2me.opts.accel.vulkan.common.Config;
import com.ishland.c2me.opts.accel.vulkan.common.compiler.VulkanGen;
import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.AbstractBinaryNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.AddNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.DivNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.MaxNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.MaxShortNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.MinNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.MinShortNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.MulNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.ConstantNode;
import com.ishland.c2me.opts.dfc.common.gen.CodeGenRegistry;
import com.ishland.c2me.opts.dfc.common.gen.meta.ValuesMethodDefF64;
import com.ishland.c2me.opts.dfc.common.gen.vulkan.VulkanCEmitter;
import com.ishland.c2me.opts.dfc.common.gen.vulkan.VulkanGenFunctionContext;
import com.ishland.c2me.opts.dfc.common.util.TreeUtils;

public class BinaryNodeVulkanEmitters {

    public static abstract class AbstractGenericBinaryNodeVulkanEmitter<T extends AbstractBinaryNode> implements VulkanCEmitter<T> {

        @Override
        public String doGen(T node, VulkanGenFunctionContext context, String storeTo) {
            StringBuilder sb = new StringBuilder();
            ValuesMethodDefF64 leftMethod = context.newVarF64(node.left);
            ValuesMethodDefF64 rightMethod = context.newVarF64(node.right);
            genBody(node, context, storeTo, sb, leftMethod, rightMethod);
            return sb.toString();
        }

        public abstract void genBody(T node, VulkanGenFunctionContext context, String storeTo, StringBuilder sb, ValuesMethodDefF64 left, ValuesMethodDefF64 right);

    }

    public static class AddNodeEmitter extends AbstractGenericBinaryNodeVulkanEmitter<AddNode> {
        public static final AddNodeEmitter INSTANCE = new AddNodeEmitter();

        private AddNodeEmitter() {
        }

        @Override
        public void genBody(AddNode node, VulkanGenFunctionContext context, String storeTo, StringBuilder sb, ValuesMethodDefF64 left, ValuesMethodDefF64 right) {
            sb.append(storeTo).append(" = ").append(context.getDelegateVar(left)).append(" + ").append(context.getDelegateVar(right)).append(";\n");
        }
    }

    public static class DivNodeEmitter extends AbstractGenericBinaryNodeVulkanEmitter<DivNode> {
        public static final DivNodeEmitter INSTANCE = new DivNodeEmitter();

        private DivNodeEmitter() {
        }

        @Override
        public void genBody(DivNode node, VulkanGenFunctionContext context, String storeTo, StringBuilder sb, ValuesMethodDefF64 left, ValuesMethodDefF64 right) {
            sb.append(storeTo).append(" = ").append(context.getDelegateVar(left)).append(" / ").append(context.getDelegateVar(right)).append(";\n");
        }
    }

    public static class MaxNodeEmitter extends AbstractGenericBinaryNodeVulkanEmitter<MaxNode> {
        public static final MaxNodeEmitter INSTANCE = new MaxNodeEmitter();

        private MaxNodeEmitter() {
        }

        @Override
        public void genBody(MaxNode node, VulkanGenFunctionContext context, String storeTo, StringBuilder sb, ValuesMethodDefF64 left, ValuesMethodDefF64 right) {
            sb.append(storeTo).append(" = fmax(").append(context.getDelegateVar(left)).append(", ").append(context.getDelegateVar(right)).append(");\n");
        }
    }

    public static class MaxShortNodeEmitter extends AbstractGenericBinaryNodeVulkanEmitter<MaxShortNode> {
        public static final MaxShortNodeEmitter INSTANCE = new MaxShortNodeEmitter();

        private MaxShortNodeEmitter() {
        }

        @Override
        public String doGen(MaxShortNode node, VulkanGenFunctionContext context, String storeTo) {
            StringBuilder sb = new StringBuilder();
            ValuesMethodDefF64 leftMethod = context.newVarF64(node.left);

            sb.append("const double _left = ").append(context.getDelegateVar(leftMethod)).append(";\n");
            sb.append("if (_left >= ").append(VulkanGen.literal(node.rightMax)).append(") {\n");
            sb.append("    ").append(storeTo).append(" = _left;\n");
            sb.append("} else {\n");

            if (!Config.preserveAllControlFlows) {
                ValuesMethodDefF64 rightMethod;
                if (TreeUtils.hasNonTrivialChildrenUntilBranch(node.right)) {
                    VulkanGenFunctionContext forked = context.fork();
                    rightMethod = forked.newVarF64(node.right);
                    sb.append(forked.getBody().indent(4));
                } else {
                    rightMethod = context.newVarF64(node.right);
                }
                sb.append("    ").append(storeTo).append(" = fmax(_left, ").append(context.getDelegateVar(rightMethod)).append(");\n");
            } else {
                ValuesMethodDefF64 rightMethod = context.getGlobalContext().newMethodF64(node.right, context.getVariant());
                sb.append("    ").append(storeTo).append(" = fmax(_left, ").append(context.getGlobalContext().callDelegate(rightMethod)).append(");\n");
            }

            sb.append("}\n");
            return sb.toString();
        }

        @Override
        public void genBody(MaxShortNode node, VulkanGenFunctionContext context, String storeTo, StringBuilder sb, ValuesMethodDefF64 left, ValuesMethodDefF64 right) {
            throw new UnsupportedOperationException();
        }
    }

    public static class MinNodeEmitter extends AbstractGenericBinaryNodeVulkanEmitter<MinNode> {
        public static final MinNodeEmitter INSTANCE = new MinNodeEmitter();

        private MinNodeEmitter() {
        }

        @Override
        public void genBody(MinNode node, VulkanGenFunctionContext context, String storeTo, StringBuilder sb, ValuesMethodDefF64 left, ValuesMethodDefF64 right) {
            sb.append(storeTo).append(" = fmin(").append(context.getDelegateVar(left)).append(", ").append(context.getDelegateVar(right)).append(");\n");
        }
    }

    public static class MinShortNodeEmitter extends AbstractGenericBinaryNodeVulkanEmitter<MinShortNode> {
        public static final MinShortNodeEmitter INSTANCE = new MinShortNodeEmitter();

        private MinShortNodeEmitter() {
        }

        @Override
        public String doGen(MinShortNode node, VulkanGenFunctionContext context, String storeTo) {
            StringBuilder sb = new StringBuilder();
            ValuesMethodDefF64 leftMethod = context.newVarF64(node.left);

            sb.append("const double _left = ").append(context.getDelegateVar(leftMethod)).append(";\n");
            sb.append("if (_left <= ").append(VulkanGen.literal(node.rightMin)).append(") {\n");
            sb.append("    ").append(storeTo).append(" = _left;\n");
            sb.append("} else {\n");

            if (!Config.preserveAllControlFlows) {
                ValuesMethodDefF64 rightMethod;
                if (TreeUtils.hasNonTrivialChildrenUntilBranch(node.right)) {
                    VulkanGenFunctionContext forked = context.fork();
                    rightMethod = forked.newVarF64(node.right);
                    sb.append(forked.getBody().indent(4));
                } else {
                    rightMethod = context.newVarF64(node.right);
                }
                sb.append("    ").append(storeTo).append(" = fmin(_left, ").append(context.getDelegateVar(rightMethod)).append(");\n");
            } else {
                ValuesMethodDefF64 rightMethod = context.getGlobalContext().newMethodF64(node.right, context.getVariant());
                sb.append("    ").append(storeTo).append(" = fmin(_left, ").append(context.getGlobalContext().callDelegate(rightMethod)).append(");\n");
            }

            sb.append("}\n");
            return sb.toString();
        }

        @Override
        public void genBody(MinShortNode node, VulkanGenFunctionContext context, String storeTo, StringBuilder sb, ValuesMethodDefF64 left, ValuesMethodDefF64 right) {
            throw new UnsupportedOperationException();
        }
    }

    public static class MulNodeEmitter extends AbstractGenericBinaryNodeVulkanEmitter<MulNode> {
        public static final MulNodeEmitter INSTANCE = new MulNodeEmitter();

        private MulNodeEmitter() {
        }

        @Override
        public String doGen(MulNode node, VulkanGenFunctionContext context, String storeTo) {
            StringBuilder sb = new StringBuilder();
            if (node.left instanceof ConstantNode) { // (0.0 * x) should already be optimized out
                ValuesMethodDefF64 leftMethod = context.newVarF64(node.left);
                ValuesMethodDefF64 rightMethod = context.newVarF64(node.right);
                sb.append(storeTo).append(" = ").append(context.getDelegateVar(leftMethod)).append(" * ").append(context.getDelegateVar(rightMethod)).append(";\n");
            } else {
                ValuesMethodDefF64 leftMethod = context.newVarF64(node.left);
                sb.append("const double _left = ").append(context.getDelegateVar(leftMethod)).append(";\n");

                sb.append("if (_left == 0.0) {\n");
                sb.append("    ").append(storeTo).append(" = 0.0;\n");
                sb.append("} else {\n");

                if (!Config.preserveAllControlFlows) {
                    ValuesMethodDefF64 rightMethod;
                    if (TreeUtils.hasNonTrivialChildrenUntilBranch(node.right)) {
                        VulkanGenFunctionContext forked = context.fork();
                        rightMethod = forked.newVarF64(node.right);
                        sb.append(forked.getBody().indent(4));
                    } else {
                        rightMethod = context.newVarF64(node.right);
                    }
                    sb.append("    ").append(storeTo).append(" = _left * ").append(context.getDelegateVar(rightMethod)).append(";\n");
                } else {
                    ValuesMethodDefF64 rightMethod = context.getGlobalContext().newMethodF64(node.right, context.getVariant());
                    sb.append("    ").append(storeTo).append(" = _left * ").append(context.getGlobalContext().callDelegate(rightMethod)).append(";\n");
                }

                sb.append("}\n");
            }
            return sb.toString();
        }

        @Override
        public void genBody(MulNode node, VulkanGenFunctionContext context, String storeTo, StringBuilder sb, ValuesMethodDefF64 left, ValuesMethodDefF64 right) {
            throw new UnsupportedOperationException();
        }
    }

    public static void register(CodeGenRegistry<VulkanCEmitter<? extends AstNode>> registry) {
        registry.registerExactMatch(AddNode.class, AddNodeEmitter.INSTANCE);
        registry.registerExactMatch(DivNode.class, DivNodeEmitter.INSTANCE);
        registry.registerExactMatch(MaxNode.class, MaxNodeEmitter.INSTANCE);
        registry.registerExactMatch(MaxShortNode.class, MaxShortNodeEmitter.INSTANCE);
        registry.registerExactMatch(MinNode.class, MinNodeEmitter.INSTANCE);
        registry.registerExactMatch(MinShortNode.class, MinShortNodeEmitter.INSTANCE);
        registry.registerExactMatch(MulNode.class, MulNodeEmitter.INSTANCE);
    }

}
