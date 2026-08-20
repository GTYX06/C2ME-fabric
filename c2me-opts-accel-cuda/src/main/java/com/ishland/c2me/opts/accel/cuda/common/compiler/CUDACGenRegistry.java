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

package com.ishland.c2me.opts.accel.cuda.common.compiler;

import com.ishland.c2me.opts.accel.cuda.common.compiler.emitters.BinaryNodeCUDACCEmitters;
import com.ishland.c2me.opts.accel.cuda.common.compiler.emitters.UnaryNodeCUDACCEmitters;
import com.ishland.c2me.opts.accel.cuda.common.compiler.emitters.conversion.ToF32NodeCUDACCEmitter;
import com.ishland.c2me.opts.accel.cuda.common.compiler.emitters.conversion.ToF64NodeCUDACCEmitter;
import com.ishland.c2me.opts.accel.cuda.common.compiler.emitters.misc.BeardifierNodeCUDACCEmitter;
import com.ishland.c2me.opts.accel.cuda.common.compiler.emitters.misc.CacheLikeNodeCUDACCEmitter;
import com.ishland.c2me.opts.accel.cuda.common.compiler.emitters.misc.ConstantF32NodeCUDACCEmitter;
import com.ishland.c2me.opts.accel.cuda.common.compiler.emitters.misc.ConstantNodeCUDACCEmitter;
import com.ishland.c2me.opts.accel.cuda.common.compiler.emitters.misc.CoordinateNodeCUDACCEmitter;
import com.ishland.c2me.opts.accel.cuda.common.compiler.emitters.misc.EndIslandsNodeCUDACCEmitter;
import com.ishland.c2me.opts.accel.cuda.common.compiler.emitters.misc.FindTopSurfaceNodeCUDACCEmitter;
import com.ishland.c2me.opts.accel.cuda.common.compiler.emitters.misc.GenericShiftedNoiseNodeCUDACCEmitter;
import com.ishland.c2me.opts.accel.cuda.common.compiler.emitters.misc.InterpolatedNoiseSamplerNodeCUDACCEmitter;
import com.ishland.c2me.opts.accel.cuda.common.compiler.emitters.misc.IntervalSelectNodeCUDACCEmitter;
import com.ishland.c2me.opts.accel.cuda.common.compiler.emitters.misc.Multi2SingleNodeCUDACCEmitter;
import com.ishland.c2me.opts.accel.cuda.common.compiler.emitters.misc.RangeChoiceNodeCUDACCEmitter;
import com.ishland.c2me.opts.accel.cuda.common.compiler.emitters.misc.RootNodeCUDACCEmitter;
import com.ishland.c2me.opts.accel.cuda.common.compiler.emitters.misc.SplineNormalNodeCUDACCEmitter;
import com.ishland.c2me.opts.accel.cuda.common.compiler.emitters.misc.YClampedGradientNodeCUDACCEmitter;
import com.ishland.c2me.opts.dfc.common.ast.binary.AddNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.MaxNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.MinNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.MulNode;
import com.ishland.c2me.opts.dfc.common.ast.conversion.ToF32Node;
import com.ishland.c2me.opts.dfc.common.ast.conversion.ToF64Node;
import com.ishland.c2me.opts.dfc.common.ast.misc.BeardifierNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.CacheLikeNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.ConstantF32Node;
import com.ishland.c2me.opts.dfc.common.ast.misc.ConstantNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.CoordinateNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.EndIslandsNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.FindTopSurfaceNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.InterpolatedNoiseSamplerNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.IntervalSelectNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.Multi2SingleNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.RangeChoiceNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.RootNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.YClampedGradientNode;
import com.ishland.c2me.opts.dfc.common.ast.noise.GenericShiftedNoiseNode;
import com.ishland.c2me.opts.dfc.common.ast.spline.SplineNormalNode;
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
import com.ishland.c2me.opts.dfc.common.gen.cuda.CUDACGenData;

public class CUDACGenRegistry {

