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

package com.ishland.c2me.opts.accel.vulkan.common.compiler.emitters.misc;

import com.ishland.c2me.opts.dfc.common.ast.misc.CoordinateNode;
import com.ishland.c2me.opts.dfc.common.gen.vulkan.VulkanCEmitter;
import com.ishland.c2me.opts.dfc.common.gen.vulkan.VulkanGenFunctionContext;

public class CoordinateNodeVulkanEmitter implements VulkanCEmitter<CoordinateNode> {
    public static final CoordinateNodeVulkanEmitter INSTANCE = new CoordinateNodeVulkanEmitter();

    private CoordinateNodeVulkanEmitter() {
    }

    @Override
    public String doGen(CoordinateNode node, VulkanGenFunctionContext context, String storeTo) {
        return switch (node.axis) {
            case X -> storeTo + " = (double) ctx.x;\n";
            case Y -> storeTo + " = (double) ctx.y;\n";
            case Z -> storeTo + " = (double) ctx.z;\n";
        };
    }
}
