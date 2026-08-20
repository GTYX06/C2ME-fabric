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

import com.ishland.c2me.opts.accel.cuda.common.enumeration.CUDADeviceLocator;
import com.ishland.c2me.opts.accel.cuda.common.enumeration.CUDADeviceMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CUDAServerGlobalContext implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(CUDAServerGlobalContext.class);

    private final CUDADevice device;

    public CUDAServerGlobalContext() {
        CUDADeviceMetadata best = CUDADeviceLocator.getBestDevice();
        if (best != null) {
            this.device = new CUDADevice(best);
            LOGGER.info("Initialized CUDAServerGlobalContext with device: {}", best.name());
        } else {
            this.device = null;
            LOGGER.warn("No suitable CUDA device found for CUDAServerGlobalContext");
        }
    }

    public CUDADevice getDevice() {
        return device;
    }

    public boolean isAvailable() {
        return device != null;
    }

    @Override
    public void close() {
        if (device != null) {
            device.close();
        }
    }
}
