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

package com.ishland.c2me.opts.accel.cuda.common.progress;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class GlobalProgressStash {

    private static final AtomicReference<String> CURRENT_STATUS = new AtomicReference<>("");
    private static final AtomicInteger TOTAL_TASKS = new AtomicInteger(0);
    private static final AtomicInteger COMPLETED_TASKS = new AtomicInteger(0);

    public static void setStatus(String status) {
        CURRENT_STATUS.set(status);
    }

    public static String getStatus() {
        return CURRENT_STATUS.get();
    }

    public static void setProgress(int completed, int total) {
        COMPLETED_TASKS.set(completed);
        TOTAL_TASKS.set(total);
    }

    public static float getProgressRatio() {
        int total = TOTAL_TASKS.get();
        if (total <= 0) return 0.0f;
        return (float) COMPLETED_TASKS.get() / (float) total;
    }
}
