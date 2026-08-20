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

import java.util.UUID;

public record CUDADeviceMetadata(
        int ordinal,
        int deviceId,
        String name,
        UUID uuid,
        int computeCapabilityMajor,
        int computeCapabilityMinor,
        long totalMemory,
        int multiProcessorCount,
        int warpSize,
        int maxThreadsPerBlock
) {
    public String getArchString() {
        return "compute_" + computeCapabilityMajor + computeCapabilityMinor;
    }

    public String getSmString() {
        return "sm_" + computeCapabilityMajor + computeCapabilityMinor;
    }

    public long score() {
        long baseScore = (long) computeCapabilityMajor * 1000000L + (long) computeCapabilityMinor * 100000L;
        baseScore += (long) multiProcessorCount * 1000L;
        baseScore += (totalMemory / (1024L * 1024L));
        return baseScore;
    }
}
