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

import com.ishland.c2me.opts.accel.vulkan.common.compiler.VulkanGen;
import com.ishland.c2me.opts.dfc.common.ast.misc.ConstantNode;
import com.ishland.c2me.opts.dfc.common.gen.vulkan.VulkanCEmitter;
import com.ishland.c2me.opts.dfc.common.gen.vulkan.VulkanGenFunctionContext;

public class ConstantNodeVulkanEmitter implements VulkanCEmitter<ConstantNode> {
    public static final ConstantNodeVulkanEmitter INSTANCE = new ConstantNodeVulkanEmitter();

    private ConstantNodeVulkanEmitter() {
    }

    @Override
    public String doGen(ConstantNode node, VulkanGenFunctionContext context, String storeTo) {
        return storeTo + " = " + VulkanGen.literal(node.getValue()) + ";";
    }
}
