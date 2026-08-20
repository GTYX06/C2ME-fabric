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

import com.ishland.c2me.opts.accel.cuda.common.bindings.CUDADriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.foreign.MemorySegment;
import java.util.concurrent.atomic.AtomicBoolean;

public class CUDAGraphTemplate implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(CUDAGraphTemplate.class);

    private final int batchSize;
    private final MemorySegment graph;
    private final MemorySegment graphExec;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public CUDAGraphTemplate(int batchSize, MemorySegment graph, MemorySegment graphExec) {
        this.batchSize = batchSize;
        this.graph = graph;
        this.graphExec = graphExec;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public MemorySegment getGraph() {
        return graph;
    }

    public MemorySegment getGraphExec() {
        return graphExec;
    }

    public int launch(MemorySegment stream) {
        if (closed.get()) {
            throw new IllegalStateException("CUDAGraphTemplate is already closed");
        }
        return CUDADriver.cuGraphLaunch(graphExec, stream);
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            if (graphExec != null && !graphExec.equals(MemorySegment.NULL)) {
                try {
                    CUDADriver.cuGraphExecDestroy(graphExec);
                } catch (Throwable t) {
                    LOGGER.warn("Failed to destroy CUgraphExec", t);
                }
            }
            if (graph != null && !graph.equals(MemorySegment.NULL)) {
                try {
                    CUDADriver.cuGraphDestroy(graph);
                } catch (Throwable t) {
                    LOGGER.warn("Failed to destroy CUgraph", t);
                }
            }
        }
    }
}
