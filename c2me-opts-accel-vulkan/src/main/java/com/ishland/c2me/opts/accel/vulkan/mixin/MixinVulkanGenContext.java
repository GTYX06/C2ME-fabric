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

package com.ishland.c2me.opts.accel.vulkan.mixin;

import com.ishland.c2me.opts.accel.vulkan.ModuleEntryPoint;
import com.ishland.c2me.opts.accel.vulkan.common.gen.VKDataUtil;
import com.ishland.c2me.opts.accel.vulkan.common.compiler.GeneratedVulkanSource;
import com.ishland.c2me.opts.accel.vulkan.common.compiler.VulkanGen;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.io.IOException;
import java.io.InputStream;

@Mixin(VulkanGen.ContextImpl.class)
public class MixinVulkanGenContext {

    @ModifyReturnValue(method = "build", at = @At("RETURN"), remap = false)
    private static GeneratedVulkanSource modifySource(GeneratedVulkanSource original) {
        try (InputStream in = ModuleEntryPoint.class.getClassLoader().getResourceAsStream("glslsources/c2me_vulkan_ext_math.glsl")) {
            if (in == null) throw new NullPointerException("Resource not found");
            String header = new String(in.readAllBytes());
            return new GeneratedVulkanSource(
                    original.getOrdinal(),
                    header + original.getGeneratedSource(),
                    original.getConstData(),
                    VKDataUtil.transformGlobalDynamicDataOffsets(original.getGlobalDynamicDataOffsets()),
                    original.getFlatCachePrefills(),
                    original.getCache2dPrefills(),
                    original.getInterpolatorPrefills(),
                    original.getDefines(),
                    original.getBiomeMappings(),
                    original.getDumpedPath()
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
