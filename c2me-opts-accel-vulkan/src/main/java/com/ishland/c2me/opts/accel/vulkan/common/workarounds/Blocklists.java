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

package com.ishland.c2me.opts.accel.vulkan.common.workarounds;

import com.ishland.c2me.opts.accel.vulkan.common.enumeration.VulkanDeviceMetadata;
import com.ishland.c2me.opts.accel.vulkan.common.workarounds.amd.AMDBlocklists;
import com.ishland.c2me.opts.accel.vulkan.common.workarounds.intel.IntelBlocklists;
import com.ishland.c2me.opts.accel.vulkan.common.workarounds.mesa.MesaBlocklists;

public class Blocklists {
    public static boolean isBlocked(VulkanDeviceMetadata metadata) {
        return AMDBlocklists.isBlocked(metadata) || IntelBlocklists.isBlocked(metadata) || MesaBlocklists.isBlocked(metadata);
    }
}
