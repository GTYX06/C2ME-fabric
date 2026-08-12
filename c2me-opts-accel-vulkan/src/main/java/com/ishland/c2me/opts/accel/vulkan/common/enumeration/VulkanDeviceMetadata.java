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

package com.ishland.c2me.opts.accel.vulkan.common.enumeration;

import org.lwjgl.vulkan.VkPhysicalDevice;

import java.util.Objects;
import java.util.UUID;

public record VulkanDeviceMetadata(
        VkPhysicalDevice physicalDevice,
        String name,
        String vendor,
        String driverVersion,
        int deviceType,
        UUID uuid,
        long totalMemory,
        boolean isDiscreteGPU,
        boolean supportsVulkan12
) {
    public VulkanDeviceMetadata {
        Objects.requireNonNull(name);
    }
}
