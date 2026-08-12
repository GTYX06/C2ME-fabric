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

import com.ishland.c2me.opts.accel.vulkan.common.Config;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK;
import org.lwjgl.vulkan.VkApplicationInfo;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkInstanceCreateInfo;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures2;
import org.lwjgl.vulkan.VkPhysicalDeviceProperties;
import org.lwjgl.vulkan.VkPhysicalDeviceVulkan12Features;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.lwjgl.vulkan.VK10.VK_MAKE_VERSION;
import static org.lwjgl.vulkan.VK10.VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_APPLICATION_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;
import static org.lwjgl.vulkan.VK10.vkCreateInstance;
import static org.lwjgl.vulkan.VK10.vkEnumeratePhysicalDevices;
import static org.lwjgl.vulkan.VK10.vkGetPhysicalDeviceProperties;
import static org.lwjgl.vulkan.VK11.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FEATURES_2;
import static org.lwjgl.vulkan.VK11.vkGetPhysicalDeviceFeatures2;
import static org.lwjgl.vulkan.VK12.VK_API_VERSION_1_2;
import static org.lwjgl.vulkan.VK12.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_2_FEATURES;

public class VulkanDeviceLocator {

    private static final Logger LOGGER = LoggerFactory.getLogger(VulkanDeviceLocator.class);
    private static VkInstance vkInstance = null;
    private static boolean initialized = false;

    public static synchronized boolean isAvailable() {
        if (initialized) return vkInstance != null;
        initialized = true;
        try {
            try {
                VK.create();
            } catch (IllegalStateException ignored) {
                // Vulkan function provider has already been created
            }
            try (MemoryStack stack = MemoryStack.stackPush()) {
                VkApplicationInfo appInfo = VkApplicationInfo.calloc(stack)
                        .sType(VK_STRUCTURE_TYPE_APPLICATION_INFO)
                        .pApplicationName(stack.UTF8("C2ME Vulkan"))
                        .applicationVersion(VK_MAKE_VERSION(1, 0, 0))
                        .pEngineName(stack.UTF8("C2ME"))
                        .engineVersion(VK_MAKE_VERSION(1, 0, 0))
                        .apiVersion(VK_API_VERSION_1_2);

                VkInstanceCreateInfo createInfo = VkInstanceCreateInfo.calloc(stack)
                        .sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
                        .pApplicationInfo(appInfo);

                PointerBuffer pInstance = stack.mallocPointer(1);
                int err = vkCreateInstance(createInfo, null, pInstance);
                if (err != VK_SUCCESS) {
                    LOGGER.error("Failed to create Vulkan instance, error code: {}", err);
                    return false;
                }
                vkInstance = new VkInstance(pInstance.get(0), createInfo);
                LOGGER.info("Successfully initialized Vulkan 1.2 instance");
                return true;
            }
        } catch (Throwable t) {
            LOGGER.error("Vulkan initialization failed", t);
            return false;
        }
    }

    public static VkInstance getInstance() {
        if (!isAvailable()) {
            throw new IllegalStateException("Vulkan is not available");
        }
        return vkInstance;
    }

    public static List<VulkanDeviceMetadata> enumerateAll() {
        List<VulkanDeviceMetadata> list = new ArrayList<>();
        if (!isAvailable()) return list;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer pCount = stack.mallocInt(1);
            int err = vkEnumeratePhysicalDevices(vkInstance, pCount, null);
            if (err != VK_SUCCESS || pCount.get(0) == 0) {
                return list;
            }

            int count = pCount.get(0);
            PointerBuffer pDevices = stack.mallocPointer(count);
            vkEnumeratePhysicalDevices(vkInstance, pCount, pDevices);

            for (int i = 0; i < count; i++) {
                VkPhysicalDevice physicalDevice = new VkPhysicalDevice(pDevices.get(i), vkInstance);
                VkPhysicalDeviceProperties props = VkPhysicalDeviceProperties.calloc(stack);
                vkGetPhysicalDeviceProperties(physicalDevice, props);

                VkPhysicalDeviceVulkan12Features vulkan12Features = VkPhysicalDeviceVulkan12Features.calloc(stack)
                        .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_2_FEATURES);
                VkPhysicalDeviceFeatures2 features2 = VkPhysicalDeviceFeatures2.calloc(stack)
                        .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FEATURES_2)
                        .pNext(vulkan12Features);

                vkGetPhysicalDeviceFeatures2(physicalDevice, features2);

                String name = props.deviceNameString();
                String vendor = String.format("0x%04x", props.vendorID());
                String driverVersion = String.valueOf(props.driverVersion());
                int deviceType = props.deviceType();
                boolean isDiscrete = deviceType == VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU;
                boolean supportsVk12 = vulkan12Features.timelineSemaphore() && vulkan12Features.bufferDeviceAddress();

                UUID uuid = UUID.nameUUIDFromBytes(name.getBytes());
                list.add(new VulkanDeviceMetadata(
                        physicalDevice,
                        name,
                        vendor,
                        driverVersion,
                        deviceType,
                        uuid,
                        0L,
                        isDiscrete,
                        supportsVk12
                ));
            }
        }
        return list;
    }
}
