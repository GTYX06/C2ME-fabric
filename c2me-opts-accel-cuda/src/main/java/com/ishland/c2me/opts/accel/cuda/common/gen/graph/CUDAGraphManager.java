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

package com.ishland.c2me.opts.accel.cuda.common.gen.graph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.foreign.MemorySegment;
import java.util.concurrent.ConcurrentHashMap;

public class CUDAGraphManager implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(CUDAGraphManager.class);

    private final ConcurrentHashMap<Integer, CUDAGraphTemplate> graphCache = new ConcurrentHashMap<>();
    private volatile boolean closed = false;

    public CUDAGraphTemplate getGraph(int batchSize) {
        if (closed) return null;
        return graphCache.get(batchSize);
    }

    public CUDAGraphTemplate registerGraph(int batchSize, MemorySegment graph, MemorySegment graphExec) {
        if (closed) return null;
        CUDAGraphTemplate template = new CUDAGraphTemplate(batchSize, graph, graphExec);
        CUDAGraphTemplate old = graphCache.put(batchSize, template);
        if (old != null) {
            old.close();
        }
        LOGGER.info("Registered and cached CUDA Graph for batch size {}", batchSize);
        return template;
    }

    @Override
    public void close() {
        closed = true;
        for (CUDAGraphTemplate template : graphCache.values()) {
            try {
                template.close();
            } catch (Throwable t) {
                LOGGER.warn("Error closing CUDAGraphTemplate", t);
            }
        }
        graphCache.clear();
    }
}
