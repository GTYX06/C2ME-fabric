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

import com.google.common.collect.ImmutableList;
import com.ishland.c2me.opts.accel.vulkan.common.enumeration.VulkanDeviceMetadata;
import com.ishland.c2me.opts.accel.vulkan.common.shader_cache.ShaderCacheManager;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class VulkanServerGlobalContext {

    private final ArrayList<VulkanDevice> openDevices = new ArrayList<>();
    private final AtomicInteger currentDeviceIndex = new AtomicInteger(0);
    private final ReferenceArrayList<VulkanServerWorldContext> registeredWorlds = new ReferenceArrayList<>();
    final ShaderCacheManager shaderCacheManager = new ShaderCacheManager();
    final ReentrantLock takeLock = new ReentrantLock();
    final Condition notEmpty = this.takeLock.newCondition();

    public boolean openDevice(VulkanDeviceMetadata device) {
        VulkanDevice vulkanDevice;
        synchronized (this) {
            for (VulkanDevice openDevice : this.openDevices) {
                if (openDevice.getMetadata().uuid().equals(device.uuid())) {
                    return false;
                }
            }
            vulkanDevice = new VulkanDevice(this, device);
            this.openDevices.add(vulkanDevice);
        }
        for (VulkanServerWorldContext world : this.registeredWorlds) {
            world.addDevice(vulkanDevice);
        }
        return true;
    }

    public void closeDevice(VulkanDeviceMetadata device) {
        VulkanDevice vulkanDevice = null;
        synchronized (this) {
            for (VulkanDevice openDevice : this.openDevices) {
                if (openDevice.getMetadata().uuid().equals(device.uuid())) {
                    vulkanDevice = openDevice;
                    this.openDevices.remove(openDevice);
                    break;
                }
            }
        }
        closeDevice0(vulkanDevice);
    }

    private void closeDevice0(VulkanDevice vulkanDevice) {
        if (vulkanDevice != null) {
            for (VulkanServerWorldContext world : this.registeredWorlds) {
                world.removeDevice(vulkanDevice);
            }
            vulkanDevice.close();
        }
    }

    public synchronized void closeAllDevices() {
        for (VulkanDevice openDevice : this.openDevices) {
            try {
                closeDevice0(openDevice);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        this.openDevices.clear();
    }

    public synchronized List<VulkanDevice> getOpenDevices() {
        return ImmutableList.copyOf(this.openDevices);
    }

    public void registerWorld(VulkanServerWorldContext world) {
        synchronized (this) {
            this.registeredWorlds.add(world);
            for (VulkanDevice device : this.openDevices) {
                world.addDevice(device);
            }
        }
    }

    public void unregisterWorld(VulkanServerWorldContext world) {
        synchronized (this) {
            this.registeredWorlds.remove(world);
        }
    }

    public void signalNotEmpty() {
        this.takeLock.lock();
        try {
            this.notEmpty.signalAll();
        } finally {
            this.takeLock.unlock();
        }
    }
}
