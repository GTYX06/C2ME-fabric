/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2026 GTYX06
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.ishland.c2me.opts.accel.cuda.common.enumeration;

import com.ishland.c2me.opts.accel.cuda.common.Config;
import com.ishland.c2me.opts.accel.cuda.common.bindings.CUDADriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class CUDADeviceLocator {

    private static final Logger LOGGER = LoggerFactory.getLogger(CUDADeviceLocator.class);

    private static List<CUDADeviceMetadata> availableDevices = List.of();

    public static void init() {
        if (!CUDADriver.IS_AVAILABLE) {
            LOGGER.warn("CUDA Driver API is not available on this system. CUDA acceleration is disabled.");
            return;
        }

        try (Arena arena = Arena.ofConfined()) {
            MemorySegment pVersion = arena.allocate(ValueLayout.JAVA_INT);
            int vRes = CUDADriver.cuDriverGetVersion(pVersion);
            if (vRes != CUDADriver.CUDA_SUCCESS) {
                LOGGER.warn("Failed to get CUDA Driver version: {}", CUDADriver.getErrorString(vRes));
                return;
            }
            int driverVersion = pVersion.get(ValueLayout.JAVA_INT, 0);
            int major = driverVersion / 1000;
            int minor = (driverVersion % 100) / 10;
            LOGGER.info("Detected CUDA Driver version: {}.{} (code: {})", major, minor, driverVersion);

            if (driverVersion < 12000) {
                LOGGER.warn("CUDA Driver version is {}.{}, but CUDA 12.0+ (12000+) is recommended.", major, minor);
            }

            MemorySegment pCount = arena.allocate(ValueLayout.JAVA_INT);
            int cRes = CUDADriver.cuDeviceGetCount(pCount);
            if (cRes != CUDADriver.CUDA_SUCCESS) {
                LOGGER.warn("Failed to get CUDA device count: {}", CUDADriver.getErrorString(cRes));
                return;
            }
            int count = pCount.get(ValueLayout.JAVA_INT, 0);
            LOGGER.info("Discovered {} CUDA device(s)", count);

            List<CUDADeviceMetadata> devices = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                MemorySegment pDev = arena.allocate(ValueLayout.JAVA_INT);
                int devRes = CUDADriver.cuDeviceGet(pDev, i);
                if (devRes != CUDADriver.CUDA_SUCCESS) {
                    LOGGER.warn("Failed to get device #{}: {}", i, CUDADriver.getErrorString(devRes));
                    continue;
                }
                int deviceId = pDev.get(ValueLayout.JAVA_INT, 0);

                MemorySegment pName = arena.allocate(256);
                CUDADriver.cuDeviceGetName(pName, 256, deviceId);
                String name = pName.getString(0);

                UUID uuid;
                MemorySegment pUuid = arena.allocate(16);
                int uuidRes = CUDADriver.cuDeviceGetUuid(pUuid, deviceId);
                if (uuidRes == CUDADriver.CUDA_SUCCESS) {
                    ByteBuffer bb = pUuid.asByteBuffer().order(ByteOrder.BIG_ENDIAN);
                    long mostSig = bb.getLong();
                    long leastSig = bb.getLong();
                    uuid = new UUID(mostSig, leastSig);
                } else {
                    uuid = UUID.nameUUIDFromBytes(("cuda_device_" + i + "_" + name).getBytes());
                }

                MemorySegment pAttr = arena.allocate(ValueLayout.JAVA_INT);
                CUDADriver.cuDeviceGetAttribute(pAttr, CUDADriver.CU_DEVICE_ATTRIBUTE_COMPUTE_CAPABILITY_MAJOR, deviceId);
                int ccMajor = pAttr.get(ValueLayout.JAVA_INT, 0);
                CUDADriver.cuDeviceGetAttribute(pAttr, CUDADriver.CU_DEVICE_ATTRIBUTE_COMPUTE_CAPABILITY_MINOR, deviceId);
                int ccMinor = pAttr.get(ValueLayout.JAVA_INT, 0);

                CUDADriver.cuDeviceGetAttribute(pAttr, CUDADriver.CU_DEVICE_ATTRIBUTE_MULTIPROCESSOR_COUNT, deviceId);
                int mpCount = pAttr.get(ValueLayout.JAVA_INT, 0);

                CUDADriver.cuDeviceGetAttribute(pAttr, CUDADriver.CU_DEVICE_ATTRIBUTE_WARP_SIZE, deviceId);
                int warpSize = pAttr.get(ValueLayout.JAVA_INT, 0);

                CUDADriver.cuDeviceGetAttribute(pAttr, CUDADriver.CU_DEVICE_ATTRIBUTE_MAX_THREADS_PER_BLOCK, deviceId);
                int maxThreads = pAttr.get(ValueLayout.JAVA_INT, 0);

                MemorySegment pMem = arena.allocate(ValueLayout.JAVA_LONG);
                CUDADriver.cuDeviceTotalMem(pMem, deviceId);
                long totalMem = pMem.get(ValueLayout.JAVA_LONG, 0);

                CUDADeviceMetadata metadata = new CUDADeviceMetadata(
                        i, deviceId, name, uuid,
                        ccMajor, ccMinor, totalMem,
                        mpCount, warpSize, maxThreads
                );

                LOGGER.info("CUDA Device #{}: {} (UUID: {}, CC: {}.{}, VRAM: {} MB, SMs: {})",
                        i, name, uuid, ccMajor, ccMinor, totalMem / (1024 * 1024), mpCount);

                if (!Config.deviceUUIDWhitelist.isEmpty() && !Config.deviceUUIDWhitelist.contains(uuid)) {
                    LOGGER.info("Device {} is not in whitelist, skipping", uuid);
                    continue;
                }
                if (Config.deviceUUIDBlacklist.contains(uuid)) {
                    LOGGER.info("Device {} is blacklisted, skipping", uuid);
                    continue;
                }

                devices.add(metadata);
            }

            devices.sort(Comparator.comparingLong(CUDADeviceMetadata::score).reversed());
            availableDevices = List.copyOf(devices);
        } catch (Throwable t) {
            LOGGER.error("Failed to enumerate CUDA devices", t);
        }
    }

    public static List<CUDADeviceMetadata> getAvailableDevices() {
        return availableDevices;
    }

    public static CUDADeviceMetadata getBestDevice() {
        return availableDevices.isEmpty() ? null : availableDevices.get(0);
    }
}
