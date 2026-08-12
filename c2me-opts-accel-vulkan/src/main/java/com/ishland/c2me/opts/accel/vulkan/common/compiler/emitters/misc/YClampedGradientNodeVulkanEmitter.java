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

import com.ishland.c2me.opts.dfc.common.ast.misc.YClampedGradientNode;
import com.ishland.c2me.opts.dfc.common.gen.vulkan.VulkanCEmitter;
import com.ishland.c2me.opts.dfc.common.gen.vulkan.VulkanGenFunctionContext;

import static com.ishland.c2me.opts.accel.vulkan.common.compiler.VulkanGen.literal;

public class YClampedGradientNodeVulkanEmitter implements VulkanCEmitter<YClampedGradientNode> {
    public static final YClampedGradientNodeVulkanEmitter INSTANCE = new YClampedGradientNodeVulkanEmitter();

    private YClampedGradientNodeVulkanEmitter() {
    }

    @Override
    public String doGen(YClampedGradientNode node, VulkanGenFunctionContext context, String storeTo) {
        return storeTo + " = math_clampedMap((double) ctx.y, " + literal(node.fromY) + ", " + literal(node.toY) + ", " + literal(node.fromValue) + ", " + literal(node.toValue) + ");\n";
    }
}