    public static void init() {
        CUDACGenData.REGISTRY.registerExactMatch(AbsNode.class, new UnaryNodeCUDACCEmitters.Abs());
        CUDACGenData.REGISTRY.registerExactMatch(CeilNode.class, new UnaryNodeCUDACCEmitters.Ceil());
        CUDACGenData.REGISTRY.registerExactMatch(FloorNode.class, new UnaryNodeCUDACCEmitters.Floor());
        CUDACGenData.REGISTRY.registerExactMatch(SinNode.class, new UnaryNodeCUDACCEmitters.Sin());
        CUDACGenData.REGISTRY.registerExactMatch(CosNode.class, new UnaryNodeCUDACCEmitters.Cos());
        CUDACGenData.REGISTRY.registerExactMatch(SqrtNode.class, new UnaryNodeCUDACCEmitters.Sqrt());
        CUDACGenData.REGISTRY.registerExactMatch(SquareNode.class, new UnaryNodeCUDACCEmitters.Square());
        CUDACGenData.REGISTRY.registerExactMatch(CubeNode.class, new UnaryNodeCUDACCEmitters.Cube());
        CUDACGenData.REGISTRY.registerExactMatch(SqueezeNode.class, new UnaryNodeCUDACCEmitters.Squeeze());
        CUDACGenData.REGISTRY.registerExactMatch(NegMulNode.class, new UnaryNodeCUDACCEmitters.NegMul());

        CUDACGenData.REGISTRY.registerExactMatch(AddNode.class, new BinaryNodeCUDACCEmitters.Add());
        CUDACGenData.REGISTRY.registerExactMatch(MulNode.class, new BinaryNodeCUDACCEmitters.Mul());
        CUDACGenData.REGISTRY.registerExactMatch(MinNode.class, new BinaryNodeCUDACCEmitters.Min());
        CUDACGenData.REGISTRY.registerExactMatch(MaxNode.class, new BinaryNodeCUDACCEmitters.Max());
        CUDACGenData.REGISTRY.registerExactMatch(com.ishland.c2me.opts.dfc.common.ast.binary.MinShortNode.class, new BinaryNodeCUDACCEmitters.MinShort());
        CUDACGenData.REGISTRY.registerExactMatch(com.ishland.c2me.opts.dfc.common.ast.binary.MaxShortNode.class, new BinaryNodeCUDACCEmitters.MaxShort());
        CUDACGenData.REGISTRY.registerExactMatch(com.ishland.c2me.opts.dfc.common.ast.binary.DivNode.class, new BinaryNodeCUDACCEmitters.Div());

        CUDACGenData.REGISTRY.registerExactMatch(ToF32Node.class, new ToF32NodeCUDACCEmitter());
        CUDACGenData.REGISTRY.registerExactMatch(ToF64Node.class, new ToF64NodeCUDACCEmitter());

        CUDACGenData.REGISTRY.registerExactMatch(ConstantNode.class, new ConstantNodeCUDACCEmitter());
        CUDACGenData.REGISTRY.registerExactMatch(ConstantF32Node.class, new ConstantF32NodeCUDACCEmitter());
        CUDACGenData.REGISTRY.registerExactMatch(CoordinateNode.class, new CoordinateNodeCUDACCEmitter());
        CUDACGenData.REGISTRY.registerExactMatch(CacheLikeNode.class, new CacheLikeNodeCUDACCEmitter());
        CUDACGenData.REGISTRY.registerExactMatch(IntervalSelectNode.class, new IntervalSelectNodeCUDACCEmitter());
        CUDACGenData.REGISTRY.registerExactMatch(RangeChoiceNode.class, new RangeChoiceNodeCUDACCEmitter());
        CUDACGenData.REGISTRY.registerExactMatch(RootNode.class, new RootNodeCUDACCEmitter());
        CUDACGenData.REGISTRY.registerExactMatch(YClampedGradientNode.class, new YClampedGradientNodeCUDACCEmitter());
        CUDACGenData.REGISTRY.registerExactMatch(Multi2SingleNode.class, new Multi2SingleNodeCUDACCEmitter());

        CUDACGenData.REGISTRY.registerExactMatch(BeardifierNode.class, new BeardifierNodeCUDACCEmitter());
        CUDACGenData.REGISTRY.registerExactMatch(EndIslandsNode.class, new EndIslandsNodeCUDACCEmitter());
        CUDACGenData.REGISTRY.registerExactMatch(FindTopSurfaceNode.class, new FindTopSurfaceNodeCUDACCEmitter());
        CUDACGenData.REGISTRY.registerExactMatch(GenericShiftedNoiseNode.class, new GenericShiftedNoiseNodeCUDACCEmitter());
        CUDACGenData.REGISTRY.registerExactMatch(InterpolatedNoiseSamplerNode.class, new InterpolatedNoiseSamplerNodeCUDACCEmitter());
        CUDACGenData.REGISTRY.registerExactMatch(SplineNormalNode.class, new SplineNormalNodeCUDACCEmitter());
        CUDACGenData.REGISTRY.registerExactMatch(com.ishland.c2me.opts.dfc.common.ast.misc.DelegateNode.class, new com.ishland.c2me.opts.accel.cuda.common.compiler.emitters.misc.DelegateNodeCUDACCEmitter());

        CUDACGenData.REGISTRY.registerExactMatch(com.ishland.c2me.opts.dfc.common.ast.integration.lithostitched.misc.MixNode.class, new com.ishland.c2me.opts.accel.cuda.common.compiler.emitters.misc.MixNodeCUDACCEmitter());
        CUDACGenData.REGISTRY.registerExactMatch(com.ishland.c2me.opts.dfc.common.ast.integration.lithostitched.misc.SelectNode.class, new com.ishland.c2me.opts.accel.cuda.common.compiler.emitters.misc.SelectNodeCUDACCEmitter());
        CUDACGenData.REGISTRY.registerExactMatch(com.ishland.c2me.opts.dfc.common.ast.integration.lithostitched.misc.RepositionNode.class, new com.ishland.c2me.opts.accel.cuda.common.compiler.emitters.misc.RepositionNodeCUDACCEmitter());
        CUDACGenData.REGISTRY.registerExactMatch(com.ishland.c2me.opts.dfc.common.ast.integration.lithostitched.misc.GenericFastNoiseNode.class, new com.ishland.c2me.opts.accel.cuda.common.compiler.emitters.misc.GenericFastNoiseNodeCUDACCEmitter());
    }
}
