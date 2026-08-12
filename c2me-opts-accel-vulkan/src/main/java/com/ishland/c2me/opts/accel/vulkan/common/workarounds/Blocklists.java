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
