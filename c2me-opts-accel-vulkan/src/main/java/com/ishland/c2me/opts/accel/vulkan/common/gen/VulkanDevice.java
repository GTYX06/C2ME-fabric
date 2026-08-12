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

package com.ishland.c2me.opts.accel.vulkan.common.gen;

import com.ishland.c2me.base.common.scheduler.SingleThreadExecutor;
import com.ishland.c2me.opts.accel.vulkan.common.Config;
import com.ishland.c2me.opts.accel.vulkan.common.enumeration.VulkanDeviceMetadata;
import com.ishland.c2me.opts.accel.vulkan.common.gen.cache.VKBufferCache;
import com.ishland.c2me.opts.accel.vulkan.common.util.VKUtil;
import com.ishland.c2me.opts.accel.vulkan.common.workarounds.Workarounds;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkDeviceCreateInfo;
import org.lwjgl.vulkan.VkDeviceQueueCreateInfo;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures2;
import org.lwjgl.vulkan.VkPhysicalDeviceVulkan12Features;
import org.lwjgl.vulkan.VkQueue;
import org.lwjgl.vulkan.VkQueueFamilyProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.lwjgl.vulkan.VK10.VK_QUEUE_COMPUTE_BIT;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.vkCreateDevice;
import static org.lwjgl.vulkan.VK10.vkDestroyDevice;
import static org.lwjgl.vulkan.VK10.vkGetDeviceQueue;
import static org.lwjgl.vulkan.VK10.vkGetPhysicalDeviceQueueFamilyProperties;
import static org.lwjgl.vulkan.VK11.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FEATURES_2;

public class VulkanDevice implements Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(VulkanDevice.class);
    private static final AtomicInteger DEVICE_COUNTER = new AtomicInteger(0);

    private final VulkanServerGlobalContext globalContext;
    private final VulkanDeviceMetadata metadata;
    private final Set<Workarounds.Reference> workarounds;
    private final String deviceDescription;
    private final AtomicInteger permits;
    private final AtomicBoolean open = new AtomicBoolean(true);
    private final SingleThreadExecutor executor;
    private final VKBufferCache bufferCache;
    private final VKEventCallbackManager eventCallbackManager;

    private VkDevice vkDevice;
    private VkQueue computeQueue;
    private int computeQueueFamilyIndex = -1;

    public VulkanDevice(VulkanServerGlobalContext globalContext, VulkanDeviceMetadata metadata) {
        this.globalContext = Objects.requireNonNull(globalContext);
        this.metadata = Objects.requireNonNull(metadata);
        this.workarounds = Workarounds.getWorkarounds(metadata);
        this.deviceDescription = String.format("Vulkan Device %s (%s)", metadata.name(), metadata.uuid());

        LOGGER.info("Initializing {}", this.deviceDescription);
        this.executor = new SingleThreadExecutor();
        this.executor.setName("c2me-vkdev-%d-%s".formatted(DEVICE_COUNTER.getAndIncrement(), this.deviceDescription.replaceAll("[^a-zA-Z0-9]", "_")));
        this.executor.start();
        this.bufferCache = new VKBufferCache(this.executor);
        this.eventCallbackManager = new VKEventCallbackManager();
        this.permits = new AtomicInteger(Config.maxConcurrentTasksPerDevice);

        initVkDevice();
    }

    private void initVkDevice() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPhysicalDevice physicalDevice = metadata.physicalDevice();

            IntBuffer pCount = stack.mallocInt(1);
            vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, pCount, null);
            int queueCount = pCount.get(0);
            VkQueueFamilyProperties.Buffer queueProps = VkQueueFamilyProperties.calloc(queueCount, stack);
            vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, pCount, queueProps);

            for (int i = 0; i < queueCount; i++) {
                if ((queueProps.get(i).queueFlags() & VK_QUEUE_COMPUTE_BIT) != 0) {
                    this.computeQueueFamilyIndex = i;
                    break;
                }
            }

            if (this.computeQueueFamilyIndex == -1) {
                throw new RuntimeException("No compute queue family found for " + deviceDescription);
            }

            FloatBuffer pPriorities = stack.floats(1.0f);
            VkDeviceQueueCreateInfo.Buffer queueCreateInfo = VkDeviceQueueCreateInfo.calloc(1, stack)
                    .sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                    .queueFamilyIndex(computeQueueFamilyIndex)
                    .pQueuePriorities(pPriorities);

            VkPhysicalDeviceVulkan12Features vulkan12Features = VkPhysicalDeviceVulkan12Features.calloc(stack)
                    .sType(VK12.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_2_FEATURES)
                    .timelineSemaphore(true)
                    .bufferDeviceAddress(true)
                    .scalarBlockLayout(true);

            VkPhysicalDeviceFeatures2 features2 = VkPhysicalDeviceFeatures2.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FEATURES_2)
                    .pNext(vulkan12Features);

            VkDeviceCreateInfo createInfo = VkDeviceCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
                    .pNext(features2)
                    .pQueueCreateInfos(queueCreateInfo);

            PointerBuffer pDevice = stack.mallocPointer(1);
            int err = vkCreateDevice(physicalDevice, createInfo, null, pDevice);
            VKUtil.checkVkResult(err);

            this.vkDevice = new VkDevice(pDevice.get(0), physicalDevice, createInfo);

            PointerBuffer pQueue = stack.mallocPointer(1);
            vkGetDeviceQueue(vkDevice, computeQueueFamilyIndex, 0, pQueue);
            this.computeQueue = new VkQueue(pQueue.get(0), vkDevice);
        }
    }

    public VkDevice getVkDevice() {
        return vkDevice;
    }

    public VkQueue getComputeQueue() {
        return computeQueue;
    }

    public int getComputeQueueFamilyIndex() {
        return computeQueueFamilyIndex;
    }

    public VulkanDeviceMetadata getMetadata() {
        return metadata;
    }

    public Executor getExecutor() {
        return executor;
    }

    public VKBufferCache getBufferCache() {
        return bufferCache;
    }

    public VKEventCallbackManager getEventCallbackManager() {
        return eventCallbackManager;
    }

    public Set<Workarounds.Reference> getWorkarounds() {
        return workarounds;
    }

    public int getPermits() {
        return permits.get();
    }

    @Override
    public String toString() {
        return deviceDescription;
    }

    @Override
    public void close() {
        if (open.compareAndSet(true, false)) {
            LOGGER.info("Closing {}", deviceDescription);
            bufferCache.clearCache();
            if (vkDevice != null) {
                vkDestroyDevice(vkDevice, null);
            }
            executor.shutdown();
            eventCallbackManager.close();
        }
    }
}
