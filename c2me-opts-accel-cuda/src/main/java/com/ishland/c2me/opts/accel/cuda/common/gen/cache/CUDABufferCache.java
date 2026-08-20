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

package com.ishland.c2me.opts.accel.cuda.common.gen.cache;

import com.ishland.c2me.opts.accel.cuda.common.bindings.CUDADriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class CUDABufferCache implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(CUDABufferCache.class);

    private final ConcurrentHashMap<Long, ConcurrentLinkedQueue<MemorySegment>> cache = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<MemorySegment> allAllocations = new ConcurrentLinkedQueue<>();
    private volatile boolean closed = false;

    private static long roundUpToPowerOfTwo(long val) {
        if (val <= 0) return 64;
        long n = val - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        n |= n >>> 32;
        return (n < 0) ? 1 : (n >= (1L << 62)) ? (1L << 62) : n + 1;
    }

    public MemorySegment acquireBuffer(long byteSize) {
        if (closed) {
            throw new IllegalStateException("CUDABufferCache is closed");
        }
        long bucketSize = roundUpToPowerOfTwo(Math.max(byteSize, 256));
        ConcurrentLinkedQueue<MemorySegment> queue = cache.get(bucketSize);
        if (queue != null) {
            MemorySegment seg = queue.poll();
            if (seg != null) {
                return seg;
            }
        }

        try (Arena arena = Arena.ofConfined()) {
            MemorySegment pDevPtr = arena.allocate(ValueLayout.ADDRESS);
            int res = CUDADriver.cuMemAlloc(pDevPtr, bucketSize);
            CUDADriver.checkCUDAError(res);
            MemorySegment devPtr = pDevPtr.get(ValueLayout.ADDRESS, 0);
            allAllocations.add(devPtr);
            return devPtr;
        }
    }

    public void releaseBuffer(MemorySegment devPtr, long byteSize) {
        if (closed || devPtr == null || devPtr.equals(MemorySegment.NULL)) {
            return;
        }
        long bucketSize = roundUpToPowerOfTwo(Math.max(byteSize, 256));
        cache.computeIfAbsent(bucketSize, k -> new ConcurrentLinkedQueue<>()).offer(devPtr);
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        cache.clear();
        for (MemorySegment seg : allAllocations) {
            try {
                if (seg != null && !seg.equals(MemorySegment.NULL)) {
                    CUDADriver.cuMemFree(seg);
                }
            } catch (Throwable t) {
                LOGGER.trace("Error freeing CUDA device memory during buffer cache shutdown", t);
            }
        }
        allAllocations.clear();
    }
}
