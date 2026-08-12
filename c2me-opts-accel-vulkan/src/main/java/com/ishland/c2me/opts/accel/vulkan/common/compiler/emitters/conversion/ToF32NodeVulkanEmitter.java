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

package com.ishland.c2me.opts.accel.vulkan.common.compiler.emitters.conversion;

import com.ishland.c2me.opts.dfc.common.ast.conversion.ToF32Node;
import com.ishland.c2me.opts.dfc.common.gen.meta.ValuesMethodDef;
import com.ishland.c2me.opts.dfc.common.gen.vulkan.VulkanCEmitter;
import com.ishland.c2me.opts.dfc.common.gen.vulkan.VulkanGenFunctionContext;

public class ToF32NodeVulkanEmitter implements VulkanCEmitter<ToF32Node> {
    public static final ToF32NodeVulkanEmitter INSTANCE = new ToF32NodeVulkanEmitter();

    private ToF32NodeVulkanEmitter() {
    }

    @Override
    public String doGen(ToF32Node node, VulkanGenFunctionContext context, String storeTo) {
        ValuesMethodDef next = context.newVar(node.next);
        return storeTo + " = (float) " + context.getDelegateVar(next, next.returnType()) + ";\n";
    }
}
