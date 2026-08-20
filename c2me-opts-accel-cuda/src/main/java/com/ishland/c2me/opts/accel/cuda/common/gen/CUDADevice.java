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

package com.ishland.c2me.opts.accel.cuda.common.gen;

import com.ishland.c2me.opts.accel.cuda.common.Config;
import com.ishland.c2me.opts.accel.cuda.common.bindings.CUDADriver;
import com.ishland.c2me.opts.accel.cuda.common.bindings.NVRTC;
import com.ishland.c2me.opts.accel.cuda.common.enumeration.CUDADeviceMetadata;
import com.ishland.c2me.opts.accel.cuda.common.gen.cache.CUDABufferCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class CUDADevice implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(CUDADevice.class);

    private final CUDADeviceMetadata metadata;
    private final MemorySegment context;
    private final CUDABufferCache bufferCache;
    private final ConcurrentLinkedQueue<MemorySegment> streamPool = new ConcurrentLinkedQueue<>();
    private final ConcurrentHashMap<String, MemorySegment> loadedModules = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, MemorySegment> loadedFunctions = new ConcurrentHashMap<>();
    private volatile boolean closed = false;

    public CUDADevice(CUDADeviceMetadata metadata) {
        this.metadata = metadata;
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment pCtx = arena.allocate(ValueLayout.ADDRESS);
            int res = CUDADriver.cuDevicePrimaryCtxRetain(pCtx, metadata.deviceId());
            CUDADriver.checkCUDAError(res);
            this.context = pCtx.get(ValueLayout.ADDRESS, 0);
            CUDADriver.cuCtxSetCurrent(this.context);
        }

        this.bufferCache = new CUDABufferCache();

        int streamCount = Math.max(4, Config.maxConcurrentTasksPerDevice);
        try (Arena arena = Arena.ofConfined()) {
            for (int i = 0; i < streamCount; i++) {
                MemorySegment pStream = arena.allocate(ValueLayout.ADDRESS);
                int res;
                if (Config.lowPriorityStreams) {
                    res = CUDADriver.cuStreamCreateWithPriority(pStream, CUDADriver.CU_STREAM_NON_BLOCKING, 1);
                } else {
                    res = CUDADriver.cuStreamCreate(pStream, CUDADriver.CU_STREAM_NON_BLOCKING);
                }
                CUDADriver.checkCUDAError(res);
                MemorySegment stream = pStream.get(ValueLayout.ADDRESS, 0);
                this.streamPool.offer(stream);
            }
        }
        LOGGER.info("Initialized CUDA device {} with {} execution streams", metadata.name(), streamCount);
    }

    public CUDADeviceMetadata getMetadata() {
        return metadata;
    }

    public MemorySegment getContext() {
        return context;
    }

    public CUDABufferCache getBufferCache() {
        return bufferCache;
    }

    public MemorySegment acquireStream() {
        MemorySegment stream = streamPool.poll();
        if (stream != null) {
            return stream;
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment pStream = arena.allocate(ValueLayout.ADDRESS);
            int res = CUDADriver.cuStreamCreate(pStream, CUDADriver.CU_STREAM_NON_BLOCKING);
            CUDADriver.checkCUDAError(res);
            return pStream.get(ValueLayout.ADDRESS, 0);
        }
    }

    public void releaseStream(MemorySegment stream) {
        if (!closed && stream != null && !stream.equals(MemorySegment.NULL)) {
            streamPool.offer(stream);
        }
    }

    public void bindToCurrentThread() {
        CUDADriver.cuCtxSetCurrent(this.context);
    }

    public MemorySegment loadModule(String key, byte[] ptxBytes) {
        return loadedModules.computeIfAbsent(key, k -> {
            bindToCurrentThread();
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment pModule = arena.allocate(ValueLayout.ADDRESS);
                MemorySegment imageSeg = arena.allocate(ptxBytes.length);
                MemorySegment.copy(MemorySegment.ofArray(ptxBytes), 0, imageSeg, 0, ptxBytes.length);

                int res = CUDADriver.cuModuleLoadData(pModule, imageSeg);
                CUDADriver.checkCUDAError(res);
                return pModule.get(ValueLayout.ADDRESS, 0);
            }
        });
    }

    public MemorySegment getFunction(MemorySegment module, String functionName) {
        String key = module.address() + "#" + functionName;
        return loadedFunctions.computeIfAbsent(key, k -> {
            bindToCurrentThread();
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment pFunc = arena.allocate(ValueLayout.ADDRESS);
                MemorySegment nameSeg = arena.allocateFrom(functionName);
                int res = CUDADriver.cuModuleGetFunction(pFunc, module, nameSeg);
                CUDADriver.checkCUDAError(res);
                return pFunc.get(ValueLayout.ADDRESS, 0);
            }
        });
    }

    public byte[] compileSource(String source, String progName, Map<String, String> defines) {
        return NVRTC.compileToPTX(source, progName, metadata.getArchString(), defines);
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;

        for (MemorySegment module : loadedModules.values()) {
            try {
                if (module != null && !module.equals(MemorySegment.NULL)) {
                    CUDADriver.cuModuleUnload(module);
                }
            } catch (Throwable ignored) {
            }
        }
        loadedModules.clear();
        loadedFunctions.clear();

        for (MemorySegment stream : streamPool) {
            try {
                if (stream != null && !stream.equals(MemorySegment.NULL)) {
                    CUDADriver.cuStreamDestroy(stream);
                }
            } catch (Throwable ignored) {
            }
        }
        streamPool.clear();

        bufferCache.close();

        try {
            CUDADriver.cuDevicePrimaryCtxRelease(metadata.deviceId());
        } catch (Throwable ignored) {
        }
    }
}
