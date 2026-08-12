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

package com.ishland.c2me.opts.accel.vulkan.common.workarounds.intel;

import com.ishland.c2me.opts.accel.vulkan.common.enumeration.VulkanDeviceMetadata;
import io.netty.util.internal.PlatformDependent;

public class IntelWorkarounds {

    public static boolean isUsingIntelOnLinux(VulkanDeviceMetadata metadata) {
        return !PlatformDependent.isWindows() && !PlatformDependent.isOsx() && isIntel(metadata);
    }

    public static boolean isUsingIntelOnWindows(VulkanDeviceMetadata metadata) {
        return PlatformDependent.isWindows() && isIntel(metadata);
    }

    public static boolean isUsingGen9OnWindows(VulkanDeviceMetadata metadata) {
        return false;
    }

    public static boolean isIntel(VulkanDeviceMetadata metadata) {
        return metadata.vendor().toLowerCase().contains("intel") || metadata.vendor().contains("0x8086");
    }
}
