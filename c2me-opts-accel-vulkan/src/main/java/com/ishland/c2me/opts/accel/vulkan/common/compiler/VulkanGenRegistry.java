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

package com.ishland.c2me.opts.accel.vulkan.common.compiler;

import com.ishland.c2me.opts.accel.vulkan.common.compiler.emitters.BinaryNodeVulkanEmitters;
import com.ishland.c2me.opts.accel.vulkan.common.compiler.emitters.UnaryNodeVulkanEmitters;
import com.ishland.c2me.opts.accel.vulkan.common.compiler.emitters.conversion.ToF32NodeVulkanEmitter;
import com.ishland.c2me.opts.accel.vulkan.common.compiler.emitters.conversion.ToF64NodeVulkanEmitter;
import com.ishland.c2me.opts.accel.vulkan.common.compiler.emitters.misc.BeardifierNodeVulkanEmitter;
import com.ishland.c2me.opts.accel.vulkan.common.compiler.emitters.misc.CacheLikeNodeVulkanEmitter;
import com.ishland.c2me.opts.accel.vulkan.common.compiler.emitters.misc.ConstantF32NodeVulkanEmitter;
import com.ishland.c2me.opts.accel.vulkan.common.compiler.emitters.misc.ConstantNodeVulkanEmitter;
import com.ishland.c2me.opts.accel.vulkan.common.compiler.emitters.misc.CoordinateNodeVulkanEmitter;
import com.ishland.c2me.opts.accel.vulkan.common.compiler.emitters.misc.EndIslandsNodeVulkanEmitter;
import com.ishland.c2me.opts.accel.vulkan.common.compiler.emitters.misc.FindTopSurfaceNodeVulkanEmitter;
import com.ishland.c2me.opts.accel.vulkan.common.compiler.emitters.misc.GenericShiftedNoiseNodeVulkanEmitter;
import com.ishland.c2me.opts.accel.vulkan.common.compiler.emitters.misc.InterpolatedNoiseSamplerNodeVulkanEmitter;
import com.ishland.c2me.opts.accel.vulkan.common.compiler.emitters.misc.IntervalSelectNodeVulkanEmitter;
import com.ishland.c2me.opts.accel.vulkan.common.compiler.emitters.misc.Multi2SingleNodeVulkanEmitter;
import com.ishland.c2me.opts.accel.vulkan.common.compiler.emitters.misc.RangeChoiceNodeVulkanEmitter;
import com.ishland.c2me.opts.accel.vulkan.common.compiler.emitters.misc.RootNodeVulkanEmitter;
import com.ishland.c2me.opts.accel.vulkan.common.compiler.emitters.misc.SplineNormalNodeVulkanEmitter;
import com.ishland.c2me.opts.accel.vulkan.common.compiler.emitters.misc.YClampedGradientNodeVulkanEmitter;
import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.conversion.ToF32Node;
import com.ishland.c2me.opts.dfc.common.ast.conversion.ToF64Node;
import com.ishland.c2me.opts.dfc.common.ast.misc.BeardifierNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.CacheLikeNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.ConstantF32Node;
import com.ishland.c2me.opts.dfc.common.ast.misc.ConstantNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.CoordinateNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.DelegateNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.EndIslandsNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.FindTopSurfaceNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.InterpolatedNoiseSamplerNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.IntervalSelectNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.Multi2SingleNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.RangeChoiceNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.RootNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.YClampedGradientNode;
import com.ishland.c2me.opts.dfc.common.ast.noise.GenericShiftedNoiseNode;
import com.ishland.c2me.opts.dfc.common.ast.spline.SplineAstNode;
import com.ishland.c2me.opts.dfc.common.ast.spline.SplineNormalNode;
import com.ishland.c2me.opts.dfc.common.gen.vulkan.VulkanCEmitter;
import com.ishland.c2me.opts.dfc.common.gen.vulkan.VulkanGenData;
import com.ishland.c2me.opts.dfc.common.gen.vulkan.VulkanGenFunctionContext;

public class VulkanGenRegistry {

    static {
        BinaryNodeVulkanEmitters.register(VulkanGenData.REGISTRY);
        UnaryNodeVulkanEmitters.register(VulkanGenData.REGISTRY);

        VulkanGenData.REGISTRY.registerExactMatch(ToF32Node.class, ToF32NodeVulkanEmitter.INSTANCE);
        VulkanGenData.REGISTRY.registerExactMatch(ToF64Node.class, ToF64NodeVulkanEmitter.INSTANCE);

        VulkanGenData.REGISTRY.registerExactMatch(BeardifierNode.class, BeardifierNodeVulkanEmitter.INSTANCE);
        VulkanGenData.REGISTRY.registerExactMatch(CacheLikeNode.class, CacheLikeNodeVulkanEmitter.INSTANCE);
        VulkanGenData.REGISTRY.registerExactMatch(ConstantNode.class, ConstantNodeVulkanEmitter.INSTANCE);
        VulkanGenData.REGISTRY.registerExactMatch(ConstantF32Node.class, ConstantF32NodeVulkanEmitter.INSTANCE);
        VulkanGenData.REGISTRY.registerExactMatch(CoordinateNode.class, CoordinateNodeVulkanEmitter.INSTANCE);
        VulkanGenData.REGISTRY.registerExactMatch(IntervalSelectNode.class, IntervalSelectNodeVulkanEmitter.INSTANCE);
        VulkanGenData.REGISTRY.registerExactMatch(EndIslandsNode.class, EndIslandsNodeVulkanEmitter.INSTANCE);
        VulkanGenData.REGISTRY.registerExactMatch(FindTopSurfaceNode.class, FindTopSurfaceNodeVulkanEmitter.INSTANCE);
        VulkanGenData.REGISTRY.registerExactMatch(GenericShiftedNoiseNode.class, GenericShiftedNoiseNodeVulkanEmitter.INSTANCE);
        VulkanGenData.REGISTRY.registerExactMatch(InterpolatedNoiseSamplerNode.class, InterpolatedNoiseSamplerNodeVulkanEmitter.INSTANCE);
        VulkanGenData.REGISTRY.registerExactMatch(RangeChoiceNode.class, RangeChoiceNodeVulkanEmitter.INSTANCE);
        VulkanGenData.REGISTRY.registerExactMatch(RootNode.class, RootNodeVulkanEmitter.INSTANCE);
        VulkanGenData.REGISTRY.registerExactMatch(Multi2SingleNode.class, Multi2SingleNodeVulkanEmitter.INSTANCE);
        VulkanGenData.REGISTRY.registerExactMatch(SplineNormalNode.class, SplineNormalNodeVulkanEmitter.INSTANCE);
        VulkanGenData.REGISTRY.registerExactMatch(YClampedGradientNode.class, YClampedGradientNodeVulkanEmitter.INSTANCE);

        VulkanGenData.REGISTRY.registerExactMatch(DelegateNode.class, (VulkanCEmitter<DelegateNode>) (node, context, storeTo) -> {
            throw new UnsupportedOperationException(String.format("Unsupported density function type: %s", node.getDelegate().getClass()));
        });
    }

    public static <T extends AstNode> String doGen(T node, VulkanGenFunctionContext context, String storeTo) {
        VulkanCEmitter<T> emitter = (VulkanCEmitter<T>) VulkanGenData.REGISTRY.get(node.getClass());
        return emitter.doGen(node, context, storeTo);
    }

}
